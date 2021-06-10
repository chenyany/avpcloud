package com.navinfo.server.service;

import com.navinfo.server.queue.memory.InMemoryStorage;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 系统监控服务
 */
@Service
public class SystemMonitorService {

    private final InMemoryStorage storage = InMemoryStorage.getInstance();

    /**
     * 消息队列中消息数量
     * @return
     */
    public Map<String,Integer> topicMessageCount(){
        return storage.status();
    }

}
