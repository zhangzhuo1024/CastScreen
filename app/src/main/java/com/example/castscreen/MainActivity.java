package com.example.castscreen;

import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private int captureRequestCode = 1;

    private MediaProjectionManager mediaProjectionManager;
    private SocketService socketService;
    private TextureView textureView;
    private Camera camera;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();

        textureView = findViewById(R.id.textureView);

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);

        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                camera = Camera.open(1);
                try {
                    camera.setPreviewTexture(surface);
                } catch (IOException e) {
                    e.printStackTrace();
                }
//                Camera.Parameters parameters = camera.getParameters();
//                Camera.Size previewSize = parameters.getPreviewSize();
//                parameters.setPreviewSize(mScreenWidth, mScreenHeight);`
//                parameters.setPictureFormat(ImageFormat.NV21);
//                camera.setParameters(parameters);
//                textureView.setLayoutParams(new FrameLayout.LayoutParams(previewSize.width, previewSize.height, Gravity.CENTER));
                camera.setPreviewCallback(new Camera.PreviewCallback() {
                    @Override
                    public void onPreviewFrame(byte[] data, Camera camera) {
                        //???????????????????????????????????????frame?????????remote???
                        Log.e(TAG, "camera:" + camera);
                        Log.e(TAG, "byte:" + data);
                    }
                });
                camera.setPreviewCallbackWithBuffer(new Camera.PreviewCallback() {
                    @Override
                    public void onPreviewFrame(byte[] data, Camera camera) {

                    }
                });
                camera.startPreview();
                //?????????????????????
//                textureView.setAlpha(0.5f);
                //????????????????????????????????????????????????
                textureView.setRotation(90.0f);

            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                camera.stopPreview();
                camera.release();
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        });

//        textureView.setBackgroundColor(getResources().getColor(android.R.color.black));  //Android 7.0 ?????????TexureView?????????????????????
    }

    private void init() {
        //??????MediaProjectionManager
        mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_start:
                startCast();
                break;
        }
    }

    //??????????????????
    private void startCast() {
//        PermissionUtil.checkPermission(this, PermissionUtil.storagePermissions, permissionRequestCode);
        Intent intent = mediaProjectionManager.createScreenCaptureIntent();
        startActivityForResult(intent, captureRequestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            return;
        }
        if (requestCode == this.captureRequestCode) {
            startCast(resultCode, data);
        }
    }

    //?????????????????????????????????
    private void startCast(int resultCode, Intent data) {
        //??????????????????resultCode?????????requestCode????????????????????????????????????
        MediaProjection mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
        if (mediaProjection == null) {
            return;
        }
        //?????????????????????
        socketService = new SocketService();
        //???MediaProjection?????? socketService
        socketService.start(mediaProjection);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (socketService != null) {
            socketService.colse();
        }
    }
}