package com.dlbarron.dlbwatchface;

import android.app.Activity;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;

public class DarkActivity extends Activity {
    private static final String TAG="DarkActivity";
    private float originalBrightness;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dark);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                WindowManager.LayoutParams settings = getWindow().getAttributes();
                originalBrightness = settings.screenBrightness;
                WindowManager.LayoutParams params = getWindow().getAttributes();
                params.screenBrightness = 0.0f;
                params.flags = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
                getWindow().setAttributes(params);
                LinearLayout screen = (LinearLayout) findViewById(R.id.screen);
                screen.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        WindowManager.LayoutParams params = getWindow().getAttributes();
                        params.screenBrightness = originalBrightness;
                        getWindow().setAttributes(params);
                        finish();
                    }
                });
            }
        });
    }
}
