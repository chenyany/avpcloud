package com.navinfo.server.queue;

import com.navinfo.server.common.msg.QueueMsg;

/**
 * 消息生产者
 * 用于向队列写入响应消息
 */
public interface QueueProducer<T extends QueueMsg> {

    /**
     * 发送消息到队列
     * @param topic
     * @param msg
     */
    void send(String topic, T msg);

}
