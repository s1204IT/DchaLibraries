package com.android.timezonepicker;

import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.android.timezonepicker.TimeZonePickerView;

public class TimeZonePickerDialog extends DialogFragment implements TimeZonePickerView.OnTimeZoneSetListener {
    public static final String TAG = TimeZonePickerDialog.class.getSimpleName();
    private boolean mHasCachedResults = false;
    private OnTimeZoneSetListener mTimeZoneSetListener;
    private TimeZonePickerView mView;

    public interface OnTimeZoneSetListener {
        void onTimeZoneSet(TimeZoneInfo timeZoneInfo);
    }

    public void setOnTimeZoneSetListener(OnTimeZoneSetListener onTimeZoneSetListener) {
        this.mTimeZoneSetListener = onTimeZoneSetListener;
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        long j;
        String string;
        boolean z;
        Bundle arguments = getArguments();
        if (arguments != null) {
            j = arguments.getLong("bundle_event_start_time");
            string = arguments.getString("bundle_event_time_zone");
        } else {
            j = 0;
            string = null;
        }
        String str = string;
        long j2 = j;
        if (bundle == null) {
            z = false;
        } else {
            z = bundle.getBoolean("hide_filter_search");
        }
        this.mView = new TimeZonePickerView(getActivity(), null, str, j2, this, z);
        if (bundle != null && bundle.getBoolean("has_results", false)) {
            this.mView.showFilterResults(bundle.getInt("last_filter_type"), bundle.getString("last_filter_string"), bundle.getInt("last_filter_time"));
        }
        return this.mView;
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putBoolean("has_results", this.mView != null && this.mView.hasResults());
        if (this.mView != null) {
            bundle.putInt("last_filter_type", this.mView.getLastFilterType());
            bundle.putString("last_filter_string", this.mView.getLastFilterString());
            bundle.putInt("last_filter_time", this.mView.getLastFilterTime());
            bundle.putBoolean("hide_filter_search", this.mView.getHideFilterSearchOnStart());
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        Dialog dialogOnCreateDialog = super.onCreateDialog(bundle);
        dialogOnCreateDialog.requestWindowFeature(1);
        dialogOnCreateDialog.getWindow().setSoftInputMode(16);
        return dialogOnCreateDialog;
    }

    @Override
    public void onTimeZoneSet(TimeZoneInfo timeZoneInfo) {
        if (this.mTimeZoneSetListener != null) {
            this.mTimeZoneSetListener.onTimeZoneSet(timeZoneInfo);
        }
        dismissAllowingStateLoss();
    }
}
