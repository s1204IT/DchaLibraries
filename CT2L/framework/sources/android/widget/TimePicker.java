package android.widget;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import com.android.internal.R;
import java.util.Locale;

public class TimePicker extends FrameLayout {
    private static final int MODE_CLOCK = 2;
    private static final int MODE_SPINNER = 1;
    private final TimePickerDelegate mDelegate;

    public interface OnTimeChangedListener {
        void onTimeChanged(TimePicker timePicker, int i, int i2);
    }

    interface TimePickerDelegate {
        boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent accessibilityEvent);

        int getBaseline();

        Integer getCurrentHour();

        Integer getCurrentMinute();

        boolean is24HourView();

        boolean isEnabled();

        void onConfigurationChanged(Configuration configuration);

        void onInitializeAccessibilityEvent(AccessibilityEvent accessibilityEvent);

        void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo accessibilityNodeInfo);

        void onPopulateAccessibilityEvent(AccessibilityEvent accessibilityEvent);

        void onRestoreInstanceState(Parcelable parcelable);

        Parcelable onSaveInstanceState(Parcelable parcelable);

        void setCurrentHour(Integer num);

        void setCurrentMinute(Integer num);

        void setEnabled(boolean z);

        void setIs24HourView(Boolean bool);

        void setOnTimeChangedListener(OnTimeChangedListener onTimeChangedListener);

        void setValidationCallback(ValidationCallback validationCallback);
    }

    public interface ValidationCallback {
        void onValidationChanged(boolean z);
    }

    public TimePicker(Context context) {
        this(context, null);
    }

    public TimePicker(Context context, AttributeSet attrs) {
        this(context, attrs, 16843933);
    }

    public TimePicker(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public TimePicker(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TimePicker, defStyleAttr, defStyleRes);
        int mode = a.getInt(8, 1);
        a.recycle();
        switch (mode) {
            case 2:
                this.mDelegate = new TimePickerClockDelegate(this, context, attrs, defStyleAttr, defStyleRes);
                break;
            default:
                this.mDelegate = new TimePickerSpinnerDelegate(this, context, attrs, defStyleAttr, defStyleRes);
                break;
        }
    }

    public void setCurrentHour(Integer currentHour) {
        this.mDelegate.setCurrentHour(currentHour);
    }

    public Integer getCurrentHour() {
        return this.mDelegate.getCurrentHour();
    }

    public void setCurrentMinute(Integer currentMinute) {
        this.mDelegate.setCurrentMinute(currentMinute);
    }

    public Integer getCurrentMinute() {
        return this.mDelegate.getCurrentMinute();
    }

    public void setIs24HourView(Boolean is24HourView) {
        this.mDelegate.setIs24HourView(is24HourView);
    }

    public boolean is24HourView() {
        return this.mDelegate.is24HourView();
    }

    public void setOnTimeChangedListener(OnTimeChangedListener onTimeChangedListener) {
        this.mDelegate.setOnTimeChangedListener(onTimeChangedListener);
    }

    public void setValidationCallback(ValidationCallback callback) {
        this.mDelegate.setValidationCallback(callback);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        this.mDelegate.setEnabled(enabled);
    }

    @Override
    public boolean isEnabled() {
        return this.mDelegate.isEnabled();
    }

    @Override
    public int getBaseline() {
        return this.mDelegate.getBaseline();
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        this.mDelegate.onConfigurationChanged(newConfig);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        return this.mDelegate.onSaveInstanceState(superState);
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        View.BaseSavedState ss = (View.BaseSavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        this.mDelegate.onRestoreInstanceState(ss);
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        return this.mDelegate.dispatchPopulateAccessibilityEvent(event);
    }

    @Override
    public void onPopulateAccessibilityEvent(AccessibilityEvent event) {
        super.onPopulateAccessibilityEvent(event);
        this.mDelegate.onPopulateAccessibilityEvent(event);
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        this.mDelegate.onInitializeAccessibilityEvent(event);
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        this.mDelegate.onInitializeAccessibilityNodeInfo(info);
    }

    static abstract class AbstractTimePickerDelegate implements TimePickerDelegate {
        protected Context mContext;
        protected Locale mCurrentLocale;
        protected TimePicker mDelegator;
        protected OnTimeChangedListener mOnTimeChangedListener;
        protected ValidationCallback mValidationCallback;

        public AbstractTimePickerDelegate(TimePicker delegator, Context context) {
            this.mDelegator = delegator;
            this.mContext = context;
            setCurrentLocale(Locale.getDefault());
        }

        public void setCurrentLocale(Locale locale) {
            if (!locale.equals(this.mCurrentLocale)) {
                this.mCurrentLocale = locale;
            }
        }

        @Override
        public void setValidationCallback(ValidationCallback callback) {
            this.mValidationCallback = callback;
        }

        protected void onValidationChanged(boolean valid) {
            if (this.mValidationCallback != null) {
                this.mValidationCallback.onValidationChanged(valid);
            }
        }
    }
}
