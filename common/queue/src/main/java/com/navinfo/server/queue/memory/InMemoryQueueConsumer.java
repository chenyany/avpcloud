package com.navinfo.server.queue.memory;

import com.navinfo.server.common.msg.QueueMsg;
import com.navinfo.server.queue.QueueConsumer;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;

/**
 * 基于内存队列的消费者
 */
@Slf4j
public class InMemoryQueueConsumer<T extends QueueMsg> implements QueueConsumer<T> {

    private final InMemoryStorage storage = InMemoryStorage.getInstance();

    private final String topic;

    public InMemoryQueueConsumer(String topic) {
        this.topic = topic;
    }

    public void status(String topic){
        storage.printStats();
    }

    @Override
    public String getTopic() {
        return topic;
    }

    @Override
    public List<T> poll(long durationInMillis) {
        List<T> messages = null;
        try {
            messages = storage.get(topic);
        } catch (InterruptedException e) {
            messages = Collections.emptyList();
            log.info("error", e);
        }

        if (messages.size() > 0) {
            return messages;
        }

        try {
            Thread.sleep(durationInMillis);
        } catch (InterruptedException e) {
            log.info("sleep failed...............");
        }
        return Collections.emptyList();
    }

}
