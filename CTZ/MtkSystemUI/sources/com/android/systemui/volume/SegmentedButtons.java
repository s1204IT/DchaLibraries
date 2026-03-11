package com.android.systemui.volume;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.systemui.R;
import com.android.systemui.volume.Interaction;
import java.util.Objects;

public class SegmentedButtons extends LinearLayout {
    private Callback mCallback;
    private final View.OnClickListener mClick;
    private final ConfigurableTexts mConfigurableTexts;
    private final Context mContext;
    protected final LayoutInflater mInflater;
    protected Object mSelectedValue;
    private static final Typeface REGULAR = Typeface.create("sans-serif", 0);
    private static final Typeface MEDIUM = Typeface.create("sans-serif-medium", 0);

    public interface Callback extends Interaction.Callback {
        void onSelected(Object obj, boolean z);
    }

    public SegmentedButtons(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mClick = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SegmentedButtons.this.setSelectedValue(view.getTag(), true);
            }
        };
        this.mContext = context;
        this.mInflater = LayoutInflater.from(this.mContext);
        setOrientation(0);
        this.mConfigurableTexts = new ConfigurableTexts(this.mContext);
    }

    public void setCallback(Callback callback) {
        this.mCallback = callback;
    }

    public Object getSelectedValue() {
        return this.mSelectedValue;
    }

    public void setSelectedValue(Object obj, boolean z) {
        if (Objects.equals(obj, this.mSelectedValue)) {
            return;
        }
        this.mSelectedValue = obj;
        for (int i = 0; i < getChildCount(); i++) {
            TextView textView = (TextView) getChildAt(i);
            boolean zEquals = Objects.equals(this.mSelectedValue, textView.getTag());
            textView.setSelected(zEquals);
            setSelectedStyle(textView, zEquals);
        }
        fireOnSelected(z);
    }

    protected void setSelectedStyle(TextView textView, boolean z) {
        textView.setTypeface(z ? MEDIUM : REGULAR);
    }

    public Button inflateButton() {
        return (Button) this.mInflater.inflate(R.layout.segmented_button, (ViewGroup) this, false);
    }

    public void addButton(int i, int i2, Object obj) {
        Button buttonInflateButton = inflateButton();
        buttonInflateButton.setTag(R.id.label, Integer.valueOf(i));
        buttonInflateButton.setText(i);
        buttonInflateButton.setContentDescription(getResources().getString(i2));
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) buttonInflateButton.getLayoutParams();
        if (getChildCount() == 0) {
            layoutParams.rightMargin = 0;
            layoutParams.leftMargin = 0;
        }
        buttonInflateButton.setLayoutParams(layoutParams);
        addView(buttonInflateButton);
        buttonInflateButton.setTag(obj);
        buttonInflateButton.setOnClickListener(this.mClick);
        Interaction.register(buttonInflateButton, new Interaction.Callback() {
            @Override
            public void onInteraction() {
                SegmentedButtons.this.fireInteraction();
            }
        });
        this.mConfigurableTexts.add(buttonInflateButton, i);
    }

    public void update() {
        this.mConfigurableTexts.update();
    }

    private void fireOnSelected(boolean z) {
        if (this.mCallback != null) {
            this.mCallback.onSelected(this.mSelectedValue, z);
        }
    }

    public void fireInteraction() {
        if (this.mCallback != null) {
            this.mCallback.onInteraction();
        }
    }
}
