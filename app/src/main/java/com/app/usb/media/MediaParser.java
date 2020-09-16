package com.app.usb.media;

/**
 * -----------------------------------------------------------------
 * Copyright (C) by AppMan, All rights reserved.
 * -----------------------------------------------------------------
 *
 * @author AppMan
 * @date Created on 2020/09/16
 */
public class MediaParser {

    enum States {
        PARSE_IDLE,
        PARSE_STEP_FIRST,
        PARSE_STEP_SECOND,
        PARSE_STEP_THIRD,
        PARSE_STEP_FOUR,
        PARSE_GET_OTHER
    }

    private States state = States.PARSE_IDLE;

    private static final byte H_00 = (byte) 0x00;
    private static final byte H_01 = (byte) 0x01;
    private static final byte H_41 = (byte) 0x41;
    private static final byte H_61 = (byte) 0x61;
    private static final byte H_65 = (byte) 0x65;
    private static final byte H_67 = (byte) 0x67;

    private static final int HEAD_LENGTH = 5;

    private static final int NOT_GOOD = -1;
    private static final int MAX_ONE_FRAME_SIZE = 50 * 1024;
    private static final int MIN_ONE_FRAME_SIZE = 200;
    /**
     * 一帧的大小
     */
    private int oneFrameSize = 0;
    /**
     * 返回大小
     */
    private int resultSize = NOT_GOOD;
    /**
     * 第一次发现帧
     */
    private boolean isNotFoundFirstFrame = true;
    /**
     * 非第一次，正常帧头部发现
     */
    private boolean isHeadFound = false;
    private boolean isInSendCondHead = false;

    /**
     * @param b
     * @return
     */
    public int parseByte(byte b) {
        oneFrameSize++;
        resultSize = NOT_GOOD;
        switch (state) {
            case PARSE_IDLE:
                if (H_00 == b) {
                    state = States.PARSE_STEP_FIRST;
                } else {
                    state = States.PARSE_IDLE;
                }
                break;
            case PARSE_STEP_FIRST:
                if (H_00 == b) {
                    state = States.PARSE_STEP_SECOND;
                } else {
                    state = States.PARSE_IDLE;
                }
                break;
            case PARSE_STEP_SECOND:
                if (H_00 == b) {
                    state = States.PARSE_STEP_THIRD;
                } else {
                    state = States.PARSE_IDLE;
                }
                break;
            case PARSE_STEP_THIRD:
                if (H_01 == b) {
                    state = States.PARSE_STEP_FOUR;
                } else {
                    state = States.PARSE_IDLE;
                }
                break;
            case PARSE_STEP_FOUR:
                // 当发现一帧时，将计数结果返回
                if (H_41 == b || H_61 == b || H_65 == b || H_67 == b) {
                    // 没有发现第一帧
                    if (isNotFoundFirstFrame) {
                        isNotFoundFirstFrame = false;
                        resultSize = oneFrameSize - HEAD_LENGTH;
                        oneFrameSize = HEAD_LENGTH + 1;
                        isHeadFound = true;
                    } else {
                        // 正常发现帧
                        if (isHeadFound) {
                            isHeadFound = false;
                            if (isInSendCondHead) {
                                resultSize = oneFrameSize - HEAD_LENGTH;
                            } else {
                                resultSize = oneFrameSize;
                            }
                            oneFrameSize = 0;
                        } else {
                            isHeadFound = true;
                            isInSendCondHead = true;
                        }
                    }
                }
                state = States.PARSE_IDLE;
                break;
        }
        return resultSize;
    }

}
