package com.android.systemui.volume;

import android.content.Context;
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
    private final Context mContext;
    private final LayoutInflater mInflater;
    private Object mSelectedValue;

    public interface Callback extends Interaction.Callback {
        void onSelected(Object obj);
    }

    public SegmentedButtons(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SegmentedButtons.this.setSelectedValue(v.getTag());
            }
        };
        this.mContext = context;
        this.mInflater = LayoutInflater.from(this.mContext);
        setOrientation(0);
    }

    public void setCallback(Callback callback) {
        this.mCallback = callback;
    }

    public Object getSelectedValue() {
        return this.mSelectedValue;
    }

    public void setSelectedValue(Object value) {
        if (!Objects.equals(value, this.mSelectedValue)) {
            this.mSelectedValue = value;
            for (int i = 0; i < getChildCount(); i++) {
                TextView c = (TextView) getChildAt(i);
                Object tag = c.getTag();
                boolean selected = Objects.equals(this.mSelectedValue, tag);
                c.setSelected(selected);
                c.getCompoundDrawables()[1].setTint(this.mContext.getResources().getColor(selected ? R.color.segmented_button_selected : R.color.segmented_button_unselected));
            }
            fireOnSelected();
        }
    }

    public void addButton(int labelResId, int iconResId, Object value) {
        Button b = (Button) this.mInflater.inflate(R.layout.segmented_button, (ViewGroup) this, false);
        b.setTag(R.id.label, Integer.valueOf(labelResId));
        b.setText(labelResId);
        b.setCompoundDrawablesWithIntrinsicBounds(0, iconResId, 0, 0);
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) b.getLayoutParams();
        if (getChildCount() == 0) {
            lp.rightMargin = 0;
            lp.leftMargin = 0;
        }
        b.setLayoutParams(lp);
        addView(b);
        b.setTag(value);
        b.setOnClickListener(this.mClick);
        Interaction.register(b, new Interaction.Callback() {
            @Override
            public void onInteraction() {
                SegmentedButtons.this.fireInteraction();
            }
        });
    }

    public void updateLocale() {
        for (int i = 0; i < getChildCount(); i++) {
            Button b = (Button) getChildAt(i);
            int labelResId = ((Integer) b.getTag(R.id.label)).intValue();
            b.setText(labelResId);
        }
    }

    private void fireOnSelected() {
        if (this.mCallback != null) {
            this.mCallback.onSelected(this.mSelectedValue);
        }
    }

    public void fireInteraction() {
        if (this.mCallback != null) {
            this.mCallback.onInteraction();
        }
    }
}
