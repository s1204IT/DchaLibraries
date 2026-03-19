package com.mediatek.camera.ui;

import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import com.mediatek.camera.R;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

public class IndicatorViewManager extends AbstractViewManager {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(IndicatorViewManager.class.getSimpleName());
    private ConcurrentSkipListMap<Integer, View> mIndicatorItems;
    private LinearLayout mIndicatorViewLayout;

    public IndicatorViewManager(IApp iApp, ViewGroup viewGroup) {
        super(iApp, viewGroup);
        this.mIndicatorItems = new ConcurrentSkipListMap<>();
    }

    @Override
    protected View getView() {
        this.mIndicatorViewLayout = (LinearLayout) this.mParentView.findViewById(R.id.indicator_view);
        updateQuickItems();
        return this.mIndicatorViewLayout;
    }

    @Override
    public void setEnabled(boolean z) {
        if (this.mIndicatorViewLayout != null) {
            int childCount = this.mIndicatorViewLayout.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View childAt = this.mIndicatorViewLayout.getChildAt(i);
                childAt.setEnabled(z);
                childAt.setClickable(z);
            }
        }
    }

    public void addToIndicatorView(View view, int i) {
        LogHelper.d(TAG, "[registerToIndicatorView] priority = " + i);
        if (this.mIndicatorItems.size() > 5) {
            LogHelper.w(TAG, "already reach to limit number : 5");
        } else if (!this.mIndicatorItems.containsValue(view)) {
            this.mIndicatorItems.put(Integer.valueOf(i), view);
            updateQuickItems();
        }
    }

    public void removeFromIndicatorView(View view) {
        if (this.mIndicatorItems.containsValue(view)) {
            for (Map.Entry<Integer, View> entry : this.mIndicatorItems.entrySet()) {
                View value = entry.getValue();
                if (value == view) {
                    this.mIndicatorItems.remove(Integer.valueOf(entry.getKey().intValue()), value);
                }
            }
            updateQuickItems();
        }
    }

    private void updateQuickItems() {
        int i = (int) (20.0f * this.mApp.getActivity().getResources().getDisplayMetrics().density);
        if (this.mIndicatorViewLayout != null && this.mIndicatorViewLayout.getChildCount() != 0) {
            this.mIndicatorViewLayout.removeAllViews();
        }
        if (this.mIndicatorViewLayout != null) {
            Iterator<Map.Entry<Integer, View>> it = this.mIndicatorItems.entrySet().iterator();
            int i2 = 0;
            while (it.hasNext()) {
                View value = it.next().getValue();
                value.setLayoutParams(new ViewGroup.LayoutParams(-2, -2));
                if (i2 != 0) {
                    value.setPadding(0, i, 0, 0);
                }
                this.mIndicatorViewLayout.addView(value);
                i2++;
            }
            updateViewOrientation();
        }
    }
}
