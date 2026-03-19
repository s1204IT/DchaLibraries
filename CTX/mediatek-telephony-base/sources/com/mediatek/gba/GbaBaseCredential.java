package com.mediatek.gba;

import android.content.Context;
import android.net.Network;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import com.mediatek.gba.IGbaService;

public abstract class GbaBaseCredential {
    static final byte[] DEFAULT_UA_SECURITY_PROTOCOL_ID_HTTP = {1, 0, 0, 0, 2};
    static final byte[] DEFAULT_UA_SECURITY_PROTOCOL_ID_TLS = {1, 0, 1, 0, 47};
    protected static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();
    static final String NAFFQDN_PREFER = "original.naf.prefer";
    private static final String TAG = "GbaBaseCredential";
    protected static IGbaService sService;
    protected boolean mIsTlsEnabled;
    protected String mNafAddress;
    protected Network mNetwork;
    protected int mSubId;

    GbaBaseCredential() {
    }

    GbaBaseCredential(Context context, String str, int i) {
        this.mSubId = i;
        str = str.charAt(str.length() - 1) == '/' ? str.substring(0, str.length() - 1) : str;
        this.mIsTlsEnabled = true;
        this.mNafAddress = str.toLowerCase();
        if (this.mNafAddress.indexOf("http://") != -1) {
            this.mNafAddress = str.substring(7);
            this.mIsTlsEnabled = false;
        } else if (this.mNafAddress.indexOf("https://") != -1) {
            this.mNafAddress = str.substring(8);
            this.mIsTlsEnabled = true;
        }
        Log.d(TAG, "nafAddress:" + this.mNafAddress);
    }

    public void setTlsEnabled(boolean z) {
        this.mIsTlsEnabled = z;
    }

    public void setSubId(int i) {
        this.mSubId = i;
    }

    public void setNetwork(Network network) {
        if (network != null) {
            Log.i(TAG, "GBA dedicated network netid:" + network);
            this.mNetwork = network;
        }
    }

    public NafSessionKey getNafSessionKey() {
        byte[] bArr;
        boolean z;
        String property;
        String property2;
        NafSessionKey nafSessionKeyRunGbaAuthenticationForSubscriber;
        GbaCipherSuite byName;
        IBinder service;
        NafSessionKey nafSessionKey = null;
        try {
            service = ServiceManager.getService("GbaService");
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        if (service == null) {
            Log.i("debug", "The binder is null");
            return null;
        }
        sService = IGbaService.Stub.asInterface(service);
        try {
            bArr = DEFAULT_UA_SECURITY_PROTOCOL_ID_TLS;
            z = true;
            if (this.mIsTlsEnabled) {
                String property3 = System.getProperty("gba.ciper.suite", "");
                if (property3.length() > 0 && (byName = GbaCipherSuite.getByName(property3)) != null) {
                    byte[] code = byName.getCode();
                    bArr[3] = code[0];
                    bArr[4] = code[1];
                }
            } else {
                bArr = DEFAULT_UA_SECURITY_PROTOCOL_ID_HTTP;
            }
            if (this.mNetwork != null) {
                sService.setNetwork(this.mNetwork);
            }
            property = System.getProperty("digest.realm", "");
            property2 = System.getProperty(NAFFQDN_PREFER, "");
            Log.i(TAG, "realm:" + property);
            Log.i(TAG, "NAFFQDN_PREFER:" + property2);
        } catch (RemoteException e2) {
            e2.printStackTrace();
        }
        if (property.length() <= 0) {
            return null;
        }
        if (property2.length() == 0) {
            String[] strArrSplit = property.split(";");
            this.mNafAddress = strArrSplit[0].substring(strArrSplit[0].indexOf("@") + 1);
        }
        Log.i(TAG, "NAF FQDN:" + this.mNafAddress);
        Log.d(TAG, "gba.auth: " + System.getProperty("gba.auth"));
        if ("401".equals(System.getProperty("gba.auth"))) {
            System.setProperty("gba.auth", "");
        } else {
            z = false;
        }
        Log.d(TAG, "forceRun: " + z);
        if (-1 == this.mSubId) {
            nafSessionKeyRunGbaAuthenticationForSubscriber = sService.runGbaAuthentication(this.mNafAddress, bArr, z);
        } else {
            nafSessionKeyRunGbaAuthenticationForSubscriber = sService.runGbaAuthenticationForSubscriber(this.mNafAddress, bArr, z, this.mSubId);
        }
        nafSessionKey = nafSessionKeyRunGbaAuthenticationForSubscriber;
        if (nafSessionKey != null && nafSessionKey.getException() != null && (nafSessionKey.getException() instanceof IllegalStateException)) {
            String message = ((IllegalStateException) nafSessionKey.getException()).getMessage();
            if ("HTTP 403 Forbidden".equals(message)) {
                Log.i(TAG, "GBA hit 403");
                System.setProperty("gba.auth", "403");
            } else if ("HTTP 400 Bad Request".equals(message)) {
                Log.i(TAG, "GBA hit 400");
            }
        }
        return nafSessionKey;
    }
}
