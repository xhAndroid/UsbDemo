package com.link.usb;

/**
 * -----------------------------------------------------------------
 * Copyright (C) by AppMan, All rights reserved.
 * -----------------------------------------------------------------
 *
 * @author AppMan
 * @date Created on 2020/09/01
 */
class LinkTransUtil {

    public static int getUnsignedShort(byte low_bytes, byte high_bytes) {
        return (low_bytes & 0x0FF) + ((high_bytes & 0x0FF) << 8);
    }
}
