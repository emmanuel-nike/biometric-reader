package com.fgtit.reader;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Menu;
import android.widget.TextView;
import android.widget.Toast;
//import android.support.design.widget.Snackbar;
//import android.support.design.widget.NavigationView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.preference.PreferenceManager;

//import android.support.v4.widget.DrawerLayout;


import com.fgtit.data.Conversions;
import com.fgtit.fpcore.FPMatch;
import com.fgtit.printer.DataUtils;
import com.fgtit.reader.databinding.ActivityMainBinding;
import com.fgtit.reader.models.User;
import com.fgtit.reader.service.ApiService;
import com.fgtit.reader.service.AsyncResponse;
import com.fgtit.reader.service.BluetoothCommands;
import com.fgtit.reader.service.DBService;
import com.fgtit.reader.ui.RegisterUserActivity;
import com.fgtit.reader.ui.home.HomeViewModel;
import com.fgtit.reader.ui.users.UserViewModel;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements
        NavigationView.OnNavigationItemSelectedListener {

    // Debugging
    private static final String TAG = "BluetoothReader";
    private static final boolean D = true;

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    public Toolbar toolbar;
    public DrawerLayout drawerLayout;
    public NavigationView navigationView;
    public NavController navController;

    private byte mDeviceCmd = 0x00;
    private boolean mIsWork = false;
    private byte mCmdData[] = new byte[10240];
    private int mCmdSize = 0;

    private Timer mTimerTimeout = null;
    private TimerTask mTaskTimeout = null;
    private Handler mHandlerTimeout;

    public byte mRefData[] = new byte[512];
    public int mRefSize = 0;
    public byte mMatData[] = new byte[512];
    public int mMatSize = 0;

    public byte mCardSn[] = new byte[7];
    public byte mCardData[] = new byte[4096];
    public int mCardSize = 0;

    public byte mBat[] = new byte[2];
    public byte mUpImage[] = new byte[73728];
    public int mUpImageSize = 0;
    public int mUpImageCount = 0;

    private int imgSize;

    private byte[] testBuf;
    int j = 0;
    int u = 0;

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothReaderService mService = null;

    // Name of the connected device
    private String mConnectedDeviceName = null;
    private String mConnectedDeviceAddress = null;

    //Intents
    ActivityResultLauncher<Intent> connectDeviceRequest;
    ActivityResultLauncher<Intent> enableBtRequest;
    ActivityResultLauncher<Intent> registerUserActivityLauncher;

    HomeViewModel homeViewModel;

    DBService dbService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        //setContentView(R.layout.activity__main);

        //Setup FAB
        binding.appBarMain.fab.hide();
        binding.appBarMain.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "FAB clicked");
                if(mConnectedDeviceAddress == null){
                    Toast.makeText(MainActivity.this, "Please connect to a device first", Toast.LENGTH_SHORT).show();
                    return;
                }
                //mService.stop();
                Intent registerUserIntent = new Intent(MainActivity.this, RegisterUserActivity.class);
                Bundle b = new Bundle();
                b.putString("deviceAddress", mConnectedDeviceAddress);
                registerUserIntent.putExtras(b);
                registerUserActivityLauncher.launch(registerUserIntent);
            }
        });

        //Toolbar support action
        toolbar = binding.appBarMain.toolbar;
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setOnMenuItemClickListener(onMenuItemClick);

        TextView navHeaderTitle = binding.navigationView.getHeaderView(0).findViewById(R.id.nav_header_title);
        TextView navHeaderSubtitle = binding.navigationView.getHeaderView(0).findViewById(R.id.nav_header_subtitle);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        navHeaderTitle.setText(preferences.getString("institution", "Example Institution"));
        navHeaderSubtitle.setText(preferences.getString("email", "example@mail.com"));

        drawerLayout = binding.drawerLayout;
        navigationView = binding.navigationView;
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_users)
                .setOpenableLayout(drawerLayout)
                .build();

        navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
        navigationView.setNavigationItemSelectedListener(this);

        requestBlePermissions(this, REQUEST_ENABLE_BT);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Bluetooth CONNECT permission not granted", Toast.LENGTH_SHORT).show();
            // finish();
            // return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Bluetooth SCAN permission not granted", Toast.LENGTH_SHORT).show();
            // finish();
            // return;
        }

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        connectDeviceRequest = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        activityResult(REQUEST_CONNECT_DEVICE, result.getResultCode(), result.getData());
                    }
                });

        enableBtRequest = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> activityResult(REQUEST_ENABLE_BT, result.getResultCode(), result.getData()));

        registerUserActivityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        UserViewModel userViewModel = new ViewModelProvider(MainActivity.this).get(UserViewModel.class);
                        userViewModel.update();
                    }
                });
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        dbService = new DBService(this);
    }

    private static final String[] BLE_PERMISSIONS = new String[]{
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    @RequiresApi(api = Build.VERSION_CODES.S)
    private static final String[] ANDROID_12_BLE_PERMISSIONS = new String[]{
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
    };

    public static void requestBlePermissions(Activity activity, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            ActivityCompat.requestPermissions(activity, ANDROID_12_BLE_PERMISSIONS, requestCode);
        else
            ActivityCompat.requestPermissions(activity, BLE_PERMISSIONS, requestCode);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (mService != null) mService.stop();
    }

    public void activityResult(int requestCode, int resultCode, Intent data) {
        if (D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    // Get the device MAC address
                    mConnectedDeviceAddress = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    // Get the BLuetoothDevice object
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mConnectedDeviceAddress);
                    // Attempt to connect to the device
                    mService.connect(device);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupBluetoothService();
                } else {
                    // User did not enable Bluetooth or an error occured
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.BLUETOOTH_CONNECT }, REQUEST_ENABLE_BT);
        }
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            return;
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBtRequest.launch(enableIntent);
            //startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else {
            if (mService == null) {
                setupBluetoothService();
            };
        }
    }

    private void setupBluetoothService(){
        mService = new BluetoothReaderService(this, mHandler);    // Initialize the BluetoothChatService to perform bluetooth connections
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    private Toolbar.OnMenuItemClickListener onMenuItemClick = new Toolbar.OnMenuItemClickListener() {
        @SuppressLint("NonConstantResourceId")
        @Override
        public boolean onMenuItemClick(MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case R.id.scan:
                    // Launch the DeviceListActivity to see devices and do scan
                    Intent serverIntent = new Intent(MainActivity.this, DeviceListActivity.class);
                    connectDeviceRequest.launch(serverIntent);
                    return true;
                case R.id.discoverable:
                    // Ensure this device is discoverable by others
                    ensureDiscoverable();
                    return true;
                case R.id.services:
                    // Stop all services
                    homeViewModel.setFpServiceRunning(false);
                    homeViewModel.setUser(null);
                    return true;
            }
            return true;
        }
    };

    private void ensureDiscoverable() {
        if (D) Log.d(TAG, "ensure discoverable");
        Log.e(TAG, "ensure discoverable");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.BLUETOOTH_SCAN }, REQUEST_ENABLE_BT);
            return;
        }
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    private void AddStatusList(String text){
        Snackbar snackbar = Snackbar.make(binding.getRoot(), text, Snackbar.LENGTH_LONG);
        snackbar.show();
    }

    private void SendCommand(byte cmdid, byte[] data, int size) {
        if (mIsWork) return;

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
        int sum = Utils.calcCheckSum(sendbuf, (7 + size));
        sendbuf[7 + size] = (byte) (sum);
        sendbuf[8 + size] = (byte) (sum >> 8);

        mIsWork = true;
        TimeOutStart();
        mDeviceCmd = cmdid;
        mCmdSize = 0;
        mService.write(sendbuf);

        switch (sendbuf[4]) {
            case BluetoothCommand.CMD_PASSWORD:
                break;
            case BluetoothCommand.CMD_ENROLID:
                AddStatusList("Enrol ID ...");
                break;
            case BluetoothCommand.CMD_VERIFY:
                AddStatusList("Verify ID ...");
                break;
            case BluetoothCommand.CMD_IDENTIFY:
                AddStatusList("Search ID ...");
                break;
            case BluetoothCommand.CMD_DELETEID:
                AddStatusList("Delete ID ...");
                break;
            case BluetoothCommand.CMD_CLEARID:
                AddStatusList("Clear ...");
                break;
            case BluetoothCommand.CMD_ENROLHOST:
                AddStatusList("Enrol Template ...");
                break;
            case BluetoothCommand.CMD_CAPTUREHOST:
                AddStatusList("Capture Template ...");
                break;
            case BluetoothCommand.CMD_MATCH:        //比对
                AddStatusList("Match Template ...");
                break;
            case BluetoothCommand.CMD_WRITEFPCARD:    //写卡
            case BluetoothCommand.CMD_WRITEDATACARD:
                AddStatusList("Write Card ...");
                break;
            case BluetoothCommand.CMD_READFPCARD:    //读卡
            case BluetoothCommand.CMD_READDATACARD:
                AddStatusList("Read Card ...");
                break;
            case BluetoothCommand.CMD_FPCARDMATCH:
                AddStatusList("FingerprintCard Match ...");
                break;
            case BluetoothCommand.CMD_CARDSN:        //�������к�
                AddStatusList("Read Card SN ...");
                break;
            case BluetoothCommand.CMD_GETSN:
                AddStatusList("Get Device SN ...");
                break;
            case BluetoothCommand.CMD_GETBAT:
                AddStatusList("Get Battery Value ...");
                break;
            case BluetoothCommand.CMD_GETIMAGE:
                mUpImageSize = 0;
                AddStatusList("Get Fingerprint Image ...");
                break;
            case BluetoothCommand.CMD_GETCHAR:
                AddStatusList("Get Fingerprint Data ...");
                break;
            case BluetoothCommand.CMD_GET_VERSION:
                AddStatusList("Get Version ...");
                break;
            case BluetoothCommand.CMD_TEST_UART:
                AddStatusList("Test ...");
                break;
            case BluetoothCommand.CMD_TWO_TEMTEURE:
                AddStatusList("Test ...");
                break;
        }
    }

    private void restartFPCommandAfterDelay(int delay){
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                homeViewModel.setUser(null);
                SendCommand(BluetoothCommands.CMD_CAPTUREHOST, null, 0);
            }
        }, delay);
    }

    private void sendAuthUserToServer(String username){

        ApiService apiService = new ApiService(getApplicationContext(), new AsyncResponse() {
            @Override
            public void processFinish(String output) {
                Log.d(TAG, "Process Finish: " + output);
                homeViewModel.setServerResponse(output);
                if(output != null){
                    Toast.makeText(MainActivity.this, "Auth-Sent to Server", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Error sending auth", Toast.LENGTH_SHORT).show();
                }
            }
        });
        apiService.execute(ApiService.POST_USER_AUTH, username, homeViewModel.getAuthMode().getValue());
    }

    private void ReceiveCommand(byte[] databuf, int datasize) {
        if (mDeviceCmd == BluetoothCommand.CMD_GETIMAGE) {
            if (imgSize == Utils.IMG200) {
                Utils.memcpy(mUpImage, mUpImageSize, databuf, 0, datasize);
                mUpImageSize = mUpImageSize + datasize;
                //AddStatusList("Image Len="+Integer.toString(mUpImageSize)+"--"+Integer.toString(mUpImageCount));
                if (mUpImageSize >= 15200) {
                    byte[] bmpdata = Utils.getFingerprintImage(mUpImage, 152, 200, 0/*18*/);
                    //textSize.setText("152 * 200");
                    Bitmap image = BitmapFactory.decodeByteArray(bmpdata, 0, bmpdata.length);
                    Log.d(TAG, "bmpdata.length:" + bmpdata.length);
                    //fingerprintImage.setImageBitmap(image);
                    mUpImageSize = 0;
                    mUpImageCount = mUpImageCount + 1;
                    mIsWork = false;

                    File file = new File("/sdcard/test.raw");
                    try {
                        file.createNewFile();
                        FileOutputStream out = new FileOutputStream(file);
                        out.write(mUpImage);
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    AddStatusList("Display Image");
                }
            } else if (imgSize == Utils.IMG288) {
                Utils.memcpy(mUpImage, mUpImageSize, databuf, 0, datasize);
                mUpImageSize = mUpImageSize + datasize;
                //AddStatusList("Image Len="+Integer.toString(mUpImageSize)+"--"+Integer.toString(mUpImageCount));
                Log.d(TAG, "mUpImageSize:" + mUpImageSize);
                if (mUpImageSize >= 36864) {
                    byte[] bmpdata = Utils.getFingerprintImage(mUpImage, 256, 288, 0/*18*/);
                    //textSize.setText("256 * 288");
                    Bitmap image = BitmapFactory.decodeByteArray(bmpdata, 0, bmpdata.length);
                    Log.d(TAG, "bmpdata.length:" + bmpdata.length);
                    //fingerprintImage.setImageBitmap(image);
                    mUpImageSize = 0;
                    mUpImageCount = mUpImageCount + 1;
                    mIsWork = false;
                    File file = new File("/sdcard/test.raw");
                    try {
                        file.createNewFile();
                        FileOutputStream out = new FileOutputStream(file);
                        out.write(mUpImage);
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    AddStatusList("Display Image");
                }
            } else if (imgSize == Utils.IMG360) {
                Utils.memcpy(mUpImage, mUpImageSize, databuf, 0, datasize);
                mUpImageSize = mUpImageSize + datasize;
                //AddStatusList("Image Len="+Integer.toString(mUpImageSize)+"--"+Integer.toString(mUpImageCount));
                if (mUpImageSize >= 46080) {

                    byte[] bmpdata = Utils.getFingerprintImage(mUpImage, 256, 360, 0/*18*/);
                    //textSize.setText("256 * 360");
                    Bitmap image = BitmapFactory.decodeByteArray(bmpdata, 0, bmpdata.length);
                    Log.d(TAG, "bmpdata.length:" + bmpdata.length);
                    //fingerprintImage.setImageBitmap(image);
                    mUpImageSize = 0;
                    mUpImageCount = mUpImageCount + 1;
                    mIsWork = false;
                    File file = new File("/sdcard/test.raw");
                    try {
                        file.createNewFile();
                        FileOutputStream out = new FileOutputStream(file);
                        out.write(mUpImage);
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    AddStatusList("Display Image");

                }
            }
        } else {
            Utils.memcpy(mCmdData, mCmdSize, databuf, 0, datasize);
            mCmdSize = mCmdSize + datasize;
            int totalsize = (byte) (mCmdData[5]) + ((mCmdData[6] << 8) & 0xFF00) + 9;
            if (mCmdSize >= totalsize) {
                mCmdSize = 0;
                mIsWork = false;
                TimeOutStop();

                if ((mCmdData[0] == 'F') && (mCmdData[1] == 'T')) {
                    switch (mCmdData[4]) {
                        case BluetoothCommand.CMD_PASSWORD: {
                        }
                        break;
                        case BluetoothCommand.CMD_ENROLID: {
                            if (mCmdData[7] == 1) {
                                //int id=mCmdData[8]+(mCmdData[9]<<8);
                                int id = (byte) (mCmdData[8]) + (byte) ((mCmdData[9] << 8) & 0xFF00);
                                AddStatusList("Enrol Succeed:" + String.valueOf(id));
                                Log.d(TAG, String.valueOf(id));
                            } else
                                AddStatusList("Search Fail");

                        }
                        break;
                        case BluetoothCommand.CMD_VERIFY: {
                            if (mCmdData[7] == 1)
                                AddStatusList("Verify Succeed");
                            else
                                AddStatusList("Search Fail");
                        }
                        break;
                        case BluetoothCommand.CMD_IDENTIFY: {
                            if (mCmdData[7] == 1) {
                                int id = (byte) (mCmdData[8]) + (byte) ((mCmdData[9] << 8) & 0xFF00);
                                //int id=mCmdData[8]+(mCmdData[9]<<8);
                                AddStatusList("Search Result:" + String.valueOf(id));
                            } else
                                AddStatusList("Search Fail");
                        }
                        break;
                        case BluetoothCommand.CMD_DELETEID: {
                            if (mCmdData[7] == 1)
                                AddStatusList("Delete Succeed");
                            else
                                AddStatusList("Search Fail");
                        }
                        break;
                        case BluetoothCommand.CMD_CLEARID: {
                            if (mCmdData[7] == 1)
                                AddStatusList("Clear Succeed");
                            else
                                AddStatusList("Search Fail");
                        }
                        break;
                        case BluetoothCommand.CMD_ENROLHOST: {
                            int size = (byte) (mCmdData[5]) + ((mCmdData[6] << 8) & 0xFF00) - 1;
                            if (mCmdData[7] == 1) {
                                Utils.memcpy(mRefData, 0, mCmdData, 8, size);
                                mRefSize = size;
                                AddStatusList("Enrol Succeed");
                            } else
                                AddStatusList("Search Fail");
                        }
                        break;
                        case BluetoothCommand.CMD_CAPTUREHOST: {
                            int delay = 1500;
                            int size = (byte) (mCmdData[5]) + ((mCmdData[6] << 8) & 0xFF00) - 1;
                            if (mCmdData[7] == 1) {
                                Utils.memcpy(mMatData, 0, mCmdData, 8, size);
                                mMatSize = size;
                                AddStatusList("Capture Succeed");

                                if(homeViewModel.isFpServiceRunning){
                                    //String bsiso = Conversions.getInstance().IsoChangeCoord(mMatData, 1);
                                    //String base64 = ExtApi.BytesToBase64(mMatData, mMatSize);
                                    ArrayList<User> users = dbService.getAllUsers();
                                    for (int i = 0; i < users.size(); i++) {
                                        int score = FPMatch.getInstance().MatchFingerData(ExtApi.Base64ToBytes(users.get(i).getFingerPrintData()), mMatData);
                                        AddStatusList("Match Score:" + String.valueOf(score));
                                        Log.e(TAG, "Match Score:" + String.valueOf(score));
                                        if(score >= 70){
                                            homeViewModel.setUser(users.get(i));
                                            sendAuthUserToServer(users.get(i).getUsername());
                                            delay = 3500;
                                            break;
                                        }
                                    }
                                }
                            } else {
                                AddStatusList("Search Fail");
                            }

                            if(Boolean.TRUE.equals(homeViewModel.getIsFpServiceRunning().getValue())){
                                restartFPCommandAfterDelay(delay);
                            }
                        }
                        break;
                        case BluetoothCommand.CMD_MATCH: {
                            int score = (byte) (mCmdData[8]) + ((mCmdData[9] << 8) & 0xFF00);
                            if (mCmdData[7] == 1)
                                AddStatusList("Match Succeed:" + String.valueOf(score));
                            else
                                AddStatusList("Search Fail");
                        }
                        break;
                        case BluetoothCommand.CMD_WRITEFPCARD: {
                            if (mCmdData[7] == 1)
                                AddStatusList("Write Fingerprint Card Succeed");
                            else
                                AddStatusList("Search Fail");
                        }
                        break;
                        case BluetoothCommand.CMD_READFPCARD: {
                            int size = (byte) (mCmdData[5]) + ((mCmdData[6] << 8) & 0xFF00);
                            if (size > 0) {
                                byte[] snb = new byte[32];
                                Utils.memcpy(snb, 0, mCmdData, 8, size);
                                String sn = null;
                                try {
                                    sn = new String(snb, 0, size, "UNICODE");
                                } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                }
                                AddStatusList("SN:" + sn);
                            } else
                                AddStatusList("Search Fail");
                        }
                        break;
                        case BluetoothCommand.CMD_FPCARDMATCH: {
                            if (mCmdData[7] == 1) {
                                AddStatusList("Fingerprint Match Succeed");
                                int size = (byte) (mCmdData[5]) + ((mCmdData[6] << 8) & 0xFF00) - 1;
                                byte[] tmpbuf = new byte[size];
                                Utils.memcpy(tmpbuf, 0, mCmdData, 8, size);
                                AddStatusList("Len=" + String.valueOf(size));
                                //AddStatusListHex(tmpbuf, size);
                                String txt = new String(tmpbuf);
                                AddStatusList(txt);
                            } else
                                AddStatusList("Search Fail");
                        }
                        break;
                        case BluetoothCommand.CMD_UPCARDSN:
                        case BluetoothCommand.CMD_CARDSN: {
                            int size = (byte) (mCmdData[5]) + ((mCmdData[6] << 8) & 0xF0) - 1;
                            if (size > 0) {
                                Utils.memcpy(mCardSn, 0, mCmdData, 8, size);
                                if(homeViewModel.isCardServiceRunning){
                                    String cardData = Conversions.getInstance().byteArrayToHexString(mCardSn);
                                    User user = dbService.findUserByFpOrCard(cardData);
                                    if(user != null){
                                        homeViewModel.setUser(user);
                                        sendAuthUserToServer(user.getUsername());
                                        AddStatusList("Read Card SN Succeed. USER " + user.getUsername() + " FOUND");
                                    }else{
                                        AddStatusList("Read Card SN Succeed. USER NOT FOUND");
                                    }
                                }

                            } else
                                AddStatusList("Search Fail");
                        }
                        break;
                        case BluetoothCommand.CMD_WRITEDATACARD: {
                            if (mCmdData[7] == 1) {
                                AddStatusList("Write Card Data Succeed");
                            } else {
                                AddStatusList("Search Fail");
                            }
                        }
                        break;
                        case BluetoothCommand.CMD_READDATACARD: {
                            int size = (byte) (mCmdData[5]) + ((mCmdData[6] << 8) & 0xFF00);
                            if (size > 0) {
                                Utils.memcpy(mCardData, 0, mCmdData, 8, size);
                                Log.d(TAG, DataUtils.bytesToStr(mCardData));
                                mCardSize = size;
                                //AddStatusListHex(mCardData, size);
                            } else
                                AddStatusList("Search Fail");
                        }
                        break;
                        case BluetoothCommand.CMD_GETSN: {
                            int size = (byte) (mCmdData[5]) + ((mCmdData[6] << 8) & 0xFF00) - 1;
                            if (mCmdData[7] == 1) {
                                byte[] snb = new byte[32];
                                Utils.memcpy(snb, 0, mCmdData, 8, size);
                                String sn = null;
                                try {
                                    sn = new String(snb, 0, size, "UNICODE");
                                } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                }
                                AddStatusList("SN:" + sn);
                            } else
                                AddStatusList("Search Fail");
                        }
                        break;
                        case BluetoothCommand.CMD_PRINTCMD: {
                            if (mCmdData[7] == 1) {
                                AddStatusList("Print OK");
                            } else {
                                AddStatusList("Search Fail");
                            }
                        }
                        break;
                        case BluetoothCommand.CMD_GETBAT: {
                            int size = (byte) (mCmdData[5]) + ((mCmdData[6] << 8) & 0xFF00) - 1;
                            if (size > 0) {
                                Utils.memcpy(mBat, 0, mCmdData, 8, size);

                                AddStatusList("Battery Value:" + Integer.toString(mBat[0] / 10) + "." + Integer.toString(mBat[0] % 10) + "V");
                            } else
                                AddStatusList("Search Fail");
                        }
                        break;
                        case BluetoothCommand.CMD_GETCHAR: {
                            int size = (byte) (mCmdData[5]) + ((mCmdData[6] << 8) & 0xFF00) - 1;
                            if (mCmdData[7] == 1) {
                                Utils.memcpy(mMatData, 0, mCmdData, 8, size);
                                mMatSize = size;
                                AddStatusList("Len=" + String.valueOf(mMatSize));
                                AddStatusList("Get Data Succeed");
                                //AddStatusListHex(mMatData, mMatSize);
                            } else
                                AddStatusList("Search Fail");
                        }
                        break;
                        case BluetoothCommand.CMD_GET_VERSION: {
                            int size = (byte) (mCmdData[5]) + ((mCmdData[6] << 8) & 0xFF00) - 1;
                            if (mCmdData[7] == 1) {
                                Utils.memcpy(mMatData, 0, mCmdData, 8, size);
                                AddStatusList("Version：" + Utils.bytesToAscii(mMatData));
                            } else
                                AddStatusList("Search Fail");
                        }
                        break;
                        case BluetoothCommand.CMD_TEST_UART: {
                            u++;

                            int size = (byte) (mCmdData[5]) + ((mCmdData[6] << 8) & 0xFF00) - 1;
                            if (mCmdData[7] == 1) {

                                byte testData[] = new byte[1024];
                                Utils.memcpy(testData, 0, mCmdData, 8, size);
                                Log.d(TAG, "testData.length:" + testData.length);
                                Log.d(TAG, "testData:" + testData);
                                Log.d(TAG, ExtApi.BytesToBase64(testData, testData.length));
                                for (int i = 0; i < 1024; i++) {
                                    if (testData[i] != testBuf[i]) {
                                        j++;
                                    }
                                }
                                AddStatusList(String.valueOf(u) + "一" + j + " Extra");

                                SystemClock.sleep(500);
                                testBuf = new byte[1024];
                                Utils.memcpy(testBuf, 0, mRefData, 0, 512);
                                Utils.memcpy(testBuf, 512, mRefData, 0, 512);
                                Log.d(TAG, "testBuf.length:" + testBuf.length);
                                Log.d(TAG, "testBuf:" + testBuf);
                                Log.d(TAG, ExtApi.BytesToBase64(testBuf, testBuf.length));
                                SendCommand(BluetoothCommand.CMD_TEST_UART, testBuf, 1024);
                            } else
                                AddStatusList("Search Fail");
                        }
                        break;
                        case BluetoothCommand.CMD_TWO_TEMTEURE: {
                            int size = (byte) (mCmdData[5]) + ((mCmdData[6] << 8) & 0xFF00) - 1;
                            if (mCmdData[7] == 1) {
                                Utils.memcpy(mRefData, 0, mCmdData, 8, size);
                                mRefSize = size;
                                AddStatusList("Enrol Two Succeed");
                            } else
                                AddStatusList("Search Fail");
                        }
                        break;
                    }
                }
            }
        }
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

    // The Handler that gets information back from the BluetoothChatService
    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.e(TAG, msg.toString());
            switch (msg.what) {
                case MessageTypes.MESSAGE_STATE_CHANGE:
                    if (D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                    switch (msg.arg1) {
                        case BluetoothReaderService.STATE_CONNECTED:
                            toolbar.setSubtitle(/*R.string.title_connected_to +":"+ */mConnectedDeviceName);
                            homeViewModel.setConnected(true);
                            //mConversationArrayAdapter.clear();
                            break;
                        case BluetoothReaderService.STATE_CONNECTING:
                            toolbar.setSubtitle(R.string.title_connecting);
                            homeViewModel.setConnected(false);
                            break;
                        case BluetoothReaderService.STATE_LISTEN:
                        case BluetoothReaderService.STATE_NONE:
                            toolbar.setSubtitle(R.string.title_not_connected);
                            homeViewModel.setConnected(false);
                            break;
                    }
                    break;
                case MessageTypes.MESSAGE_WRITE:
                    //byte[] writeBuf = (byte[]) msg.obj;
                    //AddStatusListHex(writeBuf,writeBuf.length);
                    break;
                case MessageTypes.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    if (readBuf.length > 0) {
                        if (readBuf[0] == (byte) 0x1b) {
                            //AddStatusListHex(readBuf, msg.arg1);
                        } else {
                            ReceiveCommand(readBuf, msg.arg1);
                        }
                    }
                    //byte[] readBuf = (byte[]) msg.obj;
                    //ReceiveCommand(readBuf,msg.arg1);
                    //AddStatusList("Read Len="+Integer.toString(msg.arg1));
                    //AddStatusListHex(readBuf,msg.arg1);
                    break;
                case MessageTypes.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(MessageTypes.DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to " + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case MessageTypes.MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(MessageTypes.TOAST), Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        menuItem.setChecked(true);

        Log.e(TAG, "this is the nav select listener");

        drawerLayout.closeDrawers();

        int id = menuItem.getItemId();
        binding.appBarMain.fab.hide();

        switch (id) {

            case R.id.nav_home:
                navController.navigate(R.id.nav_home);
                break;

            case R.id.nav_users:
                binding.appBarMain.fab.show();
                navController.navigate(R.id.nav_users);
                break;

            case R.id.nav_debug:
                Intent debugIntent = new Intent(MainActivity.this, BluetoothReader.class);
                startActivity(debugIntent);
                break;

            case R.id.nav_support:
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/2349061925922"));
                startActivity(browserIntent);

            case R.id.nav_settings:
                navController.navigate(R.id.nav_settings);
                break;

            case R.id.nav_logout:
                break;

        }
        return true;
    }

    public void startFpService() {

        SendCommand(BluetoothCommands.CMD_CAPTUREHOST, null, 0);
        homeViewModel.setFpServiceRunning(true);
    }

    public void startCardService() {

        SendCommand(BluetoothCommands.CMD_CARDSN, null, 0);
        homeViewModel.setCardServiceRunning(true);
    }
}