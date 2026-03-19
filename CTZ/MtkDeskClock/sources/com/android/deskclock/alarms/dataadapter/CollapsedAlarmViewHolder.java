package com.android.deskclock.alarms.dataadapter;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.util.Property;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.deskclock.AnimatorUtils;
import com.android.deskclock.ItemAdapter;
import com.android.deskclock.R;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.data.Weekdays;
import com.android.deskclock.events.Events;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.provider.AlarmInstance;
import java.util.Calendar;
import java.util.List;

public final class CollapsedAlarmViewHolder extends AlarmItemViewHolder {
    public static final int VIEW_TYPE = 2131558431;
    private final TextView alarmLabel;
    public final TextView daysOfWeek;
    private final View hairLine;
    private final TextView upcomingInstanceLabel;

    private CollapsedAlarmViewHolder(View view) {
        super(view);
        this.alarmLabel = (TextView) view.findViewById(R.id.label);
        this.daysOfWeek = (TextView) view.findViewById(R.id.days_of_week);
        this.upcomingInstanceLabel = (TextView) view.findViewById(R.id.upcoming_instance_label);
        this.hairLine = view.findViewById(R.id.hairline);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view2) {
                Events.sendAlarmEvent(R.string.action_expand_implied, R.string.label_deskclock);
                CollapsedAlarmViewHolder.this.getItemHolder().expand();
            }
        });
        this.alarmLabel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view2) {
                Events.sendAlarmEvent(R.string.action_expand_implied, R.string.label_deskclock);
                CollapsedAlarmViewHolder.this.getItemHolder().expand();
            }
        });
        this.arrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view2) {
                Events.sendAlarmEvent(R.string.action_expand, R.string.label_deskclock);
                CollapsedAlarmViewHolder.this.getItemHolder().expand();
            }
        });
        this.clock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view2) {
                CollapsedAlarmViewHolder.this.getItemHolder().getAlarmTimeClickHandler().onClockClicked((Alarm) CollapsedAlarmViewHolder.this.getItemHolder().item);
                Events.sendAlarmEvent(R.string.action_expand_implied, R.string.label_deskclock);
                CollapsedAlarmViewHolder.this.getItemHolder().expand();
            }
        });
        view.setImportantForAccessibility(2);
    }

    @Override
    protected void onBindItemView(AlarmItemHolder alarmItemHolder) {
        super.onBindItemView(alarmItemHolder);
        Alarm alarm = (Alarm) alarmItemHolder.item;
        AlarmInstance alarmInstance = alarmItemHolder.getAlarmInstance();
        Context context = this.itemView.getContext();
        bindRepeatText(context, alarm);
        bindReadOnlyLabel(context, alarm);
        bindUpcomingInstance(context, alarm);
        bindPreemptiveDismissButton(context, alarm, alarmInstance);
    }

    private void bindReadOnlyLabel(Context context, Alarm alarm) {
        if (alarm.label != null && alarm.label.length() != 0) {
            this.alarmLabel.setText(alarm.label);
            this.alarmLabel.setVisibility(0);
            this.alarmLabel.setContentDescription(context.getString(R.string.label_description) + " " + alarm.label);
            return;
        }
        this.alarmLabel.setVisibility(8);
    }

    private void bindRepeatText(Context context, Alarm alarm) {
        if (alarm.daysOfWeek.isRepeating()) {
            Weekdays.Order weekdayOrder = DataModel.getDataModel().getWeekdayOrder();
            this.daysOfWeek.setText(alarm.daysOfWeek.toString(context, weekdayOrder));
            this.daysOfWeek.setContentDescription(alarm.daysOfWeek.toAccessibilityString(context, weekdayOrder));
            this.daysOfWeek.setVisibility(0);
            return;
        }
        this.daysOfWeek.setVisibility(8);
    }

    private void bindUpcomingInstance(Context context, Alarm alarm) {
        String string;
        if (alarm.daysOfWeek.isRepeating()) {
            this.upcomingInstanceLabel.setVisibility(8);
            return;
        }
        this.upcomingInstanceLabel.setVisibility(0);
        if (Alarm.isTomorrow(alarm, Calendar.getInstance())) {
            string = context.getString(R.string.alarm_tomorrow);
        } else {
            string = context.getString(R.string.alarm_today);
        }
        this.upcomingInstanceLabel.setText(string);
    }

    @Override
    public Animator onAnimateChange(List<Object> list, int i, int i2, int i3, int i4, long j) {
        return null;
    }

    @Override
    public Animator onAnimateChange(RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder viewHolder2, long j) {
        Animator animatorCreateExpandingAnimator;
        if (!(viewHolder instanceof AlarmItemViewHolder) || !(viewHolder2 instanceof AlarmItemViewHolder)) {
            return null;
        }
        boolean z = this == viewHolder2;
        setChangingViewsAlpha(z ? 0.0f : 1.0f);
        if (z) {
            animatorCreateExpandingAnimator = createCollapsingAnimator((AlarmItemViewHolder) viewHolder, j);
        } else {
            animatorCreateExpandingAnimator = createExpandingAnimator((AlarmItemViewHolder) viewHolder2, j);
        }
        animatorCreateExpandingAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                CollapsedAlarmViewHolder.this.clock.setVisibility(0);
                CollapsedAlarmViewHolder.this.onOff.setVisibility(0);
                CollapsedAlarmViewHolder.this.arrow.setVisibility(0);
                CollapsedAlarmViewHolder.this.arrow.setTranslationY(0.0f);
                CollapsedAlarmViewHolder.this.setChangingViewsAlpha(1.0f);
                CollapsedAlarmViewHolder.this.arrow.jumpDrawablesToCurrentState();
            }
        });
        return animatorCreateExpandingAnimator;
    }

    private Animator createExpandingAnimator(AlarmItemViewHolder alarmItemViewHolder, long j) {
        this.clock.setVisibility(4);
        this.onOff.setVisibility(4);
        this.arrow.setVisibility(4);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(ObjectAnimator.ofFloat(this.alarmLabel, (Property<TextView, Float>) View.ALPHA, 0.0f), ObjectAnimator.ofFloat(this.daysOfWeek, (Property<TextView, Float>) View.ALPHA, 0.0f), ObjectAnimator.ofFloat(this.upcomingInstanceLabel, (Property<TextView, Float>) View.ALPHA, 0.0f), ObjectAnimator.ofFloat(this.preemptiveDismissButton, (Property<TextView, Float>) View.ALPHA, 0.0f), ObjectAnimator.ofFloat(this.hairLine, (Property<View, Float>) View.ALPHA, 0.0f));
        animatorSet.setDuration((long) (j * 0.25f));
        View view = this.itemView;
        Animator duration = AnimatorUtils.getBoundsAnimator(view, view, alarmItemViewHolder.itemView).setDuration(j);
        duration.setInterpolator(AnimatorUtils.INTERPOLATOR_FAST_OUT_SLOW_IN);
        AnimatorSet animatorSet2 = new AnimatorSet();
        animatorSet2.playTogether(animatorSet, duration);
        return animatorSet2;
    }

    private Animator createCollapsingAnimator(AlarmItemViewHolder alarmItemViewHolder, long j) {
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(ObjectAnimator.ofFloat(this.alarmLabel, (Property<TextView, Float>) View.ALPHA, 1.0f), ObjectAnimator.ofFloat(this.daysOfWeek, (Property<TextView, Float>) View.ALPHA, 1.0f), ObjectAnimator.ofFloat(this.upcomingInstanceLabel, (Property<TextView, Float>) View.ALPHA, 1.0f), ObjectAnimator.ofFloat(this.preemptiveDismissButton, (Property<TextView, Float>) View.ALPHA, 1.0f), ObjectAnimator.ofFloat(this.hairLine, (Property<View, Float>) View.ALPHA, 1.0f));
        long j2 = (long) (j * 0.16666667f);
        animatorSet.setDuration(j2);
        animatorSet.setStartDelay(j - j2);
        View view = alarmItemViewHolder.itemView;
        View view2 = this.itemView;
        Animator duration = AnimatorUtils.getBoundsAnimator(view2, view, view2).setDuration(j);
        duration.setInterpolator(AnimatorUtils.INTERPOLATOR_FAST_OUT_SLOW_IN);
        ImageView imageView = alarmItemViewHolder.arrow;
        Rect rect = new Rect(0, 0, imageView.getWidth(), imageView.getHeight());
        ((ViewGroup) view2).offsetDescendantRectToMyCoords(this.arrow, new Rect(0, 0, this.arrow.getWidth(), this.arrow.getHeight()));
        ((ViewGroup) view).offsetDescendantRectToMyCoords(imageView, rect);
        this.arrow.setTranslationY(rect.bottom - r9.bottom);
        this.arrow.setVisibility(0);
        this.clock.setVisibility(0);
        this.onOff.setVisibility(0);
        ObjectAnimator duration2 = ObjectAnimator.ofFloat(this.arrow, (Property<ImageView, Float>) View.TRANSLATION_Y, 0.0f).setDuration(j);
        duration2.setInterpolator(AnimatorUtils.INTERPOLATOR_FAST_OUT_SLOW_IN);
        AnimatorSet animatorSet2 = new AnimatorSet();
        animatorSet2.playTogether(animatorSet, duration, duration2);
        animatorSet2.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animator) {
                AnimatorUtils.startDrawableAnimation(CollapsedAlarmViewHolder.this.arrow);
            }
        });
        return animatorSet2;
    }

    private void setChangingViewsAlpha(float f) {
        this.alarmLabel.setAlpha(f);
        this.daysOfWeek.setAlpha(f);
        this.upcomingInstanceLabel.setAlpha(f);
        this.hairLine.setAlpha(f);
        this.preemptiveDismissButton.setAlpha(f);
    }

    public static class Factory implements ItemAdapter.ItemViewHolder.Factory {
        private final LayoutInflater mLayoutInflater;

        public Factory(LayoutInflater layoutInflater) {
            this.mLayoutInflater = layoutInflater;
        }

        @Override
        public ItemAdapter.ItemViewHolder<?> createViewHolder(ViewGroup viewGroup, int i) {
            return new CollapsedAlarmViewHolder(this.mLayoutInflater.inflate(i, viewGroup, false));
        }
    }
}
