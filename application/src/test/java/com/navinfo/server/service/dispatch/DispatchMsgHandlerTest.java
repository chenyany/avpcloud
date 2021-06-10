package com.navinfo.server.service.dispatch;

import com.navinfo.server.common.msg.MsgDataPack;
import com.navinfo.server.common.msg.MsgType;
import com.navinfo.server.common.msg.QueueMsg;
import com.navinfo.server.common.msg.utils.TimeUtils;

import java.nio.ByteBuffer;
import java.util.UUID;

public class DispatchMsgHandlerTest {

    public static void main(String[] args) {
        byte version = 2;
        byte cmd = 1;
        byte res = (byte) 0xFE;
        byte crypto = 1;

        ByteBuffer buffer = ByteBuffer.allocate(10);
        buffer.put(TimeUtils.time2Bytes(System.currentTimeMillis()));
        buffer.put((byte)1);
        MsgDataPack pack = MsgDataPack.builder()
                .start("##")
                .version(version)
                .command(cmd)
                .res(res)
                .clientId("12345678901234567")
                .crypto(crypto)
                .bodyLen(10)
                .body(buffer.array())
                .checkNum((byte)0)
                .build();
        pack.calcCheckNum();

        QueueMsg msg = QueueMsg.builder()
                .id(UUID.randomUUID())
                .clientId("12345678901234567")
                .sessionId("session0")
                .msgType(new MsgType(pack.getCommand(), pack.getRes()))
                .topic("test")
                .pack(pack)
                .build();

        MsgDataPack resPack = MsgDataPack.fromBytes(msg.getPack().toBytes());
        resPack.setRes((byte) MsgDataPack.RES_FAILED);
        resPack.setBodyLen(9);
        resPack.setBody(TimeUtils.time2Bytes(System.currentTimeMillis()));
        resPack.calcCheckNum();

        System.out.println(msg.getPack().getBodyLen());



    }


}
