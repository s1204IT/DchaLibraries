package com.marvell.powergadget.eventrelay;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.app.IProcessObserver;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.powerhint.PowerHintManager;
import android.util.Log;
import java.lang.reflect.Field;
import java.util.List;

public class EventRelay extends Service {
    private IActivityManager mActivityManager;
    private IProcessObserver.Stub mProcessObserver = new IProcessObserver.Stub() {
        private Runnable mRunnable = new Runnable() {
            private String currentPkg;

            private int getTopActivityAndSendPowerHint() {
                EventRelay eventRelay = EventRelay.this;
                EventRelay.this.getBaseContext();
                ActivityManager am = (ActivityManager) eventRelay.getSystemService("activity");
                int i = 0;
                String pkgName = null;
                while (i < 5) {
                    if (Build.VERSION.SDK_INT < 21) {
                        if (am.getRunningTasks(1) == null) {
                            Log.e("EventRelay", "running task is null, ams is abnormal!!!");
                        } else {
                            ActivityManager.RunningTaskInfo mRunningTask = am.getRunningTasks(1).get(0);
                            if (mRunningTask == null) {
                                Log.e("EventRelay", "failed to get RunningTaskInfo");
                                return -1;
                            }
                            pkgName = mRunningTask.topActivity.getPackageName();
                            mRunningTask.topActivity.getShortClassName();
                        }
                    } else {
                        try {
                            Field processStateField = ActivityManager.RunningAppProcessInfo.class.getDeclaredField("processState");
                            List<ActivityManager.RunningAppProcessInfo> processes = EventRelay.this.mActivityManager.getRunningAppProcesses();
                            for (ActivityManager.RunningAppProcessInfo process : processes) {
                                if (process.importance <= 100 && process.importanceReasonCode == 0) {
                                    int state = processStateField.getInt(process);
                                    if (state == 2) {
                                        pkgName = process.processName;
                                        PackageInfo foregroundAppPackageInfo = EventRelay.this.getBaseContext().getPackageManager().getPackageInfo(pkgName, 0);
                                        foregroundAppPackageInfo.applicationInfo.loadLabel(EventRelay.this.getPackageManager()).toString();
                                    }
                                }
                            }
                        } catch (PackageManager.NameNotFoundException e) {
                            e.printStackTrace();
                            return -1;
                        } catch (RemoteException ex) {
                            ex.printStackTrace();
                            return -1;
                        } catch (IllegalAccessException e2) {
                            e = e2;
                            e.printStackTrace();
                            return -1;
                        } catch (NoSuchFieldException e3) {
                            e = e3;
                            e.printStackTrace();
                            return -1;
                        }
                    }
                    if (this.currentPkg == null || !this.currentPkg.equals(pkgName)) {
                        this.currentPkg = pkgName;
                        Log.d("EventRelay", "onForegroundActivitiesChanged: " + this.currentPkg);
                        PowerHintManager phm = new PowerHintManager();
                        phm.sendPowerHint("foreground_task", this.currentPkg);
                        return i;
                    }
                    SystemClock.sleep(500L);
                    i++;
                }
                return i;
            }

            @Override
            public void run() {
                int count = getTopActivityAndSendPowerHint();
                if (count == 0) {
                    Log.d("EventRelay", "Check if there's a missing event");
                    getTopActivityAndSendPowerHint();
                }
            }
        };

        public void onForegroundActivitiesChanged(int pid, int uid, boolean foregroundActivities) {
            if (foregroundActivities) {
                new Thread(this.mRunnable).start();
            }
        }

        public void onProcessStateChanged(int pid, int uid, int procState) {
        }

        public void onProcessDied(int pid, int uid) {
        }
    };
    private BroadcastReceiver mSsgIntentReceiver;
    private BroadcastReceiver mSystemIntentReceiver;

    @Override
    public void onCreate() {
        this.mSystemIntentReceiver = new SystemIntentReceiver();
        IntentFilter filter1 = new IntentFilter();
        filter1.addAction("android.intent.action.SCREEN_OFF");
        filter1.addAction("android.intent.action.SCREEN_ON");
        filter1.addAction("android.intent.action.PHONE_STATE");
        filter1.addAction("com.marvell.fmradio.ENABLE");
        filter1.addAction("com.marvell.fmradio.DISABLE");
        registerReceiver(this.mSystemIntentReceiver, filter1);
        Log.d("EventRelay", "Register SystemIntentReceiver");
        this.mSsgIntentReceiver = new SsgIntentReceiver();
        IntentFilter filter2 = new IntentFilter();
        filter2.addAction("com.sec.android.intent.action.CPU_BOOSTER_MIN");
        filter2.addAction("com.sec.android.intent.action.CPU_BOOSTER_MAX");
        filter2.addAction("com.sec.android.intent.action.GPU_BOOSTER_MIN");
        filter2.addAction("com.sec.android.intent.action.GPU_BOOSTER_MAX");
        filter2.addAction("com.sec.android.intent.action.DDR_BOOSTER_MIN");
        filter2.addAction("com.sec.android.intent.action.DDR_BOOSTER_MAX");
        filter2.addAction("com.sec.android.intent.action.CPU_BOOSTER_CORE_NUM");
        registerReceiver(this.mSsgIntentReceiver, filter2);
        Log.d("EventRelay", "Register SsgIntentReceiver");
        this.mActivityManager = ActivityManagerNative.getDefault();
        if (this.mActivityManager != null) {
            try {
                this.mActivityManager.registerProcessObserver(this.mProcessObserver);
            } catch (RemoteException e) {
                Log.e("EventRelay", "Register mProcessObserver failed!");
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return 1;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        Log.d("EventRelay", "onDestroy: Maybe something happened! ");
        unregisterReceiver(this.mSystemIntentReceiver);
        unregisterReceiver(this.mSsgIntentReceiver);
        if (this.mActivityManager != null) {
            try {
                this.mActivityManager.unregisterProcessObserver(this.mProcessObserver);
            } catch (RemoteException e) {
                Log.e("EventRelay", "Un-register mProcessObserver failed!");
            }
        }
    }
}
