package com.navinfo.server.utils;

public class PrintUtils {

    public static String toHex(Object value) {
        if (value instanceof Byte) {
            return "0x" + Integer.toHexString(Byte.toUnsignedInt((byte) value));
        } else if (value instanceof Long) {
            return "0x" + Long.toHexString((Long) value);
        } else if (value instanceof Integer) {
            return "0x" + Integer.toHexString((Integer) value);
        } else {
            return "format error";
        }
    }

}
