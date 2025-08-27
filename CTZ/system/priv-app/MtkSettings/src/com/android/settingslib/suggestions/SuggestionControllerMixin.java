package com.android.settingslib.suggestions;

import android.app.LoaderManager;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.OnLifecycleEvent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Loader;
import android.os.Bundle;
import android.service.settings.suggestions.Suggestion;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.suggestions.SuggestionController;
import java.util.List;

/* loaded from: classes.dex */
public class SuggestionControllerMixin implements LoaderManager.LoaderCallbacks<List<Suggestion>>, LifecycleObserver, SuggestionController.ServiceConnectionListener {
    private final Context mContext;
    private final SuggestionControllerHost mHost;
    private final SuggestionController mSuggestionController;
    private boolean mSuggestionLoaded;

    public interface SuggestionControllerHost {
        LoaderManager getLoaderManager();

        void onSuggestionReady(List<Suggestion> list);
    }

    public SuggestionControllerMixin(Context context, SuggestionControllerHost suggestionControllerHost, Lifecycle lifecycle, ComponentName componentName) {
        this.mContext = context.getApplicationContext();
        this.mHost = suggestionControllerHost;
        this.mSuggestionController = new SuggestionController(this.mContext, componentName, this);
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onStart() {
        this.mSuggestionController.start();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onStop() {
        this.mSuggestionController.stop();
    }

    @Override // com.android.settingslib.suggestions.SuggestionController.ServiceConnectionListener
    public void onServiceConnected() {
        LoaderManager loaderManager = this.mHost.getLoaderManager();
        if (loaderManager != null) {
            loaderManager.restartLoader(42, null, this);
        }
    }

    @Override // com.android.settingslib.suggestions.SuggestionController.ServiceConnectionListener
    public void onServiceDisconnected() {
        LoaderManager loaderManager = this.mHost.getLoaderManager();
        if (loaderManager != null) {
            loaderManager.destroyLoader(42);
        }
    }

    @Override // android.app.LoaderManager.LoaderCallbacks
    public Loader<List<Suggestion>> onCreateLoader(int i, Bundle bundle) {
        if (i == 42) {
            this.mSuggestionLoaded = false;
            return new SuggestionLoader(this.mContext, this.mSuggestionController);
        }
        throw new IllegalArgumentException("This loader id is not supported " + i);
    }

    /* JADX DEBUG: Method merged with bridge method: onLoadFinished(Landroid/content/Loader;Ljava/lang/Object;)V */
    @Override // android.app.LoaderManager.LoaderCallbacks
    public void onLoadFinished(Loader<List<Suggestion>> loader, List<Suggestion> list) {
        this.mSuggestionLoaded = true;
        this.mHost.onSuggestionReady(list);
    }

    @Override // android.app.LoaderManager.LoaderCallbacks
    public void onLoaderReset(Loader<List<Suggestion>> loader) {
        this.mSuggestionLoaded = false;
    }

    public boolean isSuggestionLoaded() {
        return this.mSuggestionLoaded;
    }

    public void dismissSuggestion(Suggestion suggestion) {
        this.mSuggestionController.dismissSuggestions(suggestion);
    }

    public void launchSuggestion(Suggestion suggestion) {
        this.mSuggestionController.launchSuggestion(suggestion);
    }
}
