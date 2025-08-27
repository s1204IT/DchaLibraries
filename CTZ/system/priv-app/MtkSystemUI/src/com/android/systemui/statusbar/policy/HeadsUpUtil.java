package com.android.systemui.statusbar.policy;

import android.view.View;
import com.android.systemui.R;

/* loaded from: classes.dex */
public final class HeadsUpUtil {
    public static void setIsClickedHeadsUpNotification(View view, boolean z) {
        view.setTag(R.id.is_clicked_heads_up_tag, z ? true : null);
    }

    public static boolean isClickedHeadsUpNotification(View view) {
        Boolean bool = (Boolean) view.getTag(R.id.is_clicked_heads_up_tag);
        return bool != null && bool.booleanValue();
    }
}
