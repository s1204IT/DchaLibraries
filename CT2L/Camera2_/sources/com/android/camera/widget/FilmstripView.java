package com.android.camera.widget;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.DecelerateInterpolator;
import android.widget.Scroller;
import com.android.camera.CameraActivity;
import com.android.camera.data.LocalData;
import com.android.camera.debug.Log;
import com.android.camera.filmstrip.DataAdapter;
import com.android.camera.filmstrip.FilmstripController;
import com.android.camera.filmstrip.ImageData;
import com.android.camera.ui.FilmstripGestureRecognizer;
import com.android.camera.ui.ZoomView;
import com.android.camera.util.ApiHelper;
import com.android.camera.util.CameraUtil;
import com.android.camera2.R;
import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;

public class FilmstripView extends ViewGroup {
    private static final int BUFFER_SIZE = 5;
    private static final int CAMERA_PREVIEW_SWIPE_THRESHOLD = 300;
    private static final int DECELERATION_FACTOR = 4;
    private static final float FILM_STRIP_SCALE = 0.7f;
    private static final float FLING_COASTING_DURATION_S = 0.05f;
    private static final float FULL_SCREEN_SCALE = 1.0f;
    private static final int GEOMETRY_ADJUST_TIME_MS = 400;
    private static final float PROMOTE_HEIGHT_RATIO = 0.5f;
    private static final float PROMOTE_VELOCITY = 3.5f;
    private static final int SNAP_IN_CENTER_TIME_MS = 600;
    private static final int SWIPE_TIME_OUT = 500;
    private static final Log.Tag TAG = new Log.Tag("FilmstripView");
    private static final float TOLERANCE = 0.1f;
    private static final float VELOCITY_PROMOTE_HEIGHT_RATIO = 0.1f;
    private static final int ZOOM_ANIMATION_DURATION_MS = 200;
    private LocalData.ActionCallback mActionCallback;
    private CameraActivity mActivity;
    private int mCenterX;
    private boolean mCheckToIntercept;
    private MyController mController;
    private final int mCurrentItem;
    private DataAdapter mDataAdapter;
    private int mDataIdOnUserScrolling;
    private MotionEvent mDown;
    private final Rect mDrawArea;
    private boolean mFullScreenUIHidden;
    private FilmstripGestureRecognizer.Listener mGestureListener;
    private FilmstripGestureRecognizer mGestureRecognizer;
    private boolean mIsUserScrolling;
    private FilmstripController.FilmstripListener mListener;
    private float mOverScaleFactor;
    private float mScale;
    private int mSlop;
    private TimeInterpolator mViewAnimInterpolator;
    private int mViewGapInPixel;
    private final ViewItem[] mViewItem;
    private ZoomView mZoomView;
    private final SparseArray<Queue<View>> recycledViews;

    static int access$1916(FilmstripView x0, float x1) {
        int i = (int) (x0.mCenterX + x1);
        x0.mCenterX = i;
        return i;
    }

    public static class ActionCallbackImpl implements LocalData.ActionCallback {
        private final WeakReference<Activity> mActivity;

        public ActionCallbackImpl(Activity activity) {
            this.mActivity = new WeakReference<>(activity);
        }

        @Override
        public void playVideo(Uri uri, String title) {
            Activity activity = this.mActivity.get();
            if (activity != null) {
                CameraUtil.playVideo(activity, uri, title);
            }
        }
    }

    private class ViewItem {
        private ValueAnimator mAlphaAnimator;
        private final ImageData mData;
        private int mDataId;
        private int mLeftPosition;
        private boolean mMaximumBitmapRequested;
        private ValueAnimator mTranslationXAnimator;
        private ValueAnimator mTranslationYAnimator;
        private final View mView;
        private final RectF mViewArea;

        public ViewItem(int id, View v, ImageData data) {
            v.setPivotX(0.0f);
            v.setPivotY(0.0f);
            this.mDataId = id;
            this.mData = data;
            this.mView = v;
            this.mMaximumBitmapRequested = false;
            this.mLeftPosition = -1;
            this.mViewArea = new RectF();
        }

        public boolean isMaximumBitmapRequested() {
            return this.mMaximumBitmapRequested;
        }

        public void setMaximumBitmapRequested() {
            this.mMaximumBitmapRequested = true;
        }

        public int getId() {
            return this.mDataId;
        }

        public void setId(int id) {
            this.mDataId = id;
        }

        public void setLeftPosition(int pos) {
            this.mLeftPosition = pos;
        }

        public int getLeftPosition() {
            return this.mLeftPosition;
        }

        public float getTranslationY() {
            return this.mView.getTranslationY() / FilmstripView.this.mScale;
        }

        public float getTranslationX() {
            return this.mView.getTranslationX() / FilmstripView.this.mScale;
        }

        public void setTranslationY(float transY) {
            this.mView.setTranslationY(FilmstripView.this.mScale * transY);
        }

        public void setTranslationX(float transX) {
            this.mView.setTranslationX(FilmstripView.this.mScale * transX);
        }

        public void setAlpha(float alpha) {
            this.mView.setAlpha(alpha);
        }

        public float getAlpha() {
            return this.mView.getAlpha();
        }

        public int getMeasuredWidth() {
            return this.mView.getMeasuredWidth();
        }

        public void animateTranslationX(float targetX, long duration_ms, TimeInterpolator interpolator) {
            if (this.mTranslationXAnimator == null) {
                this.mTranslationXAnimator = new ValueAnimator();
                this.mTranslationXAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        FilmstripView.this.invalidate();
                    }
                });
            }
            runAnimation(this.mTranslationXAnimator, getTranslationX(), targetX, duration_ms, interpolator);
        }

        public void animateTranslationY(float targetY, long duration_ms, TimeInterpolator interpolator) {
            if (this.mTranslationYAnimator == null) {
                this.mTranslationYAnimator = new ValueAnimator();
                this.mTranslationYAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        ViewItem.this.setTranslationY(((Float) valueAnimator.getAnimatedValue()).floatValue());
                    }
                });
            }
            runAnimation(this.mTranslationYAnimator, getTranslationY(), targetY, duration_ms, interpolator);
        }

        public void animateAlpha(float targetAlpha, long duration_ms, TimeInterpolator interpolator) {
            if (this.mAlphaAnimator == null) {
                this.mAlphaAnimator = new ValueAnimator();
                this.mAlphaAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        ViewItem.this.setAlpha(((Float) valueAnimator.getAnimatedValue()).floatValue());
                    }
                });
            }
            runAnimation(this.mAlphaAnimator, getAlpha(), targetAlpha, duration_ms, interpolator);
        }

        private void runAnimation(ValueAnimator animator, float startValue, float targetValue, long duration_ms, TimeInterpolator interpolator) {
            if (startValue != targetValue) {
                animator.setInterpolator(interpolator);
                animator.setDuration(duration_ms);
                animator.setFloatValues(startValue, targetValue);
                animator.start();
            }
        }

        public void translateXScaledBy(float transX) {
            setTranslationX(getTranslationX() + (FilmstripView.this.mScale * transX));
        }

        public void getHitRect(Rect rect) {
            this.mView.getHitRect(rect);
        }

        public int getCenterX() {
            return this.mLeftPosition + (this.mView.getMeasuredWidth() / 2);
        }

        public int getVisibility() {
            return this.mView.getVisibility();
        }

        public void setVisibility(int visibility) {
            this.mView.setVisibility(visibility);
        }

        public void resizeView(Context context, int w, int h) {
            FilmstripView.this.mDataAdapter.resizeView(context, this.mDataId, this.mView, w, h);
        }

        public void addViewToHierarchy() {
            if (FilmstripView.this.indexOfChild(this.mView) < 0) {
                this.mData.prepare();
                FilmstripView.this.addView(this.mView);
            }
            setVisibility(0);
            setAlpha(1.0f);
            setTranslationX(0.0f);
            setTranslationY(0.0f);
        }

        public void removeViewFromHierarchy(boolean force) {
            if (force || this.mData.getViewType() != 1) {
                FilmstripView.this.removeView(this.mView);
                this.mData.recycle(this.mView);
                FilmstripView.this.recycleView(this.mView, this.mDataId);
                return;
            }
            setVisibility(4);
        }

        public void bringViewToFront() {
            FilmstripView.this.bringChildToFront(this.mView);
        }

        public float getX() {
            return this.mView.getX();
        }

        public float getY() {
            return this.mView.getY();
        }

        public void measure(int widthSpec, int heightSpec) {
            this.mView.measure(widthSpec, heightSpec);
        }

        private void layoutAt(int left, int top) {
            this.mView.layout(left, top, this.mView.getMeasuredWidth() + left, this.mView.getMeasuredHeight() + top);
        }

        public RectF getViewRect() {
            RectF r = new RectF();
            r.left = this.mView.getX();
            r.top = this.mView.getY();
            r.right = r.left + (this.mView.getWidth() * this.mView.getScaleX());
            r.bottom = r.top + (this.mView.getHeight() * this.mView.getScaleY());
            return r;
        }

        private View getView() {
            return this.mView;
        }

        public void layoutWithTranslationX(Rect drawArea, int refCenter, float scale) {
            float translationX = (this.mTranslationXAnimator == null || !this.mTranslationXAnimator.isRunning()) ? 0.0f : ((Float) this.mTranslationXAnimator.getAnimatedValue()).floatValue();
            int left = (int) (drawArea.centerX() + (((this.mLeftPosition - refCenter) + translationX) * scale));
            int top = (int) (drawArea.centerY() - ((this.mView.getMeasuredHeight() / 2) * scale));
            layoutAt(left, top);
            this.mView.setScaleX(scale);
            this.mView.setScaleY(scale);
            int l = this.mView.getLeft();
            int t = this.mView.getTop();
            this.mViewArea.set(l, t, l + (this.mView.getMeasuredWidth() * scale), t + (this.mView.getMeasuredHeight() * scale));
        }

        public boolean areaContains(float x, float y) {
            return this.mViewArea.contains(x, y);
        }

        public int getWidth() {
            return this.mView.getWidth();
        }

        public int getDrawAreaLeft() {
            return Math.round(this.mViewArea.left);
        }

        public void copyAttributes(ViewItem item) {
            setLeftPosition(item.getLeftPosition());
            setTranslationX(item.getTranslationX());
            if (item.mTranslationXAnimator != null) {
                this.mTranslationXAnimator = item.mTranslationXAnimator;
                this.mTranslationXAnimator.removeAllUpdateListeners();
                this.mTranslationXAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        FilmstripView.this.invalidate();
                    }
                });
            }
            setTranslationY(item.getTranslationY());
            if (item.mTranslationYAnimator != null) {
                this.mTranslationYAnimator = item.mTranslationYAnimator;
                this.mTranslationYAnimator.removeAllUpdateListeners();
                this.mTranslationYAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        ViewItem.this.setTranslationY(((Float) valueAnimator.getAnimatedValue()).floatValue());
                    }
                });
            }
            setAlpha(item.getAlpha());
            if (item.mAlphaAnimator != null) {
                this.mAlphaAnimator = item.mAlphaAnimator;
                this.mAlphaAnimator.removeAllUpdateListeners();
                this.mAlphaAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        ViewItem.this.setAlpha(((Float) valueAnimator.getAnimatedValue()).floatValue());
                    }
                });
            }
        }

        void postScale(float focusX, float focusY, float postScale, int viewportWidth, int viewportHeight) {
            float transX = this.mView.getTranslationX();
            float transY = this.mView.getTranslationY();
            float transX2 = transX - ((focusX - getX()) * (postScale - 1.0f));
            float transY2 = transY - ((focusY - getY()) * (postScale - 1.0f));
            float scaleX = this.mView.getScaleX() * postScale;
            float scaleY = this.mView.getScaleY() * postScale;
            updateTransform(transX2, transY2, scaleX, scaleY, viewportWidth, viewportHeight);
        }

        void updateTransform(float transX, float transY, float scaleX, float scaleY, int viewportWidth, int viewportHeight) {
            float left = transX + this.mView.getLeft();
            float top = transY + this.mView.getTop();
            RectF r = ZoomView.adjustToFitInBounds(new RectF(left, top, (this.mView.getWidth() * scaleX) + left, (this.mView.getHeight() * scaleY) + top), viewportWidth, viewportHeight);
            this.mView.setScaleX(scaleX);
            this.mView.setScaleY(scaleY);
            float transX2 = r.left - this.mView.getLeft();
            float transY2 = r.top - this.mView.getTop();
            this.mView.setTranslationX(transX2);
            this.mView.setTranslationY(transY2);
        }

        void resetTransform() {
            this.mView.setScaleX(1.0f);
            this.mView.setScaleY(1.0f);
            this.mView.setTranslationX(0.0f);
            this.mView.setTranslationY(0.0f);
        }

        public String toString() {
            return "DataID = " + this.mDataId + "\n\t left = " + this.mLeftPosition + "\n\t viewArea = " + this.mViewArea + "\n\t centerX = " + getCenterX() + "\n\t view MeasuredSize = " + this.mView.getMeasuredWidth() + ',' + this.mView.getMeasuredHeight() + "\n\t view Size = " + this.mView.getWidth() + ',' + this.mView.getHeight() + "\n\t view scale = " + this.mView.getScaleX();
        }
    }

    public FilmstripView(Context context) {
        super(context);
        this.mDrawArea = new Rect();
        this.mCurrentItem = 2;
        this.mCenterX = -1;
        this.mViewItem = new ViewItem[5];
        this.mZoomView = null;
        this.mCheckToIntercept = true;
        this.mOverScaleFactor = 1.0f;
        this.mFullScreenUIHidden = false;
        this.recycledViews = new SparseArray<>();
        init((CameraActivity) context);
    }

    public FilmstripView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mDrawArea = new Rect();
        this.mCurrentItem = 2;
        this.mCenterX = -1;
        this.mViewItem = new ViewItem[5];
        this.mZoomView = null;
        this.mCheckToIntercept = true;
        this.mOverScaleFactor = 1.0f;
        this.mFullScreenUIHidden = false;
        this.recycledViews = new SparseArray<>();
        init((CameraActivity) context);
    }

    public FilmstripView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mDrawArea = new Rect();
        this.mCurrentItem = 2;
        this.mCenterX = -1;
        this.mViewItem = new ViewItem[5];
        this.mZoomView = null;
        this.mCheckToIntercept = true;
        this.mOverScaleFactor = 1.0f;
        this.mFullScreenUIHidden = false;
        this.recycledViews = new SparseArray<>();
        init((CameraActivity) context);
    }

    private void init(CameraActivity cameraActivity) {
        setWillNotDraw(false);
        this.mActivity = cameraActivity;
        this.mActionCallback = new ActionCallbackImpl(this.mActivity);
        this.mScale = 1.0f;
        this.mDataIdOnUserScrolling = 0;
        this.mController = new MyController(cameraActivity);
        this.mViewAnimInterpolator = new DecelerateInterpolator();
        this.mZoomView = new ZoomView(cameraActivity);
        this.mZoomView.setVisibility(8);
        addView(this.mZoomView);
        this.mGestureListener = new MyGestureReceiver();
        this.mGestureRecognizer = new FilmstripGestureRecognizer(cameraActivity, this.mGestureListener);
        this.mSlop = (int) getContext().getResources().getDimension(R.dimen.pie_touch_slop);
        DisplayMetrics metrics = new DisplayMetrics();
        this.mActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        this.mOverScaleFactor = metrics.densityDpi / 240.0f;
        if (this.mOverScaleFactor < 1.0f) {
            this.mOverScaleFactor = 1.0f;
        }
        setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override
            public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setClassName(FilmstripView.class.getName());
                info.setScrollable(true);
                info.addAction(4096);
                info.addAction(8192);
            }

            @Override
            public boolean performAccessibilityAction(View host, int action, Bundle args) {
                if (!FilmstripView.this.mController.isScrolling()) {
                    switch (action) {
                        case 64:
                            ViewItem currentItem = FilmstripView.this.mViewItem[2];
                            currentItem.getView().performAccessibilityAction(action, args);
                            return true;
                        case 4096:
                            FilmstripView.this.mController.goToNextItem();
                            return true;
                        case 8192:
                            boolean wentToPrevious = FilmstripView.this.mController.goToPreviousItem();
                            if (wentToPrevious) {
                                return true;
                            }
                            FilmstripView.this.mActivity.getCameraAppUI().hideFilmstrip();
                            return true;
                    }
                }
                return super.performAccessibilityAction(host, action, args);
            }
        });
    }

    private void recycleView(View view, int dataId) {
        int viewType = ((Integer) view.getTag(R.id.mediadata_tag_viewtype)).intValue();
        if (viewType > 0) {
            Queue<View> recycledViewsForType = this.recycledViews.get(viewType);
            if (recycledViewsForType == null) {
                recycledViewsForType = new ArrayDeque<>();
                this.recycledViews.put(viewType, recycledViewsForType);
            }
            recycledViewsForType.offer(view);
        }
    }

    private View getRecycledView(int dataId) {
        int viewType = this.mDataAdapter.getItemViewType(dataId);
        Queue<View> recycledViewsForType = this.recycledViews.get(viewType);
        if (recycledViewsForType == null) {
            return null;
        }
        View result = recycledViewsForType.poll();
        return result;
    }

    public FilmstripController getController() {
        return this.mController;
    }

    public int getCurrentItemLeft() {
        return this.mViewItem[2].getDrawAreaLeft();
    }

    private void setListener(FilmstripController.FilmstripListener l) {
        this.mListener = l;
    }

    private void setViewGap(int viewGap) {
        this.mViewGapInPixel = viewGap;
    }

    public void zoomAtIndexChanged() {
        if (this.mViewItem[2] != null) {
            int id = this.mViewItem[2].getId();
            this.mListener.onZoomAtIndexChanged(id, this.mScale);
        }
    }

    private boolean isDataAtCenter(int id) {
        return this.mViewItem[2] != null && this.mViewItem[2].getId() == id && isCurrentItemCentered();
    }

    private void measureViewItem(ViewItem item, int boundWidth, int boundHeight) {
        int id = item.getId();
        ImageData imageData = this.mDataAdapter.getImageData(id);
        if (imageData == null) {
            Log.e(TAG, "trying to measure a null item");
        } else {
            Point dim = CameraUtil.resizeToFill(imageData.getWidth(), imageData.getHeight(), imageData.getRotation(), boundWidth, boundHeight);
            item.measure(View.MeasureSpec.makeMeasureSpec(dim.x, 1073741824), View.MeasureSpec.makeMeasureSpec(dim.y, 1073741824));
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int boundWidth = View.MeasureSpec.getSize(widthMeasureSpec);
        int boundHeight = View.MeasureSpec.getSize(heightMeasureSpec);
        if (boundWidth != 0 && boundHeight != 0) {
            ViewItem[] arr$ = this.mViewItem;
            for (ViewItem item : arr$) {
                if (item != null) {
                    measureViewItem(item, boundWidth, boundHeight);
                }
            }
            clampCenterX();
            this.mZoomView.measure(View.MeasureSpec.makeMeasureSpec(widthMeasureSpec, 1073741824), View.MeasureSpec.makeMeasureSpec(heightMeasureSpec, 1073741824));
        }
    }

    private int findTheNearestView(int pointX) {
        int nearest = 0;
        while (nearest < 5 && (this.mViewItem[nearest] == null || this.mViewItem[nearest].getLeftPosition() == -1)) {
            nearest++;
        }
        if (nearest == 5) {
            return -1;
        }
        int min = Math.abs(pointX - this.mViewItem[nearest].getCenterX());
        for (int itemID = nearest + 1; itemID < 5 && this.mViewItem[itemID] != null; itemID++) {
            if (this.mViewItem[itemID].getLeftPosition() != -1) {
                int c = this.mViewItem[itemID].getCenterX();
                int dist = Math.abs(pointX - c);
                if (dist < min) {
                    min = dist;
                    nearest = itemID;
                }
            }
        }
        return nearest;
    }

    private ViewItem buildItemFromData(int dataID) {
        if (this.mActivity.isDestroyed()) {
            Log.d(TAG, "Activity destroyed, don't load data");
            return null;
        }
        ImageData data = this.mDataAdapter.getImageData(dataID);
        if (data == null) {
            return null;
        }
        int width = Math.round(getWidth() * FILM_STRIP_SCALE);
        int height = Math.round(getHeight() * FILM_STRIP_SCALE);
        Log.v(TAG, "suggesting item bounds: " + width + "x" + height);
        this.mDataAdapter.suggestViewSizeBound(width, height);
        data.prepare();
        View recycled = getRecycledView(dataID);
        View v = this.mDataAdapter.getView(this.mActivity.getAndroidContext(), recycled, dataID, this.mActionCallback);
        if (v == null) {
            return null;
        }
        ViewItem item = new ViewItem(dataID, v, data);
        item.addViewToHierarchy();
        return item;
    }

    private void checkItemAtMaxSize() {
        ViewItem item = this.mViewItem[2];
        if (!item.isMaximumBitmapRequested()) {
            item.setMaximumBitmapRequested();
            int id = item.getId();
            int h = this.mDataAdapter.getImageData(id).getHeight();
            int w = this.mDataAdapter.getImageData(id).getWidth();
            item.resizeView(this.mActivity, w, h);
        }
    }

    private void removeItem(int itemID) {
        if (itemID < this.mViewItem.length && this.mViewItem[itemID] != null) {
            ImageData data = this.mDataAdapter.getImageData(this.mViewItem[itemID].getId());
            if (data == null) {
                Log.e(TAG, "trying to remove a null item");
            } else {
                this.mViewItem[itemID].removeViewFromHierarchy(false);
                this.mViewItem[itemID] = null;
            }
        }
    }

    private void stepIfNeeded() {
        int nearest;
        if ((inFilmstrip() || inFullScreen()) && (nearest = findTheNearestView(this.mCenterX)) != -1 && nearest != 2) {
            int prevDataId = this.mViewItem[2] != null ? this.mViewItem[2].getId() : -1;
            int adjust = nearest - 2;
            if (adjust > 0) {
                for (int k = 0; k < adjust; k++) {
                    removeItem(k);
                }
                for (int k2 = 0; k2 + adjust < 5; k2++) {
                    this.mViewItem[k2] = this.mViewItem[k2 + adjust];
                }
                for (int k3 = 5 - adjust; k3 < 5; k3++) {
                    this.mViewItem[k3] = null;
                    if (this.mViewItem[k3 - 1] != null) {
                        this.mViewItem[k3] = buildItemFromData(this.mViewItem[k3 - 1].getId() + 1);
                    }
                }
                adjustChildZOrder();
            } else {
                for (int k4 = 4; k4 >= adjust + 5; k4--) {
                    removeItem(k4);
                }
                for (int k5 = 4; k5 + adjust >= 0; k5--) {
                    this.mViewItem[k5] = this.mViewItem[k5 + adjust];
                }
                for (int k6 = (-1) - adjust; k6 >= 0; k6--) {
                    this.mViewItem[k6] = null;
                    if (this.mViewItem[k6 + 1] != null) {
                        this.mViewItem[k6] = buildItemFromData(this.mViewItem[k6 + 1].getId() - 1);
                    }
                }
            }
            invalidate();
            if (this.mListener != null) {
                this.mListener.onDataFocusChanged(prevDataId, this.mViewItem[2].getId());
                int firstVisible = this.mViewItem[2].getId() - 2;
                int visibleItemCount = firstVisible + 5;
                int totalItemCount = this.mDataAdapter.getTotalNumber();
                this.mListener.onScroll(firstVisible, visibleItemCount, totalItemCount);
            }
            zoomAtIndexChanged();
        }
    }

    private boolean clampCenterX() {
        ViewItem curr = this.mViewItem[2];
        if (curr == null) {
            return false;
        }
        boolean stopScroll = false;
        if (curr.getId() == 1 && this.mCenterX < curr.getCenterX() && this.mDataIdOnUserScrolling > 1 && this.mDataAdapter.getImageData(0).getViewType() == 1 && this.mController.isScrolling()) {
            stopScroll = true;
        } else if (curr.getId() == 0 && this.mCenterX < curr.getCenterX()) {
            stopScroll = true;
        }
        if (curr.getId() == this.mDataAdapter.getTotalNumber() - 1 && this.mCenterX > curr.getCenterX()) {
            stopScroll = true;
        }
        if (stopScroll) {
            this.mCenterX = curr.getCenterX();
            return stopScroll;
        }
        return stopScroll;
    }

    private void adjustChildZOrder() {
        for (int i = 4; i >= 0; i--) {
            if (this.mViewItem[i] != null) {
                this.mViewItem[i].bringViewToFront();
            }
        }
        bringChildToFront(this.mZoomView);
        if (ApiHelper.isLOrHigher()) {
            setMaxElevation(this.mZoomView);
        }
    }

    @TargetApi(21)
    private void setMaxElevation(View v) {
        v.setElevation(Float.MAX_VALUE);
    }

    private int getCurrentId() {
        ViewItem current = this.mViewItem[2];
        if (current == null) {
            return -1;
        }
        return current.getId();
    }

    private void snapInCenter() {
        ViewItem currItem = this.mViewItem[2];
        if (currItem != null) {
            int currentViewCenter = currItem.getCenterX();
            if (!this.mController.isScrolling() && !this.mIsUserScrolling && !isCurrentItemCentered()) {
                int snapInTime = (int) ((600.0f * Math.abs(this.mCenterX - currentViewCenter)) / this.mDrawArea.width());
                this.mController.scrollToPosition(currentViewCenter, snapInTime, false);
                if (isViewTypeSticky(currItem) && !this.mController.isScaling() && this.mScale != 1.0f) {
                    this.mController.goToFullScreen();
                }
            }
        }
    }

    private void translateLeftViewItem(int currItem, int drawAreaWidth, float scaleFraction) {
        if (currItem < 0 || currItem > 4) {
            Log.e(TAG, "currItem id out of bound.");
            return;
        }
        ViewItem curr = this.mViewItem[currItem];
        ViewItem next = this.mViewItem[currItem + 1];
        if (curr == null || next == null) {
            Log.e(TAG, "Invalid view item (curr or next == null). curr = " + currItem);
            return;
        }
        int currCenterX = curr.getCenterX();
        int nextCenterX = next.getCenterX();
        int translate = (int) (((nextCenterX - drawAreaWidth) - currCenterX) * scaleFraction);
        curr.layoutWithTranslationX(this.mDrawArea, this.mCenterX, this.mScale);
        curr.setAlpha(1.0f);
        curr.setVisibility(0);
        if (inFullScreen()) {
            curr.setTranslationX(((this.mCenterX - currCenterX) * translate) / (nextCenterX - currCenterX));
        } else {
            curr.setTranslationX(translate);
        }
    }

    private void fadeAndScaleRightViewItem(int currItemId) {
        if (currItemId < 1 || currItemId > 5) {
            Log.e(TAG, "currItem id out of bound.");
            return;
        }
        ViewItem currItem = this.mViewItem[currItemId];
        ViewItem prevItem = this.mViewItem[currItemId - 1];
        if (currItem == null || prevItem == null) {
            Log.e(TAG, "Invalid view item (curr or prev == null). curr = " + currItemId);
            return;
        }
        if (currItemId > 3) {
            currItem.setVisibility(4);
            return;
        }
        int prevCenterX = prevItem.getCenterX();
        if (this.mCenterX <= prevCenterX) {
            currItem.setVisibility(4);
            return;
        }
        int currCenterX = currItem.getCenterX();
        float fadeDownFraction = (this.mCenterX - prevCenterX) / (currCenterX - prevCenterX);
        currItem.layoutWithTranslationX(this.mDrawArea, currCenterX, FILM_STRIP_SCALE + (0.3f * fadeDownFraction));
        currItem.setAlpha(fadeDownFraction);
        currItem.setTranslationX(0.0f);
        currItem.setVisibility(0);
    }

    private void layoutViewItems(boolean layoutChanged) {
        ViewItem curr;
        ViewItem curr2;
        if (this.mViewItem[2] != null && this.mDrawArea.width() != 0 && this.mDrawArea.height() != 0) {
            if (layoutChanged) {
                this.mViewItem[2].setLeftPosition(this.mCenterX - (this.mViewItem[2].getMeasuredWidth() / 2));
            }
            if (!inZoomView()) {
                float scaleFraction = this.mViewAnimInterpolator.getInterpolation((this.mScale - FILM_STRIP_SCALE) / 0.3f);
                int fullScreenWidth = this.mDrawArea.width() + this.mViewGapInPixel;
                for (int itemID = 1; itemID >= 0; itemID--) {
                    ViewItem curr3 = this.mViewItem[itemID];
                    if (curr3 == null) {
                        break;
                    }
                    int currLeft = (this.mViewItem[itemID + 1].getLeftPosition() - curr3.getMeasuredWidth()) - this.mViewGapInPixel;
                    curr3.setLeftPosition(currLeft);
                }
                for (int itemID2 = 3; itemID2 < 5 && (curr2 = this.mViewItem[itemID2]) != null; itemID2++) {
                    ViewItem prev = this.mViewItem[itemID2 - 1];
                    int currLeft2 = prev.getLeftPosition() + prev.getMeasuredWidth() + this.mViewGapInPixel;
                    curr2.setLeftPosition(currLeft2);
                }
                boolean immediateRight = this.mViewItem[2].getId() == 1 && this.mDataAdapter.getImageData(0).getViewType() == 1;
                if (immediateRight) {
                    ViewItem currItem = this.mViewItem[2];
                    currItem.layoutWithTranslationX(this.mDrawArea, this.mCenterX, this.mScale);
                    currItem.setTranslationX(0.0f);
                    currItem.setAlpha(1.0f);
                } else if (scaleFraction == 1.0f) {
                    ViewItem currItem2 = this.mViewItem[2];
                    int currCenterX = currItem2.getCenterX();
                    if (this.mCenterX < currCenterX) {
                        fadeAndScaleRightViewItem(2);
                    } else if (this.mCenterX > currCenterX) {
                        translateLeftViewItem(2, fullScreenWidth, scaleFraction);
                    } else {
                        currItem2.layoutWithTranslationX(this.mDrawArea, this.mCenterX, this.mScale);
                        currItem2.setTranslationX(0.0f);
                        currItem2.setAlpha(1.0f);
                    }
                } else {
                    ViewItem currItem3 = this.mViewItem[2];
                    currItem3.setTranslationX(currItem3.getTranslationX() * scaleFraction);
                    currItem3.layoutWithTranslationX(this.mDrawArea, this.mCenterX, this.mScale);
                    if (this.mViewItem[1] == null) {
                        currItem3.setAlpha(1.0f);
                    } else {
                        int currCenterX2 = currItem3.getCenterX();
                        int prevCenterX = this.mViewItem[1].getCenterX();
                        float fadeDownFraction = (this.mCenterX - prevCenterX) / (currCenterX2 - prevCenterX);
                        currItem3.setAlpha(((1.0f - fadeDownFraction) * (1.0f - scaleFraction)) + fadeDownFraction);
                    }
                }
                for (int itemID3 = 1; itemID3 >= 0 && this.mViewItem[itemID3] != null; itemID3--) {
                    translateLeftViewItem(itemID3, fullScreenWidth, scaleFraction);
                }
                for (int itemID4 = 3; itemID4 < 5 && (curr = this.mViewItem[itemID4]) != null; itemID4++) {
                    curr.layoutWithTranslationX(this.mDrawArea, this.mCenterX, this.mScale);
                    if (curr.getId() == 1 && isViewTypeSticky(curr)) {
                        curr.setAlpha(1.0f);
                    } else if (scaleFraction == 1.0f) {
                        fadeAndScaleRightViewItem(itemID4);
                    } else {
                        boolean setToVisible = curr.getVisibility() == 4;
                        if (itemID4 == 3) {
                            curr.setAlpha(1.0f - scaleFraction);
                        } else if (scaleFraction == 0.0f) {
                            curr.setAlpha(1.0f);
                        } else {
                            setToVisible = false;
                        }
                        if (setToVisible) {
                            curr.setVisibility(0);
                        }
                        curr.setTranslationX((this.mViewItem[2].getLeftPosition() - curr.getLeftPosition()) * scaleFraction);
                    }
                }
                stepIfNeeded();
            }
        }
    }

    private boolean isViewTypeSticky(ViewItem item) {
        if (item == null) {
            return false;
        }
        return this.mDataAdapter.getImageData(item.getId()).getViewType() == 1;
    }

    @Override
    public void onDraw(Canvas c) {
        layoutViewItems(false);
        super.onDraw(c);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        this.mDrawArea.left = 0;
        this.mDrawArea.top = 0;
        this.mDrawArea.right = r - l;
        this.mDrawArea.bottom = b - t;
        this.mZoomView.layout(this.mDrawArea.left, this.mDrawArea.top, this.mDrawArea.right, this.mDrawArea.bottom);
        if (!inZoomView() || changed) {
            resetZoomView();
            layoutViewItems(changed);
        }
    }

    private void resetZoomView() {
        ViewItem current;
        if (inZoomView() && (current = this.mViewItem[2]) != null) {
            this.mScale = 1.0f;
            this.mController.cancelZoomAnimation();
            this.mController.cancelFlingAnimation();
            current.resetTransform();
            this.mController.cancelLoadingZoomedImage();
            this.mZoomView.setVisibility(8);
            this.mController.setSurroundingViewsVisible(true);
        }
    }

    private void hideZoomView() {
        if (!inZoomView()) {
            return;
        }
        this.mController.cancelLoadingZoomedImage();
        this.mZoomView.setVisibility(8);
    }

    private void slideViewBack(ViewItem item) {
        item.animateTranslationX(0.0f, 400L, this.mViewAnimInterpolator);
        item.animateTranslationY(0.0f, 400L, this.mViewAnimInterpolator);
        item.animateAlpha(1.0f, 400L, this.mViewAnimInterpolator);
    }

    private void animateItemRemoval(int dataID, ImageData data) {
        if (this.mScale > 1.0f) {
            resetZoomView();
        }
        int removedItemId = findItemByDataID(dataID);
        for (int i = 0; i < 5; i++) {
            if (this.mViewItem[i] != null && this.mViewItem[i].getId() > dataID) {
                this.mViewItem[i].setId(this.mViewItem[i].getId() - 1);
            }
        }
        if (removedItemId != -1) {
            final ViewItem removedItem = this.mViewItem[removedItemId];
            int offsetX = removedItem.getMeasuredWidth() + this.mViewGapInPixel;
            for (int i2 = removedItemId + 1; i2 < 5; i2++) {
                if (this.mViewItem[i2] != null) {
                    this.mViewItem[i2].setLeftPosition(this.mViewItem[i2].getLeftPosition() - offsetX);
                }
            }
            if (removedItemId >= 2 && this.mViewItem[removedItemId].getId() < this.mDataAdapter.getTotalNumber()) {
                for (int i3 = removedItemId; i3 < 4; i3++) {
                    this.mViewItem[i3] = this.mViewItem[i3 + 1];
                }
                int prev = 4 - 1;
                if (this.mViewItem[prev] != null) {
                    this.mViewItem[4] = buildItemFromData(this.mViewItem[prev].getId() + 1);
                }
                if (inFullScreen()) {
                    this.mViewItem[2].setVisibility(0);
                    ViewItem nextItem = this.mViewItem[3];
                    if (nextItem != null) {
                        nextItem.setVisibility(4);
                    }
                }
                for (int i4 = removedItemId; i4 < 5; i4++) {
                    if (this.mViewItem[i4] != null) {
                        this.mViewItem[i4].setTranslationX(offsetX);
                    }
                }
                ViewItem currItem = this.mViewItem[2];
                if (currItem != null) {
                    if (currItem.getId() == this.mDataAdapter.getTotalNumber() - 1 && this.mCenterX > currItem.getCenterX()) {
                        int adjustDiff = currItem.getCenterX() - this.mCenterX;
                        this.mCenterX = currItem.getCenterX();
                        for (int i5 = 0; i5 < 5; i5++) {
                            if (this.mViewItem[i5] != null) {
                                this.mViewItem[i5].translateXScaledBy(adjustDiff);
                            }
                        }
                    }
                } else {
                    Log.w(TAG, "Caught invalid update in removal animation.");
                }
            } else {
                this.mCenterX -= offsetX;
                for (int i6 = removedItemId; i6 > 0; i6--) {
                    this.mViewItem[i6] = this.mViewItem[i6 - 1];
                }
                int next = 0 + 1;
                if (this.mViewItem[next] != null) {
                    this.mViewItem[0] = buildItemFromData(this.mViewItem[next].getId() - 1);
                }
                for (int i7 = removedItemId; i7 >= 0; i7--) {
                    if (this.mViewItem[i7] != null) {
                        this.mViewItem[i7].setTranslationX(-offsetX);
                    }
                }
            }
            int transY = getHeight() / 8;
            if (removedItem.getTranslationY() < 0.0f) {
                transY = -transY;
            }
            removedItem.animateTranslationY(removedItem.getTranslationY() + transY, 400L, this.mViewAnimInterpolator);
            removedItem.animateAlpha(0.0f, 400L, this.mViewAnimInterpolator);
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    removedItem.removeViewFromHierarchy(false);
                }
            }, 400L);
            adjustChildZOrder();
            invalidate();
            if (this.mViewItem[2] != null) {
                for (int i8 = 0; i8 < 5; i8++) {
                    if (this.mViewItem[i8] != null && this.mViewItem[i8].getTranslationX() != 0.0f) {
                        slideViewBack(this.mViewItem[i8]);
                    }
                }
                if (isCurrentItemCentered() && isViewTypeSticky(this.mViewItem[2])) {
                    this.mController.goToFullScreen();
                }
            }
        }
    }

    private int findItemByDataID(int dataID) {
        for (int i = 0; i < 5; i++) {
            if (this.mViewItem[i] != null && this.mViewItem[i].getId() == dataID) {
                return i;
            }
        }
        return -1;
    }

    private void updateInsertion(int dataID) {
        int prev;
        int insertedItemId = findItemByDataID(dataID);
        if (insertedItemId == -1 && dataID == this.mDataAdapter.getTotalNumber() - 1 && (prev = findItemByDataID(dataID - 1)) >= 0 && prev < 4) {
            insertedItemId = prev + 1;
        }
        for (int i = 0; i < 5; i++) {
            if (this.mViewItem[i] != null && this.mViewItem[i].getId() >= dataID) {
                this.mViewItem[i].setId(this.mViewItem[i].getId() + 1);
            }
        }
        if (insertedItemId != -1) {
            ImageData data = this.mDataAdapter.getImageData(dataID);
            Point dim = CameraUtil.resizeToFill(data.getWidth(), data.getHeight(), data.getRotation(), getMeasuredWidth(), getMeasuredHeight());
            int offsetX = dim.x + this.mViewGapInPixel;
            ViewItem viewItem = buildItemFromData(dataID);
            if (viewItem == null) {
                Log.w(TAG, "unable to build inserted item from data");
                return;
            }
            if (insertedItemId >= 2) {
                if (insertedItemId == 2) {
                    viewItem.setLeftPosition(this.mViewItem[2].getLeftPosition());
                }
                removeItem(4);
                for (int i2 = 4; i2 > insertedItemId; i2--) {
                    this.mViewItem[i2] = this.mViewItem[i2 - 1];
                    if (this.mViewItem[i2] != null) {
                        this.mViewItem[i2].setTranslationX(-offsetX);
                        slideViewBack(this.mViewItem[i2]);
                    }
                }
            } else {
                insertedItemId--;
                if (insertedItemId >= 0) {
                    removeItem(0);
                    for (int i3 = 1; i3 <= insertedItemId; i3++) {
                        if (this.mViewItem[i3] != null) {
                            this.mViewItem[i3].setTranslationX(offsetX);
                            slideViewBack(this.mViewItem[i3]);
                            this.mViewItem[i3 - 1] = this.mViewItem[i3];
                        }
                    }
                } else {
                    return;
                }
            }
            this.mViewItem[insertedItemId] = viewItem;
            viewItem.setAlpha(0.0f);
            viewItem.setTranslationY(getHeight() / 8);
            slideViewBack(viewItem);
            adjustChildZOrder();
            invalidate();
        }
    }

    private void setDataAdapter(DataAdapter adapter) {
        this.mDataAdapter = adapter;
        int maxEdge = (int) (Math.max(getHeight(), getWidth()) * FILM_STRIP_SCALE);
        this.mDataAdapter.suggestViewSizeBound(maxEdge, maxEdge);
        this.mDataAdapter.setListener(new DataAdapter.Listener() {
            @Override
            public void onDataLoaded() {
                FilmstripView.this.reload();
            }

            @Override
            public void onDataUpdated(DataAdapter.UpdateReporter reporter) {
                FilmstripView.this.update(reporter);
            }

            @Override
            public void onDataInserted(int dataId, ImageData data) {
                if (FilmstripView.this.mViewItem[2] == null) {
                    FilmstripView.this.reload();
                } else {
                    FilmstripView.this.updateInsertion(dataId);
                }
                if (FilmstripView.this.mListener != null) {
                    FilmstripView.this.mListener.onDataFocusChanged(dataId, FilmstripView.this.getCurrentId());
                }
            }

            @Override
            public void onDataRemoved(int dataId, ImageData data) {
                FilmstripView.this.animateItemRemoval(dataId, data);
                if (FilmstripView.this.mListener != null) {
                    FilmstripView.this.mListener.onDataFocusChanged(dataId, FilmstripView.this.getCurrentId());
                }
            }
        });
    }

    private boolean inFilmstrip() {
        return this.mScale == FILM_STRIP_SCALE;
    }

    private boolean inFullScreen() {
        return this.mScale == 1.0f;
    }

    private boolean inZoomView() {
        return this.mScale > 1.0f;
    }

    private boolean isCameraPreview() {
        return isViewTypeSticky(this.mViewItem[2]);
    }

    private boolean inCameraFullscreen() {
        return isDataAtCenter(0) && inFullScreen() && isViewTypeSticky(this.mViewItem[2]);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (this.mController.isScrolling()) {
            return true;
        }
        if (ev.getActionMasked() == 0) {
            this.mCheckToIntercept = true;
            this.mDown = MotionEvent.obtain(ev);
            ViewItem viewItem = this.mViewItem[2];
            if (viewItem != null && !this.mDataAdapter.canSwipeInFullScreen(viewItem.getId())) {
                this.mCheckToIntercept = false;
            }
            return false;
        }
        if (ev.getActionMasked() == 5) {
            this.mCheckToIntercept = false;
            return false;
        }
        if (this.mCheckToIntercept && ev.getEventTime() - ev.getDownTime() <= 500) {
            int deltaX = (int) (ev.getX() - this.mDown.getX());
            int deltaY = (int) (ev.getY() - this.mDown.getY());
            return ev.getActionMasked() == 2 && deltaX < this.mSlop * (-1) && Math.abs(deltaX) >= Math.abs(deltaY) * 2;
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return this.mGestureRecognizer.onTouchEvent(ev);
    }

    FilmstripGestureRecognizer.Listener getGestureListener() {
        return this.mGestureListener;
    }

    private void updateViewItem(int itemID) {
        ViewItem item = this.mViewItem[itemID];
        if (item == null) {
            Log.e(TAG, "trying to update an null item");
            return;
        }
        item.removeViewFromHierarchy(true);
        ViewItem newItem = buildItemFromData(item.getId());
        if (newItem == null) {
            Log.e(TAG, "new item is null");
            item.addViewToHierarchy();
            return;
        }
        newItem.copyAttributes(item);
        this.mViewItem[itemID] = newItem;
        this.mZoomView.resetDecoder();
        boolean stopScroll = clampCenterX();
        if (stopScroll) {
            this.mController.stopScrolling(true);
        }
        adjustChildZOrder();
        invalidate();
        if (this.mListener != null) {
            this.mListener.onDataUpdated(newItem.getId());
        }
    }

    private void update(DataAdapter.UpdateReporter reporter) {
        if (this.mViewItem[2] == null) {
            reload();
            return;
        }
        ViewItem curr = this.mViewItem[2];
        int dataId = curr.getId();
        if (reporter.isDataRemoved(dataId)) {
            reload();
            return;
        }
        if (reporter.isDataUpdated(dataId)) {
            updateViewItem(2);
            ImageData data = this.mDataAdapter.getImageData(dataId);
            if (!this.mIsUserScrolling && !this.mController.isScrolling()) {
                Point dim = CameraUtil.resizeToFill(data.getWidth(), data.getHeight(), data.getRotation(), getMeasuredWidth(), getMeasuredHeight());
                this.mCenterX = curr.getLeftPosition() + (dim.x / 2);
            }
        }
        for (int i = 1; i >= 0; i--) {
            ViewItem curr2 = this.mViewItem[i];
            if (curr2 != null) {
                int dataId2 = curr2.getId();
                if (reporter.isDataRemoved(dataId2) || reporter.isDataUpdated(dataId2)) {
                    updateViewItem(i);
                }
            } else {
                ViewItem next = this.mViewItem[i + 1];
                if (next != null) {
                    this.mViewItem[i] = buildItemFromData(next.getId() - 1);
                }
            }
        }
        for (int i2 = 3; i2 < 5; i2++) {
            ViewItem curr3 = this.mViewItem[i2];
            if (curr3 != null) {
                int dataId3 = curr3.getId();
                if (reporter.isDataRemoved(dataId3) || reporter.isDataUpdated(dataId3)) {
                    updateViewItem(i2);
                }
            } else {
                ViewItem prev = this.mViewItem[i2 - 1];
                if (prev != null) {
                    this.mViewItem[i2] = buildItemFromData(prev.getId() + 1);
                }
            }
        }
        adjustChildZOrder();
        requestLayout();
    }

    private void reload() {
        this.mController.stopScrolling(true);
        this.mController.stopScale();
        this.mDataIdOnUserScrolling = 0;
        int prevId = -1;
        if (this.mViewItem[2] != null) {
            prevId = this.mViewItem[2].getId();
        }
        for (int i = 0; i < this.mViewItem.length; i++) {
            if (this.mViewItem[i] != null) {
                this.mViewItem[i].removeViewFromHierarchy(false);
            }
        }
        Arrays.fill(this.mViewItem, (Object) null);
        int dataNumber = this.mDataAdapter.getTotalNumber();
        if (dataNumber != 0) {
            this.mViewItem[2] = buildItemFromData(0);
            if (this.mViewItem[2] != null) {
                this.mViewItem[2].setLeftPosition(0);
                for (int i2 = 3; i2 < 5; i2++) {
                    this.mViewItem[i2] = buildItemFromData(this.mViewItem[i2 - 1].getId() + 1);
                    if (this.mViewItem[i2] == null) {
                        break;
                    }
                }
                this.mCenterX = -1;
                this.mScale = FILM_STRIP_SCALE;
                adjustChildZOrder();
                invalidate();
                if (this.mListener != null) {
                    this.mListener.onDataReloaded();
                    this.mListener.onDataFocusChanged(prevId, this.mViewItem[2].getId());
                }
            }
        }
    }

    private void promoteData(int itemID, int dataID) {
        if (this.mListener != null) {
            this.mListener.onFocusedDataPromoted(dataID);
        }
    }

    private void demoteData(int itemID, int dataID) {
        if (this.mListener != null) {
            this.mListener.onFocusedDataDemoted(dataID);
        }
    }

    private void onEnterFilmstrip() {
        if (this.mListener != null) {
            this.mListener.onEnterFilmstrip(getCurrentId());
        }
    }

    private void onLeaveFilmstrip() {
        if (this.mListener != null) {
            this.mListener.onLeaveFilmstrip(getCurrentId());
        }
    }

    private void onEnterFullScreen() {
        this.mFullScreenUIHidden = false;
        if (this.mListener != null) {
            this.mListener.onEnterFullScreenUiShown(getCurrentId());
        }
    }

    private void onLeaveFullScreen() {
        if (this.mListener != null) {
            this.mListener.onLeaveFullScreenUiShown(getCurrentId());
        }
    }

    private void onEnterFullScreenUiHidden() {
        this.mFullScreenUIHidden = true;
        if (this.mListener != null) {
            this.mListener.onEnterFullScreenUiHidden(getCurrentId());
        }
    }

    private void onLeaveFullScreenUiHidden() {
        this.mFullScreenUIHidden = false;
        if (this.mListener != null) {
            this.mListener.onLeaveFullScreenUiHidden(getCurrentId());
        }
    }

    private void onEnterZoomView() {
        if (this.mListener != null) {
            this.mListener.onEnterZoomView(getCurrentId());
        }
    }

    private void onLeaveZoomView() {
        this.mController.setSurroundingViewsVisible(true);
    }

    private class MyController implements FilmstripController {
        private boolean mCanStopScroll;
        private AnimatorSet mFlingAnimator;
        private final ValueAnimator mScaleAnimator;
        private final MyScroller mScroller;
        private ValueAnimator mZoomAnimator;
        private final MyScroller.Listener mScrollerListener = new MyScroller.Listener() {
            @Override
            public void onScrollUpdate(int currX, int currY) {
                FilmstripView.this.mCenterX = currX;
                boolean stopScroll = FilmstripView.this.clampCenterX();
                if (stopScroll) {
                    FilmstripView.this.mController.stopScrolling(true);
                }
                FilmstripView.this.invalidate();
            }

            @Override
            public void onScrollEnd() {
                MyController.this.mCanStopScroll = true;
                if (FilmstripView.this.mViewItem[2] != null) {
                    FilmstripView.this.snapInCenter();
                    if (FilmstripView.this.isCurrentItemCentered() && FilmstripView.this.isViewTypeSticky(FilmstripView.this.mViewItem[2])) {
                        MyController.this.goToFullScreen();
                    }
                }
            }
        };
        private final ValueAnimator.AnimatorUpdateListener mScaleAnimatorUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (FilmstripView.this.mViewItem[2] != null) {
                    FilmstripView.this.mScale = ((Float) animation.getAnimatedValue()).floatValue();
                    FilmstripView.this.invalidate();
                }
            }
        };

        MyController(Context context) {
            TimeInterpolator decelerateInterpolator = new DecelerateInterpolator(1.5f);
            this.mScroller = new MyScroller(FilmstripView.this.mActivity.getAndroidContext(), new Handler(FilmstripView.this.mActivity.getMainLooper()), this.mScrollerListener, decelerateInterpolator);
            this.mCanStopScroll = true;
            this.mScaleAnimator = new ValueAnimator();
            this.mScaleAnimator.addUpdateListener(this.mScaleAnimatorUpdateListener);
            this.mScaleAnimator.setInterpolator(decelerateInterpolator);
            this.mScaleAnimator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animator) {
                    if (FilmstripView.this.mScale == 1.0f) {
                        FilmstripView.this.onLeaveFullScreen();
                    } else if (FilmstripView.this.mScale == FilmstripView.FILM_STRIP_SCALE) {
                        FilmstripView.this.onLeaveFilmstrip();
                    }
                }

                @Override
                public void onAnimationEnd(Animator animator) {
                    if (FilmstripView.this.mScale == 1.0f) {
                        FilmstripView.this.onEnterFullScreen();
                    } else if (FilmstripView.this.mScale == FilmstripView.FILM_STRIP_SCALE) {
                        FilmstripView.this.onEnterFilmstrip();
                    }
                    FilmstripView.this.zoomAtIndexChanged();
                }

                @Override
                public void onAnimationCancel(Animator animator) {
                }

                @Override
                public void onAnimationRepeat(Animator animator) {
                }
            });
        }

        @Override
        public void setImageGap(int imageGap) {
            FilmstripView.this.setViewGap(imageGap);
        }

        @Override
        public int getCurrentId() {
            return FilmstripView.this.getCurrentId();
        }

        @Override
        public void setDataAdapter(DataAdapter adapter) {
            FilmstripView.this.setDataAdapter(adapter);
        }

        @Override
        public boolean inFilmstrip() {
            return FilmstripView.this.inFilmstrip();
        }

        @Override
        public boolean inFullScreen() {
            return FilmstripView.this.inFullScreen();
        }

        @Override
        public boolean isCameraPreview() {
            return FilmstripView.this.isCameraPreview();
        }

        @Override
        public boolean inCameraFullscreen() {
            return FilmstripView.this.inCameraFullscreen();
        }

        @Override
        public void setListener(FilmstripController.FilmstripListener l) {
            FilmstripView.this.setListener(l);
        }

        @Override
        public boolean isScrolling() {
            return !this.mScroller.isFinished();
        }

        @Override
        public boolean isScaling() {
            return this.mScaleAnimator.isRunning();
        }

        private int estimateMinX(int dataID, int leftPos, int viewWidth) {
            return leftPos - ((dataID + 100) * (FilmstripView.this.mViewGapInPixel + viewWidth));
        }

        private int estimateMaxX(int dataID, int leftPos, int viewWidth) {
            return (((FilmstripView.this.mDataAdapter.getTotalNumber() - dataID) + 100) * (FilmstripView.this.mViewGapInPixel + viewWidth)) + leftPos;
        }

        private void zoomAt(final ViewItem current, final float focusX, final float focusY) {
            if (this.mZoomAnimator != null) {
                this.mZoomAnimator.end();
            }
            float maxScale = getCurrentDataMaxScale(false);
            final float endScale = FilmstripView.this.mScale < maxScale - (0.1f * maxScale) ? maxScale : 1.0f;
            this.mZoomAnimator = new ValueAnimator();
            this.mZoomAnimator.setFloatValues(FilmstripView.this.mScale, endScale);
            this.mZoomAnimator.setDuration(200L);
            this.mZoomAnimator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    if (FilmstripView.this.mScale == 1.0f) {
                        if (FilmstripView.this.mFullScreenUIHidden) {
                            FilmstripView.this.onLeaveFullScreenUiHidden();
                        } else {
                            FilmstripView.this.onLeaveFullScreen();
                        }
                        MyController.this.setSurroundingViewsVisible(false);
                    } else if (MyController.this.inZoomView()) {
                        FilmstripView.this.onLeaveZoomView();
                    }
                    MyController.this.cancelLoadingZoomedImage();
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (FilmstripView.this.mScale != endScale) {
                        current.postScale(focusX, focusY, endScale / FilmstripView.this.mScale, FilmstripView.this.mDrawArea.width(), FilmstripView.this.mDrawArea.height());
                        FilmstripView.this.mScale = endScale;
                    }
                    if (MyController.this.inFullScreen()) {
                        MyController.this.setSurroundingViewsVisible(true);
                        FilmstripView.this.mZoomView.setVisibility(8);
                        current.resetTransform();
                        FilmstripView.this.onEnterFullScreenUiHidden();
                    } else {
                        FilmstripView.this.mController.loadZoomedImage();
                        FilmstripView.this.onEnterZoomView();
                    }
                    MyController.this.mZoomAnimator = null;
                    FilmstripView.this.zoomAtIndexChanged();
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }
            });
            this.mZoomAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float newScale = ((Float) animation.getAnimatedValue()).floatValue();
                    float postScale = newScale / FilmstripView.this.mScale;
                    FilmstripView.this.mScale = newScale;
                    current.postScale(focusX, focusY, postScale, FilmstripView.this.mDrawArea.width(), FilmstripView.this.mDrawArea.height());
                }
            });
            this.mZoomAnimator.start();
        }

        @Override
        public void scroll(float deltaX) {
            if (stopScrolling(false)) {
                FilmstripView.access$1916(FilmstripView.this, deltaX);
                boolean stopScroll = FilmstripView.this.clampCenterX();
                if (stopScroll) {
                    FilmstripView.this.mController.stopScrolling(true);
                }
                FilmstripView.this.invalidate();
            }
        }

        @Override
        public void fling(float velocityX) {
            ViewItem item;
            if (stopScrolling(false) && (item = FilmstripView.this.mViewItem[2]) != null) {
                float scaledVelocityX = velocityX / FilmstripView.this.mScale;
                if (inFullScreen() && FilmstripView.this.isViewTypeSticky(item) && scaledVelocityX < 0.0f) {
                    goToFilmstrip();
                }
                int w = FilmstripView.this.getWidth();
                int minX = estimateMinX(item.getId(), item.getLeftPosition(), w);
                int maxX = estimateMaxX(item.getId(), item.getLeftPosition(), w);
                this.mScroller.fling(FilmstripView.this.mCenterX, 0, (int) (-velocityX), 0, minX, maxX, 0, 0);
            }
        }

        void flingInsideZoomView(float velocityX, float velocityY) {
            final ViewItem current;
            if (inZoomView() && (current = FilmstripView.this.mViewItem[2]) != null) {
                float velocity = Math.max(Math.abs(velocityX), Math.abs(velocityY));
                float duration = (float) (0.05000000074505806d * Math.pow(velocity, 0.3333333432674408d));
                float translationX = current.getTranslationX() * FilmstripView.this.mScale;
                float translationY = current.getTranslationY() * FilmstripView.this.mScale;
                final ValueAnimator decelerationX = ValueAnimator.ofFloat(translationX, ((duration / 4.0f) * velocityX) + translationX);
                final ValueAnimator decelerationY = ValueAnimator.ofFloat(translationY, ((duration / 4.0f) * velocityY) + translationY);
                decelerationY.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        float transX = ((Float) decelerationX.getAnimatedValue()).floatValue();
                        float transY = ((Float) decelerationY.getAnimatedValue()).floatValue();
                        current.updateTransform(transX, transY, FilmstripView.this.mScale, FilmstripView.this.mScale, FilmstripView.this.mDrawArea.width(), FilmstripView.this.mDrawArea.height());
                    }
                });
                this.mFlingAnimator = new AnimatorSet();
                this.mFlingAnimator.play(decelerationX).with(decelerationY);
                this.mFlingAnimator.setDuration((int) (1000.0f * duration));
                this.mFlingAnimator.setInterpolator(new TimeInterpolator() {
                    @Override
                    public float getInterpolation(float input) {
                        return (float) (1.0d - Math.pow(1.0f - input, 4.0d));
                    }
                });
                this.mFlingAnimator.addListener(new Animator.AnimatorListener() {
                    private boolean mCancelled = false;

                    @Override
                    public void onAnimationStart(Animator animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (!this.mCancelled) {
                            MyController.this.loadZoomedImage();
                        }
                        MyController.this.mFlingAnimator = null;
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        this.mCancelled = true;
                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {
                    }
                });
                this.mFlingAnimator.start();
            }
        }

        @Override
        public boolean stopScrolling(boolean forced) {
            if (!isScrolling()) {
                return true;
            }
            if (!this.mCanStopScroll && !forced) {
                return false;
            }
            this.mScroller.forceFinished(true);
            return true;
        }

        private void stopScale() {
            this.mScaleAnimator.cancel();
        }

        @Override
        public void scrollToPosition(int position, int duration, boolean interruptible) {
            if (FilmstripView.this.mViewItem[2] != null) {
                this.mCanStopScroll = interruptible;
                this.mScroller.startScroll(FilmstripView.this.mCenterX, 0, position - FilmstripView.this.mCenterX, 0, duration);
            }
        }

        @Override
        public boolean goToNextItem() {
            return goToItem(3);
        }

        @Override
        public boolean goToPreviousItem() {
            return goToItem(1);
        }

        private boolean goToItem(int itemIndex) {
            ViewItem nextItem = FilmstripView.this.mViewItem[itemIndex];
            if (nextItem == null) {
                return false;
            }
            stopScrolling(true);
            scrollToPosition(nextItem.getCenterX(), 800, false);
            if (FilmstripView.this.isViewTypeSticky(FilmstripView.this.mViewItem[2])) {
                scaleTo(FilmstripView.FILM_STRIP_SCALE, 400);
            }
            return true;
        }

        private void scaleTo(float scale, int duration) {
            if (FilmstripView.this.mViewItem[2] != null) {
                stopScale();
                this.mScaleAnimator.setDuration(duration);
                this.mScaleAnimator.setFloatValues(FilmstripView.this.mScale, scale);
                this.mScaleAnimator.start();
            }
        }

        @Override
        public void goToFilmstrip() {
            if (FilmstripView.this.mViewItem[2] != null && FilmstripView.this.mScale != FilmstripView.FILM_STRIP_SCALE) {
                scaleTo(FilmstripView.FILM_STRIP_SCALE, 400);
                ViewItem currItem = FilmstripView.this.mViewItem[2];
                ViewItem nextItem = FilmstripView.this.mViewItem[3];
                if (currItem.getId() == 0 && FilmstripView.this.isViewTypeSticky(currItem) && nextItem != null) {
                    scrollToPosition(nextItem.getCenterX(), 400, false);
                }
                if (FilmstripView.this.mScale == FilmstripView.FILM_STRIP_SCALE) {
                    FilmstripView.this.onLeaveFilmstrip();
                }
            }
        }

        @Override
        public void goToFullScreen() {
            if (!inFullScreen()) {
                scaleTo(1.0f, 400);
            }
        }

        private void cancelFlingAnimation() {
            if (isFlingAnimationRunning()) {
                this.mFlingAnimator.cancel();
            }
        }

        private void cancelZoomAnimation() {
            if (isZoomAnimationRunning()) {
                this.mZoomAnimator.cancel();
            }
        }

        private void setSurroundingViewsVisible(boolean visible) {
            for (int i = 0; i < 2; i++) {
                if (i != 2 && FilmstripView.this.mViewItem[i] != null) {
                    FilmstripView.this.mViewItem[i].setVisibility(visible ? 0 : 4);
                }
            }
        }

        private Uri getCurrentUri() {
            ViewItem curr = FilmstripView.this.mViewItem[2];
            if (curr != null) {
                return FilmstripView.this.mDataAdapter.getImageData(curr.getId()).getUri();
            }
            return Uri.EMPTY;
        }

        private float getCurrentDataMaxScale(boolean allowOverScale) {
            ImageData imageData;
            ViewItem curr = FilmstripView.this.mViewItem[2];
            if (curr == null || (imageData = FilmstripView.this.mDataAdapter.getImageData(curr.getId())) == null || !imageData.isUIActionSupported(4)) {
                return 1.0f;
            }
            float imageWidth = imageData.getWidth();
            if (imageData.getRotation() == 90 || imageData.getRotation() == 270) {
                imageWidth = imageData.getHeight();
            }
            float scale = imageWidth / curr.getWidth();
            if (allowOverScale) {
                return scale * FilmstripView.this.mOverScaleFactor;
            }
            return scale;
        }

        private void loadZoomedImage() {
            ViewItem curr;
            if (inZoomView() && (curr = FilmstripView.this.mViewItem[2]) != null) {
                ImageData imageData = FilmstripView.this.mDataAdapter.getImageData(curr.getId());
                if (imageData.isUIActionSupported(4)) {
                    Uri uri = getCurrentUri();
                    RectF viewRect = curr.getViewRect();
                    if (uri != null && uri != Uri.EMPTY) {
                        int orientation = imageData.getRotation();
                        FilmstripView.this.mZoomView.loadBitmap(uri, orientation, viewRect);
                    }
                }
            }
        }

        private void cancelLoadingZoomedImage() {
            FilmstripView.this.mZoomView.cancelPartialDecodingTask();
        }

        @Override
        public void goToFirstItem() {
            if (FilmstripView.this.mViewItem[2] != null) {
                FilmstripView.this.resetZoomView();
                FilmstripView.this.reload();
            }
        }

        public boolean inZoomView() {
            return FilmstripView.this.inZoomView();
        }

        public boolean isFlingAnimationRunning() {
            return this.mFlingAnimator != null && this.mFlingAnimator.isRunning();
        }

        public boolean isZoomAnimationRunning() {
            return this.mZoomAnimator != null && this.mZoomAnimator.isRunning();
        }
    }

    private boolean isCurrentItemCentered() {
        return this.mViewItem[2].getCenterX() == this.mCenterX;
    }

    private static class MyScroller {
        private final Handler mHandler;
        private final Listener mListener;
        private final Scroller mScroller;
        private final Runnable mScrollChecker = new Runnable() {
            @Override
            public void run() {
                boolean newPosition = MyScroller.this.mScroller.computeScrollOffset();
                if (!newPosition) {
                    MyScroller.this.mListener.onScrollEnd();
                    return;
                }
                MyScroller.this.mListener.onScrollUpdate(MyScroller.this.mScroller.getCurrX(), MyScroller.this.mScroller.getCurrY());
                MyScroller.this.mHandler.removeCallbacks(this);
                MyScroller.this.mHandler.post(this);
            }
        };
        private final ValueAnimator.AnimatorUpdateListener mXScrollAnimatorUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                MyScroller.this.mListener.onScrollUpdate(((Integer) animation.getAnimatedValue()).intValue(), 0);
            }
        };
        private final Animator.AnimatorListener mXScrollAnimatorListener = new Animator.AnimatorListener() {
            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                MyScroller.this.mListener.onScrollEnd();
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }

            @Override
            public void onAnimationStart(Animator animation) {
            }
        };
        private final ValueAnimator mXScrollAnimator = new ValueAnimator();

        public interface Listener {
            void onScrollEnd();

            void onScrollUpdate(int i, int i2);
        }

        public MyScroller(Context ctx, Handler handler, Listener listener, TimeInterpolator interpolator) {
            this.mHandler = handler;
            this.mListener = listener;
            this.mScroller = new Scroller(ctx);
            this.mXScrollAnimator.addUpdateListener(this.mXScrollAnimatorUpdateListener);
            this.mXScrollAnimator.addListener(this.mXScrollAnimatorListener);
            this.mXScrollAnimator.setInterpolator(interpolator);
        }

        public void fling(int startX, int startY, int velocityX, int velocityY, int minX, int maxX, int minY, int maxY) {
            this.mScroller.fling(startX, startY, velocityX, velocityY, minX, maxX, minY, maxY);
            runChecker();
        }

        public void startScroll(int startX, int startY, int dx, int dy) {
            this.mScroller.startScroll(startX, startY, dx, dy);
            runChecker();
        }

        public void startScroll(int startX, int startY, int dx, int dy, int duration) {
            this.mXScrollAnimator.cancel();
            this.mXScrollAnimator.setDuration(duration);
            this.mXScrollAnimator.setIntValues(startX, startX + dx);
            this.mXScrollAnimator.start();
        }

        public boolean isFinished() {
            return this.mScroller.isFinished() && !this.mXScrollAnimator.isRunning();
        }

        public void forceFinished(boolean finished) {
            this.mScroller.forceFinished(finished);
            if (finished) {
                this.mXScrollAnimator.cancel();
            }
        }

        private void runChecker() {
            if (this.mHandler != null && this.mListener != null) {
                this.mHandler.removeCallbacks(this.mScrollChecker);
                this.mHandler.post(this.mScrollChecker);
            }
        }
    }

    private class MyGestureReceiver implements FilmstripGestureRecognizer.Listener {
        private static final int SCROLL_DIR_HORIZONTAL = 2;
        private static final int SCROLL_DIR_NONE = 0;
        private static final int SCROLL_DIR_VERTICAL = 1;
        private long mLastDownTime;
        private float mLastDownY;
        private float mMaxScale;
        private float mScaleTrend;
        private int mScrollingDirection;

        private MyGestureReceiver() {
            this.mScrollingDirection = 0;
        }

        @Override
        public boolean onSingleTapUp(float x, float y) {
            ViewItem centerItem = FilmstripView.this.mViewItem[2];
            if (!FilmstripView.this.inFilmstrip()) {
                if (FilmstripView.this.inFullScreen()) {
                    if (FilmstripView.this.mFullScreenUIHidden) {
                        FilmstripView.this.onLeaveFullScreenUiHidden();
                        FilmstripView.this.onEnterFullScreen();
                        return true;
                    }
                    FilmstripView.this.onLeaveFullScreen();
                    FilmstripView.this.onEnterFullScreenUiHidden();
                    return true;
                }
            } else if (centerItem != null && centerItem.areaContains(x, y)) {
                FilmstripView.this.mController.goToFullScreen();
                return true;
            }
            return false;
        }

        @Override
        public boolean onDoubleTap(float x, float y) {
            ViewItem current = FilmstripView.this.mViewItem[2];
            if (current == null) {
                return false;
            }
            if (FilmstripView.this.inFilmstrip()) {
                FilmstripView.this.mController.goToFullScreen();
                return true;
            }
            if (FilmstripView.this.mScale < 1.0f || FilmstripView.this.inCameraFullscreen() || !FilmstripView.this.mController.stopScrolling(false)) {
                return false;
            }
            if (FilmstripView.this.inFullScreen()) {
                FilmstripView.this.mController.zoomAt(current, x, y);
                FilmstripView.this.checkItemAtMaxSize();
                return true;
            }
            if (FilmstripView.this.mScale <= 1.0f) {
                return false;
            }
            FilmstripView.this.mController.zoomAt(current, x, y);
            return false;
        }

        @Override
        public boolean onDown(float x, float y) {
            this.mLastDownTime = SystemClock.uptimeMillis();
            this.mLastDownY = y;
            FilmstripView.this.mController.cancelFlingAnimation();
            return FilmstripView.this.mController.stopScrolling(false);
        }

        @Override
        public boolean onUp(float x, float y) {
            if (FilmstripView.this.mViewItem[2] != null && !FilmstripView.this.mController.isZoomAnimationRunning() && !FilmstripView.this.mController.isFlingAnimationRunning()) {
                if (FilmstripView.this.inZoomView()) {
                    FilmstripView.this.mController.loadZoomedImage();
                    return true;
                }
                float promoteHeight = FilmstripView.this.getHeight() * FilmstripView.PROMOTE_HEIGHT_RATIO;
                float velocityPromoteHeight = FilmstripView.this.getHeight() * 0.1f;
                FilmstripView.this.mIsUserScrolling = false;
                this.mScrollingDirection = 0;
                float speedY = Math.abs(y - this.mLastDownY) / (SystemClock.uptimeMillis() - this.mLastDownTime);
                for (int i = 0; i < 5; i++) {
                    if (FilmstripView.this.mViewItem[i] != null) {
                        float transY = FilmstripView.this.mViewItem[i].getTranslationY();
                        if (transY != 0.0f) {
                            int id = FilmstripView.this.mViewItem[i].getId();
                            if (!FilmstripView.this.mDataAdapter.getImageData(id).isUIActionSupported(2) || (transY <= promoteHeight && (transY <= velocityPromoteHeight || speedY <= FilmstripView.PROMOTE_VELOCITY))) {
                                if (!FilmstripView.this.mDataAdapter.getImageData(id).isUIActionSupported(1) || (transY >= (-promoteHeight) && (transY >= (-velocityPromoteHeight) || speedY <= FilmstripView.PROMOTE_VELOCITY))) {
                                    FilmstripView.this.slideViewBack(FilmstripView.this.mViewItem[i]);
                                } else {
                                    FilmstripView.this.promoteData(i, id);
                                }
                            } else {
                                FilmstripView.this.demoteData(i, id);
                            }
                        }
                    }
                }
                ViewItem currItem = FilmstripView.this.mViewItem[2];
                if (currItem == null) {
                    return true;
                }
                int currId = currItem.getId();
                if (FilmstripView.this.mCenterX > currItem.getCenterX() + 300 && currId == 0 && FilmstripView.this.isViewTypeSticky(currItem) && FilmstripView.this.mDataIdOnUserScrolling == 0) {
                    FilmstripView.this.mController.goToFilmstrip();
                    if (FilmstripView.this.mViewItem[3] != null) {
                        FilmstripView.this.mController.scrollToPosition(FilmstripView.this.mViewItem[3].getCenterX(), 400, false);
                    } else {
                        FilmstripView.this.snapInCenter();
                    }
                }
                if (!FilmstripView.this.isCurrentItemCentered() || currId != 0 || !FilmstripView.this.isViewTypeSticky(currItem)) {
                    if (FilmstripView.this.mDataIdOnUserScrolling == 0 && currId != 0) {
                        FilmstripView.this.mController.goToFilmstrip();
                        FilmstripView.this.mDataIdOnUserScrolling = currId;
                    }
                    FilmstripView.this.snapInCenter();
                } else {
                    FilmstripView.this.mController.goToFullScreen();
                }
                return false;
            }
            return false;
        }

        @Override
        public void onLongPress(float x, float y) {
            int dataId = FilmstripView.this.getCurrentId();
            if (dataId != -1) {
                FilmstripView.this.mListener.onFocusedDataLongPressed(dataId);
            }
        }

        @Override
        public boolean onScroll(float x, float y, float dx, float dy) {
            ViewItem currItem = FilmstripView.this.mViewItem[2];
            if (currItem != null) {
                if (!FilmstripView.this.inFullScreen() || FilmstripView.this.mDataAdapter.canSwipeInFullScreen(currItem.getId())) {
                    FilmstripView.this.hideZoomView();
                    if (FilmstripView.this.inZoomView()) {
                        ViewItem curr = FilmstripView.this.mViewItem[2];
                        float transX = (curr.getTranslationX() * FilmstripView.this.mScale) - dx;
                        curr.updateTransform(transX, (curr.getTranslationY() * FilmstripView.this.mScale) - dy, FilmstripView.this.mScale, FilmstripView.this.mScale, FilmstripView.this.mDrawArea.width(), FilmstripView.this.mDrawArea.height());
                        return true;
                    }
                    int deltaX = (int) (dx / FilmstripView.this.mScale);
                    FilmstripView.this.mController.stopScrolling(true);
                    if (!FilmstripView.this.mIsUserScrolling) {
                        FilmstripView.this.mIsUserScrolling = true;
                        FilmstripView.this.mDataIdOnUserScrolling = FilmstripView.this.mViewItem[2].getId();
                    }
                    if (!FilmstripView.this.inFilmstrip()) {
                        if (FilmstripView.this.inFullScreen()) {
                            if (FilmstripView.this.mViewItem[2] != null && (deltaX >= 0 || FilmstripView.this.mCenterX > currItem.getCenterX() || currItem.getId() != 0)) {
                                FilmstripView.this.mController.scroll((int) (((double) deltaX) * 1.2d));
                            } else {
                                return false;
                            }
                        }
                    } else {
                        if (this.mScrollingDirection == 0) {
                            this.mScrollingDirection = Math.abs(dx) > Math.abs(dy) ? 2 : 1;
                        }
                        if (this.mScrollingDirection == 2) {
                            if (FilmstripView.this.mCenterX != currItem.getCenterX() || currItem.getId() != 0 || dx >= 0.0f) {
                                FilmstripView.this.mController.scroll(deltaX);
                            } else {
                                FilmstripView.this.mIsUserScrolling = false;
                                this.mScrollingDirection = 0;
                                return false;
                            }
                        } else {
                            int hit = 0;
                            Rect hitRect = new Rect();
                            while (hit < 5) {
                                if (FilmstripView.this.mViewItem[hit] != null) {
                                    FilmstripView.this.mViewItem[hit].getHitRect(hitRect);
                                    if (hitRect.contains((int) x, (int) y)) {
                                        break;
                                    }
                                }
                                hit++;
                            }
                            if (hit != 5) {
                                ImageData data = FilmstripView.this.mDataAdapter.getImageData(FilmstripView.this.mViewItem[hit].getId());
                                float transY = FilmstripView.this.mViewItem[hit].getTranslationY() - (dy / FilmstripView.this.mScale);
                                if (!data.isUIActionSupported(2) && transY > 0.0f) {
                                    transY = 0.0f;
                                }
                                if (!data.isUIActionSupported(1) && transY < 0.0f) {
                                    transY = 0.0f;
                                }
                                FilmstripView.this.mViewItem[hit].setTranslationY(transY);
                            } else {
                                return true;
                            }
                        }
                    }
                    FilmstripView.this.invalidate();
                    return true;
                }
                return false;
            }
            return false;
        }

        @Override
        public boolean onFling(float velocityX, float velocityY) {
            ViewItem currItem = FilmstripView.this.mViewItem[2];
            if (currItem == null || !FilmstripView.this.mDataAdapter.canSwipeInFullScreen(currItem.getId())) {
                return false;
            }
            if (FilmstripView.this.inZoomView()) {
                FilmstripView.this.mController.flingInsideZoomView(velocityX, velocityY);
                return true;
            }
            if (Math.abs(velocityX) < Math.abs(velocityY)) {
                return true;
            }
            if (FilmstripView.this.mScale == 1.0f) {
                int currItemCenterX = currItem.getCenterX();
                if (velocityX > 0.0f) {
                    if (FilmstripView.this.mCenterX > currItemCenterX) {
                        FilmstripView.this.mController.scrollToPosition(currItemCenterX, 400, true);
                        return true;
                    }
                    ViewItem prevItem = FilmstripView.this.mViewItem[1];
                    if (prevItem == null) {
                        return false;
                    }
                    FilmstripView.this.mController.scrollToPosition(prevItem.getCenterX(), 400, true);
                } else if (FilmstripView.this.mController.stopScrolling(false)) {
                    if (FilmstripView.this.mCenterX < currItemCenterX) {
                        FilmstripView.this.mController.scrollToPosition(currItemCenterX, 400, true);
                        return true;
                    }
                    ViewItem nextItem = FilmstripView.this.mViewItem[3];
                    if (nextItem == null) {
                        return false;
                    }
                    FilmstripView.this.mController.scrollToPosition(nextItem.getCenterX(), 400, true);
                    if (FilmstripView.this.isViewTypeSticky(currItem)) {
                        FilmstripView.this.mController.goToFilmstrip();
                    }
                }
            }
            if (FilmstripView.this.mScale == FilmstripView.FILM_STRIP_SCALE) {
                FilmstripView.this.mController.fling(velocityX);
            }
            return true;
        }

        @Override
        public boolean onScaleBegin(float focusX, float focusY) {
            if (!FilmstripView.this.inCameraFullscreen()) {
                FilmstripView.this.hideZoomView();
                this.mScaleTrend = 1.0f;
                this.mMaxScale = Math.max(FilmstripView.this.mController.getCurrentDataMaxScale(true), 1.0f);
                return true;
            }
            return false;
        }

        @Override
        public boolean onScale(float focusX, float focusY, float scale) {
            if (FilmstripView.this.inCameraFullscreen()) {
                return false;
            }
            this.mScaleTrend = (this.mScaleTrend * 0.3f) + (scale * FilmstripView.FILM_STRIP_SCALE);
            float newScale = FilmstripView.this.mScale * scale;
            if (FilmstripView.this.mScale < 1.0f && newScale < 1.0f) {
                if (newScale <= FilmstripView.FILM_STRIP_SCALE) {
                    newScale = FilmstripView.FILM_STRIP_SCALE;
                }
                if (FilmstripView.this.mScale != newScale) {
                    if (FilmstripView.this.mScale == FilmstripView.FILM_STRIP_SCALE) {
                        FilmstripView.this.onLeaveFilmstrip();
                    }
                    if (newScale == FilmstripView.FILM_STRIP_SCALE) {
                        FilmstripView.this.onEnterFilmstrip();
                    }
                }
                FilmstripView.this.mScale = newScale;
                FilmstripView.this.invalidate();
            } else if (FilmstripView.this.mScale < 1.0f && newScale >= 1.0f) {
                if (FilmstripView.this.mScale == FilmstripView.FILM_STRIP_SCALE) {
                    FilmstripView.this.onLeaveFilmstrip();
                }
                FilmstripView.this.mScale = 1.0f;
                FilmstripView.this.onEnterFullScreen();
                FilmstripView.this.mController.setSurroundingViewsVisible(false);
                FilmstripView.this.invalidate();
            } else if (FilmstripView.this.mScale < 1.0f || newScale >= 1.0f) {
                if (!FilmstripView.this.inZoomView()) {
                    FilmstripView.this.mController.setSurroundingViewsVisible(false);
                }
                ViewItem curr = FilmstripView.this.mViewItem[2];
                float newScale2 = Math.min(newScale, this.mMaxScale);
                if (newScale2 == FilmstripView.this.mScale) {
                    return true;
                }
                float postScale = newScale2 / FilmstripView.this.mScale;
                curr.postScale(focusX, focusY, postScale, FilmstripView.this.mDrawArea.width(), FilmstripView.this.mDrawArea.height());
                FilmstripView.this.mScale = newScale2;
                if (FilmstripView.this.mScale == 1.0f) {
                    FilmstripView.this.onEnterFullScreen();
                } else {
                    FilmstripView.this.onEnterZoomView();
                }
                FilmstripView.this.checkItemAtMaxSize();
            } else {
                if (FilmstripView.this.inFullScreen()) {
                    if (FilmstripView.this.mFullScreenUIHidden) {
                        FilmstripView.this.onLeaveFullScreenUiHidden();
                    } else {
                        FilmstripView.this.onLeaveFullScreen();
                    }
                } else {
                    FilmstripView.this.onLeaveZoomView();
                }
                FilmstripView.this.mScale = newScale;
                FilmstripView.this.onEnterFilmstrip();
                FilmstripView.this.invalidate();
            }
            return true;
        }

        @Override
        public void onScaleEnd() {
            FilmstripView.this.zoomAtIndexChanged();
            if (FilmstripView.this.mScale <= 1.1f) {
                FilmstripView.this.mController.setSurroundingViewsVisible(true);
                if (FilmstripView.this.mScale <= 0.8f) {
                    FilmstripView.this.mController.goToFilmstrip();
                } else if (this.mScaleTrend > 1.0f || FilmstripView.this.mScale > 0.9f) {
                    if (FilmstripView.this.inZoomView()) {
                        FilmstripView.this.mScale = 1.0f;
                        FilmstripView.this.resetZoomView();
                    }
                    FilmstripView.this.mController.goToFullScreen();
                } else {
                    FilmstripView.this.mController.goToFilmstrip();
                }
                this.mScaleTrend = 1.0f;
            }
        }
    }
}
