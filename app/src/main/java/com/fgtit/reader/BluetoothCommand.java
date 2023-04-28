package com.fgtit.reader;

public class BluetoothCommand {
    public final static byte CMD_PASSWORD = 0x01;    //Password
    public final static byte CMD_ENROLID = 0x02;        //Enroll in Device
    public final static byte CMD_VERIFY = 0x03;        //Verify in Device
    public final static byte CMD_IDENTIFY = 0x04;    //Identify in Device
    public final static byte CMD_DELETEID = 0x05;    //Delete in Device
    public final static byte CMD_CLEARID = 0x06;        //Clear in Device

    public final static byte CMD_ENROLHOST = 0x07;    //Enroll to Host
    public final static byte CMD_CAPTUREHOST = 0x08;    //Caputre to Host
    public final static byte CMD_MATCH = 0x09;        //Match
    public final static byte CMD_GETIMAGE = 0x30;      //GETIMAGE
    public final static byte CMD_GETCHAR = 0x31;       //GETDATA

    public final static byte CMD_WRITEFPCARD = 0x0A;    //Write Card Data
    public final static byte CMD_READFPCARD = 0x0B;    //Read Card Data
    public final static byte CMD_CARDSN = 0x55;        //Read Card Sn
    public final static byte CMD_GETSN = 0x10;

    public final static byte CMD_FPCARDMATCH = 0x13;   //

    public final static byte CMD_WRITEDATACARD = 0x14;    //Write Card Data
    public final static byte CMD_READDATACARD = 0x15;     //Read Card Data

    public final static byte CMD_PRINTCMD = 0x20;        //Printer Print
    public final static byte CMD_GETBAT = 0x21;
    public final static byte CMD_UPCARDSN = 0x43;
    public final static byte CMD_GET_VERSION = 0x22;        //Version
    public final static byte CMD_TEST_UART = 0x62;
    public final static byte CMD_TWO_TEMTEURE = (byte) 0x80;
}
