package com.hcmus.thesis.nhatminhanhkiet.documentscanner.camera;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.hcmus.thesis.nhatminhanhkiet.documentscanner.R;

public class CameraActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, Camera2Fragment.newInstance())
                    .commit();
        }
    }
}
