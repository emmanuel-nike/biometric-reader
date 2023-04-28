package com.fgtit.data;

import android.util.Base64;

public class Conversions {

	
	private static Conversions mCom=null;
	
	public static Conversions getInstance(){
		if(mCom==null){
			mCom=new Conversions();
		}
		return mCom;
	}
	
	public native int StdToIso(int itype,byte[] input,byte[] output);
	public native int IsoToStd(int itype,byte[] input,byte[] output);
	public native int GetDataType(byte[] input);
	public native int StdChangeCoord(byte[] input,int size,byte[] output,int dk);

	public byte[] hexStringToByteArray(String s) {
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
					+ Character.digit(s.charAt(i+1), 16));
		}
		return data;
	}

	public String byteArrayToHexString(byte[] b) {
		StringBuffer sb = new StringBuffer(b.length * 2);
		for (int i = 0; i < b.length; i++) {
			int v = b[i] & 0xff;
			if (v < 16) {
				sb.append('0');
			}
			sb.append(Integer.toHexString(v));
		}
		return sb.toString().toUpperCase();
	}

	
	public String IsoChangeCoord(byte[] input, int dk){
		int dt=GetDataType(input);
		switch(dt){
		case 1:{
				byte output[] =new byte[512];
				byte crddat[]=new byte[512];
				StdChangeCoord(input,256,crddat,dk);
				StdToIso(2,crddat,output);
				return Base64.encodeToString(output,0,378, Base64.DEFAULT);
			}			
		case 2:{
				byte output[] =new byte[512];
				byte stddat[]=new byte[512];
				byte crddat[]=new byte[512];
				IsoToStd(1,input,stddat);
				StdChangeCoord(stddat,256,crddat,dk);
				StdToIso(2,crddat,output);
				return Base64.encodeToString(output,0,378, Base64.DEFAULT);
			}
		case 3:{
				byte output[] =new byte[512];
				byte stddat[]=new byte[512];
				byte crddat[]=new byte[512];
				IsoToStd(2,input,stddat);
				StdChangeCoord(stddat,256,crddat,dk);
				StdToIso(2,crddat,output);
				return Base64.encodeToString(output,0,378, Base64.DEFAULT);
			}
		}
		return "";
	}

	static {
		System.loadLibrary("conversions");
	}
}
