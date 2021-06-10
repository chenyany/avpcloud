package com.navinfo.server.service.core;

import com.navinfo.server.cache.ClientCache;
import com.navinfo.server.common.msg.*;
import com.navinfo.server.common.msg.utils.AvpThreadFactory;
import com.navinfo.server.gateway.SessionManager;
import com.navinfo.server.queue.QueueConsumer;
import com.navinfo.server.queue.QueueProducer;
import com.navinfo.server.queue.memory.InMemoryQueueConsumer;
import com.navinfo.server.queue.memory.InMemoryQueueProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 非耗时请求处理
 */
@Slf4j
@Service
public class CoreMsgHandler implements CommandLineRunner {

    private SessionManager sessionManager;

    @Autowired
    private ClientCache clientCache;
    private final ExecutorService coreConsumerExecutor = Executors.newFixedThreadPool(3, AvpThreadFactory.forName("AVP_CORE_PROCESS"));
    private final ExecutorService coreProcessExecutor = Executors.newSingleThreadExecutor(AvpThreadFactory.forName("AVP_CORE_REQ"));
    private QueueProducer coreProducer;
    private QueueConsumer coreConsumer;


    @PostConstruct
    public void init() {
        sessionManager = SessionManager.getInstance();
        coreProducer = new InMemoryQueueProducer();
        coreConsumer = new InMemoryQueueConsumer<>(ServiceType.AVP_CORE.name());
        coreConsumerExecutor.execute(() -> {
            while (true) {
                List<QueueMsg> records = coreConsumer.poll(100);
                if (records.size() == 0) {
                    continue;
                }
                records.forEach(record -> {
                    try {
                        coreProcessExecutor.execute(() -> process(record));
                    } catch (Exception e) {
                        log.warn("Failed to process the notification.", e);
                    }
                });
            }
        });
        log.info("core service init");
    }

    /**
     * 根据消息类型 路由消息
     * 非耗时处理 不使用线程
     *
     * @param msg
     */
    private void process(QueueMsg msg) {
        CommandType type = msg.getMsgType().getCommandType();
        switch (type) {
            case VEHICLE_LOGIN:
            case APP_LOGIN:
                processLogin(msg);
                break;
            case VEHICLE_LOGOUT:
            case APP_LOGOUT:
                processLogout(msg);
                break;
            case VEHICLE_REALTIME_UPLOAD:
                processRealtime(msg);
                break;
            case VEHICLE_FAULT_UPLOAD:
                processFault(msg);
                break;
            case VEHICLE_WARNING_UPLOAD:
                processWarning(msg);
                break;
            case VEHICLE_POSITION_UPLOAD:
                processPosition(msg);
                break;
        }
    }

    /**
     * 位置数据上报
     * @param msg
     */
    private void processPosition(QueueMsg msg) {
        QueueMsg resMsg = dataResponse(msg, CommandType.POSITION_FORWARD_APP.getValue());
        coreProducer.send(resMsg.getTopic(), resMsg);
    }


    /**
     * 告警数据上报
     *
     * @param msg
     */
    private void processWarning(QueueMsg msg) {
        QueueMsg resMsg = dataResponse(msg, CommandType.WARNING_FORWARD.getValue());
        coreProducer.send(resMsg.getTopic(), resMsg);
    }

    /**
     * 故障数据上报
     *
     * @param msg
     */
    private void processFault(QueueMsg msg) {
        QueueMsg resMsg = dataResponse(msg, CommandType.FAULT_FORWARD.getValue());
        coreProducer.send(resMsg.getTopic(), resMsg);
    }

    /**
     * 实时数据上传
     *
     * @param msg
     */
    private void processRealtime(QueueMsg msg) {
        QueueMsg resMsg = dataResponse(msg, CommandType.REALTIME_FORWARD.getValue());
        if (resMsg == null) {
            return;
        }
        coreProducer.send(resMsg.getTopic(), resMsg);
    }

    /**
     * 登出请求
     *
     * @param msg
     */
    private void processLogout(QueueMsg msg) {
        MsgDataPack pack = MsgDataPack.base();
        ByteBuffer body = ByteBuffer.allocate(10);
        body.put(pack.getTimestamp());
        try {
            //保留连接，清除缓存
            clientCache.removeClient(msg.getClientId());
            body.put((byte) MsgDataPack.RES_SUCCESS);
        }catch (Exception e){
            body.put((byte) MsgDataPack.RES_FAILED);
        }
        pack.setClientId(msg.getClientId());
        CommandType resCmd = resCmd(msg.getMsgType().getCommandType());
        pack.setRes((byte) MsgDataPack.RES_COMMAND);
        pack.setCommand((byte) resCmd.getValue());
        pack.setBodyLen(body.capacity());
        pack.setBody(body.array());
        pack.calcCheckNum();

        msg.setTopic(ServiceType.AVP_CORE_FORWARD.name());
        msg.setPack(pack);
        msg.setMsgType(new MsgType(pack.getCommand(), pack.getRes()));
        coreProducer.send(msg.getTopic(), msg);
    }

    /**
     * 登入请求
     *
     * @param msg
     */
    private void processLogin(QueueMsg msg) {

        MsgDataPack pack = MsgDataPack.base();
        CommandType resCmd = resCmd(msg.getMsgType().getCommandType());
        ByteBuffer body = ByteBuffer.allocate(10);
        body.put(pack.getTimestamp());
        //登入成功
        if (clientCache.validateClient(msg.getClientId())) {
            //重复登入 关闭之前连接
            if (clientCache.exist(msg.getClientId())) {
                String oldSession = clientCache.getClientSession(msg.getClientId());
                sessionManager.remove(oldSession);
            }
            clientCache.setClientSession(msg.getClientId(), msg.getSessionId());
            clientCache.status();
            body.put((byte) MsgDataPack.RES_SUCCESS);
        }else{
            //登入失败
            body.put((byte) MsgDataPack.RES_FAILED);
        }
        pack.setRes((byte) MsgDataPack.RES_COMMAND);
        pack.setClientId(msg.getClientId());
        pack.setCommand((byte) resCmd.getValue());
        pack.setBodyLen(body.capacity());
        pack.setBody(body.array());
        pack.calcCheckNum();

        msg.setTopic(ServiceType.AVP_CORE_FORWARD.name());
        msg.setPack(pack);
        msg.setMsgType(new MsgType(pack.getCommand(), pack.getRes()));
        coreProducer.send(msg.getTopic(), msg);
    }

    /**
     * 数据转发
     *
     * @param msg
     * @param command
     * @return
     */
    private QueueMsg dataResponse(QueueMsg msg, int command) {
        String vin = msg.getClientId();
        String sessionId = clientCache.getAppSession(vin);
        //app不在线
        if (sessionId == null) {
            MsgDataPack pack = MsgDataPack.base();
            pack.setCommand((byte) CommandType.ONLINE_FORWARD_APP.getValue());
            pack.setRes((byte) MsgDataPack.RES_COMMAND);
            pack.setClientId(msg.getClientId());
            ByteBuffer body = ByteBuffer.allocate(10);
            body.put(pack.getTimestamp());
            body.put((byte) 0x02);
            pack.setBodyLen(body.capacity());
            pack.setBody(body.array());
            pack.calcCheckNum();
            msg.setMsgType(new MsgType(pack.getCommand(), MsgDataPack.RES_COMMAND));
            msg.setTopic(ServiceType.AVP_CORE_FORWARD.name());
            msg.setPack(pack);
            return msg;
        }
        msg.setSessionId(sessionId);
        msg.setTopic(ServiceType.AVP_CORE_FORWARD.name());
        MsgDataPack pack = msg.getPack();
        pack.setCommand((byte) command);
        pack.calcCheckNum();
        return msg;
    }

    /**
     * 请求对应相应代码
     * @param type
     * @return
     */
    private CommandType resCmd(CommandType type){
        switch (type){
            case APP_LOGIN:
                return CommandType.LOGIN_FORWARD_APP;
            case APP_LOGOUT:
                return CommandType.LOGOUT_FORWARD_APP;
            case VEHICLE_LOGIN:
                return CommandType.LOGIN_FORWARD_VEHICLE;
            case VEHICLE_LOGOUT:
                return CommandType.LOGOUT_FORWARD_VEHICLE;
        }
        return null;
    }



    @Override
    public void run(String... args) throws Exception {
        log.info("core service setup");
    }


}
