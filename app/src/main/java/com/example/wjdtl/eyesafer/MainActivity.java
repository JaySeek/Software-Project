package com.example.wjdtl.eyesafer;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.tsengvn.typekit.TypekitContextWrapper;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity {

    //디버깅용
    private static final String TAG = "MainActivity";

    // 인텐트 요청 코드(상수 정의)
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    private BluetoothAdapter mBluetoothAdapter = null; // BluetoothAdapter 클래스 접근 변수
    private BluetoothService mBluetoothService = null; // BluetoothService 클래스 접근 변수

    private int warnCount = 0; // 경고 횟수
    private int distTimeCounter = 0; // 거리 경고 시간 체크 카운터
    private int tTimeCounter = 0; // 사용 시간 체크 카운터
    private int rTimeCounter = 0; // 휴식 시간 체크 카운터

    private static final int T_TIME_LIMIT = 40; // 사용 시간 제한 기준(초)
    private static final int T_TIME_REST = 5; // 휴식 시간 기준(초)

    private boolean isSafeDist = true;  // 안전 거리 유지 여부
    private boolean isNeedRest = false; // 휴식 필요 여부
    private boolean isRestComp = true; // 휴식 완료 여부
    private boolean isRestMegPr = false; // 휴식 더 필요하다는 메시지 출력 여부
    private boolean isAutoBright = false; // 자동 밝기 ON/OFF 여부
    private boolean isCompMsg = false; // 휴식 완료 메시지 출력 여부

    private int currentBright = 0; // 현재 밝기 값
    private int alertCount = 0;

    private int distVioCount = 0;
    private int timeVioCount = 0;

    String alertTitle, alertCont;

    PowerManager pm;
    NotificationManager mNotiManager;
    Notification.Builder distNoti;

    TimerTask distTimerTask, tTimerTask;
    Timer distTimer, tTimer;

    TextView nowDistTxt, nowDistInfoTxt, nowTimeTxt, distViolationTxt, timeViolationTxt;
    TextView toastTitle, toastCont;

    Toast toastAlert;

    ImageView distImage, timeImage, alertImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

        // Notification

        mNotiManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        distNoti = new Notification.Builder(this);
        distNoti.setSmallIcon(R.drawable.ic_noti_alert);
        distNoti.setContentTitle("기본 타이틀");
        distNoti.setContentText("기본 내용");
        distNoti.setAutoCancel(true);

        // Main Activity 뷰

        nowDistTxt = (TextView) findViewById(R.id.nowDistTxt);
        nowTimeTxt = (TextView) findViewById(R.id.nowUseTimeTxt);
        nowDistInfoTxt = (TextView) findViewById(R.id.nowDistInfoTxt);

        distImage = (ImageView)findViewById(R.id.distImageView);
        timeImage = (ImageView)findViewById(R.id.timeImageView);

        distViolationTxt = (TextView)findViewById(R.id.distViolationTxt);
        timeViolationTxt = (TextView)findViewById(R.id.timeViolationTxt);

        // Custon Toast

        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.custom_toast_layout,(ViewGroup)findViewById(R.id.custom_toast_layout_main));

        toastTitle = (TextView)layout.findViewById(R.id.toastTitle);
        toastCont = (TextView)layout.findViewById(R.id.toastContents);

        alertImage = (ImageView)layout.findViewById(R.id.toastImage);
        alertImage.setImageResource(R.drawable.ic_noti_alert);

        toastAlert = new Toast(getApplicationContext());
        toastAlert.setGravity(Gravity.CENTER,0,0);
        toastAlert.setDuration(Toast.LENGTH_SHORT);
        toastAlert.setView(layout);

        // 사용 시간 알림 Task

        tTimerTask = new TimerTask() {
            @Override
            public void run() {
                if (pm.isScreenOn()) {
                    if (isCompMsg) {
                        tHandler.sendEmptyMessage(3);
                        isCompMsg = false;
                    }
                    tHandler.sendEmptyMessage(0);
                    tTimeCounter++;
                    if (tTimeCounter == T_TIME_LIMIT) { // 제한이 필요한 시간이 누적될 경우 알림
                        tHandler.sendEmptyMessage(1);
                        isNeedRest = true;

                    }
                    if (tTimeCounter > T_TIME_LIMIT && isNeedRest)
                        tTimeCounter /= 2; // 정해진 시간의 휴식을 취하지 않고 사용할 경우 제한 시간의 절반만 사용해도 알림 출력
                    if (isNeedRest && !isRestComp && !isRestMegPr)  // 정해진 휴식 시간을 채우지 못한 경우 1회에 한하여 출력
                        tHandler.sendEmptyMessage(2);
                } else {
                    if (isNeedRest) {
                        rTimeCounter++;
                        if (rTimeCounter >= T_TIME_REST) { // 설정한 휴식 시간에 도달
                            tTimeCounter = 0;
                            rTimeCounter = 0;
                            isNeedRest = false;
                            isRestComp = true;
                            isRestMegPr = false;
                            isCompMsg = true;
                        } else { // 설정한 휴식 시간에 도달 X
                            isRestComp = false;
                        }
                    } else // 휴식할 필요가 없지만 충분한 휴식을 취할 경우
                        if (rTimeCounter >= T_TIME_REST) {
                            tTimeCounter = 0;
                            rTimeCounter = 0;
                            isNeedRest = false;
                            isRestComp = true;
                        }
                }
            }
        };
        tTimer = new Timer();
        tTimer.schedule(tTimerTask, 0, 1000);

        if (mBluetoothAdapter == null) { // 기기에 Bluetooth 장치가 없을 경우
            Toast.makeText(this, "블루투스 기능이 없는 장치입니다.", Toast.LENGTH_LONG).show();
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
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else if (mBluetoothService == null) {
            // 이미 Enable 상태라면 Bluetooth 연결을 위해 BluetoothService 초기화
            mBluetoothService = new BluetoothService(this, mHandler);
            connectActivity();
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

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                return true; // BACK 버튼 눌러도 앱 종료 X
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override // startActivityforResult 에서 호출한 Activity 결과값 return 처리
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE: // 블루투스 Connect 목적의 Intent 요청일 경우
                connectDevice(data);
                break;
            case REQUEST_ENABLE_BT: // 블루투스를 Enable 시키기 위한 요청 코드일 경우
                if (resultCode == Activity.RESULT_OK) {
                    // 켠다면
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, "블루투스 ON 상태, \n장치를 연결합니다.", Toast.LENGTH_SHORT).show();
                    mBluetoothService = new BluetoothService(this, mHandler);
                    connectActivity();
                } else { // 켜지 않는다면
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, "블루투스 OFF 상태, \n앱을 종료합니다.", Toast.LENGTH_SHORT).show();
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

    private final Handler tHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            //String strTitle = "", strCont = "";
            boolean needToast = false;
            switch (msg.what) {
                case 0:
                    nowTimeTxt.setText("사용 시간 : " + tTimeCounter + "초");
                    timeViolationTxt.setText("시간 유지 위반 횟수 : " + timeVioCount);
                    break;
                case 1:
                    needToast = true;
                    alertTitle = "[사용 시간 경과]";
                    alertCont = "휴식 필요";
                    timeImage.setImageResource(R.drawable.ic_grade_angry);
                    timeVioCount++;
                    break;
                case 2:
                    needToast = true;
                    alertTitle = "[휴식 부족]";
                    alertCont = "휴식시간이 부족합니다.";
                    isRestMegPr = true;
                    break;
                case 3:
                    needToast = true;
                    alertTitle = "[휴식 완료]";
                    alertCont = "충분한 휴식을 취했습니다.";
                    timeImage.setImageResource(R.drawable.ic_grade_smile);
                    break;
            }
            if(needToast == true) {
                toastTitle.setText(alertTitle);
                toastCont.setText(alertCont);
                toastAlert.show();
            }
        }
    };

    //BluetoothService로부터 오는 정보를 받아오는 Handler
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            setStatus(getString(R.string.connected_device_name));
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            setStatus(R.string.connecting);
                            break;
                        case BluetoothService.STATE_NONE:
                            setStatus(R.string.not_connected);
                            break;
                    }
                    break;
                case Constants.MESSAGE_READ:
                    int distance = (int) msg.obj;
                    boolean isSleepMode = pm.isScreenOn();
                    if (isSleepMode) {
                        alertDistance(distance);
                    }
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // 장치명 저장
                    Toast.makeText(MainActivity.this, "연결 성공", Toast.LENGTH_SHORT).show();
                    break;
                case Constants.MESSAGE_TOAST:
                    Toast.makeText(MainActivity.this, msg.getData().getString(Constants.TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    private void alertDistance(int distance) {
        distViolationTxt.setText("거리 유지 위반 횟수 : " + distVioCount);
        nowDistTxt.setText("거리 : " + distance + "cm");
        nowDistInfoTxt.setText("적정 거리입니다.");
        distImage.setImageResource(R.drawable.ic_grade_smile);
        if (distance > 0 && distance < 40) { // 거리가 40cm 미만일 경우
            nowDistInfoTxt.setText("적절한 거리를 유지하세요.");
            distImage.setImageResource(R.drawable.ic_grade_angry);
            switch (warnCount) {
                case 0: // 1차 경고
                    distNoti.setContentTitle("1차 경고");
                    distNoti.setContentText("거리 유지 필요");
                    mNotiManager.notify(alertCount++, distNoti.getNotification());

                    isSafeDist = false;

                    alertTitle = "[1차 경고]";
                    alertCont = "현재 거리는 " + distance + "cm 입니다.";
                    toastTitle.setText(alertTitle);
                    toastCont.setText(alertCont);
                    toastAlert.show();

                    distTimerTask = new TimerTask() {
                        @Override
                        public void run() {
                            if (!isSafeDist)
                                distTimeCounter++;
                            else if (isSafeDist) {
                                distTimerTask.cancel();
                            }
                        }
                    };

                    distTimer = new Timer();
                    distTimer.schedule(distTimerTask, 0, 1000);
                    warnCount++;
                    distVioCount++;
                    break;

                case 1: // 2차 경고, 밝기 제한
                    if (distTimeCounter == 5) { // 적정 거리 5초 미 유지시
                        distVioCount++;
                        mNotiManager.cancelAll();
                        alertCount = 0;
                        try { // 자동 밝기 기능이 켜져있을 경우 해제 및 현재 밝기값 저장
                            currentBright = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
                            if (Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE) != 0) {
                                isAutoBright = true;
                                Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, 0);
                            }
                        } catch (Settings.SettingNotFoundException e) {
                            e.printStackTrace();
                        }
                        distNoti.setContentTitle("2차 경고");
                        distNoti.setContentText("밝기 제한");

                        alertTitle = "[2차 경고]";
                        alertCont = "밝기가 제한되었습니다.";
                        toastTitle.setText(alertTitle);
                        toastCont.setText(alertCont);

                        toastAlert.show();
                        mNotiManager.notify(alertCount++, distNoti.getNotification());
                        setBrightness(1);
                        warnCount = -1;
                    }
                    break;
            }
        } else { // 안전 거리를 유지한다면
            if(warnCount == 1) {
                alertDistSafe();
                warnCount = 0;
            }

            isSafeDist = true;
            distTimeCounter = 0;
            alertCount = 0;

            if (warnCount == -1) {
                setBrightness(0);
                alertDistSafe();
                mNotiManager.cancelAll();

                if (isAutoBright) { // 기존에 자동 밝기 기능 사용시 원래대로 복귀
                    try {
                        if (Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE) != 1) {
                            Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, 1);
                        }
                    } catch (Settings.SettingNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                warnCount = 0;
            }
        }
    }

    private void alertDistSafe() {
        alertTitle = "[적정 거리 확보]";
        alertCont = "적정 거리를 유지했습니다.";
        toastTitle.setText(alertTitle);
        toastCont.setText(alertCont);
        toastAlert.show();
    }


    private void setBrightness(int num) { // 밝기 설정
        Intent intent = new Intent(getApplicationContext(), BrightnessActivity.class);
        if (isTopActivity()) {
            intent.putExtra("finish Task", "finish");
        } else {
            intent.putExtra("finish Task", "background");
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        switch (num) {
            case 0:
                intent.putExtra("window bright", 1f);
                Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, currentBright);
                break;
            case 1:
                intent.putExtra("window bright", 0.1f);
                Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 25);
                break;
        }
        getApplication().startActivity(intent);
    }

    private boolean isTopActivity() { // 최상단 (현재) 작업이 MainActivity인지 확인
        ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> info;
        info = activityManager.getRunningTasks(1);
        if (info.get(0).topActivity.getClassName().equals(MainActivity.this.getClass().getName())) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.connect_bluetooth: {
                Intent connectIntent = new Intent(MainActivity.this, DeviceActivity.class);
                startActivityForResult(connectIntent, REQUEST_CONNECT_DEVICE);
                return true;
            }
            case R.id.guide_menu: {
                Intent guideIntent = new Intent(MainActivity.this, GuideActivity.class);
                startActivity(guideIntent);
            }
            return false;
        }
        return super.onOptionsItemSelected(item);
    }

    private void connectActivity() {
    Intent serverIntent = new Intent(MainActivity.this, DeviceActivity.class);
    startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
    }


    private void connectDevice(Intent data) {
        // Intent 통해 전달된 MAC 주소 확인
        String address = data.getExtras().getString(DeviceActivity.EXTRA_DEVICE_ADDRESS);
        // MAC 주소 이용해 BluetoothDevice 객체 생성
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // 연결을 위한 메소드 호출
        mBluetoothService.connect(device);
    }

    @Override protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(TypekitContextWrapper.wrap(newBase));
    }
}
