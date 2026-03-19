package com.android.settingslib.notification;

import android.app.ActivityManager;
import android.content.Context;
import android.provider.Settings;
import android.service.notification.ZenModeConfig;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.policy.PhoneWindow;
import com.android.settingslib.R;
import java.util.Arrays;

public class ZenDurationDialog {
    protected static final int ALWAYS_ASK_CONDITION_INDEX = 2;
    protected static final int COUNTDOWN_CONDITION_INDEX = 1;
    protected static final int FOREVER_CONDITION_INDEX = 0;
    private int MAX_MANUAL_DND_OPTIONS;
    protected int mBucketIndex;
    protected Context mContext;
    protected LayoutInflater mLayoutInflater;
    private RadioGroup mZenRadioGroup;
    protected LinearLayout mZenRadioGroupContent;
    private static final int[] MINUTE_BUCKETS = ZenModeConfig.MINUTE_BUCKETS;
    protected static final int MIN_BUCKET_MINUTES = MINUTE_BUCKETS[0];
    protected static final int MAX_BUCKET_MINUTES = MINUTE_BUCKETS[MINUTE_BUCKETS.length - 1];
    private static final int DEFAULT_BUCKET_INDEX = Arrays.binarySearch(MINUTE_BUCKETS, 60);

    protected void updateZenDuration(int i) {
        int checkedRadioButtonId = this.mZenRadioGroup.getCheckedRadioButtonId();
        int i2 = 0;
        int i3 = Settings.Global.getInt(this.mContext.getContentResolver(), "zen_duration", 0);
        switch (checkedRadioButtonId) {
            case 0:
                MetricsLogger.action(this.mContext, 1343);
                break;
            case COUNTDOWN_CONDITION_INDEX:
                i2 = getConditionTagAt(checkedRadioButtonId).countdownZenDuration;
                MetricsLogger.action(this.mContext, 1342, i2);
                break;
            case ALWAYS_ASK_CONDITION_INDEX:
                i2 = -1;
                MetricsLogger.action(this.mContext, 1344);
                break;
            default:
                i2 = i3;
                break;
        }
        if (i != i2) {
            Settings.Global.putInt(this.mContext.getContentResolver(), "zen_duration", i2);
        }
    }

    protected View getContentView() {
        if (this.mLayoutInflater == null) {
            this.mLayoutInflater = new PhoneWindow(this.mContext).getLayoutInflater();
        }
        View viewInflate = this.mLayoutInflater.inflate(R.layout.zen_mode_duration_dialog, (ViewGroup) null);
        ScrollView scrollView = (ScrollView) viewInflate.findViewById(R.id.zen_duration_container);
        this.mZenRadioGroup = (RadioGroup) scrollView.findViewById(R.id.zen_radio_buttons);
        this.mZenRadioGroupContent = (LinearLayout) scrollView.findViewById(R.id.zen_radio_buttons_content);
        for (int i = 0; i < this.MAX_MANUAL_DND_OPTIONS; i += COUNTDOWN_CONDITION_INDEX) {
            View viewInflate2 = this.mLayoutInflater.inflate(R.layout.zen_mode_radio_button, (ViewGroup) this.mZenRadioGroup, false);
            this.mZenRadioGroup.addView(viewInflate2);
            viewInflate2.setId(i);
            View viewInflate3 = this.mLayoutInflater.inflate(R.layout.zen_mode_condition, (ViewGroup) this.mZenRadioGroupContent, false);
            viewInflate3.setId(this.MAX_MANUAL_DND_OPTIONS + i);
            this.mZenRadioGroupContent.addView(viewInflate3);
        }
        return viewInflate;
    }

    protected void setupRadioButtons(int i) {
        int i2 = i == 0 ? 0 : i > 0 ? COUNTDOWN_CONDITION_INDEX : ALWAYS_ASK_CONDITION_INDEX;
        bindTag(i, this.mZenRadioGroupContent.getChildAt(0), 0);
        bindTag(i, this.mZenRadioGroupContent.getChildAt(COUNTDOWN_CONDITION_INDEX), COUNTDOWN_CONDITION_INDEX);
        bindTag(i, this.mZenRadioGroupContent.getChildAt(ALWAYS_ASK_CONDITION_INDEX), ALWAYS_ASK_CONDITION_INDEX);
        getConditionTagAt(i2).rb.setChecked(true);
    }

    private void bindTag(int i, View view, int i2) {
        final ConditionTag conditionTag = view.getTag() != null ? (ConditionTag) view.getTag() : new ConditionTag();
        view.setTag(conditionTag);
        if (conditionTag.rb == null) {
            conditionTag.rb = (RadioButton) this.mZenRadioGroup.getChildAt(i2);
        }
        if (i <= 0) {
            conditionTag.countdownZenDuration = MINUTE_BUCKETS[DEFAULT_BUCKET_INDEX];
        } else {
            conditionTag.countdownZenDuration = i;
        }
        conditionTag.rb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                if (z) {
                    conditionTag.rb.setChecked(true);
                }
            }
        });
        updateUi(conditionTag, view, i2);
    }

    protected ConditionTag getConditionTagAt(int i) {
        return (ConditionTag) this.mZenRadioGroupContent.getChildAt(i).getTag();
    }

    private void setupUi(final ConditionTag conditionTag, View view) {
        if (conditionTag.lines == null) {
            conditionTag.lines = view.findViewById(android.R.id.content);
            conditionTag.lines.setAccessibilityLiveRegion(COUNTDOWN_CONDITION_INDEX);
        }
        if (conditionTag.line1 == null) {
            conditionTag.line1 = (TextView) view.findViewById(android.R.id.text1);
        }
        view.findViewById(android.R.id.text2).setVisibility(8);
        conditionTag.lines.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view2) {
                conditionTag.rb.setChecked(true);
            }
        });
    }

    private void updateButtons(final ConditionTag conditionTag, final View view, final int i) {
        boolean z;
        ImageView imageView = (ImageView) view.findViewById(android.R.id.button1);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view2) {
                ZenDurationDialog.this.onClickTimeButton(view, conditionTag, false, i);
            }
        });
        ImageView imageView2 = (ImageView) view.findViewById(android.R.id.button2);
        imageView2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view2) {
                ZenDurationDialog.this.onClickTimeButton(view, conditionTag, true, i);
            }
        });
        long j = conditionTag.countdownZenDuration;
        boolean z2 = true;
        if (i == COUNTDOWN_CONDITION_INDEX) {
            imageView.setVisibility(0);
            imageView2.setVisibility(0);
            if (j > MIN_BUCKET_MINUTES) {
                z = COUNTDOWN_CONDITION_INDEX;
            } else {
                z = false;
            }
            imageView.setEnabled(z);
            if (conditionTag.countdownZenDuration == MAX_BUCKET_MINUTES) {
                z2 = false;
            }
            imageView2.setEnabled(z2);
            imageView.setAlpha(imageView.isEnabled() ? 1.0f : 0.5f);
            imageView2.setAlpha(imageView2.isEnabled() ? 1.0f : 0.5f);
            return;
        }
        imageView.setVisibility(8);
        imageView2.setVisibility(8);
    }

    protected void updateUi(ConditionTag conditionTag, View view, int i) {
        if (conditionTag.lines == null) {
            setupUi(conditionTag, view);
        }
        updateButtons(conditionTag, view, i);
        String string = "";
        switch (i) {
            case 0:
                string = this.mContext.getString(android.R.string.one_handed_mode_feature_name);
                break;
            case COUNTDOWN_CONDITION_INDEX:
                string = ZenModeConfig.toTimeCondition(this.mContext, conditionTag.countdownZenDuration, ActivityManager.getCurrentUser(), false).line1;
                break;
            case ALWAYS_ASK_CONDITION_INDEX:
                string = this.mContext.getString(R.string.zen_mode_duration_always_prompt_title);
                break;
        }
        conditionTag.line1.setText(string);
    }

    protected void onClickTimeButton(View view, ConditionTag conditionTag, boolean z, int i) {
        int i2;
        int i3;
        int length = MINUTE_BUCKETS.length;
        if (this.mBucketIndex == -1) {
            long j = conditionTag.countdownZenDuration;
            for (int i4 = 0; i4 < length; i4 += COUNTDOWN_CONDITION_INDEX) {
                if (!z) {
                    i3 = (length - 1) - i4;
                } else {
                    i3 = i4;
                }
                i2 = MINUTE_BUCKETS[i3];
                if ((z && i2 > j) || (!z && i2 < j)) {
                    this.mBucketIndex = i3;
                    break;
                }
            }
            i2 = -1;
            if (i2 == -1) {
                this.mBucketIndex = DEFAULT_BUCKET_INDEX;
                i2 = MINUTE_BUCKETS[this.mBucketIndex];
            }
        } else {
            this.mBucketIndex = Math.max(0, Math.min(length - COUNTDOWN_CONDITION_INDEX, this.mBucketIndex + (z ? COUNTDOWN_CONDITION_INDEX : -1)));
            i2 = MINUTE_BUCKETS[this.mBucketIndex];
        }
        conditionTag.countdownZenDuration = i2;
        bindTag(i2, view, i);
        conditionTag.rb.setChecked(true);
    }

    protected static class ConditionTag {
        public int countdownZenDuration;
        public TextView line1;
        public View lines;
        public RadioButton rb;

        protected ConditionTag() {
        }
    }
}
