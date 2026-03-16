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

    public void setOnTimeZoneSetListener(OnTimeZoneSetListener l) {
        this.mTimeZoneSetListener = l;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        long timeMillis = 0;
        String timeZone = null;
        Bundle b = getArguments();
        if (b != null) {
            timeMillis = b.getLong("bundle_event_start_time");
            timeZone = b.getString("bundle_event_time_zone");
        }
        boolean hideFilterSearch = false;
        if (savedInstanceState != null) {
            hideFilterSearch = savedInstanceState.getBoolean("hide_filter_search");
        }
        this.mView = new TimeZonePickerView(getActivity(), null, timeZone, timeMillis, this, hideFilterSearch);
        if (savedInstanceState != null && savedInstanceState.getBoolean("has_results", false)) {
            this.mView.showFilterResults(savedInstanceState.getInt("last_filter_type"), savedInstanceState.getString("last_filter_string"), savedInstanceState.getInt("last_filter_time"));
        }
        return this.mView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("has_results", this.mView != null && this.mView.hasResults());
        if (this.mView != null) {
            outState.putInt("last_filter_type", this.mView.getLastFilterType());
            outState.putString("last_filter_string", this.mView.getLastFilterString());
            outState.putInt("last_filter_time", this.mView.getLastFilterTime());
            outState.putBoolean("hide_filter_search", this.mView.getHideFilterSearchOnStart());
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(1);
        dialog.getWindow().setSoftInputMode(16);
        return dialog;
    }

    @Override
    public void onTimeZoneSet(TimeZoneInfo tzi) {
        if (this.mTimeZoneSetListener != null) {
            this.mTimeZoneSetListener.onTimeZoneSet(tzi);
        }
        dismiss();
    }
}
