package com.android.settings.core.instrumentation;

import android.content.Context;
import com.android.settings.DialogCreatable;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.Instrumentable;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.core.instrumentation.VisibilityLoggerMixin;
import com.android.settingslib.core.lifecycle.ObservableDialogFragment;
/* loaded from: classes.dex */
public abstract class InstrumentedDialogFragment extends ObservableDialogFragment implements Instrumentable {
    protected final DialogCreatable mDialogCreatable;
    protected int mDialogId;
    protected MetricsFeatureProvider mMetricsFeatureProvider;

    public InstrumentedDialogFragment() {
        this(null, 0);
    }

    public InstrumentedDialogFragment(DialogCreatable dialogCreatable, int i) {
        this.mDialogCreatable = dialogCreatable;
        this.mDialogId = i;
    }

    @Override // com.android.settingslib.core.lifecycle.ObservableDialogFragment, android.app.DialogFragment, android.app.Fragment
    public void onAttach(Context context) {
        super.onAttach(context);
        this.mMetricsFeatureProvider = FeatureFactory.getFactory(context).getMetricsFeatureProvider();
        this.mLifecycle.addObserver(new VisibilityLoggerMixin(getMetricsCategory(), this.mMetricsFeatureProvider));
        this.mLifecycle.onAttach(context);
    }
}
