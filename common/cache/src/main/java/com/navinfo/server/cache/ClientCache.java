package com.navinfo.server.cache;

import cn.hutool.core.map.BiMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端缓存
 * 缓存app与车辆关系
 * 缓存客户端与sessionId关系
 */
@Slf4j
@Component
public class ClientCache {

    private final int VEHICLE = 0;
    private final int APP = 1;

    private final String NOTFOUND = "NOTFOUND";

    @Autowired
    private Environment env;

    private static final BiMap<String, String> appVehicleCache = new BiMap<>(new ConcurrentHashMap<>());
    //客户端登入时设置
    private static final BiMap<String, String> clientSessionCache = new BiMap<>(new ConcurrentHashMap<>());

    @PostConstruct
    public void init() {

        String[] cache = env.getProperty("client.cache").split(",");
        for(String item : cache) {
            String[] map = item.split(":");
            //加载预定义 app与车辆绑定关系
            appVehicleCache.put(map[0], map[1]);
        }
        System.out.println(appVehicleCache);
    }

    /**
     * 根据appId 获取 车端sessionId
     *
     * @return
     */
    public String getVehicleSession(String appId) {
        String vin = Optional.ofNullable(appVehicleCache.get(appId)).orElse(NOTFOUND);
        return clientSessionCache.get(vin);
    }

    /**
     * 根据vin获取 app端session
     * @param vin
     * @return
     */
    public String getAppSession(String vin) {
        String appId = Optional.ofNullable(appVehicleCache.getKey(vin)).orElse(NOTFOUND);
        return clientSessionCache.get(appId);
    }

    /**
     * 获取客户端session
     * @param clientId
     * @return
     */
    public String getClientSession(String clientId){
        return clientSessionCache.get(clientId);
    }

    /**
     * 添加客户端和session映射
     * @param clientId
     * @param sessionId
     */
    public void setClientSession(String clientId, String sessionId) {
        clientSessionCache.put(clientId, sessionId);
    }


    /**
     * 测试用 监控状态
     */
    public void status() {
        log.info("client online : " + clientSessionCache.size());
        clientSessionCache.forEach((k, v) -> {
            log.info("client id " + k + ", session id " + v);
        });
    }

    /**
     * 根据client删除客户端缓存
     * @param clientId
     */
    public void removeClient(String clientId) {
        clientSessionCache.remove(clientId);
        status();
    }

    /**
     * 根据sessionId 删除客户端缓存
     * @param sessionId
     */
    public void removeSession(String sessionId) {
        String clientId = clientSessionCache.getKey(sessionId);
        if(clientId != null){
            removeClient(clientId);
        }
    }

    /**
     * 客户端是否存在
     * @param clientId
     * @return
     */
    public boolean exist(String clientId) {
        return clientSessionCache.containsKey(clientId);
    }

    /**
     * 是否为合法客户端
     * @param clientId
     */
    public boolean validateClient(String clientId){
        return appVehicleCache.containsKey(clientId)
                || appVehicleCache.containsValue(clientId);
    }
}
