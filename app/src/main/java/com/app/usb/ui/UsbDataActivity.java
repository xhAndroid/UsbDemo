package com.app.usb.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.SurfaceView;

import com.app.usb.R;
import com.link.usb.IUsbHostResponseListener;
import com.link.usb.UsbHostManager;

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
        UsbHostManager.getInstance().init(this);
        UsbHostManager.getInstance().setUsbHostResponseListener(this);

        surfaceView = findViewById(R.id.sv);
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
    public void onVideoOneResponse(byte[] data_bytes) {

    }

    @Override
    public void onVideoTwoResponse(byte[] data_bytes) {

    }
}
