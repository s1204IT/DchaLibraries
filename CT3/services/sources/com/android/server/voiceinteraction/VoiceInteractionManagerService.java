package com.android.server.voiceinteraction;

import android.R;
import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.AppGlobals;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.hardware.soundtrigger.IRecognitionStatusCallback;
import android.hardware.soundtrigger.SoundTrigger;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.voice.IVoiceInteractionService;
import android.service.voice.IVoiceInteractionSession;
import android.service.voice.VoiceInteractionManagerInternal;
import android.service.voice.VoiceInteractionServiceInfo;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import com.android.internal.app.IVoiceInteractionManagerService;
import com.android.internal.app.IVoiceInteractionSessionShowCallback;
import com.android.internal.app.IVoiceInteractor;
import com.android.internal.content.PackageMonitor;
import com.android.internal.os.BackgroundThread;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.UiThread;
import com.android.server.soundtrigger.SoundTriggerInternal;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

public class VoiceInteractionManagerService extends SystemService {
    static final boolean DEBUG = true;
    static final String TAG = "VoiceInteractionManagerService";
    final ActivityManagerInternal mAmInternal;
    final Context mContext;
    final DatabaseHelper mDbHelper;
    final TreeSet<Integer> mLoadedKeyphraseIds;
    final ContentResolver mResolver;
    private final VoiceInteractionManagerServiceStub mServiceStub;
    SoundTriggerInternal mSoundTriggerInternal;

    public VoiceInteractionManagerService(Context context) {
        super(context);
        this.mContext = context;
        this.mResolver = context.getContentResolver();
        this.mDbHelper = new DatabaseHelper(context);
        this.mServiceStub = new VoiceInteractionManagerServiceStub();
        this.mAmInternal = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
        this.mLoadedKeyphraseIds = new TreeSet<>();
        PackageManagerInternal packageManagerInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        packageManagerInternal.setVoiceInteractionPackagesProvider(new PackageManagerInternal.PackagesProvider() {
            public String[] getPackages(int userId) {
                VoiceInteractionManagerService.this.mServiceStub.initForUser(userId);
                ComponentName interactor = VoiceInteractionManagerService.this.mServiceStub.getCurInteractor(userId);
                if (interactor != null) {
                    return new String[]{interactor.getPackageName()};
                }
                return null;
            }
        });
    }

    @Override
    public void onStart() {
        publishBinderService("voiceinteraction", this.mServiceStub);
        publishLocalService(VoiceInteractionManagerInternal.class, new LocalService());
    }

    @Override
    public void onBootPhase(int phase) {
        if (500 == phase) {
            this.mSoundTriggerInternal = (SoundTriggerInternal) LocalServices.getService(SoundTriggerInternal.class);
        } else {
            if (phase != 600) {
                return;
            }
            this.mServiceStub.systemRunning(isSafeMode());
        }
    }

    @Override
    public void onStartUser(int userHandle) {
        this.mServiceStub.initForUser(userHandle);
    }

    @Override
    public void onUnlockUser(int userHandle) {
        this.mServiceStub.initForUser(userHandle);
        this.mServiceStub.switchImplementationIfNeeded(false);
    }

    @Override
    public void onSwitchUser(int userHandle) {
        this.mServiceStub.switchUser(userHandle);
    }

    class LocalService extends VoiceInteractionManagerInternal {
        LocalService() {
        }

        public void startLocalVoiceInteraction(IBinder callingActivity, Bundle options) {
            Slog.i(VoiceInteractionManagerService.TAG, "startLocalVoiceInteraction " + callingActivity);
            VoiceInteractionManagerService.this.mServiceStub.startLocalVoiceInteraction(callingActivity, options);
        }

        public boolean supportsLocalVoiceInteraction() {
            return VoiceInteractionManagerService.this.mServiceStub.supportsLocalVoiceInteraction();
        }

        public void stopLocalVoiceInteraction(IBinder callingActivity) {
            Slog.i(VoiceInteractionManagerService.TAG, "stopLocalVoiceInteraction " + callingActivity);
            VoiceInteractionManagerService.this.mServiceStub.stopLocalVoiceInteraction(callingActivity);
        }
    }

    class VoiceInteractionManagerServiceStub extends IVoiceInteractionManagerService.Stub {
        private int mCurUser;
        private final boolean mEnableService;
        VoiceInteractionManagerServiceImpl mImpl;
        PackageMonitor mPackageMonitor = new PackageMonitor() {
            public boolean onHandleForceStop(Intent intent, String[] packages, int uid, boolean doit) {
                Slog.d(VoiceInteractionManagerService.TAG, "onHandleForceStop uid=" + uid + " doit=" + doit);
                int userHandle = UserHandle.getUserId(uid);
                ComponentName curInteractor = VoiceInteractionManagerServiceStub.this.getCurInteractor(userHandle);
                ComponentName curRecognizer = VoiceInteractionManagerServiceStub.this.getCurRecognizer(userHandle);
                boolean hit = false;
                boolean changed = false;
                int i = 0;
                int length = packages.length;
                while (true) {
                    if (i >= length) {
                        break;
                    }
                    String pkg = packages[i];
                    if (curInteractor != null && pkg.equals(curInteractor.getPackageName())) {
                        int change = isPackageDisappearing(pkg);
                        Slog.d(VoiceInteractionManagerService.TAG, "Interactor. change =" + change + " pkg =" + pkg);
                        if (change == 3) {
                            changed = true;
                        }
                        hit = true;
                    } else if (curRecognizer == null || !pkg.equals(curRecognizer.getPackageName())) {
                        i++;
                    } else {
                        int change2 = isPackageDisappearing(pkg);
                        Slog.d(VoiceInteractionManagerService.TAG, "Recognizer. change =" + change2 + " pkg =" + pkg);
                        if (change2 == 3) {
                            changed = true;
                        }
                        hit = true;
                    }
                }
                if (hit && doit && changed) {
                    synchronized (VoiceInteractionManagerServiceStub.this) {
                        VoiceInteractionManagerServiceStub.this.unloadAllKeyphraseModels();
                        if (VoiceInteractionManagerServiceStub.this.mImpl != null) {
                            VoiceInteractionManagerServiceStub.this.mImpl.shutdownLocked();
                            VoiceInteractionManagerServiceStub.this.mImpl = null;
                        }
                        VoiceInteractionManagerServiceStub.this.setCurInteractor(null, userHandle);
                        VoiceInteractionManagerServiceStub.this.setCurRecognizer(null, userHandle);
                        VoiceInteractionManagerServiceStub.this.resetCurAssistant(userHandle);
                        VoiceInteractionManagerServiceStub.this.initForUser(userHandle);
                        VoiceInteractionManagerServiceStub.this.switchImplementationIfNeededLocked(true);
                    }
                }
                return hit;
            }

            public void onHandleUserStop(Intent intent, int userHandle) {
            }

            public void onSomePackagesChanged() {
                ComponentName curRecognizer;
                int userHandle = getChangingUserId();
                Slog.d(VoiceInteractionManagerService.TAG, "onSomePackagesChanged user=" + userHandle);
                synchronized (VoiceInteractionManagerServiceStub.this) {
                    ComponentName curInteractor = VoiceInteractionManagerServiceStub.this.getCurInteractor(userHandle);
                    ComponentName curRecognizer2 = VoiceInteractionManagerServiceStub.this.getCurRecognizer(userHandle);
                    if (curRecognizer2 == null) {
                        if (anyPackagesAppearing() && (curRecognizer = VoiceInteractionManagerServiceStub.this.findAvailRecognizer(null, userHandle)) != null) {
                            VoiceInteractionManagerServiceStub.this.setCurRecognizer(curRecognizer, userHandle);
                        }
                        return;
                    }
                    if (curInteractor != null) {
                        if (isPackageDisappearing(curInteractor.getPackageName()) == 3) {
                            VoiceInteractionManagerServiceStub.this.setCurInteractor(null, userHandle);
                            VoiceInteractionManagerServiceStub.this.setCurRecognizer(null, userHandle);
                            VoiceInteractionManagerServiceStub.this.initForUser(userHandle);
                            return;
                        } else {
                            if (isPackageAppearing(curInteractor.getPackageName()) != 0 && VoiceInteractionManagerServiceStub.this.mImpl != null && curInteractor.getPackageName().equals(VoiceInteractionManagerServiceStub.this.mImpl.mComponent.getPackageName())) {
                                VoiceInteractionManagerServiceStub.this.switchImplementationIfNeededLocked(true);
                            }
                            return;
                        }
                    }
                    int change = isPackageDisappearing(curRecognizer2.getPackageName());
                    if (change == 3 || change == 2) {
                        VoiceInteractionManagerServiceStub.this.setCurRecognizer(VoiceInteractionManagerServiceStub.this.findAvailRecognizer(null, userHandle), userHandle);
                    } else if (isPackageModified(curRecognizer2.getPackageName())) {
                        VoiceInteractionManagerServiceStub.this.setCurRecognizer(VoiceInteractionManagerServiceStub.this.findAvailRecognizer(curRecognizer2.getPackageName(), userHandle), userHandle);
                    }
                }
            }
        };
        private boolean mSafeMode;

        VoiceInteractionManagerServiceStub() {
            this.mEnableService = shouldEnableService(VoiceInteractionManagerService.this.mContext.getResources());
        }

        void startLocalVoiceInteraction(final IBinder token, Bundle options) {
            if (this.mImpl == null) {
                return;
            }
            long caller = Binder.clearCallingIdentity();
            try {
                this.mImpl.showSessionLocked(options, 16, new IVoiceInteractionSessionShowCallback.Stub() {
                    public void onFailed() {
                    }

                    public void onShown() {
                        VoiceInteractionManagerService.this.mAmInternal.onLocalVoiceInteractionStarted(token, VoiceInteractionManagerServiceStub.this.mImpl.mActiveSession.mSession, VoiceInteractionManagerServiceStub.this.mImpl.mActiveSession.mInteractor);
                    }
                }, token);
            } finally {
                Binder.restoreCallingIdentity(caller);
            }
        }

        public void stopLocalVoiceInteraction(IBinder callingActivity) {
            if (this.mImpl == null) {
                return;
            }
            long caller = Binder.clearCallingIdentity();
            try {
                this.mImpl.finishLocked(callingActivity, true);
            } finally {
                Binder.restoreCallingIdentity(caller);
            }
        }

        public boolean supportsLocalVoiceInteraction() {
            if (this.mImpl == null) {
                return false;
            }
            return this.mImpl.supportsLocalVoiceInteraction();
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            try {
                return super.onTransact(code, data, reply, flags);
            } catch (RuntimeException e) {
                if (!(e instanceof SecurityException)) {
                    Slog.wtf(VoiceInteractionManagerService.TAG, "VoiceInteractionManagerService Crash", e);
                }
                throw e;
            }
        }

        public void initForUser(int userHandle) {
            Slog.d(VoiceInteractionManagerService.TAG, "**************** initForUser user=" + userHandle);
            String curInteractorStr = Settings.Secure.getStringForUser(VoiceInteractionManagerService.this.mContext.getContentResolver(), "voice_interaction_service", userHandle);
            ComponentName curRecognizer = getCurRecognizer(userHandle);
            VoiceInteractionServiceInfo curInteractorInfo = null;
            Slog.d(VoiceInteractionManagerService.TAG, "curInteractorStr=" + curInteractorStr + " curRecognizer=" + curRecognizer);
            if (curInteractorStr == null && curRecognizer != null && this.mEnableService && (curInteractorInfo = findAvailInteractor(userHandle, curRecognizer.getPackageName())) != null) {
                Slog.d(VoiceInteractionManagerService.TAG, "No set interactor, found avail: " + curInteractorInfo.getServiceInfo().name);
                curRecognizer = null;
            }
            String forceInteractorPackage = getForceVoiceInteractionServicePackage(VoiceInteractionManagerService.this.mContext.getResources());
            if (forceInteractorPackage != null && (curInteractorInfo = findAvailInteractor(userHandle, forceInteractorPackage)) != null) {
                curRecognizer = null;
            }
            if (!this.mEnableService && curInteractorStr != null && !TextUtils.isEmpty(curInteractorStr)) {
                Slog.d(VoiceInteractionManagerService.TAG, "Svelte device; disabling interactor");
                setCurInteractor(null, userHandle);
                curInteractorStr = "";
            }
            if (curRecognizer != null) {
                IPackageManager pm = AppGlobals.getPackageManager();
                ServiceInfo interactorInfo = null;
                ServiceInfo recognizerInfo = null;
                ComponentName componentNameUnflattenFromString = !TextUtils.isEmpty(curInteractorStr) ? ComponentName.unflattenFromString(curInteractorStr) : null;
                try {
                    recognizerInfo = pm.getServiceInfo(curRecognizer, 0, userHandle);
                    if (componentNameUnflattenFromString != null) {
                        interactorInfo = pm.getServiceInfo(componentNameUnflattenFromString, 0, userHandle);
                    }
                } catch (RemoteException e) {
                }
                if (recognizerInfo != null && (componentNameUnflattenFromString == null || interactorInfo != null)) {
                    Slog.d(VoiceInteractionManagerService.TAG, "Current interactor/recognizer okay, done!");
                    return;
                }
                Slog.d(VoiceInteractionManagerService.TAG, "Bad recognizer (" + recognizerInfo + ") or interactor (" + interactorInfo + ")");
            }
            if (curInteractorInfo == null && this.mEnableService) {
                curInteractorInfo = findAvailInteractor(userHandle, null);
            }
            if (curInteractorInfo != null) {
                setCurInteractor(new ComponentName(curInteractorInfo.getServiceInfo().packageName, curInteractorInfo.getServiceInfo().name), userHandle);
                if (curInteractorInfo.getRecognitionService() != null) {
                    setCurRecognizer(new ComponentName(curInteractorInfo.getServiceInfo().packageName, curInteractorInfo.getRecognitionService()), userHandle);
                    return;
                }
            }
            ComponentName curRecognizer2 = findAvailRecognizer(null, userHandle);
            if (curRecognizer2 == null) {
                return;
            }
            if (curInteractorInfo == null) {
                setCurInteractor(null, userHandle);
            }
            setCurRecognizer(curRecognizer2, userHandle);
        }

        private boolean shouldEnableService(Resources res) {
            return (ActivityManager.isLowRamDeviceStatic() && getForceVoiceInteractionServicePackage(res) == null) ? false : true;
        }

        private String getForceVoiceInteractionServicePackage(Resources res) {
            String interactorPackage = res.getString(R.string.PERSOSUBSTATE_RUIM_CORPORATE_PUK_SUCCESS);
            if (TextUtils.isEmpty(interactorPackage)) {
                return null;
            }
            return interactorPackage;
        }

        public void systemRunning(boolean safeMode) {
            this.mSafeMode = safeMode;
            this.mPackageMonitor.register(VoiceInteractionManagerService.this.mContext, BackgroundThread.getHandler().getLooper(), UserHandle.ALL, true);
            new SettingsObserver(UiThread.getHandler());
            synchronized (this) {
                this.mCurUser = ActivityManager.getCurrentUser();
                switchImplementationIfNeededLocked(false);
            }
        }

        public void switchUser(int userHandle) {
            synchronized (this) {
                this.mCurUser = userHandle;
                switchImplementationIfNeededLocked(false);
            }
        }

        void switchImplementationIfNeeded(boolean force) {
            synchronized (this) {
                switchImplementationIfNeededLocked(force);
            }
        }

        void switchImplementationIfNeededLocked(boolean force) {
            if (this.mSafeMode) {
                return;
            }
            String curService = Settings.Secure.getStringForUser(VoiceInteractionManagerService.this.mResolver, "voice_interaction_service", this.mCurUser);
            ComponentName serviceComponent = null;
            ServiceInfo serviceInfo = null;
            if (curService != null && !curService.isEmpty()) {
                try {
                    serviceComponent = ComponentName.unflattenFromString(curService);
                    serviceInfo = AppGlobals.getPackageManager().getServiceInfo(serviceComponent, 0, this.mCurUser);
                } catch (RemoteException | RuntimeException e) {
                    Slog.wtf(VoiceInteractionManagerService.TAG, "Bad voice interaction service name " + curService, e);
                    serviceComponent = null;
                    serviceInfo = null;
                }
            }
            if (!force && this.mImpl != null && this.mImpl.mUser == this.mCurUser && this.mImpl.mComponent.equals(serviceComponent)) {
                return;
            }
            unloadAllKeyphraseModels();
            if (this.mImpl != null) {
                this.mImpl.shutdownLocked();
            }
            if (serviceComponent != null && serviceInfo != null) {
                this.mImpl = new VoiceInteractionManagerServiceImpl(VoiceInteractionManagerService.this.mContext, UiThread.getHandler(), this, this.mCurUser, serviceComponent);
                this.mImpl.startLocked();
            } else {
                this.mImpl = null;
            }
        }

        VoiceInteractionServiceInfo findAvailInteractor(int userHandle, String packageName) {
            List<ResolveInfo> available = VoiceInteractionManagerService.this.mContext.getPackageManager().queryIntentServicesAsUser(new Intent("android.service.voice.VoiceInteractionService"), 269221888, userHandle);
            int numAvailable = available.size();
            if (numAvailable == 0) {
                Slog.w(VoiceInteractionManagerService.TAG, "no available voice interaction services found for user " + userHandle);
                return null;
            }
            VoiceInteractionServiceInfo foundInfo = null;
            for (int i = 0; i < numAvailable; i++) {
                ServiceInfo cur = available.get(i).serviceInfo;
                if ((cur.applicationInfo.flags & 1) != 0) {
                    ComponentName comp = new ComponentName(cur.packageName, cur.name);
                    try {
                        VoiceInteractionServiceInfo info = new VoiceInteractionServiceInfo(VoiceInteractionManagerService.this.mContext.getPackageManager(), comp, userHandle);
                        if (info.getParseError() != null) {
                            Slog.w(VoiceInteractionManagerService.TAG, "Bad interaction service " + comp + ": " + info.getParseError());
                        } else if (packageName == null || info.getServiceInfo().packageName.equals(packageName)) {
                            if (foundInfo == null) {
                                foundInfo = info;
                            } else {
                                Slog.w(VoiceInteractionManagerService.TAG, "More than one voice interaction service, picking first " + new ComponentName(foundInfo.getServiceInfo().packageName, foundInfo.getServiceInfo().name) + " over " + new ComponentName(cur.packageName, cur.name));
                            }
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                        Slog.w(VoiceInteractionManagerService.TAG, "Failure looking up interaction service " + comp);
                    }
                }
            }
            return foundInfo;
        }

        ComponentName getCurInteractor(int userHandle) {
            String curInteractor = Settings.Secure.getStringForUser(VoiceInteractionManagerService.this.mContext.getContentResolver(), "voice_interaction_service", userHandle);
            if (TextUtils.isEmpty(curInteractor)) {
                return null;
            }
            Slog.d(VoiceInteractionManagerService.TAG, "getCurInteractor curInteractor=" + curInteractor + " user=" + userHandle);
            return ComponentName.unflattenFromString(curInteractor);
        }

        void setCurInteractor(ComponentName comp, int userHandle) {
            Settings.Secure.putStringForUser(VoiceInteractionManagerService.this.mContext.getContentResolver(), "voice_interaction_service", comp != null ? comp.flattenToShortString() : "", userHandle);
            Slog.d(VoiceInteractionManagerService.TAG, "setCurInteractor comp=" + comp + " user=" + userHandle);
        }

        ComponentName findAvailRecognizer(String prefPackage, int userHandle) {
            List<ResolveInfo> available = VoiceInteractionManagerService.this.mContext.getPackageManager().queryIntentServicesAsUser(new Intent("android.speech.RecognitionService"), 0, userHandle);
            int numAvailable = available.size();
            if (numAvailable == 0) {
                Slog.w(VoiceInteractionManagerService.TAG, "no available voice recognition services found for user " + userHandle);
                return null;
            }
            if (prefPackage != null) {
                for (int i = 0; i < numAvailable; i++) {
                    ServiceInfo serviceInfo = available.get(i).serviceInfo;
                    if (prefPackage.equals(serviceInfo.packageName)) {
                        return new ComponentName(serviceInfo.packageName, serviceInfo.name);
                    }
                }
            }
            if (numAvailable > 1) {
                Slog.w(VoiceInteractionManagerService.TAG, "more than one voice recognition service found, picking first");
            }
            ServiceInfo serviceInfo2 = available.get(0).serviceInfo;
            return new ComponentName(serviceInfo2.packageName, serviceInfo2.name);
        }

        ComponentName getCurRecognizer(int userHandle) {
            String curRecognizer = Settings.Secure.getStringForUser(VoiceInteractionManagerService.this.mContext.getContentResolver(), "voice_recognition_service", userHandle);
            if (TextUtils.isEmpty(curRecognizer)) {
                return null;
            }
            Slog.d(VoiceInteractionManagerService.TAG, "getCurRecognizer curRecognizer=" + curRecognizer + " user=" + userHandle);
            return ComponentName.unflattenFromString(curRecognizer);
        }

        void setCurRecognizer(ComponentName comp, int userHandle) {
            Settings.Secure.putStringForUser(VoiceInteractionManagerService.this.mContext.getContentResolver(), "voice_recognition_service", comp != null ? comp.flattenToShortString() : "", userHandle);
            Slog.d(VoiceInteractionManagerService.TAG, "setCurRecognizer comp=" + comp + " user=" + userHandle);
        }

        void resetCurAssistant(int userHandle) {
            Settings.Secure.putStringForUser(VoiceInteractionManagerService.this.mContext.getContentResolver(), "assistant", null, userHandle);
        }

        public void showSession(IVoiceInteractionService service, Bundle args, int flags) {
            synchronized (this) {
                if (this.mImpl == null || this.mImpl.mService == null || service.asBinder() != this.mImpl.mService.asBinder()) {
                    throw new SecurityException("Caller is not the current voice interaction service");
                }
                long caller = Binder.clearCallingIdentity();
                try {
                    this.mImpl.showSessionLocked(args, flags, null, null);
                } finally {
                    Binder.restoreCallingIdentity(caller);
                }
            }
        }

        public boolean deliverNewSession(IBinder token, IVoiceInteractionSession session, IVoiceInteractor interactor) {
            boolean zDeliverNewSessionLocked;
            synchronized (this) {
                if (this.mImpl == null) {
                    throw new SecurityException("deliverNewSession without running voice interaction service");
                }
                long caller = Binder.clearCallingIdentity();
                try {
                    zDeliverNewSessionLocked = this.mImpl.deliverNewSessionLocked(token, session, interactor);
                } finally {
                    Binder.restoreCallingIdentity(caller);
                }
            }
            return zDeliverNewSessionLocked;
        }

        public boolean showSessionFromSession(IBinder token, Bundle sessionArgs, int flags) {
            synchronized (this) {
                if (this.mImpl == null) {
                    Slog.w(VoiceInteractionManagerService.TAG, "showSessionFromSession without running voice interaction service");
                    return false;
                }
                long caller = Binder.clearCallingIdentity();
                try {
                    return this.mImpl.showSessionLocked(sessionArgs, flags, null, null);
                } finally {
                    Binder.restoreCallingIdentity(caller);
                }
            }
        }

        public boolean hideSessionFromSession(IBinder token) {
            synchronized (this) {
                if (this.mImpl == null) {
                    Slog.w(VoiceInteractionManagerService.TAG, "hideSessionFromSession without running voice interaction service");
                    return false;
                }
                long caller = Binder.clearCallingIdentity();
                try {
                    return this.mImpl.hideSessionLocked();
                } finally {
                    Binder.restoreCallingIdentity(caller);
                }
            }
        }

        public int startVoiceActivity(IBinder token, Intent intent, String resolvedType) {
            synchronized (this) {
                if (this.mImpl == null) {
                    Slog.w(VoiceInteractionManagerService.TAG, "startVoiceActivity without running voice interaction service");
                    return -6;
                }
                int callingPid = Binder.getCallingPid();
                int callingUid = Binder.getCallingUid();
                long caller = Binder.clearCallingIdentity();
                try {
                    return this.mImpl.startVoiceActivityLocked(callingPid, callingUid, token, intent, resolvedType);
                } finally {
                    Binder.restoreCallingIdentity(caller);
                }
            }
        }

        public void setKeepAwake(IBinder token, boolean keepAwake) {
            synchronized (this) {
                if (this.mImpl == null) {
                    Slog.w(VoiceInteractionManagerService.TAG, "setKeepAwake without running voice interaction service");
                    return;
                }
                long caller = Binder.clearCallingIdentity();
                try {
                    this.mImpl.setKeepAwakeLocked(token, keepAwake);
                } finally {
                    Binder.restoreCallingIdentity(caller);
                }
            }
        }

        public void closeSystemDialogs(IBinder token) {
            synchronized (this) {
                if (this.mImpl == null) {
                    Slog.w(VoiceInteractionManagerService.TAG, "closeSystemDialogs without running voice interaction service");
                    return;
                }
                long caller = Binder.clearCallingIdentity();
                try {
                    this.mImpl.closeSystemDialogsLocked(token);
                } finally {
                    Binder.restoreCallingIdentity(caller);
                }
            }
        }

        public void finish(IBinder token) {
            synchronized (this) {
                if (this.mImpl == null) {
                    Slog.w(VoiceInteractionManagerService.TAG, "finish without running voice interaction service");
                    return;
                }
                long caller = Binder.clearCallingIdentity();
                try {
                    this.mImpl.finishLocked(token, false);
                } finally {
                    Binder.restoreCallingIdentity(caller);
                }
            }
        }

        public void setDisabledShowContext(int flags) {
            synchronized (this) {
                if (this.mImpl == null) {
                    Slog.w(VoiceInteractionManagerService.TAG, "setDisabledShowContext without running voice interaction service");
                    return;
                }
                int callingUid = Binder.getCallingUid();
                long caller = Binder.clearCallingIdentity();
                try {
                    this.mImpl.setDisabledShowContextLocked(callingUid, flags);
                } finally {
                    Binder.restoreCallingIdentity(caller);
                }
            }
        }

        public int getDisabledShowContext() {
            synchronized (this) {
                if (this.mImpl == null) {
                    Slog.w(VoiceInteractionManagerService.TAG, "getDisabledShowContext without running voice interaction service");
                    return 0;
                }
                int callingUid = Binder.getCallingUid();
                long caller = Binder.clearCallingIdentity();
                try {
                    return this.mImpl.getDisabledShowContextLocked(callingUid);
                } finally {
                    Binder.restoreCallingIdentity(caller);
                }
            }
        }

        public int getUserDisabledShowContext() {
            synchronized (this) {
                if (this.mImpl == null) {
                    Slog.w(VoiceInteractionManagerService.TAG, "getUserDisabledShowContext without running voice interaction service");
                    return 0;
                }
                int callingUid = Binder.getCallingUid();
                long caller = Binder.clearCallingIdentity();
                try {
                    return this.mImpl.getUserDisabledShowContextLocked(callingUid);
                } finally {
                    Binder.restoreCallingIdentity(caller);
                }
            }
        }

        public SoundTrigger.KeyphraseSoundModel getKeyphraseSoundModel(int keyphraseId, String bcp47Locale) {
            enforceCallingPermission("android.permission.MANAGE_VOICE_KEYPHRASES");
            if (bcp47Locale == null) {
                throw new IllegalArgumentException("Illegal argument(s) in getKeyphraseSoundModel");
            }
            int callingUid = UserHandle.getCallingUserId();
            long caller = Binder.clearCallingIdentity();
            try {
                return VoiceInteractionManagerService.this.mDbHelper.getKeyphraseSoundModel(keyphraseId, callingUid, bcp47Locale);
            } finally {
                Binder.restoreCallingIdentity(caller);
            }
        }

        public int updateKeyphraseSoundModel(SoundTrigger.KeyphraseSoundModel model) {
            enforceCallingPermission("android.permission.MANAGE_VOICE_KEYPHRASES");
            if (model == null) {
                throw new IllegalArgumentException("Model must not be null");
            }
            long caller = Binder.clearCallingIdentity();
            try {
                if (VoiceInteractionManagerService.this.mDbHelper.updateKeyphraseSoundModel(model)) {
                    synchronized (this) {
                        if (this.mImpl != null && this.mImpl.mService != null) {
                            this.mImpl.notifySoundModelsChangedLocked();
                        }
                    }
                    return 0;
                }
                return Integer.MIN_VALUE;
            } finally {
                Binder.restoreCallingIdentity(caller);
            }
        }

        public int deleteKeyphraseSoundModel(int keyphraseId, String bcp47Locale) {
            enforceCallingPermission("android.permission.MANAGE_VOICE_KEYPHRASES");
            if (bcp47Locale == null) {
                throw new IllegalArgumentException("Illegal argument(s) in deleteKeyphraseSoundModel");
            }
            int callingUid = UserHandle.getCallingUserId();
            long caller = Binder.clearCallingIdentity();
            try {
                int unloadStatus = VoiceInteractionManagerService.this.mSoundTriggerInternal.unloadKeyphraseModel(keyphraseId);
                if (unloadStatus != 0) {
                    Slog.w(VoiceInteractionManagerService.TAG, "Unable to unload keyphrase sound model:" + unloadStatus);
                }
                boolean deleted = VoiceInteractionManagerService.this.mDbHelper.deleteKeyphraseSoundModel(keyphraseId, callingUid, bcp47Locale);
                int i = deleted ? 0 : Integer.MIN_VALUE;
                if (deleted) {
                    synchronized (this) {
                        if (this.mImpl != null && this.mImpl.mService != null) {
                            this.mImpl.notifySoundModelsChangedLocked();
                        }
                        VoiceInteractionManagerService.this.mLoadedKeyphraseIds.remove(Integer.valueOf(keyphraseId));
                    }
                }
                Binder.restoreCallingIdentity(caller);
                return i;
            } catch (Throwable th) {
                if (0 != 0) {
                    synchronized (this) {
                        if (this.mImpl != null && this.mImpl.mService != null) {
                            this.mImpl.notifySoundModelsChangedLocked();
                        }
                        VoiceInteractionManagerService.this.mLoadedKeyphraseIds.remove(Integer.valueOf(keyphraseId));
                    }
                }
                Binder.restoreCallingIdentity(caller);
                throw th;
            }
        }

        public boolean isEnrolledForKeyphrase(IVoiceInteractionService service, int keyphraseId, String bcp47Locale) {
            synchronized (this) {
                if (this.mImpl == null || this.mImpl.mService == null || service.asBinder() != this.mImpl.mService.asBinder()) {
                    throw new SecurityException("Caller is not the current voice interaction service");
                }
            }
            if (bcp47Locale == null) {
                throw new IllegalArgumentException("Illegal argument(s) in isEnrolledForKeyphrase");
            }
            int callingUid = UserHandle.getCallingUserId();
            long caller = Binder.clearCallingIdentity();
            try {
                SoundTrigger.KeyphraseSoundModel model = VoiceInteractionManagerService.this.mDbHelper.getKeyphraseSoundModel(keyphraseId, callingUid, bcp47Locale);
                return model != null;
            } finally {
                Binder.restoreCallingIdentity(caller);
            }
        }

        public SoundTrigger.ModuleProperties getDspModuleProperties(IVoiceInteractionService service) {
            SoundTrigger.ModuleProperties moduleProperties;
            synchronized (this) {
                if (this.mImpl == null || this.mImpl.mService == null || service == null || service.asBinder() != this.mImpl.mService.asBinder()) {
                    throw new SecurityException("Caller is not the current voice interaction service");
                }
                long caller = Binder.clearCallingIdentity();
                try {
                    moduleProperties = VoiceInteractionManagerService.this.mSoundTriggerInternal.getModuleProperties();
                } finally {
                    Binder.restoreCallingIdentity(caller);
                }
            }
            return moduleProperties;
        }

        public int startRecognition(IVoiceInteractionService service, int keyphraseId, String bcp47Locale, IRecognitionStatusCallback callback, SoundTrigger.RecognitionConfig recognitionConfig) {
            synchronized (this) {
                if (this.mImpl == null || this.mImpl.mService == null || service == null || service.asBinder() != this.mImpl.mService.asBinder()) {
                    throw new SecurityException("Caller is not the current voice interaction service");
                }
                if (callback == null || recognitionConfig == null || bcp47Locale == null) {
                    throw new IllegalArgumentException("Illegal argument(s) in startRecognition");
                }
            }
            int callingUid = UserHandle.getCallingUserId();
            long caller = Binder.clearCallingIdentity();
            try {
                SoundTrigger.KeyphraseSoundModel soundModel = VoiceInteractionManagerService.this.mDbHelper.getKeyphraseSoundModel(keyphraseId, callingUid, bcp47Locale);
                if (soundModel == null || soundModel.uuid == null || soundModel.keyphrases == null) {
                    Slog.w(VoiceInteractionManagerService.TAG, "No matching sound model found in startRecognition");
                    return Integer.MIN_VALUE;
                }
                synchronized (this) {
                    VoiceInteractionManagerService.this.mLoadedKeyphraseIds.add(Integer.valueOf(keyphraseId));
                }
                return VoiceInteractionManagerService.this.mSoundTriggerInternal.startRecognition(keyphraseId, soundModel, callback, recognitionConfig);
            } finally {
                Binder.restoreCallingIdentity(caller);
            }
        }

        public int stopRecognition(IVoiceInteractionService service, int keyphraseId, IRecognitionStatusCallback callback) {
            synchronized (this) {
                if (this.mImpl == null || this.mImpl.mService == null || service == null || service.asBinder() != this.mImpl.mService.asBinder()) {
                    throw new SecurityException("Caller is not the current voice interaction service");
                }
            }
            long caller = Binder.clearCallingIdentity();
            try {
                return VoiceInteractionManagerService.this.mSoundTriggerInternal.stopRecognition(keyphraseId, callback);
            } finally {
                Binder.restoreCallingIdentity(caller);
            }
        }

        private synchronized void unloadAllKeyphraseModels() {
            Iterator keyphraseId$iterator = VoiceInteractionManagerService.this.mLoadedKeyphraseIds.iterator();
            while (keyphraseId$iterator.hasNext()) {
                int keyphraseId = ((Integer) keyphraseId$iterator.next()).intValue();
                long caller = Binder.clearCallingIdentity();
                try {
                    int status = VoiceInteractionManagerService.this.mSoundTriggerInternal.unloadKeyphraseModel(keyphraseId);
                    if (status != 0) {
                        Slog.w(VoiceInteractionManagerService.TAG, "Failed to unload keyphrase " + keyphraseId + ":" + status);
                    }
                } finally {
                    Binder.restoreCallingIdentity(caller);
                }
            }
            VoiceInteractionManagerService.this.mLoadedKeyphraseIds.clear();
        }

        public ComponentName getActiveServiceComponentName() {
            ComponentName componentName;
            enforceCallingPermission("android.permission.ACCESS_VOICE_INTERACTION_SERVICE");
            synchronized (this) {
                componentName = this.mImpl != null ? this.mImpl.mComponent : null;
            }
            return componentName;
        }

        public boolean showSessionForActiveService(Bundle args, int sourceFlags, IVoiceInteractionSessionShowCallback showCallback, IBinder activityToken) {
            enforceCallingPermission("android.permission.ACCESS_VOICE_INTERACTION_SERVICE");
            synchronized (this) {
                if (this.mImpl == null) {
                    Slog.w(VoiceInteractionManagerService.TAG, "showSessionForActiveService without running voice interactionservice");
                    return false;
                }
                long caller = Binder.clearCallingIdentity();
                try {
                    return this.mImpl.showSessionLocked(args, sourceFlags | 1 | 2, showCallback, activityToken);
                } finally {
                    Binder.restoreCallingIdentity(caller);
                }
            }
        }

        public void hideCurrentSession() throws RemoteException {
            enforceCallingPermission("android.permission.ACCESS_VOICE_INTERACTION_SERVICE");
            synchronized (this) {
                if (this.mImpl == null) {
                    return;
                }
                long caller = Binder.clearCallingIdentity();
                try {
                    if (this.mImpl.mActiveSession != null && this.mImpl.mActiveSession.mSession != null) {
                        try {
                            this.mImpl.mActiveSession.mSession.closeSystemDialogs();
                        } catch (RemoteException e) {
                            Log.w(VoiceInteractionManagerService.TAG, "Failed to call closeSystemDialogs", e);
                        }
                    }
                } finally {
                    Binder.restoreCallingIdentity(caller);
                }
            }
        }

        public void launchVoiceAssistFromKeyguard() {
            enforceCallingPermission("android.permission.ACCESS_VOICE_INTERACTION_SERVICE");
            synchronized (this) {
                if (this.mImpl == null) {
                    Slog.w(VoiceInteractionManagerService.TAG, "launchVoiceAssistFromKeyguard without running voice interactionservice");
                    return;
                }
                long caller = Binder.clearCallingIdentity();
                try {
                    this.mImpl.launchVoiceAssistFromKeyguard();
                } finally {
                    Binder.restoreCallingIdentity(caller);
                }
            }
        }

        public boolean isSessionRunning() {
            boolean z = false;
            enforceCallingPermission("android.permission.ACCESS_VOICE_INTERACTION_SERVICE");
            synchronized (this) {
                if (this.mImpl != null) {
                    if (this.mImpl.mActiveSession != null) {
                        z = true;
                    }
                }
            }
            return z;
        }

        public boolean activeServiceSupportsAssist() {
            boolean supportsAssist;
            enforceCallingPermission("android.permission.ACCESS_VOICE_INTERACTION_SERVICE");
            synchronized (this) {
                supportsAssist = (this.mImpl == null || this.mImpl.mInfo == null) ? false : this.mImpl.mInfo.getSupportsAssist();
            }
            return supportsAssist;
        }

        public boolean activeServiceSupportsLaunchFromKeyguard() throws RemoteException {
            boolean supportsLaunchFromKeyguard;
            enforceCallingPermission("android.permission.ACCESS_VOICE_INTERACTION_SERVICE");
            synchronized (this) {
                supportsLaunchFromKeyguard = (this.mImpl == null || this.mImpl.mInfo == null) ? false : this.mImpl.mInfo.getSupportsLaunchFromKeyguard();
            }
            return supportsLaunchFromKeyguard;
        }

        public void onLockscreenShown() {
            enforceCallingPermission("android.permission.ACCESS_VOICE_INTERACTION_SERVICE");
            synchronized (this) {
                if (this.mImpl == null) {
                    return;
                }
                long caller = Binder.clearCallingIdentity();
                try {
                    if (this.mImpl.mActiveSession != null && this.mImpl.mActiveSession.mSession != null) {
                        try {
                            this.mImpl.mActiveSession.mSession.onLockscreenShown();
                        } catch (RemoteException e) {
                            Log.w(VoiceInteractionManagerService.TAG, "Failed to call onLockscreenShown", e);
                        }
                    }
                } finally {
                    Binder.restoreCallingIdentity(caller);
                }
            }
        }

        public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (VoiceInteractionManagerService.this.mContext.checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
                pw.println("Permission Denial: can't dump PowerManager from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
                return;
            }
            synchronized (this) {
                pw.println("VOICE INTERACTION MANAGER (dumpsys voiceinteraction)");
                pw.println("  mEnableService: " + this.mEnableService);
                if (this.mImpl == null) {
                    pw.println("  (No active implementation)");
                } else {
                    this.mImpl.dumpLocked(fd, pw, args);
                    VoiceInteractionManagerService.this.mSoundTriggerInternal.dump(fd, pw, args);
                }
            }
        }

        private void enforceCallingPermission(String permission) {
            if (VoiceInteractionManagerService.this.mContext.checkCallingOrSelfPermission(permission) == 0) {
            } else {
                throw new SecurityException("Caller does not hold the permission " + permission);
            }
        }

        class SettingsObserver extends ContentObserver {
            SettingsObserver(Handler handler) {
                super(handler);
                ContentResolver resolver = VoiceInteractionManagerService.this.mContext.getContentResolver();
                resolver.registerContentObserver(Settings.Secure.getUriFor("voice_interaction_service"), false, this, -1);
            }

            @Override
            public void onChange(boolean selfChange) {
                synchronized (VoiceInteractionManagerServiceStub.this) {
                    VoiceInteractionManagerServiceStub.this.switchImplementationIfNeededLocked(false);
                }
            }
        }
    }
}
