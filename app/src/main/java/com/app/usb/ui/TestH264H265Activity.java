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
    private InputStream is = null;
    private FileInputStream fs = null;

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
    private final static int TIME_INTERNAL = 30;
    private final static int HEAD_OFFSET = 512;

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
        mReadButton.setText("Play : " + mimeType);
        mReadButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (videoFile.exists()) {
                    if (!isInit) {
                        initDecoder();
                        isInit = true;
                    }

                    readFileThread = new Thread(readFile);
                    readFileThread.start();
                } else {
                    Toast.makeText(getApplicationContext(), "H264 file not found", Toast.LENGTH_SHORT).show();
                }
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

    public void initDecoder() {
        try {
            mCodec = MediaCodec.createDecoderByType(mimeType);
            MediaFormat mediaFormat = MediaFormat.createVideoFormat(mimeType, VIDEO_WIDTH, VIDEO_HEIGHT);
            // 关键帧频率，请求关键帧之间的秒数间隔。
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 60);
            // 1 表示 停止播放时设置为黑色空白
            ///mediaFormat.setInteger(MediaFormat.KEY_PUSH_BLANK_BUFFERS_ON_STOP,  1);
            mCodec.configure(mediaFormat, mSurfaceView.getHolder().getSurface(), null, 0);
            mCodec.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    int mCount = 0;

    public boolean onFrame(byte[] buf, int offset, int length) {
        Log.e("Media", "onFrame start");
        Log.e("Media", "onFrame Thread:" + Thread.currentThread().getId());
        // Get input buffer index
        ByteBuffer[] inputBuffers = mCodec.getInputBuffers();
        int inputBufferIndex = mCodec.dequeueInputBuffer(100);

        Log.e("Media", "onFrame index:" + inputBufferIndex);
        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
            inputBuffer.clear();
            inputBuffer.put(buf, offset, length);
            mCodec.queueInputBuffer(inputBufferIndex, 0, length, mCount
                    * TIME_INTERNAL, 0);
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
        Log.e("Media", "onFrame end");
        return true;
    }

    /**
     * Find H264 frame head
     *
     * @param buffer
     * @param len
     * @return the offset of frame head, return 0 if can not find one
     */
    static int findHead(byte[] buffer, int len) {
        int i;
        for (i = HEAD_OFFSET; i < len; i++) {
            if (checkHead(buffer, i))
                break;
        }
        if (i == len)
            return 0;
        if (i == HEAD_OFFSET)
            return 0;
        return i;
    }

    /**
     * Check if is H264 frame head
     *
     * @param buffer
     * @param offset
     * @return whether the src buffer is frame head
     */
    static boolean checkHead(byte[] buffer, int offset) {
        // 00 00 00 01
        if (buffer[offset] == 0 && buffer[offset + 1] == 0
                && buffer[offset + 2] == 0 && buffer[3] == 1)
            return true;
        // 00 00 01
        if (buffer[offset] == 0 && buffer[offset + 1] == 0
                && buffer[offset + 2] == 1)
            return true;
        return false;
    }

    Runnable readFile = new Runnable() {

        @Override
        public void run() {
            int h264Read = 0;
            int frameOffset = 0;
            byte[] buffer = new byte[100000];
            byte[] framebuffer = new byte[200000];
            boolean readFlag = true;
            try {
                fs = new FileInputStream(videoFile);
                is = new BufferedInputStream(fs);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                readFlag = false;
            }
            while (!Thread.interrupted() && readFlag) {
                try {
                    int length = is.available();
                    if (length > 0) {
                        // Read file and fill buffer
                        int count = is.read(buffer);
                        Log.i("count", "" + count);
                        h264Read += count;
                        Log.d("Read", "count:" + count + " h264Read:"
                                + h264Read);
                        // Fill frameBuffer
                        if (frameOffset + count < 200000) {
                            System.arraycopy(buffer, 0, framebuffer,
                                    frameOffset, count);
                            frameOffset += count;
                        } else {
                            frameOffset = 0;
                            System.arraycopy(buffer, 0, framebuffer,
                                    frameOffset, count);
                            frameOffset += count;
                        }

                        // Find H264 head
                        int offset = findHead(framebuffer, frameOffset);
                        Log.i("find head", " Head:" + offset);
                        while (offset > 0) {
                            if (checkHead(framebuffer, 0)) {
                                // Fill decoder
                                boolean flag = onFrame(framebuffer, 0, offset);
                                if (flag) {
                                    byte[] temp = framebuffer;
                                    framebuffer = new byte[200000];
                                    System.arraycopy(temp, offset, framebuffer,
                                            0, frameOffset - offset);
                                    frameOffset -= offset;
                                    Log.e("Check", "is Head:" + offset);
                                    // Continue finding head
                                    offset = findHead(framebuffer, frameOffset);
                                }
                            } else {

                                offset = 0;
                            }

                        }
                        Log.d("loop", "end loop");
                    } else {
                        h264Read = 0;
                        frameOffset = 0;
                        readFlag = false;
                        // Start a new thread
                        readFileThread = new Thread(readFile);
                        readFileThread.start();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

                try {
                    Thread.sleep(TIME_INTERNAL);
                } catch (InterruptedException e) {

                }
            }
        }
    };
}
