package com.fgtit.reader.service;

import static com.fgtit.reader.Utils.memcpy;
import static com.fgtit.reader.Utils.toBmpByte;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.fgtit.data.Conversions;
import com.fgtit.fpcore.FPMatch;
import com.fgtit.printer.DataUtils;
import com.fgtit.reader.BluetoothCommand;
import com.fgtit.reader.BluetoothReaderService;
import com.fgtit.reader.DeviceListActivity;
import com.fgtit.reader.ExtApi;
import com.fgtit.reader.R;
import com.fgtit.reader.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class BlueToothReaderClient {

    private static final String TAG = "BlueToothReaderClient";

    private boolean mIsWork = false;

    private Timer mTimerTimeout = null;
    private TimerTask mTaskTimeout = null;
    private Handler mHandlerTimeout;

    private String mConnectedDeviceName = null;

    private BluetoothReaderService mChatService = null;

    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothDevice mBluetoothDevice = null;
    private StringBuffer mOutStringBuffer;

    private byte mDeviceCmd = 0x00;
    private byte mCmdData[] = new byte[10240];
    public byte mCardSn[] = new byte[7];
    private int mCmdSize = 0;

    public static final int IMG360 = 360;

    public byte mBat[] = new byte[2];
    public byte mUpImage[] = new byte[73728];
    public int mUpImageSize = 0;
    public int mUpImageCount = 0;

    public Bitmap fingerprintImage;

    public static final String TOAST = "toast";
    public static final String DEVICE_NAME = "device_name";

    BluetoothCallback delegate = null;

    BluetoothSocket mSocket = null;

    Context context;

    public BlueToothReaderClient(Context context, BluetoothCallback delegate, String address) {
        this.delegate = delegate;
        this.context = context;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
//            return;
//        }
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice d: pairedDevices) {
                if(d.getAddress().equals(address)) {
                    mBluetoothDevice = d;
                    try{
                        mSocket = d.createRfcommSocketToServiceRecord(BluetoothReaderService.MY_UUID);
                        if(mSocket != null) {
                            mSocket.connect();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                // do what you need/want this these list items
            }
        }
        // Get the BLuetoothDevice object
        //mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(address);
    }

    private int calcCheckSum(byte[] buffer, int size) {
        int sum = 0;
        for (int i = 0; i < size; i++) {
            sum = sum + buffer[i];
        }
        return (sum & 0x00ff);
    }

    public byte[] getFingerprintImage(byte[] data, int width, int height, int offset) {
        if (data == null) {
            return null;
        }
        byte[] imageData = new byte[width * height];
        for (int i = 0; i < (width * height / 2); i++) {
            imageData[i * 2] = (byte) (data[i + offset] & 0xf0);
            imageData[i * 2 + 1] = (byte) (data[i + offset] << 4 & 0xf0);
        }
        byte[] bmpData = toBmpByte(width, height, imageData);
        return bmpData;
    }

    public String getmConnectedDeviceName() {
        return mConnectedDeviceName != null ? mConnectedDeviceName : "not connected";
    }

    public boolean isBluetoothServiceStarted() {
        return mChatService != null && mChatService.getState() != BluetoothReaderService.STATE_NONE;
    }

    @SuppressLint("HandlerLeak")
    public void TimeOutStart() {
        if (mTimerTimeout != null) {
            return;
        }
        mTimerTimeout = new Timer();
        mHandlerTimeout = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                TimeOutStop();
                if (mIsWork) {
                    mIsWork = false;
                    //AddStatusList("Time Out");
                }
                super.handleMessage(msg);
            }
        };
        mTaskTimeout = new TimerTask() {
            @Override
            public void run() {
                Message message = new Message();
                message.what = 1;
                mHandlerTimeout.sendMessage(message);
            }
        };
        mTimerTimeout.schedule(mTaskTimeout, 10000, 10000);
    }

    public void TimeOutStop() {
        if (mTimerTimeout != null) {
            mTimerTimeout.cancel();
            mTimerTimeout = null;
            mTaskTimeout.cancel();
            mTaskTimeout = null;
        }
    }

    public void startService() {
        Log.e(TAG, "setupChat() ==> Socket State" + (mSocket != null && mSocket.isConnected()));
        mChatService = new BluetoothReaderService(context, mHandler);    // Initialize the BluetoothChatService to perform bluetooth connections
        if(mSocket != null && mSocket.isConnected()) mChatService.connected(mSocket, mBluetoothDevice);
        else mChatService.connect(mBluetoothDevice);
        mOutStringBuffer = new StringBuffer("");                    // Initialize the buffer for outgoing messages
        if(mChatService.getState() == BluetoothReaderService.STATE_NONE) {
            Log.e(TAG, "Starting service...");
            mChatService.start();
        }
    }

    public void SendCommand(byte cmdid, byte[] data, int size) {
        if (mIsWork) {
            return;
        };

        int sendsize = 9 + size;
        byte[] sendbuf = new byte[sendsize];
        sendbuf[0] = 'F';
        sendbuf[1] = 'T';
        sendbuf[2] = 0;
        sendbuf[3] = 0;
        sendbuf[4] = cmdid;
        sendbuf[5] = (byte) (size);
        sendbuf[6] = (byte) (size >> 8);
        if (size > 0) {
            for (int i = 0; i < size; i++) {
                sendbuf[7 + i] = data[i];
            }
        }
        int sum = calcCheckSum(sendbuf, (7 + size));
        sendbuf[7 + size] = (byte) (sum);
        sendbuf[8 + size] = (byte) (sum >> 8);

        mIsWork = true;
        TimeOutStart();
        mDeviceCmd = cmdid;
        mCmdSize = 0;
        mChatService.write(sendbuf);
    }

    public void sendFingerPrintImageCommand(){
        mUpImageSize = 0;
        SendCommand(BluetoothCommands.CMD_GETIMAGE, null, 0);
    }

    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BluetoothMessages.MESSAGE_STATE_CHANGE:
                    Log.e(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                    delegate.getConnectionState(msg.arg1);
                    break;
                case BluetoothMessages.MESSAGE_WRITE:
                    break;
                case BluetoothMessages.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    if (readBuf.length > 0 && readBuf[0] != (byte) 0x1b) {
                        int datasize = msg.arg1;
                        if (mDeviceCmd == BluetoothCommand.CMD_GETIMAGE) {
                            memcpy(mUpImage, mUpImageSize, readBuf, 0, datasize);
                            mUpImageSize = mUpImageSize + datasize;
                            if (mUpImageSize >= 46080) {
                                byte[] bmpdata = getFingerprintImage(mUpImage, 256, 360, 0/*18*/);
                                fingerprintImage = BitmapFactory.decodeByteArray(bmpdata, 0, bmpdata.length);
                                delegate.receiveCommand(readBuf, msg.arg1);
                            }
                            return;
                        }

                        memcpy(mCmdData, mCmdSize, readBuf, 0, datasize);
                        mCmdSize = mCmdSize + datasize;
                        int totalsize = (byte) (mCmdData[5]) + ((mCmdData[6] << 8) & 0xFF00) + 9;
                        if (mCmdSize >= totalsize) {
                            mCmdSize = 0;
                            mIsWork = false;
                            TimeOutStop();

                            if ((mCmdData[0] == 'F') && (mCmdData[1] == 'T')) {
                                switch (mCmdData[4]) {
                                    case BluetoothCommands.CMD_PASSWORD: {
                                    }
                                    break;
                                    case BluetoothCommands.CMD_ENROLID: {
                                        if (mCmdData[7] == 1) {
                                            //int id=mCmdData[8]+(mCmdData[9]<<8);
                                            int id = (byte) (mCmdData[8]) + (byte) ((mCmdData[9] << 8) & 0xFF00);
                                            //AddStatusList("Enrol Succeed:" + String.valueOf(id));
                                            Log.d(TAG, String.valueOf(id));
                                        }

                                    }
                                    break;
                                    case BluetoothCommands.CMD_VERIFY: {
//                                        if (mCmdData[7] == 1)
//                                            AddStatusList("Verify Succeed");
                                    }
                                    break;
                                    case BluetoothCommands.CMD_IDENTIFY: {
                                        if (mCmdData[7] == 1) {
                                            int id = (byte) (mCmdData[8]) + (byte) ((mCmdData[9] << 8) & 0xFF00);
                                            //int id=mCmdData[8]+(mCmdData[9]<<8);
                                            //AddStatusList("Search Result:" + String.valueOf(id));
                                        }
                                    }
                                    break;
                                    case BluetoothCommands.CMD_DELETEID: {
//                                        if (mCmdData[7] == 1)
//                                            AddStatusList("Delete Succeed");
                                    }
                                    break;
                                    case BluetoothCommands.CMD_CLEARID: {
//                                        if (mCmdData[7] == 1)
//                                            AddStatusList("Clear Succeed");
                                    }
                                    break;
                                    case BluetoothCommands.CMD_ENROLHOST: {
                                        int size = (byte) (mCmdData[5]) + ((mCmdData[6] << 8) & 0xFF00) - 1;
                                        if (mCmdData[7] == 1) {
                                            byte mRefData[] = new byte[512];
                                            int mRefSize = 0;
                                            memcpy(mRefData, 0, mCmdData, 8, size);
                                            mRefSize = size;

                                            //ISO Format
                                            //String bsiso = Conversions.getInstance().IsoChangeCoord(mRefData, 1);
                                            String base64 = ExtApi.BytesToBase64(mRefData, mRefSize);
                                            delegate.receiveIsoFingerPrintTemplate(base64);
                                        }
                                    }
                                    break;
                                    case BluetoothCommands.CMD_CAPTUREHOST: {
                                        int size = (byte) (mCmdData[5]) + ((mCmdData[6] << 8) & 0xFF00) - 1;
                                        if (mCmdData[7] == 1) {
//                                            memcpy(mMatData, 0, mCmdData, 8, size);
//                                            mMatSize = size;
                                            //AddStatusList("Capture Succeed");

                                        }
                                    }
                                    break;
                                    case BluetoothCommands.CMD_MATCH: {
                                        int score = (byte) (mCmdData[8]) + ((mCmdData[9] << 8) & 0xFF00);
//                                        if (mCmdData[7] == 1)
//                                            AddStatusList("Match Succeed:" + String.valueOf(score));
                                    }
                                    break;
                                    case BluetoothCommands.CMD_WRITEFPCARD: {

                                    }
                                    break;
                                    case BluetoothCommands.CMD_READFPCARD: {
                                        int size = (byte) (mCmdData[5]) + ((mCmdData[6] << 8) & 0xFF00);
                                    }
                                    break;
                                    case BluetoothCommands.CMD_FPCARDMATCH: {
                                        if (mCmdData[7] == 1) {

                                        }
                                    }
                                    break;
                                    case BluetoothCommand.CMD_UPCARDSN:
                                    case BluetoothCommand.CMD_CARDSN: {
                                        int size = (byte) (mCmdData[5]) + ((mCmdData[6] << 8) & 0xF0) - 1;
                                        if (size > 0) {
                                            Utils.memcpy(mCardSn, 0, mCmdData, 8, size);
                                            String cardData2 = Conversions.getInstance().byteArrayToHexString(mCardSn);
                                            delegate.receiveCardData(cardData2);
                                            //String cardData = Integer.toHexString(mCardSn[0] & 0xFF) + Integer.toHexString(mCardSn[1] & 0xFF) + Integer.toHexString(mCardSn[2] & 0xFF) + Integer.toHexString(mCardSn[3] & 0xFF) + Integer.toHexString(mCardSn[4] & 0xFF) + Integer.toHexString(mCardSn[5] & 0xFF) + Integer.toHexString(mCardSn[6] & 0xFF);
                                        }
                                    }
                                    break;
                                    case BluetoothCommands.CMD_WRITEDATACARD: {

                                    }
                                    break;
                                    case BluetoothCommands.CMD_READDATACARD: {
                                        //int size = (byte) (mCmdData[5]) + ((mCmdData[6] << 8) & 0xFF00);
                                    }
                                    break;
                                    case BluetoothCommands.CMD_GETSN: {
                                        //int size = (byte) (mCmdData[5]) + ((mCmdData[6] << 8) & 0xFF00) - 1;
                                    }
                                    break;
                                    case BluetoothCommands.CMD_PRINTCMD: {

                                    }
                                    break;
                                    case BluetoothCommands.CMD_GETBAT: {
                                        //int size = (byte) (mCmdData[5]) + ((mCmdData[6] << 8) & 0xFF00) - 1;
                                    }
                                    break;
                                    case BluetoothCommands.CMD_GETCHAR: {
                                        //int size = (byte) (mCmdData[5]) + ((mCmdData[6] << 8) & 0xFF00) - 1;
                                    }
                                    break;
                                    case BluetoothCommands.CMD_GET_VERSION: {
                                        //int size = (byte) (mCmdData[5]) + ((mCmdData[6] << 8) & 0xFF00) - 1;
                                    }
                                    break;
                                    case BluetoothCommands.CMD_TWO_TEMTEURE: {
                                        //int size = (byte) (mCmdData[5]) + ((mCmdData[6] << 8) & 0xFF00) - 1;
                                    }
                                    break;
                                }
                            }

                            delegate.receiveCommand(readBuf, msg.arg1);
                        }
                    }
                    break;
                case BluetoothMessages.MESSAGE_DEVICE_NAME:
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    break;
                case BluetoothMessages.MESSAGE_TOAST:
                    delegate.sendToast(msg.getData().getString(TOAST));
                    break;
            }
        }
    };
}
