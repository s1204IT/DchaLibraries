package com.android.phone.common.dialpad;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.RippleDrawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.contacts.R;
import java.util.Locale;

public class DialpadView extends LinearLayout {
    private static final String TAG = DialpadView.class.getSimpleName();
    private final int[] mButtonIds;
    private ImageButton mDelete;
    private EditText mDigits;
    private TextView mIldCountry;
    private TextView mIldRate;
    private final boolean mIsLandscape;
    private final boolean mIsRtl;
    private View mOverflowMenuButton;
    private ViewGroup mRateContainer;
    private ColorStateList mRippleColor;
    private int mTranslateDistance;

    public DialpadView(Context context) {
        this(context, null);
    }

    public DialpadView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DialpadView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mButtonIds = new int[]{R.id.zero, R.id.one, R.id.two, R.id.three, R.id.four, R.id.five, R.id.six, R.id.seven, R.id.eight, R.id.nine, R.id.star, R.id.pound};
        TypedArray a = context.obtainStyledAttributes(attrs, com.android.phone.common.R.styleable.Dialpad);
        this.mRippleColor = a.getColorStateList(0);
        a.recycle();
        this.mTranslateDistance = getResources().getDimensionPixelSize(R.dimen.dialpad_key_button_translate_y);
        this.mIsLandscape = getResources().getConfiguration().orientation == 2;
        this.mIsRtl = TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()) == 1;
    }

    @Override
    protected void onFinishInflate() {
        setupKeypad();
        this.mDigits = (EditText) findViewById(R.id.digits);
        this.mDelete = (ImageButton) findViewById(R.id.deleteButton);
        this.mOverflowMenuButton = findViewById(R.id.dialpad_overflow);
        this.mRateContainer = (ViewGroup) findViewById(R.id.rate_container);
        this.mIldCountry = (TextView) this.mRateContainer.findViewById(R.id.ild_country);
        this.mIldRate = (TextView) this.mRateContainer.findViewById(R.id.ild_rate);
        AccessibilityManager accessibilityManager = (AccessibilityManager) getContext().getSystemService("accessibility");
        if (accessibilityManager.isEnabled()) {
            this.mDigits.setSelected(true);
        }
    }

    private void setupKeypad() {
        int[] numberIds = {R.string.dialpad_0_number, R.string.dialpad_1_number, R.string.dialpad_2_number, R.string.dialpad_3_number, R.string.dialpad_4_number, R.string.dialpad_5_number, R.string.dialpad_6_number, R.string.dialpad_7_number, R.string.dialpad_8_number, R.string.dialpad_9_number, R.string.dialpad_star_number, R.string.dialpad_pound_number};
        int[] letterIds = {R.string.dialpad_0_letters, R.string.dialpad_1_letters, R.string.dialpad_2_letters, R.string.dialpad_3_letters, R.string.dialpad_4_letters, R.string.dialpad_5_letters, R.string.dialpad_6_letters, R.string.dialpad_7_letters, R.string.dialpad_8_letters, R.string.dialpad_9_letters, R.string.dialpad_star_letters, R.string.dialpad_pound_letters};
        Resources resources = getContext().getResources();
        for (int i = 0; i < this.mButtonIds.length; i++) {
            DialpadKeyButton dialpadKey = (DialpadKeyButton) findViewById(this.mButtonIds[i]);
            TextView numberView = (TextView) dialpadKey.findViewById(R.id.dialpad_key_number);
            TextView lettersView = (TextView) dialpadKey.findViewById(R.id.dialpad_key_letters);
            String numberString = resources.getString(numberIds[i]);
            RippleDrawable rippleBackground = (RippleDrawable) getContext().getDrawable(R.drawable.btn_dialpad_key);
            if (this.mRippleColor != null) {
                rippleBackground.setColor(this.mRippleColor);
            }
            numberView.setText(numberString);
            numberView.setElegantTextHeight(false);
            dialpadKey.setContentDescription(numberString);
            dialpadKey.setBackground(rippleBackground);
            if (lettersView != null) {
                lettersView.setText(resources.getString(letterIds[i]));
            }
        }
        DialpadKeyButton one = (DialpadKeyButton) findViewById(R.id.one);
        one.setLongHoverContentDescription(resources.getText(R.string.description_voicemail_button));
        DialpadKeyButton zero = (DialpadKeyButton) findViewById(R.id.zero);
        zero.setLongHoverContentDescription(resources.getText(R.string.description_image_button_plus));
    }

    @Override
    public boolean onHoverEvent(MotionEvent event) {
        return true;
    }
}
