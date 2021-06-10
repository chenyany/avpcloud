package com.navinfo.server.common.msg;

import com.navinfo.server.common.msg.body.DataBody;
import lombok.Builder;
import lombok.Data;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * 队列消息
 */
@Data
public class QueueMsg<T extends DataBody> {
    //队列消息id
    private UUID id;
    private String sessionId;
    private String clientId;
    private String topic;
    private MsgType msgType;
    //原始报文
    private MsgDataPack pack;

    @Builder
    public QueueMsg(UUID id, String sessionId, String clientId, String topic, MsgType msgType, MsgDataPack pack) {
        this.id = id;
        this.sessionId = sessionId;
        this.clientId = clientId;
        this.topic = topic;
        this.msgType = msgType;
        this.pack = pack;
    }


    public QueueMsg(QueueMsg msg) {
        this.id = msg.getId();
        this.sessionId = msg.getSessionId();
        this.clientId = msg.getClientId();
        this.topic = msg.getTopic();
        this.msgType = msg.msgType;
        this.pack = msg.getPack();
    }

    /**
     * 数据内容转换
     *
     * @return
     */
    public T build() {
        ByteBuffer body = ByteBuffer.wrap(pack.getBody());
        return (T) msgType.buildBody(body);
    }
}
