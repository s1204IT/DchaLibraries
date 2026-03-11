package com.android.settings.notification;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.SeekBarPreference;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;

public class ImportanceSeekBarPreference extends SeekBarPreference implements SeekBar.OnSeekBarChangeListener {
    private float mActiveSliderAlpha;
    private ColorStateList mActiveSliderTint;
    private boolean mAutoOn;
    private Callback mCallback;
    private Handler mHandler;
    private float mInactiveSliderAlpha;
    private ColorStateList mInactiveSliderTint;
    private int mMinProgress;
    private final Runnable mNotifyChanged;
    private SeekBar mSeekBar;
    private String mSummary;
    private TextView mSummaryTextView;

    public interface Callback {
        void onImportanceChanged(int i, boolean z);
    }

    public ImportanceSeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mActiveSliderAlpha = 1.0f;
        this.mNotifyChanged = new Runnable() {
            @Override
            public void run() {
                ImportanceSeekBarPreference.this.postNotifyChanged();
            }
        };
        setLayoutResource(R.layout.preference_importance_slider);
        this.mActiveSliderTint = ColorStateList.valueOf(context.getColor(R.color.importance_slider_color));
        this.mInactiveSliderTint = ColorStateList.valueOf(context.getColor(R.color.importance_disabled_slider_color));
        this.mHandler = new Handler();
        TypedArray ta = context.obtainStyledAttributes(attrs, com.android.internal.R.styleable.Theme, 0, 0);
        this.mInactiveSliderAlpha = ta.getFloat(3, 0.5f);
        ta.recycle();
    }

    public ImportanceSeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ImportanceSeekBarPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ImportanceSeekBarPreference(Context context) {
        this(context, null);
    }

    public void setCallback(Callback callback) {
        this.mCallback = callback;
    }

    public void setMinimumProgress(int minProgress) {
        this.mMinProgress = minProgress;
        notifyChanged();
    }

    @Override
    public void setProgress(int progress) {
        this.mSummary = getProgressSummary(progress);
        super.setProgress(progress);
    }

    public void setAutoOn(boolean autoOn) {
        this.mAutoOn = autoOn;
        notifyChanged();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        this.mSummaryTextView = (TextView) view.findViewById(android.R.id.summary);
        this.mSeekBar = (SeekBar) view.findViewById(android.R.id.locked);
        final ImageView autoButton = (ImageView) view.findViewById(R.id.auto_importance);
        applyAutoUi(autoButton);
        autoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ImportanceSeekBarPreference.this.applyAuto(autoButton);
            }
        });
    }

    public void applyAuto(ImageView autoButton) {
        this.mAutoOn = !this.mAutoOn;
        if (!this.mAutoOn) {
            setProgress(3);
            this.mCallback.onImportanceChanged(3, true);
        } else {
            this.mCallback.onImportanceChanged(-1000, true);
        }
        applyAutoUi(autoButton);
    }

    private void applyAutoUi(ImageView autoButton) {
        this.mSeekBar.setEnabled(!this.mAutoOn);
        float alpha = this.mAutoOn ? this.mInactiveSliderAlpha : this.mActiveSliderAlpha;
        ColorStateList starTint = this.mAutoOn ? this.mActiveSliderTint : this.mInactiveSliderTint;
        Drawable icon = autoButton.getDrawable().mutate();
        icon.setTintList(starTint);
        autoButton.setImageDrawable(icon);
        this.mSeekBar.setAlpha(alpha);
        if (this.mAutoOn) {
            setProgress(3);
            this.mSummary = getProgressSummary(-1000);
        }
        this.mSummaryTextView.setText(this.mSummary);
    }

    @Override
    public CharSequence getSummary() {
        return this.mSummary;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
        super.onProgressChanged(seekBar, progress, fromTouch);
        if (progress < this.mMinProgress) {
            seekBar.setProgress(this.mMinProgress);
            progress = this.mMinProgress;
        }
        if (this.mSummaryTextView != null) {
            this.mSummary = getProgressSummary(progress);
            this.mSummaryTextView.setText(this.mSummary);
        }
        this.mCallback.onImportanceChanged(progress, fromTouch);
    }

    private String getProgressSummary(int progress) {
        switch (progress) {
            case DefaultWfcSettingsExt.RESUME:
                return getContext().getString(R.string.notification_importance_blocked);
            case DefaultWfcSettingsExt.PAUSE:
                return getContext().getString(R.string.notification_importance_min);
            case DefaultWfcSettingsExt.CREATE:
                return getContext().getString(R.string.notification_importance_low);
            case DefaultWfcSettingsExt.DESTROY:
                return getContext().getString(R.string.notification_importance_default);
            case DefaultWfcSettingsExt.CONFIG_CHANGE:
                return getContext().getString(R.string.notification_importance_high);
            case 5:
                return getContext().getString(R.string.notification_importance_max);
            default:
                return getContext().getString(R.string.notification_importance_unspecified);
        }
    }

    @Override
    protected void notifyChanged() {
        this.mHandler.post(this.mNotifyChanged);
    }

    public void postNotifyChanged() {
        super.notifyChanged();
    }
}
