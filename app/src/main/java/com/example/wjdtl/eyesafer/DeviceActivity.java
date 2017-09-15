package com.example.wjdtl.eyesafer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class DeviceActivity extends Activity {

    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String address = "34:FC:EF:10:DD:80";

        // Intent 객체에 MAC 주소 추가
        Intent intent = new Intent();
        intent.putExtra(EXTRA_DEVICE_ADDRESS, address);

        // 메인 액티비티로 응답
        setResult(Activity.RESULT_OK, intent);
        finish();
    }
}
