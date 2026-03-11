package com.android.systemui.doze;

import android.content.Context;
import android.os.PowerManager;
import android.os.SystemClock;
import android.service.dreams.DreamService;
import android.util.Log;
import com.android.systemui.Dependency;
import com.android.systemui.doze.DozeMachine;
import com.android.systemui.plugins.DozeServicePlugin;
import com.android.systemui.plugins.PluginListener;
import com.android.systemui.plugins.PluginManager;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class DozeService extends DreamService implements DozeMachine.Service, DozeServicePlugin.RequestDoze, PluginListener<DozeServicePlugin> {
    static final boolean DEBUG = Log.isLoggable("DozeService", 3);
    private DozeMachine mDozeMachine;
    private DozeServicePlugin mDozePlugin;

    public DozeService() {
        setDebug(DEBUG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        setWindowless(true);
        if (DozeFactory.getHost(this) == null) {
            finish();
        } else {
            ((PluginManager) Dependency.get(PluginManager.class)).addPluginListener((PluginListener) this, DozeServicePlugin.class, false);
            this.mDozeMachine = new DozeFactory().assembleMachine(this);
        }
    }

    @Override
    public void onDestroy() {
        ((PluginManager) Dependency.get(PluginManager.class)).removePluginListener(this);
        super.onDestroy();
        this.mDozeMachine = null;
    }

    @Override
    public void onPluginConnected(DozeServicePlugin dozeServicePlugin, Context context) {
        this.mDozePlugin = dozeServicePlugin;
        this.mDozePlugin.setDozeRequester(this);
    }

    @Override
    public void onPluginDisconnected(DozeServicePlugin dozeServicePlugin) {
        if (this.mDozePlugin != null) {
            this.mDozePlugin.onDreamingStopped();
            this.mDozePlugin = null;
        }
    }

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
        this.mDozeMachine.requestState(DozeMachine.State.INITIALIZED);
        startDozing();
        if (this.mDozePlugin != null) {
            this.mDozePlugin.onDreamingStarted();
        }
    }

    @Override
    public void onDreamingStopped() {
        super.onDreamingStopped();
        this.mDozeMachine.requestState(DozeMachine.State.FINISH);
        if (this.mDozePlugin != null) {
            this.mDozePlugin.onDreamingStopped();
        }
    }

    protected void dumpOnHandler(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        super.dumpOnHandler(fileDescriptor, printWriter, strArr);
        if (this.mDozeMachine != null) {
            this.mDozeMachine.dump(printWriter);
        }
    }

    @Override
    public void requestWakeUp() {
        ((PowerManager) getSystemService(PowerManager.class)).wakeUp(SystemClock.uptimeMillis(), "com.android.systemui:NODOZE");
    }

    @Override
    public void onRequestShowDoze() {
        if (this.mDozeMachine != null) {
            this.mDozeMachine.requestState(DozeMachine.State.DOZE_AOD);
        }
    }

    @Override
    public void onRequestHideDoze() {
        if (this.mDozeMachine != null) {
            this.mDozeMachine.requestState(DozeMachine.State.DOZE);
        }
    }
}
