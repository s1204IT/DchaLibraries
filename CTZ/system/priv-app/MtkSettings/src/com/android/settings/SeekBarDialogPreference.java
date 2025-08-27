package com.android.settings;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import com.android.settingslib.CustomDialogPreference;

/* loaded from: classes.dex */
public class SeekBarDialogPreference extends CustomDialogPreference {
    private final Drawable mMyIcon;

    public SeekBarDialogPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        setDialogLayoutResource(R.layout.preference_dialog_seekbar_material);
        createActionButtons();
        this.mMyIcon = getDialogIcon();
        setDialogIcon(null);
    }

    public SeekBarDialogPreference(Context context) {
        this(context, null);
    }

    public void createActionButtons() {
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);
    }

    @Override // com.android.settingslib.CustomDialogPreference
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        ImageView imageView = (ImageView) view.findViewById(android.R.id.icon);
        if (this.mMyIcon != null) {
            imageView.setImageDrawable(this.mMyIcon);
        } else {
            imageView.setVisibility(8);
        }
    }

    protected static SeekBar getSeekBar(View view) {
        return (SeekBar) view.findViewById(R.id.seekbar);
    }
}
