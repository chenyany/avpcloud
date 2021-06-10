package com.navinfo.server.common.msg.body;

import com.navinfo.server.common.msg.utils.TimeUtils;
import lombok.Data;

import java.nio.ByteBuffer;

@Data
public class MapReq extends DataBody{

    private int parkingId;

    public static MapReq fromBytes(ByteBuffer buffer){
        buffer.flip();
        byte[] time = new byte[9];
        buffer.get(time);
        MapReq instance = new MapReq();
        instance.setTs(TimeUtils.bytes2Time(time));
        instance.setParkingId(buffer.getInt());
        return instance;
    }

}
