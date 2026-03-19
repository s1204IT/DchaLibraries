package com.android.music;

import android.content.Context;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ListView;

public class TouchInterceptor extends ListView {
    private final ContentObserver mContentObserver;
    private Bitmap mDragBitmap;
    private DragListener mDragListener;
    private int mDragPointX;
    private int mDragPointY;
    private int mDragPos;
    private ImageView mDragView;
    private DropListener mDropListener;
    private GestureDetector mGestureDetector;
    private int mHeight;
    private boolean mIsDraw;
    private int mItemHeightExpanded;
    private int mItemHeightHalf;
    private int mItemHeightNormal;
    private int mListViewHeight;
    private int mListviewWidth;
    private int mLowerBound;
    ViewTreeObserver.OnPreDrawListener mPreDrawListener;
    private RemoveListener mRemoveListener;
    private int mRemoveMode;
    private Object mSizeChangedLock;
    private int mSrcDragPos;
    private int mSrcDragPosition;
    private Rect mTempRect;
    private final int mTouchSlop;
    private Drawable mTrashcan;
    private UpgradeAlbumArtListener mUpgradeAlbumArtListener;
    private int mUpperBound;
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mWindowParams;
    private int mXOffset;
    private int mYOffset;

    public interface DragListener {
        void drag(int i, int i2);
    }

    public interface DropListener {
        void drop(int i, int i2);
    }

    public interface RemoveListener {
        void remove(int i);
    }

    public interface UpgradeAlbumArtListener {
        void UpgradeAlbumArt();
    }

    public TouchInterceptor(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mRemoveMode = -1;
        this.mTempRect = new Rect();
        this.mSizeChangedLock = new Object();
        this.mListviewWidth = 0;
        this.mListViewHeight = 0;
        this.mIsDraw = true;
        this.mContentObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean z) {
                super.onChange(z);
                TouchInterceptor.this.mIsDraw = true;
                MusicLogUtils.v("TouchInterceptor", "onChange: mIsDraw = " + TouchInterceptor.this.mIsDraw);
            }
        };
        this.mPreDrawListener = new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                MusicLogUtils.v("TouchInterceptor", "onPreDraw: mIsDraw = " + TouchInterceptor.this.mIsDraw);
                return TouchInterceptor.this.mIsDraw;
            }
        };
        this.mRemoveMode = context.getSharedPreferences("Music", 0).getInt("deletemode", -1);
        this.mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        Resources resources = getResources();
        this.mItemHeightNormal = resources.getDimensionPixelSize(R.dimen.normal_height);
        this.mItemHeightHalf = this.mItemHeightNormal / 2;
        this.mItemHeightExpanded = resources.getDimensionPixelSize(R.dimen.expanded_height);
        getRootView().getViewTreeObserver().addOnPreDrawListener(this.mPreDrawListener);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        int x;
        int y;
        int iPointToPosition;
        if (this.mRemoveListener != null && this.mGestureDetector == null && this.mRemoveMode == 0) {
            this.mGestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onFling(MotionEvent motionEvent2, MotionEvent motionEvent3, float f, float f2) {
                    if (TouchInterceptor.this.mDragView == null) {
                        return false;
                    }
                    if (f > 1000.0f) {
                        Rect rect = TouchInterceptor.this.mTempRect;
                        TouchInterceptor.this.mDragView.getDrawingRect(rect);
                        if (motionEvent3.getX() > (((double) rect.right) * 2.0d) / 3.0d) {
                            TouchInterceptor.this.stopDragging();
                            TouchInterceptor.this.mRemoveListener.remove(TouchInterceptor.this.mSrcDragPos);
                            TouchInterceptor.this.unExpandViews(true);
                        }
                    }
                    return true;
                }
            });
        }
        if ((this.mDragListener != null || this.mDropListener != null) && motionEvent.getAction() == 0 && (iPointToPosition = pointToPosition((x = (int) motionEvent.getX()), (y = (int) motionEvent.getY()))) != -1) {
            ViewGroup viewGroup = (ViewGroup) getChildAt(iPointToPosition - getFirstVisiblePosition());
            this.mDragPointX = x - viewGroup.getLeft();
            this.mDragPointY = y - viewGroup.getTop();
            int[] iArr = new int[2];
            getLocationInWindow(iArr);
            this.mXOffset = iArr[0];
            this.mYOffset = iArr[1];
            if (x < 64) {
                viewGroup.setDrawingCacheEnabled(false);
                viewGroup.setDrawingCacheEnabled(true);
                Bitmap bitmapCreateBitmap = Bitmap.createBitmap(viewGroup.getDrawingCache());
                this.mSrcDragPosition = y;
                startDragging(bitmapCreateBitmap, x, y);
                this.mDragPos = iPointToPosition;
                this.mSrcDragPos = this.mDragPos;
                this.mHeight = getHeight();
                int i = this.mTouchSlop;
                this.mUpperBound = Math.min(y - i, this.mHeight / 3);
                this.mLowerBound = Math.max(y + i, (this.mHeight * 2) / 3);
                return false;
            }
            stopDragging();
        }
        return super.onInterceptTouchEvent(motionEvent);
    }

    private int myPointToPosition(int i, int i2) {
        int iMyPointToPosition;
        if (i2 < 0 && (iMyPointToPosition = myPointToPosition(i, this.mItemHeightNormal + i2)) > 0) {
            return iMyPointToPosition - 1;
        }
        Rect rect = this.mTempRect;
        for (int childCount = getChildCount() - 1; childCount >= 0; childCount--) {
            getChildAt(childCount).getHitRect(rect);
            if (rect.contains(i, i2)) {
                return getFirstVisiblePosition() + childCount;
            }
        }
        return -1;
    }

    private int getItemForPosition(int i) {
        int i2 = (i - this.mDragPointY) - this.mItemHeightHalf;
        int i3 = 0;
        int iMyPointToPosition = myPointToPosition(0, i2);
        if (iMyPointToPosition >= 0) {
            if (iMyPointToPosition <= this.mSrcDragPos) {
                i3 = iMyPointToPosition + 1;
            } else {
                i3 = iMyPointToPosition;
            }
        } else if (i2 >= 0) {
        }
        if (i > this.mHeight - this.mItemHeightNormal && i3 == getCount() - 2 && i >= this.mSrcDragPosition + (this.mItemHeightNormal / 3)) {
            return i3 + 1;
        }
        return i3;
    }

    private void adjustScrollBounds(int i) {
        if (i >= this.mHeight / 3) {
            this.mUpperBound = this.mHeight / 3;
        }
        if (i <= (this.mHeight * 2) / 3) {
            this.mLowerBound = (this.mHeight * 2) / 3;
        }
    }

    private void unExpandViews(boolean z) {
        try {
            layoutChildren();
        } catch (IllegalStateException e) {
            MusicLogUtils.v("TouchInterceptor", "layoutChildren IllegalStateException", e);
        }
        int i = 0;
        while (true) {
            View childAt = getChildAt(i);
            if (childAt == null) {
                if (z) {
                    int firstVisiblePosition = getFirstVisiblePosition();
                    int top = getChildAt(0).getTop();
                    setAdapter(getAdapter());
                    setSelectionFromTop(firstVisiblePosition, top);
                }
                try {
                    layoutChildren();
                    childAt = getChildAt(i);
                } catch (IllegalStateException e2) {
                }
                if (childAt == null) {
                    MusicLogUtils.v("TouchInterceptor", "unExpandViews with null view, return");
                    return;
                }
            }
            ViewGroup.LayoutParams layoutParams = childAt.getLayoutParams();
            layoutParams.height = this.mItemHeightNormal;
            childAt.setLayoutParams(layoutParams);
            childAt.setVisibility(0);
            i++;
        }
    }

    private void doExpansion() {
        int firstVisiblePosition = this.mDragPos - getFirstVisiblePosition();
        if (this.mDragPos > this.mSrcDragPos) {
            firstVisiblePosition++;
        }
        int headerViewsCount = getHeaderViewsCount();
        View childAt = getChildAt(this.mSrcDragPos - getFirstVisiblePosition());
        int i = 0;
        while (true) {
            View childAt2 = getChildAt(i);
            if (childAt2 != null) {
                int i2 = this.mItemHeightNormal;
                int i3 = 4;
                if (this.mDragPos < headerViewsCount && i == headerViewsCount) {
                    if (!childAt2.equals(childAt)) {
                        i2 = this.mItemHeightExpanded;
                        i3 = 0;
                    }
                } else if (childAt2.equals(childAt)) {
                    if (this.mDragPos != this.mSrcDragPos && getPositionForView(childAt2) != getCount() - 1) {
                        i2 = 1;
                    }
                } else {
                    if (i == firstVisiblePosition && this.mDragPos >= headerViewsCount && this.mDragPos < getCount() - 1) {
                        i2 = this.mItemHeightExpanded;
                    }
                    i3 = 0;
                }
                ViewGroup.LayoutParams layoutParams = childAt2.getLayoutParams();
                layoutParams.height = i2;
                childAt2.setLayoutParams(layoutParams);
                childAt2.setVisibility(i3);
                i++;
            } else {
                return;
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        int i;
        if (this.mGestureDetector != null) {
            this.mGestureDetector.onTouchEvent(motionEvent);
        }
        if ((this.mDragListener != null || this.mDropListener != null) && this.mDragView != null) {
            int action = motionEvent.getAction();
            int i2 = 0;
            switch (action) {
                case 0:
                case 2:
                    int x = (int) motionEvent.getX();
                    int y = (int) motionEvent.getY();
                    dragView(x, y);
                    int itemForPosition = getItemForPosition(y);
                    if (itemForPosition >= 0) {
                        if (action == 0 || itemForPosition != this.mDragPos) {
                            if (this.mDragListener != null) {
                                this.mDragListener.drag(this.mDragPos, itemForPosition);
                            }
                            this.mDragPos = itemForPosition;
                            doExpansion();
                        }
                        adjustScrollBounds(y);
                        if (y > this.mLowerBound) {
                            if (getLastVisiblePosition() < getCount() - 1) {
                                i = y > (this.mHeight + this.mLowerBound) / 2 ? 16 : 4;
                                i2 = i;
                                if (i2 != 0) {
                                    smoothScrollBy(i2, 30);
                                }
                            } else {
                                i2 = 1;
                                if (i2 != 0) {
                                }
                            }
                        } else {
                            if (y < this.mUpperBound) {
                                i = y < this.mUpperBound / 2 ? -16 : -4;
                                if (getFirstVisiblePosition() != 0 || getChildAt(0).getTop() < getPaddingTop()) {
                                    i2 = i;
                                }
                            }
                            if (i2 != 0) {
                            }
                        }
                    }
                    return true;
                case 1:
                case 3:
                    Rect rect = this.mTempRect;
                    this.mDragView.getDrawingRect(rect);
                    stopDragging();
                    this.mIsDraw = false;
                    if (this.mRemoveMode == 1 && motionEvent.getX() > (((double) rect.right) * 3.0d) / 4.0d) {
                        if (this.mRemoveListener != null) {
                            this.mRemoveListener.remove(this.mSrcDragPos);
                        }
                        unExpandViews(true);
                    } else {
                        if (this.mDropListener != null && this.mDragPos >= 0 && this.mDragPos < getCount()) {
                            this.mDropListener.drop(this.mSrcDragPos, this.mDragPos);
                        }
                        if (this.mSrcDragPos == this.mDragPos || (this.mSrcDragPos == getCount() - 1 && this.mDragPos >= this.mSrcDragPos)) {
                            this.mIsDraw = true;
                        }
                        unExpandViews(false);
                    }
                    return true;
                default:
                    return true;
            }
        }
        return super.onTouchEvent(motionEvent);
    }

    private void startDragging(Bitmap bitmap, int i, int i2) {
        MusicLogUtils.v("TouchInterceptor", "startDragging ");
        stopDragging();
        this.mWindowParams = new WindowManager.LayoutParams();
        this.mWindowParams.gravity = 51;
        this.mWindowParams.x = (i - this.mDragPointX) + this.mXOffset;
        this.mWindowParams.y = (i2 - this.mDragPointY) + this.mYOffset;
        this.mWindowParams.height = -2;
        this.mWindowParams.width = -2;
        this.mWindowParams.flags = 920;
        this.mWindowParams.format = -3;
        this.mWindowParams.windowAnimations = 0;
        Context context = getContext();
        ImageView imageView = new ImageView(context);
        imageView.setBackgroundResource(R.drawable.playlist_tile_drag);
        imageView.setPadding(0, 0, 0, 0);
        imageView.setImageBitmap(bitmap);
        this.mDragBitmap = bitmap;
        this.mWindowManager = (WindowManager) context.getSystemService("window");
        this.mWindowManager.addView(imageView, this.mWindowParams);
        this.mDragView = imageView;
    }

    private void dragView(int i, int i2) {
        if (this.mRemoveMode == 1) {
            float f = 1.0f;
            int width = this.mDragView.getWidth() / 2;
            if (i > width) {
                f = (r2 - i) / width;
            }
            this.mWindowParams.alpha = f;
        }
        if (this.mRemoveMode == 0 || this.mRemoveMode == 2) {
            this.mWindowParams.x = (i - this.mDragPointX) + this.mXOffset;
        } else {
            this.mWindowParams.x = 0;
        }
        this.mWindowParams.y = (i2 - this.mDragPointY) + this.mYOffset;
        this.mWindowManager.updateViewLayout(this.mDragView, this.mWindowParams);
        if (this.mTrashcan != null) {
            int width2 = this.mDragView.getWidth();
            if (i2 > (getHeight() * 3) / 4) {
                this.mTrashcan.setLevel(2);
            } else if (width2 > 0 && i > width2 / 4) {
                this.mTrashcan.setLevel(1);
            } else {
                this.mTrashcan.setLevel(0);
            }
        }
    }

    private void stopDragging() {
        MusicLogUtils.v("TouchInterceptor", "stopDragging() ");
        if (this.mDragView != null) {
            this.mDragView.setVisibility(8);
            ((WindowManager) getContext().getSystemService("window")).removeView(this.mDragView);
            this.mDragView.setImageDrawable(null);
            this.mDragView = null;
        }
        if (this.mDragBitmap != null) {
            this.mDragBitmap.recycle();
            this.mDragBitmap = null;
        }
        if (this.mTrashcan != null) {
            this.mTrashcan.setLevel(0);
        }
    }

    public void setDropListener(DropListener dropListener) {
        this.mDropListener = dropListener;
    }

    public void setRemoveListener(RemoveListener removeListener) {
        this.mRemoveListener = removeListener;
    }

    public void setUpgradeAlbumArtListener(UpgradeAlbumArtListener upgradeAlbumArtListener) {
        this.mUpgradeAlbumArtListener = upgradeAlbumArtListener;
    }

    @Override
    protected void onSizeChanged(int i, int i2, int i3, int i4) {
        super.onSizeChanged(i, i2, i3, i4);
        if (this.mListviewWidth == 0 || this.mListViewHeight == 0) {
            this.mListviewWidth = i;
            this.mListViewHeight = i2;
            return;
        }
        if (this.mListviewWidth == i3 && this.mListViewHeight == i4) {
            this.mListviewWidth = i;
            this.mListViewHeight = i2;
            int i5 = getResources().getConfiguration().orientation;
            synchronized (this.mSizeChangedLock) {
                if (this.mUpgradeAlbumArtListener != null) {
                    if (i5 == 2) {
                        if (this.mListviewWidth > this.mListViewHeight) {
                            this.mUpgradeAlbumArtListener.UpgradeAlbumArt();
                            MusicLogUtils.v("TouchInterceptor", "onSizeChanged with right size, call upgrade album art:orientation=" + i5);
                        }
                    } else if (this.mListviewWidth < this.mListViewHeight) {
                        this.mUpgradeAlbumArtListener.UpgradeAlbumArt();
                        MusicLogUtils.v("TouchInterceptor", "onSizeChanged with right size, call upgrade album art:orientation=" + i5);
                    }
                }
                this.mSizeChangedLock.notify();
            }
        }
    }

    public void waitMeasureFinished(boolean z) {
        if (!z ? this.mListviewWidth < this.mListViewHeight : this.mListviewWidth > this.mListViewHeight) {
            MusicLogUtils.v("TouchInterceptor", "waitMeasureFinished: (w" + this.mListviewWidth + ",h" + this.mListViewHeight + ")");
            return;
        }
        synchronized (this.mSizeChangedLock) {
            int i = 0;
            while (true) {
                if (!z) {
                    if (this.mListviewWidth < this.mListViewHeight) {
                        break;
                    }
                } else {
                    try {
                        if (this.mListviewWidth > this.mListViewHeight) {
                            break;
                        }
                        try {
                            this.mSizeChangedLock.wait(200L);
                            i += 200;
                        } catch (InterruptedException e) {
                            MusicLogUtils.v("TouchInterceptor", "wait has been interupted " + e);
                        }
                        if (i > 3000) {
                            break;
                        }
                        MusicLogUtils.v("TouchInterceptor", "Wait for listview onMeasure finished!" + i);
                    } catch (Throwable th) {
                        throw th;
                    }
                }
            }
        }
        MusicLogUtils.v("TouchInterceptor", "waitMeasureFinished: listview has finished measure!");
    }

    void registerContentObserver(Context context) {
        context.getContentResolver().registerContentObserver(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, true, this.mContentObserver);
        MusicLogUtils.v("TouchInterceptor", "registerContentObserver " + this.mContentObserver);
    }

    void unregisterContentObserver(Context context) {
        context.getContentResolver().unregisterContentObserver(this.mContentObserver);
        MusicLogUtils.v("TouchInterceptor", "unregisterContentObserver " + this.mContentObserver);
    }

    public void resetPredrawStatus() {
        this.mIsDraw = true;
    }
}
