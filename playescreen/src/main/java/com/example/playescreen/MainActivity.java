package com.example.playescreen;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.nio.ByteBuffer;

public class MainActivity extends AppCompatActivity implements SocketServer.SocketCallback {

    private static final String TAG = "gsy";
    private Surface surface;
    private SurfaceView surfaceView;
    private MediaCodec mediaCodec;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }

    private void init() {
        surfaceView = findViewById(R.id.sfv_play);
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                surface = holder.getSurface();
                //连接到服务端
                initSocket();
                //配置MediaCodec
                initDecoder(surface);
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {

            }
        });
    }

    private void initDecoder(Surface surface) {
        try {
            //配置MediaFormat MediaCodec
            mediaCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_HEVC);
            MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_HEVC, 720, 1280);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 720 * 1280);
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 20);
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
            mediaCodec.configure(mediaFormat, surface, null, 0);
            mediaCodec.start();
        } catch (IOException e) {
            Log.d(TAG, "initDecoder IOException ");
            e.printStackTrace();
        }
    }

    private void initSocket() {
        Log.d(TAG, "initSocket");
        //启动客户端
        SocketServer socketServer = new SocketServer();
        socketServer.setSocketCallback(this);
        socketServer.start();
    }

    @Override
    public void callBack(byte[] data) {
        Log.d(TAG, "mainActivity callBack");
        //得到填充了有效数据的input buffer的索引
        int index = mediaCodec.dequeueInputBuffer(10000);
        if (index >= 0) {
            //获取输入缓冲区
            ByteBuffer inputBuffer = mediaCodec.getInputBuffer(index);
            //清除原来的内容以接收新的内容
            inputBuffer.clear();
            inputBuffer.put(data, 0, data.length);
            //将其提交给编解码器 把缓存数据入队
            mediaCodec.queueInputBuffer(index, 0, data.length, System.currentTimeMillis(), 0);
        }
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        //请求一个输出缓存
        int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 10000);
        //直到outputBufferIndex < 0 才算处理完所有数据
        while (outputBufferIndex > 0) {
            mediaCodec.releaseOutputBuffer(outputBufferIndex, true);
            outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
        }
    }
}
