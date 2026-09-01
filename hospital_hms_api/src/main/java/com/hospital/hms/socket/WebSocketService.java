package com.hospital.hms.socket;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@ServerEndpoint(value = "/socket")
@Component
public class WebSocketService {

    //出于线程安全的考量，这里采用ConcurrentHashMap
    public static ConcurrentHashMap<String, Session> sessionMap= new ConcurrentHashMap<>();


    //创建连接之后执行的回调函数
    @OnOpen
    public void onOpen(Session session) {

    }


    //关闭连接之后执行的回调函数
    @OnClose
    public void onClose(Session session) {
        Map map=session.getUserProperties();
        if(map.containsKey("userId")){
            String userId= MapUtil.getStr(map,"userId");
            sessionMap.remove(userId);
        }
    }


    //接收消息后执行的回调函数
    @OnMessage
    public void onMessage(String message, Session session) {
        //把接收到的字符串消息解析成JSON对象
        JSONObject json= JSONUtil.parseObj(message);

        //opt参数代表要执行的行为（自己约定的，属于自定义参数）
        String opt=json.getStr("opt");

        /*
         * 即便WebSocket也会有超时时间，过了超时时间也会被服务端切断。
         * 为了不让服务端切断连接，浏览器会定时发送心跳请求
         */
        if("ping".equals(opt)){

            return;
        }


        String token=json.getStr("token");
        //因为不是Http请求，无法获取请求头数据，所以需要特殊手段从Token解析出userId
        String userId= StpUtil.stpLogic.getLoginIdByToken(token).toString();


        Map map=session.getUserProperties();
        if(!map.containsKey("userId")){
            //把userId缓存到WebSocket的Session中，给onClose()函数使用
            map.put("userId",userId);
        }

        /*
         * 由于无法分清当前的Session是之前连接，还是断联后新建的，
         * 所以每次收到消息都把Session缓存到Map中，因为当前的连接肯定是有效的。
         *
         */
        if(sessionMap.containsKey(userId)){
            sessionMap.replace(userId,session);
        }
        else{
            sessionMap.put(userId,session);
        }
        sendInfo("ok",userId);

        //
//        JSONObject json= JSONUtil.parseObj(message);
//        String opt=json.getStr("opt");
////原来这里是处理ping命令
//        String token=json.getStr("token");
//        String userId= StpUtil.stpLogic.getLoginIdByToken(token).toString();
//        Map map=session.getUserProperties();
//        if(!map.containsKey("userId")){
//            map.put("userId",userId);
//        }
//        if(sessionMap.containsKey(userId)){
//            sessionMap.replace(userId,session);
//        }
//        else{
//            sessionMap.put(userId,session);
//        }
////现在把ping命令放在后面
//        if("ping".equals(opt)){
//            return;
//        }
//        sendInfo("ok",userId);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("发生错误", error);
    }

    public static void sendInfo(String message,String userId){
        if(StrUtil.isNotBlank(userId)&&sessionMap.containsKey(userId)){
            Session session=sessionMap.get(userId);
            sendMessage(message,session);
        }
    }

    private static void sendMessage(String message,Session session){
        try {
            session.getBasicRemote().sendText(message);
        }catch (Exception e){
            log.error("执行异常",e);
        }
    }
}