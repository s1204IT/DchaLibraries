package com.android.server.voiceinteraction;

import android.app.ActivityManager;
import android.app.AppGlobals;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.database.ContentObserver;
import android.hardware.soundtrigger.IRecognitionStatusCallback;
import android.hardware.soundtrigger.SoundTrigger;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.voice.IVoiceInteractionService;
import android.service.voice.IVoiceInteractionSession;
import android.service.voice.VoiceInteractionServiceInfo;
import android.text.TextUtils;
import android.util.Slog;
import com.android.internal.app.IVoiceInteractionManagerService;
import com.android.internal.app.IVoiceInteractor;
import com.android.internal.content.PackageMonitor;
import com.android.internal.os.BackgroundThread;
import com.android.server.SystemService;
import com.android.server.UiThread;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.List;

public class VoiceInteractionManagerService extends SystemService {
    static final boolean DEBUG = false;
    static final String TAG = "VoiceInteractionManagerService";
    final Context mContext;
    final DatabaseHelper mDbHelper;
    final ContentResolver mResolver;
    private final VoiceInteractionManagerServiceStub mServiceStub;
    final SoundTriggerHelper mSoundTriggerHelper;

    public VoiceInteractionManagerService(Context context) {
        super(context);
        this.mServiceStub = new VoiceInteractionManagerServiceStub();
        this.mContext = context;
        this.mResolver = context.getContentResolver();
        this.mDbHelper = new DatabaseHelper(context);
        this.mSoundTriggerHelper = new SoundTriggerHelper(context);
    }

    @Override
    public void onStart() {
        publishBinderService("voiceinteraction", this.mServiceStub);
    }

    @Override
    public void onBootPhase(int phase) {
        if (phase == 600) {
            this.mServiceStub.systemRunning(isSafeMode());
        }
    }

    @Override
    public void onStartUser(int userHandle) {
        this.mServiceStub.initForUser(userHandle);
    }

    @Override
    public void onSwitchUser(int userHandle) {
        this.mServiceStub.switchUser(userHandle);
    }

    class VoiceInteractionManagerServiceStub extends IVoiceInteractionManagerService.Stub {
        private int mCurUser;
        VoiceInteractionManagerServiceImpl mImpl;
        PackageMonitor mPackageMonitor = new PackageMonitor() {
            public boolean onHandleForceStop(Intent intent, String[] packages, int uid, boolean doit) {
                return super.onHandleForceStop(intent, packages, uid, doit);
            }

            public void onHandleUserStop(Intent intent, int userHandle) {
            }

            public void onSomePackagesChanged() {
                ComponentName curRecognizer;
                int userHandle = getChangingUserId();
                ComponentName curInteractor = VoiceInteractionManagerServiceStub.this.getCurInteractor(userHandle);
                ComponentName curRecognizer2 = VoiceInteractionManagerServiceStub.this.getCurRecognizer(userHandle);
                if (curRecognizer2 == null) {
                    if (anyPackagesAppearing() && (curRecognizer = VoiceInteractionManagerServiceStub.this.findAvailRecognizer(null, userHandle)) != null) {
                        VoiceInteractionManagerServiceStub.this.setCurRecognizer(curRecognizer, userHandle);
                        return;
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
                            return;
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
        };
        private boolean mSafeMode;

        VoiceInteractionManagerServiceStub() {
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
            String curInteractorStr = Settings.Secure.getStringForUser(VoiceInteractionManagerService.this.mContext.getContentResolver(), "voice_interaction_service", userHandle);
            ComponentName curRecognizer = getCurRecognizer(userHandle);
            VoiceInteractionServiceInfo curInteractorInfo = null;
            if (curInteractorStr == null && curRecognizer != null && !ActivityManager.isLowRamDeviceStatic() && (curInteractorInfo = findAvailInteractor(userHandle, curRecognizer)) != null) {
                curRecognizer = null;
            }
            if (ActivityManager.isLowRamDeviceStatic() && curInteractorStr != null && !TextUtils.isEmpty(curInteractorStr)) {
                setCurInteractor(null, userHandle);
                curInteractorStr = "";
            }
            if (curRecognizer != null) {
                IPackageManager pm = AppGlobals.getPackageManager();
                ServiceInfo interactorInfo = null;
                ServiceInfo recognizerInfo = null;
                ComponentName curInteractor = !TextUtils.isEmpty(curInteractorStr) ? ComponentName.unflattenFromString(curInteractorStr) : null;
                try {
                    recognizerInfo = pm.getServiceInfo(curRecognizer, 0, userHandle);
                    if (curInteractor != null) {
                        interactorInfo = pm.getServiceInfo(curInteractor, 0, userHandle);
                    }
                } catch (RemoteException e) {
                }
                if (recognizerInfo != null && (curInteractor == null || interactorInfo != null)) {
                    return;
                }
            }
            if (curInteractorInfo == null && !ActivityManager.isLowRamDeviceStatic()) {
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
            if (curRecognizer2 != null) {
                if (curInteractorInfo == null) {
                    setCurInteractor(null, userHandle);
                }
                setCurRecognizer(curRecognizer2, userHandle);
            }
        }

        public void systemRunning(boolean safeMode) {
            this.mSafeMode = safeMode;
            this.mPackageMonitor.register(VoiceInteractionManagerService.this.mContext, BackgroundThread.getHandler().getLooper(), UserHandle.ALL, true);
            new SettingsObserver(UiThread.getHandler());
            synchronized (this) {
                this.mCurUser = ActivityManager.getCurrentUser();
                switchImplementationIfNeededLocked(VoiceInteractionManagerService.DEBUG);
            }
        }

        public void switchUser(int userHandle) {
            synchronized (this) {
                this.mCurUser = userHandle;
                switchImplementationIfNeededLocked(VoiceInteractionManagerService.DEBUG);
            }
        }

        void switchImplementationIfNeededLocked(boolean force) {
            if (!this.mSafeMode) {
                String curService = Settings.Secure.getStringForUser(VoiceInteractionManagerService.this.mResolver, "voice_interaction_service", this.mCurUser);
                ComponentName serviceComponent = null;
                if (curService != null && !curService.isEmpty()) {
                    try {
                        serviceComponent = ComponentName.unflattenFromString(curService);
                    } catch (RuntimeException e) {
                        Slog.wtf(VoiceInteractionManagerService.TAG, "Bad voice interaction service name " + curService, e);
                        serviceComponent = null;
                    }
                }
                if (force || this.mImpl == null || this.mImpl.mUser != this.mCurUser || !this.mImpl.mComponent.equals(serviceComponent)) {
                    VoiceInteractionManagerService.this.mSoundTriggerHelper.stopAllRecognitions();
                    if (this.mImpl != null) {
                        this.mImpl.shutdownLocked();
                    }
                    if (serviceComponent != null) {
                        this.mImpl = new VoiceInteractionManagerServiceImpl(VoiceInteractionManagerService.this.mContext, UiThread.getHandler(), this, this.mCurUser, serviceComponent);
                        this.mImpl.startLocked();
                    } else {
                        this.mImpl = null;
                    }
                }
            }
        }

        VoiceInteractionServiceInfo findAvailInteractor(int userHandle, ComponentName recognizer) {
            List<ResolveInfo> available = VoiceInteractionManagerService.this.mContext.getPackageManager().queryIntentServicesAsUser(new Intent("android.service.voice.VoiceInteractionService"), 0, userHandle);
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
                        if (info.getParseError() == null) {
                            if (recognizer == null || info.getServiceInfo().packageName.equals(recognizer.getPackageName())) {
                                if (foundInfo == null) {
                                    foundInfo = info;
                                } else {
                                    Slog.w(VoiceInteractionManagerService.TAG, "More than one voice interaction service, picking first " + new ComponentName(foundInfo.getServiceInfo().packageName, foundInfo.getServiceInfo().name) + " over " + new ComponentName(cur.packageName, cur.name));
                                }
                            }
                        } else {
                            Slog.w(VoiceInteractionManagerService.TAG, "Bad interaction service " + comp + ": " + info.getParseError());
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                        Slog.w(VoiceInteractionManagerService.TAG, "Failure looking up interaction service " + comp);
                    } catch (RemoteException e2) {
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
            return ComponentName.unflattenFromString(curInteractor);
        }

        void setCurInteractor(ComponentName comp, int userHandle) {
            Settings.Secure.putStringForUser(VoiceInteractionManagerService.this.mContext.getContentResolver(), "voice_interaction_service", comp != null ? comp.flattenToShortString() : "", userHandle);
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
            return ComponentName.unflattenFromString(curRecognizer);
        }

        void setCurRecognizer(ComponentName comp, int userHandle) {
            Settings.Secure.putStringForUser(VoiceInteractionManagerService.this.mContext.getContentResolver(), "voice_recognition_service", comp != null ? comp.flattenToShortString() : "", userHandle);
        }

        public void startSession(IVoiceInteractionService service, Bundle args) {
            synchronized (this) {
                if (this.mImpl == null || this.mImpl.mService == null || service.asBinder() != this.mImpl.mService.asBinder()) {
                    throw new SecurityException("Caller is not the current voice interaction service");
                }
                int callingPid = Binder.getCallingPid();
                int callingUid = Binder.getCallingUid();
                long caller = Binder.clearCallingIdentity();
                try {
                    this.mImpl.startSessionLocked(callingPid, callingUid, args);
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
                int callingPid = Binder.getCallingPid();
                int callingUid = Binder.getCallingUid();
                long caller = Binder.clearCallingIdentity();
                try {
                    zDeliverNewSessionLocked = this.mImpl.deliverNewSessionLocked(callingPid, callingUid, token, session, interactor);
                } finally {
                    Binder.restoreCallingIdentity(caller);
                }
            }
            return zDeliverNewSessionLocked;
        }

        public int startVoiceActivity(IBinder token, Intent intent, String resolvedType) {
            int iStartVoiceActivityLocked;
            synchronized (this) {
                if (this.mImpl == null) {
                    Slog.w(VoiceInteractionManagerService.TAG, "startVoiceActivity without running voice interaction service");
                    iStartVoiceActivityLocked = -6;
                } else {
                    int callingPid = Binder.getCallingPid();
                    int callingUid = Binder.getCallingUid();
                    long caller = Binder.clearCallingIdentity();
                    if (!SystemProperties.getBoolean("persist.test.voice_interaction", VoiceInteractionManagerService.DEBUG)) {
                        throw new SecurityException("Voice interaction not supported");
                    }
                    try {
                        iStartVoiceActivityLocked = this.mImpl.startVoiceActivityLocked(callingPid, callingUid, token, intent, resolvedType);
                    } finally {
                        Binder.restoreCallingIdentity(caller);
                    }
                }
            }
            return iStartVoiceActivityLocked;
        }

        public void finish(IBinder token) {
            synchronized (this) {
                if (this.mImpl == null) {
                    Slog.w(VoiceInteractionManagerService.TAG, "finish without running voice interaction service");
                    return;
                }
                int callingPid = Binder.getCallingPid();
                int callingUid = Binder.getCallingUid();
                long caller = Binder.clearCallingIdentity();
                try {
                    this.mImpl.finishLocked(callingPid, callingUid, token);
                } finally {
                    Binder.restoreCallingIdentity(caller);
                }
            }
        }

        public SoundTrigger.KeyphraseSoundModel getKeyphraseSoundModel(int keyphraseId, String bcp47Locale) {
            synchronized (this) {
                if (VoiceInteractionManagerService.this.mContext.checkCallingPermission("android.permission.MANAGE_VOICE_KEYPHRASES") != 0) {
                    throw new SecurityException("Caller does not hold the permission android.permission.MANAGE_VOICE_KEYPHRASES");
                }
            }
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
            synchronized (this) {
                if (VoiceInteractionManagerService.this.mContext.checkCallingPermission("android.permission.MANAGE_VOICE_KEYPHRASES") != 0) {
                    throw new SecurityException("Caller does not hold the permission android.permission.MANAGE_VOICE_KEYPHRASES");
                }
                if (model == null) {
                    throw new IllegalArgumentException("Model must not be null");
                }
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
                return SoundTriggerHelper.STATUS_ERROR;
            } finally {
                Binder.restoreCallingIdentity(caller);
            }
        }

        public int deleteKeyphraseSoundModel(int keyphraseId, String bcp47Locale) {
            synchronized (this) {
                if (VoiceInteractionManagerService.this.mContext.checkCallingPermission("android.permission.MANAGE_VOICE_KEYPHRASES") != 0) {
                    throw new SecurityException("Caller does not hold the permission android.permission.MANAGE_VOICE_KEYPHRASES");
                }
            }
            if (bcp47Locale == null) {
                throw new IllegalArgumentException("Illegal argument(s) in deleteKeyphraseSoundModel");
            }
            int callingUid = UserHandle.getCallingUserId();
            long caller = Binder.clearCallingIdentity();
            try {
                boolean deleted = VoiceInteractionManagerService.this.mDbHelper.deleteKeyphraseSoundModel(keyphraseId, callingUid, bcp47Locale);
                int i = deleted ? 0 : SoundTriggerHelper.STATUS_ERROR;
                if (deleted) {
                    synchronized (this) {
                        if (this.mImpl != null && this.mImpl.mService != null) {
                            this.mImpl.notifySoundModelsChangedLocked();
                        }
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
                return model != null ? true : VoiceInteractionManagerService.DEBUG;
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
                    moduleProperties = VoiceInteractionManagerService.this.mSoundTriggerHelper.moduleProperties;
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
                    return SoundTriggerHelper.STATUS_ERROR;
                }
                return VoiceInteractionManagerService.this.mSoundTriggerHelper.startRecognition(keyphraseId, soundModel, callback, recognitionConfig);
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
                return VoiceInteractionManagerService.this.mSoundTriggerHelper.stopRecognition(keyphraseId, callback);
            } finally {
                Binder.restoreCallingIdentity(caller);
            }
        }

        public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (VoiceInteractionManagerService.this.mContext.checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
                pw.println("Permission Denial: can't dump PowerManager from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
                return;
            }
            synchronized (this) {
                pw.println("VOICE INTERACTION MANAGER (dumpsys voiceinteraction)\n");
                if (this.mImpl == null) {
                    pw.println("  (No active implementation)");
                } else {
                    this.mImpl.dumpLocked(fd, pw, args);
                    VoiceInteractionManagerService.this.mSoundTriggerHelper.dump(fd, pw, args);
                }
            }
        }

        class SettingsObserver extends ContentObserver {
            SettingsObserver(Handler handler) {
                super(handler);
                ContentResolver resolver = VoiceInteractionManagerService.this.mContext.getContentResolver();
                resolver.registerContentObserver(Settings.Secure.getUriFor("voice_interaction_service"), VoiceInteractionManagerService.DEBUG, this);
            }

            @Override
            public void onChange(boolean selfChange) {
                synchronized (VoiceInteractionManagerServiceStub.this) {
                    VoiceInteractionManagerServiceStub.this.switchImplementationIfNeededLocked(VoiceInteractionManagerService.DEBUG);
                }
            }
        }
    }
}
