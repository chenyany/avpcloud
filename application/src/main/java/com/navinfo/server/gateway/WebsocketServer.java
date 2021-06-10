package com.navinfo.server.gateway;

import com.navinfo.server.common.msg.MsgDataPack;
import com.navinfo.server.service.TransportService;
import com.navinfo.server.common.msg.utils.AvpThreadFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * websocket服务
 * chenyy
 */
@Slf4j
@Component
@ServerEndpoint("/avp")
public class WebsocketServer {

    private int binaryMessageMaxSize = 100 * 1024 * 1024;;

    private static SessionManager sessionManager;

    private static TransportService transportService;

    private final ExecutorService transportExec = Executors.newFixedThreadPool(5, AvpThreadFactory.forName("AVP_TRANSPORT_PROCESS"));

    @Autowired
    public void setTransportService(TransportService transportService) {
        WebsocketServer.transportService = transportService;
    }

    @PostConstruct
    public void init(){
        log.info("websocket server init");
        WebsocketServer.sessionManager = SessionManager.getInstance();
    }


    /**
     * 开启连接
     * @param session
     */
    @OnOpen
    public void onOpen(Session session){
        session.setMaxBinaryMessageBufferSize(binaryMessageMaxSize);
        String sessionId = session.getId();
        sessionManager.add(sessionId, session);
        log.info("a client connect, sessionId:" + sessionId);
    }


    /**
     * 接收消息
     * @param msg
     */
//    @OnMessage
    public void onMessage(ByteBuffer msg, Session session){
        transportExec.execute(()-> {
                WebsocketServer.transportService.process(msg, session.getId());
        });


    }

    @OnMessage
    public void onMessage(byte[] msg, Session session){
        transportExec.execute(()-> {
            try {
                WebsocketServer.transportService.process(ByteBuffer.wrap(msg), session.getId());
            }catch (Exception e){
                log.error("process msg error", e);
                StringBuffer buf = new StringBuffer("[");
                for(byte b : msg){
                    buf.append(b).append(",");
                }
                buf.append("]");
                log.error("error bytes : " + buf.toString());
                MsgDataPack pack = MsgDataPack.fromBytes(ByteBuffer.wrap(msg));
                log.error("error pack : "  + pack.toString());
            }
        });
    }

    /**
     * 连接关闭时
     */
    @OnClose
    public void onClose(Session session, CloseReason reason){
        log.info("close reason: " + reason.getReasonPhrase());
        log.info("a client close, sessionId:" + session.getId());
        sessionManager.remove(session.getId());
        WebsocketServer.transportService.processClose(session.getId());
        transportExec.shutdown();
    }

    @OnError
    public void onError(Session session, Throwable e){
        log.error("error ", e);
    }



}
