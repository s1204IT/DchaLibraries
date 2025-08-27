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
import com.android.browser.R;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/* loaded from: classes.dex */
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

    private void init(Context context) {
        this.mItems = new ArrayList();
        this.mLevels = 0;
        this.mCounts = new int[5];
        Resources resources = context.getResources();
        this.mRadius = (int) resources.getDimension(R.dimen.qc_radius_start);
        this.mRadiusInc = (int) resources.getDimension(R.dimen.qc_radius_increment);
        this.mSlop = (int) resources.getDimension(R.dimen.qc_slop);
        this.mTouchOffset = (int) resources.getDimension(R.dimen.qc_touch_offset);
        this.mOpen = false;
        setWillNotDraw(false);
        setDrawingCacheEnabled(false);
        this.mCenter = new Point(0, 0);
        this.mBackground = resources.getDrawable(R.drawable.qc_background_normal);
        this.mNormalPaint = new Paint();
        this.mNormalPaint.setAntiAlias(true);
        this.mSelectedPaint = new Paint();
        this.mSelectedPaint.setColor(resources.getColor(R.color.qc_selected));
        this.mSelectedPaint.setAntiAlias(true);
        this.mSubPaint = new Paint();
        this.mSubPaint.setAntiAlias(true);
        this.mSubPaint.setColor(resources.getColor(R.color.qc_sub));
    }

    public void setController(PieController pieController) {
        this.mController = pieController;
    }

    public void addItem(PieItem pieItem) {
        this.mItems.add(pieItem);
        int level = pieItem.getLevel();
        this.mLevels = Math.max(this.mLevels, level);
        int[] iArr = this.mCounts;
        iArr[level] = iArr[level] + 1;
    }

    private boolean onTheLeft() {
        return this.mCenter.x < this.mSlop;
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

    /* renamed from: com.android.browser.view.PieMenu$1 */
    class AnonymousClass1 implements ValueAnimator.AnimatorUpdateListener {
        AnonymousClass1() {
        }

        @Override // android.animation.ValueAnimator.AnimatorUpdateListener
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            for (PieItem pieItem : PieMenu.this.mCurrentItems) {
                pieItem.setAnimationAngle((1.0f - valueAnimator.getAnimatedFraction()) * (-pieItem.getStart()));
            }
            PieMenu.this.invalidate();
        }
    }

    private void animateOpen() {
        ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(0.0f, 1.0f);
        valueAnimatorOfFloat.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.browser.view.PieMenu.1
            AnonymousClass1() {
            }

            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                for (PieItem pieItem : PieMenu.this.mCurrentItems) {
                    pieItem.setAnimationAngle((1.0f - valueAnimator.getAnimatedFraction()) * (-pieItem.getStart()));
                }
                PieMenu.this.invalidate();
            }
        });
        valueAnimatorOfFloat.setDuration(160L);
        valueAnimatorOfFloat.start();
    }

    private void setCenter(int i, int i2) {
        if (i < this.mSlop) {
            this.mCenter.x = 0;
        } else {
            this.mCenter.x = getWidth();
        }
        this.mCenter.y = i2;
    }

    private void layoutPie() {
        Iterator<PieItem> it;
        int i;
        int i2;
        PieItem pieItem;
        int i3;
        int i4;
        int i5;
        int i6 = 0;
        int i7 = this.mRadius + 2;
        int i8 = (this.mRadius + this.mRadiusInc) - 2;
        while (i6 < this.mLevels) {
            int i9 = i6 + 1;
            float f = ((float) (3.141592653589793d - 0.3926991f)) / this.mCounts[i9];
            float f2 = f / 2.0f;
            float f3 = 0.19634955f + f2;
            float f4 = 1;
            this.mPath = makeSlice(getDegrees(0.0d) - f4, getDegrees(f) + f4, i8, i7, this.mCenter);
            Iterator<PieItem> it2 = this.mCurrentItems.iterator();
            while (it2.hasNext()) {
                PieItem next = it2.next();
                if (next.getLevel() == i9) {
                    View view = next.getView();
                    if (view != null) {
                        view.measure(view.getLayoutParams().width, view.getLayoutParams().height);
                        int measuredWidth = view.getMeasuredWidth();
                        int measuredHeight = view.getMeasuredHeight();
                        double d = (((i8 - i7) * 2) / 3) + i7;
                        it = it2;
                        pieItem = next;
                        double d2 = f3;
                        i3 = i8;
                        i4 = i7;
                        int iSin = (int) (d * Math.sin(d2));
                        int iCos = (this.mCenter.y - ((int) (d * Math.cos(d2)))) - (measuredHeight / 2);
                        if (onTheLeft()) {
                            i5 = (this.mCenter.x + iSin) - (measuredWidth / 2);
                        } else {
                            i5 = (this.mCenter.x - iSin) - (measuredWidth / 2);
                        }
                        view.layout(i5, iCos, measuredWidth + i5, measuredHeight + iCos);
                    } else {
                        it = it2;
                        pieItem = next;
                        i3 = i8;
                        i4 = i7;
                    }
                    i = i3;
                    i2 = i4;
                    pieItem.setGeometry(f3 - f2, f, i2, i);
                    f3 += f;
                } else {
                    it = it2;
                    i = i8;
                    i2 = i7;
                }
                i8 = i;
                i7 = i2;
                it2 = it;
            }
            i7 += this.mRadiusInc;
            i8 += this.mRadiusInc;
            i6 = i9;
        }
    }

    private float getDegrees(double d) {
        return (float) (270.0d - ((180.0d * d) / 3.141592653589793d));
    }

    @Override // android.view.View
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
            PieItem pieItem = this.mCurrentItem;
            if (this.mOpenItem != null) {
                pieItem = this.mOpenItem;
            }
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

    private Path makeSlice(float f, float f2, int i, int i2, Point point) {
        RectF rectF = new RectF(point.x - i, point.y - i, point.x + i, point.y + i);
        RectF rectF2 = new RectF(point.x - i2, point.y - i2, point.x + i2, point.y + i2);
        Path path = new Path();
        path.arcTo(rectF, f, f2 - f, true);
        path.arcTo(rectF2, f2, f - f2);
        path.close();
        return path;
    }

    @Override // android.view.View
    public boolean onTouchEvent(MotionEvent motionEvent) {
        boolean zOnTouchEvent;
        boolean zOnTouchEvent2;
        float x = motionEvent.getX();
        float y = motionEvent.getY();
        int actionMasked = motionEvent.getActionMasked();
        if (actionMasked == 0) {
            if (x > getWidth() - this.mSlop || x < this.mSlop) {
                setCenter((int) x, (int) y);
                show(true);
                return true;
            }
        } else if (1 == actionMasked) {
            if (this.mOpen) {
                if (this.mPieView != null) {
                    zOnTouchEvent2 = this.mPieView.onTouchEvent(motionEvent);
                } else {
                    zOnTouchEvent2 = false;
                }
                PieItem pieItem = this.mCurrentItem;
                if (!this.mAnimating) {
                    deselect();
                }
                show(false);
                if (!zOnTouchEvent2 && pieItem != null && pieItem.getView() != null && (pieItem == this.mOpenItem || !this.mAnimating)) {
                    pieItem.getView().performClick();
                }
                return true;
            }
        } else {
            if (3 == actionMasked) {
                if (this.mOpen) {
                    show(false);
                }
                if (!this.mAnimating) {
                    deselect();
                    invalidate();
                }
                return false;
            }
            if (2 != actionMasked || this.mAnimating) {
                return false;
            }
            PointF polar = getPolar(x, y);
            int i = this.mRadius + (this.mLevels * this.mRadiusInc) + 50;
            if (this.mPieView != null) {
                zOnTouchEvent = this.mPieView.onTouchEvent(motionEvent);
            } else {
                zOnTouchEvent = false;
            }
            if (zOnTouchEvent) {
                invalidate();
                return false;
            }
            if (polar.y < this.mRadius) {
                if (this.mOpenItem != null) {
                    closeSub();
                } else if (!this.mAnimating) {
                    deselect();
                    invalidate();
                }
                return false;
            }
            if (polar.y > i) {
                deselect();
                show(false);
                motionEvent.setAction(0);
                if (getParent() != null) {
                    ((ViewGroup) getParent()).dispatchTouchEvent(motionEvent);
                }
                return false;
            }
            PieItem pieItemFindItem = findItem(polar);
            if (pieItemFindItem != null && this.mCurrentItem != pieItemFindItem) {
                onEnter(pieItemFindItem);
                if (pieItemFindItem != null && pieItemFindItem.isPieView() && pieItemFindItem.getView() != null) {
                    int left = pieItemFindItem.getView().getLeft() + (onTheLeft() ? pieItemFindItem.getView().getWidth() : 0);
                    int top = pieItemFindItem.getView().getTop();
                    this.mPieView = pieItemFindItem.getPieView();
                    layoutPieView(this.mPieView, left, top, (pieItemFindItem.getStartAngle() + pieItemFindItem.getSweep()) / 2.0f);
                }
                invalidate();
            }
        }
        return false;
    }

    private void layoutPieView(PieView pieView, int i, int i2, float f) {
        pieView.layout(i, i2, onTheLeft(), f, getHeight());
    }

    private void onEnter(PieItem pieItem) {
        if (this.mCurrentItem != null) {
            this.mCurrentItem.setSelected(false);
        }
        if (pieItem != null) {
            playSoundEffect(0);
            pieItem.setSelected(true);
            this.mPieView = null;
            this.mCurrentItem = pieItem;
            if (this.mCurrentItem != this.mOpenItem && this.mCurrentItem.hasItems()) {
                openSub(this.mCurrentItem);
                this.mOpenItem = pieItem;
                return;
            }
            return;
        }
        this.mCurrentItem = null;
    }

    private void animateOut(PieItem pieItem, Animator.AnimatorListener animatorListener) {
        if (this.mCurrentItems == null || pieItem == null) {
            return;
        }
        float startAngle = pieItem.getStartAngle();
        ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(0.0f, 1.0f);
        valueAnimatorOfFloat.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.browser.view.PieMenu.2
            final /* synthetic */ PieItem val$fixed;
            final /* synthetic */ float val$target;

            AnonymousClass2(PieItem pieItem2, float startAngle2) {
                pieItem = pieItem2;
                f = startAngle2;
            }

            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                for (PieItem pieItem2 : PieMenu.this.mCurrentItems) {
                    if (pieItem2 != pieItem) {
                        pieItem2.setAnimationAngle(valueAnimator.getAnimatedFraction() * (f - pieItem2.getStart()));
                    }
                }
                PieMenu.this.invalidate();
            }
        });
        valueAnimatorOfFloat.setDuration(80L);
        valueAnimatorOfFloat.addListener(animatorListener);
        valueAnimatorOfFloat.start();
    }

    /* renamed from: com.android.browser.view.PieMenu$2 */
    class AnonymousClass2 implements ValueAnimator.AnimatorUpdateListener {
        final /* synthetic */ PieItem val$fixed;
        final /* synthetic */ float val$target;

        AnonymousClass2(PieItem pieItem2, float startAngle2) {
            pieItem = pieItem2;
            f = startAngle2;
        }

        @Override // android.animation.ValueAnimator.AnimatorUpdateListener
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            for (PieItem pieItem2 : PieMenu.this.mCurrentItems) {
                if (pieItem2 != pieItem) {
                    pieItem2.setAnimationAngle(valueAnimator.getAnimatedFraction() * (f - pieItem2.getStart()));
                }
            }
            PieMenu.this.invalidate();
        }
    }

    private void animateIn(PieItem pieItem, Animator.AnimatorListener animatorListener) {
        if (this.mCurrentItems == null || pieItem == null) {
            return;
        }
        float startAngle = pieItem.getStartAngle();
        ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(0.0f, 1.0f);
        valueAnimatorOfFloat.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.browser.view.PieMenu.3
            final /* synthetic */ PieItem val$fixed;
            final /* synthetic */ float val$target;

            AnonymousClass3(PieItem pieItem2, float startAngle2) {
                pieItem = pieItem2;
                f = startAngle2;
            }

            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                for (PieItem pieItem2 : PieMenu.this.mCurrentItems) {
                    if (pieItem2 != pieItem) {
                        pieItem2.setAnimationAngle((1.0f - valueAnimator.getAnimatedFraction()) * (f - pieItem2.getStart()));
                    }
                }
                PieMenu.this.invalidate();
            }
        });
        valueAnimatorOfFloat.setDuration(80L);
        valueAnimatorOfFloat.addListener(animatorListener);
        valueAnimatorOfFloat.start();
    }

    /* renamed from: com.android.browser.view.PieMenu$3 */
    class AnonymousClass3 implements ValueAnimator.AnimatorUpdateListener {
        final /* synthetic */ PieItem val$fixed;
        final /* synthetic */ float val$target;

        AnonymousClass3(PieItem pieItem2, float startAngle2) {
            pieItem = pieItem2;
            f = startAngle2;
        }

        @Override // android.animation.ValueAnimator.AnimatorUpdateListener
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            for (PieItem pieItem2 : PieMenu.this.mCurrentItems) {
                if (pieItem2 != pieItem) {
                    pieItem2.setAnimationAngle((1.0f - valueAnimator.getAnimatedFraction()) * (f - pieItem2.getStart()));
                }
            }
            PieMenu.this.invalidate();
        }
    }

    /* renamed from: com.android.browser.view.PieMenu$4 */
    class AnonymousClass4 extends AnimatorListenerAdapter {
        final /* synthetic */ PieItem val$item;

        AnonymousClass4(PieItem pieItem) {
            pieItem = pieItem;
        }

        @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
        public void onAnimationEnd(Animator animator) {
            Iterator it = PieMenu.this.mCurrentItems.iterator();
            while (it.hasNext()) {
                ((PieItem) it.next()).setAnimationAngle(0.0f);
            }
            PieMenu.this.mCurrentItems = new ArrayList(PieMenu.this.mItems.size());
            int i = 0;
            for (int i2 = 0; i2 < PieMenu.this.mItems.size(); i2++) {
                if (PieMenu.this.mItems.get(i2) == pieItem) {
                    PieMenu.this.mCurrentItems.add(pieItem);
                } else {
                    PieMenu.this.mCurrentItems.add(pieItem.getItems().get(i));
                    i++;
                }
            }
            PieMenu.this.layoutPie();
            PieMenu.this.animateIn(pieItem, new AnimatorListenerAdapter() { // from class: com.android.browser.view.PieMenu.4.1
                AnonymousClass1() {
                }

                @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
                public void onAnimationEnd(Animator animator2) {
                    Iterator it2 = PieMenu.this.mCurrentItems.iterator();
                    while (it2.hasNext()) {
                        ((PieItem) it2.next()).setAnimationAngle(0.0f);
                    }
                    PieMenu.this.mAnimating = false;
                }
            });
        }

        /* renamed from: com.android.browser.view.PieMenu$4$1 */
        class AnonymousClass1 extends AnimatorListenerAdapter {
            AnonymousClass1() {
            }

            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator2) {
                Iterator it2 = PieMenu.this.mCurrentItems.iterator();
                while (it2.hasNext()) {
                    ((PieItem) it2.next()).setAnimationAngle(0.0f);
                }
                PieMenu.this.mAnimating = false;
            }
        }
    }

    private void openSub(PieItem pieItem) {
        this.mAnimating = true;
        animateOut(pieItem, new AnimatorListenerAdapter() { // from class: com.android.browser.view.PieMenu.4
            final /* synthetic */ PieItem val$item;

            AnonymousClass4(PieItem pieItem2) {
                pieItem = pieItem2;
            }

            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator) {
                Iterator it = PieMenu.this.mCurrentItems.iterator();
                while (it.hasNext()) {
                    ((PieItem) it.next()).setAnimationAngle(0.0f);
                }
                PieMenu.this.mCurrentItems = new ArrayList(PieMenu.this.mItems.size());
                int i = 0;
                for (int i2 = 0; i2 < PieMenu.this.mItems.size(); i2++) {
                    if (PieMenu.this.mItems.get(i2) == pieItem) {
                        PieMenu.this.mCurrentItems.add(pieItem);
                    } else {
                        PieMenu.this.mCurrentItems.add(pieItem.getItems().get(i));
                        i++;
                    }
                }
                PieMenu.this.layoutPie();
                PieMenu.this.animateIn(pieItem, new AnimatorListenerAdapter() { // from class: com.android.browser.view.PieMenu.4.1
                    AnonymousClass1() {
                    }

                    @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
                    public void onAnimationEnd(Animator animator2) {
                        Iterator it2 = PieMenu.this.mCurrentItems.iterator();
                        while (it2.hasNext()) {
                            ((PieItem) it2.next()).setAnimationAngle(0.0f);
                        }
                        PieMenu.this.mAnimating = false;
                    }
                });
            }

            /* renamed from: com.android.browser.view.PieMenu$4$1 */
            class AnonymousClass1 extends AnimatorListenerAdapter {
                AnonymousClass1() {
                }

                @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
                public void onAnimationEnd(Animator animator2) {
                    Iterator it2 = PieMenu.this.mCurrentItems.iterator();
                    while (it2.hasNext()) {
                        ((PieItem) it2.next()).setAnimationAngle(0.0f);
                    }
                    PieMenu.this.mAnimating = false;
                }
            }
        });
    }

    private void closeSub() {
        this.mAnimating = true;
        if (this.mCurrentItem != null) {
            this.mCurrentItem.setSelected(false);
        }
        animateOut(this.mOpenItem, new AnimatorListenerAdapter() { // from class: com.android.browser.view.PieMenu.5
            AnonymousClass5() {
            }

            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator) {
                Iterator it = PieMenu.this.mCurrentItems.iterator();
                while (it.hasNext()) {
                    ((PieItem) it.next()).setAnimationAngle(0.0f);
                }
                PieMenu.this.mCurrentItems = PieMenu.this.mItems;
                PieMenu.this.mPieView = null;
                PieMenu.this.animateIn(PieMenu.this.mOpenItem, new AnimatorListenerAdapter() { // from class: com.android.browser.view.PieMenu.5.1
                    AnonymousClass1() {
                    }

                    @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
                    public void onAnimationEnd(Animator animator2) {
                        Iterator it2 = PieMenu.this.mCurrentItems.iterator();
                        while (it2.hasNext()) {
                            ((PieItem) it2.next()).setAnimationAngle(0.0f);
                        }
                        PieMenu.this.mAnimating = false;
                        PieMenu.this.mOpenItem = null;
                        PieMenu.this.mCurrentItem = null;
                    }
                });
            }

            /* renamed from: com.android.browser.view.PieMenu$5$1 */
            class AnonymousClass1 extends AnimatorListenerAdapter {
                AnonymousClass1() {
                }

                @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
                public void onAnimationEnd(Animator animator2) {
                    Iterator it2 = PieMenu.this.mCurrentItems.iterator();
                    while (it2.hasNext()) {
                        ((PieItem) it2.next()).setAnimationAngle(0.0f);
                    }
                    PieMenu.this.mAnimating = false;
                    PieMenu.this.mOpenItem = null;
                    PieMenu.this.mCurrentItem = null;
                }
            }
        });
    }

    /* renamed from: com.android.browser.view.PieMenu$5 */
    class AnonymousClass5 extends AnimatorListenerAdapter {
        AnonymousClass5() {
        }

        @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
        public void onAnimationEnd(Animator animator) {
            Iterator it = PieMenu.this.mCurrentItems.iterator();
            while (it.hasNext()) {
                ((PieItem) it.next()).setAnimationAngle(0.0f);
            }
            PieMenu.this.mCurrentItems = PieMenu.this.mItems;
            PieMenu.this.mPieView = null;
            PieMenu.this.animateIn(PieMenu.this.mOpenItem, new AnimatorListenerAdapter() { // from class: com.android.browser.view.PieMenu.5.1
                AnonymousClass1() {
                }

                @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
                public void onAnimationEnd(Animator animator2) {
                    Iterator it2 = PieMenu.this.mCurrentItems.iterator();
                    while (it2.hasNext()) {
                        ((PieItem) it2.next()).setAnimationAngle(0.0f);
                    }
                    PieMenu.this.mAnimating = false;
                    PieMenu.this.mOpenItem = null;
                    PieMenu.this.mCurrentItem = null;
                }
            });
        }

        /* renamed from: com.android.browser.view.PieMenu$5$1 */
        class AnonymousClass1 extends AnimatorListenerAdapter {
            AnonymousClass1() {
            }

            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator2) {
                Iterator it2 = PieMenu.this.mCurrentItems.iterator();
                while (it2.hasNext()) {
                    ((PieItem) it2.next()).setAnimationAngle(0.0f);
                }
                PieMenu.this.mAnimating = false;
                PieMenu.this.mOpenItem = null;
                PieMenu.this.mCurrentItem = null;
            }
        }
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

    private PieItem findItem(PointF pointF) {
        for (PieItem pieItem : this.mCurrentItems) {
            if (inside(pointF, this.mTouchOffset, pieItem)) {
                return pieItem;
            }
        }
        return null;
    }

    private boolean inside(PointF pointF, float f, PieItem pieItem) {
        return ((float) pieItem.getInnerRadius()) - f < pointF.y && ((float) pieItem.getOuterRadius()) - f > pointF.y && pieItem.getStartAngle() < pointF.x && pieItem.getStartAngle() + pieItem.getSweep() > pointF.x;
    }
}
