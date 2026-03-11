package com.android.launcher3.util;

import android.util.Property;
import android.view.View;
import com.android.launcher3.LauncherSettings;

public class MultiValueAlpha {
    public static final Property<AlphaProperty, Float> VALUE = new Property<AlphaProperty, Float>(Float.TYPE, LauncherSettings.Settings.EXTRA_VALUE) {
        @Override
        public Float get(AlphaProperty alphaProperty) {
            return Float.valueOf(alphaProperty.mValue);
        }

        @Override
        public void set(AlphaProperty alphaProperty, Float f) {
            alphaProperty.setValue(f.floatValue());
        }
    };
    private final AlphaProperty[] mMyProperties;
    private int mValidMask = 0;
    private final View mView;

    public MultiValueAlpha(View view, int i) {
        this.mView = view;
        this.mMyProperties = new AlphaProperty[i];
        for (int i2 = 0; i2 < i; i2++) {
            int i3 = 1 << i2;
            this.mValidMask |= i3;
            this.mMyProperties[i2] = new AlphaProperty(i3);
        }
    }

    public AlphaProperty getProperty(int i) {
        return this.mMyProperties[i];
    }

    public class AlphaProperty {
        private final int mMyMask;
        private float mValue = 1.0f;
        private float mOthers = 1.0f;

        AlphaProperty(int i) {
            this.mMyMask = i;
        }

        public void setValue(float f) {
            if (this.mValue != f) {
                if ((MultiValueAlpha.this.mValidMask & this.mMyMask) == 0) {
                    this.mOthers = 1.0f;
                    for (AlphaProperty alphaProperty : MultiValueAlpha.this.mMyProperties) {
                        if (alphaProperty != this) {
                            this.mOthers *= alphaProperty.mValue;
                        }
                    }
                }
                MultiValueAlpha.this.mValidMask = this.mMyMask;
                this.mValue = f;
                MultiValueAlpha.this.mView.setAlpha(this.mOthers * this.mValue);
            }
        }

        public float getValue() {
            return this.mValue;
        }
    }
}
