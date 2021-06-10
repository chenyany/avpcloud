package com.navinfo.server.common.msg.body;

import com.navinfo.server.common.msg.utils.TimeUtils;
import lombok.Data;

import java.nio.ByteBuffer;

@Data
public class ActionReq extends DataBody {
    //经度
    private int lng;
    //纬度
    private int lat;

    public static ActionReq fromBytes(ByteBuffer buffer) {
        buffer.flip();
        byte[] time = new byte[9];
        buffer.get(time);
        ActionReq instance = new ActionReq();
        instance.setTs(TimeUtils.bytes2Time(time));
        instance.setLng(buffer.getInt());
        instance.setLat(buffer.getInt());
        return instance;
    }
}
