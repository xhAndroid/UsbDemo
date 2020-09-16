package com.app.usb.ui;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.app.usb.R;
import com.app.usb.media.ByteInOutBuffer;
import com.app.usb.media.MediaParser;
import com.link.usb.ByteTransUtil;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestPermissions();
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(
                this, new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, 0
        );
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_h264:
                TestH264H265Activity.startActivity(this, false);
                testWrite();
                break;
            case R.id.btn_h265:
                TestH264H265Activity.startActivity(this, true);
                testRead();
                break;
            case R.id.btn_usb_data:
                UsbDataActivity.startActivity(this);
//                startRw();
                break;
        }
    }

    private void startRw() {
        if (isTestRw) {
            return;
        }
        isTestRw = true;
        new Thread(writeRunner).start();
        new Thread(readRunner).start();;
    }

    private boolean isTestRw = false;

    private Runnable writeRunner = () -> {
        while (isTestRw) {
            testWrite();
            sleep(5);
        }
    };

    private Runnable readRunner = () -> {
        while (isTestRw) {
            testRead();
            sleep(5);
        }
    };

    private byte[] testBytes = new byte[]{(byte) 0xA1, (byte) 0xA2, (byte) 0xA3, (byte) 0xA4, (byte) 0xA5,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x41, (byte) 0xB1, (byte) 0xB2, (byte) 0xB3, (byte) 0xB4, (byte) 0xB5, (byte) 0xB6,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x41, (byte) 0xD1, (byte) 0xD2, (byte) 0xD3, (byte) 0xD4, (byte) 0xD5, (byte) 0xD6, (byte) 0xD7,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x41, (byte) 0xF1, (byte) 0xF2, (byte) 0xF3, (byte) 0xF4, (byte) 0xF5, (byte) 0xF6, (byte) 0xF7, (byte) 0xF8
    };

    private ByteInOutBuffer inOutBuffer = new ByteInOutBuffer();

    private void testWrite() {
        inOutBuffer.write(testBytes, testBytes.length);
    }

    private MediaParser mediaParser = new MediaParser();

    private void testRead() {
        if (inOutBuffer.isCanRead()) {
            int parser_result;
            for (int i = 0; i < inOutBuffer.getValidSize(); i++) {
                byte b = inOutBuffer.getByteByIndex(i);
                parser_result = mediaParser.parseByte(b);
//                Log.v("TEST", i + ", readIndex = " + inOutBuffer.getReadIndex() + ", " + ByteTransUtil.byteToHexStr(b));
                if (parser_result > 0) {
                    byte[] frame_bytes = new byte[parser_result];
                    inOutBuffer.read(frame_bytes, parser_result);
                    Log.e("TEST", "parser_result = " + parser_result + " : " + ByteTransUtil.byteToHexStr(frame_bytes));
                    break;
                }
            }
        }
    }

    private void sleep(long time_ms) {
        try {
            Thread.sleep(time_ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}