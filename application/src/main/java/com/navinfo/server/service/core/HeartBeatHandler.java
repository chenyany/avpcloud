package com.navinfo.server.service.core;

import cn.hutool.core.date.DateUtil;
import com.navinfo.server.cache.ClientCache;
import com.navinfo.server.common.msg.*;
import com.navinfo.server.common.msg.body.DataBody;
import com.navinfo.server.common.msg.utils.TimeUtils;
import com.navinfo.server.queue.QueueConsumer;
import com.navinfo.server.queue.QueueProducer;
import com.navinfo.server.queue.memory.InMemoryQueueConsumer;
import com.navinfo.server.queue.memory.InMemoryQueueProducer;
import com.navinfo.server.common.msg.utils.AvpThreadFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

@Slf4j
@Service
public class HeartBeatHandler implements CommandLineRunner {

    @Autowired
    private ClientCache clientCache;

    //在线监测时间间隔
    @Value("${timeout.period: 300}")
    private long CHECK_PERIOD;
    private final long CHECK_DELAY = 10;

    private final ConcurrentHashMap<String, Long> onLineState = new ConcurrentHashMap<>();
    private ExecutorService consumerExecutor;
    private ExecutorService processExecutor;
    private QueueConsumer heartBeatConsumer;
    private QueueProducer heartBeatProducer;
    private ScheduledExecutorService onlineCheckExecutor;


    @PostConstruct
    public void init() {

        onlineCheckExecutor = Executors.newScheduledThreadPool(1, AvpThreadFactory.forName("AVP_ONLINE_CHECK"));
        processExecutor = Executors.newFixedThreadPool(2, AvpThreadFactory.forName("AVP_HEART_BEAT_PROCESS"));
        heartBeatProducer = new InMemoryQueueProducer();
        consumerExecutor = Executors.newSingleThreadExecutor();
        heartBeatConsumer = new InMemoryQueueConsumer(ServiceType.AVP_HEART_BEAT.name());
        consumerExecutor.execute(() -> {
            while (true) {
                List<QueueMsg> messages = heartBeatConsumer.poll(1000);
                if (messages.size() == 0) {
                    continue;
                }
                messages.forEach(msg -> {
                    try {
                        processExecutor.execute(() -> process(msg));
                    } catch (Throwable e) {
                        log.warn("Failed to process the notification.", e);
                    }
                });
            }
        });

        onlineCheckExecutor.scheduleAtFixedRate(this::processOnlineCheck, CHECK_DELAY, CHECK_PERIOD, TimeUnit.SECONDS);
        log.info("heart beat service init");
    }

    /*检查客户端在线状态*/
    private void processOnlineCheck() {
        clientCache.status();
        long now = System.currentTimeMillis();
        onLineState.forEach((clientId,lastTime) -> {
//            log.info(clientId + " : " + lastTime);
            boolean offLine = now - lastTime > CHECK_PERIOD * 1000;
            if(offLine){
                QueueMsg res = QueueMsg.builder()
                        .id(UUID.randomUUID())
                        .msgType(new MsgType((byte) CommandType.VEHICLE_HEART_BEAT.getValue(), MsgDataPack.RES_FAILED))
                        .topic(ServiceType.AVP_HEART_BEAT_FORWARD.name())
                        .clientId(clientId)
                        .sessionId(clientCache.getClientSession(clientId))
                        .build();
                clientCache.removeClient(clientId);
                //关闭session通知
                heartBeatProducer.send(res.getTopic(), res);
                //删除缓存
                onLineState.remove(clientId);
            }
        });
    }

    /*处理回调心跳消息*/
    private void process(QueueMsg msg) {
        long ts = msg.build().getTs();
        CommandType resCmd = resCmd(msg.getMsgType().getCommandType());

        MsgDataPack pack = MsgDataPack.base();
        pack.setCommand((byte) resCmd.getValue());
        pack.setRes((byte) MsgDataPack.RES_COMMAND);
        pack.setClientId(msg.getClientId());
        pack.setBodyLen(pack.getTimestamp().length);
        pack.setBody(pack.getTimestamp());
        pack.calcCheckNum();
        msg.setPack(pack);
        msg.setTopic(ServiceType.AVP_HEART_BEAT_FORWARD.name());
        heartBeatProducer.send(msg.getTopic(), msg);
        onLineState.put(msg.getClientId(), ts); //更新在线时间
    }


    private CommandType resCmd(CommandType type){
        switch (type){
            case APP_HEART_BEAT:
                return CommandType.HEART_BEAT_FORWARD_APP;
            case VEHICLE_HEART_BEAT:
                return CommandType.HEART_BEAT_FORWARD_VEHICLE;
        }
        return null;
    }


    @Override
    public void run(String... args) throws Exception {
        log.info("heart beat service setup");
    }
}
