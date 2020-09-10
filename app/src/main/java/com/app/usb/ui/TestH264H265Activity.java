package com.app.usb.ui;

import android.content.Context;
import android.content.Intent;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.app.usb.R;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import androidx.appcompat.app.AppCompatActivity;

/**
 * -----------------------------------------------------------------
 * Copyright (C) by AppMan, All rights reserved.
 * -----------------------------------------------------------------
 * 1、Android平台对H264硬解码
 * https://blog.csdn.net/yqj234/article/details/51791476
 * 2、H265 硬解码
 *
 * @author AppMan
 * @date Created on 2020/09/02
 */
public class TestH264H265Activity extends AppCompatActivity {

    private static final String TAG = TestH264H265Activity.class.getSimpleName();

    public static void startActivity(Context context, boolean is_h265) {
        Intent intent = new Intent(context, TestH264H265Activity.class);
        intent.putExtra(KEY_IS_H265, is_h265);
        context.startActivity(intent);
    }

    private static final String KEY_IS_H265 = "KEY_IS_H265";

    // 注意外部SD卡目录一定要拷贝存在此文件
    private String h264Path = Environment.getExternalStorageDirectory() + File.separator + "720pq.h264";
    private String h265Path = Environment.getExternalStorageDirectory() + File.separator + "temp.h265";
    private File videoFile;
    private InputStream inputStream = null;

    private SurfaceView mSurfaceView;
    private Button mReadButton;
    private MediaCodec mCodec;

    private Thread readFileThread;
    private boolean isInit = false;

    /**
     * Video Constants
     * "video/avc" : H.264 Advanced Video
     * "video/hevc" : H.265 Advanced Video
     */
    private String mimeType = MediaFormat.MIMETYPE_VIDEO_AVC;
    private final static int VIDEO_WIDTH = 1280;
    private final static int VIDEO_HEIGHT = 720;
    private final static int TIME_INTERNAL = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_h264_h265);

        boolean is_h265 = getIntent().getBooleanExtra(KEY_IS_H265, false);
        if (is_h265) {
            videoFile = new File(h265Path);
            mimeType = MediaFormat.MIMETYPE_VIDEO_HEVC;
        } else {
            videoFile = new File(h264Path);
            mimeType = MediaFormat.MIMETYPE_VIDEO_AVC;
        }

        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceView1);
        mReadButton = (Button) findViewById(R.id.btn_readfile);
        mReadButton.setText(is_h265 ? "Play H265" : "Play H264");
        mReadButton.setOnClickListener((View v) -> {
            if (videoFile.exists()) {
                initDecoder();
                startReadFileThread();
            } else {
                Toast.makeText(getApplicationContext(), "Video File Not Found!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (readFileThread != null) {
            readFileThread.interrupt();
        }
    }

    private void initDecoder() {
        if (isInit) {
            return;
        }
        isInit = true;
        try {
            mCodec = MediaCodec.createDecoderByType(mimeType);
            MediaFormat mediaFormat = MediaFormat.createVideoFormat(mimeType, VIDEO_WIDTH, VIDEO_HEIGHT);
            // 关键帧频率，请求关键帧之间的xx秒数间隔。
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 60);
            // 1 表示 停止播放时设置为黑色空白
            ///mediaFormat.setInteger(MediaFormat.KEY_PUSH_BLANK_BUFFERS_ON_STOP,  1);
            mCodec.configure(mediaFormat, mSurfaceView.getHolder().getSurface(), null, 0);
            mCodec.start();
        } catch (IOException e) {
            isInit = false;
            e.printStackTrace();
        }
    }

    private int mCount = 0;

    private boolean onFrame(byte[] buf, int offset, int length) {
        Log.e(TAG, "onFrame start, Thread : " + Thread.currentThread().getId());
        // Get input buffer index
        ByteBuffer[] inputBuffers = mCodec.getInputBuffers();
        int inputBufferIndex = mCodec.dequeueInputBuffer(100);
        Log.e(TAG, "onFrame inputBufferIndex:" + inputBufferIndex);

        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
            inputBuffer.clear();
            inputBuffer.put(buf, offset, length);
            mCodec.queueInputBuffer(inputBufferIndex, 0, length, mCount * 1000000 / 60, 0);
            mCount++;
        } else {
            return false;
        }

        // Get output buffer index
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex = mCodec.dequeueOutputBuffer(bufferInfo, 100);
        while (outputBufferIndex >= 0) {
            mCodec.releaseOutputBuffer(outputBufferIndex, true);
            outputBufferIndex = mCodec.dequeueOutputBuffer(bufferInfo, 0);
        }
        Log.e(TAG, "onFrame end");
        return true;
    }

    Runnable readFileRunnable = () -> {
        int h264Read = 0;
        int frameOffset = 0;
        byte[] buffer = new byte[100000];
        byte[] framebuffer = new byte[200000];
        boolean readFlag = initVideoBufferRead();
        while (!Thread.interrupted() && readFlag) {
            try {
                int length = inputStream.available();
                if (length > 0) {
                    // Read file and fill buffer
                    int count = inputStream.read(buffer);
                    h264Read += count;
                    Log.d(TAG, "Read count = " + count + ", h264Read = " + h264Read);
                    // Fill frameBuffer
                    if (frameOffset + count < 200000) {
                        System.arraycopy(buffer, 0, framebuffer, frameOffset, count);
                        frameOffset += count;
                    } else {
                        frameOffset = 0;
                        System.arraycopy(buffer, 0, framebuffer, frameOffset, count);
                        frameOffset += count;
                    }

                    // Find H264 head
                    int offset = VideoCoderHelper.findHead(framebuffer, frameOffset);
                    Log.i(TAG, "Find  Head:" + offset);
                    while (offset > 0) {
                        if (VideoCoderHelper.checkHead(framebuffer, 0)) {
                            // Fill decoder
                            boolean flag = onFrame(framebuffer, 0, offset);
                            if (flag) {
                                byte[] temp = framebuffer;
                                framebuffer = new byte[200000];
                                System.arraycopy(temp, offset, framebuffer,
                                        0, frameOffset - offset);
                                frameOffset -= offset;
                                Log.e(TAG, "Check is Head : " + offset);
                                // Continue finding head
                                offset = VideoCoderHelper.findHead(framebuffer, frameOffset);
                            }
                        } else {
                            offset = 0;
                        }
                    }
                    Log.d(TAG, "end loop");
                } else {
                    readFlag = false;
                    startReadFileThread();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            VideoCoderHelper.sleep(TIME_INTERNAL);
        }
    };

    private boolean initVideoBufferRead() {
        try {
            FileInputStream fileInputStream = new FileInputStream(videoFile);
            inputStream = new BufferedInputStream(fileInputStream);
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void startReadFileThread() {
        // Start a new thread
        readFileThread = new Thread(readFileRunnable);
        readFileThread.start();
    }

}
