/**
 * Created by snambi on 6/22/16.
 */
package com.github.antennaesdk.ws;

import org.antennae.common.messages.Message;
import org.glassfish.tyrus.client.ClientManager;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import javax.websocket.*;

@ClientEndpoint
public class WebSocketMessageProcessor {

    private static Object waitLock = new Object();

    WebSocketContainer container=null;
    private Session session=null;

    public WebSocketMessageProcessor(){
        container = ContainerProvider.getWebSocketContainer();
    }

    public void processTextMessage( String m ){
        if( m != null ){
            Message message = Message.fromJson(m);
        }
    }

    @OnMessage
    public void onMessage(String message) {
        System.out.println("Received msg: "+message);
    }

    @OnOpen
    public void onOpen(Session session, EndpointConfig config){
        System.out.println("session opened :" + session.getId() );
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason){
        System.out.println("session closed :" + closeReason.toString() );

        // reconnect
        this.connect();
    }

    public void connect(){

        try {
            session = container.connectToServer(WebSocketMessageProcessor.class, URI.create("ws://localhost:8080/server"));
        } catch (DeploymentException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close(){
        if(session!=null){
            try {
                session.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void  wait4TerminateSignal()
    {
        synchronized(waitLock)
        {
            try {
                waitLock.wait();
            } catch (InterruptedException e) {

            }
        }
    }


    public static void main(String[] args) {



        try{

            WebSocketMessageProcessor processor = new WebSocketMessageProcessor();
            processor.connect();

            wait4TerminateSignal();

        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    // sample ws endpoint class
    public static class MyEndPoint extends Endpoint{

        public void onOpen(Session session, EndpointConfig config) {
            final RemoteEndpoint remote = session.getBasicRemote();
            session.addMessageHandler(new MessageHandler.Whole<String>(){
                public void onMessage(String message) {

                }
            });
        }

        public void onClose(Session session, CloseReason closeReason) {
        }
        public void onError(Session session, Throwable thr) {
        }
    }
}