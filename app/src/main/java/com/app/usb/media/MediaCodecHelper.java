package com.app.usb.media;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;

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

    public MediaCodecHelper(@NonNull Surface surface, int width, int height) {
        initDecoder(surface, width, height);
    }

    private void initDecoder(@NonNull Surface surface, int width, int height) {
        try {
            releaseMediaCodec();
            mediaCodec = MediaCodec.createDecoderByType(mimeType);
            MediaFormat mediaFormat = MediaFormat.createVideoFormat(mimeType, width, height);
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

    private void releaseMediaCodec() {
        isStartRunner = false;
        if (mediaCodec != null) {
            mediaCodec.stop();
            mediaCodec.release();
            mediaCodec = null;
        }
    }

    private ByteInOutBuffer inOutBuffer = new ByteInOutBuffer();

    public void respVideoData(@NonNull byte[] data_bytes, int length) {
//        Log.v(TAG, "offset = " + offset + ", length = " + length);
        inOutBuffer.write(data_bytes, length);
        startReadRunner();
    }

    private boolean isStartRunner = false;

    private void startReadRunner() {
        if (isStartRunner) {
            return;
        }
        isStartRunner = true;
        new Thread(responseRunner).start();
        new Thread(playSurfaceRunner).start();
    }

    private MediaParser mediaParser = new MediaParser();

    private LinkedList<byte[]> frameLinkedList = new LinkedList<>();

    private Runnable responseRunner = () -> {
        while (isStartRunner) {
            if (inOutBuffer.isCanRead()) {
                int parser_result;
                for (int i = 0; i < inOutBuffer.getValidSize(); i++) {
                    byte b = inOutBuffer.getByteByIndex(i);
                    parser_result = mediaParser.parseByte(b);
//                    Log.v("TEST", i + ", readIndex = " + inOutBuffer.getReadIndex() + ", " + ByteTransUtil.byteToHexStr(b));
                    if (parser_result > 0) {
                        byte[] frame_bytes = new byte[parser_result];
                        inOutBuffer.read(frame_bytes, parser_result);
                        frameLinkedList.add(frame_bytes);
//                        Log.e("TEST", "parser_result = " + parser_result + " : " + ByteTransUtil.byteToHexStr(frame_bytes));
                        break;
                    }
                }
            }
        }
        isStartRunner = false;
        Log.e(TAG, "end responseRunner." + inOutBuffer.isCanRead());
    };

    private int mFrameIndex = 0;

    private Runnable playSurfaceRunner = () -> {
        while (isStartRunner) {
            if (frameLinkedList.size() > 0) {
                try {
                    byte[] frame_bytes = frameLinkedList.removeFirst();
                    ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
                    int inputBufferIndex = mediaCodec.dequeueInputBuffer(-1);
                    if (inputBufferIndex >= 0) {
                        ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                        long timestamp = mFrameIndex++ * (1000000 / 60);
                        inputBuffer.rewind();
                        inputBuffer.put(frame_bytes);
                        mediaCodec.queueInputBuffer(inputBufferIndex, 0, frame_bytes.length, timestamp, 0);
                    }

                    MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                    int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 10000);
                    while (outputBufferIndex >= 0) {
                        mediaCodec.releaseOutputBuffer(outputBufferIndex, true);
                        outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }
        isStartRunner = false;
    };

}
