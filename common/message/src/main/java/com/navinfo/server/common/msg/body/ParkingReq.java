package com.navinfo.server.common.msg.body;

import com.navinfo.server.common.msg.utils.TimeUtils;
import lombok.Data;

import java.nio.ByteBuffer;

@Data
public class ParkingReq extends DataBody {
    //0x01：自动寻找车位，0x02：用户指定车位
    private byte type;
    //经度
    private long lng;
    //纬度
    private long lat;

    public static ParkingReq fromBytes(ByteBuffer buffer) {
        byte[] time = new byte[9];
        buffer.get(time);
        ParkingReq instance = new ParkingReq();
        instance.setTs(TimeUtils.bytes2Time(time));
        instance.setType(buffer.get());
        instance.setLng(buffer.getLong());
        instance.setLat(buffer.getLong());
        return instance;
    }
}
