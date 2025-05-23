package com.example.reproductorvideos;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;

public class TransparentExitActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        new Handler().postDelayed(() -> {
            finishAffinity();
            android.os.Process.killProcess(android.os.Process.myPid());
        }, 50);
    }
}

