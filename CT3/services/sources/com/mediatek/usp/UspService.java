package com.mediatek.usp;

import android.content.Context;
import android.util.Log;
import com.android.server.SystemService;

public class UspService extends SystemService {
    private final boolean DEBUG;
    private final String TAG;
    final UspServiceImpl mImpl;

    public UspService(Context context) {
        super(context);
        this.TAG = "UspService";
        this.DEBUG = true;
        this.mImpl = new UspServiceImpl(context);
        Log.i("UspService", "UspServiceImpl" + this.mImpl.toString());
    }

    @Override
    public void onStart() {
        Log.i("UspService", "Registering service uniservice-pack");
        publishBinderService("uniservice-pack", this.mImpl);
        this.mImpl.start();
    }

    @Override
    public void onBootPhase(int phase) {
        Log.i("UspService", "phase " + phase);
        if (phase != 1000) {
            return;
        }
        Log.i("UspService", "Boot completed: unfreezed");
        this.mImpl.unfreezeScreen();
    }
}
