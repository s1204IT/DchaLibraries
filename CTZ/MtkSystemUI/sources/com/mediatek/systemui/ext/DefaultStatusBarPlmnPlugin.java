package com.mediatek.systemui.ext;

import android.content.Context;
import android.content.ContextWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

public class DefaultStatusBarPlmnPlugin extends ContextWrapper implements IStatusBarPlmnPlugin {
    public DefaultStatusBarPlmnPlugin(Context context) {
        super(context);
    }

    @Override
    public boolean supportCustomizeCarrierLabel() {
        return false;
    }

    @Override
    public View customizeCarrierLabel(ViewGroup viewGroup, View view) {
        return null;
    }

    @Override
    public void updateCarrierLabelVisibility(boolean z, boolean z2) {
    }

    @Override
    public void updateCarrierLabel(int i, boolean z, boolean z2, String[] strArr) {
    }

    @Override
    public void addPlmn(LinearLayout linearLayout, Context context) {
    }

    @Override
    public void setPlmnVisibility(int i) {
    }
}
