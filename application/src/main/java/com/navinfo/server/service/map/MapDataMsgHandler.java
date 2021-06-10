package com.navinfo.server.service.map;

import com.navinfo.server.common.msg.CommandType;
import com.navinfo.server.common.msg.QueueMsg;
import com.navinfo.server.common.msg.ServiceType;
import com.navinfo.server.queue.QueueConsumer;
import com.navinfo.server.queue.memory.InMemoryQueueConsumer;
import com.navinfo.server.service.impl.MapDataService;
import com.navinfo.server.common.msg.utils.AvpThreadFactory;
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
public class MapDataMsgHandler implements CommandLineRunner {

    @Autowired
    private MapDataService mapDataService;

    private QueueConsumer mapDataConsumer;

    private final ExecutorService mapConsumerExecutor = Executors.newSingleThreadExecutor(AvpThreadFactory.forName("AVP_MAP_REQ"));
    private final ExecutorService mapServiceExecutor = Executors.newFixedThreadPool(5,AvpThreadFactory.forName("AVP_MAP_DATA"));


    @PostConstruct
    public void init() {
        mapDataConsumer = new InMemoryQueueConsumer<>(ServiceType.AVP_DATA.name());
        mapConsumerExecutor.execute(() -> {
            while (true) {
                List<QueueMsg> records = mapDataConsumer.poll(500);
                if (records.size() == 0) {
                    continue;
                }
                records.forEach(record -> {
                    try {
                        process(record);
                    } catch (Exception e) {
                        log.warn("Failed to process the notification.", e);
                    }
                });
            }
        });
        log.info("map service init");
    }

    private void process(QueueMsg msg) {
        CommandType cmdType = msg.getMsgType().getCommandType();
        if (cmdType == CommandType.VEHICLE_MAP_DOWNLOAD || cmdType == CommandType.APP_MAP_DOWNLOAD) {
            mapServiceExecutor.execute(() -> mapDataService.downloadMap(msg));

        } else if (cmdType == CommandType.VEHICLE_ROUTE_PLANNING) {
            mapServiceExecutor.execute(() -> mapDataService.routePlanning(msg));

        } else if( cmdType == CommandType.VEHICLE_MAP_MANAGER_DOWNLOAD){
            mapServiceExecutor.execute(() -> mapDataService.downloadMapManager(msg));
        }
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("map service setup");
    }

}
