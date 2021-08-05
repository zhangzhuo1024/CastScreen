package com.example.castscreen;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.util.Log;
import android.view.Surface;
import java.io.IOException;
import java.nio.ByteBuffer;

public class CodecH265 extends Thread {

    private static final String TAG = "gsy";
    //图省事宽高直接固定了
    private int width = 720;
    private int height = 1280;
    //h265编码
    private final String enCodeType = "video/hevc";
    private MediaCodec mediaCodec;
    private MediaProjection mediaProjection;
    private SocketService socketService;
    private boolean play = true;
    private long timeOut = 10000;
    //记录vps pps sps
    private byte[] vps_pps_sps;
    //I帧
    private final int NAL_I = 19;
    //vps帧
    private final int NAL_VPS = 32;

    public CodecH265(SocketService socketService, MediaProjection mediaProjection) {
        this.socketService = socketService;
        this.mediaProjection = mediaProjection;
    }

    public void startEncode() {
        //声明MediaFormat，创建视频格式。
        MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_HEVC, width, height);
        //描述视频格式的内容的颜色格式
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        //比特率（比特/秒）
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, width * height);
        //帧率
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 20);
        //I帧的频率
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
        try {
            //创建编码MediaCodec 类型是video/hevc
            mediaCodec = MediaCodec.createEncoderByType(enCodeType);
            //配置编码器
            mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            //创建一个目的surface来存放输入数据
            Surface surface = mediaCodec.createInputSurface();
            //获取屏幕流
            mediaProjection.createVirtualDisplay("screen", width, height, 1, DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC
                    , surface, null, null);
        } catch (IOException e) {
            Log.d(TAG,"initEncode IOException");
            e.printStackTrace();
        }
        //启动子线程
        this.start();
    }

    @Override
    public void run() {
        //编解码器立即进入刷新子状态
        mediaCodec.start();
        //缓存区的元数据
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        //子线程需要一直运行，进行编码推流，所以要一直循环
        while (play) {
            //查询编码输出
            int outPutBufferId = mediaCodec.dequeueOutputBuffer(bufferInfo, timeOut);
            if (outPutBufferId >= 0) {
                //获取编码之后的数据输出流队列
                ByteBuffer byteBuffer = mediaCodec.getOutputBuffer(outPutBufferId);
                //添加上vps,sps,pps
                reEncode(byteBuffer, bufferInfo);
                //处理完成，释放ByteBuffer数据
                mediaCodec.releaseOutputBuffer(outPutBufferId, false);
            }
        }
    }

    private void reEncode(ByteBuffer byteBuffer, MediaCodec.BufferInfo bufferInfo) {
        //偏移4 00 00 00 01为分隔符需要跳过
        int offSet = 4;
        if (byteBuffer.get(2) == 0x01) {
            offSet = 3;
        }
        //计算出当前帧的类型
        int type = (byteBuffer.get(offSet) & 0x7E) >> 1;
        if (type == NAL_VPS) {
            //保存vps sps pps信息
            vps_pps_sps = new byte[bufferInfo.size];
            byteBuffer.get(vps_pps_sps);
        } else if (type == NAL_I) {
            //将保存的vps sps pps添加到I帧前
            final byte[] bytes = new byte[bufferInfo.size];
            byteBuffer.get(bytes);
            byte[] newBytes = new byte[vps_pps_sps.length + bytes.length];
            System.arraycopy(vps_pps_sps, 0, newBytes, 0, vps_pps_sps.length);
            System.arraycopy(bytes, 0, newBytes, vps_pps_sps.length, bytes.length);
            //将重新编码好的数据发送出去
            socketService.sendData(newBytes);
        } else {
            //B帧 P帧 直接发送
            byte[] bytes = new byte[bufferInfo.size];
            byteBuffer.get(bytes);
            socketService.sendData(bytes);
        }
    }

    public void stopEncode() {
        play = false;
    }

}
