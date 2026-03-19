package com.android.deskclock.widget.selector;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.android.deskclock.R;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.widget.TextTime;
import java.util.Calendar;
import java.util.List;

public class AlarmSelectionAdapter extends ArrayAdapter<AlarmSelection> {
    public AlarmSelectionAdapter(Context context, int i, List<AlarmSelection> list) {
        super(context, i, list);
    }

    @Override
    @NonNull
    public View getView(int i, @Nullable View view, @NonNull ViewGroup viewGroup) {
        String string;
        Context context = getContext();
        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.alarm_row, viewGroup, false);
        }
        Alarm alarm = getItem(i).getAlarm();
        ((TextTime) view.findViewById(R.id.digital_clock)).setTime(alarm.hour, alarm.minutes);
        ((TextView) view.findViewById(R.id.label)).setText(alarm.label);
        if (!alarm.daysOfWeek.isRepeating()) {
            if (Alarm.isTomorrow(alarm, Calendar.getInstance())) {
                string = context.getResources().getString(R.string.alarm_tomorrow);
            } else {
                string = context.getResources().getString(R.string.alarm_today);
            }
        } else {
            string = alarm.daysOfWeek.toString(context, DataModel.getDataModel().getWeekdayOrder());
        }
        ((TextView) view.findViewById(R.id.daysOfWeek)).setText(string);
        return view;
    }
}
