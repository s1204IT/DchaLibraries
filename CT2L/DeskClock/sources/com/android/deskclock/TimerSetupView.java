package com.android.deskclock;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import com.android.deskclock.timer.TimerView;

public class TimerSetupView extends LinearLayout implements View.OnClickListener, View.OnLongClickListener {
    protected final Context mContext;
    protected ImageButton mDelete;
    protected View mDivider;
    protected TimerView mEnteredTime;
    private final AnimatorListenerAdapter mHideFabAnimatorListener;
    protected int[] mInput;
    protected int mInputPointer;
    protected int mInputSize;
    protected Button mLeft;
    protected final Button[] mNumbers;
    protected Button mRight;
    private final AnimatorListenerAdapter mShowFabAnimatorListener;
    protected ImageButton mStart;

    public TimerSetupView(Context context) {
        this(context, null);
    }

    public TimerSetupView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mInputSize = 5;
        this.mNumbers = new Button[10];
        this.mInput = new int[this.mInputSize];
        this.mInputPointer = -1;
        this.mHideFabAnimatorListener = new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (TimerSetupView.this.mStart != null) {
                    TimerSetupView.this.mStart.setScaleX(1.0f);
                    TimerSetupView.this.mStart.setScaleY(1.0f);
                    TimerSetupView.this.mStart.setVisibility(4);
                }
            }
        };
        this.mShowFabAnimatorListener = new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                if (TimerSetupView.this.mStart != null) {
                    TimerSetupView.this.mStart.setVisibility(0);
                }
            }
        };
        this.mContext = context;
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService("layout_inflater");
        layoutInflater.inflate(R.layout.time_setup_view, this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        View v1 = findViewById(R.id.first);
        View v2 = findViewById(R.id.second);
        View v3 = findViewById(R.id.third);
        View v4 = findViewById(R.id.fourth);
        this.mEnteredTime = (TimerView) findViewById(R.id.timer_time_text);
        this.mDelete = (ImageButton) findViewById(R.id.delete);
        this.mDelete.setOnClickListener(this);
        this.mDelete.setOnLongClickListener(this);
        this.mDivider = findViewById(R.id.divider);
        this.mNumbers[1] = (Button) v1.findViewById(R.id.key_left);
        this.mNumbers[2] = (Button) v1.findViewById(R.id.key_middle);
        this.mNumbers[3] = (Button) v1.findViewById(R.id.key_right);
        this.mNumbers[4] = (Button) v2.findViewById(R.id.key_left);
        this.mNumbers[5] = (Button) v2.findViewById(R.id.key_middle);
        this.mNumbers[6] = (Button) v2.findViewById(R.id.key_right);
        this.mNumbers[7] = (Button) v3.findViewById(R.id.key_left);
        this.mNumbers[8] = (Button) v3.findViewById(R.id.key_middle);
        this.mNumbers[9] = (Button) v3.findViewById(R.id.key_right);
        this.mLeft = (Button) v4.findViewById(R.id.key_left);
        this.mNumbers[0] = (Button) v4.findViewById(R.id.key_middle);
        this.mRight = (Button) v4.findViewById(R.id.key_right);
        this.mLeft.setVisibility(4);
        this.mRight.setVisibility(4);
        for (int i = 0; i < 10; i++) {
            this.mNumbers[i].setOnClickListener(this);
            this.mNumbers[i].setText(String.format("%d", Integer.valueOf(i)));
            this.mNumbers[i].setTextColor(-1);
            this.mNumbers[i].setTag(R.id.numbers_key, new Integer(i));
        }
        updateTime();
    }

    public void registerStartButton(ImageButton start) {
        this.mStart = start;
        initializeStartButtonVisibility();
    }

    private void initializeStartButtonVisibility() {
        if (this.mStart != null) {
            this.mStart.setVisibility(isInputHasValue() ? 0 : 4);
        }
    }

    private void updateStartButton() {
        setFabButtonVisibility(isInputHasValue());
    }

    public void updateDeleteButtonAndDivider() {
        boolean enabled = isInputHasValue();
        if (this.mDelete != null) {
            this.mDelete.setEnabled(enabled);
            this.mDivider.setBackgroundResource(enabled ? R.color.hot_pink : R.color.dialog_gray);
        }
    }

    private boolean isInputHasValue() {
        return this.mInputPointer != -1;
    }

    private void setFabButtonVisibility(boolean show) {
        int finalVisibility = show ? 0 : 4;
        if (this.mStart != null && this.mStart.getVisibility() != finalVisibility) {
            ImageButton imageButton = this.mStart;
            float[] fArr = new float[2];
            fArr[0] = show ? 0.0f : 1.0f;
            fArr[1] = show ? 1.0f : 0.0f;
            Animator scaleAnimator = AnimatorUtils.getScaleAnimator(imageButton, fArr);
            scaleAnimator.setDuration(266L);
            scaleAnimator.addListener(show ? this.mShowFabAnimatorListener : this.mHideFabAnimatorListener);
            scaleAnimator.start();
        }
    }

    @Override
    public void onClick(View v) {
        doOnClick(v);
        updateStartButton();
        updateDeleteButtonAndDivider();
    }

    protected void doOnClick(View v) {
        Integer val = (Integer) v.getTag(R.id.numbers_key);
        if (val != null) {
            if ((this.mInputPointer != -1 || val.intValue() != 0) && this.mInputPointer < this.mInputSize - 1) {
                for (int i = this.mInputPointer; i >= 0; i--) {
                    this.mInput[i + 1] = this.mInput[i];
                }
                this.mInputPointer++;
                this.mInput[0] = val.intValue();
                updateTime();
                return;
            }
            return;
        }
        if (v == this.mDelete && this.mInputPointer >= 0) {
            for (int i2 = 0; i2 < this.mInputPointer; i2++) {
                this.mInput[i2] = this.mInput[i2 + 1];
            }
            this.mInput[this.mInputPointer] = 0;
            this.mInputPointer--;
            updateTime();
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if (v != this.mDelete) {
            return false;
        }
        reset();
        updateStartButton();
        updateDeleteButtonAndDivider();
        return true;
    }

    protected void updateTime() {
        this.mEnteredTime.setTime(this.mInput[4], this.mInput[3], this.mInput[2], (this.mInput[1] * 10) + this.mInput[0]);
    }

    public void reset() {
        for (int i = 0; i < this.mInputSize; i++) {
            this.mInput[i] = 0;
        }
        this.mInputPointer = -1;
        updateTime();
    }

    public int getTime() {
        return (this.mInput[4] * 3600) + (this.mInput[3] * 600) + (this.mInput[2] * 60) + (this.mInput[1] * 10) + this.mInput[0];
    }

    public void saveEntryState(Bundle outState, String key) {
        outState.putIntArray(key, this.mInput);
    }

    public void restoreEntryState(Bundle inState, String key) {
        int[] input = inState.getIntArray(key);
        if (input != null && this.mInputSize == input.length) {
            for (int i = 0; i < this.mInputSize; i++) {
                this.mInput[i] = input[i];
                if (this.mInput[i] != 0) {
                    this.mInputPointer = i;
                }
            }
            updateTime();
        }
        initializeStartButtonVisibility();
    }
}
