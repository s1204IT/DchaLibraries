package com.vzw.apnservice;

import android.app.Service;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.vzw.apnservice.Manifest;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class VZWAPNService extends Service {
    private static final Uri CONTENT_URI = Uri.parse("content://com.verizon.vzwavs.provider/apis");
    private static final boolean DBG = true;
    private static final String TAG = "VZWAPNService";
    private ConnectivityManager cm;
    private final int START = 1;
    private final int REQUEST = 2;
    private final int CONNECTED = 3;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public int startUsingNetworkFeature(int networkType, String feature) {
        Log.v(TAG, "startUsingNetworkFeature: " + networkType + " " + feature);
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

    public boolean requestRouteToHost(int networkType, InetAddress ip) {
        Boolean result = false;
        if (Build.VERSION.SDK_INT > 14) {
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
        return (ip[3] * 16777216) + (ip[2] * 65536) + (ip[1] * 256) + ip[0];
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
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            Intent localIntent = new Intent(intent);
            String socket = localIntent.getStringExtra("socket");
            LocalSocket ls = getSocket(socket);
            if (ls == null) {
                Log.v(TAG, "LocalSocket null");
            } else {
                Log.v(TAG, "LocalSocket good");
                try {
                    int uid = ls.getPeerCredentials().getUid();
                    int pid = ls.getPeerCredentials().getPid();
                    boolean match = validatePermission(uid, pid);
                    if (!match) {
                        localIntent.putExtra("exception", DBG);
                        Log.v(TAG, "Permission not granted");
                    }
                    processRequest(localIntent, ls);
                } catch (IOException e) {
                }
            }
        }
        return 1;
    }

    private boolean validatePermission(int uid, int pid) {
        ContentResolver cr = getContentResolver();
        PackageManager pm = getPackageManager();
        boolean match = false;
        String[] packages = pm.getPackagesForUid(uid);
        Cursor cursor = cr.query(CONTENT_URI, null, packages[0], null, null);
        if (cursor != null && cursor.moveToFirst()) {
            String apis = cursor.getString(0);
            match = apis.contains("VZWAPPSAPN");
        }
        if (match) {
            Log.v(TAG, "Permission granted: AVS match");
        }
        if (!match) {
            match = checkPermission(Manifest.permission.APNPERMISSION, pid, uid) == 0 ? DBG : false;
            if (match) {
                Log.v(TAG, "Permission granted: com.vzw.APNPERMISSION match");
            }
        }
        if (!match) {
            try {
                PackageInfo pi = pm.getPackageInfo(packages[0], 64);
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

    private void processRequest(Intent intent, LocalSocket ls) {
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
            boolean exception = extras.getBoolean("exception");
            if (exception) {
                buffer[0] = 127;
                sendResult(buffer, ls);
                return;
            }
            switch (extras.getInt("command")) {
                case com.verizon.vzwcommonserviceinterface.BuildConfig.VERSION_CODE:
                    int result = startUsingNetworkFeature(0, feature);
                    buffer[0] = (byte) result;
                    sendResult(buffer, ls);
                    break;
                case 2:
                    String host = extras.getString("host");
                    new MyThread(host, "", buffer, networkType, ls).start();
                    break;
                case 3:
                    int result2 = this.cm.getNetworkInfo(networkType).isConnected() ? 1 : 0;
                    buffer[0] = (byte) result2;
                    sendResult(buffer, ls);
                    break;
            }
        }
    }

    private void routeDNS(int networkType) {
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        try {
            Class<?> sp = cl.loadClass("android.os.SystemProperties");
            Class<?>[] params = {String.class};
            Method m = sp.getMethod("get", params);
            for (int i = 1; i <= 4; i++) {
                String dns = (String) m.invoke(sp, "net.dns" + i);
                requestRouteToHost(networkType, InetAddress.getByName(dns));
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

    private LocalSocket getSocket(String socket) {
        LocalSocket ls;
        if (socket == null || socket.isEmpty()) {
            return null;
        }
        LocalSocketAddress rsa = new LocalSocketAddress(socket, LocalSocketAddress.Namespace.ABSTRACT);
        LocalSocketAddress lsa = new LocalSocketAddress("apnservsock." + socket, LocalSocketAddress.Namespace.ABSTRACT);
        try {
            ls = new LocalSocket();
            ls.bind(lsa);
            int counter = 0;
            do {
                try {
                    ls.connect(rsa);
                    Log.v(TAG, "ls.connect()");
                    break;
                } catch (Exception e) {
                    counter++;
                    System.out.println("ServiceAPN " + counter);
                    synchronized (this) {
                        try {
                            wait(500L);
                        } catch (InterruptedException e2) {
                            e2.printStackTrace();
                        }
                    }
                }
            } while (counter < 5);
        } catch (IOException e3) {
            ls = null;
        }
        if (ls != null && !ls.isConnected()) {
            try {
                ls.close();
            } catch (IOException e4) {
                e4.printStackTrace();
            }
            return null;
        }
        return ls;
    }

    private void sendResult(byte[] buffer, LocalSocket ls) {
        if (ls != null) {
            try {
                Log.v(TAG, "sendResult - entered");
                BufferedOutputStream bos = new BufferedOutputStream(ls.getOutputStream());
                bos.write(buffer);
                bos.flush();
                bos.close();
                ls.close();
                Log.v(TAG, "sendResult - exited");
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    private class MyThread extends Thread {
        byte[] buffer;
        String host;
        private LocalSocket ls;
        int networkType;
        String socket;

        public MyThread(String host, String socket, byte[] buffer, int networkType, LocalSocket ls) {
            this.socket = socket;
            this.buffer = buffer;
            this.host = host;
            this.networkType = networkType;
            this.ls = ls;
        }

        @Override
        public void run() {
            try {
                VZWAPNService.this.routeDNS(this.networkType);
                InetAddress ip = InetAddress.getByName(this.host);
                this.buffer[0] = (byte) (VZWAPNService.this.requestRouteToHost(this.networkType, ip) ? 0 : 1);
                synchronized (this) {
                    wait(1000L);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (UnknownHostException e2) {
                e2.printStackTrace();
            }
            VZWAPNService.this.sendResult(this.buffer, this.ls);
        }
    }
}
