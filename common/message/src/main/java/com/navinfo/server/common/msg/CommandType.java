package com.navinfo.server.common.msg;

import com.navinfo.server.common.msg.body.DataBody;

import java.nio.ByteBuffer;

/**
 * 命令类型
 */
public enum CommandType {

    VEHICLE_LOGIN(0x01, "车辆登入"),
    VEHICLE_LOGOUT(0x02, "车辆登出"),
    VEHICLE_MAP_DOWNLOAD(0x03, "车端请求下载矢量地图数据"),
    VEHICLE_ROUTE_PLANNING(0x04, "请求在线规划"),
    VEHICLE_REALTIME_UPLOAD(0x05, "上报实时信息"),
    VEHICLE_FAULT_UPLOAD(0x06, "上报故障信息"),
    VEHICLE_WARNING_UPLOAD(0x07, "上报报警信息"),
    VEHICLE_MAP_MANAGER_DOWNLOAD(0x08, "请求下载矢量地图数据管理文件"),
    VEHICLE_POSITION_UPLOAD(0x09, "上报车辆位置"),
    VEHICLE_HEART_BEAT(0x30, "车端心跳上报"),
    APP_LOGIN(0x40, "手机登入"),
    APP_LOGOUT(0x41, "手机登出"),
    APP_DISPATCH_PARKING(0x42, "一键泊车"),
    APP_DISPATCH_ACTION(0x43, "一键取车"),
    APP_DISPATCH_PAUSE(0x44, "一键暂停"),
    APP_DISPATCH_RECOVER(0x45, "一键恢复"),
    APP_DISPATCH_STOP(0x46, "一键终止"),
    APP_MAP_DOWNLOAD(0x47, "手机请求下载矢量地图数据"),
    APP_HEART_BEAT(0x70, "手机心跳上报"),
    MAP_FORWARD_VEHICLE(0x80, "下发矢量地图数据到车端"),
    ROUTE_PLANNING_FORWARD(0x81, "下发规划结果"),
    PARKING_FORWARD(0x82, "下发一键泊车指令"),
    ACTION_FORWARD(0x83, "下发一键取车指令"),
    PAUSE_FORWARD(0x84, "下发一键暂停指令"),
    RECOVER_FORWARD(0x85, "下发一键恢复指令"),
    STOP_FORWARD(0x86, "下发一键终止指令"),
    MAP_MANAGER_FORWARD_VEHICLE(0x87, "下发矢量地图数据管理文件"),
    LOGIN_FORWARD_VEHICLE(0x88, "下发车辆登入结果"),
    LOGOUT_FORWARD_VEHICLE(0x89, "下发车辆登出结果"),
    HEART_BEAT_FORWARD_VEHICLE(0x8A, "下发云端心跳"),
    ONLINE_FORWARD_APP(0x8B, "下发手机端连接状态"),
    MAP_FORWARD_APP(0xC0, "下发矢量地图数据到手机"),
    REALTIME_FORWARD(0xC1, "下发车辆实时信息"),
    FAULT_FORWARD(0xC2, "下发车辆故障信息"),
    WARNING_FORWARD(0xC3, "下发车辆报警信息"),
    ONLINE_FORWARD_VEHICLE(0xC4, "下发车载终端连接状态"),
    LOGIN_FORWARD_APP(0xC5,"	下发手机登入结果"),
    LOGOUT_FORWARD_APP(0xC6,"下发手机登出结果"),
    HEART_BEAT_FORWARD_APP(0xC7, "下发云端心跳"),
    POSITION_FORWARD_APP(0xC8, "下发车辆位置信息");

    private int value;
    private String desc;

    CommandType(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public int getValue() {
        return value;
    }

    public String getDesc() {
        return desc;
    }

    public DataBody build(ByteBuffer buffer) {
        return DataBody.fromBytes(buffer);
    }

}
