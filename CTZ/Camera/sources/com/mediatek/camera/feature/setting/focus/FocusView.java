package com.mediatek.camera.feature.setting.focus;

import android.content.Context;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import com.mediatek.camera.R;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.widget.RotateLayout;
import com.mediatek.camera.feature.setting.focus.IFocusView;

public class FocusView extends RotateLayout implements IFocusView {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(FocusView.class.getSimpleName());
    private Runnable mDisappear;
    private Runnable mEndAction;
    private RelativeLayout mExpandView;
    private ImageView mFocusRing;
    private int mFocusViewX;
    private int mFocusViewY;
    private boolean mIsExpandViewRightOfFocusRing;
    private RectF mPreviewRect;
    private IFocusView.FocusViewState mState;

    public FocusView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mDisappear = new Disappear();
        this.mEndAction = new EndAction();
        this.mState = IFocusView.FocusViewState.STATE_IDLE;
        this.mPreviewRect = new RectF();
        this.mIsExpandViewRightOfFocusRing = true;
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mFocusRing = (ImageView) findViewById(R.id.focus_ring);
        this.mExpandView = (RelativeLayout) findViewById(R.id.expand_view);
    }

    public boolean isPassiveFocusRunning() {
        LogHelper.d(TAG, "[isPassiveFocusRunning] mState =  " + this.mState);
        return getFocusState() == IFocusView.FocusViewState.STATE_PASSIVE_FOCUSING;
    }

    private boolean isExpandViewOutOfDisplay() {
        int i = (int) this.mPreviewRect.left;
        int i2 = (int) this.mPreviewRect.right;
        int i3 = (int) this.mPreviewRect.top;
        int i4 = (int) this.mPreviewRect.bottom;
        int orientation = getOrientation();
        return orientation != 0 ? orientation != 90 ? orientation != 180 ? orientation == 270 && this.mFocusViewY + ((this.mFocusRing.getWidth() / 2) + this.mExpandView.getWidth()) > i4 : this.mFocusViewX - ((this.mFocusRing.getWidth() / 2) + this.mExpandView.getWidth()) < i : this.mFocusViewY - ((this.mFocusRing.getWidth() / 2) + this.mExpandView.getWidth()) < i3 : this.mFocusViewX + ((this.mFocusRing.getWidth() / 2) + this.mExpandView.getWidth()) > i2;
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(this.mExpandView.getLayoutParams());
        if (isExpandViewOutOfDisplay()) {
            if (this.mIsExpandViewRightOfFocusRing) {
                layoutParams.addRule(0, R.id.focus_ring);
                layoutParams.addRule(15);
                this.mExpandView.setLayoutParams(layoutParams);
                this.mExpandView.postInvalidate();
                this.mIsExpandViewRightOfFocusRing = false;
                LogHelper.d(TAG, "[onLayout] set ExpandView to left");
                return;
            }
            return;
        }
        if (!this.mIsExpandViewRightOfFocusRing) {
            LogHelper.d(TAG, "[onLayout] set ExpandView to right");
            layoutParams.addRule(1, R.id.focus_ring);
            layoutParams.addRule(15);
            this.mExpandView.setLayoutParams(layoutParams);
            this.mExpandView.postInvalidate();
            this.mIsExpandViewRightOfFocusRing = true;
        }
    }

    public boolean isActiveFocusRunning() {
        LogHelper.d(TAG, "[isActiveFocusRunning] mState =  " + this.mState);
        return getFocusState() == IFocusView.FocusViewState.STATE_ACTIVE_FOCUSING;
    }

    public void startPassiveFocus() {
        if (getFocusState() != IFocusView.FocusViewState.STATE_IDLE || getHandler() == null) {
            LogHelper.w(TAG, "[startPassiveFocus] mState " + this.mState + ",getHandler = " + getHandler());
            return;
        }
        getHandler().removeCallbacks(this.mDisappear);
        setContentDescription("continue focus");
        this.mFocusRing.setVisibility(0);
        this.mExpandView.setVisibility(4);
        this.mFocusRing.setImageDrawable(getResources().getDrawable(R.drawable.ic_continue_focus));
        setVisibility(0);
        animate().withLayer().setDuration(1000L).scaleX(1.2f).scaleY(1.2f).alpha(1.0f);
        setFocusState(IFocusView.FocusViewState.STATE_PASSIVE_FOCUSING);
    }

    public void startActiveFocus() {
        if (getFocusState() != IFocusView.FocusViewState.STATE_IDLE || getHandler() == null) {
            LogHelper.w(TAG, "[startActiveFocus] mState " + this.mState + ",getHandler = " + getHandler());
            return;
        }
        getHandler().removeCallbacks(this.mDisappear);
        setContentDescription("touch focus");
        this.mExpandView.setVisibility(0);
        this.mFocusRing.setVisibility(0);
        this.mFocusRing.setImageDrawable(getResources().getDrawable(R.drawable.ic_touch_focus));
        setVisibility(0);
        animate().withLayer().setDuration(1000L).scaleX(1.2f).scaleY(1.2f).alpha(1.0f);
        setFocusState(IFocusView.FocusViewState.STATE_ACTIVE_FOCUSING);
    }

    public void stopFocusAnimations() {
        if (isPassiveFocusRunning()) {
            animate().withLayer().setDuration(200L).scaleX(1.0f).scaleY(1.0f).withEndAction(this.mEndAction);
        } else if (isActiveFocusRunning()) {
            setFocusState(IFocusView.FocusViewState.STATE_ACTIVE_FOCUSED);
            animate().withLayer().setDuration(200L).scaleX(1.0f).scaleY(1.0f);
            postDelayed(new ActiveFocusEndAction(), 2000L);
        }
    }

    public void setFocusLocation(float f, float f2) {
        this.mFocusViewX = (int) f;
        this.mFocusViewY = (int) f2;
    }

    public void centerFocusLocation() {
    }

    protected void setPreviewRect(RectF rectF) {
        this.mPreviewRect = rectF;
    }

    protected synchronized IFocusView.FocusViewState getFocusState() {
        return this.mState;
    }

    protected void setFocusState(IFocusView.FocusViewState focusViewState) {
        this.mState = focusViewState;
    }

    protected void clearFocusUi() {
        this.mFocusRing.setVisibility(4);
        this.mExpandView.setVisibility(4);
        setVisibility(4);
        setFocusState(IFocusView.FocusViewState.STATE_IDLE);
    }

    protected void highlightFocusView() {
        animate().withLayer().alpha(1.0f);
    }

    protected void lowlightFocusView() {
        postDelayed(new ActiveFocusEndAction(), 2000L);
    }

    private class EndAction implements Runnable {
        private EndAction() {
        }

        @Override
        public void run() {
            FocusView.this.postDelayed(FocusView.this.mDisappear, 200L);
        }
    }

    private class ActiveFocusEndAction implements Runnable {
        private ActiveFocusEndAction() {
        }

        @Override
        public void run() {
            LogHelper.d(FocusView.TAG, "[ActiveFocusEndAction run +] mState " + FocusView.this.mState);
            FocusView.this.animate().withLayer().alpha(0.5f);
        }
    }

    private class Disappear implements Runnable {
        private Disappear() {
        }

        @Override
        public void run() {
            LogHelper.d(FocusView.TAG, "[Disappear run +] mState " + FocusView.this.mState);
            FocusView.this.clearFocusUi();
        }
    }
}
