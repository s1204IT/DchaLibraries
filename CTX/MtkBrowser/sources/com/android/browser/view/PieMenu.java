package com.android.browser.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PieMenu extends FrameLayout {
    private boolean mAnimating;
    private Drawable mBackground;
    private Point mCenter;
    private PieController mController;
    private int[] mCounts;
    private PieItem mCurrentItem;
    private List<PieItem> mCurrentItems;
    private List<PieItem> mItems;
    private int mLevels;
    private Paint mNormalPaint;
    private boolean mOpen;
    private PieItem mOpenItem;
    private Path mPath;
    private PieView mPieView;
    private int mRadius;
    private int mRadiusInc;
    private Paint mSelectedPaint;
    private int mSlop;
    private Paint mSubPaint;
    private int mTouchOffset;
    private boolean mUseBackground;

    class AnonymousClass4 extends AnimatorListenerAdapter {
        final PieMenu this$0;
        final PieItem val$item;

        AnonymousClass4(PieMenu pieMenu, PieItem pieItem) {
            this.this$0 = pieMenu;
            this.val$item = pieItem;
        }

        @Override
        public void onAnimationEnd(Animator animator) {
            int i;
            int i2 = 0;
            Iterator it = this.this$0.mCurrentItems.iterator();
            while (it.hasNext()) {
                ((PieItem) it.next()).setAnimationAngle(0.0f);
            }
            this.this$0.mCurrentItems = new ArrayList(this.this$0.mItems.size());
            int i3 = 0;
            while (true) {
                int i4 = i2;
                if (i4 >= this.this$0.mItems.size()) {
                    this.this$0.layoutPie();
                    this.this$0.animateIn(this.val$item, new AnimatorListenerAdapter(this) {
                        final AnonymousClass4 this$1;

                        {
                            this.this$1 = this;
                        }

                        @Override
                        public void onAnimationEnd(Animator animator2) {
                            Iterator it2 = this.this$1.this$0.mCurrentItems.iterator();
                            while (it2.hasNext()) {
                                ((PieItem) it2.next()).setAnimationAngle(0.0f);
                            }
                            this.this$1.this$0.mAnimating = false;
                        }
                    });
                    return;
                }
                if (this.this$0.mItems.get(i4) == this.val$item) {
                    this.this$0.mCurrentItems.add(this.val$item);
                    i = i3;
                } else {
                    this.this$0.mCurrentItems.add(this.val$item.getItems().get(i3));
                    i = i3 + 1;
                }
                i2 = i4 + 1;
                i3 = i;
            }
        }
    }

    class AnonymousClass5 extends AnimatorListenerAdapter {
        final PieMenu this$0;

        AnonymousClass5(PieMenu pieMenu) {
            this.this$0 = pieMenu;
        }

        @Override
        public void onAnimationEnd(Animator animator) {
            Iterator it = this.this$0.mCurrentItems.iterator();
            while (it.hasNext()) {
                ((PieItem) it.next()).setAnimationAngle(0.0f);
            }
            this.this$0.mCurrentItems = this.this$0.mItems;
            this.this$0.mPieView = null;
            this.this$0.animateIn(this.this$0.mOpenItem, new AnimatorListenerAdapter(this) {
                final AnonymousClass5 this$1;

                {
                    this.this$1 = this;
                }

                @Override
                public void onAnimationEnd(Animator animator2) {
                    Iterator it2 = this.this$1.this$0.mCurrentItems.iterator();
                    while (it2.hasNext()) {
                        ((PieItem) it2.next()).setAnimationAngle(0.0f);
                    }
                    this.this$1.this$0.mAnimating = false;
                    this.this$1.this$0.mOpenItem = null;
                    this.this$1.this$0.mCurrentItem = null;
                }
            });
        }
    }

    public interface PieController {
        boolean onOpen();

        void stopEditingUrl();
    }

    public interface PieView {

        public interface OnLayoutListener {
            void onLayout(int i, int i2, boolean z);
        }

        void draw(Canvas canvas);

        void layout(int i, int i2, boolean z, float f, int i3);

        boolean onTouchEvent(MotionEvent motionEvent);
    }

    public PieMenu(Context context) {
        super(context);
        this.mPieView = null;
        init(context);
    }

    public void animateIn(PieItem pieItem, Animator.AnimatorListener animatorListener) {
        if (this.mCurrentItems == null || pieItem == null) {
            return;
        }
        float startAngle = pieItem.getStartAngle();
        ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(0.0f, 1.0f);
        valueAnimatorOfFloat.addUpdateListener(new ValueAnimator.AnimatorUpdateListener(this, pieItem, startAngle) {
            final PieMenu this$0;
            final PieItem val$fixed;
            final float val$target;

            {
                this.this$0 = this;
                this.val$fixed = pieItem;
                this.val$target = startAngle;
            }

            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                for (PieItem pieItem2 : this.this$0.mCurrentItems) {
                    if (pieItem2 != this.val$fixed) {
                        pieItem2.setAnimationAngle((1.0f - valueAnimator.getAnimatedFraction()) * (this.val$target - pieItem2.getStart()));
                    }
                }
                this.this$0.invalidate();
            }
        });
        valueAnimatorOfFloat.setDuration(80L);
        valueAnimatorOfFloat.addListener(animatorListener);
        valueAnimatorOfFloat.start();
    }

    private void animateOpen() {
        ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(0.0f, 1.0f);
        valueAnimatorOfFloat.addUpdateListener(new ValueAnimator.AnimatorUpdateListener(this) {
            final PieMenu this$0;

            {
                this.this$0 = this;
            }

            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                for (PieItem pieItem : this.this$0.mCurrentItems) {
                    pieItem.setAnimationAngle((1.0f - valueAnimator.getAnimatedFraction()) * (-pieItem.getStart()));
                }
                this.this$0.invalidate();
            }
        });
        valueAnimatorOfFloat.setDuration(160L);
        valueAnimatorOfFloat.start();
    }

    private void animateOut(PieItem pieItem, Animator.AnimatorListener animatorListener) {
        if (this.mCurrentItems == null || pieItem == null) {
            return;
        }
        float startAngle = pieItem.getStartAngle();
        ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(0.0f, 1.0f);
        valueAnimatorOfFloat.addUpdateListener(new ValueAnimator.AnimatorUpdateListener(this, pieItem, startAngle) {
            final PieMenu this$0;
            final PieItem val$fixed;
            final float val$target;

            {
                this.this$0 = this;
                this.val$fixed = pieItem;
                this.val$target = startAngle;
            }

            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                for (PieItem pieItem2 : this.this$0.mCurrentItems) {
                    if (pieItem2 != this.val$fixed) {
                        pieItem2.setAnimationAngle(valueAnimator.getAnimatedFraction() * (this.val$target - pieItem2.getStart()));
                    }
                }
                this.this$0.invalidate();
            }
        });
        valueAnimatorOfFloat.setDuration(80L);
        valueAnimatorOfFloat.addListener(animatorListener);
        valueAnimatorOfFloat.start();
    }

    private void closeSub() {
        this.mAnimating = true;
        if (this.mCurrentItem != null) {
            this.mCurrentItem.setSelected(false);
        }
        animateOut(this.mOpenItem, new AnonymousClass5(this));
    }

    private void deselect() {
        if (this.mCurrentItem != null) {
            this.mCurrentItem.setSelected(false);
        }
        if (this.mOpenItem != null) {
            this.mOpenItem = null;
            this.mCurrentItems = this.mItems;
        }
        this.mCurrentItem = null;
        this.mPieView = null;
    }

    private void drawItem(Canvas canvas, PieItem pieItem) {
        if (pieItem.getView() != null) {
            Paint paint = pieItem.isSelected() ? this.mSelectedPaint : this.mNormalPaint;
            if (!this.mItems.contains(pieItem)) {
                paint = pieItem.isSelected() ? this.mSelectedPaint : this.mSubPaint;
            }
            int iSave = canvas.save();
            if (onTheLeft()) {
                canvas.scale(-1.0f, 1.0f);
            }
            canvas.rotate(getDegrees(pieItem.getStartAngle()) - 270.0f, this.mCenter.x, this.mCenter.y);
            canvas.drawPath(this.mPath, paint);
            canvas.restoreToCount(iSave);
            View view = pieItem.getView();
            int iSave2 = canvas.save();
            canvas.translate(view.getX(), view.getY());
            view.draw(canvas);
            canvas.restoreToCount(iSave2);
        }
    }

    private PieItem findItem(PointF pointF) {
        for (PieItem pieItem : this.mCurrentItems) {
            if (inside(pointF, this.mTouchOffset, pieItem)) {
                return pieItem;
            }
        }
        return null;
    }

    private float getDegrees(double d) {
        return (float) (270.0d - ((180.0d * d) / 3.141592653589793d));
    }

    private PointF getPolar(float f, float f2) {
        PointF pointF = new PointF();
        pointF.x = 1.5707964f;
        float f3 = this.mCenter.x - f;
        if (this.mCenter.x < this.mSlop) {
            f3 = -f3;
        }
        float f4 = this.mCenter.y - f2;
        pointF.y = (float) Math.sqrt((f3 * f3) + (f4 * f4));
        if (f4 > 0.0f) {
            pointF.x = (float) Math.asin(f3 / pointF.y);
        } else if (f4 < 0.0f) {
            pointF.x = (float) (3.141592653589793d - Math.asin(f3 / pointF.y));
        }
        return pointF;
    }

    private void init(Context context) {
        this.mItems = new ArrayList();
        this.mLevels = 0;
        this.mCounts = new int[5];
        Resources resources = context.getResources();
        this.mRadius = (int) resources.getDimension(2131427343);
        this.mRadiusInc = (int) resources.getDimension(2131427344);
        this.mSlop = (int) resources.getDimension(2131427345);
        this.mTouchOffset = (int) resources.getDimension(2131427346);
        this.mOpen = false;
        setWillNotDraw(false);
        setDrawingCacheEnabled(false);
        this.mCenter = new Point(0, 0);
        this.mBackground = resources.getDrawable(2130837604);
        this.mNormalPaint = new Paint();
        this.mNormalPaint.setAntiAlias(true);
        this.mSelectedPaint = new Paint();
        this.mSelectedPaint.setColor(resources.getColor(2131361804));
        this.mSelectedPaint.setAntiAlias(true);
        this.mSubPaint = new Paint();
        this.mSubPaint.setAntiAlias(true);
        this.mSubPaint.setColor(resources.getColor(2131361805));
    }

    private boolean inside(PointF pointF, float f, PieItem pieItem) {
        return ((float) pieItem.getInnerRadius()) - f < pointF.y && ((float) pieItem.getOuterRadius()) - f > pointF.y && pieItem.getStartAngle() < pointF.x && pieItem.getStartAngle() + pieItem.getSweep() > pointF.x;
    }

    public void layoutPie() {
        float f;
        int i = 0;
        int i2 = this.mRadius + 2;
        int i3 = (this.mRadius + this.mRadiusInc) - 2;
        while (i < this.mLevels) {
            int i4 = i + 1;
            float f2 = ((float) (3.141592653589793d - ((double) 0.3926991f))) / this.mCounts[i4];
            float f3 = f2 / 2.0f;
            float f4 = 1;
            this.mPath = makeSlice(getDegrees(0.0d) - f4, f4 + getDegrees(f2), i3, i2, this.mCenter);
            float f5 = 0.19634955f + f3;
            for (PieItem pieItem : this.mCurrentItems) {
                if (pieItem.getLevel() == i4) {
                    View view = pieItem.getView();
                    if (view != null) {
                        view.measure(view.getLayoutParams().width, view.getLayoutParams().height);
                        int measuredWidth = view.getMeasuredWidth();
                        int measuredHeight = view.getMeasuredHeight();
                        double d = (((i3 - i2) * 2) / 3) + i2;
                        double d2 = f5;
                        int iSin = (int) (Math.sin(d2) * d);
                        int iCos = (this.mCenter.y - ((int) (d * Math.cos(d2)))) - (measuredHeight / 2);
                        int i5 = onTheLeft() ? (iSin + this.mCenter.x) - (measuredWidth / 2) : (this.mCenter.x - iSin) - (measuredWidth / 2);
                        view.layout(i5, iCos, measuredWidth + i5, measuredHeight + iCos);
                    }
                    pieItem.setGeometry(f5 - f3, f2, i2, i3);
                    f = f5 + f2;
                } else {
                    f = f5;
                }
                f5 = f;
            }
            i2 += this.mRadiusInc;
            i3 += this.mRadiusInc;
            i = i4;
        }
    }

    private void layoutPieView(PieView pieView, int i, int i2, float f) {
        pieView.layout(i, i2, onTheLeft(), f, getHeight());
    }

    private Path makeSlice(float f, float f2, int i, int i2, Point point) {
        RectF rectF = new RectF(point.x - i, point.y - i, point.x + i, point.y + i);
        RectF rectF2 = new RectF(point.x - i2, point.y - i2, point.x + i2, point.y + i2);
        Path path = new Path();
        path.arcTo(rectF, f, f2 - f, true);
        path.arcTo(rectF2, f2, f - f2);
        path.close();
        return path;
    }

    private void onEnter(PieItem pieItem) {
        if (this.mCurrentItem != null) {
            this.mCurrentItem.setSelected(false);
        }
        if (pieItem == null) {
            this.mCurrentItem = null;
            return;
        }
        playSoundEffect(0);
        pieItem.setSelected(true);
        this.mPieView = null;
        this.mCurrentItem = pieItem;
        if (this.mCurrentItem == this.mOpenItem || !this.mCurrentItem.hasItems()) {
            return;
        }
        openSub(this.mCurrentItem);
        this.mOpenItem = pieItem;
    }

    private boolean onTheLeft() {
        return this.mCenter.x < this.mSlop;
    }

    private void openSub(PieItem pieItem) {
        this.mAnimating = true;
        animateOut(pieItem, new AnonymousClass4(this, pieItem));
    }

    private void setCenter(int i, int i2) {
        if (i < this.mSlop) {
            this.mCenter.x = 0;
        } else {
            this.mCenter.x = getWidth();
        }
        this.mCenter.y = i2;
    }

    private void show(boolean z) {
        this.mOpen = z;
        if (this.mOpen) {
            this.mAnimating = false;
            this.mCurrentItem = null;
            this.mOpenItem = null;
            this.mPieView = null;
            this.mController.stopEditingUrl();
            this.mCurrentItems = this.mItems;
            Iterator<PieItem> it = this.mCurrentItems.iterator();
            while (it.hasNext()) {
                it.next().setSelected(false);
            }
            if (this.mController != null) {
                this.mController.onOpen();
            }
            layoutPie();
            animateOpen();
        }
        invalidate();
    }

    public void addItem(PieItem pieItem) {
        this.mItems.add(pieItem);
        int level = pieItem.getLevel();
        this.mLevels = Math.max(this.mLevels, level);
        int[] iArr = this.mCounts;
        iArr[level] = iArr[level] + 1;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (this.mOpen) {
            if (this.mUseBackground) {
                int intrinsicWidth = this.mBackground.getIntrinsicWidth();
                int intrinsicHeight = this.mBackground.getIntrinsicHeight();
                int i = this.mCenter.x - intrinsicWidth;
                int i2 = this.mCenter.y - (intrinsicHeight / 2);
                this.mBackground.setBounds(i, i2, intrinsicWidth + i, intrinsicHeight + i2);
                int iSave = canvas.save();
                if (onTheLeft()) {
                    canvas.scale(-1.0f, 1.0f);
                }
                this.mBackground.draw(canvas);
                canvas.restoreToCount(iSave);
            }
            PieItem pieItem = this.mOpenItem != null ? this.mOpenItem : this.mCurrentItem;
            for (PieItem pieItem2 : this.mCurrentItems) {
                if (pieItem2 != pieItem) {
                    drawItem(canvas, pieItem2);
                }
            }
            if (pieItem != null) {
                drawItem(canvas, pieItem);
            }
            if (this.mPieView != null) {
                this.mPieView.draw(canvas);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        float x = motionEvent.getX();
        float y = motionEvent.getY();
        int actionMasked = motionEvent.getActionMasked();
        if (actionMasked == 0) {
            if (x <= getWidth() - this.mSlop && x >= this.mSlop) {
                return false;
            }
            setCenter((int) x, (int) y);
            show(true);
            return true;
        }
        if (1 == actionMasked) {
            if (!this.mOpen) {
                return false;
            }
            boolean zOnTouchEvent = this.mPieView != null ? this.mPieView.onTouchEvent(motionEvent) : false;
            PieItem pieItem = this.mCurrentItem;
            if (!this.mAnimating) {
                deselect();
            }
            show(false);
            if (!zOnTouchEvent && pieItem != null && pieItem.getView() != null && (pieItem == this.mOpenItem || !this.mAnimating)) {
                pieItem.getView().performClick();
            }
            return true;
        }
        if (3 == actionMasked) {
            if (this.mOpen) {
                show(false);
            }
            if (this.mAnimating) {
                return false;
            }
            deselect();
            invalidate();
            return false;
        }
        if (2 != actionMasked || this.mAnimating) {
            return false;
        }
        PointF polar = getPolar(x, y);
        int i = this.mRadius;
        int i2 = this.mLevels;
        int i3 = this.mRadiusInc;
        if (this.mPieView != null ? this.mPieView.onTouchEvent(motionEvent) : false) {
            invalidate();
            return false;
        }
        if (polar.y < this.mRadius) {
            if (this.mOpenItem != null) {
                closeSub();
                return false;
            }
            if (this.mAnimating) {
                return false;
            }
            deselect();
            invalidate();
            return false;
        }
        if (polar.y > i + (i2 * i3) + 50) {
            deselect();
            show(false);
            motionEvent.setAction(0);
            if (getParent() == null) {
                return false;
            }
            ((ViewGroup) getParent()).dispatchTouchEvent(motionEvent);
            return false;
        }
        PieItem pieItemFindItem = findItem(polar);
        if (pieItemFindItem == null || this.mCurrentItem == pieItemFindItem) {
            return false;
        }
        onEnter(pieItemFindItem);
        if (pieItemFindItem != null && pieItemFindItem.isPieView() && pieItemFindItem.getView() != null) {
            int left = pieItemFindItem.getView().getLeft();
            int width = onTheLeft() ? pieItemFindItem.getView().getWidth() : 0;
            int top = pieItemFindItem.getView().getTop();
            this.mPieView = pieItemFindItem.getPieView();
            layoutPieView(this.mPieView, width + left, top, (pieItemFindItem.getSweep() + pieItemFindItem.getStartAngle()) / 2.0f);
        }
        invalidate();
        return false;
    }

    public void setController(PieController pieController) {
        this.mController = pieController;
    }
}
