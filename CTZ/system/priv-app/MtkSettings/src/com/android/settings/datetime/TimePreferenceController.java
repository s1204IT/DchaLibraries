package com.android.settings.datetime;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.TimePickerDialog;
import android.content.Context;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.widget.TimePicker;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.core.AbstractPreferenceController;
import java.util.Calendar;

/* loaded from: classes.dex */
public class TimePreferenceController extends AbstractPreferenceController implements TimePickerDialog.OnTimeSetListener, PreferenceControllerMixin {
    private final AutoTimePreferenceController mAutoTimePreferenceController;
    private final TimePreferenceHost mHost;

    public interface TimePreferenceHost extends UpdateTimeAndDateCallback {
        void showTimePicker();
    }

    public TimePreferenceController(Context context, TimePreferenceHost timePreferenceHost, AutoTimePreferenceController autoTimePreferenceController) {
        super(context);
        this.mHost = timePreferenceHost;
        this.mAutoTimePreferenceController = autoTimePreferenceController;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return true;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        if (!(preference instanceof RestrictedPreference)) {
            return;
        }
        preference.setSummary(DateFormat.getTimeFormat(this.mContext).format(Calendar.getInstance().getTime()));
        if (!((RestrictedPreference) preference).isDisabledByAdmin()) {
            preference.setEnabled(!this.mAutoTimePreferenceController.isEnabled());
        }
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals("time", preference.getKey())) {
            return false;
        }
        this.mHost.showTimePicker();
        return true;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "time";
    }

    @Override // android.app.TimePickerDialog.OnTimeSetListener
    public void onTimeSet(TimePicker timePicker, int i, int i2) {
        if (this.mContext != null) {
            setTime(i, i2);
            this.mHost.updateTimeAndDateDisplay(this.mContext);
        }
    }

    public TimePickerDialog buildTimePicker(Activity activity) {
        Calendar calendar = Calendar.getInstance();
        return new TimePickerDialog(activity, this, calendar.get(11), calendar.get(12), DateFormat.is24HourFormat(activity));
    }

    void setTime(int i, int i2) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(11, i);
        calendar.set(12, i2);
        calendar.set(13, 0);
        calendar.set(14, 0);
        long jMax = Math.max(calendar.getTimeInMillis(), 1194220800000L);
        if (jMax / 1000 < 2147483647L) {
            ((AlarmManager) this.mContext.getSystemService("alarm")).setTime(jMax);
        }
    }
}
