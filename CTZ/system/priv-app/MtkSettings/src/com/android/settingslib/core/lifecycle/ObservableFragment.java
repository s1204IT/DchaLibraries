package com.android.settingslib.core.lifecycle;

import android.app.Fragment;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleOwner;
import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

/* loaded from: classes.dex */
public class ObservableFragment extends Fragment implements LifecycleOwner {
    private final Lifecycle mLifecycle = new Lifecycle(this);

    /* JADX DEBUG: Method merged with bridge method: getLifecycle()Landroid/arch/lifecycle/Lifecycle; */
    @Override // android.arch.lifecycle.LifecycleOwner
    public Lifecycle getLifecycle() {
        return this.mLifecycle;
    }

    @Override // android.app.Fragment
    public void onAttach(Context context) {
        super.onAttach(context);
        this.mLifecycle.onAttach(context);
    }

    @Override // android.app.Fragment
    public void onCreate(Bundle bundle) {
        this.mLifecycle.onCreate(bundle);
        this.mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
        super.onCreate(bundle);
    }

    @Override // android.app.Fragment
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        this.mLifecycle.onSaveInstanceState(bundle);
    }

    @Override // android.app.Fragment
    public void onStart() {
        this.mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START);
        super.onStart();
    }

    @Override // android.app.Fragment
    public void onResume() {
        this.mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
        super.onResume();
    }

    @Override // android.app.Fragment
    public void onPause() {
        this.mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);
        super.onPause();
    }

    @Override // android.app.Fragment
    public void onStop() {
        this.mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
        super.onStop();
    }

    @Override // android.app.Fragment
    public void onDestroy() {
        this.mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);
        super.onDestroy();
    }

    @Override // android.app.Fragment
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        this.mLifecycle.onCreateOptionsMenu(menu, menuInflater);
        super.onCreateOptionsMenu(menu, menuInflater);
    }

    @Override // android.app.Fragment
    public void onPrepareOptionsMenu(Menu menu) {
        this.mLifecycle.onPrepareOptionsMenu(menu);
        super.onPrepareOptionsMenu(menu);
    }

    @Override // android.app.Fragment
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        boolean zOnOptionsItemSelected = this.mLifecycle.onOptionsItemSelected(menuItem);
        if (!zOnOptionsItemSelected) {
            return super.onOptionsItemSelected(menuItem);
        }
        return zOnOptionsItemSelected;
    }
}
