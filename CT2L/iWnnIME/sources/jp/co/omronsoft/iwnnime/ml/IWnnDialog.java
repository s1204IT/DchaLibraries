package jp.co.omronsoft.iwnnime.ml;

import android.app.Dialog;
import android.content.Context;

public class IWnnDialog extends Dialog {
    public IWnnDialog(Context context, int theme) {
        super(context, theme);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (!hasFocus) {
            dismiss();
        }
    }
}
