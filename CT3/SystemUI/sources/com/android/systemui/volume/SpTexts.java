package com.android.systemui.volume;

import android.content.Context;
import android.content.res.Resources;
import android.util.ArrayMap;
import android.view.View;
import android.widget.TextView;

public class SpTexts {
    private final Context mContext;
    private final ArrayMap<TextView, Integer> mTexts = new ArrayMap<>();
    private final Runnable mUpdateAll = new Runnable() {
        @Override
        public void run() {
            for (int i = 0; i < SpTexts.this.mTexts.size(); i++) {
                SpTexts.this.setTextSizeH((TextView) SpTexts.this.mTexts.keyAt(i), ((Integer) SpTexts.this.mTexts.valueAt(i)).intValue());
            }
        }
    };

    public SpTexts(Context context) {
        this.mContext = context;
    }

    public int add(final TextView text) {
        if (text == null) {
            return 0;
        }
        Resources res = this.mContext.getResources();
        float fontScale = res.getConfiguration().fontScale;
        float density = res.getDisplayMetrics().density;
        float px = text.getTextSize();
        final int sp = (int) ((px / fontScale) / density);
        this.mTexts.put(text, Integer.valueOf(sp));
        text.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewDetachedFromWindow(View v) {
            }

            @Override
            public void onViewAttachedToWindow(View v) {
                SpTexts.this.setTextSizeH(text, sp);
            }
        });
        return sp;
    }

    public void update() {
        if (this.mTexts.isEmpty()) {
            return;
        }
        this.mTexts.keyAt(0).post(this.mUpdateAll);
    }

    public void setTextSizeH(TextView text, int sp) {
        text.setTextSize(2, sp);
    }
}
