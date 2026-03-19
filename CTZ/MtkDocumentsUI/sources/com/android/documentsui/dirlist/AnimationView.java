package com.android.documentsui.dirlist;

import android.app.FragmentTransaction;
import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import com.android.documentsui.R;

public class AnimationView extends LinearLayout {
    private float mPosition;
    private int mSpan;

    public AnimationView(Context context) {
        super(context);
        this.mPosition = 0.0f;
    }

    public AnimationView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mPosition = 0.0f;
    }

    @Override
    protected void onSizeChanged(int i, int i2, int i3, int i4) {
        super.onSizeChanged(i, i2, i3, i4);
        this.mSpan = i2;
        setPosition(this.mPosition);
    }

    public void setPosition(float f) {
        this.mPosition = f;
        setY(this.mSpan > 0 ? this.mPosition * this.mSpan : 0.0f);
        if (this.mPosition != 0.0f) {
            setTranslationZ(getResources().getDimensionPixelSize(R.dimen.dir_elevation));
        } else {
            setTranslationZ(0.0f);
        }
    }

    static void setupAnimations(FragmentTransaction fragmentTransaction, int i, Bundle bundle) {
        switch (i) {
            case 2:
                bundle.putBoolean("ignoreState", true);
                break;
            case 3:
                fragmentTransaction.setCustomAnimations(R.animator.fade_in, R.animator.dir_leave);
                break;
            case 4:
                bundle.putBoolean("ignoreState", true);
                fragmentTransaction.setCustomAnimations(R.animator.dir_enter, R.animator.fade_out);
                break;
        }
    }
}
