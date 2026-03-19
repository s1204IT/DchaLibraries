package android.content.pm.permission;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
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
        public void onGetAppPermissions(List<RuntimePermissionPresentationInfo> permissions) {
        }

        public void getAppsUsingPermissions(boolean system, List<ApplicationInfo> apps) {
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

    public void getAppPermissions(String packageName, OnResultCallback callback, Handler handler) {
        SomeArgs args = SomeArgs.obtain();
        args.arg1 = packageName;
        args.arg2 = callback;
        args.arg3 = handler;
        Message message = this.mRemoteService.obtainMessage(1, args);
        this.mRemoteService.processMessage(message);
    }

    public void getAppsUsingPermissions(boolean system, OnResultCallback callback, Handler handler) {
        SomeArgs args = SomeArgs.obtain();
        args.arg1 = callback;
        args.arg2 = handler;
        args.argi1 = system ? 1 : 0;
        Message message = this.mRemoteService.obtainMessage(2, args);
        this.mRemoteService.processMessage(message);
    }

    private static final class RemoteService extends Handler implements ServiceConnection {
        public static final int MSG_GET_APPS_USING_PERMISSIONS = 2;
        public static final int MSG_GET_APP_PERMISSIONS = 1;
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
        public void onServiceConnected(ComponentName name, IBinder service) {
            synchronized (this.mLock) {
                this.mRemoteInstance = IRuntimePermissionPresenter.Stub.asInterface(service);
                scheduleNextMessageIfNeededLocked();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            synchronized (this.mLock) {
                this.mRemoteInstance = null;
            }
        }

        @Override
        public void handleMessage(Message msg) {
            IRuntimePermissionPresenter remoteInstance;
            IRuntimePermissionPresenter remoteInstance2;
            switch (msg.what) {
                case 1:
                    SomeArgs args = (SomeArgs) msg.obj;
                    String packageName = (String) args.arg1;
                    final OnResultCallback callback = (OnResultCallback) args.arg2;
                    final Handler handler = (Handler) args.arg3;
                    args.recycle();
                    synchronized (this.mLock) {
                        remoteInstance2 = this.mRemoteInstance;
                    }
                    if (remoteInstance2 == null) {
                        return;
                    }
                    try {
                        remoteInstance2.getAppPermissions(packageName, new RemoteCallback(new RemoteCallback.OnResultListener() {
                            @Override
                            public void onResult(Bundle result) {
                                List<RuntimePermissionPresentationInfo> permissions = null;
                                if (result != null) {
                                    permissions = result.getParcelableArrayList(RuntimePermissionPresenter.KEY_RESULT);
                                }
                                if (permissions == null) {
                                    permissions = Collections.emptyList();
                                }
                                final List<RuntimePermissionPresentationInfo> reportedPermissions = permissions;
                                if (handler != null) {
                                    Handler handler2 = handler;
                                    final OnResultCallback onResultCallback = callback;
                                    handler2.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            onResultCallback.onGetAppPermissions(reportedPermissions);
                                        }
                                    });
                                    return;
                                }
                                callback.onGetAppPermissions(reportedPermissions);
                            }
                        }, this));
                    } catch (RemoteException re) {
                        Log.e(RuntimePermissionPresenter.TAG, "Error getting app permissions", re);
                    }
                    scheduleUnbind();
                    break;
                    break;
                case 2:
                    SomeArgs args2 = (SomeArgs) msg.obj;
                    final OnResultCallback callback2 = (OnResultCallback) args2.arg1;
                    final Handler handler2 = (Handler) args2.arg2;
                    final boolean system = args2.argi1 == 1;
                    args2.recycle();
                    synchronized (this.mLock) {
                        remoteInstance = this.mRemoteInstance;
                    }
                    if (remoteInstance == null) {
                        return;
                    }
                    try {
                        remoteInstance.getAppsUsingPermissions(system, new RemoteCallback(new RemoteCallback.OnResultListener() {
                            @Override
                            public void onResult(Bundle result) {
                                List<ApplicationInfo> apps = null;
                                if (result != null) {
                                    apps = result.getParcelableArrayList(RuntimePermissionPresenter.KEY_RESULT);
                                }
                                if (apps == null) {
                                    apps = Collections.emptyList();
                                }
                                final List<ApplicationInfo> reportedApps = apps;
                                if (handler2 != null) {
                                    Handler handler3 = handler2;
                                    final OnResultCallback onResultCallback = callback2;
                                    final boolean z = system;
                                    handler3.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            onResultCallback.getAppsUsingPermissions(z, reportedApps);
                                        }
                                    });
                                    return;
                                }
                                callback2.getAppsUsingPermissions(system, reportedApps);
                            }
                        }, this));
                    } catch (RemoteException re2) {
                        Log.e(RuntimePermissionPresenter.TAG, "Error getting apps using permissions", re2);
                    }
                    scheduleUnbind();
                    break;
                    break;
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
            }
            synchronized (this.mLock) {
                scheduleNextMessageIfNeededLocked();
            }
        }

        private void scheduleNextMessageIfNeededLocked() {
            if (!this.mBound || this.mRemoteInstance == null || this.mPendingWork.isEmpty()) {
                return;
            }
            Message nextMessage = this.mPendingWork.remove(0);
            sendMessage(nextMessage);
        }

        private void scheduleUnbind() {
            removeMessages(3);
            sendEmptyMessageDelayed(3, UNBIND_TIMEOUT_MILLIS);
        }
    }
}
