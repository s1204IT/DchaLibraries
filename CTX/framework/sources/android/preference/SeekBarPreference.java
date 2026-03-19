package android.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.widget.SeekBar;
import com.android.internal.R;

public class SeekBarPreference extends Preference implements SeekBar.OnSeekBarChangeListener {
    private int mMax;
    private int mProgress;
    private boolean mTrackingTouch;

    public SeekBarPreference(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.ProgressBar, i, i2);
        setMax(typedArrayObtainStyledAttributes.getInt(2, this.mMax));
        typedArrayObtainStyledAttributes.recycle();
        TypedArray typedArrayObtainStyledAttributes2 = context.obtainStyledAttributes(attributeSet, R.styleable.SeekBarPreference, i, i2);
        int resourceId = typedArrayObtainStyledAttributes2.getResourceId(0, R.layout.preference_widget_seekbar);
        typedArrayObtainStyledAttributes2.recycle();
        setLayoutResource(resourceId);
    }

    public SeekBarPreference(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public SeekBarPreference(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, R.attr.seekBarPreferenceStyle);
    }

    public SeekBarPreference(Context context) {
        this(context, null);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        SeekBar seekBar = (SeekBar) view.findViewById(R.id.seekbar);
        seekBar.setOnSeekBarChangeListener(this);
        seekBar.setMax(this.mMax);
        seekBar.setProgress(this.mProgress);
        seekBar.setEnabled(isEnabled());
    }

    @Override
    protected void onSetInitialValue(boolean z, Object obj) {
        setProgress(z ? getPersistedInt(this.mProgress) : ((Integer) obj).intValue());
    }

    @Override
    protected Object onGetDefaultValue(TypedArray typedArray, int i) {
        return Integer.valueOf(typedArray.getInt(i, 0));
    }

    @Override
    public boolean onKey(View view, int i, KeyEvent keyEvent) {
        SeekBar seekBar;
        if (keyEvent.getAction() == 0 && (seekBar = (SeekBar) view.findViewById(R.id.seekbar)) != null) {
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

    public void setProgress(int i) {
        setProgress(i, true);
    }

    private void setProgress(int i, boolean z) {
        if (i > this.mMax) {
            i = this.mMax;
        }
        if (i < 0) {
            i = 0;
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

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean z) {
        if (z && !this.mTrackingTouch) {
            syncProgress(seekBar);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        this.mTrackingTouch = true;
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        this.mTrackingTouch = false;
        if (seekBar.getProgress() != this.mProgress) {
            syncProgress(seekBar);
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable parcelableOnSaveInstanceState = super.onSaveInstanceState();
        if (isPersistent()) {
            return parcelableOnSaveInstanceState;
        }
        SavedState savedState = new SavedState(parcelableOnSaveInstanceState);
        savedState.progress = this.mProgress;
        savedState.max = this.mMax;
        return savedState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable parcelable) {
        if (!parcelable.getClass().equals(SavedState.class)) {
            super.onRestoreInstanceState(parcelable);
            return;
        }
        SavedState savedState = (SavedState) parcelable;
        super.onRestoreInstanceState(savedState.getSuperState());
        this.mProgress = savedState.progress;
        this.mMax = savedState.max;
        notifyChanged();
    }

    private static class SavedState extends Preference.BaseSavedState {
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel parcel) {
                return new SavedState(parcel);
            }

            @Override
            public SavedState[] newArray(int i) {
                return new SavedState[i];
            }
        };
        int max;
        int progress;

        public SavedState(Parcel parcel) {
            super(parcel);
            this.progress = parcel.readInt();
            this.max = parcel.readInt();
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            super.writeToParcel(parcel, i);
            parcel.writeInt(this.progress);
            parcel.writeInt(this.max);
        }

        public SavedState(Parcelable parcelable) {
            super(parcelable);
        }
    }
}
