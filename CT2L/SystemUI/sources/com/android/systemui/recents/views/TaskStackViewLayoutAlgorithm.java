package com.android.systemui.recents.views;

import android.graphics.Rect;
import com.android.systemui.recents.RecentsConfiguration;
import com.android.systemui.recents.misc.Utilities;
import com.android.systemui.recents.model.Task;
import java.util.ArrayList;
import java.util.HashMap;

public class TaskStackViewLayoutAlgorithm {
    static float[] px;
    static float[] xp;
    int mBetweenAffiliationOffset;
    RecentsConfiguration mConfig;
    float mInitialScrollP;
    float mMaxScrollP;
    float mMinScrollP;
    int mWithinAffiliationOffset;
    Rect mViewRect = new Rect();
    Rect mStackVisibleRect = new Rect();
    Rect mStackRect = new Rect();
    Rect mTaskRect = new Rect();
    HashMap<Task.TaskKey, Float> mTaskProgressMap = new HashMap<>();

    public class VisibilityReport {
        public int numVisibleTasks;
        public int numVisibleThumbnails;

        VisibilityReport(int tasks, int thumbnails) {
            this.numVisibleTasks = tasks;
            this.numVisibleThumbnails = thumbnails;
        }
    }

    public TaskStackViewLayoutAlgorithm(RecentsConfiguration config) {
        this.mConfig = config;
        initializeCurve();
    }

    public void computeRects(int windowWidth, int windowHeight, Rect taskStackBounds) {
        this.mViewRect.set(0, 0, windowWidth, windowHeight);
        this.mStackRect.set(taskStackBounds);
        this.mStackVisibleRect.set(taskStackBounds);
        this.mStackVisibleRect.bottom = this.mViewRect.bottom;
        int widthPadding = (int) (this.mConfig.taskStackWidthPaddingPct * this.mStackRect.width());
        int heightPadding = this.mConfig.taskStackTopPaddingPx;
        this.mStackRect.inset(widthPadding, heightPadding);
        int size = this.mStackRect.width();
        int left = this.mStackRect.left + ((this.mStackRect.width() - size) / 2);
        this.mTaskRect.set(left, this.mStackRect.top, left + size, this.mStackRect.top + size);
        this.mWithinAffiliationOffset = this.mConfig.taskBarHeight;
        this.mBetweenAffiliationOffset = (int) (this.mTaskRect.height() * 0.5f);
    }

    void computeMinMaxScroll(ArrayList<Task> tasks, boolean launchedWithAltTab, boolean launchedFromHome) {
        this.mTaskProgressMap.clear();
        if (tasks.isEmpty()) {
            this.mMaxScrollP = 0.0f;
            this.mMinScrollP = 0.0f;
            return;
        }
        int taskHeight = this.mTaskRect.height();
        float pAtBottomOfStackRect = screenYToCurveProgress(this.mStackVisibleRect.bottom);
        float pWithinAffiliateTop = screenYToCurveProgress(this.mStackVisibleRect.bottom - this.mWithinAffiliationOffset);
        float scale = curveProgressToScale(pWithinAffiliateTop);
        int scaleYOffset = (int) (((1.0f - scale) * taskHeight) / 2.0f);
        float pWithinAffiliateTop2 = screenYToCurveProgress((this.mStackVisibleRect.bottom - this.mWithinAffiliationOffset) + scaleYOffset);
        float pWithinAffiliateOffset = pAtBottomOfStackRect - pWithinAffiliateTop2;
        float pBetweenAffiliateOffset = pAtBottomOfStackRect - screenYToCurveProgress(this.mStackVisibleRect.bottom - this.mBetweenAffiliationOffset);
        float pTaskHeightOffset = pAtBottomOfStackRect - screenYToCurveProgress(this.mStackVisibleRect.bottom - taskHeight);
        float pNavBarOffset = pAtBottomOfStackRect - screenYToCurveProgress(this.mStackVisibleRect.bottom - (this.mStackVisibleRect.bottom - this.mStackRect.bottom));
        float pAtFrontMostCardTop = 0.5f;
        int taskCount = tasks.size();
        for (int i = 0; i < taskCount; i++) {
            Task task = tasks.get(i);
            this.mTaskProgressMap.put(task.key, Float.valueOf(pAtFrontMostCardTop));
            if (i < taskCount - 1) {
                float pPeek = task.group.isFrontMostTask(task) ? pBetweenAffiliateOffset : pWithinAffiliateOffset;
                pAtFrontMostCardTop += pPeek;
            }
        }
        this.mMaxScrollP = pAtFrontMostCardTop - ((1.0f - pTaskHeightOffset) - pNavBarOffset);
        this.mMinScrollP = tasks.size() == 1 ? Math.max(this.mMaxScrollP, 0.0f) : 0.0f;
        if (launchedWithAltTab && launchedFromHome) {
            this.mInitialScrollP = this.mMaxScrollP;
        } else {
            this.mInitialScrollP = pAtFrontMostCardTop - 0.825f;
        }
        this.mInitialScrollP = Math.min(this.mMaxScrollP, Math.max(0.0f, this.mInitialScrollP));
    }

    public VisibilityReport computeStackVisibilityReport(ArrayList<Task> tasks) {
        if (tasks.size() <= 1) {
            return new VisibilityReport(1, 1);
        }
        int taskHeight = this.mTaskRect.height();
        int numVisibleTasks = 1;
        int numVisibleThumbnails = 1;
        int prevScreenY = curveProgressToScreenY(this.mTaskProgressMap.get(tasks.get(tasks.size() - 1).key).floatValue() - this.mInitialScrollP);
        int i = tasks.size() - 2;
        while (true) {
            if (i < 0) {
                break;
            }
            Task task = tasks.get(i);
            float progress = this.mTaskProgressMap.get(task.key).floatValue() - this.mInitialScrollP;
            if (progress < 0.0f) {
                break;
            }
            boolean isFrontMostTaskInGroup = task.group.isFrontMostTask(task);
            if (isFrontMostTaskInGroup) {
                float scaleAtP = curveProgressToScale(progress);
                int scaleYOffsetAtP = (int) (((1.0f - scaleAtP) * taskHeight) / 2.0f);
                int screenY = curveProgressToScreenY(progress) + scaleYOffsetAtP;
                boolean hasVisibleThumbnail = prevScreenY - screenY > this.mConfig.taskBarHeight;
                if (hasVisibleThumbnail) {
                    numVisibleThumbnails++;
                    numVisibleTasks++;
                    prevScreenY = screenY;
                } else {
                    for (int j = i; j >= 0; j--) {
                        numVisibleTasks++;
                        if (this.mTaskProgressMap.get(tasks.get(j).key).floatValue() - this.mInitialScrollP < 0.0f) {
                            break;
                        }
                    }
                }
            } else if (!isFrontMostTaskInGroup) {
                numVisibleTasks++;
            }
            i--;
        }
        return new VisibilityReport(numVisibleTasks, numVisibleThumbnails);
    }

    public TaskViewTransform getStackTransform(Task task, float stackScroll, TaskViewTransform transformOut, TaskViewTransform prevTransform) {
        if (task != null && this.mTaskProgressMap.containsKey(task.key)) {
            return getStackTransform(this.mTaskProgressMap.get(task.key).floatValue(), stackScroll, transformOut, prevTransform);
        }
        transformOut.reset();
        return transformOut;
    }

    public TaskViewTransform getStackTransform(float taskProgress, float stackScroll, TaskViewTransform transformOut, TaskViewTransform prevTransform) {
        float pTaskRelative = taskProgress - stackScroll;
        float pBounded = Math.max(0.0f, Math.min(pTaskRelative, 1.0f));
        if (pTaskRelative > 1.0f) {
            transformOut.reset();
            transformOut.rect.set(this.mTaskRect);
        } else if (pTaskRelative < 0.0f && prevTransform != null && Float.compare(prevTransform.p, 0.0f) <= 0) {
            transformOut.reset();
            transformOut.rect.set(this.mTaskRect);
        } else {
            float scale = curveProgressToScale(pBounded);
            int scaleYOffset = (int) (((1.0f - scale) * this.mTaskRect.height()) / 2.0f);
            int minZ = this.mConfig.taskViewTranslationZMinPx;
            int maxZ = this.mConfig.taskViewTranslationZMaxPx;
            transformOut.scale = scale;
            transformOut.translationY = (curveProgressToScreenY(pBounded) - this.mStackVisibleRect.top) - scaleYOffset;
            transformOut.translationZ = Math.max(minZ, minZ + ((maxZ - minZ) * pBounded));
            transformOut.rect.set(this.mTaskRect);
            transformOut.rect.offset(0, transformOut.translationY);
            Utilities.scaleRectAboutCenter(transformOut.rect, transformOut.scale);
            transformOut.visible = true;
            transformOut.p = pTaskRelative;
        }
        return transformOut;
    }

    public Rect getUntransformedTaskViewSize() {
        Rect tvSize = new Rect(this.mTaskRect);
        tvSize.offsetTo(0, 0);
        return tvSize;
    }

    float getStackScrollForTask(Task t) {
        if (this.mTaskProgressMap.containsKey(t.key)) {
            return this.mTaskProgressMap.get(t.key).floatValue();
        }
        return 0.0f;
    }

    public static void initializeCurve() {
        if (xp == null || px == null) {
            xp = new float[251];
            px = new float[251];
            float[] fx = new float[251];
            float x = 0.0f;
            for (int xStep = 0; xStep <= 250; xStep++) {
                fx[xStep] = logFunc(x);
                x += 0.004f;
            }
            float pLength = 0.0f;
            float[] dx = new float[251];
            dx[0] = 0.0f;
            for (int xStep2 = 1; xStep2 < 250; xStep2++) {
                dx[xStep2] = (float) Math.sqrt(Math.pow(fx[xStep2] - fx[xStep2 - 1], 2.0d) + Math.pow(0.004f, 2.0d));
                pLength += dx[xStep2];
            }
            float p = 0.0f;
            px[0] = 0.0f;
            px[250] = 1.0f;
            for (int xStep3 = 1; xStep3 <= 250; xStep3++) {
                p += Math.abs(dx[xStep3] / pLength);
                px[xStep3] = p;
            }
            int xStep4 = 0;
            float p2 = 0.0f;
            xp[0] = 0.0f;
            xp[250] = 1.0f;
            for (int pStep = 0; pStep < 250; pStep++) {
                while (xStep4 < 250 && px[xStep4] <= p2) {
                    xStep4++;
                }
                if (xStep4 == 0) {
                    xp[pStep] = 0.0f;
                } else {
                    float fraction = (p2 - px[xStep4 - 1]) / (px[xStep4] - px[xStep4 - 1]);
                    float x2 = ((xStep4 - 1) + fraction) * 0.004f;
                    xp[pStep] = x2;
                }
                p2 += 0.004f;
            }
        }
    }

    static float reverse(float x) {
        return ((-x) * 1.75f) + 1.0f;
    }

    static float logFunc(float x) {
        return 1.0f - (((float) Math.pow(3000.0d, reverse(x))) / 3000.0f);
    }

    int curveProgressToScreenY(float p) {
        if (p < 0.0f || p > 1.0f) {
            return this.mStackVisibleRect.top + ((int) (this.mStackVisibleRect.height() * p));
        }
        float pIndex = p * 250.0f;
        int pFloorIndex = (int) Math.floor(pIndex);
        int pCeilIndex = (int) Math.ceil(pIndex);
        float xFraction = 0.0f;
        if (pFloorIndex < 250 && pCeilIndex != pFloorIndex) {
            float pFraction = (pIndex - pFloorIndex) / (pCeilIndex - pFloorIndex);
            xFraction = (xp[pCeilIndex] - xp[pFloorIndex]) * pFraction;
        }
        float x = xp[pFloorIndex] + xFraction;
        return this.mStackVisibleRect.top + ((int) (this.mStackVisibleRect.height() * x));
    }

    float curveProgressToScale(float p) {
        if (p < 0.0f) {
            return 0.8f;
        }
        if (p > 1.0f) {
            return 1.0f;
        }
        return 0.8f + (p * 0.19999999f);
    }

    float screenYToCurveProgress(int screenY) {
        float x = (screenY - this.mStackVisibleRect.top) / this.mStackVisibleRect.height();
        if (x >= 0.0f && x <= 1.0f) {
            float xIndex = x * 250.0f;
            int xFloorIndex = (int) Math.floor(xIndex);
            int xCeilIndex = (int) Math.ceil(xIndex);
            float pFraction = 0.0f;
            if (xFloorIndex < 250 && xCeilIndex != xFloorIndex) {
                float xFraction = (xIndex - xFloorIndex) / (xCeilIndex - xFloorIndex);
                pFraction = (px[xCeilIndex] - px[xFloorIndex]) * xFraction;
            }
            return px[xFloorIndex] + pFraction;
        }
        return x;
    }
}
