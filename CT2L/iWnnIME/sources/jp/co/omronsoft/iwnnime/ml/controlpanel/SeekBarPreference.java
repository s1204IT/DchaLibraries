package jp.co.omronsoft.iwnnime.ml.controlpanel;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import jp.co.omronsoft.iwnnime.ml.R;

public class SeekBarPreference extends DialogPreference {
    private Drawable mMyIcon;

    public SeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.seekbar_dialog);
        setPositiveButtonText(R.string.ti_dialog_button_ok_txt);
        setNegativeButtonText(R.string.ti_dialog_button_cancel_txt);
        this.mMyIcon = getDialogIcon();
        setDialogIcon((Drawable) null);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        ImageView iconView = (ImageView) view.findViewById(R.id.icon);
        if (this.mMyIcon != null) {
            iconView.setImageDrawable(this.mMyIcon);
        } else {
            iconView.setVisibility(8);
        }
    }

    protected static SeekBar getSeekBar(View dialogView) {
        return (SeekBar) dialogView.findViewById(R.id.seekbar);
    }
}
