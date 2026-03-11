package com.android.systemui.qs.customize;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.android.systemui.R;
import com.android.systemui.qs.QSIconView;
import com.android.systemui.qs.QSTileView;
import libcore.util.Objects;

public class CustomizeTileView extends QSTileView {
    private TextView mAppLabel;
    private int mLabelMinLines;

    public CustomizeTileView(Context context, QSIconView icon) {
        super(context, icon);
    }

    @Override
    protected void createLabel() {
        super.createLabel();
        this.mLabelMinLines = this.mLabel.getMinLines();
        View view = LayoutInflater.from(this.mContext).inflate(R.layout.qs_tile_label, (ViewGroup) null);
        this.mAppLabel = (TextView) view.findViewById(R.id.tile_label);
        this.mAppLabel.setAlpha(0.6f);
        this.mAppLabel.setSingleLine(true);
        addView(view);
    }

    public void setShowAppLabel(boolean showAppLabel) {
        this.mAppLabel.setVisibility(showAppLabel ? 0 : 8);
        this.mLabel.setSingleLine(showAppLabel);
        if (showAppLabel) {
            return;
        }
        this.mLabel.setMinLines(this.mLabelMinLines);
    }

    public void setAppLabel(CharSequence label) {
        if (Objects.equal(label, this.mAppLabel.getText())) {
            return;
        }
        this.mAppLabel.setText(label);
    }

    public TextView getAppLabel() {
        return this.mAppLabel;
    }
}
