package com.navinfo.server.service;

import com.navinfo.server.cache.ClientCache;
import com.navinfo.server.common.msg.*;
import com.navinfo.server.common.msg.utils.TimeUtils;
import com.navinfo.server.gateway.SessionManager;
import com.navinfo.server.queue.QueueConsumer;
import com.navinfo.server.queue.QueueProducer;
import com.navinfo.server.queue.memory.InMemoryQueueConsumer;
import com.navinfo.server.queue.memory.InMemoryQueueProducer;
import com.navinfo.server.common.msg.utils.AvpThreadFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.websocket.Session;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 数据传输服务
 * 接收请求数据
 * 发送响应数据
 */
@Slf4j
@Service
public class TransportService {

    @Autowired
    private ClientCache clientCache;
    private SessionManager sessionManager;

    private final ExecutorService coreConsumerExecutor = Executors.newSingleThreadExecutor(AvpThreadFactory.forName("AVP_CORE_RES"));
    private final ExecutorService mapConsumerExecutor = Executors.newSingleThreadExecutor(AvpThreadFactory.forName("AVP_MAP_RES"));
    private final ExecutorService dispatchConsumerExecutor = Executors.newSingleThreadExecutor(AvpThreadFactory.forName("AVP_DISPATCH_RES"));
    private final ExecutorService heartBeatExecService = Executors.newSingleThreadExecutor(AvpThreadFactory.forName("AVP_HEART_BEAT_RES"));
    private final ExecutorService responseExecutor = Executors.newFixedThreadPool(5, AvpThreadFactory.forName("AVP_PROCESS_RES"));

    private QueueConsumer coreConsumer;
    private QueueProducer producer;
    private QueueConsumer mapConsumer;
    private QueueConsumer dispatchConsumer;
    private QueueConsumer heartBeatConsumer;

    @PostConstruct
    public void init() {
        sessionManager = SessionManager.getInstance();
        //后期改为工厂方式创建，方便替换消息队列
        producer = new InMemoryQueueProducer();
        coreConsumer = new InMemoryQueueConsumer(ServiceType.AVP_CORE_FORWARD.name());
        mapConsumer = new InMemoryQueueConsumer(ServiceType.AVP_DATA_FORWARD.name());
        dispatchConsumer = new InMemoryQueueConsumer(ServiceType.AVP_DISPATCH_FORWARD.name());
        heartBeatConsumer = new InMemoryQueueConsumer(ServiceType.AVP_HEART_BEAT_FORWARD.name());

        coreConsumerExecutor.execute(() ->
                consumeResponseMsg(coreConsumer)
        );
        mapConsumerExecutor.execute(() ->
                consumeResponseMsg(mapConsumer)
        );
        dispatchConsumerExecutor.execute(() ->
                consumeResponseMsg(dispatchConsumer)
        );

        heartBeatExecService.execute(() ->
                consumeHearBeatResponseMsg()
        );

    }

    /*消费响应消息*/
    private void consumeResponseMsg(QueueConsumer consumer) {
        while (true) {
            List<QueueMsg> records = consumer.poll(500);
            if (records.size() == 0) {
                continue;
            }
            records.forEach((record) ->
                    responseExecutor.execute(() -> {
                        try {
                            processResponse(record);
                        } catch (Exception e) {
                            log.error("process response error: ", e);
                        }
                    })
            );
        }
    }

    /*处理心跳响应*/
    private void consumeHearBeatResponseMsg() {
        while (true) {
            List<QueueMsg> messages = heartBeatConsumer.poll(500);
            if (messages.size() == 0) {
                continue;
            }
            messages.forEach((message) -> responseExecutor.execute(() -> {
                //心跳检测失败
                try {
                    if (message.getMsgType().getRes() == MsgDataPack.RES_FAILED) {
                        Session session = sessionManager.get(message.getSessionId());
                        if (session != null) {
                            log.info("close disconnection session : clientId[" + message.getClientId() + "]");
                            sessionManager.close(session);
                        }
                    } else {
                        processResponse(message);
                    }
                } catch (Exception e) {
                    log.error("heart beat response error", e);
                }
            }));

        }
    }

    /**
     * 下发请求
     *
     * @param msg
     */
    private void processResponse(QueueMsg msg) {
//        log.info("RESPONSE PACK : [cmd:" + PrintUtils.toHex(msg.getPack().getCommand()) + ", res:" + PrintUtils.toHex(msg.getPack().getRes())+"]");
        ByteBuffer buf = msg.getPack().toBytes();
        buf.flip();
        try {
            Session session = sessionManager.get(msg.getSessionId());
            if(session == null){ //连接断开消息丢弃
                return;
            }
            sendResponse(session, buf);
        } catch (IOException e) {
            log.info("send response error", e);
        }
    }

    /* 临时同步处理 session 不能同时发送消息 AsyncBasicRemote也不行*/
    private void sendResponse(Session session, ByteBuffer data) throws IOException {
        synchronized (session) {
            session.getBasicRemote().sendBinary(data);
//            log.info("message send time: " + System.currentTimeMillis());
        }
    }


    /**
     * 处理服务端接收的请求封装后发送到消息队列
     *
     * @param msg
     */

    /*
    public void process(ByteBuffer msg, String sessionId) {
        //1、异或校验
        //2、登录校验


        long ts;
        int resCode = 0;
        byte commonRes = MsgDataPack.RES_SUCCESS;
        boolean isLogin = false;
        if (MsgDataPack.xorCheck(msg)) {
            MsgDataPack pack = MsgDataPack.fromBytes(msg);
            if (CommandType.APP_LOGIN.getValue() != Byte.toUnsignedInt(pack.getCommand())
                    && CommandType.VEHICLE_LOGIN.getValue() != Byte.toUnsignedInt(pack.getCommand())) {
                isLogin = clientCache.exist(pack.getClientId());
            }
            if (isLogin) {
                MsgType msgType = new MsgType(pack.getCommand(), pack.getRes());
                QueueMsg queueMsg = QueueMsg.builder()
                        .id(UUID.randomUUID())
                        .sessionId(sessionId)
                        .clientId(pack.getClientId())
                        .topic(choseTopic(msgType))
                        .msgType(msgType)
                        .pack(pack)
                        .build();
                resCode = Byte.toUnsignedInt(pack.getRes());
                if (resCode == MsgDataPack.RES_COMMAND) { //客户端响应不处理
                    producer.send(queueMsg.getTopic(), queueMsg);
                }
                ts = queueMsg.build().getTs();
            } else {
                ts = System.currentTimeMillis();
            }
        } else { //校验码错误
            ts = System.currentTimeMillis();
            commonRes = MsgDataPack.RES_FAILED;
            //记录校验错误包
        }

        if (resCode == MsgDataPack.RES_COMMAND
                || resCode == 0
                || resCode == MsgDataPack.RES_NO_LOGIN) {
            //上行请求全部带有时间
            MsgDataPack res = MsgDataPack.fromBytes(msg);
            res.setRes(commonRes);
            res.setBodyLen(9);
            res.setBody(TimeUtils.time2Bytes(ts));
            res.calcCheckNum();
            try {
                ByteBuffer resBuffer = res.toBytes();
                resBuffer.flip();
                sendResponse(sessionManager.get(sessionId), resBuffer);
//                    sessionManager.get(sessionId).getBasicRemote().sendBinary(resBuffer);
            } catch (IOException e) {
                log.error("COMMON RESPONSE ERROR", e);
            }
        }
    }
    */

    /**
     * 消息处理
     * @param buffer
     * @param sessionId
     */
    public void process(ByteBuffer buffer, String sessionId) {
        long ts;
        byte resCmd = MsgDataPack.RES_SUCCESS;
        MsgDataPack resPack = null;
        if (MsgDataPack.xorCheck(buffer)) {
            MsgDataPack pack = MsgDataPack.fromBytes(buffer);
            MsgType msgType = new MsgType(pack.getCommand(), pack.getRes());
            QueueMsg msg = QueueMsg.builder()
                    .id(UUID.randomUUID())
                    .sessionId(sessionId)
                    .clientId(pack.getClientId())
                    .topic(choseTopic(msgType))
                    .msgType(msgType)
                    .pack(pack)
                    .build();
            ts = TimeUtils.bytes2Time(pack.getTimestamp());
            if (checkClientLogin(pack)
                    && Byte.toUnsignedInt(pack.getRes()) == MsgDataPack.RES_COMMAND) { //客户端应答请求不处理
                producer.send(msg.getTopic(), msg);
            } else {
                resCmd = MsgDataPack.RES_NO_LOGIN;
            }
        } else {

            StringBuffer buf = new StringBuffer();
            buf.append("[");
            for (byte b : buffer.array()) {
                buf.append(b).append(",");
            }
            buf.append("]");
            log.error("xor error: " + buf.toString());

            resCmd = MsgDataPack.RES_FAILED;
            ts = System.currentTimeMillis(); //校验码错误 无法获取包中时间
        }
        try {
            resPack = MsgDataPack.fromBytes(buffer); //解析错误包
        }catch (Exception e){
            resPack = MsgDataPack.base();
            resPack.setClientId("00000000000000000");
            resPack.setCommand((byte) CommandType.APP_LOGOUT.getValue());//随便放一个
        }
        resPack.setRes(resCmd);
        byte[] body = TimeUtils.time2Bytes(ts);
        resPack.setBodyLen(body.length);
        resPack.setBody(body);
        resPack.calcCheckNum();
        ByteBuffer resBuffer = resPack.toBytes();
        resBuffer.flip();
        try {
            Session session = sessionManager.get(sessionId);
            if(session == null){ //连接已断开，丢弃
                return;
            }
            sendResponse(session, resBuffer);
        } catch (IOException e) {
            log.error("COMMON RESPONSE ERROR", e);
        }
    }

    private boolean checkClientLogin(MsgDataPack pack) {
        int cmd = Byte.toUnsignedInt(pack.getCommand());
        if (CommandType.VEHICLE_LOGIN.getValue() != cmd
                && CommandType.APP_LOGIN.getValue() != cmd) {
            return clientCache.exist(pack.getClientId());
        }
        return true;
    }


    /**
     * 通用校验应答
     * 保留时间戳
     * 更新应答表示
     *
     * @param pack
     * @param resCode
     * @param ts
     */
    private void commonResponse(MsgDataPack pack, byte resCode, long ts, String sessionId) throws IOException {
        pack.setRes(resCode);
        byte[] body = TimeUtils.time2Bytes(ts);
        pack.setBodyLen(body.length);
        pack.setBody(body);
        pack.calcCheckNum();
        ByteBuffer buffer = pack.toBytes();
        buffer.flip();
        sendResponse(sessionManager.get(sessionId), buffer);

    }


    private String choseTopic(MsgType msgType) {
        ServiceType type;
        switch (msgType.getCommandType()) {
            case VEHICLE_LOGIN:
            case VEHICLE_LOGOUT:
            case VEHICLE_REALTIME_UPLOAD:
            case VEHICLE_FAULT_UPLOAD:
            case VEHICLE_WARNING_UPLOAD:
            case VEHICLE_POSITION_UPLOAD:
            case APP_LOGIN:
            case APP_LOGOUT:
                type = ServiceType.AVP_CORE;
                break;
            case VEHICLE_MAP_MANAGER_DOWNLOAD:
            case VEHICLE_MAP_DOWNLOAD:
            case APP_MAP_DOWNLOAD:
            case VEHICLE_ROUTE_PLANNING:
                type = ServiceType.AVP_DATA;
                break;
            case APP_DISPATCH_PARKING:
            case APP_DISPATCH_ACTION:
            case APP_DISPATCH_PAUSE:
            case APP_DISPATCH_RECOVER:
            case APP_DISPATCH_STOP:
                type = ServiceType.AVP_DISPATCH;
                break;
            case APP_HEART_BEAT:
            case VEHICLE_HEART_BEAT:
                type = ServiceType.AVP_HEART_BEAT;
                break;
            default:
                type = ServiceType.AVP_CORE;
        }
        return type.name();
    }

    /**
     * 处理意外断开情况
     */
    public void processClose(String sessionId) {
        log.info("removeSession in clientCache : " + sessionId);
        clientCache.removeSession(sessionId);
    }

}
