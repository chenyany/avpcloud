package com.navinfo.server.common.msg.body;

import com.navinfo.server.common.msg.utils.TimeUtils;
import lombok.Data;

import java.nio.ByteBuffer;

@Data
public class MapManagerReq extends DataBody {

    private String areaId;

    public static MapManagerReq fromBytes(ByteBuffer buffer) {
        buffer.flip();
        byte[] time = new byte[9];
        buffer.get(time);
        byte[] areaId = new byte[20];
        buffer.get(areaId);
        MapManagerReq instance = new MapManagerReq();
        instance.setTs(TimeUtils.bytes2Time(time));
        instance.setAreaId(new String(areaId));
        return instance;
    }


}
