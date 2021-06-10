package com.navinfo.server.common.msg;

public enum ServiceType {

    AVP_CORE, //登录，数据上传等请求队列
    AVP_CORE_FORWARD, //响应队列
    AVP_DISPATCH, //调度请求队列
    AVP_DISPATCH_FORWARD, //调度响应队列
    AVP_DATA, //数据请求队列
    AVP_DATA_FORWARD, //数据响应队列
    AVP_HEART_BEAT, //心跳请求
    AVP_HEART_BEAT_FORWARD; //心跳响应

    public static ServiceType of(String serviceType) {
        return ServiceType.valueOf(serviceType.replace("-", "_").toUpperCase());
    }

}
