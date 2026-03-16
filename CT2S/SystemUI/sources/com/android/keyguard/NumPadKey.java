package com.android.keyguard;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.android.internal.widget.LockPatternUtils;

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

    public NumPadKey(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NumPadKey(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mDigit = -1;
        this.mListener = new View.OnClickListener() {
            @Override
            public void onClick(View thisView) {
                View v;
                if (NumPadKey.this.mTextView == null && NumPadKey.this.mTextViewResId > 0 && (v = NumPadKey.this.getRootView().findViewById(NumPadKey.this.mTextViewResId)) != null && (v instanceof PasswordTextView)) {
                    NumPadKey.this.mTextView = (PasswordTextView) v;
                }
                if (NumPadKey.this.mTextView != null && NumPadKey.this.mTextView.isEnabled()) {
                    NumPadKey.this.mTextView.append(Character.forDigit(NumPadKey.this.mDigit, 10));
                }
                NumPadKey.this.userActivity();
                NumPadKey.this.doHapticKeyClick();
            }
        };
        setFocusable(true);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.NumPadKey);
        try {
            this.mDigit = a.getInt(R.styleable.NumPadKey_digit, this.mDigit);
            this.mTextViewResId = a.getResourceId(R.styleable.NumPadKey_textView, 0);
            a.recycle();
            setOnClickListener(this.mListener);
            setOnHoverListener(new LiftToActivateListener(context));
            setAccessibilityDelegate(new ObscureSpeechDelegate(context));
            this.mEnableHaptics = new LockPatternUtils(context).isTactileFeedbackEnabled();
            this.mPM = (PowerManager) this.mContext.getSystemService("power");
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService("layout_inflater");
            inflater.inflate(R.layout.keyguard_num_pad_key, (ViewGroup) this, true);
            this.mDigitText = (TextView) findViewById(R.id.digit_text);
            this.mDigitText.setText(Integer.toString(this.mDigit));
            this.mKlondikeText = (TextView) findViewById(R.id.klondike_text);
            if (this.mDigit >= 0) {
                if (sKlondike == null) {
                    sKlondike = getResources().getStringArray(R.array.lockscreen_num_pad_klondike);
                }
                if (sKlondike != null && sKlondike.length > this.mDigit) {
                    String klondike = sKlondike[this.mDigit];
                    int len = klondike.length();
                    if (len > 0) {
                        this.mKlondikeText.setText(klondike);
                    } else {
                        this.mKlondikeText.setVisibility(4);
                    }
                }
            }
            setBackground(this.mContext.getDrawable(R.drawable.ripple_drawable));
            setContentDescription(this.mDigitText.getText().toString());
        } catch (Throwable th) {
            a.recycle();
            throw th;
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        ObscureSpeechDelegate.sAnnouncedHeadset = false;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        measureChildren(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int digitHeight = this.mDigitText.getMeasuredHeight();
        int klondikeHeight = this.mKlondikeText.getMeasuredHeight();
        int totalHeight = digitHeight + klondikeHeight;
        int top = (getHeight() / 2) - (totalHeight / 2);
        int centerX = getWidth() / 2;
        int left = centerX - (this.mDigitText.getMeasuredWidth() / 2);
        int bottom = top + digitHeight;
        this.mDigitText.layout(left, top, this.mDigitText.getMeasuredWidth() + left, bottom);
        int top2 = (int) (bottom - (klondikeHeight * 0.35f));
        int left2 = centerX - (this.mKlondikeText.getMeasuredWidth() / 2);
        this.mKlondikeText.layout(left2, top2, this.mKlondikeText.getMeasuredWidth() + left2, top2 + klondikeHeight);
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    public void doHapticKeyClick() {
        if (this.mEnableHaptics) {
            performHapticFeedback(1, 3);
        }
    }
}
