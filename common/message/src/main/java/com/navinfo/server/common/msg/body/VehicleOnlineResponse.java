package com.navinfo.server.common.msg.body;

import com.navinfo.server.common.msg.utils.TimeUtils;
import lombok.Data;

import java.nio.ByteBuffer;

@Data
public class VehicleOnlineResponse extends DataBody {

    private byte onlineState;

    public ByteBuffer toBytes(){
        ByteBuffer buffer = ByteBuffer.allocate(10);
        buffer.put(TimeUtils.time2Bytes(ts));
        buffer.put(onlineState);
        buffer.flip();
        return buffer;
    }

}
