/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.antennaesdk.ws;

import com.google.gson.Gson;
import org.antennae.common.messages.ClientMessage;
import org.antennae.common.messages.ClientMessageWrapper;
import org.antennae.common.messages.ServerMessageWrapper;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

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

            ServerMessageWrapper serverMessageWrapper = ServerMessageWrapper.fromJson(m);

            ClientMessageWrapper clientMessageWrapper = new ClientMessageWrapper();
            ClientMessage clientMessage = new ClientMessage();
            clientMessage.setTo( serverMessageWrapper.getServerMessage().getFrom());

            String payload = serverMessageWrapper.getServerMessage().getPayLoad();

            clientMessage.setPayLoad( doBuzinezzLogic(payload) );

            clientMessageWrapper.setClientMessage(clientMessage);
            clientMessageWrapper.setSessionId( serverMessageWrapper.getSessionId());
            clientMessageWrapper.setNodeId( serverMessageWrapper.getNodeId() );

            if( session != null && session.isOpen() ){
                session.getAsyncRemote().sendText( clientMessageWrapper.toJson() );
            }
        }
    }

    public String doBuzinezzLogic( String payLoad ){
        Message m = Message.fromJson(payLoad);

        m.body.text = "echo from: " + m.body.text + " : \n" + MicroTimestamp.INSTANCE.getMillis();

        return m.toJson();
    }

    @OnMessage
    public void onMessage(String message) {
        System.out.println("Received msg: "+message);

        processTextMessage(message);
    }

    @OnOpen
    public void onOpen(Session session, EndpointConfig config){
        System.out.println("session opened :" + session.getId() );
        this.session = session;
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason){
        System.out.println("session closed :" + closeReason.toString() );

        // reconnect
        this.connect();
    }

    public void connect(){

        try {
            container.connectToServer(WebSocketMessageProcessor.class, URI.create("ws://localhost:8080/server"));
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

    /*
    {
      "id": "123e4567-e89b-12d3-a456-426655440013",
      "type": "TEXT",
      "version": "1",
      "sender": {
      "username": "N1"
      },
      "body": {
        "text": "Thanks for call me, Dave!"
       }
    }
     */
    public static class Message{
        String id;
        String type;
        String version;
        String sender;
        String username;
        Body body;

        public static class Body{
            String text;
        }

        public String toJson(){
            Gson gson = new Gson();
            String result = gson.toJson(this);
            return result;
        }
        public static Message fromJson( String json ){
            Gson gson = new Gson();
            Message message = gson.fromJson( json, Message.class);
            return message;
        }
    }

    /**
     * Class to generate timestamps with microsecond precision
     * For example: MicroTimestamp.INSTANCE.get() = "2012-10-21 19:13:45.267128"
     */
    public enum MicroTimestamp
    {  INSTANCE ;

        private long              startDate ;
        private long              startNanoseconds ;
        private SimpleDateFormat  dateFormat ;

        private MicroTimestamp()
        {  this.startDate = System.currentTimeMillis() ;
            this.startNanoseconds = System.nanoTime() ;
            this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS") ;
        }

        public String get()
        {  long microSeconds = (System.nanoTime() - this.startNanoseconds) / 1000 ;
            long date = this.startDate + (microSeconds/1000) ;
            return this.dateFormat.format(date) + String.format("%03d", microSeconds % 1000) ;
        }
        public String getMillis(){
            return this.dateFormat.format(startDate);
        }
    }
}