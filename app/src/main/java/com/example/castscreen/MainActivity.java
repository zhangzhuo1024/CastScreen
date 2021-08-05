package com.example.castscreen;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {
    private int permissionRequestCode = 100;
    private int captureRequestCode = 1;

    private MediaProjectionManager mediaProjectionManager;
    private SocketService socketService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }

    private void init() {
        //拿到MediaProjectionManager
        mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_start:
                startCast();
                break;
        }
    }

    //请求开始录屏
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

    //录屏开始后进行编码推流
    private void startCast(int resultCode, Intent data) {
        //这里需要传入resultCode而不是requestCode，在这里踩了个坑大家注意
        MediaProjection mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
        if (mediaProjection == null) {
            return;
        }
        //初始化服务器端
        socketService = new SocketService();
        //将MediaProjection传给 socketService
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