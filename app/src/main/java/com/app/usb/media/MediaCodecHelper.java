package com.app.usb.media;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.view.Surface;

import java.io.IOException;

import androidx.annotation.NonNull;

/**
 * -----------------------------------------------------------------
 * Copyright (C) by AppMan, All rights reserved.
 * -----------------------------------------------------------------
 * 解码数据
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
    }

}
