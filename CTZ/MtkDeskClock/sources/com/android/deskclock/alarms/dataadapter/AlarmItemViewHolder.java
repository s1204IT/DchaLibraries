package com.android.deskclock.alarms.dataadapter;

import android.content.Context;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.deskclock.AlarmUtils;
import com.android.deskclock.ItemAdapter;
import com.android.deskclock.ItemAnimator;
import com.android.deskclock.R;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.provider.AlarmInstance;
import com.android.deskclock.widget.TextTime;

public abstract class AlarmItemViewHolder extends ItemAdapter.ItemViewHolder<AlarmItemHolder> implements ItemAnimator.OnAnimateChangeListener {
    public static final String ANIMATE_REPEAT_DAYS = "ANIMATE_REPEAT_DAYS";
    public static final float ANIM_LONG_DELAY_INCREMENT_MULTIPLIER = 0.5833333f;
    public static final float ANIM_LONG_DURATION_MULTIPLIER = 0.6666667f;
    public static final float ANIM_SHORT_DELAY_INCREMENT_MULTIPLIER = 0.08333331f;
    public static final float ANIM_SHORT_DURATION_MULTIPLIER = 0.25f;
    public static final float ANIM_STANDARD_DELAY_MULTIPLIER = 0.16666667f;
    private static final float CLOCK_DISABLED_ALPHA = 0.69f;
    private static final float CLOCK_ENABLED_ALPHA = 1.0f;
    public final ImageView arrow;
    public final TextTime clock;
    public final CompoundButton onOff;
    public final TextView preemptiveDismissButton;

    public AlarmItemViewHolder(View view) {
        super(view);
        this.clock = (TextTime) view.findViewById(R.id.digital_clock);
        this.onOff = (CompoundButton) view.findViewById(R.id.onoff);
        this.arrow = (ImageView) view.findViewById(R.id.arrow);
        this.preemptiveDismissButton = (TextView) view.findViewById(R.id.preemptive_dismiss_button);
        this.preemptiveDismissButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view2) {
                AlarmInstance alarmInstance = AlarmItemViewHolder.this.getItemHolder().getAlarmInstance();
                if (alarmInstance != null) {
                    AlarmItemViewHolder.this.getItemHolder().getAlarmTimeClickHandler().dismissAlarmInstance(alarmInstance);
                }
            }
        });
        this.onOff.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                AlarmItemViewHolder.this.getItemHolder().getAlarmTimeClickHandler().setAlarmEnabled((Alarm) AlarmItemViewHolder.this.getItemHolder().item, z);
            }
        });
    }

    @Override
    protected void onBindItemView(AlarmItemHolder alarmItemHolder) {
        Alarm alarm = (Alarm) alarmItemHolder.item;
        bindOnOffSwitch(alarm);
        bindClock(alarm);
        Context context = this.itemView.getContext();
        this.itemView.setContentDescription(((Object) this.clock.getText()) + " " + alarm.getLabelOrDefault(context));
    }

    protected void bindOnOffSwitch(Alarm alarm) {
        if (this.onOff.isChecked() != alarm.enabled) {
            this.onOff.setChecked(alarm.enabled);
        }
    }

    protected void bindClock(Alarm alarm) {
        this.clock.setTime(alarm.hour, alarm.minutes);
        this.clock.setAlpha(alarm.enabled ? 1.0f : CLOCK_DISABLED_ALPHA);
    }

    protected boolean bindPreemptiveDismissButton(Context context, Alarm alarm, AlarmInstance alarmInstance) {
        String string;
        boolean z = alarm.canPreemptivelyDismiss() && alarmInstance != null;
        if (z) {
            this.preemptiveDismissButton.setVisibility(0);
            if (alarm.instanceState == 4) {
                string = context.getString(R.string.alarm_alert_snooze_until, AlarmUtils.getAlarmText(context, alarmInstance, false));
            } else {
                string = context.getString(R.string.alarm_alert_dismiss_text);
            }
            this.preemptiveDismissButton.setText(string);
            this.preemptiveDismissButton.setClickable(true);
        } else {
            this.preemptiveDismissButton.setVisibility(8);
            this.preemptiveDismissButton.setClickable(false);
        }
        return z;
    }
}
