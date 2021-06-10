package com.navinfo.server.queue;

import com.navinfo.server.common.msg.QueueMsg;

import java.util.List;


/**
 * 队列消费者
 * @param <T>
 */
public interface QueueConsumer<T extends QueueMsg> {

    /**
     * 获取主题
     * @return
     */
    String getTopic();

    /**
     * 从队列中拉取数据
     * @param durationInMillis
     * @return
     */
    List<T> poll(long durationInMillis);

    void status(String topic);
}