package com.navinfo.server.common.msg.utils;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoField;

public class TimeUtils {


    /**
     * 毫秒转数组
     *
     * @param ts
     * @return
     */
    public static byte[] time2Bytes(long ts) {
        Instant instant = Instant.ofEpochMilli(ts);
        ZoneId zone = ZoneId.systemDefault(); //UTC 是否需要时区
        LocalDateTime time = LocalDateTime.ofInstant(instant, zone);
        ByteBuffer bytes = ByteBuffer.allocate(9);
        long milli = time.getLong(ChronoField.MILLI_OF_SECOND);
        bytes.putChar((char) time.getYear());
        bytes.put((byte) time.getMonthValue());
        bytes.put((byte) time.getDayOfMonth());
        bytes.put((byte) time.getHour());
        bytes.put((byte) time.getMinute());
        bytes.put((byte) time.getSecond());
        bytes.putChar((char) milli);
        return bytes.array();
    }

    /**
     * 数组转时间戳
     * @param bytes
     * @return
     */
    public static long bytes2Time(byte[] bytes) {
        ByteBuffer buf = ByteBuffer.allocate(9);
        buf.put(bytes);
        buf.flip();
        int year = buf.getChar();
        int month = buf.get();
        int day = buf.get();
        int hour = buf.get();
        int minute = buf.get();
        int second = buf.get();
        int milli = buf.getChar();
        int nano = milli * 1000000;
        LocalDateTime time = LocalDateTime.of(year, month, day, hour, minute, second, nano);
        ZoneId zone = ZoneId.systemDefault();
        Instant instant = time.atZone(zone).toInstant();
        return instant.toEpochMilli();
    }

    /**
     * 当前时间
     * @return
     */
    public static byte[] current() {
        return time2Bytes(System.currentTimeMillis());
    }

}
