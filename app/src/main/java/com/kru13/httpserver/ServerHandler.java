package com.kru13.httpserver;

import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Calendar;

import static com.kru13.httpserver.SocketServer.sendMessage;


public class ServerHandler extends Thread{

    public static String NEWLINE = "\r\n";
	private ServerSocket serverSocket;
	private Handler handler;
	private HttpServerActivity activity;
    public static DataOutputStream stream;
    private ByteArrayOutputStream imageBuffer;
    private String boundary = "--boundary";
    private boolean closeSocket = true;

	public ServerHandler(ServerSocket serverSocket, Handler handler, HttpServerActivity activity){
	    this.serverSocket = serverSocket;
	    this.handler = handler;
	    this.activity = activity;

        imageBuffer = new ByteArrayOutputStream();
    }

	public void run() {
        boolean bRunning;
        try {
            bRunning = true;
            while (bRunning) {
                Log.d("SERVER", "Socket Waiting for connection");

                Socket s = serverSocket.accept();

                (new ServerHandler(serverSocket, handler, activity)).start();

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

                        Log.d("Activ2", data[1]);
                        // writing image async
                        if(data[1].indexOf("/snapshot") != -1){
                            data[1] = "/snapshot";

                            out.write("HTTP/1.0 200 Ok" + NEWLINE);
                            out.write("Date: " + Calendar.getInstance().getTime() + NEWLINE);
                            out.write("Content-Length: " + String.valueOf(activity.getPicture().length) + NEWLINE);
                            out.write("Content-Type: image/jpeg" + NEWLINE);

                            out.write(NEWLINE);
                            out.flush();

                            o.write(activity.getPicture(), 0, activity.getPicture().length);
                        }
                        else if(data[1].indexOf("/stream") != -1){
                            stream = new DataOutputStream(s.getOutputStream());
                            if (stream != null)
                            {
                                try
                                {
                                    Log.d("onPreviewFrame", "stream succ");
                                    stream.write(("HTTP/1.0 200 OK" + NEWLINE +
                                            "Server: localhost/12345" + NEWLINE +
                                            "Cache-Control:  no-cache" + NEWLINE +
                                            "Cache-Control:  private" + NEWLINE +
                                            "Content-Type: multipart/x-mixed-replace;boundary=" + boundary + NEWLINE ).getBytes());

                                    stream.flush();

                                    closeSocket = false;
                                    Log.d("onPreviewFrame", "stream created");

                                    sendStreamData();
                                }
                                catch (IOException e)
                                {
                                    Log.d("ERROR:", e.getLocalizedMessage());
                                }
                            }
                        }
                        else if(data[1].indexOf("/cgi-bin") != -1){
                            String commands[] = data[1].split("/");

                            if (commands.length < 3)
                                break;

                            String command = commands[2];

                            try{
                                Process process = Runtime.getRuntime().exec(command);
                                BufferedReader bufferedReader = new BufferedReader(
                                        new InputStreamReader(process.getInputStream()));

                                out.write("HTTP/1.0 200 Ok" + NEWLINE);
                                out.write("Date: " + Calendar.getInstance().getTime() + NEWLINE);
//                                out.write("Content-Length: " + String.valueOf(bufferedReader) + NEWLINE);
                                out.write("Content-Type: text/html" + NEWLINE);
                                out.write(NEWLINE);
                                out.write("<html>");
                                String line;
                                while ((line = bufferedReader.readLine()) != null){
                                    out.write("<pre>" + line + "</pre>");
                                }
                                out.write(NEWLINE);

                                out.write("</html>");
                                out.flush();

                            }
                            catch (Exception e){
                                Log.d("ProcessOutput", "just failed: " + e.getMessage());

                            }

                        }
                        else {
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
                    }
                    else
                    {
                        Log.d("SERVER","bad request methode!");
                    }
                }

                if(closeSocket){
                    s.close();

                    Log.d("SERVER", "Socket Closed");
                }
            }
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

    private void sendStreamData(){
        if (stream != null){
            try
            {
                byte[] baos = activity.getPicture();
                // buffer is a ByteArrayOutputStream
                imageBuffer.reset();
                imageBuffer.write(baos);
                imageBuffer.flush();
                // write the content header
                stream.write((NEWLINE +  boundary + NEWLINE +
                        "Content-type: image/jpeg" + NEWLINE +
                        "Content-Length: " + imageBuffer.size() + NEWLINE + NEWLINE).getBytes());

                stream.write(imageBuffer.toByteArray());
                stream.write((NEWLINE ).getBytes());

                stream.flush();
                Log.d("onPreviewFrame", "succ");
            }
            catch (IOException e)
            {
                Log.d("onPreviewFrame error:  ", e.getLocalizedMessage());
            }
        }
        else{

            Log.d("onPreviewFrame", "null");
        }

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                sendStreamData();
            }
        }, 100);
    }
}
