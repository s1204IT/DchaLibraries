package com.android.settings.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.TextAppearanceSpan;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import com.android.internal.logging.MetricsLogger;
import com.android.settings.R;
import com.android.settingslib.RestrictedLockUtils;
import java.util.ArrayList;

public class SwitchBar extends LinearLayout implements CompoundButton.OnCheckedChangeListener, View.OnClickListener {
    private static int[] XML_ATTRIBUTES = {R.attr.switchBarMarginStart, R.attr.switchBarMarginEnd, R.attr.switchBarBackgroundColor};
    private boolean mDisabledByAdmin;
    private RestrictedLockUtils.EnforcedAdmin mEnforcedAdmin;
    private String mLabel;
    private String mMetricsTag;
    private View mRestrictedIcon;
    private String mSummary;
    private final TextAppearanceSpan mSummarySpan;
    private ToggleSwitch mSwitch;
    private ArrayList<OnSwitchChangeListener> mSwitchChangeListeners;
    private TextView mTextView;

    public interface OnSwitchChangeListener {
        void onSwitchChanged(Switch r1, boolean z);
    }

    public SwitchBar(Context context) {
        this(context, null);
    }

    public SwitchBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SwitchBar(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public SwitchBar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mDisabledByAdmin = false;
        this.mEnforcedAdmin = null;
        this.mSwitchChangeListeners = new ArrayList<>();
        LayoutInflater.from(context).inflate(R.layout.switch_bar, this);
        TypedArray a = context.obtainStyledAttributes(attrs, XML_ATTRIBUTES);
        int switchBarMarginStart = (int) a.getDimension(0, 0.0f);
        int switchBarMarginEnd = (int) a.getDimension(1, 0.0f);
        int switchBarBackgroundColor = a.getColor(2, 0);
        a.recycle();
        this.mTextView = (TextView) findViewById(R.id.switch_text);
        this.mTextView.setImportantForAccessibility(2);
        this.mLabel = getResources().getString(R.string.switch_off_text);
        this.mSummarySpan = new TextAppearanceSpan(this.mContext, R.style.TextAppearance_Small_SwitchBar);
        updateText();
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) this.mTextView.getLayoutParams();
        lp.setMarginStart(switchBarMarginStart);
        this.mSwitch = (ToggleSwitch) findViewById(R.id.switch_widget);
        this.mSwitch.setSaveEnabled(false);
        this.mSwitch.setImportantForAccessibility(2);
        ViewGroup.MarginLayoutParams lp2 = (ViewGroup.MarginLayoutParams) this.mSwitch.getLayoutParams();
        lp2.setMarginEnd(switchBarMarginEnd);
        setBackgroundColor(switchBarBackgroundColor);
        this.mSwitch.setBackgroundColor(switchBarBackgroundColor);
        addOnSwitchChangeListener(new OnSwitchChangeListener() {
            @Override
            public void onSwitchChanged(Switch switchView, boolean isChecked) {
                SwitchBar.this.setTextViewLabel(isChecked);
            }
        });
        this.mRestrictedIcon = findViewById(R.id.restricted_icon);
        setOnClickListener(this);
        setVisibility(8);
    }

    public void setMetricsTag(String tag) {
        this.mMetricsTag = tag;
    }

    public void setTextViewLabel(boolean isChecked) {
        this.mLabel = getResources().getString(isChecked ? R.string.switch_on_text : R.string.switch_off_text);
        updateText();
    }

    private void updateText() {
        if (TextUtils.isEmpty(this.mSummary)) {
            this.mTextView.setText(this.mLabel);
            return;
        }
        SpannableStringBuilder ssb = new SpannableStringBuilder(this.mLabel).append('\n');
        int start = ssb.length();
        ssb.append((CharSequence) this.mSummary);
        ssb.setSpan(this.mSummarySpan, start, ssb.length(), 0);
        this.mTextView.setText(ssb);
    }

    public void setChecked(boolean checked) {
        setTextViewLabel(checked);
        this.mSwitch.setChecked(checked);
    }

    public void setCheckedInternal(boolean checked) {
        setTextViewLabel(checked);
        this.mSwitch.setCheckedInternal(checked);
    }

    public boolean isChecked() {
        return this.mSwitch.isChecked();
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (enabled && this.mDisabledByAdmin) {
            setDisabledByAdmin(null);
            return;
        }
        super.setEnabled(enabled);
        this.mTextView.setEnabled(enabled);
        this.mSwitch.setEnabled(enabled);
    }

    public void setDisabledByAdmin(RestrictedLockUtils.EnforcedAdmin admin) {
        this.mEnforcedAdmin = admin;
        if (admin != null) {
            super.setEnabled(true);
            this.mDisabledByAdmin = true;
            this.mTextView.setEnabled(false);
            this.mSwitch.setEnabled(false);
            this.mSwitch.setVisibility(8);
            this.mRestrictedIcon.setVisibility(0);
            return;
        }
        this.mDisabledByAdmin = false;
        this.mSwitch.setVisibility(0);
        this.mRestrictedIcon.setVisibility(8);
        setEnabled(true);
    }

    public final ToggleSwitch getSwitch() {
        return this.mSwitch;
    }

    public void show() {
        if (isShowing()) {
            return;
        }
        setVisibility(0);
        this.mSwitch.setOnCheckedChangeListener(this);
    }

    public void hide() {
        if (!isShowing()) {
            return;
        }
        setVisibility(8);
        this.mSwitch.setOnCheckedChangeListener(null);
    }

    public boolean isShowing() {
        return getVisibility() == 0;
    }

    @Override
    public void onClick(View v) {
        if (this.mDisabledByAdmin) {
            MetricsLogger.count(this.mContext, this.mMetricsTag + "/switch_bar|restricted", 1);
            RestrictedLockUtils.sendShowAdminSupportDetailsIntent(this.mContext, this.mEnforcedAdmin);
        } else {
            boolean isChecked = !this.mSwitch.isChecked();
            MetricsLogger.count(this.mContext, this.mMetricsTag + "/switch_bar|" + isChecked, 1);
            setChecked(isChecked);
        }
    }

    public void propagateChecked(boolean isChecked) {
        int count = this.mSwitchChangeListeners.size();
        for (int n = 0; n < count; n++) {
            this.mSwitchChangeListeners.get(n).onSwitchChanged(this.mSwitch, isChecked);
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        propagateChecked(isChecked);
    }

    public void addOnSwitchChangeListener(OnSwitchChangeListener listener) {
        if (this.mSwitchChangeListeners.contains(listener)) {
            throw new IllegalStateException("Cannot add twice the same OnSwitchChangeListener");
        }
        this.mSwitchChangeListeners.add(listener);
    }

    public void removeOnSwitchChangeListener(OnSwitchChangeListener listener) {
        if (!this.mSwitchChangeListeners.contains(listener)) {
            throw new IllegalStateException("Cannot remove OnSwitchChangeListener");
        }
        this.mSwitchChangeListeners.remove(listener);
    }

    static class SavedState extends View.BaseSavedState {
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in, null);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
        boolean checked;
        boolean visible;

        SavedState(Parcel in, SavedState savedState) {
            this(in);
        }

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            this.checked = ((Boolean) in.readValue(null)).booleanValue();
            this.visible = ((Boolean) in.readValue(null)).booleanValue();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeValue(Boolean.valueOf(this.checked));
            out.writeValue(Boolean.valueOf(this.visible));
        }

        public String toString() {
            return "SwitchBar.SavedState{" + Integer.toHexString(System.identityHashCode(this)) + " checked=" + this.checked + " visible=" + this.visible + "}";
        }
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        ss.checked = this.mSwitch.isChecked();
        ss.visible = isShowing();
        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        this.mSwitch.setCheckedInternal(ss.checked);
        setTextViewLabel(ss.checked);
        setVisibility(ss.visible ? 0 : 8);
        this.mSwitch.setOnCheckedChangeListener(ss.visible ? this : null);
        requestLayout();
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return Switch.class.getName();
    }

    public void onInitializeAccessibilityNodeInfoInternal(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfoInternal(info);
        info.setText(this.mTextView.getText());
        info.setCheckable(true);
        info.setChecked(this.mSwitch.isChecked());
    }

    public void onInitializeAccessibilityEventInternal(AccessibilityEvent event) {
        super.onInitializeAccessibilityEventInternal(event);
        event.setContentDescription(this.mTextView.getText());
        event.setChecked(this.mSwitch.isChecked());
    }
}
