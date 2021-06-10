package com.navinfo.server.common.msg;

import com.navinfo.server.common.msg.body.DataBody;
import lombok.Data;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

@Data
public class MsgType {

    private static final Map<Integer, CommandType> commandMap = new HashMap<>();
    //上行
    public static final int DIRECTION_UP = 0;
    //下行
    public static final int DIRECTION_DOWN = 1;
    //消息类型枚举
    private CommandType commandType;
    //响应类型
    private int res;

    //TODO   枚举类型与命令类型 数量 对应
    static {
        int[] commands = new int[]{
                                0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x30,
                                0x40, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47, 0x70,
                                0x80, 0x81, 0x82, 0x83, 0x84, 0x85, 0x86, 0x87, 0x88, 0x89, 0x8A, 0x8B,
                                0xC0, 0xC1, 0xC2, 0xC3, 0xC4, 0xC5, 0xC6, 0xC7, 0xC8
        };
        CommandType[] types = CommandType.values();
        for (int i = 0; i < commands.length; i++) {
            try {
                commandMap.put(commands[i], types[i]);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public MsgType(byte cmd, int res) {
        int key = Byte.toUnsignedInt(cmd);
        this.commandType = commandMap.get(key);
        this.res = res;
    }

    public DataBody buildBody(ByteBuffer body) {
        return commandType.build(body);
    }
}
