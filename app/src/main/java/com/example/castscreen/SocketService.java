package com.example.castscreen;

import android.media.projection.MediaProjection;

import java.io.IOException;
import java.net.InetSocketAddress;

public class SocketService {

    private static final String TAG = "SocketService";
    //端口号，尽量设大一些
    private int port = 11006;
    private CodecH265 codecH265;
    private SocketServer webSocketServer;

    public SocketService() {
        webSocketServer = new SocketServer(new InetSocketAddress(port));
    }

    public void start(MediaProjection mediaProjection) {
        //启动webSocketServer  此时当前设备就可以作为一个服务器了
        webSocketServer.start();
        codecH265 = new CodecH265(this, mediaProjection);
        //开始编码
        codecH265.startEncode();
    }

    //关闭服务端
    public void colse() {
        try {
            webSocketServer.stop();
            webSocketServer.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        codecH265.stopEncode();
    }

    //发送编码后的数据
    public void sendData(byte[] bytes) {
        webSocketServer.sendData(bytes);
    }
}
