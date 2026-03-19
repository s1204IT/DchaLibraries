package android.content.pm.permission;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.permission.IRuntimePermissionPresenter;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteCallback;
import android.os.RemoteException;
import android.permissionpresenterservice.RuntimePermissionPresenterService;
import android.util.Log;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.os.SomeArgs;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RuntimePermissionPresenter {
    public static final String KEY_RESULT = "android.content.pm.permission.RuntimePermissionPresenter.key.result";
    private static final String TAG = "RuntimePermPresenter";

    @GuardedBy("sLock")
    private static RuntimePermissionPresenter sInstance;
    private static final Object sLock = new Object();
    private final RemoteService mRemoteService;

    public static abstract class OnResultCallback {
        public void onGetAppPermissions(List<RuntimePermissionPresentationInfo> list) {
        }
    }

    public static RuntimePermissionPresenter getInstance(Context context) {
        RuntimePermissionPresenter runtimePermissionPresenter;
        synchronized (sLock) {
            if (sInstance == null) {
                sInstance = new RuntimePermissionPresenter(context.getApplicationContext());
            }
            runtimePermissionPresenter = sInstance;
        }
        return runtimePermissionPresenter;
    }

    private RuntimePermissionPresenter(Context context) {
        this.mRemoteService = new RemoteService(context);
    }

    public void getAppPermissions(String str, OnResultCallback onResultCallback, Handler handler) {
        SomeArgs someArgsObtain = SomeArgs.obtain();
        someArgsObtain.arg1 = str;
        someArgsObtain.arg2 = onResultCallback;
        someArgsObtain.arg3 = handler;
        this.mRemoteService.processMessage(this.mRemoteService.obtainMessage(1, someArgsObtain));
    }

    public void revokeRuntimePermission(String str, String str2) {
        SomeArgs someArgsObtain = SomeArgs.obtain();
        someArgsObtain.arg1 = str;
        someArgsObtain.arg2 = str2;
        this.mRemoteService.processMessage(this.mRemoteService.obtainMessage(4, someArgsObtain));
    }

    private static final class RemoteService extends Handler implements ServiceConnection {
        public static final int MSG_GET_APPS_USING_PERMISSIONS = 2;
        public static final int MSG_GET_APP_PERMISSIONS = 1;
        public static final int MSG_REVOKE_APP_PERMISSIONS = 4;
        public static final int MSG_UNBIND = 3;
        private static final long UNBIND_TIMEOUT_MILLIS = 10000;

        @GuardedBy("mLock")
        private boolean mBound;
        private final Context mContext;
        private final Object mLock;

        @GuardedBy("mLock")
        private final List<Message> mPendingWork;

        @GuardedBy("mLock")
        private IRuntimePermissionPresenter mRemoteInstance;

        public RemoteService(Context context) {
            super(context.getMainLooper(), null, false);
            this.mLock = new Object();
            this.mPendingWork = new ArrayList();
            this.mContext = context;
        }

        public void processMessage(Message message) {
            synchronized (this.mLock) {
                if (!this.mBound) {
                    Intent intent = new Intent(RuntimePermissionPresenterService.SERVICE_INTERFACE);
                    intent.setPackage(this.mContext.getPackageManager().getPermissionControllerPackageName());
                    this.mBound = this.mContext.bindService(intent, this, 1);
                }
                this.mPendingWork.add(message);
                scheduleNextMessageIfNeededLocked();
            }
        }

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            synchronized (this.mLock) {
                this.mRemoteInstance = IRuntimePermissionPresenter.Stub.asInterface(iBinder);
                scheduleNextMessageIfNeededLocked();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            synchronized (this.mLock) {
                this.mRemoteInstance = null;
            }
        }

        @Override
        public void handleMessage(Message message) {
            IRuntimePermissionPresenter iRuntimePermissionPresenter;
            IRuntimePermissionPresenter iRuntimePermissionPresenter2;
            int i = message.what;
            if (i == 1) {
                SomeArgs someArgs = (SomeArgs) message.obj;
                String str = (String) someArgs.arg1;
                final OnResultCallback onResultCallback = (OnResultCallback) someArgs.arg2;
                final Handler handler = (Handler) someArgs.arg3;
                someArgs.recycle();
                synchronized (this.mLock) {
                    iRuntimePermissionPresenter = this.mRemoteInstance;
                }
                if (iRuntimePermissionPresenter == null) {
                    return;
                }
                try {
                    iRuntimePermissionPresenter.getAppPermissions(str, new RemoteCallback(new RemoteCallback.OnResultListener() {
                        @Override
                        public void onResult(Bundle bundle) {
                            final List<RuntimePermissionPresentationInfo> listEmptyList;
                            if (bundle != null) {
                                listEmptyList = bundle.getParcelableArrayList(RuntimePermissionPresenter.KEY_RESULT);
                            } else {
                                listEmptyList = null;
                            }
                            if (listEmptyList == null) {
                                listEmptyList = Collections.emptyList();
                            }
                            if (handler != null) {
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        onResultCallback.onGetAppPermissions(listEmptyList);
                                    }
                                });
                            } else {
                                onResultCallback.onGetAppPermissions(listEmptyList);
                            }
                        }
                    }, this));
                } catch (RemoteException e) {
                    Log.e(RuntimePermissionPresenter.TAG, "Error getting app permissions", e);
                }
                scheduleUnbind();
            } else {
                switch (i) {
                    case 3:
                        synchronized (this.mLock) {
                            if (this.mBound) {
                                this.mContext.unbindService(this);
                                this.mBound = false;
                            }
                            this.mRemoteInstance = null;
                            break;
                        }
                        break;
                    case 4:
                        SomeArgs someArgs2 = (SomeArgs) message.obj;
                        String str2 = (String) someArgs2.arg1;
                        String str3 = (String) someArgs2.arg2;
                        someArgs2.recycle();
                        synchronized (this.mLock) {
                            iRuntimePermissionPresenter2 = this.mRemoteInstance;
                            break;
                        }
                        if (iRuntimePermissionPresenter2 == null) {
                            return;
                        }
                        try {
                            iRuntimePermissionPresenter2.revokeRuntimePermission(str2, str3);
                        } catch (RemoteException e2) {
                            Log.e(RuntimePermissionPresenter.TAG, "Error getting app permissions", e2);
                        }
                        break;
                        break;
                }
            }
            synchronized (this.mLock) {
                scheduleNextMessageIfNeededLocked();
            }
        }

        @GuardedBy("mLock")
        private void scheduleNextMessageIfNeededLocked() {
            if (this.mBound && this.mRemoteInstance != null && !this.mPendingWork.isEmpty()) {
                sendMessage(this.mPendingWork.remove(0));
            }
        }

        private void scheduleUnbind() {
            removeMessages(3);
            sendEmptyMessageDelayed(3, 10000L);
        }
    }
}
