package com.android.deskclock.alarms.dataadapter;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Vibrator;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Property;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.deskclock.AnimatorUtils;
import com.android.deskclock.ItemAdapter;
import com.android.deskclock.R;
import com.android.deskclock.ThemeUtils;
import com.android.deskclock.Utils;
import com.android.deskclock.alarms.AlarmTimeClickHandler;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.events.Events;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.provider.AlarmInstance;
import com.android.deskclock.uidata.UiDataModel;
import java.util.List;

public final class ExpandedAlarmViewHolder extends AlarmItemViewHolder {
    public static final int VIEW_TYPE = 2131558432;
    private final CompoundButton[] dayButtons;
    public final TextView delete;
    private final TextView editLabel;
    private final View hairLine;
    private final boolean mHasVibrator;
    public final CheckBox repeat;
    public final LinearLayout repeatDays;
    public final TextView ringtone;
    public final CheckBox vibrate;

    private ExpandedAlarmViewHolder(View view, boolean z) {
        super(view);
        this.dayButtons = new CompoundButton[7];
        this.mHasVibrator = z;
        this.delete = (TextView) view.findViewById(R.id.delete);
        this.repeat = (CheckBox) view.findViewById(R.id.repeat_onoff);
        this.vibrate = (CheckBox) view.findViewById(R.id.vibrate_onoff);
        this.ringtone = (TextView) view.findViewById(R.id.choose_ringtone);
        this.editLabel = (TextView) view.findViewById(R.id.edit_label);
        this.repeatDays = (LinearLayout) view.findViewById(R.id.repeat_days);
        this.hairLine = view.findViewById(R.id.hairline);
        final Context context = view.getContext();
        view.setBackground(new LayerDrawable(new Drawable[]{ContextCompat.getDrawable(context, R.drawable.alarm_background_expanded), ThemeUtils.resolveDrawable(context, R.attr.selectableItemBackground)}));
        LayoutInflater layoutInflaterFrom = LayoutInflater.from(context);
        List<Integer> calendarDays = DataModel.getDataModel().getWeekdayOrder().getCalendarDays();
        for (int i = 0; i < 7; i++) {
            View viewInflate = layoutInflaterFrom.inflate(R.layout.day_button, (ViewGroup) this.repeatDays, false);
            CompoundButton compoundButton = (CompoundButton) viewInflate.findViewById(R.id.day_button_box);
            int iIntValue = calendarDays.get(i).intValue();
            compoundButton.setText(UiDataModel.getUiDataModel().getShortWeekday(iIntValue));
            compoundButton.setContentDescription(UiDataModel.getUiDataModel().getLongWeekday(iIntValue));
            this.repeatDays.addView(viewInflate);
            this.dayButtons[i] = compoundButton;
        }
        this.editLabel.setCompoundDrawablesRelativeWithIntrinsicBounds(Utils.getVectorDrawable(context, R.drawable.ic_label), (Drawable) null, (Drawable) null, (Drawable) null);
        this.delete.setCompoundDrawablesRelativeWithIntrinsicBounds(Utils.getVectorDrawable(context, R.drawable.ic_delete_small), (Drawable) null, (Drawable) null, (Drawable) null);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view2) {
                Events.sendAlarmEvent(R.string.action_collapse_implied, R.string.label_deskclock);
                ExpandedAlarmViewHolder.this.getItemHolder().collapse();
            }
        });
        this.arrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view2) {
                Events.sendAlarmEvent(R.string.action_collapse, R.string.label_deskclock);
                ExpandedAlarmViewHolder.this.getItemHolder().collapse();
            }
        });
        this.clock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view2) {
                ExpandedAlarmViewHolder.this.getAlarmTimeClickHandler().onClockClicked((Alarm) ExpandedAlarmViewHolder.this.getItemHolder().item);
            }
        });
        this.editLabel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view2) {
                ExpandedAlarmViewHolder.this.getAlarmTimeClickHandler().onEditLabelClicked((Alarm) ExpandedAlarmViewHolder.this.getItemHolder().item);
            }
        });
        this.vibrate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view2) {
                ExpandedAlarmViewHolder.this.getAlarmTimeClickHandler().setAlarmVibrationEnabled((Alarm) ExpandedAlarmViewHolder.this.getItemHolder().item, ((CheckBox) view2).isChecked());
            }
        });
        this.ringtone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view2) {
                ExpandedAlarmViewHolder.this.getAlarmTimeClickHandler().onRingtoneClicked(context, (Alarm) ExpandedAlarmViewHolder.this.getItemHolder().item);
            }
        });
        this.delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view2) {
                ExpandedAlarmViewHolder.this.getAlarmTimeClickHandler().onDeleteClicked(ExpandedAlarmViewHolder.this.getItemHolder());
                view2.announceForAccessibility(context.getString(R.string.alarm_deleted));
            }
        });
        this.repeat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view2) {
                ExpandedAlarmViewHolder.this.getAlarmTimeClickHandler().setAlarmRepeatEnabled((Alarm) ExpandedAlarmViewHolder.this.getItemHolder().item, ((CheckBox) view2).isChecked());
                ExpandedAlarmViewHolder.this.getItemHolder().notifyItemChanged(AlarmItemViewHolder.ANIMATE_REPEAT_DAYS);
            }
        });
        for (final int i2 = 0; i2 < this.dayButtons.length; i2++) {
            this.dayButtons[i2].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view2) {
                    ExpandedAlarmViewHolder.this.getAlarmTimeClickHandler().setDayOfWeekEnabled((Alarm) ExpandedAlarmViewHolder.this.getItemHolder().item, ((CompoundButton) view2).isChecked(), i2);
                }
            });
        }
        view.setImportantForAccessibility(2);
    }

    @Override
    protected void onBindItemView(AlarmItemHolder alarmItemHolder) {
        super.onBindItemView(alarmItemHolder);
        Alarm alarm = (Alarm) alarmItemHolder.item;
        AlarmInstance alarmInstance = alarmItemHolder.getAlarmInstance();
        Context context = this.itemView.getContext();
        bindEditLabel(context, alarm);
        bindDaysOfWeekButtons(alarm, context);
        bindVibrator(alarm);
        bindRingtone(context, alarm);
        bindPreemptiveDismissButton(context, alarm, alarmInstance);
    }

    private void bindRingtone(Context context, Alarm alarm) {
        String ringtoneTitle = DataModel.getDataModel().getRingtoneTitle(alarm.alert);
        this.ringtone.setText(ringtoneTitle);
        String string = context.getString(R.string.ringtone_description);
        this.ringtone.setContentDescription(string + " " + ringtoneTitle);
        this.ringtone.setCompoundDrawablesRelativeWithIntrinsicBounds(Utils.getVectorDrawable(context, Utils.RINGTONE_SILENT.equals(alarm.alert) ? R.drawable.ic_ringtone_silent : R.drawable.ic_ringtone), (Drawable) null, (Drawable) null, (Drawable) null);
    }

    private void bindDaysOfWeekButtons(Alarm alarm, Context context) {
        List<Integer> calendarDays = DataModel.getDataModel().getWeekdayOrder().getCalendarDays();
        for (int i = 0; i < calendarDays.size(); i++) {
            CompoundButton compoundButton = this.dayButtons[i];
            if (alarm.daysOfWeek.isBitOn(calendarDays.get(i).intValue())) {
                compoundButton.setChecked(true);
                compoundButton.setTextColor(ThemeUtils.resolveColor(context, android.R.attr.windowBackground));
            } else {
                compoundButton.setChecked(false);
                compoundButton.setTextColor(-1);
            }
        }
        if (!alarm.daysOfWeek.isRepeating()) {
            this.repeat.setChecked(false);
            this.repeatDays.setVisibility(8);
        } else {
            this.repeat.setChecked(true);
            this.repeatDays.setVisibility(0);
        }
    }

    private void bindEditLabel(Context context, Alarm alarm) {
        String string;
        this.editLabel.setText(alarm.label);
        TextView textView = this.editLabel;
        if (alarm.label != null && alarm.label.length() > 0) {
            string = context.getString(R.string.label_description) + " " + alarm.label;
        } else {
            string = context.getString(R.string.no_label_specified);
        }
        textView.setContentDescription(string);
    }

    private void bindVibrator(Alarm alarm) {
        if (!this.mHasVibrator) {
            this.vibrate.setVisibility(4);
        } else {
            this.vibrate.setVisibility(0);
            this.vibrate.setChecked(alarm.vibrate);
        }
    }

    private AlarmTimeClickHandler getAlarmTimeClickHandler() {
        return getItemHolder().getAlarmTimeClickHandler();
    }

    @Override
    public Animator onAnimateChange(List<Object> list, int i, int i2, int i3, int i4, long j) {
        if (list == null || list.isEmpty() || !list.contains(AlarmItemViewHolder.ANIMATE_REPEAT_DAYS)) {
            return null;
        }
        final boolean z = this.repeatDays.getVisibility() == 0;
        setTranslationY(z ? -r4 : 0.0f, z ? -r4 : this.repeatDays.getHeight());
        this.repeatDays.setVisibility(0);
        this.repeatDays.setAlpha(z ? 0.0f : 1.0f);
        AnimatorSet animatorSet = new AnimatorSet();
        Animator[] animatorArr = new Animator[10];
        animatorArr[0] = AnimatorUtils.getBoundsAnimator(this.itemView, i, i2, i3, i4, this.itemView.getLeft(), this.itemView.getTop(), this.itemView.getRight(), this.itemView.getBottom());
        LinearLayout linearLayout = this.repeatDays;
        Property property = View.ALPHA;
        float[] fArr = new float[1];
        fArr[0] = z ? 1.0f : 0.0f;
        animatorArr[1] = ObjectAnimator.ofFloat(linearLayout, (Property<LinearLayout, Float>) property, fArr);
        LinearLayout linearLayout2 = this.repeatDays;
        Property property2 = View.TRANSLATION_Y;
        float[] fArr2 = new float[1];
        fArr2[0] = z ? 0.0f : -r4;
        animatorArr[2] = ObjectAnimator.ofFloat(linearLayout2, (Property<LinearLayout, Float>) property2, fArr2);
        animatorArr[3] = ObjectAnimator.ofFloat(this.ringtone, (Property<TextView, Float>) View.TRANSLATION_Y, 0.0f);
        animatorArr[4] = ObjectAnimator.ofFloat(this.vibrate, (Property<CheckBox, Float>) View.TRANSLATION_Y, 0.0f);
        animatorArr[5] = ObjectAnimator.ofFloat(this.editLabel, (Property<TextView, Float>) View.TRANSLATION_Y, 0.0f);
        animatorArr[6] = ObjectAnimator.ofFloat(this.preemptiveDismissButton, (Property<TextView, Float>) View.TRANSLATION_Y, 0.0f);
        animatorArr[7] = ObjectAnimator.ofFloat(this.hairLine, (Property<View, Float>) View.TRANSLATION_Y, 0.0f);
        animatorArr[8] = ObjectAnimator.ofFloat(this.delete, (Property<TextView, Float>) View.TRANSLATION_Y, 0.0f);
        animatorArr[9] = ObjectAnimator.ofFloat(this.arrow, (Property<ImageView, Float>) View.TRANSLATION_Y, 0.0f);
        animatorSet.playTogether(animatorArr);
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                ExpandedAlarmViewHolder.this.setTranslationY(0.0f, 0.0f);
                ExpandedAlarmViewHolder.this.repeatDays.setAlpha(1.0f);
                ExpandedAlarmViewHolder.this.repeatDays.setVisibility(z ? 0 : 8);
                ExpandedAlarmViewHolder.this.itemView.requestLayout();
            }
        });
        animatorSet.setDuration(j);
        animatorSet.setInterpolator(AnimatorUtils.INTERPOLATOR_FAST_OUT_SLOW_IN);
        return animatorSet;
    }

    private void setTranslationY(float f, float f2) {
        this.repeatDays.setTranslationY(f);
        this.ringtone.setTranslationY(f2);
        this.vibrate.setTranslationY(f2);
        this.editLabel.setTranslationY(f2);
        this.preemptiveDismissButton.setTranslationY(f2);
        this.hairLine.setTranslationY(f2);
        this.delete.setTranslationY(f2);
        this.arrow.setTranslationY(f2);
    }

    @Override
    public Animator onAnimateChange(RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder viewHolder2, long j) {
        Animator animatorCreateCollapsingAnimator;
        if (!(viewHolder instanceof AlarmItemViewHolder) || !(viewHolder2 instanceof AlarmItemViewHolder)) {
            return null;
        }
        boolean z = this == viewHolder2;
        AnimatorUtils.setBackgroundAlpha(this.itemView, Integer.valueOf(z ? 0 : 255));
        setChangingViewsAlpha(z ? 0.0f : 1.0f);
        if (z) {
            animatorCreateCollapsingAnimator = createExpandingAnimator((AlarmItemViewHolder) viewHolder, j);
        } else {
            animatorCreateCollapsingAnimator = createCollapsingAnimator((AlarmItemViewHolder) viewHolder2, j);
        }
        animatorCreateCollapsingAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                AnimatorUtils.setBackgroundAlpha(ExpandedAlarmViewHolder.this.itemView, 255);
                ExpandedAlarmViewHolder.this.clock.setVisibility(0);
                ExpandedAlarmViewHolder.this.onOff.setVisibility(0);
                ExpandedAlarmViewHolder.this.arrow.setVisibility(0);
                ExpandedAlarmViewHolder.this.arrow.setTranslationY(0.0f);
                ExpandedAlarmViewHolder.this.setChangingViewsAlpha(1.0f);
                ExpandedAlarmViewHolder.this.arrow.jumpDrawablesToCurrentState();
            }
        });
        return animatorCreateCollapsingAnimator;
    }

    private Animator createCollapsingAnimator(AlarmItemViewHolder alarmItemViewHolder, long j) {
        ObjectAnimator objectAnimator;
        this.arrow.setVisibility(4);
        this.clock.setVisibility(4);
        this.onOff.setVisibility(4);
        boolean z = this.repeatDays.getVisibility() == 0;
        int iCountNumberOfItems = countNumberOfItems();
        View view = this.itemView;
        View view2 = alarmItemViewHolder.itemView;
        ObjectAnimator objectAnimatorOfPropertyValuesHolder = ObjectAnimator.ofPropertyValuesHolder(view, PropertyValuesHolder.ofInt(AnimatorUtils.BACKGROUND_ALPHA, 255, 0));
        objectAnimatorOfPropertyValuesHolder.setDuration(j);
        Animator boundsAnimator = AnimatorUtils.getBoundsAnimator(view, view, view2);
        boundsAnimator.setDuration(j);
        boundsAnimator.setInterpolator(AnimatorUtils.INTERPOLATOR_FAST_OUT_SLOW_IN);
        float f = j;
        long j2 = (long) (0.25f * f);
        ObjectAnimator duration = ObjectAnimator.ofFloat(this.repeat, (Property<CheckBox, Float>) View.ALPHA, 0.0f).setDuration(j2);
        ObjectAnimator duration2 = ObjectAnimator.ofFloat(this.editLabel, (Property<TextView, Float>) View.ALPHA, 0.0f).setDuration(j2);
        ObjectAnimator duration3 = ObjectAnimator.ofFloat(this.repeatDays, (Property<LinearLayout, Float>) View.ALPHA, 0.0f).setDuration(j2);
        ObjectAnimator duration4 = ObjectAnimator.ofFloat(this.vibrate, (Property<CheckBox, Float>) View.ALPHA, 0.0f).setDuration(j2);
        ObjectAnimator duration5 = ObjectAnimator.ofFloat(this.ringtone, (Property<TextView, Float>) View.ALPHA, 0.0f).setDuration(j2);
        ObjectAnimator duration6 = ObjectAnimator.ofFloat(this.preemptiveDismissButton, (Property<TextView, Float>) View.ALPHA, 0.0f).setDuration(j2);
        ObjectAnimator duration7 = ObjectAnimator.ofFloat(this.delete, (Property<TextView, Float>) View.ALPHA, 0.0f).setDuration(j2);
        boolean z2 = z;
        ObjectAnimator duration8 = ObjectAnimator.ofFloat(this.hairLine, (Property<View, Float>) View.ALPHA, 0.0f).setDuration(j2);
        long j3 = 0;
        long j4 = ((long) (f * 0.5833333f)) / ((long) (iCountNumberOfItems - 1));
        duration7.setStartDelay(0L);
        if (this.preemptiveDismissButton.getVisibility() == 0) {
            j3 = 0 + j4;
            duration6.setStartDelay(j3);
        }
        duration8.setStartDelay(j3);
        long j5 = j3 + j4;
        duration2.setStartDelay(j5);
        long j6 = j5 + j4;
        duration4.setStartDelay(j6);
        duration5.setStartDelay(j6);
        long j7 = j6 + j4;
        if (z2) {
            objectAnimator = duration3;
            objectAnimator.setStartDelay(j7);
            j7 += j4;
        } else {
            objectAnimator = duration3;
        }
        duration.setStartDelay(j7);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(objectAnimatorOfPropertyValuesHolder, boundsAnimator, duration, objectAnimator, duration4, duration5, duration2, duration7, duration8, duration6);
        return animatorSet;
    }

    private Animator createExpandingAnimator(AlarmItemViewHolder alarmItemViewHolder, long j) {
        ObjectAnimator objectAnimator;
        View view = alarmItemViewHolder.itemView;
        View view2 = this.itemView;
        Animator boundsAnimator = AnimatorUtils.getBoundsAnimator(view2, view, view2);
        boundsAnimator.setDuration(j);
        boundsAnimator.setInterpolator(AnimatorUtils.INTERPOLATOR_FAST_OUT_SLOW_IN);
        ObjectAnimator objectAnimatorOfPropertyValuesHolder = ObjectAnimator.ofPropertyValuesHolder(view2, PropertyValuesHolder.ofInt(AnimatorUtils.BACKGROUND_ALPHA, 0, 255));
        objectAnimatorOfPropertyValuesHolder.setDuration(j);
        ImageView imageView = alarmItemViewHolder.arrow;
        Rect rect = new Rect(0, 0, imageView.getWidth(), imageView.getHeight());
        ((ViewGroup) view2).offsetDescendantRectToMyCoords(this.arrow, new Rect(0, 0, this.arrow.getWidth(), this.arrow.getHeight()));
        ((ViewGroup) view).offsetDescendantRectToMyCoords(imageView, rect);
        this.arrow.setTranslationY(rect.bottom - r12.bottom);
        this.arrow.setVisibility(0);
        this.clock.setVisibility(0);
        this.onOff.setVisibility(0);
        float f = j;
        long j2 = (long) (0.6666667f * f);
        ObjectAnimator duration = ObjectAnimator.ofFloat(this.repeat, (Property<CheckBox, Float>) View.ALPHA, 1.0f).setDuration(j2);
        ObjectAnimator duration2 = ObjectAnimator.ofFloat(this.repeatDays, (Property<LinearLayout, Float>) View.ALPHA, 1.0f).setDuration(j2);
        ObjectAnimator duration3 = ObjectAnimator.ofFloat(this.ringtone, (Property<TextView, Float>) View.ALPHA, 1.0f).setDuration(j2);
        ObjectAnimator duration4 = ObjectAnimator.ofFloat(this.preemptiveDismissButton, (Property<TextView, Float>) View.ALPHA, 1.0f).setDuration(j2);
        ObjectAnimator duration5 = ObjectAnimator.ofFloat(this.vibrate, (Property<CheckBox, Float>) View.ALPHA, 1.0f).setDuration(j2);
        ObjectAnimator duration6 = ObjectAnimator.ofFloat(this.editLabel, (Property<TextView, Float>) View.ALPHA, 1.0f).setDuration(j2);
        ObjectAnimator duration7 = ObjectAnimator.ofFloat(this.hairLine, (Property<View, Float>) View.ALPHA, 1.0f).setDuration(j2);
        ObjectAnimator duration8 = ObjectAnimator.ofFloat(this.delete, (Property<TextView, Float>) View.ALPHA, 1.0f).setDuration(j2);
        ObjectAnimator duration9 = ObjectAnimator.ofFloat(this.arrow, (Property<ImageView, Float>) View.TRANSLATION_Y, 0.0f).setDuration(j);
        duration9.setInterpolator(AnimatorUtils.INTERPOLATOR_FAST_OUT_SLOW_IN);
        long j3 = (long) (0.16666667f * f);
        long jCountNumberOfItems = ((long) (f * 0.08333331f)) / ((long) (countNumberOfItems() - 1));
        duration.setStartDelay(j3);
        long j4 = j3 + jCountNumberOfItems;
        if (this.repeatDays.getVisibility() == 0) {
            duration2.setStartDelay(j4);
            j4 += jCountNumberOfItems;
        }
        duration3.setStartDelay(j4);
        duration5.setStartDelay(j4);
        long j5 = j4 + jCountNumberOfItems;
        duration6.setStartDelay(j5);
        long j6 = j5 + jCountNumberOfItems;
        duration7.setStartDelay(j6);
        if (this.preemptiveDismissButton.getVisibility() == 0) {
            objectAnimator = duration4;
            objectAnimator.setStartDelay(j6);
            j6 += jCountNumberOfItems;
        } else {
            objectAnimator = duration4;
        }
        duration8.setStartDelay(j6);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(objectAnimatorOfPropertyValuesHolder, duration, boundsAnimator, duration2, duration5, duration3, duration6, duration8, duration7, objectAnimator, duration9);
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animator) {
                AnimatorUtils.startDrawableAnimation(ExpandedAlarmViewHolder.this.arrow);
            }
        });
        return animatorSet;
    }

    private int countNumberOfItems() {
        int i;
        if (this.preemptiveDismissButton.getVisibility() == 0) {
            i = 5;
        } else {
            i = 4;
        }
        if (this.repeatDays.getVisibility() == 0) {
            return i + 1;
        }
        return i;
    }

    private void setChangingViewsAlpha(float f) {
        this.repeat.setAlpha(f);
        this.editLabel.setAlpha(f);
        this.repeatDays.setAlpha(f);
        this.vibrate.setAlpha(f);
        this.ringtone.setAlpha(f);
        this.hairLine.setAlpha(f);
        this.delete.setAlpha(f);
        this.preemptiveDismissButton.setAlpha(f);
    }

    public static class Factory implements ItemAdapter.ItemViewHolder.Factory {
        private final boolean mHasVibrator;
        private final LayoutInflater mLayoutInflater;

        public Factory(Context context) {
            this.mLayoutInflater = LayoutInflater.from(context);
            this.mHasVibrator = ((Vibrator) context.getSystemService("vibrator")).hasVibrator();
        }

        @Override
        public ItemAdapter.ItemViewHolder<?> createViewHolder(ViewGroup viewGroup, int i) {
            return new ExpandedAlarmViewHolder(this.mLayoutInflater.inflate(i, viewGroup, false), this.mHasVibrator);
        }
    }
}
