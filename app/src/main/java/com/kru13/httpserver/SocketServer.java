package com.kru13.httpserver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
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

import android.os.Environment;
import android.os.Handler;
import android.util.Log;

import static android.icu.lang.UCharacter.WordBreak.NEWLINE;

public class SocketServer extends Thread {
	
	ServerSocket serverSocket;
	Handler handler = new Handler();
	public final int port = 12345;
	boolean bRunning;
	
	public void close() {
		try {
			serverSocket.close();
		} catch (IOException e) {
			Log.d("SERVER", "Error, probably interrupted in accept(), see log");
			e.printStackTrace();
		}
		bRunning = false;
	}
	
	public void run() {
        try {
        	Log.d("SERVER", "Creating Socket");
            serverSocket = new ServerSocket(port);
            bRunning = true;
            while (bRunning) {
                Log.d("SERVER", "Socket Waiting for connection");
                Socket s = serverSocket.accept();
                handler.post(new Runnable() {
                    @Override
                    public void run() {

                    }
                });

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
                    String header[] = responses.get(0).split(" ");
                    if (header[0].toUpperCase().equals("GET"))
                    {
                        Log.d("SERVER", "File exist");

                        String fileName = header[1].substring(header[1].lastIndexOf("/")+1);
                        File outFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath(),fileName);
                            if (outFile.exists())
                            {
                                BufferedReader outFileStream = new BufferedReader(new InputStreamReader(new FileInputStream(outFile)));

                            out.write("HTTP/1.0 200 OK"+ NEWLINE);
                            out.write("Date: "+ Calendar.getInstance().getTime()+ NEWLINE);
                            out.write("Server: localhost/12345"+ NEWLINE);
                            out.write("Content-Length: " + String.valueOf(outFile.length())+ NEWLINE);
                            out.write("Connection: Closed"+ NEWLINE);
                            out.write(NEWLINE);
                            out.flush();


//                            Log.d("SERVER","Size: " + fileLength);
                            int fileLength = 0;
                            byte[] buf = new byte[1024];
                            int len = 0;
                            FileInputStream fis = new FileInputStream(outFile);
                            while ((len = fis.read(buf)) > 0){
                                o.write(buf, 0, len);
                            }

                            outFileStream.close();
                        }
                        else
                        {
                            File notFoundFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath(),"notfound.html");
//                            notFoundFile.createNewFile();
                            BufferedReader outFileStream = new BufferedReader(new InputStreamReader(new FileInputStream(notFoundFile)));

                            outFileStream.close();
                            out.write(header[2] + " 404 Not Found"+ NEWLINE);
                            out.write("Date: "+Calendar.getInstance().getTime()+ NEWLINE);
                            out.write("Server: localhost/12345"+ NEWLINE);
                            out.write("Content-Length: " + String.valueOf(notFoundFile.length()) + NEWLINE);
                            out.write("Connection: Closed"+ NEWLINE);
                            out.write("Content-Type: text/html"+ NEWLINE);
                            out.write(NEWLINE);
                            out.flush();

                            byte[] buf = new byte[1024];
                            int len = 0;
                            FileInputStream fis = new FileInputStream(notFoundFile);
                            while ((len = fis.read(buf)) > 0){
                                o.write(buf, 0, len);
                            }

                            Log.d("SERVER","File not found");
                        }
                    }
                    else if(header[0].toUpperCase().equals("PUT"))
                    {
                        Log.d("SERVER","Put methode");

                    }
                    else
                    {
                        Log.d("SERVER","bad request methode!");
                    }
                }
                else {

                }

                s.close();
                Log.d("SERVER", "Socket Closed");}
        }
        catch (IOException e) {
            if (serverSocket != null && serverSocket.isClosed())
            	Log.d("SERVER", "Normal exit");
            else {
            	Log.d("SERVER", "Error");
            	e.printStackTrace();
            }
        }
        finally {
        	serverSocket = null;
        	bRunning = false;
        }
    }

}
