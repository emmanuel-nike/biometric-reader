package com.fgtit.reader.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.fgtit.reader.models.User;

public class HomeViewModel extends ViewModel {

    private final MutableLiveData<String> mText;
    private final MutableLiveData<String> authMode;
    private final MutableLiveData<Boolean> mIsFpServiceRunning;
    private final MutableLiveData<Boolean> mIsConnected;
    private final MutableLiveData<Boolean> mIsCardServiceRunning;
    private final MutableLiveData<User> mUser;
    private final MutableLiveData<String> mServerResponse;
    public Boolean isFpServiceRunning = false;
    public Boolean isCardServiceRunning = false;
    public Boolean isConnected = false;

    public HomeViewModel() {
        this.authMode = new MutableLiveData<>();
        this.mIsFpServiceRunning = new MutableLiveData<>();
        this.mIsCardServiceRunning = new MutableLiveData<>();
        this.mIsConnected = new MutableLiveData<>();
        this.mText = new MutableLiveData<>();
        this.mUser = new MutableLiveData<>();
        this.mServerResponse = new MutableLiveData<>();

        this.authMode.setValue("clockin");
    }

    public LiveData<String> getText() {
        return mText;
    }

    public LiveData<String> getAuthMode() {
        return authMode;
    }

    public LiveData<Boolean> getIsFpServiceRunning() {
        return mIsFpServiceRunning;
    }

    public LiveData<Boolean> getIsCardServiceRunning() {
        return mIsCardServiceRunning;
    }

    public LiveData<Boolean> getIsConnected() {
        return mIsConnected;
    }

    public LiveData<User> getUser() {
        return mUser;
    }

    public LiveData<String> getServerResponse() {
        return mServerResponse;
    }

    public void setFpServiceRunning(boolean isRunning) {
        this.mIsCardServiceRunning.setValue(false);
        this.isCardServiceRunning = false;
        this.mIsFpServiceRunning.setValue(isRunning);
        this.isFpServiceRunning = isRunning;
    }

    public void setCardServiceRunning(boolean isRunning) {
        this.mIsFpServiceRunning.setValue(false);
        this.isFpServiceRunning = false;
        this.mIsCardServiceRunning.setValue(isRunning);
        this.isCardServiceRunning = isRunning;
    }

    public void setUser(User user) {
        this.mUser.setValue(user);
    }

    public void setServerResponse(String response) {
        this.mServerResponse.setValue(response);
    }

    public void setConnected(boolean isConnected) {
        this.isConnected = isConnected;
        this.mIsConnected.setValue(isConnected);
    }

    public void setText(String text) {
        this.mText.setValue(text);
    }

    public void setAuthMode(String mode) {
        this.authMode.setValue(mode);
    }

    public void clearUser() {
        this.mUser.setValue(null);
    }
}