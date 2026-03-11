package com.android.quickstep.views;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.FloatProperty;
import android.view.MotionEvent;
import android.view.View;
import com.android.launcher3.InsettableFrameLayout;
import com.android.launcher3.R;
import java.util.ArrayList;

public class RecentsViewContainer extends InsettableFrameLayout {
    public static final FloatProperty<RecentsViewContainer> CONTENT_ALPHA = new FloatProperty<RecentsViewContainer>("contentAlpha") {
        @Override
        public void setValue(RecentsViewContainer recentsViewContainer, float f) {
            recentsViewContainer.setContentAlpha(f);
        }

        @Override
        public Float get(RecentsViewContainer recentsViewContainer) {
            return Float.valueOf(recentsViewContainer.mRecentsView.getContentAlpha());
        }
    };
    private ClearAllButton mClearAllButton;
    private RecentsView mRecentsView;
    private final Rect mTempRect;

    public RecentsViewContainer(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mTempRect = new Rect();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mClearAllButton = (ClearAllButton) findViewById(R.id.clear_all_button);
        this.mClearAllButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                RecentsViewContainer.lambda$onFinishInflate$0(this.f$0, view);
            }
        });
        this.mRecentsView = (RecentsView) findViewById(R.id.overview_panel);
        this.mClearAllButton.forceHasOverlappingRendering(false);
        this.mRecentsView.setClearAllButton(this.mClearAllButton);
        this.mClearAllButton.setRecentsView(this.mRecentsView);
        if (this.mRecentsView.isRtl()) {
            this.mClearAllButton.setNextFocusRightId(this.mRecentsView.getId());
            this.mRecentsView.setNextFocusLeftId(this.mClearAllButton.getId());
        } else {
            this.mClearAllButton.setNextFocusLeftId(this.mRecentsView.getId());
            this.mRecentsView.setNextFocusRightId(this.mClearAllButton.getId());
        }
    }

    public static void lambda$onFinishInflate$0(RecentsViewContainer recentsViewContainer, View view) {
        recentsViewContainer.mRecentsView.mActivity.getUserEventDispatcher().logActionOnControl(0, 13);
        recentsViewContainer.mRecentsView.dismissAllTasks();
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        this.mRecentsView.getTaskSize(this.mTempRect);
        this.mClearAllButton.setTranslationX(((this.mRecentsView.isRtl() ? 1 : -1) * (getResources().getDimension(R.dimen.clear_all_container_width) - this.mClearAllButton.getMeasuredWidth())) / 2.0f);
        this.mClearAllButton.setTranslationY((this.mTempRect.top + ((this.mTempRect.height() - this.mClearAllButton.getMeasuredHeight()) / 2)) - this.mClearAllButton.getTop());
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        super.onTouchEvent(motionEvent);
        return true;
    }

    public void setContentAlpha(float f) {
        if (f == this.mRecentsView.getContentAlpha()) {
            return;
        }
        this.mRecentsView.setContentAlpha(f);
        setVisibility(f > 0.0f ? 0 : 8);
    }

    @Override
    public void addFocusables(ArrayList<View> arrayList, int i, int i2) {
        if (this.mRecentsView.getChildCount() > 0) {
            arrayList.add(this.mRecentsView);
            arrayList.add(this.mClearAllButton);
        }
    }

    @Override
    public boolean requestFocus(int i, Rect rect) {
        return this.mRecentsView.requestFocus(i, rect) || super.requestFocus(i, rect);
    }

    @Override
    public void addChildrenForAccessibility(ArrayList<View> arrayList) {
        arrayList.add(this.mRecentsView);
    }
}
