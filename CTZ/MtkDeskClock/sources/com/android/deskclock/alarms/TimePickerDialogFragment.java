package com.android.deskclock.alarms;

import android.R;
import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.format.DateFormat;
import android.widget.TimePicker;
import com.android.deskclock.Utils;
import java.util.Calendar;

public class TimePickerDialogFragment extends DialogFragment {
    private static final String ARG_HOUR = "TimePickerDialogFragment_hour";
    private static final String ARG_MINUTE = "TimePickerDialogFragment_minute";
    private static final String TAG = "TimePickerDialogFragment";

    public interface OnTimeSetListener {
        void onTimeSet(TimePickerDialogFragment timePickerDialogFragment, int i, int i2);
    }

    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        final OnTimeSetListener onTimeSetListener = (OnTimeSetListener) getParentFragment();
        Calendar calendar = Calendar.getInstance();
        Bundle arguments = getArguments() == null ? Bundle.EMPTY : getArguments();
        int i = arguments.getInt(ARG_HOUR, calendar.get(11));
        int i2 = arguments.getInt(ARG_MINUTE, calendar.get(12));
        if (Utils.isLOrLater()) {
            Activity activity = getActivity();
            return new TimePickerDialog(activity, new TimePickerDialog.OnTimeSetListener() {
                @Override
                public void onTimeSet(TimePicker timePicker, int i3, int i4) {
                    onTimeSetListener.onTimeSet(TimePickerDialogFragment.this, i3, i4);
                }
            }, i, i2, DateFormat.is24HourFormat(activity));
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        Context context = builder.getContext();
        final TimePicker timePicker = new TimePicker(context);
        timePicker.setCurrentHour(Integer.valueOf(i));
        timePicker.setCurrentMinute(Integer.valueOf(i2));
        timePicker.setIs24HourView(Boolean.valueOf(DateFormat.is24HourFormat(context)));
        return builder.setView(timePicker).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i3) {
                onTimeSetListener.onTimeSet(TimePickerDialogFragment.this, timePicker.getCurrentHour().intValue(), timePicker.getCurrentMinute().intValue());
            }
        }).setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener) null).create();
    }

    public static void show(Fragment fragment) {
        show(fragment, -1, -1);
    }

    public static void show(Fragment fragment, int i, int i2) {
        if (!(fragment instanceof OnTimeSetListener)) {
            throw new IllegalArgumentException("Fragment must implement OnTimeSetListener");
        }
        FragmentManager childFragmentManager = fragment.getChildFragmentManager();
        if (childFragmentManager == null || childFragmentManager.isDestroyed()) {
            return;
        }
        removeTimeEditDialog(childFragmentManager);
        TimePickerDialogFragment timePickerDialogFragment = new TimePickerDialogFragment();
        Bundle bundle = new Bundle();
        if (i >= 0 && i < 24) {
            bundle.putInt(ARG_HOUR, i);
        }
        if (i2 >= 0 && i2 < 60) {
            bundle.putInt(ARG_MINUTE, i2);
        }
        timePickerDialogFragment.setArguments(bundle);
        timePickerDialogFragment.show(childFragmentManager, TAG);
    }

    public static void removeTimeEditDialog(FragmentManager fragmentManager) {
        Fragment fragmentFindFragmentByTag;
        if (fragmentManager != null && (fragmentFindFragmentByTag = fragmentManager.findFragmentByTag(TAG)) != null) {
            fragmentManager.beginTransaction().remove(fragmentFindFragmentByTag).commit();
        }
    }
}
