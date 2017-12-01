package com.example.wjdtl.eyesafer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.WindowManager;

public class BrightnessActivity extends Activity {

    private static final int DELAYED_MESSAGE = 1;

    private Handler handler;

    private String finishTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = this.getIntent();
        float brightness = intent.getFloatExtra("window bright", 0);
        finishTask = intent.getStringExtra("finish Task");
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = brightness;
        getWindow().setAttributes(lp);

        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if(msg.what == DELAYED_MESSAGE) {
                    if(finishTask.equals("finish"))
                        BrightnessActivity.this.finish();
                    else if(finishTask.equals("background"))
                        BrightnessActivity.this.moveTaskToBack(true);
                }
                super.handleMessage(msg);
            }
        };

        Message message = handler.obtainMessage(DELAYED_MESSAGE);
        handler.sendMessageDelayed(message,1);
    }
}
