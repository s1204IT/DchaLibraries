package android.permissionpresenterservice;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.permission.IRuntimePermissionPresenter;
import android.content.pm.permission.RuntimePermissionPresentationInfo;
import android.content.pm.permission.RuntimePermissionPresenter;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteCallback;
import com.android.internal.os.SomeArgs;
import java.util.List;

public abstract class RuntimePermissionPresenterService extends Service {
    public static final String SERVICE_INTERFACE = "android.permissionpresenterservice.RuntimePermissionPresenterService";
    private Handler mHandler;

    public abstract List<RuntimePermissionPresentationInfo> onGetAppPermissions(String str);

    public abstract List<ApplicationInfo> onGetAppsUsingPermissions(boolean z);

    @Override
    public final void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        this.mHandler = new MyHandler(base.getMainLooper());
    }

    @Override
    public final IBinder onBind(Intent intent) {
        return new IRuntimePermissionPresenter.Stub() {
            @Override
            public void getAppPermissions(String packageName, RemoteCallback callback) {
                SomeArgs args = SomeArgs.obtain();
                args.arg1 = packageName;
                args.arg2 = callback;
                RuntimePermissionPresenterService.this.mHandler.obtainMessage(1, args).sendToTarget();
            }

            @Override
            public void getAppsUsingPermissions(boolean system, RemoteCallback callback) {
                RuntimePermissionPresenterService.this.mHandler.obtainMessage(2, system ? 1 : 0, 0, callback).sendToTarget();
            }
        };
    }

    private final class MyHandler extends Handler {
        public static final int MSG_GET_APPS_USING_PERMISSIONS = 2;
        public static final int MSG_GET_APP_PERMISSIONS = 1;

        public MyHandler(Looper looper) {
            super(looper, null, false);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    SomeArgs args = (SomeArgs) msg.obj;
                    String packageName = (String) args.arg1;
                    RemoteCallback callback = (RemoteCallback) args.arg2;
                    args.recycle();
                    List<RuntimePermissionPresentationInfo> permissions = RuntimePermissionPresenterService.this.onGetAppPermissions(packageName);
                    if (permissions != null && !permissions.isEmpty()) {
                        Bundle result = new Bundle();
                        result.putParcelableList(RuntimePermissionPresenter.KEY_RESULT, permissions);
                        callback.sendResult(result);
                    } else {
                        callback.sendResult(null);
                    }
                    break;
                case 2:
                    RemoteCallback callback2 = (RemoteCallback) msg.obj;
                    boolean system = msg.arg1 == 1;
                    List<ApplicationInfo> apps = RuntimePermissionPresenterService.this.onGetAppsUsingPermissions(system);
                    if (apps != null && !apps.isEmpty()) {
                        Bundle result2 = new Bundle();
                        result2.putParcelableList(RuntimePermissionPresenter.KEY_RESULT, apps);
                        callback2.sendResult(result2);
                    } else {
                        callback2.sendResult(null);
                    }
                    break;
            }
        }
    }
}
