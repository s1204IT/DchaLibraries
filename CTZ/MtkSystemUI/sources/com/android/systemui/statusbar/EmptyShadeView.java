package com.android.systemui.statusbar;

import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import com.android.systemui.R;
import com.android.systemui.statusbar.stack.ExpandableViewState;
import com.android.systemui.statusbar.stack.StackScrollState;

public class EmptyShadeView extends StackScrollerDecorView {
    private TextView mEmptyText;
    private int mText;

    public EmptyShadeView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mText = R.string.empty_shade_text;
    }

    @Override
    protected void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        this.mEmptyText.setText(this.mText);
    }

    @Override
    protected View findContentView() {
        return findViewById(R.id.no_notifications);
    }

    @Override
    protected View findSecondaryView() {
        return null;
    }

    public void setTextColor(int i) {
        this.mEmptyText.setTextColor(i);
    }

    public void setText(int i) {
        this.mText = i;
        this.mEmptyText.setText(this.mText);
    }

    public int getTextResource() {
        return this.mText;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mEmptyText = (TextView) findContentView();
    }

    @Override
    public ExpandableViewState createNewViewState(StackScrollState stackScrollState) {
        return new EmptyShadeViewState();
    }

    public class EmptyShadeViewState extends ExpandableViewState {
        public EmptyShadeViewState() {
        }

        @Override
        public void applyToView(View view) {
            super.applyToView(view);
            if (view instanceof EmptyShadeView) {
                EmptyShadeView emptyShadeView = (EmptyShadeView) view;
                boolean z = false;
                if ((((float) this.clipTopAmount) <= ((float) EmptyShadeView.this.mEmptyText.getPaddingTop()) * 0.6f) && emptyShadeView.isVisible()) {
                    z = true;
                }
                emptyShadeView.setContentVisible(z);
            }
        }
    }
}
