package com.navinfo.server.common.msg;

import cn.hutool.core.date.DateUtil;
import com.navinfo.server.common.msg.utils.TimeUtils;
import lombok.Builder;
import lombok.Data;

import java.nio.ByteBuffer;

@Data
public class MsgDataPack {

    public final static byte PROTOCOL_VERSION = 0X01;

    public final static int RES_SUCCESS = 0x01;
    public final static int RES_FAILED = 0x02;
    public final static int RES_DATA = 0xFD;
    public final static int RES_COMMAND = 0xFE;
    public final static int RES_NO_LOGIN = 0x03;


    //开始标志##                  0
    private String start;

    //协议版本                    2
    private byte version;

    //请求命令                    3
    private byte command;

    //响应                       4
    private byte res;

    //客户端唯一标识              5
    private String clientId;

    //                          22
    private byte[] timestamp;

    //加密方式                   31
    private byte crypto;

    //消息体长度                 32
    private int bodyLen;

    //消息体
    private byte[] body;

    //校验码 bbc校验             last 1
    private byte checkNum;

    @Builder
    public MsgDataPack(String start, byte version, byte command, byte res, String clientId,  byte[] timestamp, byte crypto, int bodyLen, byte[] body, byte checkNum) {
        this.start = start;
        this.version = version;
        this.command = command;
        this.res = res;
        this.clientId = clientId;
        this.timestamp = timestamp;
        this.crypto = crypto;
        this.bodyLen = bodyLen;
        this.body = body;
        this.checkNum = checkNum;
    }

    public static MsgDataPack fromBytes(ByteBuffer buffer) {
        buffer.rewind();
        byte[] start = new byte[2];
        buffer.get(start);
        byte version = buffer.get();
        byte command = buffer.get();
        byte res = buffer.get();
        byte[] clientId = new byte[17];
        buffer.get(clientId);
        byte[] timestamp = new byte[9];
        buffer.get(timestamp);
        byte crypto = buffer.get();
        int bodyLen = buffer.getInt();
        byte[] body = new byte[bodyLen];
        buffer.get(body);
        byte checkNum = buffer.get();

        MsgDataPack dataPack = MsgDataPack.builder()
                .start(new String(start))
                .version(version)
                .command(command)
                .res(res)
                .clientId(new String(clientId))
                .timestamp(timestamp)
                .crypto(crypto)
                .bodyLen(bodyLen)
                .body(body)
                .checkNum(checkNum)
                .build();
        return dataPack;
    }

    public static MsgDataPack base() {
        return MsgDataPack.builder()
                .start("##")
                .version(PROTOCOL_VERSION)
                .timestamp(TimeUtils.time2Bytes(DateUtil.current()))
                .crypto((byte) 0x01)
                .checkNum((byte) 0)
                .build();
    }

    /**
     * 转为byte
     *
     * @return
     */
    public ByteBuffer toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(37 + bodyLen);
        buffer.put(start.getBytes());
        buffer.put(version);
        buffer.put(command);
        buffer.put(res);
        buffer.put(clientId.getBytes());
        buffer.put(timestamp);
        buffer.put(crypto);
        buffer.putInt(bodyLen);
        buffer.put(body);
        buffer.put(checkNum);
        return buffer;
    }


    /**
     * 异或校验
     *
     * @param
     * @return
     */
    public static boolean xorCheck(ByteBuffer buffer) {
        buffer.rewind();
        byte[] bytes = new byte[buffer.capacity()];
        buffer.get(bytes);
        int checkNum = bytes[bytes.length - 1];
        checkNum = checkNum > 0 ? checkNum : checkNum & 0xff;
        int temp = checkNum(bytes);
//        System.out.println("temp : " + temp + "  checkNum : " + checkNum);
        return temp == checkNum;
    }

    /**
     * 获取校验码
     *
     * @param bytes
     * @return
     */
    public static int checkNum(byte[] bytes) {
        int temp = bytes[3];
        for (int i = 4; i < bytes.length - 1; i++) {
            int iData;
            if (bytes[i] < 0) {
                iData = bytes[i] & 0xff;      // 变为正数计算
            } else {
                iData = bytes[i];
            }
            if (temp < 0) {
                temp = temp & 0xff;          // 变为正数
            }
            temp ^= iData;
        }
        return temp;
    }

    public void calcCheckNum() {
        ByteBuffer buf = toBytes();
        buf.rewind();
        byte[] bytes = new byte[buf.capacity()];
        buf.get(bytes);
        this.checkNum = (byte) checkNum(bytes);
    }


    @Override
    public String toString() {
        return "MsgDataPack{" +
                "start='" + start + '\'' +
                ", version=" + version +
                ", command=0x" + toHex(command) +
                ", res=0x" + toHex(res) +
                ", clientId='" + clientId + '\'' +
                ", crypto=" + Byte.toUnsignedInt(crypto) +
                ", bodyLen=" + bodyLen +
                ", body=bytes[" + body.length + "]" +
                ", checkNum=" + Byte.toUnsignedInt(checkNum) +
                '}';
    }

    private String toHex(byte num){
        return Integer.toHexString(Byte.toUnsignedInt(num));
    }


}
