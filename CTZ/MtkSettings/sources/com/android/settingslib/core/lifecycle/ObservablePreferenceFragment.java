package com.android.settingslib.core.lifecycle;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleOwner;
import android.content.Context;
import android.os.Bundle;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.PreferenceScreen;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public abstract class ObservablePreferenceFragment extends PreferenceFragment implements LifecycleOwner {
    private final Lifecycle mLifecycle = new Lifecycle(this);

    @Override
    public Lifecycle getLifecycle() {
        return this.mLifecycle;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.mLifecycle.onAttach(context);
    }

    @Override
    public void onCreate(Bundle bundle) {
        this.mLifecycle.onCreate(bundle);
        this.mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
        super.onCreate(bundle);
    }

    @Override
    public void setPreferenceScreen(PreferenceScreen preferenceScreen) {
        this.mLifecycle.setPreferenceScreen(preferenceScreen);
        super.setPreferenceScreen(preferenceScreen);
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        this.mLifecycle.onSaveInstanceState(bundle);
    }

    @Override
    public void onStart() {
        this.mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START);
        super.onStart();
    }

    @Override
    public void onResume() {
        this.mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
        super.onResume();
    }

    @Override
    public void onPause() {
        this.mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);
        super.onPause();
    }

    @Override
    public void onStop() {
        this.mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
        super.onStop();
    }

    @Override
    public void onDestroy() {
        this.mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);
        super.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        this.mLifecycle.onCreateOptionsMenu(menu, menuInflater);
        super.onCreateOptionsMenu(menu, menuInflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        this.mLifecycle.onPrepareOptionsMenu(menu);
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        boolean zOnOptionsItemSelected = this.mLifecycle.onOptionsItemSelected(menuItem);
        if (!zOnOptionsItemSelected) {
            return super.onOptionsItemSelected(menuItem);
        }
        return zOnOptionsItemSelected;
    }
}
