package android.app;

import android.annotation.SystemApi;
import android.app.IInstantAppResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.InstantAppResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.IRemoteCallback;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.util.Slog;
import com.android.internal.os.SomeArgs;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@SystemApi
public abstract class InstantAppResolverService extends Service {
    private static final boolean DEBUG_INSTANT = Build.IS_DEBUGGABLE;
    public static final String EXTRA_RESOLVE_INFO = "android.app.extra.RESOLVE_INFO";
    public static final String EXTRA_SEQUENCE = "android.app.extra.SEQUENCE";
    private static final String TAG = "PackageManager";
    Handler mHandler;

    @Deprecated
    public void onGetInstantAppResolveInfo(int[] iArr, String str, InstantAppResolutionCallback instantAppResolutionCallback) {
        throw new IllegalStateException("Must define onGetInstantAppResolveInfo");
    }

    @Deprecated
    public void onGetInstantAppIntentFilter(int[] iArr, String str, InstantAppResolutionCallback instantAppResolutionCallback) {
        throw new IllegalStateException("Must define onGetInstantAppIntentFilter");
    }

    public void onGetInstantAppResolveInfo(Intent intent, int[] iArr, String str, InstantAppResolutionCallback instantAppResolutionCallback) {
        if (intent.isWebIntent()) {
            onGetInstantAppResolveInfo(iArr, str, instantAppResolutionCallback);
        } else {
            instantAppResolutionCallback.onInstantAppResolveInfo(Collections.emptyList());
        }
    }

    public void onGetInstantAppIntentFilter(Intent intent, int[] iArr, String str, InstantAppResolutionCallback instantAppResolutionCallback) {
        Log.e(TAG, "New onGetInstantAppIntentFilter is not overridden");
        if (intent.isWebIntent()) {
            onGetInstantAppIntentFilter(iArr, str, instantAppResolutionCallback);
        } else {
            instantAppResolutionCallback.onInstantAppResolveInfo(Collections.emptyList());
        }
    }

    Looper getLooper() {
        return getBaseContext().getMainLooper();
    }

    @Override
    public final void attachBaseContext(Context context) {
        super.attachBaseContext(context);
        this.mHandler = new ServiceHandler(getLooper());
    }

    @Override
    public final IBinder onBind(Intent intent) {
        return new IInstantAppResolver.Stub() {
            @Override
            public void getInstantAppResolveInfoList(Intent intent2, int[] iArr, String str, int i, IRemoteCallback iRemoteCallback) {
                if (InstantAppResolverService.DEBUG_INSTANT) {
                    Slog.v(InstantAppResolverService.TAG, "[" + str + "] Phase1 called; posting");
                }
                SomeArgs someArgsObtain = SomeArgs.obtain();
                someArgsObtain.arg1 = iRemoteCallback;
                someArgsObtain.arg2 = iArr;
                someArgsObtain.arg3 = str;
                someArgsObtain.arg4 = intent2;
                InstantAppResolverService.this.mHandler.obtainMessage(1, i, 0, someArgsObtain).sendToTarget();
            }

            @Override
            public void getInstantAppIntentFilterList(Intent intent2, int[] iArr, String str, IRemoteCallback iRemoteCallback) {
                if (InstantAppResolverService.DEBUG_INSTANT) {
                    Slog.v(InstantAppResolverService.TAG, "[" + str + "] Phase2 called; posting");
                }
                SomeArgs someArgsObtain = SomeArgs.obtain();
                someArgsObtain.arg1 = iRemoteCallback;
                someArgsObtain.arg2 = iArr;
                someArgsObtain.arg3 = str;
                someArgsObtain.arg4 = intent2;
                InstantAppResolverService.this.mHandler.obtainMessage(2, iRemoteCallback).sendToTarget();
            }
        };
    }

    public static final class InstantAppResolutionCallback {
        private final IRemoteCallback mCallback;
        private final int mSequence;

        InstantAppResolutionCallback(int i, IRemoteCallback iRemoteCallback) {
            this.mCallback = iRemoteCallback;
            this.mSequence = i;
        }

        public void onInstantAppResolveInfo(List<InstantAppResolveInfo> list) {
            Bundle bundle = new Bundle();
            bundle.putParcelableList(InstantAppResolverService.EXTRA_RESOLVE_INFO, list);
            bundle.putInt(InstantAppResolverService.EXTRA_SEQUENCE, this.mSequence);
            try {
                this.mCallback.sendResult(bundle);
            } catch (RemoteException e) {
            }
        }
    }

    private final class ServiceHandler extends Handler {
        public static final int MSG_GET_INSTANT_APP_INTENT_FILTER = 2;
        public static final int MSG_GET_INSTANT_APP_RESOLVE_INFO = 1;

        public ServiceHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override
        public void handleMessage(Message message) {
            int i = message.what;
            switch (i) {
                case 1:
                    SomeArgs someArgs = (SomeArgs) message.obj;
                    IRemoteCallback iRemoteCallback = (IRemoteCallback) someArgs.arg1;
                    int[] iArr = (int[]) someArgs.arg2;
                    String str = (String) someArgs.arg3;
                    Intent intent = (Intent) someArgs.arg4;
                    int i2 = message.arg1;
                    if (InstantAppResolverService.DEBUG_INSTANT) {
                        Slog.d(InstantAppResolverService.TAG, "[" + str + "] Phase1 request; prefix: " + Arrays.toString(iArr));
                    }
                    InstantAppResolverService.this.onGetInstantAppResolveInfo(intent, iArr, str, new InstantAppResolutionCallback(i2, iRemoteCallback));
                    return;
                case 2:
                    SomeArgs someArgs2 = (SomeArgs) message.obj;
                    IRemoteCallback iRemoteCallback2 = (IRemoteCallback) someArgs2.arg1;
                    int[] iArr2 = (int[]) someArgs2.arg2;
                    String str2 = (String) someArgs2.arg3;
                    Intent intent2 = (Intent) someArgs2.arg4;
                    if (InstantAppResolverService.DEBUG_INSTANT) {
                        Slog.d(InstantAppResolverService.TAG, "[" + str2 + "] Phase2 request; prefix: " + Arrays.toString(iArr2));
                    }
                    InstantAppResolverService.this.onGetInstantAppIntentFilter(intent2, iArr2, str2, new InstantAppResolutionCallback(-1, iRemoteCallback2));
                    return;
                default:
                    throw new IllegalArgumentException("Unknown message: " + i);
            }
        }
    }
}
