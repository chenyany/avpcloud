package com.navinfo.server.service.impl;


import java.nio.ByteBuffer;

public class TestProto {
    public static void main(String[] args) {
        int[] data = {129,144,18,52,86,120,35,35,115,101,110,100,115,117,99,99,101,115,115,102,117,108};
        for (int b: data) {
            String str = Integer.toBinaryString(b);
            for(int i=0;i< 8 - str.length(); i++){
                System.out.print("0");
            }
            System.out.println(str);
        }
        //18,52,86,120
        byte[] bytes = {120, 86, 52, 18};
        ByteBuffer mask_key = ByteBuffer.allocate(4);
        mask_key.put(bytes);
        mask_key.flip();
        System.out.println(mask_key.getInt());
    }

    //   1 0 0 0 0 0 0 1 1 0 0 1 0 0 0 0
    //   00010010 00110100 01010110 01111000

    /*
         0                   1                   2                   3
         0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
        +-+-+-+-+-------+-+-------------+-------------------------------+
        |F|R|R|R| opcode|M| Payload len |    Extended payload length    |
        |I|S|S|S|  (4)  |A|     (7)     |             (16/64)           |
        |N|V|V|V|       |S|             |   (if payload len==126/127)   |
        | |1|2|3|       |K|             |                               |
        +-+-+-+-+-------+-+-------------+ - - - - - - - - - - - - - - - +
        |     Extended payload length continued, if payload len == 127  |
        + - - - - - - - - - - - - - - - +-------------------------------+
        |                               |Masking-key, if MASK set to 1  |
        +-------------------------------+-------------------------------+
        | Masking-key (continued)       |          Payload Data         |
        +-------------------------------- - - - - - - - - - - - - - - - +
        :                     Payload Data continued ...                :
        + - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - +
        |                     Payload Data continued ...                |
        +---------------------------------------------------------------+
    */
}
