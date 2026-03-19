package com.android.server.voiceinteraction;

import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.IActivityManager;
import android.app.assist.AssistContent;
import android.app.assist.AssistStructure;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.voice.IVoiceInteractionSession;
import android.service.voice.IVoiceInteractionSessionService;
import android.util.Slog;
import android.view.IWindowManager;
import com.android.internal.app.AssistUtils;
import com.android.internal.app.IVoiceInteractionSessionShowCallback;
import com.android.internal.app.IVoiceInteractor;
import com.android.server.LocalServices;
import com.android.server.am.AssistDataRequester;
import com.android.server.statusbar.StatusBarManagerInternal;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

final class VoiceInteractionSessionConnection implements ServiceConnection, AssistDataRequester.AssistDataRequesterCallbacks {
    static final String TAG = "VoiceInteractionServiceManager";
    final AppOpsManager mAppOps;
    AssistDataRequester mAssistDataRequester;
    final Intent mBindIntent;
    boolean mBound;
    final Callback mCallback;
    final int mCallingUid;
    boolean mCanceled;
    final Context mContext;
    boolean mFullyBound;
    final Handler mHandler;
    IVoiceInteractor mInteractor;
    final Object mLock;
    final IBinder mPermissionOwner;
    IVoiceInteractionSessionService mService;
    IVoiceInteractionSession mSession;
    final ComponentName mSessionComponentName;
    Bundle mShowArgs;
    int mShowFlags;
    boolean mShown;
    final int mUser;
    final IBinder mToken = new Binder();
    ArrayList<IVoiceInteractionSessionShowCallback> mPendingShowCallbacks = new ArrayList<>();
    IVoiceInteractionSessionShowCallback mShowCallback = new IVoiceInteractionSessionShowCallback.Stub() {
        public void onFailed() throws RemoteException {
            synchronized (VoiceInteractionSessionConnection.this.mLock) {
                VoiceInteractionSessionConnection.this.notifyPendingShowCallbacksFailedLocked();
            }
        }

        public void onShown() throws RemoteException {
            synchronized (VoiceInteractionSessionConnection.this.mLock) {
                VoiceInteractionSessionConnection.this.notifyPendingShowCallbacksShownLocked();
            }
        }
    };
    final ServiceConnection mFullConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
        }
    };
    private Runnable mShowAssistDisclosureRunnable = new Runnable() {
        @Override
        public void run() {
            StatusBarManagerInternal statusBarManagerInternal = (StatusBarManagerInternal) LocalServices.getService(StatusBarManagerInternal.class);
            if (statusBarManagerInternal != null) {
                statusBarManagerInternal.showAssistDisclosure();
            }
        }
    };
    final IActivityManager mAm = ActivityManager.getService();
    final IWindowManager mIWindowManager = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));

    public interface Callback {
        void onSessionHidden(VoiceInteractionSessionConnection voiceInteractionSessionConnection);

        void onSessionShown(VoiceInteractionSessionConnection voiceInteractionSessionConnection);

        void sessionConnectionGone(VoiceInteractionSessionConnection voiceInteractionSessionConnection);
    }

    public VoiceInteractionSessionConnection(Object obj, ComponentName componentName, int i, Context context, Callback callback, int i2, Handler handler) {
        IBinder iBinderNewUriPermissionOwner;
        this.mLock = obj;
        this.mSessionComponentName = componentName;
        this.mUser = i;
        this.mContext = context;
        this.mCallback = callback;
        this.mCallingUid = i2;
        this.mHandler = handler;
        this.mAppOps = (AppOpsManager) context.getSystemService(AppOpsManager.class);
        this.mAssistDataRequester = new AssistDataRequester(this.mContext, this.mAm, this.mIWindowManager, (AppOpsManager) this.mContext.getSystemService("appops"), this, this.mLock, 49, 50);
        try {
            iBinderNewUriPermissionOwner = this.mAm.newUriPermissionOwner("voicesession:" + componentName.flattenToShortString());
        } catch (RemoteException e) {
            Slog.w("voicesession", "AM dead", e);
            iBinderNewUriPermissionOwner = null;
        }
        this.mPermissionOwner = iBinderNewUriPermissionOwner;
        this.mBindIntent = new Intent("android.service.voice.VoiceInteractionService");
        this.mBindIntent.setComponent(this.mSessionComponentName);
        this.mBound = this.mContext.bindServiceAsUser(this.mBindIntent, this, 49, new UserHandle(this.mUser));
        if (this.mBound) {
            try {
                this.mIWindowManager.addWindowToken(this.mToken, 2031, 0);
                return;
            } catch (RemoteException e2) {
                Slog.w(TAG, "Failed adding window token", e2);
                return;
            }
        }
        Slog.w(TAG, "Failed binding to voice interaction session service " + this.mSessionComponentName);
    }

    public int getUserDisabledShowContextLocked() {
        int i = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "assist_structure_enabled", 1, this.mUser) == 0 ? 1 : 0;
        if (Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "assist_screenshot_enabled", 1, this.mUser) == 0) {
            return i | 2;
        }
        return i;
    }

    public boolean showLocked(Bundle bundle, int i, int i2, IVoiceInteractionSessionShowCallback iVoiceInteractionSessionShowCallback, List<IBinder> list) {
        if (this.mBound) {
            if (!this.mFullyBound) {
                this.mFullyBound = this.mContext.bindServiceAsUser(this.mBindIntent, this.mFullConnection, 201326593, new UserHandle(this.mUser));
            }
            this.mShown = true;
            this.mShowArgs = bundle;
            this.mShowFlags = i;
            int userDisabledShowContextLocked = getUserDisabledShowContextLocked() | i2;
            this.mAssistDataRequester.requestAssistData(list, (i & 1) != 0, (i & 2) != 0, (userDisabledShowContextLocked & 1) == 0, (userDisabledShowContextLocked & 2) == 0, this.mCallingUid, this.mSessionComponentName.getPackageName());
            if ((this.mAssistDataRequester.getPendingDataCount() > 0 || this.mAssistDataRequester.getPendingScreenshotCount() > 0) && AssistUtils.shouldDisclose(this.mContext, this.mSessionComponentName)) {
                this.mHandler.post(this.mShowAssistDisclosureRunnable);
            }
            if (this.mSession != null) {
                try {
                    this.mSession.show(this.mShowArgs, this.mShowFlags, iVoiceInteractionSessionShowCallback);
                    this.mShowArgs = null;
                    this.mShowFlags = 0;
                } catch (RemoteException e) {
                }
                this.mAssistDataRequester.processPendingAssistData();
            } else if (iVoiceInteractionSessionShowCallback != null) {
                this.mPendingShowCallbacks.add(iVoiceInteractionSessionShowCallback);
            }
            this.mCallback.onSessionShown(this);
            return true;
        }
        if (iVoiceInteractionSessionShowCallback != null) {
            try {
                iVoiceInteractionSessionShowCallback.onFailed();
            } catch (RemoteException e2) {
            }
        }
        return false;
    }

    @Override
    public boolean canHandleReceivedAssistDataLocked() {
        return this.mSession != null;
    }

    @Override
    public void onAssistDataReceivedLocked(Bundle bundle, int i, int i2) {
        ClipData clipData;
        if (this.mSession == null) {
            return;
        }
        if (bundle == null) {
            try {
                this.mSession.handleAssist((Bundle) null, (AssistStructure) null, (AssistContent) null, 0, 0);
                return;
            } catch (RemoteException e) {
                return;
            }
        }
        Bundle bundle2 = bundle.getBundle("data");
        AssistStructure assistStructure = (AssistStructure) bundle.getParcelable("structure");
        AssistContent assistContent = (AssistContent) bundle.getParcelable("content");
        int i3 = bundle.getInt("android.intent.extra.ASSIST_UID", -1);
        if (i3 >= 0 && assistContent != null) {
            Intent intent = assistContent.getIntent();
            if (intent != null && (clipData = intent.getClipData()) != null && Intent.isAccessUriMode(intent.getFlags())) {
                grantClipDataPermissions(clipData, intent.getFlags(), i3, this.mCallingUid, this.mSessionComponentName.getPackageName());
            }
            ClipData clipData2 = assistContent.getClipData();
            if (clipData2 != null) {
                grantClipDataPermissions(clipData2, 1, i3, this.mCallingUid, this.mSessionComponentName.getPackageName());
            }
        }
        try {
            this.mSession.handleAssist(bundle2, assistStructure, assistContent, i, i2);
        } catch (RemoteException e2) {
        }
    }

    @Override
    public void onAssistScreenshotReceivedLocked(Bitmap bitmap) {
        if (this.mSession == null) {
            return;
        }
        try {
            this.mSession.handleScreenshot(bitmap);
        } catch (RemoteException e) {
        }
    }

    void grantUriPermission(Uri uri, int i, int i2, int i3, String str) {
        if (!"content".equals(uri.getScheme())) {
            return;
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            try {
                this.mAm.checkGrantUriPermission(i2, (String) null, ContentProvider.getUriWithoutUserId(uri), i, ContentProvider.getUserIdFromUri(uri, UserHandle.getUserId(i2)));
                int userIdFromUri = ContentProvider.getUserIdFromUri(uri, this.mUser);
                this.mAm.grantUriPermissionFromOwner(this.mPermissionOwner, i2, str, ContentProvider.getUriWithoutUserId(uri), 1, userIdFromUri, this.mUser);
            } catch (RemoteException e) {
            } catch (SecurityException e2) {
                Slog.w(TAG, "Can't propagate permission", e2);
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    void grantClipDataItemPermission(ClipData.Item item, int i, int i2, int i3, String str) {
        if (item.getUri() != null) {
            grantUriPermission(item.getUri(), i, i2, i3, str);
        }
        Intent intent = item.getIntent();
        if (intent != null && intent.getData() != null) {
            grantUriPermission(intent.getData(), i, i2, i3, str);
        }
    }

    void grantClipDataPermissions(ClipData clipData, int i, int i2, int i3, String str) {
        int itemCount = clipData.getItemCount();
        for (int i4 = 0; i4 < itemCount; i4++) {
            grantClipDataItemPermission(clipData.getItemAt(i4), i, i2, i3, str);
        }
    }

    public boolean hideLocked() {
        if (!this.mBound) {
            return false;
        }
        if (this.mShown) {
            this.mShown = false;
            this.mShowArgs = null;
            this.mShowFlags = 0;
            this.mAssistDataRequester.cancel();
            this.mPendingShowCallbacks.clear();
            if (this.mSession != null) {
                try {
                    this.mSession.hide();
                } catch (RemoteException e) {
                }
            }
            try {
                this.mAm.revokeUriPermissionFromOwner(this.mPermissionOwner, (Uri) null, 3, this.mUser);
            } catch (RemoteException e2) {
            }
            if (this.mSession != null) {
                try {
                    this.mAm.finishVoiceTask(this.mSession);
                } catch (RemoteException e3) {
                }
            }
            this.mCallback.onSessionHidden(this);
        }
        if (this.mFullyBound) {
            this.mContext.unbindService(this.mFullConnection);
            this.mFullyBound = false;
            return true;
        }
        return true;
    }

    public void cancelLocked(boolean z) {
        hideLocked();
        this.mCanceled = true;
        if (this.mBound) {
            if (this.mSession != null) {
                try {
                    this.mSession.destroy();
                } catch (RemoteException e) {
                    Slog.w(TAG, "Voice interation session already dead");
                }
            }
            if (z && this.mSession != null) {
                try {
                    this.mAm.finishVoiceTask(this.mSession);
                } catch (RemoteException e2) {
                }
            }
            this.mContext.unbindService(this);
            try {
                this.mIWindowManager.removeWindowToken(this.mToken, 0);
            } catch (RemoteException e3) {
                Slog.w(TAG, "Failed removing window token", e3);
            }
            this.mBound = false;
            this.mService = null;
            this.mSession = null;
            this.mInteractor = null;
        }
        if (this.mFullyBound) {
            this.mContext.unbindService(this.mFullConnection);
            this.mFullyBound = false;
        }
    }

    public boolean deliverNewSessionLocked(IVoiceInteractionSession iVoiceInteractionSession, IVoiceInteractor iVoiceInteractor) {
        this.mSession = iVoiceInteractionSession;
        this.mInteractor = iVoiceInteractor;
        if (this.mShown) {
            try {
                iVoiceInteractionSession.show(this.mShowArgs, this.mShowFlags, this.mShowCallback);
                this.mShowArgs = null;
                this.mShowFlags = 0;
            } catch (RemoteException e) {
            }
            this.mAssistDataRequester.processPendingAssistData();
            return true;
        }
        return true;
    }

    private void notifyPendingShowCallbacksShownLocked() {
        for (int i = 0; i < this.mPendingShowCallbacks.size(); i++) {
            try {
                this.mPendingShowCallbacks.get(i).onShown();
            } catch (RemoteException e) {
            }
        }
        this.mPendingShowCallbacks.clear();
    }

    private void notifyPendingShowCallbacksFailedLocked() {
        for (int i = 0; i < this.mPendingShowCallbacks.size(); i++) {
            try {
                this.mPendingShowCallbacks.get(i).onFailed();
            } catch (RemoteException e) {
            }
        }
        this.mPendingShowCallbacks.clear();
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        synchronized (this.mLock) {
            this.mService = IVoiceInteractionSessionService.Stub.asInterface(iBinder);
            if (!this.mCanceled) {
                try {
                    this.mService.newSession(this.mToken, this.mShowArgs, this.mShowFlags);
                } catch (RemoteException e) {
                    Slog.w(TAG, "Failed adding window token", e);
                }
            }
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        this.mCallback.sessionConnectionGone(this);
        synchronized (this.mLock) {
            this.mService = null;
        }
    }

    public void dump(String str, PrintWriter printWriter) {
        printWriter.print(str);
        printWriter.print("mToken=");
        printWriter.println(this.mToken);
        printWriter.print(str);
        printWriter.print("mShown=");
        printWriter.println(this.mShown);
        printWriter.print(str);
        printWriter.print("mShowArgs=");
        printWriter.println(this.mShowArgs);
        printWriter.print(str);
        printWriter.print("mShowFlags=0x");
        printWriter.println(Integer.toHexString(this.mShowFlags));
        printWriter.print(str);
        printWriter.print("mBound=");
        printWriter.println(this.mBound);
        if (this.mBound) {
            printWriter.print(str);
            printWriter.print("mService=");
            printWriter.println(this.mService);
            printWriter.print(str);
            printWriter.print("mSession=");
            printWriter.println(this.mSession);
            printWriter.print(str);
            printWriter.print("mInteractor=");
            printWriter.println(this.mInteractor);
        }
        this.mAssistDataRequester.dump(str, printWriter);
    }
}
