package com.android.settings.datetime;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.content.Context;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.widget.DatePicker;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.core.AbstractPreferenceController;
import java.util.Calendar;

/* loaded from: classes.dex */
public class DatePreferenceController extends AbstractPreferenceController implements DatePickerDialog.OnDateSetListener, PreferenceControllerMixin {
    private final AutoTimePreferenceController mAutoTimePreferenceController;
    private final DatePreferenceHost mHost;

    public interface DatePreferenceHost extends UpdateTimeAndDateCallback {
        void showDatePicker();
    }

    public DatePreferenceController(Context context, DatePreferenceHost datePreferenceHost, AutoTimePreferenceController autoTimePreferenceController) {
        super(context);
        this.mHost = datePreferenceHost;
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
        preference.setSummary(DateFormat.getLongDateFormat(this.mContext).format(Calendar.getInstance().getTime()));
        if (!((RestrictedPreference) preference).isDisabledByAdmin()) {
            preference.setEnabled(!this.mAutoTimePreferenceController.isEnabled());
        }
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), "date")) {
            return false;
        }
        this.mHost.showDatePicker();
        return true;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "date";
    }

    @Override // android.app.DatePickerDialog.OnDateSetListener
    public void onDateSet(DatePicker datePicker, int i, int i2, int i3) {
        setDate(i, i2, i3);
        this.mHost.updateTimeAndDateDisplay(this.mContext);
    }

    public DatePickerDialog buildDatePicker(Activity activity) {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(activity, this, calendar.get(1), calendar.get(2), calendar.get(5));
        calendar.clear();
        calendar.set(2007, 0, 1);
        datePickerDialog.getDatePicker().setMinDate(calendar.getTimeInMillis());
        calendar.clear();
        calendar.set(2037, 11, 31);
        datePickerDialog.getDatePicker().setMaxDate(calendar.getTimeInMillis());
        return datePickerDialog;
    }

    void setDate(int i, int i2, int i3) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(1, i);
        calendar.set(2, i2);
        calendar.set(5, i3);
        long jMax = Math.max(calendar.getTimeInMillis(), 1194220800000L);
        if (jMax / 1000 < 2147483647L) {
            ((AlarmManager) this.mContext.getSystemService("alarm")).setTime(jMax);
        }
    }
}
