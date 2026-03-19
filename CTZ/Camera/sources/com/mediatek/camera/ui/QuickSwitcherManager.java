package com.mediatek.camera.ui;

import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import com.mediatek.camera.R;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.utils.CameraUtil;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

public class QuickSwitcherManager extends AbstractViewManager {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(QuickSwitcherManager.class.getSimpleName());
    private ViewGroup mOptionRoot;
    private final OnOrientationChangeListenerImpl mOrientationChangeListener;
    private ConcurrentSkipListMap<Integer, View> mQuickItems;
    private LinearLayout mQuickSwitcherLayout;
    private View mTopBar;

    public QuickSwitcherManager(IApp iApp, ViewGroup viewGroup) {
        super(iApp, viewGroup);
        this.mQuickItems = new ConcurrentSkipListMap<>();
        this.mTopBar = iApp.getActivity().findViewById(R.id.top_bar);
        this.mOptionRoot = (ViewGroup) this.mApp.getActivity().findViewById(R.id.quick_switcher_option);
        this.mOrientationChangeListener = new OnOrientationChangeListenerImpl();
    }

    @Override
    protected View getView() {
        this.mQuickSwitcherLayout = (LinearLayout) this.mParentView.findViewById(R.id.quick_switcher);
        updateQuickItems();
        return this.mQuickSwitcherLayout;
    }

    @Override
    public void setEnabled(boolean z) {
        if (this.mQuickSwitcherLayout != null) {
            int childCount = this.mQuickSwitcherLayout.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View childAt = this.mQuickSwitcherLayout.getChildAt(i);
                childAt.setEnabled(z);
                childAt.setClickable(z);
            }
        }
    }

    public void addToQuickSwitcher(View view, int i) {
        LogHelper.d(TAG, "[registerToQuickSwitcher] priority = " + i);
        if (this.mQuickItems.size() > 4) {
            LogHelper.w(TAG, "already reach to limit number : 4");
        } else if (!this.mQuickItems.containsValue(view)) {
            this.mQuickItems.put(Integer.valueOf(i), view);
        }
    }

    public void removeFromQuickSwitcher(View view) {
        LogHelper.d(TAG, "[removeFromQuickSwitcher]");
        if (this.mQuickItems.containsValue(view)) {
            for (Map.Entry<Integer, View> entry : this.mQuickItems.entrySet()) {
                View value = entry.getValue();
                if (value == view) {
                    int iIntValue = entry.getKey().intValue();
                    LogHelper.d(TAG, "[removeFromQuickSwitcher] priority = " + iIntValue);
                    this.mQuickItems.remove(Integer.valueOf(iIntValue), value);
                }
            }
        }
    }

    public void registerQuickIconDone() {
        updateQuickItems();
    }

    public void showQuickSwitcherOption(View view) {
        if (this.mOptionRoot.getChildCount() != 0) {
            LogHelper.e(TAG, "[showQuickSwitcherOption] Already has options to be shown!");
            return;
        }
        Animation animationLoadAnimation = AnimationUtils.loadAnimation(this.mApp.getActivity(), R.anim.anim_top_in);
        this.mOptionRoot.addView(view);
        CameraUtil.rotateRotateLayoutChildView(this.mApp.getActivity(), this.mOptionRoot, this.mApp.getGSensorOrientation(), true);
        this.mOptionRoot.setVisibility(0);
        this.mOptionRoot.setClickable(true);
        this.mOptionRoot.startAnimation(animationLoadAnimation);
        this.mTopBar.setVisibility(8);
        this.mApp.registerOnOrientationChangeListener(this.mOrientationChangeListener);
    }

    public void hideQuickSwitcherOption() {
        Animation animationLoadAnimation = AnimationUtils.loadAnimation(this.mApp.getActivity(), R.anim.anim_top_out);
        animationLoadAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                QuickSwitcherManager.this.mOptionRoot.setVisibility(8);
                QuickSwitcherManager.this.mOptionRoot.setClickable(false);
                QuickSwitcherManager.this.mOptionRoot.removeAllViews();
                QuickSwitcherManager.this.mTopBar.setVisibility(0);
                QuickSwitcherManager.this.mApp.unregisterOnOrientationChangeListener(QuickSwitcherManager.this.mOrientationChangeListener);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        this.mOptionRoot.startAnimation(animationLoadAnimation);
        animationLoadAnimation.setFillAfter(true);
    }

    public void hideQuickSwitcherImmediately() {
        this.mOptionRoot.setVisibility(8);
        this.mOptionRoot.removeAllViews();
        this.mTopBar.setVisibility(0);
        this.mApp.unregisterOnOrientationChangeListener(this.mOrientationChangeListener);
    }

    private void updateQuickItems() {
        int i = (int) (46.0f * this.mApp.getActivity().getResources().getDisplayMetrics().density);
        if (this.mQuickSwitcherLayout != null && this.mQuickSwitcherLayout.getChildCount() != 0) {
            this.mQuickSwitcherLayout.removeAllViews();
        }
        if (this.mQuickSwitcherLayout != null) {
            Iterator<Map.Entry<Integer, View>> it = this.mQuickItems.entrySet().iterator();
            while (it.hasNext()) {
                View value = it.next().getValue();
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-2, -2);
                layoutParams.setMargins(i, 0, 0, 0);
                value.setLayoutParams(layoutParams);
                this.mQuickSwitcherLayout.addView(value);
            }
            updateViewOrientation();
        }
    }

    private class OnOrientationChangeListenerImpl implements IApp.OnOrientationChangeListener {
        private OnOrientationChangeListenerImpl() {
        }

        @Override
        public void onOrientationChanged(int i) {
            if (QuickSwitcherManager.this.mOptionRoot != null && QuickSwitcherManager.this.mOptionRoot.getChildCount() != 0) {
                CameraUtil.rotateRotateLayoutChildView(QuickSwitcherManager.this.mApp.getActivity(), QuickSwitcherManager.this.mOptionRoot, i, true);
            }
        }
    }
}
