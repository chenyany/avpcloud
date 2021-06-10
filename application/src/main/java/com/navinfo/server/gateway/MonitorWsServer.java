package com.navinfo.server.gateway;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.navinfo.server.common.msg.utils.AvpThreadFactory;
import com.navinfo.server.service.SystemMonitorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 监控websocket
 */
@Slf4j
@Component
@ServerEndpoint("/monitor")
public class MonitorWsServer {


    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(AvpThreadFactory.forName("AVP_MONITOR"));

    private final ConcurrentHashMap<String, Session> sessionMap = new ConcurrentHashMap<>();

    private static SystemMonitorService systemMonitorService;

    @Autowired
    public void setSystemMonitorService(SystemMonitorService systemMonitorService) {
        MonitorWsServer.systemMonitorService = systemMonitorService;
    }

    @OnOpen
    public void onOpen(Session session){
        String sessionId = session.getId();
        sessionMap.put(sessionId, session);
        log.info("a client connect, sessionId:" + sessionId);
    }

    @OnMessage
    public void onMessage(String text, Session session){
        JSONObject msg = JSONUtil.parseObj(text);
        if(1 == msg.getInt("start")){
            executorService.scheduleWithFixedDelay(() -> {
                try {
                    Map<String,Integer> data = systemMonitorService.topicMessageCount();
                    JSONObject status = JSONUtil.parseObj(data);
                    /*status.set("AVP_CORE", RandomUtil.randomInt(0, 100));
                    status.set("AVP_CORE_FORWARD", RandomUtil.randomInt(0,100));
                    if(status.size() > 2){
                        status.set("AVP_DISPATCH", RandomUtil.randomInt(0,100));
                        status.set("AVP_DISPATCH_FORWARD", RandomUtil.randomInt(0,100));
                    }*/
                    sessionMap.get(session.getId()).getBasicRemote().sendText(status.toString());
                } catch (Exception e) {
                    log.info("monitor error", e);
                }
            },5,1, TimeUnit.SECONDS);
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason reason){
        log.info("close reason: " + reason.getReasonPhrase());
        sessionMap.remove(session.getId());
        executorService.shutdown();
    }

    @OnError
    public void onError(Session session, Throwable e){
        log.error("error ", e);
    }

}
