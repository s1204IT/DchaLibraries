package com.android.location.fused;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.os.WorkSource;
import com.android.location.fused.FusionEngine;
import com.android.location.provider.LocationProviderBase;
import com.android.location.provider.ProviderPropertiesUnbundled;
import com.android.location.provider.ProviderRequestUnbundled;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class FusedLocationProvider extends LocationProviderBase implements FusionEngine.Callback {
    private static ProviderPropertiesUnbundled PROPERTIES = ProviderPropertiesUnbundled.create(false, false, false, false, true, true, true, 1, 1);
    private final Context mContext;
    private final FusionEngine mEngine;
    private Handler mHandler;

    private static class RequestWrapper {
        public ProviderRequestUnbundled request;
        public WorkSource source;

        public RequestWrapper(ProviderRequestUnbundled request, WorkSource source) {
            this.request = request;
            this.source = source;
        }
    }

    public FusedLocationProvider(Context context) {
        super("FusedLocationProvider", PROPERTIES);
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        FusedLocationProvider.this.mEngine.init(FusedLocationProvider.this);
                        break;
                    case 2:
                        FusedLocationProvider.this.mEngine.deinit();
                        break;
                    case 3:
                        RequestWrapper wrapper = (RequestWrapper) msg.obj;
                        FusedLocationProvider.this.mEngine.setRequest(wrapper.request, wrapper.source);
                        break;
                }
            }
        };
        this.mContext = context;
        this.mEngine = new FusionEngine(context, Looper.myLooper());
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.USER_SWITCHED");
        this.mContext.registerReceiverAsUser(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                if ("android.intent.action.USER_SWITCHED".equals(action)) {
                    FusedLocationProvider.this.mEngine.switchUser();
                }
            }
        }, UserHandle.ALL, intentFilter, null, this.mHandler);
    }

    public void onEnable() {
        this.mHandler.sendEmptyMessage(1);
    }

    public void onDisable() {
        this.mHandler.sendEmptyMessage(2);
    }

    public void onSetRequest(ProviderRequestUnbundled request, WorkSource source) {
        this.mHandler.obtainMessage(3, new RequestWrapper(request, source)).sendToTarget();
    }

    public void onDump(FileDescriptor fd, PrintWriter pw, String[] args) {
        this.mEngine.dump(fd, pw, args);
    }

    public int onGetStatus(Bundle extras) {
        return 2;
    }

    public long onGetStatusUpdateTime() {
        return 0L;
    }
}
