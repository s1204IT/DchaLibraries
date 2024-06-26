package com.android.systemui.volume;

import android.content.Context;
import android.content.res.Resources;
import android.util.ArrayMap;
import android.view.View;
import android.widget.TextView;
/* loaded from: classes.dex */
public class ConfigurableTexts {
    private final Context mContext;
    private final ArrayMap<TextView, Integer> mTexts = new ArrayMap<>();
    private final ArrayMap<TextView, Integer> mTextLabels = new ArrayMap<>();
    private final Runnable mUpdateAll = new Runnable() { // from class: com.android.systemui.volume.ConfigurableTexts.2
        @Override // java.lang.Runnable
        public void run() {
            for (int i = 0; i < ConfigurableTexts.this.mTexts.size(); i++) {
                ConfigurableTexts.this.setTextSizeH((TextView) ConfigurableTexts.this.mTexts.keyAt(i), ((Integer) ConfigurableTexts.this.mTexts.valueAt(i)).intValue());
            }
            for (int i2 = 0; i2 < ConfigurableTexts.this.mTextLabels.size(); i2++) {
                ConfigurableTexts.this.setTextLabelH((TextView) ConfigurableTexts.this.mTextLabels.keyAt(i2), ((Integer) ConfigurableTexts.this.mTextLabels.valueAt(i2)).intValue());
            }
        }
    };

    public ConfigurableTexts(Context context) {
        this.mContext = context;
    }

    public int add(TextView textView) {
        return add(textView, -1);
    }

    public int add(final TextView textView, int i) {
        if (textView == null) {
            return 0;
        }
        Resources resources = this.mContext.getResources();
        float f = resources.getConfiguration().fontScale;
        final int textSize = (int) ((textView.getTextSize() / f) / resources.getDisplayMetrics().density);
        this.mTexts.put(textView, Integer.valueOf(textSize));
        textView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() { // from class: com.android.systemui.volume.ConfigurableTexts.1
            @Override // android.view.View.OnAttachStateChangeListener
            public void onViewDetachedFromWindow(View view) {
            }

            @Override // android.view.View.OnAttachStateChangeListener
            public void onViewAttachedToWindow(View view) {
                ConfigurableTexts.this.setTextSizeH(textView, textSize);
            }
        });
        this.mTextLabels.put(textView, Integer.valueOf(i));
        return textSize;
    }

    public void update() {
        if (this.mTexts.isEmpty()) {
            return;
        }
        this.mTexts.keyAt(0).post(this.mUpdateAll);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setTextSizeH(TextView textView, int i) {
        textView.setTextSize(2, i);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setTextLabelH(TextView textView, int i) {
        if (i >= 0) {
            try {
                Util.setText(textView, this.mContext.getString(i));
            } catch (Resources.NotFoundException e) {
            }
        }
    }
}
