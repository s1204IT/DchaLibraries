package com.google.android.mms.pdu;

import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class EncodedStringValue implements Cloneable {
    private static final boolean DEBUG = false;
    private static final boolean LOCAL_LOGV = false;
    private static final String TAG = "EncodedStringValue";
    protected int mCharacterSet;
    protected byte[] mData;

    public EncodedStringValue(int i, byte[] bArr) {
        if (bArr == null) {
            throw new NullPointerException("EncodedStringValue: Text-string is null.");
        }
        this.mCharacterSet = i;
        this.mData = new byte[bArr.length];
        System.arraycopy(bArr, 0, this.mData, 0, bArr.length);
    }

    public EncodedStringValue(byte[] bArr) {
        this(106, bArr);
    }

    public EncodedStringValue(String str) {
        try {
            this.mData = str.getBytes("utf-8");
            this.mCharacterSet = 106;
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Default encoding must be supported.", e);
        }
    }

    public int getCharacterSet() {
        return this.mCharacterSet;
    }

    public void setCharacterSet(int i) {
        this.mCharacterSet = i;
    }

    public byte[] getTextString() {
        byte[] bArr = new byte[this.mData.length];
        System.arraycopy(this.mData, 0, bArr, 0, this.mData.length);
        return bArr;
    }

    public void setTextString(byte[] bArr) {
        if (bArr == null) {
            throw new NullPointerException("EncodedStringValue: Text-string is null.");
        }
        this.mData = new byte[bArr.length];
        System.arraycopy(bArr, 0, this.mData, 0, bArr.length);
    }

    public String getString() {
        if (this.mCharacterSet == 0) {
            return new String(this.mData);
        }
        try {
            return new String(this.mData, CharacterSets.getMimeName(this.mCharacterSet));
        } catch (UnsupportedEncodingException e) {
            try {
                return new String(this.mData, CharacterSets.MIMENAME_ISO_8859_1);
            } catch (UnsupportedEncodingException e2) {
                return new String(this.mData);
            }
        }
    }

    public void appendTextString(byte[] bArr) {
        if (bArr == null) {
            throw new NullPointerException("Text-string is null.");
        }
        if (this.mData == null) {
            this.mData = new byte[bArr.length];
            System.arraycopy(bArr, 0, this.mData, 0, bArr.length);
            return;
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            byteArrayOutputStream.write(this.mData);
            byteArrayOutputStream.write(bArr);
            this.mData = byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            throw new NullPointerException("appendTextString: failed when write a new Text-string");
        }
    }

    public Object clone() throws CloneNotSupportedException {
        super.clone();
        int length = this.mData.length;
        byte[] bArr = new byte[length];
        System.arraycopy(this.mData, 0, bArr, 0, length);
        try {
            return new EncodedStringValue(this.mCharacterSet, bArr);
        } catch (Exception e) {
            Log.e(TAG, "failed to clone an EncodedStringValue: " + this);
            e.printStackTrace();
            throw new CloneNotSupportedException(e.getMessage());
        }
    }

    public EncodedStringValue[] split(String str) {
        String[] strArrSplit = getString().split(str);
        EncodedStringValue[] encodedStringValueArr = new EncodedStringValue[strArrSplit.length];
        for (int i = 0; i < encodedStringValueArr.length; i++) {
            try {
                encodedStringValueArr[i] = new EncodedStringValue(this.mCharacterSet, strArrSplit[i].getBytes());
            } catch (NullPointerException e) {
                return null;
            }
        }
        return encodedStringValueArr;
    }

    public static EncodedStringValue[] extract(String str) {
        String[] strArrSplit = str.split(";");
        ArrayList arrayList = new ArrayList();
        for (int i = 0; i < strArrSplit.length; i++) {
            if (strArrSplit[i].length() > 0) {
                arrayList.add(new EncodedStringValue(strArrSplit[i]));
            }
        }
        int size = arrayList.size();
        if (size > 0) {
            return (EncodedStringValue[]) arrayList.toArray(new EncodedStringValue[size]);
        }
        return null;
    }

    public static String concat(EncodedStringValue[] encodedStringValueArr) {
        StringBuilder sb = new StringBuilder();
        int length = encodedStringValueArr.length - 1;
        for (int i = 0; i <= length; i++) {
            sb.append(encodedStringValueArr[i].getString());
            if (i < length) {
                sb.append(";");
            }
        }
        return sb.toString();
    }

    public static EncodedStringValue copy(EncodedStringValue encodedStringValue) {
        if (encodedStringValue == null) {
            return null;
        }
        return new EncodedStringValue(encodedStringValue.mCharacterSet, encodedStringValue.mData);
    }

    public static EncodedStringValue[] encodeStrings(String[] strArr) {
        int length = strArr.length;
        if (length > 0) {
            EncodedStringValue[] encodedStringValueArr = new EncodedStringValue[length];
            for (int i = 0; i < length; i++) {
                encodedStringValueArr[i] = new EncodedStringValue(strArr[i]);
            }
            return encodedStringValueArr;
        }
        return null;
    }
}
