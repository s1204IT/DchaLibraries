package com.android.server;

import android.content.Context;
import android.util.Log;
import com.android.internal.util.ConcurrentUtils;
import com.android.server.location.ContextHubService;
import java.util.concurrent.Future;

class ContextHubSystemService extends SystemService {
    private static final String TAG = "ContextHubSystemService";
    private ContextHubService mContextHubService;
    private Future<?> mInit;

    public ContextHubSystemService(final Context context) {
        super(context);
        this.mInit = SystemServerInitThreadPool.get().submit(new Runnable() {
            @Override
            public final void run() {
                this.f$0.mContextHubService = new ContextHubService(context);
            }
        }, "Init ContextHubSystemService");
    }

    @Override
    public void onStart() {
    }

    @Override
    public void onBootPhase(int i) {
        if (i == 500) {
            Log.d(TAG, "onBootPhase: PHASE_SYSTEM_SERVICES_READY");
            ConcurrentUtils.waitForFutureNoInterrupt(this.mInit, "Wait for ContextHubSystemService init");
            this.mInit = null;
            publishBinderService("contexthub", this.mContextHubService);
        }
    }
}
