package com.android.settings.gestures;

import android.content.Context;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import java.util.List;

/* loaded from: classes.dex */
public interface AssistGestureFeatureProvider {
    List<AbstractPreferenceController> getControllers(Context context, Lifecycle lifecycle);

    boolean isSensorAvailable(Context context);

    boolean isSupported(Context context);
}
