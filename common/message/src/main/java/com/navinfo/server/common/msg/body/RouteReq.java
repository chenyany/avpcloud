package com.navinfo.server.common.msg.body;

import com.navinfo.server.common.msg.utils.TimeUtils;
import lombok.Data;

import java.nio.ByteBuffer;

@Data
public class RouteReq extends DataBody {

    //规划类型
    private byte planingType;
    //起点经度
    private long lngBegin;
    //起点纬度
    private long latBegin;
    //终点经度
    private long lngEnd;
    //终点纬度
    private long latEnd;
    //偏航角
    private int heading;
    //停车场ID
    private int depotId;

    public static DataBody fromBytes(ByteBuffer buffer) {
        byte[] time = new byte[9];
        buffer.get(time);
        RouteReq obj = new RouteReq();
        obj.setTs(TimeUtils.bytes2Time(time));
        obj.setPlaningType(buffer.get());
        obj.setLngBegin(buffer.getLong());
        obj.setLatBegin(buffer.getLong());
        obj.setLngEnd(buffer.getLong());
        obj.setLatEnd(buffer.getLong());
        obj.setHeading(buffer.getInt());
        obj.setDepotId(buffer.getInt());
        return obj;
    }

    public ByteBuffer toBytes() {
        ByteBuffer buf = ByteBuffer.allocate(32);
        buf.put(TimeUtils.time2Bytes(ts));
        buf.put(planingType);
        buf.putLong(lngBegin);
        buf.putLong(latBegin);
        buf.putLong(lngEnd);
        buf.putLong(latEnd);
        buf.putInt(heading);
        buf.putInt(depotId);
        buf.flip();
        return buf;
    }
}
