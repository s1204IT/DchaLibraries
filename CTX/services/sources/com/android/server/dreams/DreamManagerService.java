package com.android.server.dreams;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.database.ContentObserver;
import android.hardware.input.InputManagerInternal;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.PowerManagerInternal;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.dreams.DreamManagerInternal;
import android.service.dreams.IDreamManager;
import android.util.Slog;
import android.view.Display;
import com.android.internal.hardware.AmbientDisplayConfiguration;
import com.android.internal.util.DumpUtils;
import com.android.server.FgThread;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.dreams.DreamController;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Objects;

public final class DreamManagerService extends SystemService {
    private static final boolean DEBUG = false;
    private static final String TAG = "DreamManagerService";
    private final Context mContext;
    private final DreamController mController;
    private final DreamController.Listener mControllerListener;
    private boolean mCurrentDreamCanDoze;
    private int mCurrentDreamDozeScreenBrightness;
    private int mCurrentDreamDozeScreenState;
    private boolean mCurrentDreamIsDozing;
    private boolean mCurrentDreamIsTest;
    private boolean mCurrentDreamIsWaking;
    private ComponentName mCurrentDreamName;
    private Binder mCurrentDreamToken;
    private int mCurrentDreamUserId;
    private AmbientDisplayConfiguration mDozeConfig;
    private final ContentObserver mDozeEnabledObserver;
    private final PowerManager.WakeLock mDozeWakeLock;
    private final DreamHandler mHandler;
    private final Object mLock;
    private final PowerManager mPowerManager;
    private final PowerManagerInternal mPowerManagerInternal;
    private final Runnable mSystemPropertiesChanged;

    public DreamManagerService(Context context) {
        super(context);
        this.mLock = new Object();
        this.mCurrentDreamDozeScreenState = 0;
        this.mCurrentDreamDozeScreenBrightness = -1;
        this.mControllerListener = new DreamController.Listener() {
            @Override
            public void onDreamStopped(Binder binder) {
                synchronized (DreamManagerService.this.mLock) {
                    if (DreamManagerService.this.mCurrentDreamToken == binder) {
                        DreamManagerService.this.cleanupDreamLocked();
                    }
                }
            }
        };
        this.mDozeEnabledObserver = new ContentObserver(null) {
            @Override
            public void onChange(boolean z) {
                DreamManagerService.this.writePulseGestureEnabled();
            }
        };
        this.mSystemPropertiesChanged = new Runnable() {
            @Override
            public void run() {
                synchronized (DreamManagerService.this.mLock) {
                    if (DreamManagerService.this.mCurrentDreamName != null && DreamManagerService.this.mCurrentDreamCanDoze && !DreamManagerService.this.mCurrentDreamName.equals(DreamManagerService.this.getDozeComponent())) {
                        DreamManagerService.this.mPowerManager.wakeUp(SystemClock.uptimeMillis(), "android.server.dreams:SYSPROP");
                    }
                }
            }
        };
        this.mContext = context;
        this.mHandler = new DreamHandler(FgThread.get().getLooper());
        this.mController = new DreamController(context, this.mHandler, this.mControllerListener);
        this.mPowerManager = (PowerManager) context.getSystemService("power");
        this.mPowerManagerInternal = (PowerManagerInternal) getLocalService(PowerManagerInternal.class);
        this.mDozeWakeLock = this.mPowerManager.newWakeLock(64, TAG);
        this.mDozeConfig = new AmbientDisplayConfiguration(this.mContext);
    }

    @Override
    public void onStart() {
        publishBinderService("dreams", new BinderService());
        publishLocalService(DreamManagerInternal.class, new LocalService());
    }

    @Override
    public void onBootPhase(int i) {
        if (i == 600) {
            if (Build.IS_DEBUGGABLE) {
                SystemProperties.addChangeCallback(this.mSystemPropertiesChanged);
            }
            this.mContext.registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    DreamManagerService.this.writePulseGestureEnabled();
                    synchronized (DreamManagerService.this.mLock) {
                        DreamManagerService.this.stopDreamLocked(false);
                    }
                }
            }, new IntentFilter("android.intent.action.USER_SWITCHED"), null, this.mHandler);
            this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("doze_pulse_on_double_tap"), false, this.mDozeEnabledObserver, -1);
            writePulseGestureEnabled();
        }
    }

    private void dumpInternal(PrintWriter printWriter) {
        printWriter.println("DREAM MANAGER (dumpsys dreams)");
        printWriter.println();
        printWriter.println("mCurrentDreamToken=" + this.mCurrentDreamToken);
        printWriter.println("mCurrentDreamName=" + this.mCurrentDreamName);
        printWriter.println("mCurrentDreamUserId=" + this.mCurrentDreamUserId);
        printWriter.println("mCurrentDreamIsTest=" + this.mCurrentDreamIsTest);
        printWriter.println("mCurrentDreamCanDoze=" + this.mCurrentDreamCanDoze);
        printWriter.println("mCurrentDreamIsDozing=" + this.mCurrentDreamIsDozing);
        printWriter.println("mCurrentDreamIsWaking=" + this.mCurrentDreamIsWaking);
        printWriter.println("mCurrentDreamDozeScreenState=" + Display.stateToString(this.mCurrentDreamDozeScreenState));
        printWriter.println("mCurrentDreamDozeScreenBrightness=" + this.mCurrentDreamDozeScreenBrightness);
        printWriter.println("getDozeComponent()=" + getDozeComponent());
        printWriter.println();
        DumpUtils.dumpAsync(this.mHandler, new DumpUtils.Dump() {
            public void dump(PrintWriter printWriter2, String str) {
                DreamManagerService.this.mController.dump(printWriter2);
            }
        }, printWriter, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, 200L);
    }

    private boolean isDreamingInternal() {
        boolean z;
        synchronized (this.mLock) {
            z = (this.mCurrentDreamToken == null || this.mCurrentDreamIsTest || this.mCurrentDreamIsWaking) ? false : true;
        }
        return z;
    }

    private void requestDreamInternal() {
        long jUptimeMillis = SystemClock.uptimeMillis();
        this.mPowerManager.userActivity(jUptimeMillis, true);
        this.mPowerManager.nap(jUptimeMillis);
    }

    private void requestAwakenInternal() {
        this.mPowerManager.userActivity(SystemClock.uptimeMillis(), false);
        stopDreamInternal(false);
    }

    private void finishSelfInternal(IBinder iBinder, boolean z) {
        synchronized (this.mLock) {
            if (this.mCurrentDreamToken == iBinder) {
                stopDreamLocked(z);
            }
        }
    }

    private void testDreamInternal(ComponentName componentName, int i) {
        synchronized (this.mLock) {
            startDreamLocked(componentName, true, false, i);
        }
    }

    private void startDreamInternal(boolean z) {
        int currentUser = ActivityManager.getCurrentUser();
        ComponentName componentNameChooseDreamForUser = chooseDreamForUser(z, currentUser);
        if (componentNameChooseDreamForUser != null) {
            synchronized (this.mLock) {
                startDreamLocked(componentNameChooseDreamForUser, false, z, currentUser);
            }
        }
    }

    private void stopDreamInternal(boolean z) {
        synchronized (this.mLock) {
            stopDreamLocked(z);
        }
    }

    private void startDozingInternal(IBinder iBinder, int i, int i2) {
        synchronized (this.mLock) {
            if (this.mCurrentDreamToken == iBinder && this.mCurrentDreamCanDoze) {
                this.mCurrentDreamDozeScreenState = i;
                this.mCurrentDreamDozeScreenBrightness = i2;
                this.mPowerManagerInternal.setDozeOverrideFromDreamManager(i, i2);
                if (!this.mCurrentDreamIsDozing) {
                    this.mCurrentDreamIsDozing = true;
                    this.mDozeWakeLock.acquire();
                }
            }
        }
    }

    private void stopDozingInternal(IBinder iBinder) {
        synchronized (this.mLock) {
            if (this.mCurrentDreamToken == iBinder && this.mCurrentDreamIsDozing) {
                this.mCurrentDreamIsDozing = false;
                this.mDozeWakeLock.release();
                this.mPowerManagerInternal.setDozeOverrideFromDreamManager(0, -1);
            }
        }
    }

    private ComponentName chooseDreamForUser(boolean z, int i) {
        if (z) {
            ComponentName dozeComponent = getDozeComponent(i);
            if (validateDream(dozeComponent)) {
                return dozeComponent;
            }
            return null;
        }
        ComponentName[] dreamComponentsForUser = getDreamComponentsForUser(i);
        if (dreamComponentsForUser == null || dreamComponentsForUser.length == 0) {
            return null;
        }
        return dreamComponentsForUser[0];
    }

    private boolean validateDream(ComponentName componentName) {
        if (componentName == null) {
            return false;
        }
        ServiceInfo serviceInfo = getServiceInfo(componentName);
        if (serviceInfo == null) {
            Slog.w(TAG, "Dream " + componentName + " does not exist");
            return false;
        }
        if (serviceInfo.applicationInfo.targetSdkVersion >= 21 && !"android.permission.BIND_DREAM_SERVICE".equals(serviceInfo.permission)) {
            Slog.w(TAG, "Dream " + componentName + " is not available because its manifest is missing the android.permission.BIND_DREAM_SERVICE permission on the dream service declaration.");
            return false;
        }
        return true;
    }

    private ComponentName[] getDreamComponentsForUser(int i) {
        ComponentName defaultDreamComponentForUser;
        ComponentName[] componentNameArrComponentsFromString = componentsFromString(Settings.Secure.getStringForUser(this.mContext.getContentResolver(), "screensaver_components", i));
        ArrayList arrayList = new ArrayList();
        if (componentNameArrComponentsFromString != null) {
            for (ComponentName componentName : componentNameArrComponentsFromString) {
                if (validateDream(componentName)) {
                    arrayList.add(componentName);
                }
            }
        }
        if (arrayList.isEmpty() && (defaultDreamComponentForUser = getDefaultDreamComponentForUser(i)) != null) {
            Slog.w(TAG, "Falling back to default dream " + defaultDreamComponentForUser);
            arrayList.add(defaultDreamComponentForUser);
        }
        return (ComponentName[]) arrayList.toArray(new ComponentName[arrayList.size()]);
    }

    private void setDreamComponentsForUser(int i, ComponentName[] componentNameArr) {
        Settings.Secure.putStringForUser(this.mContext.getContentResolver(), "screensaver_components", componentsToString(componentNameArr), i);
    }

    private ComponentName getDefaultDreamComponentForUser(int i) {
        String stringForUser = Settings.Secure.getStringForUser(this.mContext.getContentResolver(), "screensaver_default_component", i);
        if (stringForUser == null) {
            return null;
        }
        return ComponentName.unflattenFromString(stringForUser);
    }

    private ComponentName getDozeComponent() {
        return getDozeComponent(ActivityManager.getCurrentUser());
    }

    private ComponentName getDozeComponent(int i) {
        if (this.mDozeConfig.enabled(i)) {
            return ComponentName.unflattenFromString(this.mDozeConfig.ambientDisplayComponent());
        }
        return null;
    }

    private ServiceInfo getServiceInfo(ComponentName componentName) {
        if (componentName != null) {
            try {
                return this.mContext.getPackageManager().getServiceInfo(componentName, 268435456);
            } catch (PackageManager.NameNotFoundException e) {
                return null;
            }
        }
        return null;
    }

    private void startDreamLocked(final ComponentName componentName, final boolean z, final boolean z2, final int i) {
        if (Objects.equals(this.mCurrentDreamName, componentName) && this.mCurrentDreamIsTest == z && this.mCurrentDreamCanDoze == z2 && this.mCurrentDreamUserId == i) {
            Slog.i(TAG, "Already in target dream.");
            return;
        }
        stopDreamLocked(true);
        Slog.i(TAG, "Entering dreamland.");
        final Binder binder = new Binder();
        this.mCurrentDreamToken = binder;
        this.mCurrentDreamName = componentName;
        this.mCurrentDreamIsTest = z;
        this.mCurrentDreamCanDoze = z2;
        this.mCurrentDreamUserId = i;
        final PowerManager.WakeLock wakeLockNewWakeLock = this.mPowerManager.newWakeLock(1, "startDream");
        this.mHandler.post(wakeLockNewWakeLock.wrap(new Runnable() {
            @Override
            public final void run() {
                this.f$0.mController.startDream(binder, componentName, z, z2, i, wakeLockNewWakeLock);
            }
        }));
    }

    private void stopDreamLocked(final boolean z) {
        if (this.mCurrentDreamToken != null) {
            if (z) {
                Slog.i(TAG, "Leaving dreamland.");
                cleanupDreamLocked();
            } else {
                if (this.mCurrentDreamIsWaking) {
                    return;
                }
                Slog.i(TAG, "Gently waking up from dream.");
                this.mCurrentDreamIsWaking = true;
            }
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Slog.i(DreamManagerService.TAG, "Performing gentle wake from dream.");
                    DreamManagerService.this.mController.stopDream(z);
                }
            });
        }
    }

    private void cleanupDreamLocked() {
        this.mCurrentDreamToken = null;
        this.mCurrentDreamName = null;
        this.mCurrentDreamIsTest = false;
        this.mCurrentDreamCanDoze = false;
        this.mCurrentDreamUserId = 0;
        this.mCurrentDreamIsWaking = false;
        if (this.mCurrentDreamIsDozing) {
            this.mCurrentDreamIsDozing = false;
            this.mDozeWakeLock.release();
        }
        this.mCurrentDreamDozeScreenState = 0;
        this.mCurrentDreamDozeScreenBrightness = -1;
    }

    private void checkPermission(String str) {
        if (this.mContext.checkCallingOrSelfPermission(str) != 0) {
            throw new SecurityException("Access denied to process: " + Binder.getCallingPid() + ", must have permission " + str);
        }
    }

    private void writePulseGestureEnabled() {
        ((InputManagerInternal) LocalServices.getService(InputManagerInternal.class)).setPulseGestureEnabled(validateDream(getDozeComponent()));
    }

    private static String componentsToString(ComponentName[] componentNameArr) {
        StringBuilder sb = new StringBuilder();
        if (componentNameArr != null) {
            for (ComponentName componentName : componentNameArr) {
                if (sb.length() > 0) {
                    sb.append(',');
                }
                sb.append(componentName.flattenToString());
            }
        }
        return sb.toString();
    }

    private static ComponentName[] componentsFromString(String str) {
        if (str == null) {
            return null;
        }
        String[] strArrSplit = str.split(",");
        ComponentName[] componentNameArr = new ComponentName[strArrSplit.length];
        for (int i = 0; i < strArrSplit.length; i++) {
            componentNameArr[i] = ComponentName.unflattenFromString(strArrSplit[i]);
        }
        return componentNameArr;
    }

    private final class DreamHandler extends Handler {
        public DreamHandler(Looper looper) {
            super(looper, null, true);
        }
    }

    private final class BinderService extends IDreamManager.Stub {
        private BinderService() {
        }

        protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
            if (DumpUtils.checkDumpPermission(DreamManagerService.this.mContext, DreamManagerService.TAG, printWriter)) {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    DreamManagerService.this.dumpInternal(printWriter);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        }

        public ComponentName[] getDreamComponents() {
            DreamManagerService.this.checkPermission("android.permission.READ_DREAM_STATE");
            int callingUserId = UserHandle.getCallingUserId();
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                return DreamManagerService.this.getDreamComponentsForUser(callingUserId);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void setDreamComponents(ComponentName[] componentNameArr) {
            DreamManagerService.this.checkPermission("android.permission.WRITE_DREAM_STATE");
            int callingUserId = UserHandle.getCallingUserId();
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                DreamManagerService.this.setDreamComponentsForUser(callingUserId, componentNameArr);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public ComponentName getDefaultDreamComponent() {
            DreamManagerService.this.checkPermission("android.permission.READ_DREAM_STATE");
            int callingUserId = UserHandle.getCallingUserId();
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                return DreamManagerService.this.getDefaultDreamComponentForUser(callingUserId);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public boolean isDreaming() {
            DreamManagerService.this.checkPermission("android.permission.READ_DREAM_STATE");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                return DreamManagerService.this.isDreamingInternal();
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void dream() {
            DreamManagerService.this.checkPermission("android.permission.WRITE_DREAM_STATE");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                DreamManagerService.this.requestDreamInternal();
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void testDream(ComponentName componentName) {
            if (componentName != null) {
                DreamManagerService.this.checkPermission("android.permission.WRITE_DREAM_STATE");
                int callingUserId = UserHandle.getCallingUserId();
                int currentUser = ActivityManager.getCurrentUser();
                if (callingUserId != currentUser) {
                    Slog.w(DreamManagerService.TAG, "Aborted attempt to start a test dream while a different  user is active: callingUserId=" + callingUserId + ", currentUserId=" + currentUser);
                    return;
                }
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    DreamManagerService.this.testDreamInternal(componentName, callingUserId);
                    return;
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
            throw new IllegalArgumentException("dream must not be null");
        }

        public void awaken() {
            DreamManagerService.this.checkPermission("android.permission.WRITE_DREAM_STATE");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                DreamManagerService.this.requestAwakenInternal();
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void finishSelf(IBinder iBinder, boolean z) {
            if (iBinder == null) {
                throw new IllegalArgumentException("token must not be null");
            }
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                DreamManagerService.this.finishSelfInternal(iBinder, z);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void startDozing(IBinder iBinder, int i, int i2) {
            if (iBinder == null) {
                throw new IllegalArgumentException("token must not be null");
            }
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                DreamManagerService.this.startDozingInternal(iBinder, i, i2);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void stopDozing(IBinder iBinder) {
            if (iBinder == null) {
                throw new IllegalArgumentException("token must not be null");
            }
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                DreamManagerService.this.stopDozingInternal(iBinder);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
    }

    private final class LocalService extends DreamManagerInternal {
        private LocalService() {
        }

        public void startDream(boolean z) {
            DreamManagerService.this.startDreamInternal(z);
        }

        public void stopDream(boolean z) {
            DreamManagerService.this.stopDreamInternal(z);
        }

        public boolean isDreaming() {
            return DreamManagerService.this.isDreamingInternal();
        }
    }
}
