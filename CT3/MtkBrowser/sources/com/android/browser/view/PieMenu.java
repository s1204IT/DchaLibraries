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

    private void init(Context ctx) {
        this.mItems = new ArrayList();
        this.mLevels = 0;
        this.mCounts = new int[5];
        Resources res = ctx.getResources();
        this.mRadius = (int) res.getDimension(R.dimen.qc_radius_start);
        this.mRadiusInc = (int) res.getDimension(R.dimen.qc_radius_increment);
        this.mSlop = (int) res.getDimension(R.dimen.qc_slop);
        this.mTouchOffset = (int) res.getDimension(R.dimen.qc_touch_offset);
        this.mOpen = false;
        setWillNotDraw(false);
        setDrawingCacheEnabled(false);
        this.mCenter = new Point(0, 0);
        this.mBackground = res.getDrawable(R.drawable.qc_background_normal);
        this.mNormalPaint = new Paint();
        this.mNormalPaint.setAntiAlias(true);
        this.mSelectedPaint = new Paint();
        this.mSelectedPaint.setColor(res.getColor(R.color.qc_selected));
        this.mSelectedPaint.setAntiAlias(true);
        this.mSubPaint = new Paint();
        this.mSubPaint.setAntiAlias(true);
        this.mSubPaint.setColor(res.getColor(R.color.qc_sub));
    }

    public void setController(PieController ctl) {
        this.mController = ctl;
    }

    public void addItem(PieItem item) {
        this.mItems.add(item);
        int l = item.getLevel();
        this.mLevels = Math.max(this.mLevels, l);
        int[] iArr = this.mCounts;
        iArr[l] = iArr[l] + 1;
    }

    private boolean onTheLeft() {
        return this.mCenter.x < this.mSlop;
    }

    private void show(boolean show) {
        this.mOpen = show;
        if (this.mOpen) {
            this.mAnimating = false;
            this.mCurrentItem = null;
            this.mOpenItem = null;
            this.mPieView = null;
            this.mController.stopEditingUrl();
            this.mCurrentItems = this.mItems;
            for (PieItem item : this.mCurrentItems) {
                item.setSelected(false);
            }
            if (this.mController != null) {
                this.mController.onOpen();
            }
            layoutPie();
            animateOpen();
        }
        invalidate();
    }

    private void animateOpen() {
        ValueAnimator anim = ValueAnimator.ofFloat(0.0f, 1.0f);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                for (PieItem item : PieMenu.this.mCurrentItems) {
                    item.setAnimationAngle((1.0f - animation.getAnimatedFraction()) * (-item.getStart()));
                }
                PieMenu.this.invalidate();
            }
        });
        anim.setDuration(160L);
        anim.start();
    }

    private void setCenter(int x, int y) {
        if (x < this.mSlop) {
            this.mCenter.x = 0;
        } else {
            this.mCenter.x = getWidth();
        }
        this.mCenter.y = y;
    }

    public void layoutPie() {
        int x;
        int inner = this.mRadius + 2;
        int outer = (this.mRadius + this.mRadiusInc) - 2;
        for (int i = 0; i < this.mLevels; i++) {
            int level = i + 1;
            float sweep = ((float) (3.141592653589793d - ((double) 0.3926991f))) / this.mCounts[level];
            float angle = 0.19634955f + (sweep / 2.0f);
            this.mPath = makeSlice(getDegrees(0.0d) - 1.0f, 1.0f + getDegrees(sweep), outer, inner, this.mCenter);
            for (PieItem item : this.mCurrentItems) {
                if (item.getLevel() == level) {
                    View view = item.getView();
                    if (view != null) {
                        view.measure(view.getLayoutParams().width, view.getLayoutParams().height);
                        int w = view.getMeasuredWidth();
                        int h = view.getMeasuredHeight();
                        int r = inner + (((outer - inner) * 2) / 3);
                        int x2 = (int) (((double) r) * Math.sin(angle));
                        int y = (this.mCenter.y - ((int) (((double) r) * Math.cos(angle)))) - (h / 2);
                        if (onTheLeft()) {
                            x = (this.mCenter.x + x2) - (w / 2);
                        } else {
                            x = (this.mCenter.x - x2) - (w / 2);
                        }
                        view.layout(x, y, x + w, y + h);
                    }
                    float itemstart = angle - (sweep / 2.0f);
                    item.setGeometry(itemstart, sweep, inner, outer);
                    angle += sweep;
                }
            }
            inner += this.mRadiusInc;
            outer += this.mRadiusInc;
        }
    }

    private float getDegrees(double angle) {
        return (float) (270.0d - ((180.0d * angle) / 3.141592653589793d));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!this.mOpen) {
            return;
        }
        if (this.mUseBackground) {
            int w = this.mBackground.getIntrinsicWidth();
            int h = this.mBackground.getIntrinsicHeight();
            int left = this.mCenter.x - w;
            int top = this.mCenter.y - (h / 2);
            this.mBackground.setBounds(left, top, left + w, top + h);
            int state = canvas.save();
            if (onTheLeft()) {
                canvas.scale(-1.0f, 1.0f);
            }
            this.mBackground.draw(canvas);
            canvas.restoreToCount(state);
        }
        PieItem last = this.mCurrentItem;
        if (this.mOpenItem != null) {
            last = this.mOpenItem;
        }
        for (PieItem item : this.mCurrentItems) {
            if (item != last) {
                drawItem(canvas, item);
            }
        }
        if (last != null) {
            drawItem(canvas, last);
        }
        if (this.mPieView == null) {
            return;
        }
        this.mPieView.draw(canvas);
    }

    private void drawItem(Canvas canvas, PieItem item) {
        if (item.getView() == null) {
            return;
        }
        Paint p = item.isSelected() ? this.mSelectedPaint : this.mNormalPaint;
        if (!this.mItems.contains(item)) {
            p = item.isSelected() ? this.mSelectedPaint : this.mSubPaint;
        }
        int state = canvas.save();
        if (onTheLeft()) {
            canvas.scale(-1.0f, 1.0f);
        }
        float r = getDegrees(item.getStartAngle()) - 270.0f;
        canvas.rotate(r, this.mCenter.x, this.mCenter.y);
        canvas.drawPath(this.mPath, p);
        canvas.restoreToCount(state);
        View view = item.getView();
        int state2 = canvas.save();
        canvas.translate(view.getX(), view.getY());
        view.draw(canvas);
        canvas.restoreToCount(state2);
    }

    private Path makeSlice(float start, float end, int outer, int inner, Point center) {
        RectF bb = new RectF(center.x - outer, center.y - outer, center.x + outer, center.y + outer);
        RectF bbi = new RectF(center.x - inner, center.y - inner, center.x + inner, center.y + inner);
        Path path = new Path();
        path.arcTo(bb, start, end - start, true);
        path.arcTo(bbi, end, start - end);
        path.close();
        return path;
    }

    @Override
    public boolean onTouchEvent(MotionEvent evt) {
        float x = evt.getX();
        float y = evt.getY();
        int action = evt.getActionMasked();
        if (action == 0) {
            if (x > getWidth() - this.mSlop || x < this.mSlop) {
                setCenter((int) x, (int) y);
                show(true);
                return true;
            }
        } else if (1 == action) {
            if (this.mOpen) {
                boolean handled = false;
                if (this.mPieView != null) {
                    handled = this.mPieView.onTouchEvent(evt);
                }
                PieItem item = this.mCurrentItem;
                if (!this.mAnimating) {
                    deselect();
                }
                show(false);
                if (!handled && item != null && item.getView() != null && (item == this.mOpenItem || !this.mAnimating)) {
                    item.getView().performClick();
                }
                return true;
            }
        } else {
            if (3 == action) {
                if (this.mOpen) {
                    show(false);
                }
                if (!this.mAnimating) {
                    deselect();
                    invalidate();
                }
                return false;
            }
            if (2 != action || this.mAnimating) {
                return false;
            }
            boolean handled2 = false;
            PointF polar = getPolar(x, y);
            int maxr = this.mRadius + (this.mLevels * this.mRadiusInc) + 50;
            if (this.mPieView != null) {
                handled2 = this.mPieView.onTouchEvent(evt);
            }
            if (handled2) {
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
            if (polar.y > maxr) {
                deselect();
                show(false);
                evt.setAction(0);
                if (getParent() != null) {
                    ((ViewGroup) getParent()).dispatchTouchEvent(evt);
                }
                return false;
            }
            PieItem item2 = findItem(polar);
            if (item2 != null && this.mCurrentItem != item2) {
                onEnter(item2);
                if (item2 != null && item2.isPieView() && item2.getView() != null) {
                    int cx = item2.getView().getLeft() + (onTheLeft() ? item2.getView().getWidth() : 0);
                    int cy = item2.getView().getTop();
                    this.mPieView = item2.getPieView();
                    layoutPieView(this.mPieView, cx, cy, (item2.getStartAngle() + item2.getSweep()) / 2.0f);
                }
                invalidate();
            }
        }
        return false;
    }

    private void layoutPieView(PieView pv, int x, int y, float angle) {
        pv.layout(x, y, onTheLeft(), angle, getHeight());
    }

    private void onEnter(PieItem item) {
        if (this.mCurrentItem != null) {
            this.mCurrentItem.setSelected(false);
        }
        if (item != null) {
            playSoundEffect(0);
            item.setSelected(true);
            this.mPieView = null;
            this.mCurrentItem = item;
            if (this.mCurrentItem == this.mOpenItem || !this.mCurrentItem.hasItems()) {
                return;
            }
            openSub(this.mCurrentItem);
            this.mOpenItem = item;
            return;
        }
        this.mCurrentItem = null;
    }

    private void animateOut(final PieItem fixed, Animator.AnimatorListener listener) {
        if (this.mCurrentItems == null || fixed == null) {
            return;
        }
        final float target = fixed.getStartAngle();
        ValueAnimator anim = ValueAnimator.ofFloat(0.0f, 1.0f);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                for (PieItem item : PieMenu.this.mCurrentItems) {
                    if (item != fixed) {
                        item.setAnimationAngle(animation.getAnimatedFraction() * (target - item.getStart()));
                    }
                }
                PieMenu.this.invalidate();
            }
        });
        anim.setDuration(80L);
        anim.addListener(listener);
        anim.start();
    }

    public void animateIn(final PieItem fixed, Animator.AnimatorListener listener) {
        if (this.mCurrentItems == null || fixed == null) {
            return;
        }
        final float target = fixed.getStartAngle();
        ValueAnimator anim = ValueAnimator.ofFloat(0.0f, 1.0f);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                for (PieItem item : PieMenu.this.mCurrentItems) {
                    if (item != fixed) {
                        item.setAnimationAngle((1.0f - animation.getAnimatedFraction()) * (target - item.getStart()));
                    }
                }
                PieMenu.this.invalidate();
            }
        });
        anim.setDuration(80L);
        anim.addListener(listener);
        anim.start();
    }

    private void openSub(final PieItem item) {
        this.mAnimating = true;
        animateOut(item, new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator a) {
                for (PieItem item2 : PieMenu.this.mCurrentItems) {
                    item2.setAnimationAngle(0.0f);
                }
                PieMenu.this.mCurrentItems = new ArrayList(PieMenu.this.mItems.size());
                int j = 0;
                for (int i = 0; i < PieMenu.this.mItems.size(); i++) {
                    if (PieMenu.this.mItems.get(i) == item) {
                        PieMenu.this.mCurrentItems.add(item);
                    } else {
                        PieMenu.this.mCurrentItems.add(item.getItems().get(j));
                        j++;
                    }
                }
                PieMenu.this.layoutPie();
                PieMenu.this.animateIn(item, new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator a2) {
                        for (PieItem item3 : PieMenu.this.mCurrentItems) {
                            item3.setAnimationAngle(0.0f);
                        }
                        PieMenu.this.mAnimating = false;
                    }
                });
            }
        });
    }

    private void closeSub() {
        this.mAnimating = true;
        if (this.mCurrentItem != null) {
            this.mCurrentItem.setSelected(false);
        }
        animateOut(this.mOpenItem, new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator a) {
                for (PieItem item : PieMenu.this.mCurrentItems) {
                    item.setAnimationAngle(0.0f);
                }
                PieMenu.this.mCurrentItems = PieMenu.this.mItems;
                PieMenu.this.mPieView = null;
                PieMenu.this.animateIn(PieMenu.this.mOpenItem, new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator a2) {
                        for (PieItem item2 : PieMenu.this.mCurrentItems) {
                            item2.setAnimationAngle(0.0f);
                        }
                        PieMenu.this.mAnimating = false;
                        PieMenu.this.mOpenItem = null;
                        PieMenu.this.mCurrentItem = null;
                    }
                });
            }
        });
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

    private PointF getPolar(float x, float y) {
        PointF res = new PointF();
        res.x = 1.5707964f;
        float x2 = this.mCenter.x - x;
        if (this.mCenter.x < this.mSlop) {
            x2 = -x2;
        }
        float y2 = this.mCenter.y - y;
        res.y = (float) Math.sqrt((x2 * x2) + (y2 * y2));
        if (y2 > 0.0f) {
            res.x = (float) Math.asin(x2 / res.y);
        } else if (y2 < 0.0f) {
            res.x = (float) (3.141592653589793d - Math.asin(x2 / res.y));
        }
        return res;
    }

    private PieItem findItem(PointF polar) {
        for (PieItem item : this.mCurrentItems) {
            if (inside(polar, this.mTouchOffset, item)) {
                return item;
            }
        }
        return null;
    }

    private boolean inside(PointF polar, float offset, PieItem item) {
        return ((float) item.getInnerRadius()) - offset < polar.y && ((float) item.getOuterRadius()) - offset > polar.y && item.getStartAngle() < polar.x && item.getStartAngle() + item.getSweep() > polar.x;
    }
}
