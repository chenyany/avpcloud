package com.navinfo.server.gateway;

import com.navinfo.server.common.msg.QueueMsg;
import lombok.extern.slf4j.Slf4j;

import javax.websocket.Session;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 管理客户端连接
 */
@Slf4j
public class SessionManager {

    private static SessionManager instance;

    private final ConcurrentHashMap<String, Session> storage;

    private SessionManager() {
        this.storage = new ConcurrentHashMap<>();
    }

    public static SessionManager getInstance() {
        synchronized (SessionManager.class){
            if (instance == null) {
                synchronized (SessionManager.class) {
                    if (instance == null) {
                        instance = new SessionManager();
                    }
                }
            }
        }
        return instance;
    }

    /**
     * 添加session
     * @param key
     * @param session
     */
    public void add(String key, Session session){
        //重复连接 替换原值
        storage.put(key, session);
    }


    /**
     * 获取session
     * @param key
     * @return
     */
    public Session get(String key){
        return storage.get(key);
    }

    /**
     * 根据客户端id删除session
     * @param key
     */
    public void remove(String key){
        storage.remove(key);
    }

    /**
     * 删除session
     * @param session
     */
    public void remove(Session session){
        Collection sessions = storage.values();
        if (sessions.contains(session)){
            sessions.remove(session);
        }
    }

    public void close(Session session){
        if(session != null) {
            try {
                session.close();
            } catch (IOException e) {
                log.error("session close failed");
            }
        }
    }


    public void send(QueueMsg msg, String sessionId){

    }

}
