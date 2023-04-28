package com.fgtit.reader.ui;

import android.bluetooth.BluetoothAdapter;
import android.os.Bundle;

import com.bumptech.glide.Glide;
import com.fgtit.reader.BluetoothReaderService;
import com.fgtit.reader.R;
import com.fgtit.reader.Utils;
import com.fgtit.reader.models.User;
import com.fgtit.reader.service.ApiService;

import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.navigation.ui.AppBarConfiguration;

import com.fgtit.reader.databinding.ActivityRegisterUserBinding;
import com.fgtit.reader.service.AsyncResponse;
import com.fgtit.reader.service.BlueToothReaderClient;
import com.fgtit.reader.service.BluetoothCallback;
import com.fgtit.reader.service.BluetoothCommands;
import com.fgtit.reader.service.DBService;

import org.json.JSONException;

import java.util.Objects;

public class RegisterUserActivity extends AppCompatActivity implements BluetoothCallback {

    private static final String TAG = "RegisterUserActivity";
    private DBService dbService;

    private AppBarConfiguration appBarConfiguration;
    private ActivityRegisterUserBinding binding;

    private Button getUserDataButton;
    private Button registerFingerPrintsButton;
    private Button registerCardButton;
    private Button registerButton;
    private EditText usernameEditText;
    private EditText fullNameEditText;
    private EditText cardTagEditText;
    public Toolbar mToolbar;

    private ImageView fingerprintImage;
    private ImageView profilePhotoImage;

    private BlueToothReaderClient blueToothReaderClient;

    private BluetoothAdapter mBluetoothAdapter = null;

    private String fpTemplate = null;
    private String imageUrl = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityRegisterUserBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Bundle b = getIntent().getExtras();
        String address = null; // or other values
        if(b != null)
            address = b.getString("deviceAddress");

        dbService = new DBService(this);

        blueToothReaderClient = new BlueToothReaderClient(this, this, address);
        blueToothReaderClient.startService();

        getUserDataButton = findViewById(R.id.getUserDataButton);
        registerFingerPrintsButton = findViewById(R.id.registerFingerprintsButton);
        registerCardButton = findViewById(R.id.registerCardButton);
        registerButton = findViewById(R.id.registerUserButton);
        fullNameEditText = findViewById(R.id.fullNameEditText);
        usernameEditText = findViewById(R.id.usernameEditText);
        cardTagEditText = findViewById(R.id.cardTagEditText);
        usernameEditText.setText("DDON576/2023/5253");
        fingerprintImage = (ImageView) findViewById(R.id.fingerPrintImageView);
        profilePhotoImage = (ImageView) findViewById(R.id.profilePhotoImageView);

        setSupportActionBar(binding.toolbar);
        mToolbar = binding.toolbar;
        mToolbar.setTitle("Register User");
        mToolbar.setSubtitle(blueToothReaderClient.getmConnectedDeviceName());
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        ApiService apiService = new ApiService(getApplicationContext(), new AsyncResponse() {
            @Override
            public void processFinish(String output) {
                Log.d(TAG, "processFinish: " + output);
                if(output != null){
                    try{
                        imageUrl = ApiService.parseJSON(output, "image");
                        if(!imageUrl.isEmpty()){
                            Glide.with(getApplicationContext()).load(imageUrl).into(profilePhotoImage);
                        }
                        fullNameEditText.setText(ApiService.parseJSON(output, "name"));
                        usernameEditText.setEnabled(false);
                        registerFingerPrintsButton.setEnabled(true);
                        registerCardButton.setEnabled(true);
                    } catch (JSONException e){
                        e.printStackTrace();
                        Toast.makeText(RegisterUserActivity.this, "Error occured - " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }

                    return;
                }else{
                    Toast.makeText(RegisterUserActivity.this, "User not found", Toast.LENGTH_SHORT).show();
//                    fullNameEditText.setText("Full Name TEST "+ String.valueOf((int)(Math.random() * 1000)));
//                    usernameEditText.setEnabled(false);
//                    registerFingerPrintsButton.setEnabled(true);
//                    registerCardButton.setEnabled(true);
                    return;
                }
                //getUserDataButton.setEnabled(true);

            }
        });

        getUserDataButton.setOnClickListener(v -> {
            String username = usernameEditText.getText().toString();
            if(username.isEmpty()){
                usernameEditText.setError("Username is required");
                return;
            }
            apiService.execute(ApiService.GET_USER_DATA, username);
            getUserDataButton.setEnabled(false);
        });
        //apiService.execute(ApiService.GET_USER_DATA, "DON6998231");

        registerFingerPrintsButton.setOnClickListener(v -> {
            String username = usernameEditText.getText().toString();
            if(username.isEmpty()){
                usernameEditText.setError("Username is required");
                return;
            }
            Log.e(TAG, "registerFingerPrintsButton: " + username);
            if(fingerprintImage.getDrawable() == null){
                Log.e(TAG, "registerFingerPrintsButton: " + "No fingerprint image");
                blueToothReaderClient.sendFingerPrintImageCommand();
            }else{
                blueToothReaderClient.SendCommand(BluetoothCommands.CMD_ENROLHOST, null, 0);
            }
        });

        registerCardButton.setOnClickListener(v -> {
            String username = usernameEditText.getText().toString();
            if(username.isEmpty()){
                usernameEditText.setError("Username is required");
                return;
            }
            blueToothReaderClient.SendCommand(BluetoothCommands.CMD_CARDSN, null, 0);
        });

        registerButton.setOnClickListener(v -> {
            String username = usernameEditText.getText().toString();
            String name = fullNameEditText.getText().toString();
            String cardTag = cardTagEditText.getText().toString();
            if(username.isEmpty()){
                usernameEditText.setError("Username is required");
                return;
            }
            if(name.isEmpty()){
                fullNameEditText.setError("Full name is required");
                return;
            }
            if(fpTemplate == null && cardTag.isEmpty()){
                Toast.makeText(this, "Fingerprint template or Card Tag is required", Toast.LENGTH_SHORT).show();
                return;
            }
            if(!cardTag.isEmpty()){
                User user = dbService.findUserByFpOrCard(cardTag);
                if(user != null){
                    Toast.makeText(this, "Card Tag already registered to " + user.getName(), Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            dbService.addNewUser(name, username, fpTemplate, cardTag.isEmpty() ? null : cardTag, imageUrl.isEmpty() ? null : imageUrl);
            Toast.makeText(this, "User registered successfully", Toast.LENGTH_SHORT).show();
            this.finish();
        });
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        Log.e(TAG, "+ ON RESUME +");

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (blueToothReaderClient != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if(!blueToothReaderClient.isBluetoothServiceStarted()){
                blueToothReaderClient.startService();
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        Log.d(TAG, "onSupportNavigateUp: XX");
        this.finish();
        return super.onSupportNavigateUp();
    }

    @Override
    public void receiveCommand(byte[] databuf, int datasize) {
        if(blueToothReaderClient.fingerprintImage != null){
            fingerprintImage.setImageBitmap(blueToothReaderClient.fingerprintImage);
        }
    }

    @Override
    public void getConnectionState(int state) {
        Log.i(TAG, "MESSAGE_STATE_CHANGE: " + state);
        switch (state) {
            case BluetoothReaderService.STATE_CONNECTED:
                mToolbar.setSubtitle(blueToothReaderClient.getmConnectedDeviceName());
                break;
            case BluetoothReaderService.STATE_CONNECTING:
                mToolbar.setSubtitle(R.string.title_connecting);
                break;
            case BluetoothReaderService.STATE_LISTEN:
            case BluetoothReaderService.STATE_NONE:
                mToolbar.setSubtitle(R.string.title_not_connected);
                break;
        }
    }

    @Override
    public void sendToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void receiveIsoFingerPrintTemplate(String template) {
        Log.d(TAG, "receiveIsoFingerPrintTemplate: " + template);
        fpTemplate = template;
        registerFingerPrintsButton.setEnabled(false);
        registerButton.setEnabled(true);
    }

    @Override
    public void receiveCardData(String cardData) {
        Log.d(TAG, "receiveCardData: " + cardData);
        cardTagEditText.setText(cardData);
        registerCardButton.setEnabled(false);
        registerButton.setEnabled(true);
    }
}