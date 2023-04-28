package com.fgtit.reader.service;

public interface BluetoothCallback {
    void receiveCommand(byte[] databuf, int datasize);
    void getConnectionState(int state);
    void sendToast(String msg);

    void receiveIsoFingerPrintTemplate(String template);

    void receiveCardData(String cardData);
}
