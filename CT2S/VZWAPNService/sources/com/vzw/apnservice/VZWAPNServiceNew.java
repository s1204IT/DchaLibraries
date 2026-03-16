package com.vzw.apnservice;

import android.annotation.TargetApi;
import android.app.IntentService;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.verizon.vzwavslibrary.VZWAVSInterface;
import com.verizon.vzwcommonserviceinterface.CommonInterface;
import com.vzw.apnservice.Manifest;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class VZWAPNServiceNew extends IntentService {
    private static final int CONNECTED = 3;
    private static final boolean DBG = true;
    private static final int REQUEST = 2;
    private static final int START = 1;
    private static final String TAG = "VZWAPNService";
    private static final String lock = "";
    private static final String startLock = "";
    private ConnectivityManager cm;
    private CommonInterface commonInterface;
    private Network globalNetwork;

    public VZWAPNServiceNew() {
        super("VZWAPNServiceNew");
    }

    @TargetApi(21)
    public int startUsingNetworkFeature(int networkType, String feature, boolean useLollipop) {
        int i;
        Log.v(TAG, "startUsingNetworkFeature: " + networkType + " " + feature);
        Log.v(TAG, "useLollipop = " + useLollipop);
        Log.v(TAG, "Build version = " + Build.VERSION.SDK_INT);
        if (useLollipop && Build.VERSION.SDK_INT >= 21) {
            Log.v(TAG, "Using lollipop");
            NetAvailable netAvailable = new NetAvailable();
            NetworkRequest networkRequest = new NetworkRequest.Builder().addCapability(5).build();
            this.cm.requestNetwork(networkRequest, netAvailable);
            synchronized ("") {
                try {
                    "".wait(15000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    this.cm.unregisterNetworkCallback(netAvailable);
                    i = -1;
                }
            }
            i = 0;
            return i;
        }
        Log.v(TAG, "Using pre-Lollipop");
        int result = this.cm.startUsingNetworkFeature(networkType, feature);
        Log.v(TAG, "startUsingNetworkFeature: result = " + result);
        return result;
    }

    private boolean is3GOnly() {
        TelephonyManager tm = (TelephonyManager) getSystemService("phone");
        ConnectivityManager cm = (ConnectivityManager) getSystemService("connectivity");
        NetworkInfo networkInfo = cm.getNetworkInfo(1);
        if (tm.getNetworkType() != 13 && tm.getNetworkType() != 14 && !networkInfo.isConnectedOrConnecting()) {
            return DBG;
        }
        return false;
    }

    public boolean requestRouteToHost(int networkType, InetAddress ip, boolean useLollipop) {
        Boolean result = false;
        if (Build.VERSION.SDK_INT >= 21 && useLollipop) {
            result = Boolean.valueOf(DBG);
        } else if (Build.VERSION.SDK_INT > 14) {
            Log.v(TAG, ">= ICS: requestRouteToHost: " + networkType);
            try {
                Method requestRouteToHostAddress = this.cm.getClass().getMethod("requestRouteToHostAddress", Integer.TYPE, InetAddress.class);
                result = (Boolean) requestRouteToHostAddress.invoke(this.cm, Integer.valueOf(networkType), ip);
                Log.v(TAG, "requestRouteToHost: result = " + result);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e2) {
                e2.printStackTrace();
            } catch (InvocationTargetException e3) {
                e3.printStackTrace();
            }
        } else {
            Log.v(TAG, "< ICS: requestRouteToHost: " + networkType);
            result = Boolean.valueOf(this.cm.requestRouteToHost(networkType, getIPasInt(ip)));
        }
        return result.booleanValue();
    }

    private int getIPasInt(InetAddress hostAddress) {
        int[] ip = convertByteToInt(hostAddress.getAddress());
        return (ip[CONNECTED] * 16777216) + (ip[REQUEST] * 65536) + (ip[1] * 256) + ip[0];
    }

    private int[] convertByteToInt(byte[] address) {
        int[] result = new int[address.length];
        int i = 0;
        for (byte b : address) {
            if (b < 0) {
                result[i] = b + 256;
            } else {
                result[i] = b;
            }
            i++;
        }
        return result;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Intent localIntent = new Intent(intent);
        byte[] response = {127};
        this.commonInterface = new CommonInterface(this, localIntent);
        String packageName = this.commonInterface.getPakageName();
        if (validatePermission(packageName)) {
            response = processRequest(localIntent);
        }
        Bundle bundle = new Bundle();
        bundle.putByteArray("response", response);
        if (this.globalNetwork != null) {
            bundle.putParcelable("network", this.globalNetwork);
        }
        try {
            this.commonInterface.sendResponse(bundle);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private boolean validatePermission(String packageName) {
        PackageManager pm = getPackageManager();
        boolean match = VZWAVSInterface.isPackageAuthorized(this, packageName, "VZWAPPSAPN");
        if (!match) {
            match = pm.checkPermission(Manifest.permission.APNPERMISSION, packageName) == 0 ? DBG : false;
            if (match) {
                Log.v(TAG, "Permission granted: com.vzw.APNPERMISSION match");
            }
        }
        if (!match) {
            try {
                PackageInfo pi = pm.getPackageInfo(packageName, 64);
                if ((pi.applicationInfo.flags & 1) > 0) {
                    Log.v(TAG, "Permission granted: System app");
                    return DBG;
                }
                Signature[] localSigs = pm.getPackageInfo(getPackageName(), 64).signatures;
                Signature[] remoteSigs = pi.signatures;
                for (Signature sig1 : remoteSigs) {
                    if (localSigs != null) {
                        int len$ = localSigs.length;
                        int i$ = 0;
                        while (true) {
                            if (i$ < len$) {
                                Signature sig2 = localSigs[i$];
                                if (!sig1.equals(sig2)) {
                                    i$++;
                                } else {
                                    match = DBG;
                                    break;
                                }
                            }
                        }
                    }
                }
                if (match) {
                    Log.v(TAG, "Permission granted: Signature match");
                    return match;
                }
                return match;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
                return match;
            }
        }
        return match;
    }

    private byte[] processRequest(Intent intent) {
        String feature;
        int networkType;
        byte[] buffer = {-1};
        this.cm = (ConnectivityManager) getSystemService("connectivity");
        if (!is3GOnly() && Build.VERSION.SDK_INT >= 14) {
            feature = "enableCBS";
            networkType = 12;
        } else {
            feature = "enableHIPRI";
            networkType = 5;
        }
        Bundle extras = intent.getExtras();
        if (extras != null) {
            boolean useLollipop = extras.getBoolean("lollipop");
            Log.v(TAG, "useLollipop = " + useLollipop);
            switch (extras.getInt("command")) {
                case 1:
                    int result = startUsingNetworkFeature(0, feature, useLollipop);
                    buffer[0] = (byte) result;
                    break;
                case REQUEST:
                    String host = extras.getString("host");
                    MyThread myThread = new MyThread(host, "", buffer, networkType, useLollipop);
                    myThread.start();
                    synchronized ("") {
                        try {
                            "".wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        break;
                    }
                    break;
                case CONNECTED:
                    int result2 = this.cm.getNetworkInfo(networkType).isConnected() ? 1 : 0;
                    buffer[0] = (byte) result2;
                    break;
            }
        }
        return buffer;
    }

    private void routeDNS(int networkType) {
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        try {
            Class<?> sp = cl.loadClass("android.os.SystemProperties");
            Class<?>[] params = {String.class};
            Method m = sp.getMethod("get", params);
            for (int i = 1; i <= 4; i++) {
                String dns = (String) m.invoke(sp, "net.dns" + i);
                requestRouteToHost(networkType, InetAddress.getByName(dns), false);
            }
            synchronized (this) {
                try {
                    wait(1000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (ClassNotFoundException e2) {
            e2.printStackTrace();
        } catch (IllegalAccessException e3) {
            e3.printStackTrace();
        } catch (IllegalArgumentException e4) {
            e4.printStackTrace();
        } catch (NoSuchMethodException e5) {
            e5.printStackTrace();
        } catch (SecurityException e6) {
            e6.printStackTrace();
        } catch (InvocationTargetException e7) {
            e7.printStackTrace();
        } catch (UnknownHostException e8) {
            e8.printStackTrace();
        }
    }

    private class MyThread extends Thread {
        byte[] buffer;
        String host;
        int networkType;
        String socket;
        private boolean useLollipop;

        public MyThread(String host, String socket, byte[] buffer, int networkType, boolean useLollipop) {
            this.socket = socket;
            this.buffer = buffer;
            this.host = host;
            this.networkType = networkType;
            this.useLollipop = useLollipop;
        }

        @Override
        public void run() {
            try {
                if (!this.useLollipop) {
                    VZWAPNServiceNew.this.routeDNS(this.networkType);
                    InetAddress ip = InetAddress.getByName(this.host);
                    this.buffer[0] = (byte) (VZWAPNServiceNew.this.requestRouteToHost(this.networkType, ip, this.useLollipop) ? 0 : 1);
                } else {
                    this.buffer[0] = 0;
                }
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            synchronized ("") {
                "".notifyAll();
            }
        }
    }

    @TargetApi(21)
    private class NetAvailable extends ConnectivityManager.NetworkCallback {
        private NetAvailable() {
        }

        @Override
        public void onAvailable(Network network) {
            Log.v(VZWAPNServiceNew.TAG, "onAvailable called");
            VZWAPNServiceNew.this.globalNetwork = network;
            synchronized ("") {
                "".notifyAll();
            }
        }
    }
}
