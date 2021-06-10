package com.navinfo.server.queue.memory;


import com.navinfo.server.common.msg.QueueMsg;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 基于内存的消息队列
 * chenyy
 */
@Slf4j
public class InMemoryStorage {

    private static InMemoryStorage instance;

    private final ConcurrentHashMap<String, BlockingQueue<QueueMsg>> storage;

    private InMemoryStorage() {
        this.storage = new ConcurrentHashMap<>();
    }


    public static InMemoryStorage getInstance() {
        if (instance == null) {
            synchronized (InMemoryStorage.class) {
                if (instance == null) {
                    instance = new InMemoryStorage();
                }
            }
        }
        return instance;
    }

    public void printStats() {
        storage.forEach((topic, queue) -> {
            if (queue.size() > 0) {
                log.info("storage : " + System.identityHashCode(storage));
                log.info("[{}] Queue Size [{}]", topic, queue.size());
            }
        });
    }

    public Map<String, Integer> status(){
        Map<String,Integer> status = new HashMap<>();
        storage.forEach((t, queue) -> {
            status.put(t, queue.size());
        });
        return status;
    }


    public boolean put(String topic, QueueMsg msg) {
        return storage.computeIfAbsent(topic, (t) -> new LinkedBlockingQueue<>()).add(msg);
    }

    public <T extends QueueMsg> List<T> get(String topic) throws InterruptedException {
        if (storage.containsKey(topic)) {
            List<T> entities;
            T first = (T) storage.get(topic).poll();
            if (first != null) {
                entities = new ArrayList<>();
                entities.add(first);
                List<QueueMsg> otherList = new ArrayList<>();
                storage.get(topic).drainTo(otherList, 999);
                for (QueueMsg other : otherList) {
                    entities.add((T) other);
                }
            } else {
                entities = Collections.emptyList();
            }
            return entities;
        }
        return Collections.emptyList();
    }


}
