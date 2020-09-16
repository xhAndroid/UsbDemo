package com.app.usb.media;

import android.util.Log;

/**
 * -----------------------------------------------------------------
 * Copyright (C) by AppMan, All rights reserved.
 * -----------------------------------------------------------------
 * byte 数据读写缓存
 *
 * @author AppMan
 * @date Created on 2020/09/15
 */
public class ByteInOutBuffer {

    private static final String TAG = ByteInOutBuffer.class.getSimpleName();

    /**
     * 队列大小 5 M
     */
    private static final int BUFFER_SIZE = 1000 * 1000;
    /**
     * 缓存 byte[]
     */
    private byte[] buffer = new byte[BUFFER_SIZE];
    /**
     * 读索引
     */
    private int readIndex = 0;
    /**
     * 写索引
     */
    private int writeIndex = 0;

    /**
     * 有效字节数
     *
     * @return
     */
    public int getValidSize() {
        if (writeIndex < readIndex) {
            return BUFFER_SIZE - readIndex + writeIndex;
        } else {
            return writeIndex - readIndex;
        }
    }

    public boolean isCanRead() {
        return getValidSize() > 0;
    }

    public byte getByteByIndex(int i) {
        return buffer[(readIndex + i) % BUFFER_SIZE];
    }

    public int getReadIndex() {
        return readIndex;
    }

    /**
     * 读数据
     *
     * @param put_bytes   存放读取的字节
     * @param want_length 希望读取的字节长度
     * @return
     */
    public int read(byte[] put_bytes, int want_length) {
        //有效字节数
        int valid_size = getValidSize();
        if (valid_size <= 0 || want_length <= 0) {
            // 有效字节小于需要读字节，则返回，先不读
            return 0;
        }

        if (valid_size < want_length) {
            return 0;
        } else {
            int right_size = BUFFER_SIZE - readIndex;
            if (right_size >= want_length) {
                // 写index 大于 读index，复制可复制字节
                System.arraycopy(buffer, readIndex, put_bytes, 0, want_length);
                readIndex += want_length;
            } else {
                // 复制右边字节
                System.arraycopy(buffer, readIndex, put_bytes, 0, right_size);
                // 复制左边可复制字节
                System.arraycopy(buffer, 0, put_bytes, right_size, want_length - right_size);
                readIndex = want_length - right_size;
            }
            return want_length;
        }
    }

    /**
     * 写数据
     *
     * @param data_bytes 承载数据数组
     * @param length     有效数据索引
     */
    public void write(byte[] data_bytes, int length) {
        if (null == data_bytes || length > data_bytes.length || 0 >= length) {
            return;
        }
        //
        int can_write_right_size = BUFFER_SIZE - writeIndex;
        if (can_write_right_size >= length) {
            System.arraycopy(data_bytes, 0, buffer, writeIndex, length);
            writeIndex += length;
//            Log.w(TAG, "length = " + length + ", can_write_right_size = " + can_write_right_size + ", writeIndex = " + writeIndex);
        } else {
            // 复制到右边
            System.arraycopy(data_bytes, 0, buffer, writeIndex, can_write_right_size);
            // 重新复制到左边
            System.arraycopy(data_bytes, can_write_right_size, buffer, 0, length - can_write_right_size);
            writeIndex = length - can_write_right_size;
//            Log.i(TAG, "length = " + length + ", can_write_right_size = " + can_write_right_size + ", writeIndex = " + writeIndex);
        }
    }

}
