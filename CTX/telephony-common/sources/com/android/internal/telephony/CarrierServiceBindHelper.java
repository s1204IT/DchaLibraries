package com.android.internal.telephony;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Process;
import android.os.SystemClock;
import android.os.UserHandle;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.content.PackageMonitor;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.List;

public class CarrierServiceBindHelper {
    private static final int EVENT_PERFORM_IMMEDIATE_UNBIND = 1;
    private static final int EVENT_REBIND = 0;
    private static final String LOG_TAG = "CarrierSvcBindHelper";
    private static final int UNBIND_DELAY_MILLIS = 30000;
    private AppBinding[] mBindings;
    private Context mContext;
    private String[] mLastSimState;
    private final PackageMonitor mPackageMonitor = new CarrierServicePackageMonitor();
    private BroadcastReceiver mUserUnlockedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            CarrierServiceBindHelper.log("Received " + action);
            if ("android.intent.action.USER_UNLOCKED".equals(action)) {
                for (int i = 0; i < CarrierServiceBindHelper.this.mBindings.length; i++) {
                    CarrierServiceBindHelper.this.mBindings[i].rebind();
                }
            }
        }
    };
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            CarrierServiceBindHelper.log("mHandler: " + message.what);
            switch (message.what) {
                case 0:
                    AppBinding appBinding = (AppBinding) message.obj;
                    CarrierServiceBindHelper.log("Rebinding if necessary for phoneId: " + appBinding.getPhoneId());
                    appBinding.rebind();
                    break;
                case 1:
                    ((AppBinding) message.obj).performImmediateUnbind();
                    break;
            }
        }
    };

    public CarrierServiceBindHelper(Context context) {
        this.mContext = context;
        int phoneCount = TelephonyManager.from(context).getPhoneCount();
        this.mBindings = new AppBinding[phoneCount];
        this.mLastSimState = new String[phoneCount];
        for (int i = 0; i < phoneCount; i++) {
            this.mBindings[i] = new AppBinding(i);
        }
        this.mPackageMonitor.register(context, this.mHandler.getLooper(), UserHandle.ALL, false);
        this.mContext.registerReceiverAsUser(this.mUserUnlockedReceiver, UserHandle.SYSTEM, new IntentFilter("android.intent.action.USER_UNLOCKED"), null, this.mHandler);
    }

    void updateForPhoneId(int i, String str) {
        log("update binding for phoneId: " + i + " simState: " + str);
        if (!SubscriptionManager.isValidPhoneId(i) || TelephonyManager.from(this.mContext).getPhoneCount() == 0 || TextUtils.isEmpty(str) || i >= this.mLastSimState.length || str.equals(this.mLastSimState[i])) {
            return;
        }
        this.mLastSimState[i] = str;
        this.mHandler.sendMessage(this.mHandler.obtainMessage(0, this.mBindings[i]));
    }

    private class AppBinding {
        private int bindCount;
        private String carrierPackage;
        private String carrierServiceClass;
        private CarrierServiceConnection connection;
        private long lastBindStartMillis;
        private long lastUnbindMillis;
        private long mUnbindScheduledUptimeMillis = -1;
        private int phoneId;
        private int unbindCount;

        public AppBinding(int i) {
            this.phoneId = i;
        }

        public int getPhoneId() {
            return this.phoneId;
        }

        public String getPackage() {
            return this.carrierPackage;
        }

        void rebind() {
            String className;
            Bundle bundle;
            String message;
            List carrierPackageNamesForIntentAndPhone = TelephonyManager.from(CarrierServiceBindHelper.this.mContext).getCarrierPackageNamesForIntentAndPhone(new Intent("android.service.carrier.CarrierService"), this.phoneId);
            if (carrierPackageNamesForIntentAndPhone == null || carrierPackageNamesForIntentAndPhone.size() <= 0) {
                CarrierServiceBindHelper.log("No carrier app for: " + this.phoneId);
                unbind(false);
                return;
            }
            CarrierServiceBindHelper.log("Found carrier app: " + carrierPackageNamesForIntentAndPhone);
            String str = (String) carrierPackageNamesForIntentAndPhone.get(0);
            if (!TextUtils.equals(this.carrierPackage, str)) {
                unbind(true);
            }
            Intent intent = new Intent("android.service.carrier.CarrierService");
            intent.setPackage(str);
            ResolveInfo resolveInfoResolveService = CarrierServiceBindHelper.this.mContext.getPackageManager().resolveService(intent, 128);
            if (resolveInfoResolveService != null) {
                bundle = resolveInfoResolveService.serviceInfo.metaData;
                className = resolveInfoResolveService.getComponentInfo().getComponentName().getClassName();
            } else {
                className = null;
                bundle = null;
            }
            if (bundle == null || !bundle.getBoolean("android.service.carrier.LONG_LIVED_BINDING", false)) {
                CarrierServiceBindHelper.log("Carrier app does not want a long lived binding");
                unbind(true);
                return;
            }
            if (!TextUtils.equals(this.carrierServiceClass, className)) {
                unbind(true);
            } else if (this.connection != null) {
                cancelScheduledUnbind();
                return;
            }
            this.carrierPackage = str;
            this.carrierServiceClass = className;
            CarrierServiceBindHelper.log("Binding to " + this.carrierPackage + " for phone " + this.phoneId);
            this.bindCount = this.bindCount + 1;
            this.lastBindStartMillis = System.currentTimeMillis();
            this.connection = new CarrierServiceConnection();
            try {
            } catch (SecurityException e) {
                message = e.getMessage();
            }
            if (CarrierServiceBindHelper.this.mContext.bindServiceAsUser(intent, this.connection, 67108865, CarrierServiceBindHelper.this.mHandler, Process.myUserHandle())) {
                return;
            }
            message = "bindService returned false";
            CarrierServiceBindHelper.log("Unable to bind to " + this.carrierPackage + " for phone " + this.phoneId + ". Error: " + message);
            unbind(true);
        }

        void unbind(boolean z) {
            if (this.connection == null) {
                return;
            }
            if (z || !this.connection.connected) {
                cancelScheduledUnbind();
                performImmediateUnbind();
            } else if (this.mUnbindScheduledUptimeMillis == -1) {
                this.mUnbindScheduledUptimeMillis = SystemClock.uptimeMillis() + 30000;
                CarrierServiceBindHelper.log("Scheduling unbind in 30000 millis");
                CarrierServiceBindHelper.this.mHandler.sendMessageAtTime(CarrierServiceBindHelper.this.mHandler.obtainMessage(1, this), this.mUnbindScheduledUptimeMillis);
            }
        }

        private void performImmediateUnbind() {
            this.unbindCount++;
            this.lastUnbindMillis = System.currentTimeMillis();
            this.carrierPackage = null;
            this.carrierServiceClass = null;
            CarrierServiceBindHelper.log("Unbinding from carrier app");
            CarrierServiceBindHelper.this.mContext.unbindService(this.connection);
            this.connection = null;
            this.mUnbindScheduledUptimeMillis = -1L;
        }

        private void cancelScheduledUnbind() {
            CarrierServiceBindHelper.this.mHandler.removeMessages(1);
            this.mUnbindScheduledUptimeMillis = -1L;
        }

        public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
            printWriter.println("Carrier app binding for phone " + this.phoneId);
            printWriter.println("  connection: " + this.connection);
            printWriter.println("  bindCount: " + this.bindCount);
            printWriter.println("  lastBindStartMillis: " + this.lastBindStartMillis);
            printWriter.println("  unbindCount: " + this.unbindCount);
            printWriter.println("  lastUnbindMillis: " + this.lastUnbindMillis);
            printWriter.println("  mUnbindScheduledUptimeMillis: " + this.mUnbindScheduledUptimeMillis);
            printWriter.println();
        }
    }

    private class CarrierServiceConnection implements ServiceConnection {
        private boolean connected;

        private CarrierServiceConnection() {
        }

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            CarrierServiceBindHelper.log("Connected to carrier app: " + componentName.flattenToString());
            this.connected = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            CarrierServiceBindHelper.log("Disconnected from carrier app: " + componentName.flattenToString());
            this.connected = false;
        }

        public String toString() {
            return "CarrierServiceConnection[connected=" + this.connected + "]";
        }
    }

    private class CarrierServicePackageMonitor extends PackageMonitor {
        private CarrierServicePackageMonitor() {
        }

        public void onPackageAdded(String str, int i) {
            evaluateBinding(str, true);
        }

        public void onPackageRemoved(String str, int i) {
            evaluateBinding(str, true);
        }

        public void onPackageUpdateFinished(String str, int i) {
            evaluateBinding(str, true);
        }

        public void onPackageModified(String str) {
            evaluateBinding(str, false);
        }

        public boolean onHandleForceStop(Intent intent, String[] strArr, int i, boolean z) {
            if (z) {
                for (String str : strArr) {
                    evaluateBinding(str, true);
                }
            }
            return super.onHandleForceStop(intent, strArr, i, z);
        }

        private void evaluateBinding(String str, boolean z) {
            for (AppBinding appBinding : CarrierServiceBindHelper.this.mBindings) {
                String str2 = appBinding.getPackage();
                boolean zEquals = str.equals(str2);
                if (zEquals) {
                    CarrierServiceBindHelper.log(str + " changed and corresponds to a phone. Rebinding.");
                }
                if (str2 == null || zEquals) {
                    if (z) {
                        appBinding.unbind(true);
                    }
                    appBinding.rebind();
                }
            }
        }
    }

    private static void log(String str) {
        Log.d(LOG_TAG, str);
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("CarrierServiceBindHelper:");
        for (AppBinding appBinding : this.mBindings) {
            appBinding.dump(fileDescriptor, printWriter, strArr);
        }
    }
}
