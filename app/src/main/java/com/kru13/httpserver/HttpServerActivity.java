package com.kru13.httpserver;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;


public class HttpServerActivity extends AppCompatActivity implements OnClickListener{

	public static String MESSAGE_STATUS = "MSG_STATUS";

	private SocketServer s;
	public static Handler handler;
	private RecyclerView mStatusRecyclerView;
	private StatusRecyclerAdapter mStatusAdapter;
	private Camera camera;
	private CameraPreview cameraPreview;
	private byte[] picture;

	public byte[] getPicture() {
		return picture;
	}

	private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

		@Override
		public void onPictureTaken(byte[] data, Camera camera) {

			picture = data;
			camera.startPreview();
		}
	};
	private Camera.PreviewCallback mPprev = new Camera.PreviewCallback() {

		@Override
		public void onPreviewFrame(byte[] bytes, Camera camera) {
			picture = convertoToJpeg(bytes, camera);
		}
	};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

		StrictMode.ThreadPolicy policy = new
				StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy);

		setContentView(R.layout.activity_http_server);

        Button btn1 = (Button)findViewById(R.id.button1);
        Button btn2 = (Button)findViewById(R.id.button2);
         
        btn1.setOnClickListener(this);
        btn2.setOnClickListener(this);

		if (ContextCompat.checkSelfPermission(this,
				Manifest.permission.WRITE_EXTERNAL_STORAGE)
				!= PackageManager.PERMISSION_GRANTED) {

			if (ActivityCompat.shouldShowRequestPermissionRationale(this,
					Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

			} else {
				// No explanation needed; request the permission
				ActivityCompat.requestPermissions(this,
						new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
						1);

			}
		}

		checkCameraHardware(this);

		mStatusRecyclerView = findViewById(R.id.status_recycler);
		LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);

		mStatusRecyclerView.setLayoutManager(mLayoutManager);

		mStatusAdapter = new StatusRecyclerAdapter(this, new ArrayList());
		mStatusRecyclerView.setAdapter(mStatusAdapter);

		handler = new Handler(){
			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);

				Bundle data = msg.getData();

				String message = data.getString(MESSAGE_STATUS);

				mStatusAdapter.insert(message);
			}
		};

		camera = getCameraInstance();
		camera.setPreviewCallback(mPprev);
		Log.d("ServerActivity","have camera? " + (camera != null));
		cameraPreview = new CameraPreview(this, camera, mPicture);
		FrameLayout preview = findViewById(R.id.camera_preview);
		preview.addView(cameraPreview);

		camera.startPreview();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.http_server, menu);
        return true;
    }


	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		if (v.getId() == R.id.button1) {
			s = new SocketServer(handler, this, camera);
			s.start();
		}
		if (v.getId() == R.id.button2) {
			s.close();
			try {
				s.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

    private boolean checkCameraHardware(Context context) {
//        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
//			Log.d("ServerActivity", "Has camera");
//            return true;
//        } else {
//			Log.d("ServerActivity", "Does not have camera");
//            return false;
//        }

		if (ContextCompat.checkSelfPermission(this,
				Manifest.permission.CAMERA)
				!= PackageManager.PERMISSION_GRANTED) {

			if (ActivityCompat.shouldShowRequestPermissionRationale(this,
					Manifest.permission.CAMERA)) {

			} else {
				// No explanation needed; request the permission
				ActivityCompat.requestPermissions(this,
						new String[]{Manifest.permission.CAMERA},
						1);

			}
		}
		return true;
    }

    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
		Log.d("ServerActivity","Get camera ? " + (c != null));
        return c; // returns null if camera is unavailable
    }

    public void takePicture(){
    	if (camera == null)
    		return;

    	camera.takePicture(null, null, mPicture);

		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				takePicture();
			}
		}, 500);
	}

	public byte[] convertoToJpeg(byte[] data, Camera camera) {

		YuvImage image = new YuvImage(data, ImageFormat.NV21,
				camera.getParameters().getPreviewSize().width, camera.getParameters().getPreviewSize().height, null);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int quality = 20; //set quality
		image.compressToJpeg(new Rect(0, 0, camera.getParameters().getPreviewSize().width, camera.getParameters().getPreviewSize().height), quality, baos);//this line decreases the image quality

		return baos.toByteArray();
	}
}
