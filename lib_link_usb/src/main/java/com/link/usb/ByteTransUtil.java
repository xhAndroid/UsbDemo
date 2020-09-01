package com.link.usb;

/**
 * byte 类型转换类
 */
public final class ByteTransUtil {

    /**
     * bytes转换成十六进制字符串
     *
     * @param bArr byte数组
     * @return String 每个Byte值之间空格分隔
     */
    public static String bytesToHexStr(byte[] bArr) {
        if (bArr == null) {
            return null;
        }
        String stmp;
        StringBuilder sb = new StringBuilder("");
        for (int n = 0; n < bArr.length; n++) {
            stmp = Integer.toHexString(bArr[n] & 0xFF);
            sb.append((stmp.length() == 1) ? "0" + stmp : stmp);
            sb.append(" ");
        }
        return sb.toString().toUpperCase().trim();
    }

    public static String bytesToHexNoEmptyStr(byte[] bArr) {
        if (bArr == null) {
            return null;
        }
        String stmp;
        StringBuilder sb = new StringBuilder("");
        for (int n = 0; n < bArr.length; n++) {
            stmp = Integer.toHexString(bArr[n] & 0xFF);
            sb.append((stmp.length() == 1) ? "0" + stmp : stmp);
        }
        return sb.toString().toUpperCase().trim();
    }

    public static String byteToHexStr(byte b) {
        return bytesToHexStr(new byte[]{b});
    }

    /**
     * Convert hex string to byte[]
     *
     * @param hexString the hex string
     * @return byte[]
     */
    public static byte[] hexStringToBytes(String hexString) {
        if (hexString == null || hexString.equals("")) {
            return null;
        }
        hexString = hexString.toUpperCase();
        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] d = new byte[length];
        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
        }
        return d;
    }

    /**
     * Convert char to byte
     *
     * @param c char
     * @return byte
     */
    private static byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }

    /**
     * 通过byte数组(两位)取到short
     *
     * @param bArr
     * @param index
     *            第几位开始取
     * @return
     */
    public static short bytesToShort(byte[] bArr, int index) {
        return (short) (((bArr[index + 1] << 8) | bArr[index + 0] & 0xff));
    }

    public static short bytesToShort(byte[] bArr) {
        return bytesToShort(bArr, 0);
    }

    /**
     * 将short转换为两个字节的byte数组(低位在前)
     *
     * @param s
     * @return byte[] 长度为2 (低位在前)
     */
	/*public static byte[] shortToBytes(short s) {
		byte[] bArr = new byte[]{0, 0};
		bArr[0] = (byte) ((s << 8) >> 8); // 低位
		bArr[1] = (byte) (s >> 8); // 高位
		return bArr;
	}*/

    /**
     * 将16位的short转换成byte数组(长度为2)
     *
     * @param s short
     * @return byte[] 长度为2 (低位在前)
     * */
    public static byte[] shortToBytes(short s) {
        byte[] bArr = new byte[2];
        bArr[0] = (byte) ((s << 8) >> 8); // 低8位
        bArr[1] = (byte) (s >> 8); // 高8位
        return bArr;
    }

    /**
     * byte数组中取int数值，本方法适用于(低位在前，高位在后)的顺序，和和intToBytes（）配套使用
     *
     * @param bArr
     *            byte数组[4]，四位数组
     * @param offset
     *            从数组的第offset位开始
     * @return int数值
     */
    public static int bytesToInt(byte[] bArr, int offset) {
        int value;
        value = ((bArr[offset] & 0xFF)
                | ((bArr[offset+1] & 0xFF)<<8)
                | ((bArr[offset+2] & 0xFF)<<16)
                | ((bArr[offset+3] & 0xFF)<<24));
        return value;
    }

    public static int bytesToInt(byte[] bArr) {
        return bytesToInt(bArr, 0);
    }

    /**
     * 将int转换成byte数组(长度为4) 高位在前
     * @param value
     * @return
     */
    public static byte[] intToBytes(int value) {
        return new byte[]{
                (byte) value,
                (byte) (value >>> 8),
                (byte) (value >>> 16),
                (byte) (value >>> 24),
        };
    }

    /**
     * byte数组中取int数值，本方法适用于(低位在前，高位在后)的顺序。
     *
     * @param ary
     *            byte数组[4]，四位数组
     * @param offset
     *            从数组的第offset位开始
     * @return int数值
     */
    /*public static int bytesToInt(byte[] ary, int offset) {
        int value;
        value = (int) ((ary[offset]&0xFF)
                | ((ary[offset+1]<<8) & 0xFF00)
                | ((ary[offset+2]<<16)& 0xFF0000)
                | ((ary[offset+3]<<24) & 0xFF000000));
        return value;
    }*/

    /**
     * 通过byte数组(4位)取得float
     *
     * @param bArr
     * @return
     */
    public static float bytesToFloat(byte[] bArr) {
        int value;
        value = bArr[0];
        value &= 0xff;
        value |= ((long) bArr[1] << 8);
        value &= 0xffff;
        value |= ((long) bArr[2] << 16);
        value &= 0xffffff;
        value |= ((long) bArr[3] << 24);
        return Float.intBitsToFloat(value);
    }

    /**
     * 通过byte数组[8] 八位数组，取得double
     *
     * @param bArr
     * @return
     */
    public static double bytesToDouble(byte[] bArr) {
        long tLong;
        tLong = bArr[0];
        tLong &= 0xff;
        tLong |= ((long) bArr[1] << 8);
        tLong &= 0xffff;
        tLong |= ((long) bArr[2] << 16);
        tLong &= 0xffffff;
        tLong |= ((long) bArr[3] << 24);
        tLong &= 0xffffffffL;
        tLong |= ((long) bArr[4] << 32);
        tLong &= 0xffffffffffL;
        tLong |= ((long) bArr[5] << 40);
        tLong &= 0xffffffffffffL;
        tLong |= ((long) bArr[6] << 48);
        tLong &= 0xffffffffffffffL;
        tLong |= ((long) bArr[7] << 56);
        return Double.longBitsToDouble(tLong);
    }

    /**
     * double 转 byte数组[8] 八位数组
     * @param value
     * @return
     */
    public static byte[] doubleToBytes(double value) {
        long accum = Double.doubleToRawLongBits(value);
        byte[] byteRet = new byte[8];
        byteRet[0] = (byte) (accum & 0xFF);
        byteRet[1] = (byte) ((accum >> 8) & 0xFF);
        byteRet[2] = (byte) ((accum >> 16) & 0xFF);
        byteRet[3] = (byte) ((accum >> 24) & 0xFF);
        byteRet[4] = (byte) ((accum >> 32) & 0xFF);
        byteRet[5] = (byte) ((accum >> 40) & 0xFF);
        byteRet[6] = (byte) ((accum >> 48) & 0xFF);
        byteRet[7] = (byte) ((accum >> 56) & 0xFF);
        return byteRet;
    }

     /*
    // 测试拆分高低位
    private static void test(int len_frame) {
        if (len_frame > 0) {
            byte low8b = (byte) ((len_frame << 8) >> 8); // 低8位
            byte height8b = (byte) (len_frame >> 8); // 高8位
            Logcat.v("low8b---" + low8b);
            Logcat.v("height8b---" + height8b);
        }
    }*/

}
