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
import android.content.pm.ShortcutServiceInternal;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.hardware.soundtrigger.IRecognitionStatusCallback;
import android.hardware.soundtrigger.SoundTrigger;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.service.voice.IVoiceInteractionService;
import android.service.voice.IVoiceInteractionSession;
import android.service.voice.VoiceInteractionManagerInternal;
import android.service.voice.VoiceInteractionServiceInfo;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import android.util.Slog;
import com.android.internal.app.IVoiceInteractionManagerService;
import com.android.internal.app.IVoiceInteractionSessionListener;
import com.android.internal.app.IVoiceInteractionSessionShowCallback;
import com.android.internal.app.IVoiceInteractor;
import com.android.internal.content.PackageMonitor;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.Preconditions;
import com.android.server.FgThread;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.UiThread;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.soundtrigger.SoundTriggerInternal;
import com.android.server.voiceinteraction.VoiceInteractionManagerService;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.List;

public class VoiceInteractionManagerService extends SystemService {
    static final boolean DEBUG = false;
    static final String TAG = "VoiceInteractionManagerService";
    final ActivityManagerInternal mAmInternal;
    final Context mContext;
    final DatabaseHelper mDbHelper;
    final ArraySet<Integer> mLoadedKeyphraseIds;
    final ContentResolver mResolver;
    private final VoiceInteractionManagerServiceStub mServiceStub;
    ShortcutServiceInternal mShortcutServiceInternal;
    SoundTriggerInternal mSoundTriggerInternal;
    final UserManager mUserManager;
    private final RemoteCallbackList<IVoiceInteractionSessionListener> mVoiceInteractionSessionListeners;

    public VoiceInteractionManagerService(Context context) {
        super(context);
        this.mLoadedKeyphraseIds = new ArraySet<>();
        this.mVoiceInteractionSessionListeners = new RemoteCallbackList<>();
        this.mContext = context;
        this.mResolver = context.getContentResolver();
        this.mDbHelper = new DatabaseHelper(context);
        this.mServiceStub = new VoiceInteractionManagerServiceStub();
        this.mAmInternal = (ActivityManagerInternal) Preconditions.checkNotNull((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class));
        this.mUserManager = (UserManager) Preconditions.checkNotNull((UserManager) context.getSystemService(UserManager.class));
        ((PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class)).setVoiceInteractionPackagesProvider(new PackageManagerInternal.PackagesProvider() {
            public String[] getPackages(int i) {
                VoiceInteractionManagerService.this.mServiceStub.initForUser(i);
                ComponentName curInteractor = VoiceInteractionManagerService.this.mServiceStub.getCurInteractor(i);
                if (curInteractor != null) {
                    return new String[]{curInteractor.getPackageName()};
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
    public void onBootPhase(int i) {
        if (500 == i) {
            this.mShortcutServiceInternal = (ShortcutServiceInternal) Preconditions.checkNotNull((ShortcutServiceInternal) LocalServices.getService(ShortcutServiceInternal.class));
            this.mSoundTriggerInternal = (SoundTriggerInternal) LocalServices.getService(SoundTriggerInternal.class);
        } else if (i == 600) {
            this.mServiceStub.systemRunning(isSafeMode());
        }
    }

    @Override
    public void onStartUser(int i) {
        this.mServiceStub.initForUser(i);
    }

    @Override
    public void onUnlockUser(int i) {
        this.mServiceStub.initForUser(i);
        this.mServiceStub.switchImplementationIfNeeded(false);
    }

    @Override
    public void onSwitchUser(int i) {
        this.mServiceStub.switchUser(i);
    }

    class LocalService extends VoiceInteractionManagerInternal {
        LocalService() {
        }

        public void startLocalVoiceInteraction(IBinder iBinder, Bundle bundle) {
            VoiceInteractionManagerService.this.mServiceStub.startLocalVoiceInteraction(iBinder, bundle);
        }

        public boolean supportsLocalVoiceInteraction() {
            return VoiceInteractionManagerService.this.mServiceStub.supportsLocalVoiceInteraction();
        }

        public void stopLocalVoiceInteraction(IBinder iBinder) {
            VoiceInteractionManagerService.this.mServiceStub.stopLocalVoiceInteraction(iBinder);
        }
    }

    class VoiceInteractionManagerServiceStub extends IVoiceInteractionManagerService.Stub {
        private int mCurUser;
        private boolean mCurUserUnlocked;
        private final boolean mEnableService;
        VoiceInteractionManagerServiceImpl mImpl;
        PackageMonitor mPackageMonitor = new PackageMonitor() {
            public boolean onHandleForceStop(Intent intent, String[] strArr, int i, boolean z) {
                int userId = UserHandle.getUserId(i);
                ComponentName curInteractor = VoiceInteractionManagerServiceStub.this.getCurInteractor(userId);
                ComponentName curRecognizer = VoiceInteractionManagerServiceStub.this.getCurRecognizer(userId);
                boolean z2 = false;
                for (String str : strArr) {
                    if ((curInteractor != null && str.equals(curInteractor.getPackageName())) || (curRecognizer != null && str.equals(curRecognizer.getPackageName()))) {
                        z2 = true;
                        break;
                    }
                }
                if (z2 && z) {
                    synchronized (VoiceInteractionManagerServiceStub.this) {
                        VoiceInteractionManagerServiceStub.this.unloadAllKeyphraseModels();
                        if (VoiceInteractionManagerServiceStub.this.mImpl != null) {
                            VoiceInteractionManagerServiceStub.this.mImpl.shutdownLocked();
                            VoiceInteractionManagerServiceStub.this.setImplLocked(null);
                        }
                        VoiceInteractionManagerServiceStub.this.setCurInteractor(null, userId);
                        VoiceInteractionManagerServiceStub.this.setCurRecognizer(null, userId);
                        VoiceInteractionManagerServiceStub.this.resetCurAssistant(userId);
                        VoiceInteractionManagerServiceStub.this.initForUser(userId);
                        VoiceInteractionManagerServiceStub.this.switchImplementationIfNeededLocked(true);
                    }
                }
                return z2;
            }

            public void onHandleUserStop(Intent intent, int i) {
            }

            public void onPackageModified(String str) {
                if (VoiceInteractionManagerServiceStub.this.mCurUser == getChangingUserId() && isPackageAppearing(str) == 0) {
                    ComponentName curInteractor = VoiceInteractionManagerServiceStub.this.getCurInteractor(VoiceInteractionManagerServiceStub.this.mCurUser);
                    if (curInteractor == null) {
                        VoiceInteractionServiceInfo voiceInteractionServiceInfoFindAvailInteractor = VoiceInteractionManagerServiceStub.this.findAvailInteractor(VoiceInteractionManagerServiceStub.this.mCurUser, str);
                        if (voiceInteractionServiceInfoFindAvailInteractor != null) {
                            VoiceInteractionManagerServiceStub.this.setCurInteractor(new ComponentName(voiceInteractionServiceInfoFindAvailInteractor.getServiceInfo().packageName, voiceInteractionServiceInfoFindAvailInteractor.getServiceInfo().name), VoiceInteractionManagerServiceStub.this.mCurUser);
                            if (VoiceInteractionManagerServiceStub.this.getCurRecognizer(VoiceInteractionManagerServiceStub.this.mCurUser) == null && voiceInteractionServiceInfoFindAvailInteractor.getRecognitionService() != null) {
                                VoiceInteractionManagerServiceStub.this.setCurRecognizer(new ComponentName(voiceInteractionServiceInfoFindAvailInteractor.getServiceInfo().packageName, voiceInteractionServiceInfoFindAvailInteractor.getRecognitionService()), VoiceInteractionManagerServiceStub.this.mCurUser);
                                return;
                            }
                            return;
                        }
                        return;
                    }
                    if (didSomePackagesChange()) {
                        if (curInteractor != null && str.equals(curInteractor.getPackageName())) {
                            VoiceInteractionManagerServiceStub.this.switchImplementationIfNeeded(true);
                            return;
                        }
                        return;
                    }
                    if (curInteractor != null && isComponentModified(curInteractor.getClassName())) {
                        VoiceInteractionManagerServiceStub.this.switchImplementationIfNeeded(true);
                    }
                }
            }

            public void onSomePackagesChanged() {
                ComponentName componentNameFindAvailRecognizer;
                int changingUserId = getChangingUserId();
                synchronized (VoiceInteractionManagerServiceStub.this) {
                    ComponentName curInteractor = VoiceInteractionManagerServiceStub.this.getCurInteractor(changingUserId);
                    ComponentName curRecognizer = VoiceInteractionManagerServiceStub.this.getCurRecognizer(changingUserId);
                    ComponentName curAssistant = VoiceInteractionManagerServiceStub.this.getCurAssistant(changingUserId);
                    if (curRecognizer == null) {
                        if (anyPackagesAppearing() && (componentNameFindAvailRecognizer = VoiceInteractionManagerServiceStub.this.findAvailRecognizer(null, changingUserId)) != null) {
                            VoiceInteractionManagerServiceStub.this.setCurRecognizer(componentNameFindAvailRecognizer, changingUserId);
                        }
                        return;
                    }
                    if (curInteractor != null) {
                        if (isPackageDisappearing(curInteractor.getPackageName()) == 3) {
                            VoiceInteractionManagerServiceStub.this.setCurInteractor(null, changingUserId);
                            VoiceInteractionManagerServiceStub.this.setCurRecognizer(null, changingUserId);
                            VoiceInteractionManagerServiceStub.this.resetCurAssistant(changingUserId);
                            VoiceInteractionManagerServiceStub.this.initForUser(changingUserId);
                            return;
                        }
                        if (isPackageAppearing(curInteractor.getPackageName()) != 0 && VoiceInteractionManagerServiceStub.this.mImpl != null && curInteractor.getPackageName().equals(VoiceInteractionManagerServiceStub.this.mImpl.mComponent.getPackageName())) {
                            VoiceInteractionManagerServiceStub.this.switchImplementationIfNeededLocked(true);
                        }
                        return;
                    }
                    if (curAssistant != null && isPackageDisappearing(curAssistant.getPackageName()) == 3) {
                        VoiceInteractionManagerServiceStub.this.setCurInteractor(null, changingUserId);
                        VoiceInteractionManagerServiceStub.this.setCurRecognizer(null, changingUserId);
                        VoiceInteractionManagerServiceStub.this.resetCurAssistant(changingUserId);
                        VoiceInteractionManagerServiceStub.this.initForUser(changingUserId);
                        return;
                    }
                    int iIsPackageDisappearing = isPackageDisappearing(curRecognizer.getPackageName());
                    if (iIsPackageDisappearing == 3 || iIsPackageDisappearing == 2) {
                        VoiceInteractionManagerServiceStub.this.setCurRecognizer(VoiceInteractionManagerServiceStub.this.findAvailRecognizer(null, changingUserId), changingUserId);
                    } else if (isPackageModified(curRecognizer.getPackageName())) {
                        VoiceInteractionManagerServiceStub.this.setCurRecognizer(VoiceInteractionManagerServiceStub.this.findAvailRecognizer(curRecognizer.getPackageName(), changingUserId), changingUserId);
                    }
                }
            }
        };
        private boolean mSafeMode;

        VoiceInteractionManagerServiceStub() {
            this.mEnableService = shouldEnableService(VoiceInteractionManagerService.this.mContext);
        }

        void startLocalVoiceInteraction(final IBinder iBinder, Bundle bundle) {
            if (this.mImpl == null) {
                return;
            }
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                this.mImpl.showSessionLocked(bundle, 16, new IVoiceInteractionSessionShowCallback.Stub() {
                    public void onFailed() {
                    }

                    public void onShown() {
                        VoiceInteractionManagerService.this.mAmInternal.onLocalVoiceInteractionStarted(iBinder, VoiceInteractionManagerServiceStub.this.mImpl.mActiveSession.mSession, VoiceInteractionManagerServiceStub.this.mImpl.mActiveSession.mInteractor);
                    }
                }, iBinder);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void stopLocalVoiceInteraction(IBinder iBinder) {
            if (this.mImpl == null) {
                return;
            }
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                this.mImpl.finishLocked(iBinder, true);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public boolean supportsLocalVoiceInteraction() {
            if (this.mImpl == null) {
                return false;
            }
            return this.mImpl.supportsLocalVoiceInteraction();
        }

        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            try {
                return super.onTransact(i, parcel, parcel2, i2);
            } catch (RuntimeException e) {
                if (!(e instanceof SecurityException)) {
                    Slog.wtf(VoiceInteractionManagerService.TAG, "VoiceInteractionManagerService Crash", e);
                }
                throw e;
            }
        }

        public void initForUser(int i) {
            VoiceInteractionServiceInfo voiceInteractionServiceInfoFindAvailInteractor;
            ComponentName componentNameUnflattenFromString;
            ServiceInfo serviceInfo;
            ServiceInfo serviceInfo2;
            String stringForUser = Settings.Secure.getStringForUser(VoiceInteractionManagerService.this.mContext.getContentResolver(), "voice_interaction_service", i);
            ComponentName curRecognizer = getCurRecognizer(i);
            if (stringForUser != null || curRecognizer == null || !this.mEnableService) {
                voiceInteractionServiceInfoFindAvailInteractor = null;
            } else {
                voiceInteractionServiceInfoFindAvailInteractor = findAvailInteractor(i, curRecognizer.getPackageName());
                if (voiceInteractionServiceInfoFindAvailInteractor != null) {
                    curRecognizer = null;
                }
            }
            String forceVoiceInteractionServicePackage = getForceVoiceInteractionServicePackage(VoiceInteractionManagerService.this.mContext.getResources());
            if (forceVoiceInteractionServicePackage != null && (voiceInteractionServiceInfoFindAvailInteractor = findAvailInteractor(i, forceVoiceInteractionServicePackage)) != null) {
                curRecognizer = null;
            }
            if (!this.mEnableService && stringForUser != null && !TextUtils.isEmpty(stringForUser)) {
                setCurInteractor(null, i);
                stringForUser = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
            }
            if (curRecognizer != null) {
                IPackageManager packageManager = AppGlobals.getPackageManager();
                if (!TextUtils.isEmpty(stringForUser)) {
                    componentNameUnflattenFromString = ComponentName.unflattenFromString(stringForUser);
                } else {
                    componentNameUnflattenFromString = null;
                }
                try {
                    serviceInfo = packageManager.getServiceInfo(curRecognizer, 786432, i);
                    if (componentNameUnflattenFromString != null) {
                        try {
                            serviceInfo2 = packageManager.getServiceInfo(componentNameUnflattenFromString, 786432, i);
                        } catch (RemoteException e) {
                            serviceInfo2 = null;
                        }
                    } else {
                        serviceInfo2 = null;
                    }
                } catch (RemoteException e2) {
                    serviceInfo = null;
                }
                if (serviceInfo != null && (componentNameUnflattenFromString == null || serviceInfo2 != null)) {
                    return;
                }
            }
            if (voiceInteractionServiceInfoFindAvailInteractor == null && this.mEnableService) {
                voiceInteractionServiceInfoFindAvailInteractor = findAvailInteractor(i, null);
            }
            if (voiceInteractionServiceInfoFindAvailInteractor != null) {
                setCurInteractor(new ComponentName(voiceInteractionServiceInfoFindAvailInteractor.getServiceInfo().packageName, voiceInteractionServiceInfoFindAvailInteractor.getServiceInfo().name), i);
                if (voiceInteractionServiceInfoFindAvailInteractor.getRecognitionService() != null) {
                    setCurRecognizer(new ComponentName(voiceInteractionServiceInfoFindAvailInteractor.getServiceInfo().packageName, voiceInteractionServiceInfoFindAvailInteractor.getRecognitionService()), i);
                    return;
                }
            }
            ComponentName componentNameFindAvailRecognizer = findAvailRecognizer(null, i);
            if (componentNameFindAvailRecognizer != null) {
                if (voiceInteractionServiceInfoFindAvailInteractor == null) {
                    setCurInteractor(null, i);
                }
                setCurRecognizer(componentNameFindAvailRecognizer, i);
            }
        }

        private boolean shouldEnableService(Context context) {
            return (!ActivityManager.isLowRamDeviceStatic() && context.getPackageManager().hasSystemFeature("android.software.voice_recognizers")) || getForceVoiceInteractionServicePackage(context.getResources()) != null;
        }

        private String getForceVoiceInteractionServicePackage(Resources resources) {
            String string = resources.getString(R.string.aerr_application_repeated);
            if (TextUtils.isEmpty(string)) {
                return null;
            }
            return string;
        }

        public void systemRunning(boolean z) {
            this.mSafeMode = z;
            this.mPackageMonitor.register(VoiceInteractionManagerService.this.mContext, BackgroundThread.getHandler().getLooper(), UserHandle.ALL, true);
            new SettingsObserver(UiThread.getHandler());
            synchronized (this) {
                this.mCurUser = ActivityManager.getCurrentUser();
                switchImplementationIfNeededLocked(false);
            }
        }

        public void switchUser(final int i) {
            FgThread.getHandler().post(new Runnable() {
                @Override
                public final void run() {
                    VoiceInteractionManagerService.VoiceInteractionManagerServiceStub.lambda$switchUser$0(this.f$0, i);
                }
            });
        }

        public static void lambda$switchUser$0(VoiceInteractionManagerServiceStub voiceInteractionManagerServiceStub, int i) {
            synchronized (voiceInteractionManagerServiceStub) {
                voiceInteractionManagerServiceStub.mCurUser = i;
                voiceInteractionManagerServiceStub.mCurUserUnlocked = false;
                voiceInteractionManagerServiceStub.switchImplementationIfNeededLocked(false);
            }
        }

        void switchImplementationIfNeeded(boolean z) {
            synchronized (this) {
                switchImplementationIfNeededLocked(z);
            }
        }

        void switchImplementationIfNeededLocked(boolean z) {
            ServiceInfo serviceInfo;
            ComponentName componentName;
            if (!this.mSafeMode) {
                String stringForUser = Settings.Secure.getStringForUser(VoiceInteractionManagerService.this.mResolver, "voice_interaction_service", this.mCurUser);
                boolean z2 = false;
                if (stringForUser != null && !stringForUser.isEmpty()) {
                    try {
                        ComponentName componentNameUnflattenFromString = ComponentName.unflattenFromString(stringForUser);
                        serviceInfo = AppGlobals.getPackageManager().getServiceInfo(componentNameUnflattenFromString, 0, this.mCurUser);
                        componentName = componentNameUnflattenFromString;
                    } catch (RemoteException | RuntimeException e) {
                        Slog.wtf(VoiceInteractionManagerService.TAG, "Bad voice interaction service name " + stringForUser, e);
                        serviceInfo = null;
                        componentName = null;
                    }
                } else {
                    serviceInfo = null;
                    componentName = null;
                }
                if (componentName != null && serviceInfo != null) {
                    z2 = true;
                }
                if (VoiceInteractionManagerService.this.mUserManager.isUserUnlockingOrUnlocked(this.mCurUser)) {
                    if (z2) {
                        VoiceInteractionManagerService.this.mShortcutServiceInternal.setShortcutHostPackage(VoiceInteractionManagerService.TAG, componentName.getPackageName(), this.mCurUser);
                        VoiceInteractionManagerService.this.mAmInternal.setAllowAppSwitches(VoiceInteractionManagerService.TAG, serviceInfo.applicationInfo.uid, this.mCurUser);
                    } else {
                        VoiceInteractionManagerService.this.mShortcutServiceInternal.setShortcutHostPackage(VoiceInteractionManagerService.TAG, (String) null, this.mCurUser);
                        VoiceInteractionManagerService.this.mAmInternal.setAllowAppSwitches(VoiceInteractionManagerService.TAG, -1, this.mCurUser);
                    }
                }
                if (z || this.mImpl == null || this.mImpl.mUser != this.mCurUser || !this.mImpl.mComponent.equals(componentName)) {
                    unloadAllKeyphraseModels();
                    if (this.mImpl != null) {
                        this.mImpl.shutdownLocked();
                    }
                    if (z2) {
                        setImplLocked(new VoiceInteractionManagerServiceImpl(VoiceInteractionManagerService.this.mContext, UiThread.getHandler(), this, this.mCurUser, componentName));
                        this.mImpl.startLocked();
                    } else {
                        setImplLocked(null);
                    }
                }
            }
        }

        VoiceInteractionServiceInfo findAvailInteractor(int i, String str) {
            List listQueryIntentServicesAsUser = VoiceInteractionManagerService.this.mContext.getPackageManager().queryIntentServicesAsUser(new Intent("android.service.voice.VoiceInteractionService"), 269221888, i);
            int size = listQueryIntentServicesAsUser.size();
            VoiceInteractionServiceInfo voiceInteractionServiceInfo = null;
            if (size == 0) {
                Slog.w(VoiceInteractionManagerService.TAG, "no available voice interaction services found for user " + i);
                return null;
            }
            for (int i2 = 0; i2 < size; i2++) {
                ServiceInfo serviceInfo = ((ResolveInfo) listQueryIntentServicesAsUser.get(i2)).serviceInfo;
                if ((serviceInfo.applicationInfo.flags & 1) != 0) {
                    ComponentName componentName = new ComponentName(serviceInfo.packageName, serviceInfo.name);
                    try {
                        VoiceInteractionServiceInfo voiceInteractionServiceInfo2 = new VoiceInteractionServiceInfo(VoiceInteractionManagerService.this.mContext.getPackageManager(), componentName, i);
                        if (voiceInteractionServiceInfo2.getParseError() == null) {
                            if (str == null || voiceInteractionServiceInfo2.getServiceInfo().packageName.equals(str)) {
                                if (voiceInteractionServiceInfo != null) {
                                    Slog.w(VoiceInteractionManagerService.TAG, "More than one voice interaction service, picking first " + new ComponentName(voiceInteractionServiceInfo.getServiceInfo().packageName, voiceInteractionServiceInfo.getServiceInfo().name) + " over " + new ComponentName(serviceInfo.packageName, serviceInfo.name));
                                } else {
                                    voiceInteractionServiceInfo = voiceInteractionServiceInfo2;
                                }
                            }
                        } else {
                            Slog.w(VoiceInteractionManagerService.TAG, "Bad interaction service " + componentName + ": " + voiceInteractionServiceInfo2.getParseError());
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                        Slog.w(VoiceInteractionManagerService.TAG, "Failure looking up interaction service " + componentName);
                    }
                }
            }
            return voiceInteractionServiceInfo;
        }

        ComponentName getCurInteractor(int i) {
            String stringForUser = Settings.Secure.getStringForUser(VoiceInteractionManagerService.this.mContext.getContentResolver(), "voice_interaction_service", i);
            if (TextUtils.isEmpty(stringForUser)) {
                return null;
            }
            return ComponentName.unflattenFromString(stringForUser);
        }

        void setCurInteractor(ComponentName componentName, int i) {
            Settings.Secure.putStringForUser(VoiceInteractionManagerService.this.mContext.getContentResolver(), "voice_interaction_service", componentName != null ? componentName.flattenToShortString() : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, i);
        }

        ComponentName findAvailRecognizer(String str, int i) {
            List listQueryIntentServicesAsUser = VoiceInteractionManagerService.this.mContext.getPackageManager().queryIntentServicesAsUser(new Intent("android.speech.RecognitionService"), 786432, i);
            int size = listQueryIntentServicesAsUser.size();
            if (size == 0) {
                Slog.w(VoiceInteractionManagerService.TAG, "no available voice recognition services found for user " + i);
                return null;
            }
            if (str != null) {
                for (int i2 = 0; i2 < size; i2++) {
                    ServiceInfo serviceInfo = ((ResolveInfo) listQueryIntentServicesAsUser.get(i2)).serviceInfo;
                    if (str.equals(serviceInfo.packageName)) {
                        return new ComponentName(serviceInfo.packageName, serviceInfo.name);
                    }
                }
            }
            if (size > 1) {
                Slog.w(VoiceInteractionManagerService.TAG, "more than one voice recognition service found, picking first");
            }
            ServiceInfo serviceInfo2 = ((ResolveInfo) listQueryIntentServicesAsUser.get(0)).serviceInfo;
            return new ComponentName(serviceInfo2.packageName, serviceInfo2.name);
        }

        ComponentName getCurRecognizer(int i) {
            String stringForUser = Settings.Secure.getStringForUser(VoiceInteractionManagerService.this.mContext.getContentResolver(), "voice_recognition_service", i);
            if (TextUtils.isEmpty(stringForUser)) {
                return null;
            }
            return ComponentName.unflattenFromString(stringForUser);
        }

        void setCurRecognizer(ComponentName componentName, int i) {
            Settings.Secure.putStringForUser(VoiceInteractionManagerService.this.mContext.getContentResolver(), "voice_recognition_service", componentName != null ? componentName.flattenToShortString() : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, i);
        }

        ComponentName getCurAssistant(int i) {
            String stringForUser = Settings.Secure.getStringForUser(VoiceInteractionManagerService.this.mContext.getContentResolver(), "assistant", i);
            if (TextUtils.isEmpty(stringForUser)) {
                return null;
            }
            return ComponentName.unflattenFromString(stringForUser);
        }

        void resetCurAssistant(int i) {
            Settings.Secure.putStringForUser(VoiceInteractionManagerService.this.mContext.getContentResolver(), "assistant", null, i);
        }

        public void showSession(IVoiceInteractionService iVoiceInteractionService, Bundle bundle, int i) {
            synchronized (this) {
                if (this.mImpl == null || this.mImpl.mService == null || iVoiceInteractionService.asBinder() != this.mImpl.mService.asBinder()) {
                    throw new SecurityException("Caller is not the current voice interaction service");
                }
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    this.mImpl.showSessionLocked(bundle, i, null, null);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        }

        public boolean deliverNewSession(IBinder iBinder, IVoiceInteractionSession iVoiceInteractionSession, IVoiceInteractor iVoiceInteractor) {
            boolean zDeliverNewSessionLocked;
            synchronized (this) {
                if (this.mImpl == null) {
                    throw new SecurityException("deliverNewSession without running voice interaction service");
                }
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    zDeliverNewSessionLocked = this.mImpl.deliverNewSessionLocked(iBinder, iVoiceInteractionSession, iVoiceInteractor);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
            return zDeliverNewSessionLocked;
        }

        public boolean showSessionFromSession(IBinder iBinder, Bundle bundle, int i) {
            synchronized (this) {
                if (this.mImpl == null) {
                    Slog.w(VoiceInteractionManagerService.TAG, "showSessionFromSession without running voice interaction service");
                    return false;
                }
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    return this.mImpl.showSessionLocked(bundle, i, null, null);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        }

        public boolean hideSessionFromSession(IBinder iBinder) {
            synchronized (this) {
                if (this.mImpl == null) {
                    Slog.w(VoiceInteractionManagerService.TAG, "hideSessionFromSession without running voice interaction service");
                    return false;
                }
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    return this.mImpl.hideSessionLocked();
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        }

        public int startVoiceActivity(IBinder iBinder, Intent intent, String str) {
            synchronized (this) {
                if (this.mImpl == null) {
                    Slog.w(VoiceInteractionManagerService.TAG, "startVoiceActivity without running voice interaction service");
                    return -96;
                }
                int callingPid = Binder.getCallingPid();
                int callingUid = Binder.getCallingUid();
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    return this.mImpl.startVoiceActivityLocked(callingPid, callingUid, iBinder, intent, str);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        }

        public int startAssistantActivity(IBinder iBinder, Intent intent, String str) {
            synchronized (this) {
                if (this.mImpl == null) {
                    Slog.w(VoiceInteractionManagerService.TAG, "startAssistantActivity without running voice interaction service");
                    return -96;
                }
                int callingPid = Binder.getCallingPid();
                int callingUid = Binder.getCallingUid();
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    return this.mImpl.startAssistantActivityLocked(callingPid, callingUid, iBinder, intent, str);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        }

        public void setKeepAwake(IBinder iBinder, boolean z) {
            synchronized (this) {
                if (this.mImpl == null) {
                    Slog.w(VoiceInteractionManagerService.TAG, "setKeepAwake without running voice interaction service");
                    return;
                }
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    this.mImpl.setKeepAwakeLocked(iBinder, z);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        }

        public void closeSystemDialogs(IBinder iBinder) {
            synchronized (this) {
                if (this.mImpl == null) {
                    Slog.w(VoiceInteractionManagerService.TAG, "closeSystemDialogs without running voice interaction service");
                    return;
                }
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    this.mImpl.closeSystemDialogsLocked(iBinder);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        }

        public void finish(IBinder iBinder) {
            synchronized (this) {
                if (this.mImpl == null) {
                    Slog.w(VoiceInteractionManagerService.TAG, "finish without running voice interaction service");
                    return;
                }
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    this.mImpl.finishLocked(iBinder, false);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        }

        public void setDisabledShowContext(int i) {
            synchronized (this) {
                if (this.mImpl == null) {
                    Slog.w(VoiceInteractionManagerService.TAG, "setDisabledShowContext without running voice interaction service");
                    return;
                }
                int callingUid = Binder.getCallingUid();
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    this.mImpl.setDisabledShowContextLocked(callingUid, i);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
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
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    return this.mImpl.getDisabledShowContextLocked(callingUid);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
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
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    return this.mImpl.getUserDisabledShowContextLocked(callingUid);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        }

        public SoundTrigger.KeyphraseSoundModel getKeyphraseSoundModel(int i, String str) {
            enforceCallingPermission("android.permission.MANAGE_VOICE_KEYPHRASES");
            if (str == null) {
                throw new IllegalArgumentException("Illegal argument(s) in getKeyphraseSoundModel");
            }
            int callingUserId = UserHandle.getCallingUserId();
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                return VoiceInteractionManagerService.this.mDbHelper.getKeyphraseSoundModel(i, callingUserId, str);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public int updateKeyphraseSoundModel(SoundTrigger.KeyphraseSoundModel keyphraseSoundModel) {
            enforceCallingPermission("android.permission.MANAGE_VOICE_KEYPHRASES");
            if (keyphraseSoundModel == null) {
                throw new IllegalArgumentException("Model must not be null");
            }
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                if (VoiceInteractionManagerService.this.mDbHelper.updateKeyphraseSoundModel(keyphraseSoundModel)) {
                    synchronized (this) {
                        if (this.mImpl != null && this.mImpl.mService != null) {
                            this.mImpl.notifySoundModelsChangedLocked();
                        }
                    }
                    return 0;
                }
                return Integer.MIN_VALUE;
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public int deleteKeyphraseSoundModel(int i, String str) {
            enforceCallingPermission("android.permission.MANAGE_VOICE_KEYPHRASES");
            if (str == null) {
                throw new IllegalArgumentException("Illegal argument(s) in deleteKeyphraseSoundModel");
            }
            int callingUserId = UserHandle.getCallingUserId();
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                int iUnloadKeyphraseModel = VoiceInteractionManagerService.this.mSoundTriggerInternal.unloadKeyphraseModel(i);
                if (iUnloadKeyphraseModel != 0) {
                    Slog.w(VoiceInteractionManagerService.TAG, "Unable to unload keyphrase sound model:" + iUnloadKeyphraseModel);
                }
                boolean zDeleteKeyphraseSoundModel = VoiceInteractionManagerService.this.mDbHelper.deleteKeyphraseSoundModel(i, callingUserId, str);
                int i2 = zDeleteKeyphraseSoundModel ? 0 : Integer.MIN_VALUE;
                if (zDeleteKeyphraseSoundModel) {
                    synchronized (this) {
                        if (this.mImpl != null && this.mImpl.mService != null) {
                            this.mImpl.notifySoundModelsChangedLocked();
                        }
                        VoiceInteractionManagerService.this.mLoadedKeyphraseIds.remove(Integer.valueOf(i));
                    }
                }
                return i2;
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public boolean isEnrolledForKeyphrase(IVoiceInteractionService iVoiceInteractionService, int i, String str) {
            synchronized (this) {
                if (this.mImpl == null || this.mImpl.mService == null || iVoiceInteractionService.asBinder() != this.mImpl.mService.asBinder()) {
                    throw new SecurityException("Caller is not the current voice interaction service");
                }
            }
            if (str == null) {
                throw new IllegalArgumentException("Illegal argument(s) in isEnrolledForKeyphrase");
            }
            int callingUserId = UserHandle.getCallingUserId();
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                return VoiceInteractionManagerService.this.mDbHelper.getKeyphraseSoundModel(i, callingUserId, str) != null;
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public SoundTrigger.ModuleProperties getDspModuleProperties(IVoiceInteractionService iVoiceInteractionService) {
            SoundTrigger.ModuleProperties moduleProperties;
            synchronized (this) {
                if (this.mImpl == null || this.mImpl.mService == null || iVoiceInteractionService == null || iVoiceInteractionService.asBinder() != this.mImpl.mService.asBinder()) {
                    throw new SecurityException("Caller is not the current voice interaction service");
                }
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    moduleProperties = VoiceInteractionManagerService.this.mSoundTriggerInternal.getModuleProperties();
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
            return moduleProperties;
        }

        public int startRecognition(IVoiceInteractionService iVoiceInteractionService, int i, String str, IRecognitionStatusCallback iRecognitionStatusCallback, SoundTrigger.RecognitionConfig recognitionConfig) {
            synchronized (this) {
                if (this.mImpl == null || this.mImpl.mService == null || iVoiceInteractionService == null || iVoiceInteractionService.asBinder() != this.mImpl.mService.asBinder()) {
                    throw new SecurityException("Caller is not the current voice interaction service");
                }
                if (iRecognitionStatusCallback == null || recognitionConfig == null || str == null) {
                    throw new IllegalArgumentException("Illegal argument(s) in startRecognition");
                }
            }
            int callingUserId = UserHandle.getCallingUserId();
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                SoundTrigger.KeyphraseSoundModel keyphraseSoundModel = VoiceInteractionManagerService.this.mDbHelper.getKeyphraseSoundModel(i, callingUserId, str);
                if (keyphraseSoundModel != null && keyphraseSoundModel.uuid != null && keyphraseSoundModel.keyphrases != null) {
                    synchronized (this) {
                        VoiceInteractionManagerService.this.mLoadedKeyphraseIds.add(Integer.valueOf(i));
                    }
                    return VoiceInteractionManagerService.this.mSoundTriggerInternal.startRecognition(i, keyphraseSoundModel, iRecognitionStatusCallback, recognitionConfig);
                }
                Slog.w(VoiceInteractionManagerService.TAG, "No matching sound model found in startRecognition");
                return Integer.MIN_VALUE;
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public int stopRecognition(IVoiceInteractionService iVoiceInteractionService, int i, IRecognitionStatusCallback iRecognitionStatusCallback) {
            synchronized (this) {
                if (this.mImpl == null || this.mImpl.mService == null || iVoiceInteractionService == null || iVoiceInteractionService.asBinder() != this.mImpl.mService.asBinder()) {
                    throw new SecurityException("Caller is not the current voice interaction service");
                }
            }
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                return VoiceInteractionManagerService.this.mSoundTriggerInternal.stopRecognition(i, iRecognitionStatusCallback);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        private synchronized void unloadAllKeyphraseModels() {
            for (int i = 0; i < VoiceInteractionManagerService.this.mLoadedKeyphraseIds.size(); i++) {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    int iUnloadKeyphraseModel = VoiceInteractionManagerService.this.mSoundTriggerInternal.unloadKeyphraseModel(VoiceInteractionManagerService.this.mLoadedKeyphraseIds.valueAt(i).intValue());
                    if (iUnloadKeyphraseModel != 0) {
                        Slog.w(VoiceInteractionManagerService.TAG, "Failed to unload keyphrase " + VoiceInteractionManagerService.this.mLoadedKeyphraseIds.valueAt(i) + ":" + iUnloadKeyphraseModel);
                    }
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                } catch (Throwable th) {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                    throw th;
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

        public boolean showSessionForActiveService(Bundle bundle, int i, IVoiceInteractionSessionShowCallback iVoiceInteractionSessionShowCallback, IBinder iBinder) {
            enforceCallingPermission("android.permission.ACCESS_VOICE_INTERACTION_SERVICE");
            synchronized (this) {
                if (this.mImpl == null) {
                    Slog.w(VoiceInteractionManagerService.TAG, "showSessionForActiveService without running voice interactionservice");
                    return false;
                }
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    return this.mImpl.showSessionLocked(bundle, i | 1 | 2, iVoiceInteractionSessionShowCallback, iBinder);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        }

        public void hideCurrentSession() throws RemoteException {
            enforceCallingPermission("android.permission.ACCESS_VOICE_INTERACTION_SERVICE");
            synchronized (this) {
                if (this.mImpl == null) {
                    return;
                }
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    if (this.mImpl.mActiveSession != null && this.mImpl.mActiveSession.mSession != null) {
                        try {
                            this.mImpl.mActiveSession.mSession.closeSystemDialogs();
                        } catch (RemoteException e) {
                            Log.w(VoiceInteractionManagerService.TAG, "Failed to call closeSystemDialogs", e);
                        }
                    }
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
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
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    this.mImpl.launchVoiceAssistFromKeyguard();
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        }

        public boolean isSessionRunning() {
            boolean z;
            enforceCallingPermission("android.permission.ACCESS_VOICE_INTERACTION_SERVICE");
            synchronized (this) {
                z = (this.mImpl == null || this.mImpl.mActiveSession == null) ? false : true;
            }
            return z;
        }

        public boolean activeServiceSupportsAssist() {
            boolean z;
            enforceCallingPermission("android.permission.ACCESS_VOICE_INTERACTION_SERVICE");
            synchronized (this) {
                z = (this.mImpl == null || this.mImpl.mInfo == null || !this.mImpl.mInfo.getSupportsAssist()) ? false : true;
            }
            return z;
        }

        public boolean activeServiceSupportsLaunchFromKeyguard() throws RemoteException {
            boolean z;
            enforceCallingPermission("android.permission.ACCESS_VOICE_INTERACTION_SERVICE");
            synchronized (this) {
                z = (this.mImpl == null || this.mImpl.mInfo == null || !this.mImpl.mInfo.getSupportsLaunchFromKeyguard()) ? false : true;
            }
            return z;
        }

        public void onLockscreenShown() {
            enforceCallingPermission("android.permission.ACCESS_VOICE_INTERACTION_SERVICE");
            synchronized (this) {
                if (this.mImpl == null) {
                    return;
                }
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    if (this.mImpl.mActiveSession != null && this.mImpl.mActiveSession.mSession != null) {
                        try {
                            this.mImpl.mActiveSession.mSession.onLockscreenShown();
                        } catch (RemoteException e) {
                            Log.w(VoiceInteractionManagerService.TAG, "Failed to call onLockscreenShown", e);
                        }
                    }
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        }

        public void registerVoiceInteractionSessionListener(IVoiceInteractionSessionListener iVoiceInteractionSessionListener) {
            enforceCallingPermission("android.permission.ACCESS_VOICE_INTERACTION_SERVICE");
            synchronized (this) {
                VoiceInteractionManagerService.this.mVoiceInteractionSessionListeners.register(iVoiceInteractionSessionListener);
            }
        }

        public void onSessionShown() {
            synchronized (this) {
                int iBeginBroadcast = VoiceInteractionManagerService.this.mVoiceInteractionSessionListeners.beginBroadcast();
                for (int i = 0; i < iBeginBroadcast; i++) {
                    try {
                        VoiceInteractionManagerService.this.mVoiceInteractionSessionListeners.getBroadcastItem(i).onVoiceSessionShown();
                    } catch (RemoteException e) {
                        Slog.e(VoiceInteractionManagerService.TAG, "Error delivering voice interaction open event.", e);
                    }
                }
                VoiceInteractionManagerService.this.mVoiceInteractionSessionListeners.finishBroadcast();
            }
        }

        public void onSessionHidden() {
            synchronized (this) {
                int iBeginBroadcast = VoiceInteractionManagerService.this.mVoiceInteractionSessionListeners.beginBroadcast();
                for (int i = 0; i < iBeginBroadcast; i++) {
                    try {
                        VoiceInteractionManagerService.this.mVoiceInteractionSessionListeners.getBroadcastItem(i).onVoiceSessionHidden();
                    } catch (RemoteException e) {
                        Slog.e(VoiceInteractionManagerService.TAG, "Error delivering voice interaction closed event.", e);
                    }
                }
                VoiceInteractionManagerService.this.mVoiceInteractionSessionListeners.finishBroadcast();
            }
        }

        public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
            if (DumpUtils.checkDumpPermission(VoiceInteractionManagerService.this.mContext, VoiceInteractionManagerService.TAG, printWriter)) {
                synchronized (this) {
                    printWriter.println("VOICE INTERACTION MANAGER (dumpsys voiceinteraction)");
                    printWriter.println("  mEnableService: " + this.mEnableService);
                    if (this.mImpl == null) {
                        printWriter.println("  (No active implementation)");
                    } else {
                        this.mImpl.dumpLocked(fileDescriptor, printWriter, strArr);
                        VoiceInteractionManagerService.this.mSoundTriggerInternal.dump(fileDescriptor, printWriter, strArr);
                    }
                }
            }
        }

        private void enforceCallingPermission(String str) {
            if (VoiceInteractionManagerService.this.mContext.checkCallingOrSelfPermission(str) != 0) {
                throw new SecurityException("Caller does not hold the permission " + str);
            }
        }

        private void setImplLocked(VoiceInteractionManagerServiceImpl voiceInteractionManagerServiceImpl) {
            this.mImpl = voiceInteractionManagerServiceImpl;
            VoiceInteractionManagerService.this.mAmInternal.notifyActiveVoiceInteractionServiceChanged(getActiveServiceComponentName());
        }

        class SettingsObserver extends ContentObserver {
            SettingsObserver(Handler handler) {
                super(handler);
                VoiceInteractionManagerService.this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("voice_interaction_service"), false, this, -1);
            }

            @Override
            public void onChange(boolean z) {
                synchronized (VoiceInteractionManagerServiceStub.this) {
                    VoiceInteractionManagerServiceStub.this.switchImplementationIfNeededLocked(false);
                }
            }
        }
    }
}
