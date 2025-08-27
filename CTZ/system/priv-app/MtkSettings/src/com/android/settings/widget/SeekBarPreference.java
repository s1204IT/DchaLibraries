package com.android.settings.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.content.res.TypedArrayUtils;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.SeekBar;
import com.android.internal.R;
import com.android.settingslib.RestrictedPreference;

/* loaded from: classes.dex */
public class SeekBarPreference extends RestrictedPreference implements View.OnKeyListener, SeekBar.OnSeekBarChangeListener {
    private int mAccessibilityRangeInfoType;
    private boolean mContinuousUpdates;
    private int mDefaultProgress;
    private int mMax;
    private int mMin;
    private int mProgress;
    private SeekBar mSeekBar;
    private CharSequence mSeekBarContentDescription;
    private boolean mShouldBlink;
    private boolean mTrackingTouch;

    public SeekBarPreference(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mDefaultProgress = -1;
        this.mAccessibilityRangeInfoType = 0;
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.ProgressBar, i, i2);
        setMax(typedArrayObtainStyledAttributes.getInt(2, this.mMax));
        setMin(typedArrayObtainStyledAttributes.getInt(26, this.mMin));
        typedArrayObtainStyledAttributes.recycle();
        TypedArray typedArrayObtainStyledAttributes2 = context.obtainStyledAttributes(attributeSet, R.styleable.SeekBarPreference, i, i2);
        int resourceId = typedArrayObtainStyledAttributes2.getResourceId(0, android.R.layout.notification_template_material_inbox);
        typedArrayObtainStyledAttributes2.recycle();
        setLayoutResource(resourceId);
    }

    public SeekBarPreference(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public SeekBarPreference(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, TypedArrayUtils.getAttr(context, com.android.settings.R.attr.seekBarPreferenceStyle, android.R.bool.config_cecQuerySadMpeg1Disabled_allowed));
    }

    public SeekBarPreference(Context context) {
        this(context, null);
    }

    @Override // com.android.settingslib.RestrictedPreference, com.android.settingslib.TwoTargetPreference, android.support.v7.preference.Preference
    public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
        super.onBindViewHolder(preferenceViewHolder);
        preferenceViewHolder.itemView.setOnKeyListener(this);
        this.mSeekBar = (SeekBar) preferenceViewHolder.findViewById(android.R.id.mode_in);
        this.mSeekBar.setOnSeekBarChangeListener(this);
        this.mSeekBar.setMax(this.mMax);
        this.mSeekBar.setMin(this.mMin);
        this.mSeekBar.setProgress(this.mProgress);
        this.mSeekBar.setEnabled(isEnabled());
        CharSequence title = getTitle();
        if (!TextUtils.isEmpty(this.mSeekBarContentDescription)) {
            this.mSeekBar.setContentDescription(this.mSeekBarContentDescription);
        } else if (!TextUtils.isEmpty(title)) {
            this.mSeekBar.setContentDescription(title);
        }
        if (this.mSeekBar instanceof DefaultIndicatorSeekBar) {
            ((DefaultIndicatorSeekBar) this.mSeekBar).setDefaultProgress(this.mDefaultProgress);
        }
        if (this.mShouldBlink) {
            final View view = preferenceViewHolder.itemView;
            view.post(new Runnable() { // from class: com.android.settings.widget.-$$Lambda$SeekBarPreference$dLBfCJMqk38mmQ3tQY-pIyDA0S8
                @Override // java.lang.Runnable
                public final void run() {
                    SeekBarPreference.lambda$onBindViewHolder$0(this.f$0, view);
                }
            });
        }
        this.mSeekBar.setAccessibilityDelegate(new View.AccessibilityDelegate() { // from class: com.android.settings.widget.SeekBarPreference.1
            @Override // android.view.View.AccessibilityDelegate
            public void onInitializeAccessibilityNodeInfo(View view2, AccessibilityNodeInfo accessibilityNodeInfo) {
                super.onInitializeAccessibilityNodeInfo(view2, accessibilityNodeInfo);
                AccessibilityNodeInfo.RangeInfo rangeInfo = accessibilityNodeInfo.getRangeInfo();
                if (rangeInfo != null) {
                    accessibilityNodeInfo.setRangeInfo(AccessibilityNodeInfo.RangeInfo.obtain(SeekBarPreference.this.mAccessibilityRangeInfoType, rangeInfo.getMin(), rangeInfo.getMax(), rangeInfo.getCurrent()));
                }
            }
        });
    }

    public static /* synthetic */ void lambda$onBindViewHolder$0(SeekBarPreference seekBarPreference, View view) {
        if (view.getBackground() != null) {
            view.getBackground().setHotspot(view.getWidth() / 2, view.getHeight() / 2);
        }
        view.setPressed(true);
        view.setPressed(false);
        seekBarPreference.mShouldBlink = false;
    }

    @Override // android.support.v7.preference.Preference
    public CharSequence getSummary() {
        return null;
    }

    @Override // android.support.v7.preference.Preference
    protected void onSetInitialValue(boolean z, Object obj) {
        setProgress(z ? getPersistedInt(this.mProgress) : ((Integer) obj).intValue());
    }

    @Override // android.support.v7.preference.Preference
    protected Object onGetDefaultValue(TypedArray typedArray, int i) {
        return Integer.valueOf(typedArray.getInt(i, 0));
    }

    @Override // android.view.View.OnKeyListener
    public boolean onKey(View view, int i, KeyEvent keyEvent) {
        SeekBar seekBar;
        if (keyEvent.getAction() == 0 && (seekBar = (SeekBar) view.findViewById(android.R.id.mode_in)) != null) {
            return seekBar.onKeyDown(i, keyEvent);
        }
        return false;
    }

    public void setMax(int i) {
        if (i != this.mMax) {
            this.mMax = i;
            notifyChanged();
        }
    }

    public void setMin(int i) {
        if (i != this.mMin) {
            this.mMin = i;
            notifyChanged();
        }
    }

    public int getMax() {
        return this.mMax;
    }

    public int getMin() {
        return this.mMin;
    }

    public void setProgress(int i) {
        setProgress(i, true);
    }

    public void setContinuousUpdates(boolean z) {
        this.mContinuousUpdates = z;
    }

    private void setProgress(int i, boolean z) {
        if (i > this.mMax) {
            i = this.mMax;
        }
        if (i < this.mMin) {
            i = this.mMin;
        }
        if (i != this.mProgress) {
            this.mProgress = i;
            persistInt(i);
            if (z) {
                notifyChanged();
            }
        }
    }

    public int getProgress() {
        return this.mProgress;
    }

    void syncProgress(SeekBar seekBar) {
        int progress = seekBar.getProgress();
        if (progress != this.mProgress) {
            if (callChangeListener(Integer.valueOf(progress))) {
                setProgress(progress, false);
            } else {
                seekBar.setProgress(this.mProgress);
            }
        }
    }

    @Override // android.widget.SeekBar.OnSeekBarChangeListener
    public void onProgressChanged(SeekBar seekBar, int i, boolean z) {
        if (z) {
            if (this.mContinuousUpdates || !this.mTrackingTouch) {
                syncProgress(seekBar);
            }
        }
    }

    @Override // android.widget.SeekBar.OnSeekBarChangeListener
    public void onStartTrackingTouch(SeekBar seekBar) {
        this.mTrackingTouch = true;
    }

    @Override // android.widget.SeekBar.OnSeekBarChangeListener
    public void onStopTrackingTouch(SeekBar seekBar) {
        this.mTrackingTouch = false;
        if (seekBar.getProgress() != this.mProgress) {
            syncProgress(seekBar);
        }
    }

    public void setAccessibilityRangeInfoType(int i) {
        this.mAccessibilityRangeInfoType = i;
    }

    public void setSeekBarContentDescription(CharSequence charSequence) {
        this.mSeekBarContentDescription = charSequence;
        if (this.mSeekBar != null) {
            this.mSeekBar.setContentDescription(charSequence);
        }
    }

    @Override // android.support.v7.preference.Preference
    protected Parcelable onSaveInstanceState() {
        Parcelable parcelableOnSaveInstanceState = super.onSaveInstanceState();
        if (isPersistent()) {
            return parcelableOnSaveInstanceState;
        }
        SavedState savedState = new SavedState(parcelableOnSaveInstanceState);
        savedState.progress = this.mProgress;
        savedState.max = this.mMax;
        savedState.min = this.mMin;
        return savedState;
    }

    @Override // android.support.v7.preference.Preference
    protected void onRestoreInstanceState(Parcelable parcelable) {
        if (!parcelable.getClass().equals(SavedState.class)) {
            super.onRestoreInstanceState(parcelable);
            return;
        }
        SavedState savedState = (SavedState) parcelable;
        super.onRestoreInstanceState(savedState.getSuperState());
        this.mProgress = savedState.progress;
        this.mMax = savedState.max;
        this.mMin = savedState.min;
        notifyChanged();
    }

    private static class SavedState extends Preference.BaseSavedState {
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() { // from class: com.android.settings.widget.SeekBarPreference.SavedState.1
            /* JADX DEBUG: Method merged with bridge method: createFromParcel(Landroid/os/Parcel;)Ljava/lang/Object; */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public SavedState createFromParcel(Parcel parcel) {
                return new SavedState(parcel);
            }

            /* JADX DEBUG: Method merged with bridge method: newArray(I)[Ljava/lang/Object; */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public SavedState[] newArray(int i) {
                return new SavedState[i];
            }
        };
        int max;
        int min;
        int progress;

        public SavedState(Parcel parcel) {
            super(parcel);
            this.progress = parcel.readInt();
            this.max = parcel.readInt();
            this.min = parcel.readInt();
        }

        @Override // android.view.AbsSavedState, android.os.Parcelable
        public void writeToParcel(Parcel parcel, int i) {
            super.writeToParcel(parcel, i);
            parcel.writeInt(this.progress);
            parcel.writeInt(this.max);
            parcel.writeInt(this.min);
        }

        public SavedState(Parcelable parcelable) {
            super(parcelable);
        }
    }
}
