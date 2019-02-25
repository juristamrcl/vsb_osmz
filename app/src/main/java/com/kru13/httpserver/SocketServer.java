package com.kru13.httpserver;
import java.io.IOException;
import java.net.ServerSocket;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import static com.kru13.httpserver.HttpServerActivity.MESSAGE_STATUS;

public class SocketServer extends Thread {
	
	private ServerSocket serverSocket;
	private ServerHandler serverHandler;
	private Handler handler;
    private final int port = 12345;

	public SocketServer(Handler handler){
	    this.handler = handler;
    }

	public void close() {
		try {
		    if(!serverSocket.isClosed()){

                serverSocket.close();
                sendMessage(handler, "Socket server exited");
            }
		} catch (IOException e) {
			Log.d("SERVER", "Error, probably interrupted in accept(), see log");
			e.printStackTrace();
		}
	}
	
	public void run() {

        try {
            serverSocket = new ServerSocket(port);
            serverHandler = new ServerHandler(serverSocket, handler);
            serverHandler.start();

            sendMessage(handler, "Socket server connected");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void sendMessage(Handler handler, String message){

        Message msg = handler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putString(MESSAGE_STATUS, message);

        msg.setData(bundle);

        handler.sendMessage(msg);
    }
}
