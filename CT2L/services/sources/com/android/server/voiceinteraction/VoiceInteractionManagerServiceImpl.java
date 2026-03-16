package com.android.server.voiceinteraction;

import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.app.ProfilerInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.service.voice.IVoiceInteractionService;
import android.service.voice.IVoiceInteractionSession;
import android.service.voice.IVoiceInteractionSessionService;
import android.service.voice.VoiceInteractionServiceInfo;
import android.util.Slog;
import android.view.IWindowManager;
import com.android.internal.app.IVoiceInteractor;
import java.io.FileDescriptor;
import java.io.PrintWriter;

class VoiceInteractionManagerServiceImpl {
    static final String TAG = "VoiceInteractionServiceManager";
    SessionConnection mActiveSession;
    final ComponentName mComponent;
    final Context mContext;
    final Handler mHandler;
    final IWindowManager mIWindowManager;
    final VoiceInteractionServiceInfo mInfo;
    final Object mLock;
    IVoiceInteractionService mService;
    final ComponentName mSessionComponentName;
    final int mUser;
    final boolean mValid;
    boolean mBound = false;
    final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(intent.getAction())) {
                synchronized (VoiceInteractionManagerServiceImpl.this.mLock) {
                    if (VoiceInteractionManagerServiceImpl.this.mActiveSession != null && VoiceInteractionManagerServiceImpl.this.mActiveSession.mSession != null) {
                        try {
                            VoiceInteractionManagerServiceImpl.this.mActiveSession.mSession.closeSystemDialogs();
                        } catch (RemoteException e) {
                        }
                    }
                }
            }
        }
    };
    final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            synchronized (VoiceInteractionManagerServiceImpl.this.mLock) {
                VoiceInteractionManagerServiceImpl.this.mService = IVoiceInteractionService.Stub.asInterface(service);
                try {
                    VoiceInteractionManagerServiceImpl.this.mService.ready();
                } catch (RemoteException e) {
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            VoiceInteractionManagerServiceImpl.this.mService = null;
        }
    };
    final IActivityManager mAm = ActivityManagerNative.getDefault();

    final class SessionConnection implements ServiceConnection {
        final Bundle mArgs;
        boolean mBound;
        IVoiceInteractor mInteractor;
        IVoiceInteractionSessionService mService;
        IVoiceInteractionSession mSession;
        final IBinder mToken = new Binder();

        SessionConnection(Bundle args) {
            this.mArgs = args;
            Intent serviceIntent = new Intent("android.service.voice.VoiceInteractionService");
            serviceIntent.setComponent(VoiceInteractionManagerServiceImpl.this.mSessionComponentName);
            this.mBound = VoiceInteractionManagerServiceImpl.this.mContext.bindServiceAsUser(serviceIntent, this, 1, new UserHandle(VoiceInteractionManagerServiceImpl.this.mUser));
            if (this.mBound) {
                try {
                    VoiceInteractionManagerServiceImpl.this.mIWindowManager.addWindowToken(this.mToken, 2031);
                    return;
                } catch (RemoteException e) {
                    Slog.w(VoiceInteractionManagerServiceImpl.TAG, "Failed adding window token", e);
                    return;
                }
            }
            Slog.w(VoiceInteractionManagerServiceImpl.TAG, "Failed binding to voice interaction session service " + VoiceInteractionManagerServiceImpl.this.mComponent);
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            synchronized (VoiceInteractionManagerServiceImpl.this.mLock) {
                this.mService = IVoiceInteractionSessionService.Stub.asInterface(service);
                if (VoiceInteractionManagerServiceImpl.this.mActiveSession == this) {
                    try {
                        this.mService.newSession(this.mToken, this.mArgs);
                    } catch (RemoteException e) {
                        Slog.w(VoiceInteractionManagerServiceImpl.TAG, "Failed adding window token", e);
                    }
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            this.mService = null;
        }

        public void cancel() {
            if (this.mBound) {
                if (this.mSession != null) {
                    try {
                        this.mSession.destroy();
                    } catch (RemoteException e) {
                        Slog.w(VoiceInteractionManagerServiceImpl.TAG, "Voice interation session already dead");
                    }
                }
                if (this.mSession != null) {
                    try {
                        VoiceInteractionManagerServiceImpl.this.mAm.finishVoiceTask(this.mSession);
                    } catch (RemoteException e2) {
                    }
                }
                VoiceInteractionManagerServiceImpl.this.mContext.unbindService(this);
                try {
                    VoiceInteractionManagerServiceImpl.this.mIWindowManager.removeWindowToken(this.mToken);
                } catch (RemoteException e3) {
                    Slog.w(VoiceInteractionManagerServiceImpl.TAG, "Failed removing window token", e3);
                }
                this.mBound = false;
                this.mService = null;
                this.mSession = null;
                this.mInteractor = null;
            }
        }

        public void dump(String prefix, PrintWriter pw) {
            pw.print(prefix);
            pw.print("mToken=");
            pw.println(this.mToken);
            pw.print(prefix);
            pw.print("mArgs=");
            pw.println(this.mArgs);
            pw.print(prefix);
            pw.print("mBound=");
            pw.println(this.mBound);
            if (this.mBound) {
                pw.print(prefix);
                pw.print("mService=");
                pw.println(this.mService);
                pw.print(prefix);
                pw.print("mSession=");
                pw.println(this.mSession);
                pw.print(prefix);
                pw.print("mInteractor=");
                pw.println(this.mInteractor);
            }
        }
    }

    VoiceInteractionManagerServiceImpl(Context context, Handler handler, Object lock, int userHandle, ComponentName service) {
        this.mContext = context;
        this.mHandler = handler;
        this.mLock = lock;
        this.mUser = userHandle;
        this.mComponent = service;
        try {
            VoiceInteractionServiceInfo info = new VoiceInteractionServiceInfo(context.getPackageManager(), service);
            this.mInfo = info;
            if (this.mInfo.getParseError() != null) {
                Slog.w(TAG, "Bad voice interaction service: " + this.mInfo.getParseError());
                this.mSessionComponentName = null;
                this.mIWindowManager = null;
                this.mValid = false;
                return;
            }
            this.mValid = true;
            this.mSessionComponentName = new ComponentName(service.getPackageName(), this.mInfo.getSessionService());
            this.mIWindowManager = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.CLOSE_SYSTEM_DIALOGS");
            this.mContext.registerReceiver(this.mBroadcastReceiver, filter, null, handler);
        } catch (PackageManager.NameNotFoundException e) {
            Slog.w(TAG, "Voice interaction service not found: " + service);
            this.mInfo = null;
            this.mSessionComponentName = null;
            this.mIWindowManager = null;
            this.mValid = false;
        }
    }

    public void startSessionLocked(int callingPid, int callingUid, Bundle args) {
        if (this.mActiveSession != null) {
            this.mActiveSession.cancel();
            this.mActiveSession = null;
        }
        this.mActiveSession = new SessionConnection(args);
    }

    public boolean deliverNewSessionLocked(int callingPid, int callingUid, IBinder token, IVoiceInteractionSession session, IVoiceInteractor interactor) {
        if (this.mActiveSession == null || token != this.mActiveSession.mToken) {
            Slog.w(TAG, "deliverNewSession does not match active session");
            return false;
        }
        this.mActiveSession.mSession = session;
        this.mActiveSession.mInteractor = interactor;
        return true;
    }

    public int startVoiceActivityLocked(int callingPid, int callingUid, IBinder token, Intent intent, String resolvedType) {
        try {
            if (this.mActiveSession == null || token != this.mActiveSession.mToken) {
                Slog.w(TAG, "startVoiceActivity does not match active session");
                return -6;
            }
            Intent intent2 = new Intent(intent);
            try {
                intent2.addCategory("android.intent.category.VOICE");
                intent2.addFlags(402653184);
                return this.mAm.startVoiceActivity(this.mComponent.getPackageName(), callingPid, callingUid, intent2, resolvedType, this.mActiveSession.mSession, this.mActiveSession.mInteractor, 0, (ProfilerInfo) null, (Bundle) null, this.mUser);
            } catch (RemoteException e) {
                e = e;
                throw new IllegalStateException("Unexpected remote error", e);
            }
        } catch (RemoteException e2) {
            e = e2;
        }
    }

    public void finishLocked(int callingPid, int callingUid, IBinder token) {
        if (this.mActiveSession == null || token != this.mActiveSession.mToken) {
            Slog.w(TAG, "finish does not match active session");
        } else {
            this.mActiveSession.cancel();
            this.mActiveSession = null;
        }
    }

    public void dumpLocked(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (!this.mValid) {
            pw.print("  NOT VALID: ");
            if (this.mInfo == null) {
                pw.println("no info");
                return;
            } else {
                pw.println(this.mInfo.getParseError());
                return;
            }
        }
        pw.print("  mComponent=");
        pw.println(this.mComponent.flattenToShortString());
        pw.print("  Session service=");
        pw.println(this.mInfo.getSessionService());
        pw.print("  Settings activity=");
        pw.println(this.mInfo.getSettingsActivity());
        pw.print("  mBound=");
        pw.print(this.mBound);
        pw.print(" mService=");
        pw.println(this.mService);
        if (this.mActiveSession != null) {
            pw.println("  Active session:");
            this.mActiveSession.dump("    ", pw);
        }
    }

    void startLocked() {
        Intent intent = new Intent("android.service.voice.VoiceInteractionService");
        intent.setComponent(this.mComponent);
        this.mBound = this.mContext.bindServiceAsUser(intent, this.mConnection, 1, new UserHandle(this.mUser));
        if (!this.mBound) {
            Slog.w(TAG, "Failed binding to voice interaction service " + this.mComponent);
        }
    }

    void shutdownLocked() {
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
        }
        try {
            this.mService.soundModelsChanged();
        } catch (RemoteException e) {
            Slog.w(TAG, "RemoteException while calling soundModelsChanged", e);
        }
    }
}
