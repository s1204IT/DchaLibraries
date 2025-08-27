package com.mediatek.systemui.ext;

import android.content.Context;
import android.content.ContextWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

/* loaded from: classes.dex */
public class DefaultStatusBarPlmnPlugin extends ContextWrapper implements IStatusBarPlmnPlugin {
    public DefaultStatusBarPlmnPlugin(Context context) {
        super(context);
    }

    @Override // com.mediatek.systemui.ext.IStatusBarPlmnPlugin
    public boolean supportCustomizeCarrierLabel() {
        return false;
    }

    @Override // com.mediatek.systemui.ext.IStatusBarPlmnPlugin
    public View customizeCarrierLabel(ViewGroup viewGroup, View view) {
        return null;
    }

    @Override // com.mediatek.systemui.ext.IStatusBarPlmnPlugin
    public void updateCarrierLabelVisibility(boolean z, boolean z2) {
    }

    @Override // com.mediatek.systemui.ext.IStatusBarPlmnPlugin
    public void updateCarrierLabel(int i, boolean z, boolean z2, String[] strArr) {
    }

    @Override // com.mediatek.systemui.ext.IStatusBarPlmnPlugin
    public void addPlmn(LinearLayout linearLayout, Context context) {
    }

    @Override // com.mediatek.systemui.ext.IStatusBarPlmnPlugin
    public void setPlmnVisibility(int i) {
    }
}
