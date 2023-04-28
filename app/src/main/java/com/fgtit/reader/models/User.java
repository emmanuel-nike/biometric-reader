package com.fgtit.reader.models;

import android.util.Log;

public class User {
    private String name, username, userType, imageUrl, fingerPrintData, cardData;

    public User(String name, String username, String fingerPrintData, String cardData, String imageUrl, String userType){
        Log.e("User", "User: " + name + " " + username + " " + fingerPrintData + " " + cardData + " " + imageUrl + " " + userType);
        this.name = name;
        this.username = username;
        this.userType = userType;
        this.fingerPrintData = fingerPrintData;
        this.cardData = cardData;
        this.imageUrl = imageUrl;
    }

    public String getName(){
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhotoUrl() {
        return imageUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public String getFingerPrintData() {
        return fingerPrintData;
    }

    public void setFingerPrintData(String fingerPrintData) {
        this.fingerPrintData = fingerPrintData;
    }

    public String getCardData() {
        return cardData;
    }

    public void setCardData(String cardData) {
        this.cardData = cardData;
    }
}
