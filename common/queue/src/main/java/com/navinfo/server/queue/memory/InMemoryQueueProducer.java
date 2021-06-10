package com.navinfo.server.queue.memory;

import com.navinfo.server.common.msg.QueueMsg;
import com.navinfo.server.queue.QueueProducer;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * 消息生产者
 * 向队列发送响应数据
 * chenyy
 */
@Slf4j
@Data
public class InMemoryQueueProducer<T extends QueueMsg> implements QueueProducer<T> {

    private final InMemoryStorage storage = InMemoryStorage.getInstance();

    @Override
    public void send(String topic, T msg) {
        storage.put(topic, msg);
//        storage.printStats();
//        System.out.println("2 消息已发送");
    }
}
