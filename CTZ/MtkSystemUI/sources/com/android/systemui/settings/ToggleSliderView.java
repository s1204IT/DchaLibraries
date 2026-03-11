package com.android.systemui.settings;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import com.android.settingslib.RestrictedLockUtils;
import com.android.systemui.R;
import com.android.systemui.settings.ToggleSlider;
import com.android.systemui.statusbar.policy.BrightnessMirrorController;

public class ToggleSliderView extends RelativeLayout implements ToggleSlider {
    private final CompoundButton.OnCheckedChangeListener mCheckListener;
    private TextView mLabel;
    private ToggleSlider.Listener mListener;
    private ToggleSliderView mMirror;
    private BrightnessMirrorController mMirrorController;
    private final SeekBar.OnSeekBarChangeListener mSeekListener;
    private ToggleSeekBar mSlider;
    private CompoundButton mToggle;
    private boolean mTracking;

    public ToggleSliderView(Context context) {
        this(context, null);
    }

    public ToggleSliderView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public ToggleSliderView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mCheckListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                ToggleSliderView.this.mSlider.setEnabled(!z);
                if (ToggleSliderView.this.mListener != null) {
                    ToggleSliderView.this.mListener.onChanged(ToggleSliderView.this, ToggleSliderView.this.mTracking, z, ToggleSliderView.this.mSlider.getProgress(), false);
                }
                if (ToggleSliderView.this.mMirror != null) {
                    ToggleSliderView.this.mMirror.mToggle.setChecked(z);
                }
            }
        };
        this.mSeekListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i2, boolean z) {
                if (ToggleSliderView.this.mListener != null) {
                    ToggleSliderView.this.mListener.onChanged(ToggleSliderView.this, ToggleSliderView.this.mTracking, ToggleSliderView.this.mToggle.isChecked(), i2, false);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                ToggleSliderView.this.mTracking = true;
                if (ToggleSliderView.this.mListener != null) {
                    ToggleSliderView.this.mListener.onChanged(ToggleSliderView.this, ToggleSliderView.this.mTracking, ToggleSliderView.this.mToggle.isChecked(), ToggleSliderView.this.mSlider.getProgress(), false);
                }
                ToggleSliderView.this.mToggle.setChecked(false);
                if (ToggleSliderView.this.mMirrorController != null) {
                    ToggleSliderView.this.mMirrorController.showMirror();
                    ToggleSliderView.this.mMirrorController.setLocation((View) ToggleSliderView.this.getParent());
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                ToggleSliderView.this.mTracking = false;
                if (ToggleSliderView.this.mListener != null) {
                    ToggleSliderView.this.mListener.onChanged(ToggleSliderView.this, ToggleSliderView.this.mTracking, ToggleSliderView.this.mToggle.isChecked(), ToggleSliderView.this.mSlider.getProgress(), true);
                }
                if (ToggleSliderView.this.mMirrorController != null) {
                    ToggleSliderView.this.mMirrorController.hideMirror();
                }
            }
        };
        View.inflate(context, R.layout.status_bar_toggle_slider, this);
        context.getResources();
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.ToggleSliderView, i, 0);
        this.mToggle = (CompoundButton) findViewById(R.id.toggle);
        this.mToggle.setOnCheckedChangeListener(this.mCheckListener);
        this.mSlider = (ToggleSeekBar) findViewById(R.id.slider);
        this.mSlider.setOnSeekBarChangeListener(this.mSeekListener);
        this.mLabel = (TextView) findViewById(R.id.label);
        this.mLabel.setText(typedArrayObtainStyledAttributes.getString(0));
        this.mSlider.setAccessibilityLabel(getContentDescription().toString());
        typedArrayObtainStyledAttributes.recycle();
    }

    public void setMirror(ToggleSliderView toggleSliderView) {
        this.mMirror = toggleSliderView;
        if (this.mMirror != null) {
            this.mMirror.setChecked(this.mToggle.isChecked());
            this.mMirror.setMax(this.mSlider.getMax());
            this.mMirror.setValue(this.mSlider.getProgress());
        }
    }

    public void setMirrorController(BrightnessMirrorController brightnessMirrorController) {
        this.mMirrorController = brightnessMirrorController;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (this.mListener != null) {
            this.mListener.onInit(this);
        }
    }

    public void setEnforcedAdmin(RestrictedLockUtils.EnforcedAdmin enforcedAdmin) {
        this.mToggle.setEnabled(enforcedAdmin == null);
        this.mSlider.setEnabled(enforcedAdmin == null);
        this.mSlider.setEnforcedAdmin(enforcedAdmin);
    }

    @Override
    public void setOnChangedListener(ToggleSlider.Listener listener) {
        this.mListener = listener;
    }

    @Override
    public void setChecked(boolean z) {
        this.mToggle.setChecked(z);
    }

    @Override
    public void setMax(int i) {
        this.mSlider.setMax(i);
        if (this.mMirror != null) {
            this.mMirror.setMax(i);
        }
    }

    @Override
    public void setValue(int i) {
        this.mSlider.setProgress(i);
        if (this.mMirror != null) {
            this.mMirror.setValue(i);
        }
    }

    @Override
    public int getValue() {
        return this.mSlider.getProgress();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent motionEvent) {
        if (this.mMirror != null) {
            MotionEvent motionEventCopy = motionEvent.copy();
            this.mMirror.dispatchTouchEvent(motionEventCopy);
            motionEventCopy.recycle();
        }
        return super.dispatchTouchEvent(motionEvent);
    }
}
