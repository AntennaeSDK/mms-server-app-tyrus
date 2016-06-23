/**
 * Created by snambi on 6/22/16.
 */
package com.github.antennaesdk.ws;

import org.antennae.common.messages.ClientMessage;
import org.antennae.common.messages.ServerTrackedMessage;
import org.glassfish.tyrus.client.ClientManager;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.Date;
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

            ServerTrackedMessage message = ServerTrackedMessage.fromJson(m);

            ClientMessage clientMessage = new ClientMessage();
            clientMessage.setTo( message.getServerMessage().getFrom());

            String payload = message.getServerMessage().getPayLoad();
            long time = Calendar.getInstance().getTimeInMillis();
            Date date = new Date(time);

            String newPayload = payload + "; time =" + date.toString();
            clientMessage.setPayLoad( newPayload );

            message.setClientMessage(clientMessage);

            if( session != null && session.isOpen() ){
                session.getAsyncRemote().sendText( message.toJson() );
            }
        }
    }

    @OnMessage
    public void onMessage(String message) {
        System.out.println("Received msg: "+message);

        processTextMessage(message);
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