package com.android.server;

import android.R;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.UEventObserver;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import android.util.Slog;
import com.android.internal.util.DumpUtils;
import com.android.server.audio.AudioService;
import com.android.server.backup.BackupManagerConstants;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;

final class DockObserver extends SystemService {
    private static final String DOCK_STATE_PATH = "/sys/class/switch/dock/state";
    private static final String DOCK_UEVENT_MATCH = "DEVPATH=/devices/virtual/switch/dock";
    private static final int MSG_DOCK_STATE_CHANGED = 0;
    private static final String TAG = "DockObserver";
    private int mActualDockState;
    private final boolean mAllowTheaterModeWakeFromDock;
    private final Handler mHandler;
    private final Object mLock;
    private final UEventObserver mObserver;
    private final PowerManager mPowerManager;
    private int mPreviousDockState;
    private int mReportedDockState;
    private boolean mSystemReady;
    private boolean mUpdatesStopped;
    private final PowerManager.WakeLock mWakeLock;

    public DockObserver(Context context) {
        super(context);
        this.mLock = new Object();
        this.mActualDockState = 0;
        this.mReportedDockState = 0;
        this.mPreviousDockState = 0;
        this.mHandler = new Handler(true) {
            @Override
            public void handleMessage(Message message) {
                if (message.what == 0) {
                    DockObserver.this.handleDockStateChange();
                    DockObserver.this.mWakeLock.release();
                }
            }
        };
        this.mObserver = new UEventObserver() {
            public void onUEvent(UEventObserver.UEvent uEvent) {
                if (Log.isLoggable(DockObserver.TAG, 2)) {
                    Slog.v(DockObserver.TAG, "Dock UEVENT: " + uEvent.toString());
                }
                try {
                    synchronized (DockObserver.this.mLock) {
                        DockObserver.this.setActualDockStateLocked(Integer.parseInt(uEvent.get("SWITCH_STATE")));
                    }
                } catch (NumberFormatException e) {
                    Slog.e(DockObserver.TAG, "Could not parse switch state from event " + uEvent);
                }
            }
        };
        this.mPowerManager = (PowerManager) context.getSystemService("power");
        this.mWakeLock = this.mPowerManager.newWakeLock(1, TAG);
        this.mAllowTheaterModeWakeFromDock = context.getResources().getBoolean(R.^attr-private.alertDialogButtonGroupStyle);
        init();
        this.mObserver.startObserving(DOCK_UEVENT_MATCH);
    }

    @Override
    public void onStart() {
        publishBinderService(TAG, new BinderService());
    }

    @Override
    public void onBootPhase(int i) {
        if (i == 550) {
            synchronized (this.mLock) {
                this.mSystemReady = true;
                if (this.mReportedDockState != 0) {
                    updateLocked();
                }
            }
        }
    }

    private void init() {
        synchronized (this.mLock) {
            try {
                try {
                    char[] cArr = new char[1024];
                    FileReader fileReader = new FileReader(DOCK_STATE_PATH);
                    try {
                        setActualDockStateLocked(Integer.parseInt(new String(cArr, 0, fileReader.read(cArr, 0, 1024)).trim()));
                        this.mPreviousDockState = this.mActualDockState;
                        fileReader.close();
                    } catch (Throwable th) {
                        fileReader.close();
                        throw th;
                    }
                } catch (Exception e) {
                    Slog.e(TAG, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, e);
                }
            } catch (FileNotFoundException e2) {
                Slog.w(TAG, "This kernel does not have dock station support");
            }
        }
    }

    private void setActualDockStateLocked(int i) {
        this.mActualDockState = i;
        if (!this.mUpdatesStopped) {
            setDockStateLocked(i);
        }
    }

    private void setDockStateLocked(int i) {
        if (i != this.mReportedDockState) {
            this.mReportedDockState = i;
            if (this.mSystemReady) {
                if (this.mAllowTheaterModeWakeFromDock || Settings.Global.getInt(getContext().getContentResolver(), "theater_mode_on", 0) == 0) {
                    this.mPowerManager.wakeUp(SystemClock.uptimeMillis(), "android.server:DOCK");
                }
                updateLocked();
            }
        }
    }

    private void updateLocked() {
        this.mWakeLock.acquire();
        this.mHandler.sendEmptyMessage(0);
    }

    private void handleDockStateChange() {
        String string;
        Ringtone ringtone;
        synchronized (this.mLock) {
            Slog.i(TAG, "Dock state changed from " + this.mPreviousDockState + " to " + this.mReportedDockState);
            int i = this.mPreviousDockState;
            this.mPreviousDockState = this.mReportedDockState;
            ContentResolver contentResolver = getContext().getContentResolver();
            if (Settings.Global.getInt(contentResolver, "device_provisioned", 0) == 0) {
                Slog.i(TAG, "Device not provisioned, skipping dock broadcast");
                return;
            }
            Intent intent = new Intent("android.intent.action.DOCK_EVENT");
            intent.addFlags(536870912);
            intent.putExtra("android.intent.extra.DOCK_STATE", this.mReportedDockState);
            boolean z = Settings.Global.getInt(contentResolver, "dock_sounds_enabled", 1) == 1;
            boolean z2 = Settings.Global.getInt(contentResolver, "dock_sounds_enabled_when_accessbility", 1) == 1;
            boolean z3 = Settings.Secure.getInt(contentResolver, "accessibility_enabled", 0) == 1;
            if (z || (z3 && z2)) {
                String str = null;
                if (this.mReportedDockState == 0) {
                    if (i == 1 || i == 3 || i == 4) {
                        str = "desk_undock_sound";
                    } else if (i == 2) {
                        str = "car_undock_sound";
                    }
                } else if (this.mReportedDockState == 1 || this.mReportedDockState == 3 || this.mReportedDockState == 4) {
                    str = "desk_dock_sound";
                } else if (this.mReportedDockState == 2) {
                    str = "car_dock_sound";
                }
                if (str != null && (string = Settings.Global.getString(contentResolver, str)) != null) {
                    Uri uri = Uri.parse("file://" + string);
                    if (uri != null && (ringtone = RingtoneManager.getRingtone(getContext(), uri)) != null) {
                        ringtone.setStreamType(1);
                        ringtone.play();
                    }
                }
            }
            getContext().sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        }
    }

    private final class BinderService extends Binder {
        private BinderService() {
        }

        @Override
        protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
            if (DumpUtils.checkDumpPermission(DockObserver.this.getContext(), DockObserver.TAG, printWriter)) {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    synchronized (DockObserver.this.mLock) {
                        if (strArr != null) {
                            try {
                                if (strArr.length == 0 || "-a".equals(strArr[0])) {
                                    printWriter.println("Current Dock Observer Service state:");
                                    if (DockObserver.this.mUpdatesStopped) {
                                        printWriter.println("  (UPDATES STOPPED -- use 'reset' to restart)");
                                    }
                                    printWriter.println("  reported state: " + DockObserver.this.mReportedDockState);
                                    printWriter.println("  previous state: " + DockObserver.this.mPreviousDockState);
                                    printWriter.println("  actual state: " + DockObserver.this.mActualDockState);
                                } else if (strArr.length == 3 && "set".equals(strArr[0])) {
                                    String str = strArr[1];
                                    String str2 = strArr[2];
                                    try {
                                        if (AudioService.CONNECT_INTENT_KEY_STATE.equals(str)) {
                                            DockObserver.this.mUpdatesStopped = true;
                                            DockObserver.this.setDockStateLocked(Integer.parseInt(str2));
                                        } else {
                                            printWriter.println("Unknown set option: " + str);
                                        }
                                    } catch (NumberFormatException e) {
                                        printWriter.println("Bad value: " + str2);
                                    }
                                } else if (strArr.length == 1 && "reset".equals(strArr[0])) {
                                    DockObserver.this.mUpdatesStopped = false;
                                    DockObserver.this.setDockStateLocked(DockObserver.this.mActualDockState);
                                } else {
                                    printWriter.println("Dump current dock state, or:");
                                    printWriter.println("  set state <value>");
                                    printWriter.println("  reset");
                                }
                            } finally {
                            }
                        }
                    }
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        }
    }
}
