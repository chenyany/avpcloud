package com.navinfo.server.service.impl;

import cn.hutool.core.io.FileUtil;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.navinfo.server.common.msg.*;
import com.navinfo.server.common.msg.body.DataBody;
import com.navinfo.server.common.msg.body.MapManagerReq;
import com.navinfo.server.common.msg.body.MapReq;
import com.navinfo.server.common.msg.utils.AvpThreadFactory;
import com.navinfo.server.common.msg.utils.TimeUtils;
import com.navinfo.server.queue.QueueProducer;
import com.navinfo.server.queue.common.AsyncCallbackTemplate;
import com.navinfo.server.queue.memory.InMemoryQueueProducer;
import com.navinfo.server.utils.MappedFileReader;
import com.navinfo.server.utils.NativeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

@Slf4j
@Service
public class MapDataService {

    private final byte TRANSPORT_STATE_START = 0x01;
    private final byte TRANSPORT_STATE_DATA = 0x02;
    private final byte TRANSPORT_STATE_FINISH = 0x03;

    private final ScheduledExecutorService timeoutExecutor = Executors.newScheduledThreadPool(1, AvpThreadFactory.forName("AVP_ROUTE_TIMEOUT"));
    private final ExecutorService callbackExecutor = Executors.newFixedThreadPool(1, AvpThreadFactory.forName("AVP_ROUTE_CALLBACK"));
    private final ListeningExecutorService callRouteExecutor = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor(AvpThreadFactory.forName("AVP_ROUTE_CALL")));
    private QueueProducer dataForwardProducer;

    @Value("${file.download.size: 7340032}")
    private int file_download_size;

    @Value("${file.map.dir: E:/map/}")
    private String mapDir;

    @PostConstruct
    public void init() {
        dataForwardProducer = new InMemoryQueueProducer();
    }


    /**
     * ????????????????????????
     *
     * @param req ????????????
     */
    public void downloadMap(QueueMsg req) {
        QueueMsg<MapReq> msg = new QueueMsg<>(req);
        CommandType reqCmd = msg.getMsgType().getCommandType();
        byte resCmd = (byte) (reqCmd == CommandType.APP_MAP_DOWNLOAD ?
                CommandType.MAP_FORWARD_VEHICLE.getValue() : CommandType.MAP_FORWARD_APP.getValue());
        MapReq data = msg.build();
        byte[] reqTime = TimeUtils.time2Bytes(data.getTs());
        int parkingId = data.getParkingId(); //?????????id
        String filePath = mapDir + "/mapdata/" + parkingId;

        //???????????????
        if (!FileUtil.exist(filePath)) {
            MsgDataPack pack = msg.getPack();
            pack.setRes((byte) MsgDataPack.RES_FAILED);
            pack.setBodyLen(reqTime.length);
            pack.setBody(reqTime);
            pack.calcCheckNum();
            QueueMsg failedRes = responseMsg(req, pack);
            dataForwardProducer.send(failedRes.getTopic(), failedRes);
            return;
        }
        ByteBuffer dataId = ByteBuffer.allocate(4);
        dataId.putInt(parkingId);


        //???????????????
        byte[] startBody = buildBody(9, dataId.array(), TRANSPORT_STATE_START, null);
        QueueMsg startRes = buildResponse(req, startBody, resCmd);
        dataForwardProducer.send(startRes.getTopic(), startRes);

        //?????????
        try (MappedFileReader fileReader = new MappedFileReader(filePath, file_download_size)) {
            while (fileReader.read() != -1) {
                byte[] fileData = fileReader.getArray();
                byte[] dataBody = buildBody(9 + fileData.length, dataId.array(), TRANSPORT_STATE_DATA, fileData);
                QueueMsg dataRes = buildResponse(req, dataBody, resCmd);
                dataForwardProducer.send(dataRes.getTopic(), dataRes);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //???????????????
        byte[] finishBody = buildBody(9, dataId.array(), TRANSPORT_STATE_FINISH, null);
        QueueMsg finishRes = buildResponse(req, finishBody, resCmd);
        dataForwardProducer.send(finishRes.getTopic(), finishRes);
    }


    /**
     * ??????????????????
     *
     * @param msg ??????
     */
    public void routePlanning(QueueMsg msg) {
        //?????????????????????
        Consumer<Throwable> onCallError = t -> {
            try {
                QueueMsg<DataBody> resMsg = new QueueMsg<>(msg);
                long reqTs = resMsg.build().getTs();
                MsgDataPack resPack = resMsg.getPack();
                resPack.setCommand((byte) CommandType.ROUTE_PLANNING_FORWARD.getValue());
                resPack.setRes((byte) MsgDataPack.RES_FAILED);
                resPack.setBodyLen(9);
                resPack.setBody(TimeUtils.time2Bytes(reqTs));
                resPack.calcCheckNum();
                resMsg.setId(UUID.randomUUID());
                resMsg.setMsgType(new MsgType(resPack.getCommand(), resPack.getRes()));
                resMsg.setTopic(ServiceType.AVP_DATA_FORWARD.name());
                sendResponse(resMsg);
            }catch (Exception e){
                log.error("call error" , e);
            }
        };
        //????????????
        Consumer<byte[]> onCallSuccess = result -> {
            try {
                MsgDataPack pack = MsgDataPack.builder()
                        .start("##")
                        .timestamp(TimeUtils.time2Bytes(System.currentTimeMillis()))
                        .version((byte) 1)
                        .clientId(msg.getClientId())
                        .command((byte) CommandType.ROUTE_PLANNING_FORWARD.getValue())
                        .res((byte) MsgDataPack.RES_DATA)
                        .crypto((byte) 1)
                        .bodyLen(result.length)
                        .body(result)
                        .checkNum((byte) 0)
                        .build();
                pack.calcCheckNum();
                MsgType msgType = new MsgType(pack.getCommand(), pack.getRes());
                QueueMsg resMsg = QueueMsg.builder()
                        .id(UUID.randomUUID())
                        .msgType(msgType)
                        .clientId(msg.getClientId())
                        .sessionId(msg.getSessionId())
                        .pack(pack)
                        .topic(ServiceType.AVP_DATA_FORWARD.name())
                        .build();
                sendResponse(resMsg);
            }catch (Exception e){
                log.error("success callback error:", e);
                throw e;
            }
        };

        ListenableFuture<byte[]> routeFuture = callRouteExecutor.submit(() -> {
            int bodyLen = msg.getPack().getBodyLen();
            ByteBuffer buf = ByteBuffer.allocate(4 + bodyLen);
            buf.putInt(bodyLen);
            buf.put(msg.getPack().getBody());
            byte[] result = null;
            try {
                result = NativeUtils.callRoutePlanning(buf.array());
            }catch (Exception e){
                log.error("call error", e);
                throw e;
            }
            if(result == null){
                throw new RuntimeException("route result is null");
            }
            return result;
        });
        //?????????????????? ???????????????????????????????????????????????????????????? ??????????????????5???
        AsyncCallbackTemplate.withCallbackAndTimeout(routeFuture, onCallSuccess, onCallError, 5000, timeoutExecutor, callbackExecutor);
    }

    /**
     * ??????????????????
     *
     * @param req
     */
    public void downloadMapManager(QueueMsg req) {
        QueueMsg<MapManagerReq> msg = new QueueMsg<>(req);
        MapManagerReq body = msg.build();
        String areaId = body.getAreaId();
        String path = mapDir + "/manager/" + areaId;
        if (!FileUtil.exist(path)) {
            MsgDataPack pack = req.getPack();
            pack.setRes((byte) MsgDataPack.RES_FAILED);
            pack.calcCheckNum();
            QueueMsg res = QueueMsg.builder()
                    .id(UUID.randomUUID())
                    .sessionId(req.getSessionId())
                    .clientId(req.getClientId())
                    .pack(pack)
                    .topic(ServiceType.AVP_DATA_FORWARD.name())
                    .msgType(req.getMsgType())
                    .build();
            dataForwardProducer.send(res.getTopic(), res);
            return;
        }

        byte resCmd = (byte) CommandType.MAP_MANAGER_FORWARD_VEHICLE.getValue();
        //???????????????
        byte[] startBody = buildBody(25, areaId.getBytes(), TRANSPORT_STATE_START, null);
        QueueMsg startRes = buildResponse(req, startBody, resCmd);
        sendResponse(startRes);

        //?????????
        try (MappedFileReader fileReader = new MappedFileReader(path, file_download_size)) {
            while (fileReader.read() != -1) {
                byte[] fileData = fileReader.getArray();
                byte[] dataBody = buildBody(25 + fileData.length, areaId.getBytes(), TRANSPORT_STATE_DATA, fileData);
                QueueMsg dataRes = buildResponse(req, dataBody, resCmd);
                sendResponse(dataRes);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //???????????????
        byte[] finishBody = buildBody(25, areaId.getBytes(), TRANSPORT_STATE_FINISH, null);
        QueueMsg finishRes = buildResponse(req, finishBody, resCmd);
        sendResponse(finishRes);
    }


    /**
     * ??????????????????
     *
     * @param packLen ??????????????????
     * @param dataId  ????????????id
     * @param data    ??????
     * @return ?????????
     */
    private byte[] buildBody(int packLen, byte[] dataId, byte state, byte[] data) {
        ByteBuffer body = ByteBuffer.allocate(packLen);
        body.put(dataId);
        body.put(state);
        if (data == null) {
            body.putInt(0);
        } else {
            body.putInt(data.length);
            body.put(data);
        }
        body.put(data);
        return body.array();
    }

    /**
     * ??????????????????
     *
     * @param req
     * @param body
     * @param res
     * @return
     */
    private QueueMsg buildResponse(QueueMsg req, byte[] body, byte res) {
        MsgDataPack pack = buildPack(body, res);
        pack.calcCheckNum();
        return QueueMsg.builder()
                .id(UUID.randomUUID())
                .clientId(req.getClientId())
                .sessionId(req.getSessionId())
                .msgType(new MsgType(pack.getCommand(), pack.getRes()))
                .topic(ServiceType.AVP_DATA_FORWARD.name())
                .pack(pack)
                .build();
    }

    private MsgDataPack buildPack(byte[] body, byte cmd) {
        return MsgDataPack.builder()
                .start("##")
                .timestamp(TimeUtils.time2Bytes(System.currentTimeMillis()))
                .version((byte) 1)
                .command(cmd)
                .res((byte) MsgDataPack.RES_DATA)
                .crypto((byte) 1)
                .bodyLen(body.length)
                .body(body)
                .checkNum((byte) 0)
                .build();
    }

    private QueueMsg responseMsg(QueueMsg req, MsgDataPack pack) {
        return QueueMsg.builder()
                .id(UUID.randomUUID())
                .clientId(req.getClientId())
                .sessionId(req.getSessionId())
                .msgType(new MsgType(pack.getCommand(), pack.getRes()))
                .topic(ServiceType.AVP_DATA_FORWARD.name())
                .pack(pack)
                .build();
    }


    private void sendResponse(QueueMsg msg) {
        dataForwardProducer.send(msg.getTopic(), msg);
    }


}
