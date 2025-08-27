package com.android.settingslib.core.lifecycle;

import android.app.Activity;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleOwner;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.view.Menu;
import android.view.MenuItem;

/* loaded from: classes.dex */
public class ObservableActivity extends Activity implements LifecycleOwner {
    private final Lifecycle mLifecycle = new Lifecycle(this);

    /* JADX DEBUG: Method merged with bridge method: getLifecycle()Landroid/arch/lifecycle/Lifecycle; */
    @Override // android.arch.lifecycle.LifecycleOwner
    public Lifecycle getLifecycle() {
        return this.mLifecycle;
    }

    @Override // android.app.Activity
    protected void onCreate(Bundle bundle) {
        this.mLifecycle.onAttach(this);
        this.mLifecycle.onCreate(bundle);
        this.mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
        super.onCreate(bundle);
    }

    @Override // android.app.Activity
    public void onCreate(Bundle bundle, PersistableBundle persistableBundle) {
        this.mLifecycle.onAttach(this);
        this.mLifecycle.onCreate(bundle);
        this.mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
        super.onCreate(bundle, persistableBundle);
    }

    @Override // android.app.Activity
    protected void onStart() {
        this.mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START);
        super.onStart();
    }

    @Override // android.app.Activity
    protected void onResume() {
        this.mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
        super.onResume();
    }

    @Override // android.app.Activity
    protected void onPause() {
        this.mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);
        super.onPause();
    }

    @Override // android.app.Activity
    protected void onStop() {
        this.mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
        super.onStop();
    }

    @Override // android.app.Activity
    protected void onDestroy() {
        this.mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);
        super.onDestroy();
    }

    @Override // android.app.Activity
    public boolean onCreateOptionsMenu(Menu menu) {
        if (super.onCreateOptionsMenu(menu)) {
            this.mLifecycle.onCreateOptionsMenu(menu, null);
            return true;
        }
        return false;
    }

    @Override // android.app.Activity
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (super.onPrepareOptionsMenu(menu)) {
            this.mLifecycle.onPrepareOptionsMenu(menu);
            return true;
        }
        return false;
    }

    @Override // android.app.Activity
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        boolean zOnOptionsItemSelected = this.mLifecycle.onOptionsItemSelected(menuItem);
        if (!zOnOptionsItemSelected) {
            return super.onOptionsItemSelected(menuItem);
        }
        return zOnOptionsItemSelected;
    }
}
