package com.navinfo.server.common.msg.body;

import com.navinfo.server.common.msg.utils.TimeUtils;
import lombok.Data;

import java.nio.ByteBuffer;

/**
 * 数据包父类
 */
@Data
public class DataBody {
    //数据采集时间
    protected long ts;

    public static DataBody fromBytes(ByteBuffer buf){
        byte[] time = new byte[9];
        buf.get(time);
        DataBody instance = new DataBody();
        instance.setTs(TimeUtils.bytes2Time(time));
        return instance;
    }

}
