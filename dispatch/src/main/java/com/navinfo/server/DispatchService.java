package com.navinfo.server;

import com.navinfo.server.cache.ClientCache;
import com.navinfo.server.common.msg.*;
import com.navinfo.server.common.msg.body.*;
import com.navinfo.server.common.msg.utils.TimeUtils;
import com.navinfo.server.queue.QueueProducer;
import com.navinfo.server.queue.memory.InMemoryQueueProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.UUID;


@Slf4j
@Service
public class DispatchService {

    @Autowired
    private ClientCache clientCache;

    private QueueProducer dispatchResponseProducer;

    @PostConstruct
    public void init() {
        dispatchResponseProducer = new InMemoryQueueProducer();
    }

    /**
     * 处理一键泊车请求
     *
     * @param msg
     */
    public void processParking(QueueMsg msg) {
        boolean vehicleOnline = vehicleOnlineResponse(msg);
        if (vehicleOnline) {
            MsgDataPack pack = msg.getPack();
            pack.setCommand((byte) CommandType.PARKING_FORWARD.getValue());
            pack.calcCheckNum();
            sendDispatchResponse(pack, msg.getClientId());
        }
    }


    public void processAction(QueueMsg msg) {
        boolean vehicleOnline = vehicleOnlineResponse(msg);
        if (vehicleOnline) {
            MsgDataPack pack = msg.getPack();
            pack.setCommand((byte) CommandType.ACTION_FORWARD.getValue());
            pack.calcCheckNum();
            sendDispatchResponse(pack, msg.getClientId());
        }
    }

    public void processPause(QueueMsg msg) {
        boolean vehicleOnline = vehicleOnlineResponse(msg);
        if (vehicleOnline) {
            MsgDataPack pack = msg.getPack();
            pack.setCommand((byte) CommandType.PAUSE_FORWARD.getValue());
            pack.calcCheckNum();
            sendDispatchResponse(pack, msg.getClientId());
        }
    }

    public void processRecover(QueueMsg msg) {
        boolean vehicleOnline = vehicleOnlineResponse(msg);
        if (vehicleOnline) {
            MsgDataPack pack = msg.getPack();
            pack.setCommand((byte) CommandType.RECOVER_FORWARD.getValue());
            pack.calcCheckNum();
            sendDispatchResponse(pack, msg.getClientId());
        }
    }

    public void processStop(QueueMsg msg) {
        boolean vehicleOnline = vehicleOnlineResponse(msg);
        if (vehicleOnline) {
            MsgDataPack pack = msg.getPack();
            pack.setCommand((byte) CommandType.STOP_FORWARD.getValue());
            pack.calcCheckNum();
            sendDispatchResponse(pack, msg.getClientId());
        }
    }

    /**
     * 处理应答消息
     * @param msg
     */
    public void processRes(QueueMsg msg){
        String vehicleId = msg.getClientId();
        String appSession = clientCache.getAppSession(vehicleId);
        msg.setSessionId(appSession);
        msg.setTopic(ServiceType.AVP_DISPATCH_FORWARD.name());
        dispatchResponseProducer.send(msg.getTopic(), msg);
    }


    /**
     * 车辆是否在线
     * @param msg 请求消息
     * @return
     */
    private boolean vehicleOnlineResponse(QueueMsg msg) {
        byte onlineState = 0x01;
        int packRes = MsgDataPack.RES_SUCCESS;
        boolean vehicleOnline = clientCache.getVehicleSession(msg.getClientId()) != null;
        if (!vehicleOnline) {
            onlineState = 0x02;
            packRes = MsgDataPack.RES_FAILED;
        }
        DataBody data = msg.build();
        VehicleOnlineResponse vehicleOnlineResponse = new VehicleOnlineResponse();
        vehicleOnlineResponse.setTs(data.getTs());
        vehicleOnlineResponse.setOnlineState(onlineState);
        byte[] body = vehicleOnlineResponse.toBytes().array();
        MsgDataPack pack = MsgDataPack.builder()
                .start("##")
                .timestamp(TimeUtils.time2Bytes(System.currentTimeMillis()))
                .version((byte) 1)
                .command((byte) CommandType.ONLINE_FORWARD_VEHICLE.getValue())
                .res((byte) packRes)
                .clientId(msg.getClientId())
                .crypto((byte) 1)
                .bodyLen(body.length)
                .body(body)
                .checkNum((byte) 0)
                .build();
        pack.calcCheckNum();
        QueueMsg resMsg = QueueMsg.builder()
                .id(UUID.randomUUID())
                .clientId(msg.getClientId())
                .sessionId(msg.getSessionId())
                .msgType(new MsgType(pack.getCommand(), pack.getRes()))
                .topic(ServiceType.AVP_DISPATCH_FORWARD.name())
                .pack(pack)
                .build();
        dispatchResponseProducer.send(resMsg.getTopic(), resMsg);
        return vehicleOnline;
    }

    /**
     * 下发调度请求
     *
     * @param pack
     * @param clientId
     */
    private void sendDispatchResponse(MsgDataPack pack, String clientId) {
        QueueMsg res = QueueMsg.builder()
                .id(UUID.randomUUID())
                .clientId(clientId)
                .sessionId(clientCache.getVehicleSession(clientId))
                .pack(pack)
                .msgType(new MsgType(pack.getCommand(), pack.getRes()))
                .topic(ServiceType.AVP_DISPATCH_FORWARD.name())
                .build();
        dispatchResponseProducer.send(res.getTopic(), res);
    }


}
