package com.example.wjdtl.eyesafer;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Set;

public class DeviceListActivity extends Activity {

    private static final String TAG = "DeviceListActivity";

    private BluetoothAdapter mBtAdapter;

    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    // 장치 목록 ListView 생성을 위해 ArrayAdapter 사용
    private ArrayAdapter<String> mNewDevicesArrayAdapter;
    private ArrayAdapter<String> mPairedDevicesArrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 상단 제목 표시줄에 진행 상태 표시, 둥근형태
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_device_list);

        // 사용자가 그냥 나간다면 CANCEL 응답 처리
        setResult(Activity.RESULT_CANCELED);

        Button scanButton = (Button) findViewById(R.id.button_scan);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doDiscovery();
                v.setVisibility(View.GONE);
            }
        });

        // 페어링된 디바이스 정보 ListView에 표시하기 위한 ArrayAdapter 객체 생성
        mPairedDevicesArrayAdapter =
                new ArrayAdapter<String>(this, R.layout.device_name);
        mNewDevicesArrayAdapter =
                new ArrayAdapter<String>(this, R.layout.device_name);

        // 페어링 디바이스 ListView에 나열
        ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
        pairedListView.setAdapter(mPairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickListener);

        // 새로 발견한 디바이스 ListView에 나열
        ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);

        // 블루투스 장치가 발견되면 인텐트를 전달받기 위한 인텐트 필터 등록
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);

        // 검색 과정이 끝나면 인텐트를 전달받기 위한 필터 등록
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);

        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        // 페어링된 디바이스 정보 가져옴
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
            for (BluetoothDevice device : pairedDevices) {
                mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        } else {
            String noDevices = "페어링된 장치 없음";
            mPairedDevicesArrayAdapter.add(noDevices);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mBtAdapter != null) {
            mBtAdapter.cancelDiscovery();
        }

        this.unregisterReceiver(mReceiver);
    }

    private void doDiscovery() {

        setTitle("scanning");
        findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);

        // 블루투스 장치 이미 검색중이라면 취소 후 검색 시작
        if (mBtAdapter.isDiscovering()) {
            mBtAdapter.cancelDiscovery();
        }
        mBtAdapter.startDiscovery();
    }

    // 장치 목록 ListView에 있는 장치명을 누르면
    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            mBtAdapter.cancelDiscovery();

            // MAC 주소 확인
            String info = ((TextView) view).getText().toString();
            String address = info.substring(info.length() - 17);

            // Intent 객체에 MAC 주소 추가
            Intent intent = new Intent();
            intent.putExtra(EXTRA_DEVICE_ADDRESS, address);

            // 메인 액티비티로 응답
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // 블루투스 디바이스가 검색되었을 때
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                //인텐트로 전달된 BluetoothDevice 객체 참조
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                // 찾은 디바이스가 페어링이 되어 있지 않다면
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    mNewDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                }
            }
            // 디바이스 찾기가 끝나면 타이틀 변경
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                setTitle("연결할 디바이스를 선택하시오");
                if (mNewDevicesArrayAdapter.getCount() == 0) {
                    String noDevices = "찾은 디바이스 없음";
                    mNewDevicesArrayAdapter.add(noDevices);
                }
            }
        }
    };
}
