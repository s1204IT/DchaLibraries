package com.android.systemui.statusbar.stack;

import android.view.View;

public class ViewState {
    public float alpha;
    public boolean gone;
    public boolean hidden;
    public float yTranslation;
    public float zTranslation;

    public void copyFrom(ViewState viewState) {
        this.alpha = viewState.alpha;
        this.yTranslation = viewState.yTranslation;
        this.zTranslation = viewState.zTranslation;
        this.gone = viewState.gone;
        this.hidden = viewState.hidden;
    }

    public void initFrom(View view) {
        this.alpha = view.getAlpha();
        this.yTranslation = view.getTranslationY();
        this.zTranslation = view.getTranslationZ();
        this.gone = view.getVisibility() == 8;
        this.hidden = false;
    }
}
