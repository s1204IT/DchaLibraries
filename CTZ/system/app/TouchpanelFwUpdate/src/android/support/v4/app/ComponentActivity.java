package android.support.v4.app;

import android.app.Activity;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.LifecycleRegistry;
import android.arch.lifecycle.ReportFragment;
import android.os.Bundle;
import android.support.v4.util.SimpleArrayMap;

/* compiled from: ComponentActivity.java */
/* renamed from: android.support.v4.app.SupportActivity, reason: use source file name */
/* loaded from: classes.dex */
public class ComponentActivity extends Activity implements LifecycleOwner {
    private SimpleArrayMap<Class<? extends Object>, Object> mExtraDataMap = new SimpleArrayMap<>();
    private LifecycleRegistry mLifecycleRegistry = new LifecycleRegistry(this);

    @Override // android.app.Activity
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ReportFragment.injectIfNeededIn(this);
    }

    @Override // android.app.Activity
    protected void onSaveInstanceState(Bundle outState) {
        this.mLifecycleRegistry.markState(Lifecycle.State.CREATED);
        super.onSaveInstanceState(outState);
    }

    public Lifecycle getLifecycle() {
        return this.mLifecycleRegistry;
    }
}
