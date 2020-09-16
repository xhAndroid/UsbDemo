package com.app.usb.media;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import com.link.usb.UsbHostManager;

import java.io.IOException;
import java.nio.ByteBuffer;

import androidx.annotation.NonNull;

/**
 * -----------------------------------------------------------------
 * Copyright (C) by AppMan, All rights reserved.
 * -----------------------------------------------------------------
 * 解码数据
 *
 * @author AppMan
 * @date Created on 2020/09/14
 */
public class MediaCodecHelper {

    private static final String TAG = MediaCodecHelper.class.getSimpleName();

    private MediaCodec mediaCodec;
    /**
     * Video Constants
     * "video/avc" : H.264 Advanced Video
     * "video/hevc" : H.265 Advanced Video
     */
    private final String mimeType = MediaFormat.MIMETYPE_VIDEO_AVC;
    private static final int VIDEO_WIDTH = 1280;
    private static final int VIDEO_HEIGHT = 720;

    public MediaCodecHelper(@NonNull Surface surface) {
        initDecoder(surface);
    }

    private void initDecoder(@NonNull Surface surface) {
        try {
            mediaCodec = MediaCodec.createDecoderByType(mimeType);
            MediaFormat mediaFormat = MediaFormat.createVideoFormat(mimeType, VIDEO_WIDTH, VIDEO_HEIGHT);
            // 关键帧频率，请求关键帧之间的xx秒数间隔。
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 60);
            // 1 表示 停止播放时设置为黑色空白
            ///mediaFormat.setInteger(MediaFormat.KEY_PUSH_BLANK_BUFFERS_ON_STOP,  1);
            mediaCodec.configure(mediaFormat, surface, null, 0);
            mediaCodec.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private ByteInOutBuffer inOutBuffer = new ByteInOutBuffer();

    public void respVideoData(@NonNull byte[] data_bytes, int length) {
        inOutBuffer.write(data_bytes, length);
//        Log.v(TAG, "offset = " + offset + ", length = " + length);
        startReadRunner();
    }

    private boolean isStartRead = false;

    private void startReadRunner() {
        if (isStartRead) {
            return;
        }
        isStartRead = true;
        new Thread(responseRunner).start();
    }

    private static final byte H_00 = (byte) 0x00;
    private static final byte H_01 = (byte) 0x01;
    private static final byte H_67 = (byte) 0x67;
    private static final byte H_61 = (byte) 0x61;

    private Runnable responseRunner = () -> {
        while (UsbHostManager.getInstance().isConnect()) {
            // 读数组，组帧解码
            int valid_size = inOutBuffer.getValidSize();
//            Log.v(TAG, "valid_size  = " + valid_size);
            for (int i = 0; i < valid_size; i++) {
                boolean is_head = inOutBuffer.getByteByIndex(i) == H_00
                        && inOutBuffer.getByteByIndex(i + 1) == H_00
                        && inOutBuffer.getByteByIndex(i + 2) == H_00
                        && inOutBuffer.getByteByIndex(i + 3) == H_01;
                boolean is_video = inOutBuffer.getByteByIndex(i + 4) == H_67
                        || inOutBuffer.getByteByIndex(i + 4) == H_61;
                if (is_head && is_video) {
                    Log.v(TAG, "valid_size : " + valid_size + ", findFrameCount  = " + findFrameCount + ", frameSize = " + frameSize);
                    findFrameCount++;
                    if (findFrameCount == 1) {
                        firstFrameIndex = i;
                    } else if (findFrameCount == 2) {
                        byte[] data = new byte[frameSize + firstFrameIndex];
                        inOutBuffer.read(data, data.length);
                        ByteBuffer byteBuffer = ByteBuffer.wrap(data, firstFrameIndex, frameSize);
//                        iUsbHostDataListner.onSearchUsbVideoFrame(byteBuffer);
                        //搜到一帧视频，目的达到。退出循环并重置所有参数 。
                        resetParameter();
                        break;
                    }
                } //end if

                if (findFrameCount == 1) {
                    frameSize++;
                }
            } // end for
            resetParameter();
        }
        isStartRead = false;
        Log.e(TAG, "end responseRunner." + inOutBuffer.isCanRead());
    };

    private static void resetParameter() {
        firstFrameIndex = 0;
        findFrameCount = 0;
        frameSize = 0;
    }

    /**
     * 第一次发现 00 00 00 01 XX 时的索引
     */
    private static int firstFrameIndex = 0;
    /**
     * 第几次发现 00 00 00 01 xx
     */
    private static int findFrameCount = 0;
    /**
     * 帧数据大小
     */
    private static int frameSize = 0;
}
