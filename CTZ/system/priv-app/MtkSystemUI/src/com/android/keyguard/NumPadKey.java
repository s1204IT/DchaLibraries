package com.android.keyguard;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.android.internal.widget.LockPatternUtils;

/* loaded from: classes.dex */
public class NumPadKey extends ViewGroup {
    static String[] sKlondike;
    private int mDigit;
    private TextView mDigitText;
    private boolean mEnableHaptics;
    private TextView mKlondikeText;
    private View.OnClickListener mListener;
    private PowerManager mPM;
    private PasswordTextView mTextView;
    private int mTextViewResId;

    public void userActivity() {
        this.mPM.userActivity(SystemClock.uptimeMillis(), false);
    }

    public NumPadKey(Context context) {
        this(context, null);
    }

    public NumPadKey(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public NumPadKey(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, com.android.systemui.R.layout.keyguard_num_pad_key);
    }

    protected NumPadKey(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i);
        this.mDigit = -1;
        this.mListener = new View.OnClickListener() { // from class: com.android.keyguard.NumPadKey.1
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                View viewFindViewById;
                if (NumPadKey.this.mTextView == null && NumPadKey.this.mTextViewResId > 0 && (viewFindViewById = NumPadKey.this.getRootView().findViewById(NumPadKey.this.mTextViewResId)) != null && (viewFindViewById instanceof PasswordTextView)) {
                    NumPadKey.this.mTextView = (PasswordTextView) viewFindViewById;
                }
                if (NumPadKey.this.mTextView != null && NumPadKey.this.mTextView.isEnabled()) {
                    NumPadKey.this.mTextView.append(Character.forDigit(NumPadKey.this.mDigit, 10));
                }
                NumPadKey.this.userActivity();
            }
        };
        setFocusable(true);
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.NumPadKey);
        try {
            this.mDigit = typedArrayObtainStyledAttributes.getInt(0, this.mDigit);
            this.mTextViewResId = typedArrayObtainStyledAttributes.getResourceId(1, 0);
            typedArrayObtainStyledAttributes.recycle();
            setOnClickListener(this.mListener);
            setOnHoverListener(new LiftToActivateListener(context));
            this.mEnableHaptics = new LockPatternUtils(context).isTactileFeedbackEnabled();
            this.mPM = (PowerManager) this.mContext.getSystemService("power");
            ((LayoutInflater) getContext().getSystemService("layout_inflater")).inflate(i2, (ViewGroup) this, true);
            this.mDigitText = (TextView) findViewById(com.android.systemui.R.id.digit_text);
            this.mDigitText.setText(Integer.toString(this.mDigit));
            this.mKlondikeText = (TextView) findViewById(com.android.systemui.R.id.klondike_text);
            if (this.mDigit >= 0) {
                if (sKlondike == null) {
                    sKlondike = getResources().getStringArray(com.android.systemui.R.array.lockscreen_num_pad_klondike);
                }
                if (sKlondike != null && sKlondike.length > this.mDigit) {
                    String str = sKlondike[this.mDigit];
                    if (str.length() > 0) {
                        this.mKlondikeText.setText(str);
                    } else {
                        this.mKlondikeText.setVisibility(4);
                    }
                }
            }
            TypedArray typedArrayObtainStyledAttributes2 = context.obtainStyledAttributes(attributeSet, android.R.styleable.View);
            if (!typedArrayObtainStyledAttributes2.hasValueOrEmpty(13)) {
                setBackground(this.mContext.getDrawable(com.android.systemui.R.drawable.ripple_drawable));
            }
            typedArrayObtainStyledAttributes2.recycle();
            setContentDescription(this.mDigitText.getText().toString());
        } catch (Throwable th) {
            typedArrayObtainStyledAttributes.recycle();
            throw th;
        }
    }

    @Override // android.view.View
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (motionEvent.getActionMasked() == 0) {
            doHapticKeyClick();
        }
        return super.onTouchEvent(motionEvent);
    }

    @Override // android.view.View
    protected void onMeasure(int i, int i2) {
        super.onMeasure(i, i2);
        measureChildren(i, i2);
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        int measuredHeight = this.mDigitText.getMeasuredHeight();
        int measuredHeight2 = this.mKlondikeText.getMeasuredHeight();
        int height = (getHeight() / 2) - ((measuredHeight + measuredHeight2) / 2);
        int width = getWidth() / 2;
        int measuredWidth = width - (this.mDigitText.getMeasuredWidth() / 2);
        int i5 = measuredHeight + height;
        this.mDigitText.layout(measuredWidth, height, this.mDigitText.getMeasuredWidth() + measuredWidth, i5);
        int i6 = (int) (i5 - (measuredHeight2 * 0.35f));
        int measuredWidth2 = width - (this.mKlondikeText.getMeasuredWidth() / 2);
        this.mKlondikeText.layout(measuredWidth2, i6, this.mKlondikeText.getMeasuredWidth() + measuredWidth2, measuredHeight2 + i6);
    }

    @Override // android.view.View
    public boolean hasOverlappingRendering() {
        return false;
    }

    public void doHapticKeyClick() {
        if (this.mEnableHaptics) {
            performHapticFeedback(1, 3);
        }
    }
}
