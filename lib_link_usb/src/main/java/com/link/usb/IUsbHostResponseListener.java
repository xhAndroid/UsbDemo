package com.link.usb;

import androidx.annotation.NonNull;

/**
 * -----------------------------------------------------------------
 * Copyright (C) by AppMan, All rights reserved.
 * -----------------------------------------------------------------
 * USB 数据返回监听
 * @author AppMan
 * @date Created on 2020/09/14
 */
public interface IUsbHostResponseListener {

    /**
     * 透传飞控数据
     * @param data_bytes
     */
    void onUavDataResponse(byte[] data_bytes);

    /**
     * 视频一返回的数据
     * @param data_bytes
     */
    void onVideoOneResponse(@NonNull byte[] data_bytes, int length);
    /**
     * 视频二返回的数据
     * @param data_bytes
     */
    void onVideoTwoResponse(byte[] data_bytes);
}
