package jp.co.omronsoft.iwnnime.ml.controlpanel;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.ListPreference;
import android.util.AttributeSet;
import jp.co.omronsoft.iwnnime.ml.R;

public class MushroomListPreference extends ListPreference {
    public MushroomListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        String oldValue = getValue();
        super.onDialogClosed(positiveResult);
        if (positiveResult && !getValue().equals(oldValue) && getValue().equals("use")) {
            setValue(oldValue);
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext()).setTitle(R.string.ti_preference_attention_dialog_title_txt).setIcon(android.R.drawable.ic_dialog_alert).setMessage(R.string.ti_preference_mushroom_dialog_message_txt).setPositiveButton(R.string.ti_dialog_button_ok_txt, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    MushroomListPreference.this.setValue("use");
                }
            }).setNegativeButton(R.string.ti_dialog_button_cancel_txt, (DialogInterface.OnClickListener) null);
            AlertDialog optionsDialog = builder.create();
            optionsDialog.show();
        }
    }
}
