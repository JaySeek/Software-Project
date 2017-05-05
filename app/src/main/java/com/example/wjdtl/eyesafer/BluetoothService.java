package com.example.wjdtl.eyesafer;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.bluetooth.BluetoothAdapter;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class BluetoothService {
    // 디버그용
    private static final String TAG = "BluetoothService";

    //UUID
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothAdapter mAdapter;
    private Handler mHandler;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;
    private int mNewState;

    public static final int STATE_NONE = 0;       // 아무것도 하지 않는 상태
    public static final int STATE_LISTEN = 1;     // 연결 대기중인 상태
    public static final int STATE_CONNECTING = 2; // 연결이 진행중인 상태
    public static final int STATE_CONNECTED = 3;  // 연결된 상태

    public BluetoothService(Context context, Handler handler) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mNewState = mState;
        mHandler = handler;
    }

    public synchronized int getState() {
        return mState;
    }

    private synchronized void updateUserInterfaceTitle() {
        mState = getState();
        Log.d(TAG, "updateUserInterfaceTitle() " + mNewState + " -> " + mState);
        mNewState = mState;

        // MainActivity의 제목에 새로운 사항을 반영하기 위해 메시지 보냄
        mHandler.obtainMessage(Constants.MESSAGE_STATE_CHANGE, mNewState, -1).sendToTarget();
    }

    public synchronized void start() {
        // 연결을 시도하는 스레드가 있다면 끊음
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // 연결된 스레드가 있다면 끊음
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        updateUserInterfaceTitle();
    }

    public synchronized void connect(BluetoothDevice device) {
        // 연결 중인 스레드가 있다면
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // 현재 연결되어 돌고 있는 스레드가 있다면
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        //장치에 연결하기 위해 스레드 시작
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();

        // UI 변경
        updateUserInterfaceTitle();
    }

    //연결 되었을 때
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // 연결 이후의 스레드 실행
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        // 디바이스 이름 메시지에 실어서 전달, UI 표시
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        updateUserInterfaceTitle();
    }

    // 모든 스레드 중지
    public synchronized void stop() {

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        mState = STATE_NONE;
        updateUserInterfaceTitle();
    }

    // 연결 실패시 UI Activity에 정보 표시
    private void connectionFailed() {
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "디바이스 연결 불가");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        mState = STATE_NONE;
        updateUserInterfaceTitle();

        // 리스닝 모드로 변경
        BluetoothService.this.start();
    }

    // 연결이 끊길 시 UI Activity에 정보 표시
    private void connectionLost() {
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "디바이스 연결 해제됨");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        mState = STATE_NONE;
        updateUserInterfaceTitle();

        BluetoothService.this.start();
    }

    // 클라이언트 연결 소켓 스레드
    private class ConnectThread extends Thread {
        private BluetoothSocket mSocket;
        private BluetoothDevice mDevice;

        public ConnectThread(BluetoothDevice device) {
            mDevice = device;
            BluetoothSocket tmp = null;

            // 데이터 교환 위해 소켓 객체 생성
            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "create() failed");
            }
            mSocket = tmp;
            mState = STATE_CONNECTING;
        }

        public void run() {
            mAdapter.cancelDiscovery();
            try {
                // 소켓 연결 시도
                mSocket.connect();
            } catch (IOException e) {
                try {
                    mSocket.close();
                } catch (IOException e2) {
                }
                connectionFailed();
                return;
            }
            synchronized (BluetoothService.this) {
                mConnectThread = null;
            }

            connected(mSocket, mDevice);
        }

        public void cancel() {
            try {
                mSocket.close();
            } catch (IOException e) {
            }
        }
    }

    // 연결된 상태일 때의 스레드
    private class ConnectedThread extends Thread {
        private BluetoothSocket mSocket;
        private InputStream mInStream;

        public ConnectedThread(BluetoothSocket socket) {
            mSocket = socket;
            // 데이터 입/출력을 위한 IOStream 객체 사용
            InputStream tmpIn = null;
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
            }

            mInStream = tmpIn;
            mState = STATE_CONNECTED;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;
            int receivedValue;
            StringBuilder readMessage = new StringBuilder();
            while (true) {
                try {
                    //데이터 읽기
                    bytes = mInStream.read(buffer);
                    String tmpReceive = new String(buffer, 0, bytes);
                    if(bytes < 5) {
                        readMessage.append(tmpReceive);
                        if(tmpReceive.contains("d")) {
                            int index = readMessage.indexOf("d");
                            receivedValue = Integer.valueOf(readMessage.substring(0,index));
                            if(receivedValue <= 400)
                            mHandler.obtainMessage(Constants.MESSAGE_READ, bytes, -1, receivedValue).sendToTarget();
                            readMessage.setLength(0);
                    }
                    }
                } catch (IOException e) {
                    connectionLost();
                    break;
                }
            }
        }

        public void cancel() {
            try {
                mSocket.close();
            } catch (IOException e) {
            }
        }
    }
}