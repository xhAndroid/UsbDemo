package com.app.usb.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.app.usb.R;
import com.link.usb.UsbHostManager;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        UsbHostManager.getInstance().init(this);
    }
}