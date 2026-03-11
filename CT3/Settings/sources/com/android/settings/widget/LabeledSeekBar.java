package com.android.settings.widget;

import android.R;
import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityEvent;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import java.util.List;

public class LabeledSeekBar extends SeekBar {
    private final android.support.v4.widget.ExploreByTouchHelper mAccessHelper;
    private String[] mLabels;
    private SeekBar.OnSeekBarChangeListener mOnSeekBarChangeListener;
    private final SeekBar.OnSeekBarChangeListener mProxySeekBarListener;

    public LabeledSeekBar(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.seekBarStyle);
    }

    public LabeledSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public LabeledSeekBar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mProxySeekBarListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (LabeledSeekBar.this.mOnSeekBarChangeListener == null) {
                    return;
                }
                LabeledSeekBar.this.mOnSeekBarChangeListener.onStopTrackingTouch(seekBar);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if (LabeledSeekBar.this.mOnSeekBarChangeListener == null) {
                    return;
                }
                LabeledSeekBar.this.mOnSeekBarChangeListener.onStartTrackingTouch(seekBar);
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (LabeledSeekBar.this.mOnSeekBarChangeListener == null) {
                    return;
                }
                LabeledSeekBar.this.mOnSeekBarChangeListener.onProgressChanged(seekBar, progress, fromUser);
                LabeledSeekBar.this.sendClickEventForAccessibility(progress);
            }
        };
        this.mAccessHelper = new LabeledSeekBarExploreByTouchHelper(this);
        ViewCompat.setAccessibilityDelegate(this, this.mAccessHelper);
        super.setOnSeekBarChangeListener(this.mProxySeekBarListener);
    }

    @Override
    public synchronized void setProgress(int progress) {
        if (this.mAccessHelper != null) {
            this.mAccessHelper.invalidateRoot();
        }
        super.setProgress(progress);
    }

    public void setLabels(String[] labels) {
        this.mLabels = labels;
    }

    @Override
    public void setOnSeekBarChangeListener(SeekBar.OnSeekBarChangeListener l) {
        this.mOnSeekBarChangeListener = l;
    }

    @Override
    protected boolean dispatchHoverEvent(MotionEvent event) {
        if (this.mAccessHelper.dispatchHoverEvent(event)) {
            return true;
        }
        return super.dispatchHoverEvent(event);
    }

    public void sendClickEventForAccessibility(int progress) {
        this.mAccessHelper.invalidateRoot();
        this.mAccessHelper.sendEventForVirtualView(progress, 1);
    }

    private class LabeledSeekBarExploreByTouchHelper extends android.support.v4.widget.ExploreByTouchHelper {
        private boolean mIsLayoutRtl;

        public LabeledSeekBarExploreByTouchHelper(LabeledSeekBar forView) {
            super(forView);
            this.mIsLayoutRtl = forView.getResources().getConfiguration().getLayoutDirection() == 1;
        }

        @Override
        protected int getVirtualViewAt(float x, float y) {
            return getVirtualViewIdIndexFromX(x);
        }

        @Override
        protected void getVisibleVirtualViews(List<Integer> list) {
            int c = LabeledSeekBar.this.getMax();
            for (int i = 0; i <= c; i++) {
                list.add(Integer.valueOf(i));
            }
        }

        @Override
        protected boolean onPerformActionForVirtualView(int virtualViewId, int action, Bundle arguments) {
            if (virtualViewId == -1) {
                return false;
            }
            switch (action) {
                case 16:
                    LabeledSeekBar.this.setProgress(virtualViewId);
                    sendEventForVirtualView(virtualViewId, 1);
                    break;
            }
            return false;
        }

        @Override
        protected void onPopulateNodeForVirtualView(int virtualViewId, AccessibilityNodeInfoCompat node) {
            node.setClassName(RadioButton.class.getName());
            node.setBoundsInParent(getBoundsInParentFromVirtualViewId(virtualViewId));
            node.addAction(16);
            node.setContentDescription(LabeledSeekBar.this.mLabels[virtualViewId]);
            node.setClickable(true);
            node.setCheckable(true);
            node.setChecked(virtualViewId == LabeledSeekBar.this.getProgress());
        }

        @Override
        protected void onPopulateEventForVirtualView(int virtualViewId, AccessibilityEvent event) {
            event.setClassName(RadioButton.class.getName());
            event.setContentDescription(LabeledSeekBar.this.mLabels[virtualViewId]);
            event.setChecked(virtualViewId == LabeledSeekBar.this.getProgress());
        }

        @Override
        protected void onPopulateNodeForHost(AccessibilityNodeInfoCompat node) {
            node.setClassName(RadioGroup.class.getName());
        }

        @Override
        protected void onPopulateEventForHost(AccessibilityEvent event) {
            event.setClassName(RadioGroup.class.getName());
        }

        private int getHalfVirtualViewWidth() {
            int width = LabeledSeekBar.this.getWidth();
            int barWidth = (width - LabeledSeekBar.this.getPaddingStart()) - LabeledSeekBar.this.getPaddingEnd();
            return Math.max(0, barWidth / (LabeledSeekBar.this.getMax() * 2));
        }

        private int getVirtualViewIdIndexFromX(float x) {
            int posBase = (Math.max(0, (((int) x) - LabeledSeekBar.this.getPaddingStart()) / getHalfVirtualViewWidth()) + 1) / 2;
            return this.mIsLayoutRtl ? LabeledSeekBar.this.getMax() - posBase : posBase;
        }

        private Rect getBoundsInParentFromVirtualViewId(int virtualViewId) {
            int updatedVirtualViewId = this.mIsLayoutRtl ? LabeledSeekBar.this.getMax() - virtualViewId : virtualViewId;
            int left = (((updatedVirtualViewId * 2) - 1) * getHalfVirtualViewWidth()) + LabeledSeekBar.this.getPaddingStart();
            int right = (((updatedVirtualViewId * 2) + 1) * getHalfVirtualViewWidth()) + LabeledSeekBar.this.getPaddingStart();
            if (updatedVirtualViewId == 0) {
                left = 0;
            }
            if (updatedVirtualViewId == LabeledSeekBar.this.getMax()) {
                right = LabeledSeekBar.this.getWidth();
            }
            Rect r = new Rect();
            r.set(left, 0, right, LabeledSeekBar.this.getHeight());
            return r;
        }
    }
}
