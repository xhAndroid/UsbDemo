package com.link.usb;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;

import java.util.HashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * -----------------------------------------------------------------
 * Copyright (C) by AppMan, All rights reserved.
 * -----------------------------------------------------------------
 * USB 主机模式
 * https://developer.android.com/guide/topics/connectivity/usb/host?hl=zh-cn#java
 * <p>
 * UsbManager	您可以枚举连接的 USB 设备并与之通信。
 * UsbDevice	表示连接的 USB 设备，并包含用于访问其标识信息、接口和端点的方法。
 * UsbInterface	表示 USB 设备的接口，它定义设备的一组功能。设备可以具有一个或多个用于通信的接口。
 * UsbEndpoint	表示接口端点，是此接口的通信通道。一个接口可以具有一个或多个端点，并且通常具有用于与设备进行双向通信的输入和输出端点。
 * UsbDeviceConnection	表示与设备的连接，可在端点上传输数据。借助此类，您能够以同步或异步方式反复发送数据。
 *
 * @author AppMan
 * @date Created on 2020/08/31
 */
public class UsbHostManager {

    private static final String TAG = UsbHostManager.class.getSimpleName();

    private static UsbHostManager instance;

    public static UsbHostManager getInstance() {
        if (null == instance) {
            synchronized (UsbHostManager.class) {
                if (null == instance) {
                    instance = new UsbHostManager();
                }
            }
        }
        return instance;
    }

    private ScheduledThreadPoolExecutor mThreadPoolExecutor;

    private UsbHostManager() {
        mThreadPoolExecutor = new ScheduledThreadPoolExecutor(6);
    }

    private int interfaceCount = 0;
    private UsbInterface[] mInterfaceArray;
    private UsbDeviceConnection mDeviceConnection;
    /**
     * 给图传发送心跳包数据
     */
    private UsbEndpoint endpointOutSendData;
    /**
     * 飞控数据
     */
    private UsbEndpoint endpointInUavData;
    /**
     * 一路视频数据，一般只有一路视频的时候，只用这路
     * 主镜头视频数据
     */
    private UsbEndpoint endpointInVideoOneData;
    /**
     * 二路视频数据
     */
    private UsbEndpoint endpointInVideoTwoData;

    /**
     * 飞控数据buffer
     */
    private byte[] uavBuffer = new byte[512];

    private static final int VIDEO_BUFFER_LENGTH = 10 * 1024;
    /**
     * 视频1数据buffer
     */
    private byte[] videoOneBuffer = new byte[VIDEO_BUFFER_LENGTH];
    /**
     * 视频1数据buffer
     */
    private byte[] videoTwoBuffer = new byte[VIDEO_BUFFER_LENGTH];


    private boolean isUsbConnect = false;
    /**
     * USB 获取数据超时时间
     */
    private final static int POINT_TIMEOUT = 1000;

    public void init(Context context) {
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceHashMap = usbManager.getDeviceList();
        UsbDevice usbDevice = null;
        for (UsbDevice device : deviceHashMap.values()) {
//            Log.w(TAG, "init : " +device.toString());
            int vendorId = device.getVendorId();
            if (device.getVendorId() == 0x0000 || device.getVendorId() == 0x04b4 || device.getVendorId() == 0x0951 ||
                    device.getVendorId() == 0x483 || device.getVendorId() == 0xAAAA) {
                usbDevice = device;
                Log.i(TAG, "init 找到设备 : " + vendorId);
            }
        }
        if (null == usbDevice) {
            return;
        }
        boolean is_have_permission = usbManager.hasPermission(usbDevice);
        if (is_have_permission) {
            connectUsbAndFindEndPoint(usbManager, usbDevice);
        } else {
            requestPermission(context, usbManager, usbDevice);
            registerReceiver(context);
        }
    }

    private void connectUsbAndFindEndPoint(UsbManager usbManager, UsbDevice usbDevice) {
        Log.i(TAG, "有权限，去打开usb 连接");
        interfaceCount = usbDevice.getInterfaceCount();
        if (interfaceCount > 0) {
            mDeviceConnection = usbManager.openDevice(usbDevice);
            if (null == mDeviceConnection) {
                Log.e(TAG, "usb connection 获取失败");
                return;
            }
            mInterfaceArray = new UsbInterface[interfaceCount];
            for (int i = 0; i < interfaceCount; i++) {
                mInterfaceArray[i] = usbDevice.getInterface(i);
//                Log.v(TAG, i + ", " + mInterfaceArray[i]);
                findEndPoint(mInterfaceArray[i]);
            }
            //
            if (null != endpointInUavData && endpointOutSendData != null) {
                isUsbConnect = true;
                Log.v(TAG, "usb 已连接");
                startOutSendData();
                startCaptureUavStream();
                if (null != endpointInVideoOneData) {
                    startCaptureVideoOneStream();
                }
                if (null != endpointInVideoTwoData) {
                    startCaptureVideoTwoStream();
                }
            }
        }
    }

    private void findEndPoint(UsbInterface usbInterface) {
        boolean claim_interface = mDeviceConnection.claimInterface(usbInterface, true);
//        Log.i(TAG, "claim_interface = " + claim_interface);
        if (claim_interface) {
            int count_endpoint = usbInterface.getEndpointCount();
            for (int i = 0; i < count_endpoint; i++) {
                UsbEndpoint usb_endpoint = usbInterface.getEndpoint(i);
                int direction = usb_endpoint.getDirection();
                int number = usb_endpoint.getEndpointNumber();
//                Log.w(TAG, "direction = " + direction + ", number = " + number);
                if (UsbConstants.USB_DIR_IN == direction) {
                    switch (number) {
                        case 0x04:
                        case 0x08:
                            endpointInUavData = usb_endpoint;
                            break;
                        case 0x06:
                            endpointInVideoOneData = usb_endpoint;
                            break;
                        case 0x05:
                            endpointInVideoTwoData = usb_endpoint;
                            break;
                    }
                } else if (UsbConstants.USB_DIR_OUT == direction) {
                    if (0x01 == number) {
                        endpointOutSendData = usb_endpoint;
                    }
                }
            }
        } else {
            Log.e(TAG, "usb 连接打开接口失败 。 节点=" + usbInterface.toString());
        }
    }

    /**
     * 启动获取USB飞控数据流，子线程
     */
    private void startCaptureUavStream() {
        mThreadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                int length, msgId, data_length;
                while (isUsbConnect) {
                    length = mDeviceConnection.bulkTransfer(endpointInUavData, uavBuffer, uavBuffer.length, POINT_TIMEOUT);
                    if (length >= UsbHostConfig.LENGTH_HEAD) {
//                        Log.i(TAG, "uav data : " + ByteTransUtil.bytesToHexStr(uavBuffer));
                        if (UsbHostConfig.HEAD_FIRST == uavBuffer[0] && UsbHostConfig.HEAD_SECOND == uavBuffer[1]) {
                            msgId = LinkTransUtil.getUnsignedShort(uavBuffer[2], uavBuffer[3]);
                            data_length = LinkTransUtil.getUnsignedShort(uavBuffer[6], uavBuffer[7]);
                            if (data_length > 0 && data_length < length) {
                                byte[] data_bytes = new byte[data_length];
                                System.arraycopy(uavBuffer, UsbHostConfig.LENGTH_HEAD, data_bytes, 0, data_length);
                                Log.v(TAG, "transfer data : " + ByteTransUtil.bytesToHexStr(data_bytes));
                                switch (msgId) {
                                    case UsbHostConfig.MSG_ID_TRANSFER_DATA:
                                        // 飞控数据
                                        break;
                                    case UsbHostConfig.MSG_ID_GS_INFO:
                                        // 信号强度等信息
                                        break;
                                    case UsbHostConfig.MSG_ID_ENABLE_FREQUENCY:
                                        // 使能对频
                                        break;
                                    case UsbHostConfig.MSG_ID_DEVICE_INFO:
                                        // 图传频段信息
                                        break;
                                    case UsbHostConfig.MSG_ID_FREQUENCY_BAND:
                                        // 图传频段选择
                                        break;
                                }
                            }
                        }
                    }
                }
            }
        });
    }

    private byte[] transferBytes;
    private byte[] deviceInfoBytes;
    private byte[] gsInfoBytes;

    /**
     * 给USB发送数据，图传心跳包
     * 如果不发，表明没有连接，图传就不会传输数据到USB
     */
    private void startOutSendData() {
        mThreadPoolExecutor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                if (isUsbConnect) {
                    if (null == transferBytes) {
                        transferBytes = UsbHostConfig.packMsgIdBytes(UsbHostConfig.MSG_ID_TRANSFER_DATA);
                    }
                    if (null == deviceInfoBytes) {
                        deviceInfoBytes = UsbHostConfig.packMsgIdBytes(UsbHostConfig.MSG_ID_DEVICE_INFO);
                    }
                    if (null == gsInfoBytes) {
                        gsInfoBytes = UsbHostConfig.packMsgIdBytes(UsbHostConfig.MSG_ID_GS_INFO);
                    }
                    mDeviceConnection.bulkTransfer(endpointOutSendData, transferBytes, transferBytes.length, POINT_TIMEOUT);
                    mDeviceConnection.bulkTransfer(endpointOutSendData, deviceInfoBytes, deviceInfoBytes.length, POINT_TIMEOUT);
                    mDeviceConnection.bulkTransfer(endpointOutSendData, gsInfoBytes, gsInfoBytes.length, POINT_TIMEOUT);
                }
            }
        }, 500, 300, TimeUnit.MILLISECONDS);
    }

    private void startCaptureVideoOneStream() {
        mThreadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                int length;
                while (isUsbConnect) {
                    length = mDeviceConnection.bulkTransfer(endpointInVideoOneData, videoOneBuffer, VIDEO_BUFFER_LENGTH, POINT_TIMEOUT);
                    if (length > 0) {
                        Log.i(TAG, "video one length = " + length);
                    }
                }
            }
        });
    }

    private void startCaptureVideoTwoStream() {
        mThreadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                int length;
                while (isUsbConnect) {
                    length = mDeviceConnection.bulkTransfer(endpointInVideoTwoData, videoTwoBuffer, VIDEO_BUFFER_LENGTH, POINT_TIMEOUT);
                    if (length > 0) {
                        Log.i(TAG, "video two length = " + length);
                    }
                }
            }
        });
    }

    private void requestPermission(Context context, UsbManager usbManager, UsbDevice usbDevice) {
        PendingIntent permissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
        usbManager.requestPermission(usbDevice, permissionIntent);
        Log.i(TAG, "usb没有权限，开始申请权限");
    }

    private void registerReceiver(Context context) {
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        context.registerReceiver(usbReceiver, filter);
    }

    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                // 请求USB授权使用
                case ACTION_USB_PERMISSION:
                    synchronized (this) {
                        UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                        if (device != null) {
                            boolean is_granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);
                            Log.d(TAG, is_granted + ", ACTION_USB_PERMISSION : " + device.toString());
                        }
                    }
                    break;
                // 发现USB设备
                case UsbManager.ACTION_USB_DEVICE_ATTACHED:
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (device != null) {
                        Log.d(TAG, "ACTION_USB_DEVICE_ATTACHED : " + device.toString());
                    } else {
                        Log.w(TAG, "ACTION_USB_DEVICE_ATTACHED : " + device);
                    }
                    break;
                // 终止USB设备的通信
                case UsbManager.ACTION_USB_DEVICE_DETACHED:
                    UsbDevice device2 = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (device2 != null) {
                        Log.d(TAG, "ACTION_USB_DEVICE_DETACHED : " + device2.toString());
                    } else {
                        Log.w(TAG, "ACTION_USB_DEVICE_DETACHED : " + device2);
                    }
                    break;
            }
        }
    };

}
