package jp.co.omronsoft.iwnnime.ml.controlpanel;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import jp.co.omronsoft.iwnnime.ml.R;
import jp.co.omronsoft.iwnnime.ml.WnnUtility;

public class KeyVibrateDialogPreference extends DialogPreference {
    public static final String KEY_VIBRATE_TIME_KEY = "key_vibrate_time";
    private int mDefaultVibrateTime;
    private SeekBar mSeekBarVibrate;
    private SharedPreferences mSharedPreferences;
    private TextView mTxtView;

    public KeyVibrateDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        setDialogLayoutResource(R.xml.iwnnime_pref_keyvibrate);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        Context context = getContext();
        Resources r = context.getResources();
        int maxVibrateTime = r.getInteger(R.integer.vibrate_time_max);
        this.mDefaultVibrateTime = r.getInteger(R.integer.vibrate_time_default_value);
        this.mSeekBarVibrate = (SeekBar) view.findViewById(R.id.key_vibrate_seekbar);
        this.mTxtView = (TextView) view.findViewById(R.id.key_vibrate_text);
        this.mSeekBarVibrate.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                KeyVibrateDialogPreference.this.setSeekBarVibrateTime();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        this.mSeekBarVibrate.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                switch (action) {
                    case 1:
                    case 6:
                        int vibrateTime = KeyVibrateDialogPreference.this.mSeekBarVibrate.getProgress();
                        WnnUtility.vibrate(KeyVibrateDialogPreference.this.getContext(), vibrateTime);
                        return true;
                    default:
                        return false;
                }
            }
        });
        this.mSeekBarVibrate.setMax(maxVibrateTime);
        int vibrateTime = this.mSharedPreferences.getInt(KEY_VIBRATE_TIME_KEY, this.mDefaultVibrateTime);
        this.mSeekBarVibrate.setProgress(vibrateTime);
        setSeekBarVibrateTime();
    }

    private void setSeekBarVibrateTime() {
        int timeValue = this.mSeekBarVibrate.getProgress();
        Resources res = getContext().getResources();
        String text = res.getQuantityString(R.plurals.ti_preference_key_vibration_time_summary_txt, timeValue, Integer.valueOf(timeValue));
        this.mTxtView.setText(text);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
        Resources r = getContext().getResources();
        builder.setPositiveButton(R.string.ti_dialog_button_ok_txt, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                KeyVibrateDialogPreference.this.onDialogClosed(true);
            }
        });
        builder.setNeutralButton(r.getString(R.string.ti_preference_key_height_clear_txt), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                KeyVibrateDialogPreference.this.editkeyVibrateTime(KeyVibrateDialogPreference.this.mDefaultVibrateTime);
            }
        });
        builder.setNegativeButton(R.string.ti_dialog_button_cancel_txt, (DialogInterface.OnClickListener) null);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            int vibrateTime = this.mSeekBarVibrate.getProgress();
            editkeyVibrateTime(vibrateTime);
        }
    }

    private void editkeyVibrateTime(int keyVibrateTime) {
        SharedPreferences.Editor editor = this.mSharedPreferences.edit();
        editor.putInt(KEY_VIBRATE_TIME_KEY, keyVibrateTime);
        editor.commit();
    }
}
