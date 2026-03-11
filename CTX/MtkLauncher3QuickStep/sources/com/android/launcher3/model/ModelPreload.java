package com.android.launcher3.model;

import android.content.Context;
import android.support.annotation.WorkerThread;
import android.util.Log;
import com.android.launcher3.AllAppsList;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherModel;
import java.util.concurrent.Executor;

public class ModelPreload implements LauncherModel.ModelUpdateTask {
    private static final String TAG = "ModelPreload";
    private AllAppsList mAllAppsList;
    private LauncherAppState mApp;
    private BgDataModel mBgDataModel;
    private LauncherModel mModel;

    @Override
    public final void init(LauncherAppState launcherAppState, LauncherModel launcherModel, BgDataModel bgDataModel, AllAppsList allAppsList, Executor executor) {
        this.mApp = launcherAppState;
        this.mModel = launcherModel;
        this.mBgDataModel = bgDataModel;
        this.mAllAppsList = allAppsList;
    }

    @Override
    public final void run() {
        this.mModel.startLoaderForResultsIfNotLoaded(new LoaderResults(this.mApp, this.mBgDataModel, this.mAllAppsList, 0, null));
        Log.d(TAG, "Preload completed : " + this.mModel.isModelLoaded());
        onComplete(this.mModel.isModelLoaded());
    }

    @WorkerThread
    public void onComplete(boolean z) {
    }

    public void start(Context context) {
        LauncherAppState.getInstance(context).getModel().enqueueModelUpdateTask(this);
    }
}
