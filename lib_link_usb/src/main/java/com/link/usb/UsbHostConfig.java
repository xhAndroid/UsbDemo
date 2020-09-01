package com.link.usb;

/**
 * -----------------------------------------------------------------
 * Copyright (C) by AppMan, All rights reserved.
 * -----------------------------------------------------------------
 * 参考文档：ARTOSYN C201 模组 USB协议说明
 *
 * @author AppMan
 * @date Created on 2020/09/01
 */
class UsbHostConfig {

    /**
     * 数据包格式
     * Byte0  Byte1  Byte2  Byte3  Byte4  Byte5  Byte6  Byte7  Byte8  Byte9  Byte10
     * 0xFF   0x5A   0x5B   0x00   0x01   0x00   0x01   0x00   0x01   0x00   0x01
     * Byte[0~1]: 包头，固定 0xFF 0x5A
     * Byte[2~3]: Message ID, Little-Endian, 0x005B
     * Byte 4 : NUM SUM, 表示该条命令包，只有一包 0x01
     * Byte 5 : index 0x00
     * Byte[6~7]: Payload 长度，Little-Endian, 0x0001
     * Byte[8~9]：Check Sum, Little-Endian, 0x0001， Payload 的数据累加
     * Check_Sum = (byte10 & 0x0FFFF);
     * Byte 10 : Payload 数据，可以没有
     */

    public static final int MIN_ARTOSYN_LENGTH = 10;

    /**
     * 包头
     */
    public static final byte HEART_FIRST = (byte) 0xFF;
    public static final byte HEART_SECOND = (byte) 0x5A;



    /**
     * 2.1 频段选择
     */
    public static final byte MSG_ID_FREQUENCY_BAND = (byte) 0x22;
    /**
     * 2.20 图传设备信息
     */
    public static final byte MSG_ID_DEVICE_INFO = (byte) 0x19;
    /**
     * 2.21 模组地面端状态信息
     */
    public static final byte MSG_ID_GS_INFO = (byte) 0x82;
    /**
     * 2.24 USB 透传读取数据
     */
    public static final byte MSG_ID_TRANSFER_DATA = (byte) 0x85;
    /**
     * 2.6 使能对频
     */
    public static final byte MSG_ID_ENABLE_FREQUENCY = (byte) 0x5B;

}
