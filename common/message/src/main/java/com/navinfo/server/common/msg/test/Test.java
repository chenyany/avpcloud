package com.navinfo.server.common.msg.test;

import com.navinfo.server.common.msg.MsgType;
import com.navinfo.server.common.msg.QueueMsg;

import java.nio.ByteBuffer;
import java.util.UUID;

public class Test {
    public static void main(String[] args) {
       /* byte[] time = new byte[]{21,4,29,19,58,10};
        byte[] clientId = "LBA1234567".getBytes();
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.put(time);
        buffer.put(clientId);

        byte cmd = 0x03;
        MsgType msgType = new MsgType(cmd, 01);
        QueueMsg queueMsg = QueueMsg.builder().id(UUID.randomUUID())
                .msgType(msgType)
                .body(buffer).build();*/


        ByteBuffer buffer = ByteBuffer.allocate(10);
        buffer.put((byte) 1);
        buffer.put((byte) 1);
        buffer.put((byte) 1);
        buffer.put((byte) 2);
        buffer.put((byte) 3);
        buffer.put((byte) 4);
        buffer.put((byte) 5);
        buffer.put((byte) 6);
        buffer.put((byte) 7);
        buffer.put((byte) 9);

    }
}
