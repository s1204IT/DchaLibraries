package com.android.server.voiceinteraction;

import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.ActivityOptions;
import android.app.IActivityManager;
import android.app.ProfilerInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.service.voice.IVoiceInteractionService;
import android.service.voice.IVoiceInteractionSession;
import android.service.voice.VoiceInteractionServiceInfo;
import android.util.PrintWriterPrinter;
import android.util.Slog;
import android.view.IWindowManager;
import com.android.internal.app.IVoiceInteractionSessionShowCallback;
import com.android.internal.app.IVoiceInteractor;
import com.android.server.LocalServices;
import com.android.server.policy.PhoneWindowManager;
import com.android.server.voiceinteraction.VoiceInteractionManagerService;
import com.android.server.voiceinteraction.VoiceInteractionSessionConnection;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

class VoiceInteractionManagerServiceImpl implements VoiceInteractionSessionConnection.Callback {
    static final String CLOSE_REASON_VOICE_INTERACTION = "voiceinteraction";
    static final String TAG = "VoiceInteractionServiceManager";
    VoiceInteractionSessionConnection mActiveSession;
    final ComponentName mComponent;
    final Context mContext;
    int mDisabledShowContext;
    final Handler mHandler;
    final IWindowManager mIWindowManager;
    final VoiceInteractionServiceInfo mInfo;
    IVoiceInteractionService mService;
    final VoiceInteractionManagerService.VoiceInteractionManagerServiceStub mServiceStub;
    final ComponentName mSessionComponentName;
    final int mUser;
    final boolean mValid;
    boolean mBound = false;
    final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(intent.getAction())) {
                String stringExtra = intent.getStringExtra(PhoneWindowManager.SYSTEM_DIALOG_REASON_KEY);
                if (!VoiceInteractionManagerServiceImpl.CLOSE_REASON_VOICE_INTERACTION.equals(stringExtra) && !"dream".equals(stringExtra)) {
                    synchronized (VoiceInteractionManagerServiceImpl.this.mServiceStub) {
                        if (VoiceInteractionManagerServiceImpl.this.mActiveSession != null && VoiceInteractionManagerServiceImpl.this.mActiveSession.mSession != null) {
                            try {
                                VoiceInteractionManagerServiceImpl.this.mActiveSession.mSession.closeSystemDialogs();
                            } catch (RemoteException e) {
                            }
                        }
                    }
                }
            }
        }
    };
    final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            synchronized (VoiceInteractionManagerServiceImpl.this.mServiceStub) {
                VoiceInteractionManagerServiceImpl.this.mService = IVoiceInteractionService.Stub.asInterface(iBinder);
                try {
                    VoiceInteractionManagerServiceImpl.this.mService.ready();
                } catch (RemoteException e) {
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            VoiceInteractionManagerServiceImpl.this.mService = null;
        }
    };
    final IActivityManager mAm = ActivityManager.getService();

    VoiceInteractionManagerServiceImpl(Context context, Handler handler, VoiceInteractionManagerService.VoiceInteractionManagerServiceStub voiceInteractionManagerServiceStub, int i, ComponentName componentName) {
        this.mContext = context;
        this.mHandler = handler;
        this.mServiceStub = voiceInteractionManagerServiceStub;
        this.mUser = i;
        this.mComponent = componentName;
        try {
            this.mInfo = new VoiceInteractionServiceInfo(context.getPackageManager(), componentName, this.mUser);
            if (this.mInfo.getParseError() != null) {
                Slog.w(TAG, "Bad voice interaction service: " + this.mInfo.getParseError());
                this.mSessionComponentName = null;
                this.mIWindowManager = null;
                this.mValid = false;
                return;
            }
            this.mValid = true;
            this.mSessionComponentName = new ComponentName(componentName.getPackageName(), this.mInfo.getSessionService());
            this.mIWindowManager = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.CLOSE_SYSTEM_DIALOGS");
            this.mContext.registerReceiver(this.mBroadcastReceiver, intentFilter, null, handler);
        } catch (PackageManager.NameNotFoundException e) {
            Slog.w(TAG, "Voice interaction service not found: " + componentName, e);
            this.mInfo = null;
            this.mSessionComponentName = null;
            this.mIWindowManager = null;
            this.mValid = false;
        }
    }

    public boolean showSessionLocked(Bundle bundle, int i, IVoiceInteractionSessionShowCallback iVoiceInteractionSessionShowCallback, IBinder iBinder) {
        List<IBinder> topVisibleActivities;
        if (this.mActiveSession == null) {
            this.mActiveSession = new VoiceInteractionSessionConnection(this.mServiceStub, this.mSessionComponentName, this.mUser, this.mContext, this, this.mInfo.getServiceInfo().applicationInfo.uid, this.mHandler);
        }
        if (iBinder != null) {
            topVisibleActivities = new ArrayList<>();
            topVisibleActivities.add(iBinder);
        } else {
            topVisibleActivities = ((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class)).getTopVisibleActivities();
        }
        return this.mActiveSession.showLocked(bundle, i, this.mDisabledShowContext, iVoiceInteractionSessionShowCallback, topVisibleActivities);
    }

    public boolean hideSessionLocked() {
        if (this.mActiveSession != null) {
            return this.mActiveSession.hideLocked();
        }
        return false;
    }

    public boolean deliverNewSessionLocked(IBinder iBinder, IVoiceInteractionSession iVoiceInteractionSession, IVoiceInteractor iVoiceInteractor) {
        if (this.mActiveSession == null || iBinder != this.mActiveSession.mToken) {
            Slog.w(TAG, "deliverNewSession does not match active session");
            return false;
        }
        this.mActiveSession.deliverNewSessionLocked(iVoiceInteractionSession, iVoiceInteractor);
        return true;
    }

    public int startVoiceActivityLocked(int i, int i2, IBinder iBinder, Intent intent, String str) {
        try {
            if (this.mActiveSession != null && iBinder == this.mActiveSession.mToken) {
                if (!this.mActiveSession.mShown) {
                    Slog.w(TAG, "startVoiceActivity not allowed on hidden session");
                    return -100;
                }
                Intent intent2 = new Intent(intent);
                intent2.addCategory("android.intent.category.VOICE");
                intent2.addFlags(402653184);
                return this.mAm.startVoiceActivity(this.mComponent.getPackageName(), i, i2, intent2, str, this.mActiveSession.mSession, this.mActiveSession.mInteractor, 0, (ProfilerInfo) null, (Bundle) null, this.mUser);
            }
            Slog.w(TAG, "startVoiceActivity does not match active session");
            return -99;
        } catch (RemoteException e) {
            throw new IllegalStateException("Unexpected remote error", e);
        }
    }

    public int startAssistantActivityLocked(int i, int i2, IBinder iBinder, Intent intent, String str) {
        try {
            if (this.mActiveSession != null && iBinder == this.mActiveSession.mToken) {
                if (!this.mActiveSession.mShown) {
                    Slog.w(TAG, "startAssistantActivity not allowed on hidden session");
                    return -90;
                }
                Intent intent2 = new Intent(intent);
                intent2.addFlags(268435456);
                ActivityOptions activityOptionsMakeBasic = ActivityOptions.makeBasic();
                activityOptionsMakeBasic.setLaunchActivityType(4);
                return this.mAm.startAssistantActivity(this.mComponent.getPackageName(), i, i2, intent2, str, activityOptionsMakeBasic.toBundle(), this.mUser);
            }
            Slog.w(TAG, "startAssistantActivity does not match active session");
            return -89;
        } catch (RemoteException e) {
            throw new IllegalStateException("Unexpected remote error", e);
        }
    }

    public void setKeepAwakeLocked(IBinder iBinder, boolean z) {
        try {
            if (this.mActiveSession != null && iBinder == this.mActiveSession.mToken) {
                this.mAm.setVoiceKeepAwake(this.mActiveSession.mSession, z);
                return;
            }
            Slog.w(TAG, "setKeepAwake does not match active session");
        } catch (RemoteException e) {
            throw new IllegalStateException("Unexpected remote error", e);
        }
    }

    public void closeSystemDialogsLocked(IBinder iBinder) {
        try {
            if (this.mActiveSession != null && iBinder == this.mActiveSession.mToken) {
                this.mAm.closeSystemDialogs(CLOSE_REASON_VOICE_INTERACTION);
                return;
            }
            Slog.w(TAG, "closeSystemDialogs does not match active session");
        } catch (RemoteException e) {
            throw new IllegalStateException("Unexpected remote error", e);
        }
    }

    public void finishLocked(IBinder iBinder, boolean z) {
        if (this.mActiveSession == null || (!z && iBinder != this.mActiveSession.mToken)) {
            Slog.w(TAG, "finish does not match active session");
        } else {
            this.mActiveSession.cancelLocked(z);
            this.mActiveSession = null;
        }
    }

    public void setDisabledShowContextLocked(int i, int i2) {
        int i3 = this.mInfo.getServiceInfo().applicationInfo.uid;
        if (i != i3) {
            throw new SecurityException("Calling uid " + i + " does not match active uid " + i3);
        }
        this.mDisabledShowContext = i2;
    }

    public int getDisabledShowContextLocked(int i) {
        int i2 = this.mInfo.getServiceInfo().applicationInfo.uid;
        if (i != i2) {
            throw new SecurityException("Calling uid " + i + " does not match active uid " + i2);
        }
        return this.mDisabledShowContext;
    }

    public int getUserDisabledShowContextLocked(int i) {
        int i2 = this.mInfo.getServiceInfo().applicationInfo.uid;
        if (i != i2) {
            throw new SecurityException("Calling uid " + i + " does not match active uid " + i2);
        }
        if (this.mActiveSession != null) {
            return this.mActiveSession.getUserDisabledShowContextLocked();
        }
        return 0;
    }

    public boolean supportsLocalVoiceInteraction() {
        return this.mInfo.getSupportsLocalInteraction();
    }

    public void dumpLocked(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        if (!this.mValid) {
            printWriter.print("  NOT VALID: ");
            if (this.mInfo == null) {
                printWriter.println("no info");
                return;
            } else {
                printWriter.println(this.mInfo.getParseError());
                return;
            }
        }
        printWriter.print("  mUser=");
        printWriter.println(this.mUser);
        printWriter.print("  mComponent=");
        printWriter.println(this.mComponent.flattenToShortString());
        printWriter.print("  Session service=");
        printWriter.println(this.mInfo.getSessionService());
        printWriter.println("  Service info:");
        this.mInfo.getServiceInfo().dump(new PrintWriterPrinter(printWriter), "    ");
        printWriter.print("  Recognition service=");
        printWriter.println(this.mInfo.getRecognitionService());
        printWriter.print("  Settings activity=");
        printWriter.println(this.mInfo.getSettingsActivity());
        printWriter.print("  Supports assist=");
        printWriter.println(this.mInfo.getSupportsAssist());
        printWriter.print("  Supports launch from keyguard=");
        printWriter.println(this.mInfo.getSupportsLaunchFromKeyguard());
        if (this.mDisabledShowContext != 0) {
            printWriter.print("  mDisabledShowContext=");
            printWriter.println(Integer.toHexString(this.mDisabledShowContext));
        }
        printWriter.print("  mBound=");
        printWriter.print(this.mBound);
        printWriter.print(" mService=");
        printWriter.println(this.mService);
        if (this.mActiveSession != null) {
            printWriter.println("  Active session:");
            this.mActiveSession.dump("    ", printWriter);
        }
    }

    void startLocked() {
        Intent intent = new Intent("android.service.voice.VoiceInteractionService");
        intent.setComponent(this.mComponent);
        this.mBound = this.mContext.bindServiceAsUser(intent, this.mConnection, 67108865, new UserHandle(this.mUser));
        if (!this.mBound) {
            Slog.w(TAG, "Failed binding to voice interaction service " + this.mComponent);
        }
    }

    public void launchVoiceAssistFromKeyguard() {
        if (this.mService == null) {
            Slog.w(TAG, "Not bound to voice interaction service " + this.mComponent);
            return;
        }
        try {
            this.mService.launchVoiceAssistFromKeyguard();
        } catch (RemoteException e) {
            Slog.w(TAG, "RemoteException while calling launchVoiceAssistFromKeyguard", e);
        }
    }

    void shutdownLocked() {
        if (this.mActiveSession != null) {
            this.mActiveSession.cancelLocked(false);
            this.mActiveSession = null;
        }
        try {
            if (this.mService != null) {
                this.mService.shutdown();
            }
        } catch (RemoteException e) {
            Slog.w(TAG, "RemoteException in shutdown", e);
        }
        if (this.mBound) {
            this.mContext.unbindService(this.mConnection);
            this.mBound = false;
        }
        if (this.mValid) {
            this.mContext.unregisterReceiver(this.mBroadcastReceiver);
        }
    }

    void notifySoundModelsChangedLocked() {
        if (this.mService == null) {
            Slog.w(TAG, "Not bound to voice interaction service " + this.mComponent);
            return;
        }
        try {
            this.mService.soundModelsChanged();
        } catch (RemoteException e) {
            Slog.w(TAG, "RemoteException while calling soundModelsChanged", e);
        }
    }

    @Override
    public void sessionConnectionGone(VoiceInteractionSessionConnection voiceInteractionSessionConnection) {
        synchronized (this.mServiceStub) {
            finishLocked(voiceInteractionSessionConnection.mToken, false);
        }
    }

    @Override
    public void onSessionShown(VoiceInteractionSessionConnection voiceInteractionSessionConnection) {
        this.mServiceStub.onSessionShown();
    }

    @Override
    public void onSessionHidden(VoiceInteractionSessionConnection voiceInteractionSessionConnection) {
        this.mServiceStub.onSessionHidden();
    }
}
