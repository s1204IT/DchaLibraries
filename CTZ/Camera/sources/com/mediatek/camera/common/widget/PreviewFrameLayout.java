package com.mediatek.camera.common.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class PreviewFrameLayout extends FrameLayout {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(PreviewFrameLayout.class.getSimpleName());
    private int mCurrentPriority;
    private int mPreviewHeight;
    private int mPreviewWidth;
    private ConcurrentSkipListMap<Integer, List<View>> mPriorityMap;

    public PreviewFrameLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mCurrentPriority = -1;
        this.mPriorityMap = new ConcurrentSkipListMap<>();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        int iIntValue;
        Iterator<Map.Entry<Integer, List<View>>> it = this.mPriorityMap.entrySet().iterator();
        loop0: while (true) {
            if (it.hasNext()) {
                Map.Entry<Integer, List<View>> next = it.next();
                List<View> value = next.getValue();
                LogHelper.d(TAG, "[dispatchDraw] While loop priority = " + next.getKey());
                for (View view : value) {
                    LogHelper.d(TAG, "[dispatchDraw] for loop view = " + view);
                    if (view.getVisibility() == 0) {
                        iIntValue = next.getKey().intValue();
                        break loop0;
                    }
                }
            } else {
                iIntValue = -1;
                break;
            }
        }
        if (this.mCurrentPriority != iIntValue) {
            LogHelper.d(TAG, "[dispatchDraw] currentPriority = " + iIntValue);
            this.mCurrentPriority = iIntValue;
        }
        if (iIntValue >= 0) {
            while (it.hasNext()) {
                Map.Entry<Integer, List<View>> next2 = it.next();
                if (next2.getKey().intValue() <= iIntValue) {
                    break;
                }
                Iterator<View> it2 = next2.getValue().iterator();
                while (it2.hasNext()) {
                    it2.next().setVisibility(4);
                }
            }
        }
        super.dispatchDraw(canvas);
    }

    public void registerView(View view, int i) {
        LogHelper.d(TAG, "registerView child = " + view + " priority " + i);
        if (this.mPriorityMap.containsKey(Integer.valueOf(i))) {
            this.mPriorityMap.get(Integer.valueOf(i)).add(view);
            return;
        }
        CopyOnWriteArrayList copyOnWriteArrayList = new CopyOnWriteArrayList();
        copyOnWriteArrayList.add(view);
        this.mPriorityMap.put(Integer.valueOf(i), copyOnWriteArrayList);
    }

    public void unRegisterView(View view) {
        LogHelper.d(TAG, "unRegisterView view = " + view);
        for (Map.Entry<Integer, List<View>> entry : this.mPriorityMap.entrySet()) {
            List<View> value = entry.getValue();
            LogHelper.d(TAG, "unRegisterView While loop priority = " + entry.getKey());
            if (value.contains(view)) {
                value.remove(view);
                LogHelper.d(TAG, "unRegisterView remove success");
            }
        }
    }

    public void setPreviewSize(int i, int i2) {
        LogHelper.d(TAG, "setPreviewSize width = " + i + " height = " + i2);
        this.mPreviewWidth = i;
        this.mPreviewHeight = i2;
        requestLayout();
    }

    @Override
    protected void onMeasure(int i, int i2) {
        super.onMeasure(View.MeasureSpec.makeMeasureSpec(this.mPreviewWidth, 1073741824), View.MeasureSpec.makeMeasureSpec(this.mPreviewHeight, 1073741824));
    }
}
