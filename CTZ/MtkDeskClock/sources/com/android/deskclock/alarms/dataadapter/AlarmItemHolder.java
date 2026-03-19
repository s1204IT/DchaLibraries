package com.android.deskclock.alarms.dataadapter;

import android.os.Bundle;
import com.android.deskclock.ItemAdapter;
import com.android.deskclock.R;
import com.android.deskclock.alarms.AlarmTimeClickHandler;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.provider.AlarmInstance;

public class AlarmItemHolder extends ItemAdapter.ItemHolder<Alarm> {
    private static final String EXPANDED_KEY = "expanded";
    private final AlarmInstance mAlarmInstance;
    private final AlarmTimeClickHandler mAlarmTimeClickHandler;
    private boolean mExpanded;

    public AlarmItemHolder(Alarm alarm, AlarmInstance alarmInstance, AlarmTimeClickHandler alarmTimeClickHandler) {
        super(alarm, alarm.id);
        this.mAlarmInstance = alarmInstance;
        this.mAlarmTimeClickHandler = alarmTimeClickHandler;
    }

    @Override
    public int getItemViewType() {
        return isExpanded() ? R.layout.alarm_time_expanded : R.layout.alarm_time_collapsed;
    }

    public AlarmTimeClickHandler getAlarmTimeClickHandler() {
        return this.mAlarmTimeClickHandler;
    }

    public AlarmInstance getAlarmInstance() {
        return this.mAlarmInstance;
    }

    public void expand() {
        if (!isExpanded()) {
            this.mExpanded = true;
            notifyItemChanged();
        }
    }

    public void collapse() {
        if (isExpanded()) {
            this.mExpanded = false;
            notifyItemChanged();
        }
    }

    public boolean isExpanded() {
        return this.mExpanded;
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putBoolean(EXPANDED_KEY, this.mExpanded);
    }

    @Override
    public void onRestoreInstanceState(Bundle bundle) {
        super.onRestoreInstanceState(bundle);
        this.mExpanded = bundle.getBoolean(EXPANDED_KEY);
    }
}
