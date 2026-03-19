package com.android.deskclock.timer;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.support.annotation.IdRes;
import android.support.v4.view.ViewCompat;
import android.text.BidiFormatter;
import android.text.TextUtils;
import android.text.style.RelativeSizeSpan;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.deskclock.FabContainer;
import com.android.deskclock.FormattedTextUtils;
import com.android.deskclock.R;
import com.android.deskclock.ThemeUtils;
import com.android.deskclock.uidata.UiDataModel;
import java.io.Serializable;
import java.util.Arrays;

public class TimerSetupView extends LinearLayout implements View.OnClickListener, View.OnLongClickListener {
    private View mDeleteView;
    private TextView[] mDigitViews;
    private View mDividerView;
    private FabContainer mFabContainer;
    private final int[] mInput;
    private int mInputPointer;
    private CharSequence mTimeTemplate;
    private TextView mTimeView;

    public TimerSetupView(Context context) {
        this(context, null);
    }

    public TimerSetupView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mInput = new int[]{0, 0, 0, 0, 0, 0};
        this.mInputPointer = -1;
        BidiFormatter bidiFormatter = BidiFormatter.getInstance(false);
        this.mTimeTemplate = TextUtils.expandTemplate("^1^4 ^2^5 ^3^6", bidiFormatter.unicodeWrap("^1"), bidiFormatter.unicodeWrap("^2"), bidiFormatter.unicodeWrap("^3"), FormattedTextUtils.formatText(bidiFormatter.unicodeWrap(context.getString(R.string.hours_label)), new RelativeSizeSpan(0.5f)), FormattedTextUtils.formatText(bidiFormatter.unicodeWrap(context.getString(R.string.minutes_label)), new RelativeSizeSpan(0.5f)), FormattedTextUtils.formatText(bidiFormatter.unicodeWrap(context.getString(R.string.seconds_label)), new RelativeSizeSpan(0.5f)));
        LayoutInflater.from(context).inflate(R.layout.timer_setup_container, this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mTimeView = (TextView) findViewById(R.id.timer_setup_time);
        this.mDeleteView = findViewById(R.id.timer_setup_delete);
        this.mDividerView = findViewById(R.id.timer_setup_divider);
        this.mDigitViews = new TextView[]{(TextView) findViewById(R.id.timer_setup_digit_0), (TextView) findViewById(R.id.timer_setup_digit_1), (TextView) findViewById(R.id.timer_setup_digit_2), (TextView) findViewById(R.id.timer_setup_digit_3), (TextView) findViewById(R.id.timer_setup_digit_4), (TextView) findViewById(R.id.timer_setup_digit_5), (TextView) findViewById(R.id.timer_setup_digit_6), (TextView) findViewById(R.id.timer_setup_digit_7), (TextView) findViewById(R.id.timer_setup_digit_8), (TextView) findViewById(R.id.timer_setup_digit_9)};
        Context context = this.mDividerView.getContext();
        ViewCompat.setBackgroundTintList(this.mDividerView, new ColorStateList(new int[][]{new int[]{android.R.attr.state_activated}, new int[0]}, new int[]{ThemeUtils.resolveColor(context, R.attr.colorControlActivated), ThemeUtils.resolveColor(context, R.attr.colorControlNormal, new int[]{-16842911})}));
        ViewCompat.setBackgroundTintMode(this.mDividerView, PorterDuff.Mode.SRC);
        UiDataModel uiDataModel = UiDataModel.getUiDataModel();
        for (TextView textView : this.mDigitViews) {
            textView.setText(uiDataModel.getFormattedNumber(getDigitForId(textView.getId()), 1));
            textView.setOnClickListener(this);
        }
        this.mDeleteView.setOnClickListener(this);
        this.mDeleteView.setOnLongClickListener(this);
        updateTime();
        updateDeleteAndDivider();
    }

    public void setFabContainer(FabContainer fabContainer) {
        this.mFabContainer = fabContainer;
    }

    @Override
    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        View view;
        if (i == 67) {
            view = this.mDeleteView;
        } else if (i >= 7 && i <= 16) {
            view = this.mDigitViews[i - 7];
        } else {
            view = null;
        }
        if (view != null) {
            boolean zPerformClick = view.performClick();
            if (zPerformClick && hasValidInput()) {
                this.mFabContainer.updateFab(4);
            }
            return zPerformClick;
        }
        return false;
    }

    @Override
    public void onClick(View view) {
        if (view == this.mDeleteView) {
            delete();
        } else {
            append(getDigitForId(view.getId()));
        }
    }

    @Override
    public boolean onLongClick(View view) {
        if (view == this.mDeleteView) {
            reset();
            updateFab();
            return true;
        }
        return false;
    }

    private int getDigitForId(@IdRes int i) {
        switch (i) {
            case R.id.timer_setup_digit_0:
                return 0;
            case R.id.timer_setup_digit_1:
                return 1;
            case R.id.timer_setup_digit_2:
                return 2;
            case R.id.timer_setup_digit_3:
                return 3;
            case R.id.timer_setup_digit_4:
                return 4;
            case R.id.timer_setup_digit_5:
                return 5;
            case R.id.timer_setup_digit_6:
                return 6;
            case R.id.timer_setup_digit_7:
                return 7;
            case R.id.timer_setup_digit_8:
                return 8;
            case R.id.timer_setup_digit_9:
                return 9;
            default:
                throw new IllegalArgumentException("Invalid id: " + i);
        }
    }

    private void updateTime() {
        int i = (this.mInput[1] * 10) + this.mInput[0];
        int i2 = (this.mInput[3] * 10) + this.mInput[2];
        int i3 = (this.mInput[5] * 10) + this.mInput[4];
        UiDataModel uiDataModel = UiDataModel.getUiDataModel();
        this.mTimeView.setText(TextUtils.expandTemplate(this.mTimeTemplate, uiDataModel.getFormattedNumber(i3, 2), uiDataModel.getFormattedNumber(i2, 2), uiDataModel.getFormattedNumber(i, 2)));
        Resources resources = getResources();
        this.mTimeView.setContentDescription(resources.getString(R.string.timer_setup_description, resources.getQuantityString(R.plurals.hours, i3, Integer.valueOf(i3)), resources.getQuantityString(R.plurals.minutes, i2, Integer.valueOf(i2)), resources.getQuantityString(R.plurals.seconds, i, Integer.valueOf(i))));
    }

    private void updateDeleteAndDivider() {
        boolean zHasValidInput = hasValidInput();
        this.mDeleteView.setEnabled(zHasValidInput);
        this.mDividerView.setActivated(zHasValidInput);
    }

    private void updateFab() {
        this.mFabContainer.updateFab(2);
    }

    private void append(int i) {
        if (i < 0 || i > 9) {
            throw new IllegalArgumentException("Invalid digit: " + i);
        }
        if ((this.mInputPointer == -1 && i == 0) || this.mInputPointer == this.mInput.length - 1) {
            return;
        }
        System.arraycopy(this.mInput, 0, this.mInput, 1, this.mInputPointer + 1);
        this.mInput[0] = i;
        this.mInputPointer++;
        updateTime();
        this.mDeleteView.setContentDescription(getContext().getString(R.string.timer_descriptive_delete, UiDataModel.getUiDataModel().getFormattedNumber(i)));
        if (this.mInputPointer == 0) {
            updateFab();
            updateDeleteAndDivider();
        }
    }

    private void delete() {
        if (this.mInputPointer < 0) {
            return;
        }
        System.arraycopy(this.mInput, 1, this.mInput, 0, this.mInputPointer);
        this.mInput[this.mInputPointer] = 0;
        this.mInputPointer--;
        updateTime();
        if (this.mInputPointer >= 0) {
            this.mDeleteView.setContentDescription(getContext().getString(R.string.timer_descriptive_delete, UiDataModel.getUiDataModel().getFormattedNumber(this.mInput[0])));
        } else {
            this.mDeleteView.setContentDescription(getContext().getString(R.string.timer_delete));
        }
        if (this.mInputPointer == -1) {
            updateFab();
            updateDeleteAndDivider();
        }
    }

    public void reset() {
        if (this.mInputPointer != -1) {
            Arrays.fill(this.mInput, 0);
            this.mInputPointer = -1;
            updateTime();
            updateDeleteAndDivider();
        }
    }

    public boolean hasValidInput() {
        return this.mInputPointer != -1;
    }

    public long getTimeInMillis() {
        return (((long) ((this.mInput[1] * 10) + this.mInput[0])) * 1000) + (((long) ((this.mInput[3] * 10) + this.mInput[2])) * 60000) + (((long) ((this.mInput[5] * 10) + this.mInput[4])) * 3600000);
    }

    public Serializable getState() {
        return Arrays.copyOf(this.mInput, this.mInput.length);
    }

    public void setState(Serializable serializable) {
        int[] iArr = (int[]) serializable;
        if (iArr != null && this.mInput.length == iArr.length) {
            for (int i = 0; i < this.mInput.length; i++) {
                this.mInput[i] = iArr[i];
                if (this.mInput[i] != 0) {
                    this.mInputPointer = i;
                }
            }
            updateTime();
            updateDeleteAndDivider();
        }
    }
}
