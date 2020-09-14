package com.app.usb.ui;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.os.Bundle;
import android.view.View;

import com.app.usb.R;

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
                break;
            case R.id.btn_h265:
                TestH264H265Activity.startActivity(this, true);
                break;
            case R.id.btn_usb_data:
                UsbDataActivity.startActivity(this);
                break;
        }
    }
}