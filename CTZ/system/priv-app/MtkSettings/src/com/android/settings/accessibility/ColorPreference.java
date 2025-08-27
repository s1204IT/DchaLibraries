package com.android.settings.accessibility;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.settings.R;

/* loaded from: classes.dex */
public class ColorPreference extends ListDialogPreference {
    private ColorDrawable mPreviewColor;
    private boolean mPreviewEnabled;

    public ColorPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        setDialogLayoutResource(R.layout.grid_picker_dialog);
        setListItemLayoutResource(R.layout.color_picker_item);
    }

    @Override // android.support.v7.preference.Preference
    public boolean shouldDisableDependents() {
        return Color.alpha(getValue()) == 0 || super.shouldDisableDependents();
    }

    @Override // com.android.settings.accessibility.ListDialogPreference
    protected CharSequence getTitleAt(int i) {
        CharSequence titleAt = super.getTitleAt(i);
        if (titleAt != null) {
            return titleAt;
        }
        int valueAt = getValueAt(i);
        return getContext().getString(R.string.color_custom, Integer.valueOf(Color.red(valueAt)), Integer.valueOf(Color.green(valueAt)), Integer.valueOf(Color.blue(valueAt)));
    }

    @Override // android.support.v7.preference.Preference
    public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
        super.onBindViewHolder(preferenceViewHolder);
        if (this.mPreviewEnabled) {
            ImageView imageView = (ImageView) preferenceViewHolder.findViewById(R.id.color_preview);
            int value = getValue();
            if (Color.alpha(value) < 255) {
                imageView.setBackgroundResource(R.drawable.transparency_tileable);
            } else {
                imageView.setBackground(null);
            }
            if (this.mPreviewColor == null) {
                this.mPreviewColor = new ColorDrawable(value);
                imageView.setImageDrawable(this.mPreviewColor);
            } else {
                this.mPreviewColor.setColor(value);
            }
            CharSequence summary = getSummary();
            if (!TextUtils.isEmpty(summary)) {
                imageView.setContentDescription(summary);
            } else {
                imageView.setContentDescription(null);
            }
            imageView.setAlpha(isEnabled() ? 1.0f : 0.2f);
        }
    }

    @Override // com.android.settings.accessibility.ListDialogPreference
    protected void onBindListItem(View view, int i) {
        int valueAt = getValueAt(i);
        int iAlpha = Color.alpha(valueAt);
        ImageView imageView = (ImageView) view.findViewById(R.id.color_swatch);
        if (iAlpha < 255) {
            imageView.setBackgroundResource(R.drawable.transparency_tileable);
        } else {
            imageView.setBackground(null);
        }
        Drawable drawable = imageView.getDrawable();
        if (drawable instanceof ColorDrawable) {
            ((ColorDrawable) drawable).setColor(valueAt);
        } else {
            imageView.setImageDrawable(new ColorDrawable(valueAt));
        }
        CharSequence titleAt = getTitleAt(i);
        if (titleAt != null) {
            ((TextView) view.findViewById(R.id.summary)).setText(titleAt);
        }
    }
}
