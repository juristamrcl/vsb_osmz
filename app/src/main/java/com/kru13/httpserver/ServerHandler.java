package com.kru13.httpserver;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Calendar;

import static com.kru13.httpserver.SocketServer.sendMessage;


public class ServerHandler extends Thread {

    public static String NEWLINE = "\r\n";
	private ServerSocket serverSocket;
	private Handler handler;

	public ServerHandler(ServerSocket serverSocket, Handler handler){
	    this.serverSocket = serverSocket;
	    this.handler = handler;
    }

	public void run() {
        boolean bRunning;
        try {
            bRunning = true;
            while (bRunning) {
                Log.d("SERVER", "Socket Waiting for connection");

                Socket s = serverSocket.accept();

                (new ServerHandler(serverSocket, handler)).start();

                Log.d("SERVER", "Socket Accepted");

                OutputStream o = s.getOutputStream();
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(o));
                BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));

                ArrayList<String> responses = new ArrayList<>();
                String response;
                while(!(response = in.readLine()).isEmpty())
                {
                    Log.d("SERVER", "reading");
                    responses.add(response);
                }
                if(!responses.isEmpty())
                {
//                    for  (int i = 0; i < responses.size(); i++)
//                    {
//                        Log.d("SERVER1", i + ": " + responses.get(i));
//                    }
                    Log.d("SERVER", "out: " + responses.toString());
                    String data[] = responses.get(0).split(" ");
                    if (data[0].toUpperCase().equals("GET"))
                    {
                        Log.d("SERVER", "File exist");

                        data[1] =  data[1].equals("/") ? "index.html" : data[1];

                        File outFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), data[1]);
                        if (outFile.exists())
                        {
                            out.write("HTTP/1.0 200 Ok" + NEWLINE);
                            out.write("Date: " + Calendar.getInstance().getTime() + NEWLINE);
                            out.write("Content-Length: " + String.valueOf(outFile.length()) + NEWLINE);

                            sendMessage(handler, "Data returned successfully to <" +  responses.get(1).split(" ")[1] + ">");
                            sendMessage(handler, "Requested url <" +  data[1] + ">");
                            sendMessage(handler, "Total size <" +  String.valueOf(outFile.length()) + ">");

                            out.write(NEWLINE);
                            out.flush();

                            byte[] buf = new byte[1024];
                            int len;
                            FileInputStream fis = new FileInputStream(outFile);
                            while ((len = fis.read(buf)) > 0){
                                o.write(buf, 0, len);
                            }

                        }
                        else
                        {

                            String[] folders = data[1].split("/");
                            Log.d("SERVER", folders[1]);

                            File notFoundFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath(),"notfound.html");
                            for (int i = 0; i < folders.length - 1; i++){
                                String tmp = "/";
                                for (int j = 0; j < i; j++) {
                                    tmp.concat(folders[j].concat("/"));
                                }
                                File toMakeFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), tmp);

                                toMakeFile.mkdirs();
                            }
                            File toMakeFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), data[1]);
                            toMakeFile.createNewFile();

                            out.write("HTTP/1.0 404 Not Found"+ NEWLINE);
                            out.write("Date: " + Calendar.getInstance().getTime()+ NEWLINE);
                            out.write("Content-Length: " + String.valueOf(notFoundFile.length()) + NEWLINE);
                            out.write("Content-Type: text/html" + NEWLINE);
                            out.write("Connection: Closed"+ NEWLINE);
                            out.write(NEWLINE);
                            out.flush();

                            byte[] buf = new byte[1024];
                            int len;
                            FileInputStream fis = new FileInputStream(notFoundFile);
                            while ((len = fis.read(buf)) > 0){
                                o.write(buf, 0, len);
                            }

                            Log.d("SERVER","File not found");
                        }
                    }
                    else
                    {
                        Log.d("SERVER","bad request methode!");
                    }
                }

                s.close();
                Log.d("SERVER", "Socket Closed");}
        }
        catch (Exception e) {
            e.printStackTrace();
            if (serverSocket != null && serverSocket.isClosed()){
                Log.d("SERVER", "Normal exit");
            }
            else {
                Log.d("SERVER", "Error");
                sendMessage(handler, "Socket server error occured");
                e.printStackTrace();
            }
        }
        finally {
            serverSocket = null;
            bRunning = false;
        }
    }

}
