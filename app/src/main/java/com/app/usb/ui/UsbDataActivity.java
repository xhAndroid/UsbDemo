package com.app.usb.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.app.usb.R;
import com.app.usb.media.MediaCodecHelper;
import com.link.usb.IUsbHostResponseListener;
import com.link.usb.UsbHostManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * -----------------------------------------------------------------
 * Copyright (C) by AppMan, All rights reserved.
 * -----------------------------------------------------------------
 * 解析USB主模式返回的视频等数据
 *
 * @author AppMan
 * @date Created on 2020/09/14
 */
public class UsbDataActivity extends AppCompatActivity implements IUsbHostResponseListener {

    public static void startActivity(Context context) {
        context.startActivity(new Intent(context, UsbDataActivity.class));
    }

    private SurfaceView surfaceView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usb_data);
        surfaceView = findViewById(R.id.sv);
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {

            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                mediaCodecHelper = new MediaCodecHelper(holder.getSurface(), width, height);
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });
        //
        initData();
    }

    private MediaCodecHelper mediaCodecHelper;

    private void initData() {
        //
        UsbHostManager.getInstance().init(this);
        UsbHostManager.getInstance().setUsbHostResponseListener(this);
    }

    @Override
    protected void onDestroy() {
        UsbHostManager.getInstance().releaseUsb(this);
        super.onDestroy();
    }

    @Override
    public void onUavDataResponse(byte[] data_bytes) {

    }

    @Override
    public void onVideoOneResponse(@NonNull byte[] data_bytes, int length) {
//        Log.v("TAG", "onVideoOneResponse, length = " + length);
        if (null != mediaCodecHelper) {
            mediaCodecHelper.respVideoData(data_bytes, length);
        }
    }

    @Override
    public void onVideoTwoResponse(@NonNull byte[] data_bytes, int length) {

    }
}
