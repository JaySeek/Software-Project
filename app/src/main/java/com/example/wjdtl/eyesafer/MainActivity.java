package com.example.wjdtl.eyesafer;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    //디버깅용
    private static final String TAG = "MainActivity";

    private String mConnectedDeviceName = null;

    // 인텐트 요청 코드(상수 정의)
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    private BluetoothAdapter mBluetoothAdapter = null; // BluetoothAdapter 클래스 접근 변수
    private BluetoothService mBluetoothService = null; // BluetoothService 클래스 접근 변수

    TextView txtv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        txtv = (TextView)findViewById(R.id.textView);

        if (mBluetoothAdapter == null) { // 기기에 Bluetooth 장치가 없을 경우
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // 블루투스가 Enable 상태인지 확인
        if (!mBluetoothAdapter.isEnabled()) {
            // 블루투스를 Enable 시키기 위한 암시적인 Intent 객체 생성
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            // Bluetooth 활성화 Activity(대화상자) 호출
            startActivityForResult(enableIntent,REQUEST_ENABLE_BT);
        }
        else if (mBluetoothService == null) {
            // Bluetooth 연결을 위해 BluetoothService 초기화
            mBluetoothService = new BluetoothService(this, mHandler);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mBluetoothService != null) {
            if (mBluetoothService.getState() == mBluetoothService.STATE_NONE) {
                mBluetoothService.start();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBluetoothService != null) {
            mBluetoothService.stop();
        }
    }

    @Override // startActivityforResult 에서 호출한 Activity 결과값 return 처리
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case REQUEST_CONNECT_DEVICE : // 블루투스 Connect 목적의 Intent 요청일 경우
                if(resultCode != 0)
                connectDevice(data);
                break;
            case REQUEST_ENABLE_BT: // 블루투스를 Enable 시키기 위한 요청 코드일 경우
                if(resultCode == Activity.RESULT_OK) {
                    // 켠다면
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this,"블루투스 ON",Toast.LENGTH_SHORT).show();
                }
                else { // 켜지 않는다면
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this,"블루투스 OFF 상태, \n앱을 종료합니다.",Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }

    // ActionBar 상태 변환(제목명)
    private void setStatus(CharSequence subTitle) {
        final ActionBar actionBar = this.getActionBar();
        actionBar.setSubtitle(subTitle);
    }

    // ActionBar 상태 변환(리소스 ID)
    private void setStatus(int resId) {
        final ActionBar actionBar = this.getActionBar();
        actionBar.setSubtitle(resId);
    }

    //BluetoothService로부터 오는 정보를 받아오는 Handler
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            setStatus(getString(R.string.connected_device_name) + mConnectedDeviceName);
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            setStatus(R.string.connecting);
                            break;
                        case BluetoothService.STATE_LISTEN:
                        case BluetoothService.STATE_NONE:
                            setStatus(R.string.not_connected);
                            break;
                    }
                    break;
                case Constants.MESSAGE_READ:
                    // 메시지로 넘어온 데이터로 스트링 만듬
                    String readMessage = (String)msg.obj;
                    Log.e("Message","readMess : " + readMessage);
                    if(readMessage.length() <= 5)
                    txtv.setText("현재 거리는 " + readMessage);
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // 장치명 저장
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    Toast.makeText(MainActivity.this, "연결 성공 : "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case Constants.MESSAGE_TOAST:
                    Toast.makeText(MainActivity.this, msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.device_scan: {
                Intent serverIntent = new Intent(MainActivity.this, DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
                return true;
            }
            case R.id.menu1: {
            }
            return false;
        }
        return super.onOptionsItemSelected(item);
    }

    private void connectDevice(Intent data) {
        // Intent 통해 전달된 MAC 주소 확인
        String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // MAC 주소 이용해 BluetoothDevice 객체 생성
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // 연결을 위한 메소드 호출
        mBluetoothService.connect(device);
    }
}
