package com.link.usb;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;

import java.util.HashMap;

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

    private UsbHostManager() {
    }

    private UsbDeviceConnection connection;
    private UsbEndpoint endpoint;

    private byte[] bytes = new byte[1024];
    private static int TIMEOUT = 0;
    private boolean forceClaim = true;

    public void init(Context context) {
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceHashMap = usbManager.getDeviceList();
        UsbDevice usbDevice = null;
        for (UsbDevice device : deviceHashMap.values()) {
            Log.w(TAG, device.toString());
            int vendorId = device.getVendorId();
            if (device.getVendorId() == 0x0000 || device.getVendorId() == 0x04b4 || device.getVendorId() == 0x0951 ||
                    device.getVendorId() == 0x483 || device.getVendorId() == 0xAAAA) {
                usbDevice = device;
                Log.i(TAG, "找到设备 : " + vendorId);
            }
        }

        if (null == usbDevice) {
            return;
        }

        PendingIntent permissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        context.registerReceiver(usbReceiver, filter);
        usbManager.requestPermission(usbDevice, permissionIntent);

        UsbInterface intf = usbDevice.getInterface(0);
        endpoint = intf.getEndpoint(0);
        connection = usbManager.openDevice(usbDevice);
        connection.claimInterface(intf, forceClaim);
        //
        new Thread(new Runnable() {
            @Override
            public void run() {
                connection.bulkTransfer(endpoint, bytes, bytes.length, TIMEOUT); //do in another thread
                Log.w(TAG, bytes[0] + "");
            }
        }).start();
    }

    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case ACTION_USB_PERMISSION:
                    synchronized (this) {
                        UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                        if (device != null) {
                            boolean is_granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);
                            Log.d(TAG, is_granted + ", ACTION_USB_PERMISSION : " + device.toString());
                        }
                    }
                    break;

                //
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
