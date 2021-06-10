package com.navinfo.server.service.dispatch;

import com.navinfo.server.DispatchService;
import com.navinfo.server.common.msg.*;
import com.navinfo.server.queue.QueueConsumer;
import com.navinfo.server.queue.memory.InMemoryQueueConsumer;
import com.navinfo.server.utils.AvpThreadFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
public class DispatchMsgHandler implements CommandLineRunner {

    @Autowired
    private DispatchService dispatchService;
    private final ExecutorService processExecutor = Executors.newFixedThreadPool(3, AvpThreadFactory.forName("AVP_DISPATCH_PROCESS"));
    private final ExecutorService disPatchConsumerExecutor = Executors.newSingleThreadExecutor(AvpThreadFactory.forName("AVP_DISPATCH_REQ"));
    private QueueConsumer dispatchDataConsumer;


    @Override
    public void run(String... args) throws Exception {
        log.info("dispatch service setup");
    }

    @PostConstruct
    public void init() {
        dispatchDataConsumer = new InMemoryQueueConsumer<>(ServiceType.AVP_DISPATCH.name());
        disPatchConsumerExecutor.execute(() -> {
            while (true) {
                List<QueueMsg> records = dispatchDataConsumer.poll(500);
                if (records.size() == 0) {
                    continue;
                }
                records.forEach(record -> {
                    try {
                        processExecutor.execute(() -> process(record));
                    } catch (Throwable e) {
                        log.error("dispatch process error", e);
                    }
                });
            }
        });
        log.info("dispatch service init");
    }

    /**
     * 消息处理
     *
     * @param record
     */
    private void process(QueueMsg record) {
//        dispatchService.sendCommonResponse(record);
        MsgType msgType = record.getMsgType();
        //调度应答消息
        if (Byte.toUnsignedInt((byte) msgType.getRes()) != MsgDataPack.RES_COMMAND) {
            dispatchService.processRes(record);
            return;
        }
//        System.out.println(msgType.getCommandType().name());
        //命令消息
        switch (msgType.getCommandType()) {
            case APP_DISPATCH_PARKING:
                dispatchService.processParking(record);
                break;
            case APP_DISPATCH_ACTION:
                dispatchService.processAction(record);
                break;
            case APP_DISPATCH_PAUSE:
                dispatchService.processPause(record);
                break;
            case APP_DISPATCH_RECOVER:
                dispatchService.processRecover(record);
                break;
            case APP_DISPATCH_STOP:
                dispatchService.processStop(record);
                break;
        }
    }



}
