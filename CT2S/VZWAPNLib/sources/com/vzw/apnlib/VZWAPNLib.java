package com.vzw.apnlib;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Network;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import com.verizon.vzwcommonserviceinterface.CommonInterface;
import java.net.InetAddress;

public class VZWAPNLib {
    public static final String COMMAND = "command";
    private static final boolean DBG = true;
    public static final String HOST = "host";
    public static final String LOLLIPOP = "lollipop";
    private static final String TAG = "VZWAPNLib";
    private static VZWAPNLib lib;
    private Context context;
    private Network network;

    private VZWAPNLib(Context context) {
        this.context = context;
        PackageManager pm = this.context.getPackageManager();
        String[] packages = pm.getPackagesForUid(Binder.getCallingUid());
        for (String pkgName : packages) {
            Log.v(TAG, "pkgName: " + pkgName);
        }
    }

    public static VZWAPNLib getInstance(Context context) {
        if (lib == null) {
            lib = new VZWAPNLib(context);
        }
        return lib;
    }

    public int startUsingNetworkFeature() {
        return startUsingNetworkFeature(false);
    }

    public int startUsingNetworkFeature(boolean useLollipop) {
        Log.v(TAG, "called startUsingNetworkFeature");
        Intent intent = new Intent("com.vzw.apnservicenew");
        intent.setClassName("com.vzw.apnservice", "com.vzw.apnservice.VZWAPNServiceNew");
        intent.putExtra(COMMAND, 1);
        intent.putExtra(LOLLIPOP, useLollipop);
        int result = -1;
        Bundle bundle = newProcessCommand(intent);
        if (bundle != null) {
            if (Build.VERSION.SDK_INT >= 21) {
                this.network = (Network) bundle.getParcelable("network");
            }
            byte[] commandResult = bundle.getByteArray("response");
            if (commandResult != null) {
                result = commandResult[0];
            }
        }
        Log.v(TAG, "result = " + result);
        if (result == 127) {
            throw new SecurityException("App is not authorized to use Apps APN");
        }
        return result;
    }

    private Bundle newProcessCommand(Intent intent) {
        CommonInterface commonInterface = new CommonInterface(this.context, intent);
        return commonInterface.sendCommand(null);
    }

    public boolean requestRouteToHost(String host) {
        Log.v(TAG, "called requestRouteToHost");
        return requestRouteToHost(host, false);
    }

    public boolean requestRouteToHost(String host, boolean useLollipop) {
        byte[] response;
        Log.v(TAG, "called requestRouteToHost");
        Intent intent = new Intent("com.vzw.apnservicenew");
        intent.setClassName("com.vzw.apnservice", "com.vzw.apnservice.VZWAPNServiceNew");
        intent.putExtra(COMMAND, 2);
        intent.putExtra(HOST, host);
        intent.putExtra(LOLLIPOP, useLollipop);
        Bundle bundle = newProcessCommand(intent);
        boolean result = false;
        if (bundle != null && (response = bundle.getByteArray("response")) != null) {
            Log.v(TAG, "response = " + ((int) response[0]));
            result = response[0] == 0 ? DBG : false;
            if (response[0] == 127) {
                throw new SecurityException("App is not authorized to use Apps APN");
            }
        }
        return result;
    }

    public boolean requestRouteToHost(InetAddress host) {
        return requestRouteToHost(host, false);
    }

    public boolean requestRouteToHost(InetAddress host, boolean useLollipop) {
        return requestRouteToHost(host.getHostAddress(), useLollipop);
    }

    public int stopUsingNetworkFeature() {
        return -1;
    }

    public boolean isConnected() {
        Intent intent = new Intent("com.vzw.apnservicenew");
        intent.setClassName("com.vzw.apnservice", "com.vzw.apnservice.VZWAPNServiceNew");
        intent.putExtra(COMMAND, 3);
        intent.putExtra(LOLLIPOP, false);
        Bundle bundle = newProcessCommand(intent);
        byte[] response = {-1};
        if (bundle != null) {
            response = bundle.getByteArray("response");
            if (response[0] == 127) {
                throw new SecurityException("App is not authorized to use Apps APN");
            }
        }
        if (response[0] == 1) {
            return DBG;
        }
        return false;
    }

    public Network getNetwork() {
        return this.network;
    }
}
