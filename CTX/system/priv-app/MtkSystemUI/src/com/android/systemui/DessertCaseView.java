package com.android.systemui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
/* loaded from: classes.dex */
public class DessertCaseView extends FrameLayout {
    float[] hsv;
    private int mCellSize;
    private View[] mCells;
    private int mColumns;
    private SparseArray<Drawable> mDrawables;
    private final Set<Point> mFreeList;
    private final Handler mHandler;
    private int mHeight;
    private final Runnable mJuggle;
    private int mRows;
    private boolean mStarted;
    private int mWidth;
    private final HashSet<View> tmpSet;
    private static final String TAG = DessertCaseView.class.getSimpleName();
    private static final int[] PASTRIES = {R.drawable.dessert_kitkat, R.drawable.dessert_android};
    private static final int[] RARE_PASTRIES = {R.drawable.dessert_cupcake, R.drawable.dessert_donut, R.drawable.dessert_eclair, R.drawable.dessert_froyo, R.drawable.dessert_gingerbread, R.drawable.dessert_honeycomb, R.drawable.dessert_ics, R.drawable.dessert_jellybean};
    private static final int[] XRARE_PASTRIES = {R.drawable.dessert_petitfour, R.drawable.dessert_donutburger, R.drawable.dessert_flan, R.drawable.dessert_keylimepie};
    private static final int[] XXRARE_PASTRIES = {R.drawable.dessert_zombiegingerbread, R.drawable.dessert_dandroid, R.drawable.dessert_jandycane};
    private static final int NUM_PASTRIES = ((PASTRIES.length + RARE_PASTRIES.length) + XRARE_PASTRIES.length) + XXRARE_PASTRIES.length;
    private static final float[] MASK = {0.0f, 0.0f, 0.0f, 0.0f, 255.0f, 0.0f, 0.0f, 0.0f, 0.0f, 255.0f, 0.0f, 0.0f, 0.0f, 0.0f, 255.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f};
    private static final float[] ALPHA_MASK = {0.0f, 0.0f, 0.0f, 0.0f, 255.0f, 0.0f, 0.0f, 0.0f, 0.0f, 255.0f, 0.0f, 0.0f, 0.0f, 0.0f, 255.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f};
    private static final float[] WHITE_MASK = {0.0f, 0.0f, 0.0f, 0.0f, 255.0f, 0.0f, 0.0f, 0.0f, 0.0f, 255.0f, 0.0f, 0.0f, 0.0f, 0.0f, 255.0f, -1.0f, 0.0f, 0.0f, 0.0f, 255.0f};

    public DessertCaseView(Context context) {
        this(context, null);
    }

    public DessertCaseView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public DessertCaseView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        int[] iArr;
        this.mDrawables = new SparseArray<>(NUM_PASTRIES);
        this.mFreeList = new HashSet();
        this.mHandler = new Handler();
        this.mJuggle = new Runnable() { // from class: com.android.systemui.DessertCaseView.1
            @Override // java.lang.Runnable
            public void run() {
                int childCount = DessertCaseView.this.getChildCount();
                for (int i2 = 0; i2 < 1; i2++) {
                    DessertCaseView.this.place(DessertCaseView.this.getChildAt((int) (Math.random() * childCount)), true);
                }
                DessertCaseView.this.fillFreeList();
                if (DessertCaseView.this.mStarted) {
                    DessertCaseView.this.mHandler.postDelayed(DessertCaseView.this.mJuggle, 2000L);
                }
            }
        };
        this.hsv = new float[]{0.0f, 1.0f, 0.85f};
        this.tmpSet = new HashSet<>();
        Resources resources = getResources();
        this.mStarted = false;
        this.mCellSize = resources.getDimensionPixelSize(R.dimen.dessert_case_cell_size);
        BitmapFactory.Options options = new BitmapFactory.Options();
        if (this.mCellSize < 512) {
            options.inSampleSize = 2;
        }
        options.inMutable = true;
        Bitmap bitmap = null;
        int[][] iArr2 = {PASTRIES, RARE_PASTRIES, XRARE_PASTRIES, XXRARE_PASTRIES};
        int length = iArr2.length;
        int i2 = 0;
        while (i2 < length) {
            Bitmap bitmap2 = bitmap;
            for (int i3 : iArr2[i2]) {
                options.inBitmap = bitmap2;
                bitmap2 = BitmapFactory.decodeResource(resources, i3, options);
                BitmapDrawable bitmapDrawable = new BitmapDrawable(resources, convertToAlphaMask(bitmap2));
                bitmapDrawable.setColorFilter(new ColorMatrixColorFilter(ALPHA_MASK));
                bitmapDrawable.setBounds(0, 0, this.mCellSize, this.mCellSize);
                this.mDrawables.append(i3, bitmapDrawable);
            }
            i2++;
            bitmap = bitmap2;
        }
    }

    private static Bitmap convertToAlphaMask(Bitmap bitmap) {
        Bitmap createBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ALPHA_8);
        Canvas canvas = new Canvas(createBitmap);
        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(MASK));
        canvas.drawBitmap(bitmap, 0.0f, 0.0f, paint);
        return createBitmap;
    }

    public void start() {
        if (!this.mStarted) {
            this.mStarted = true;
            fillFreeList(2000);
        }
        this.mHandler.postDelayed(this.mJuggle, 5000L);
    }

    public void stop() {
        this.mStarted = false;
        this.mHandler.removeCallbacks(this.mJuggle);
    }

    int pick(int[] iArr) {
        return iArr[(int) (Math.random() * iArr.length)];
    }

    int random_color() {
        this.hsv[0] = irand(0, 12) * 30.0f;
        return Color.HSVToColor(this.hsv);
    }

    @Override // android.view.View
    protected synchronized void onSizeChanged(int i, int i2, int i3, int i4) {
        super.onSizeChanged(i, i2, i3, i4);
        if (this.mWidth == i && this.mHeight == i2) {
            return;
        }
        boolean z = this.mStarted;
        if (z) {
            stop();
        }
        this.mWidth = i;
        this.mHeight = i2;
        this.mCells = null;
        removeAllViewsInLayout();
        this.mFreeList.clear();
        this.mRows = this.mHeight / this.mCellSize;
        this.mColumns = this.mWidth / this.mCellSize;
        this.mCells = new View[this.mRows * this.mColumns];
        setScaleX(0.25f);
        setScaleY(0.25f);
        setTranslationX((this.mWidth - (this.mCellSize * this.mColumns)) * 0.5f * 0.25f);
        setTranslationY(0.5f * (this.mHeight - (this.mCellSize * this.mRows)) * 0.25f);
        for (int i5 = 0; i5 < this.mRows; i5++) {
            for (int i6 = 0; i6 < this.mColumns; i6++) {
                this.mFreeList.add(new Point(i6, i5));
            }
        }
        if (z) {
            start();
        }
    }

    public void fillFreeList() {
        fillFreeList(500);
    }

    public synchronized void fillFreeList(int i) {
        Drawable drawable;
        Context context = getContext();
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(this.mCellSize, this.mCellSize);
        while (!this.mFreeList.isEmpty()) {
            Point next = this.mFreeList.iterator().next();
            this.mFreeList.remove(next);
            if (this.mCells[(next.y * this.mColumns) + next.x] == null) {
                final ImageView imageView = new ImageView(context);
                imageView.setOnClickListener(new View.OnClickListener() { // from class: com.android.systemui.DessertCaseView.2
                    @Override // android.view.View.OnClickListener
                    public void onClick(View view) {
                        DessertCaseView.this.place(imageView, true);
                        DessertCaseView.this.postDelayed(new Runnable() { // from class: com.android.systemui.DessertCaseView.2.1
                            @Override // java.lang.Runnable
                            public void run() {
                                DessertCaseView.this.fillFreeList();
                            }
                        }, 250L);
                    }
                });
                imageView.setBackgroundColor(random_color());
                float frand = frand();
                if (frand < 5.0E-4f) {
                    drawable = this.mDrawables.get(pick(XXRARE_PASTRIES));
                } else if (frand < 0.005f) {
                    drawable = this.mDrawables.get(pick(XRARE_PASTRIES));
                } else if (frand < 0.5f) {
                    drawable = this.mDrawables.get(pick(RARE_PASTRIES));
                } else if (frand < 0.7f) {
                    drawable = this.mDrawables.get(pick(PASTRIES));
                } else {
                    drawable = null;
                }
                if (drawable != null) {
                    imageView.getOverlay().add(drawable);
                }
                int i2 = this.mCellSize;
                layoutParams.height = i2;
                layoutParams.width = i2;
                addView(imageView, layoutParams);
                place(imageView, next, false);
                if (i > 0) {
                    float intValue = ((Integer) imageView.getTag(33554434)).intValue();
                    float f = 0.5f * intValue;
                    imageView.setScaleX(f);
                    imageView.setScaleY(f);
                    imageView.setAlpha(0.0f);
                    imageView.animate().withLayer().scaleX(intValue).scaleY(intValue).alpha(1.0f).setDuration(i);
                }
            }
        }
    }

    public void place(View view, boolean z) {
        place(view, new Point(irand(0, this.mColumns), irand(0, this.mRows)), z);
    }

    private final Animator.AnimatorListener makeHardwareLayerListener(final View view) {
        return new AnimatorListenerAdapter() { // from class: com.android.systemui.DessertCaseView.3
            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationStart(Animator animator) {
                if (view.isAttachedToWindow()) {
                    view.setLayerType(2, null);
                    view.buildLayer();
                }
            }

            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator) {
                if (view.isAttachedToWindow()) {
                    view.setLayerType(0, null);
                }
            }
        };
    }

    public synchronized void place(View view, Point point, boolean z) {
        int i;
        Point[] occupied;
        Point[] occupied2;
        int i2 = point.x;
        int i3 = point.y;
        float frand = frand();
        if (view.getTag(33554433) != null) {
            for (Point point2 : getOccupied(view)) {
                this.mFreeList.add(point2);
                this.mCells[(point2.y * this.mColumns) + point2.x] = null;
            }
        }
        if (frand < 0.01f) {
            if (i2 < this.mColumns - 3 && i3 < this.mRows - 3) {
                i = 4;
            }
            i = 1;
        } else if (frand < 0.1f) {
            if (i2 < this.mColumns - 2 && i3 < this.mRows - 2) {
                i = 3;
            }
            i = 1;
        } else {
            if (frand < 0.33f && i2 != this.mColumns - 1 && i3 != this.mRows - 1) {
                i = 2;
            }
            i = 1;
        }
        view.setTag(33554433, point);
        view.setTag(33554434, Integer.valueOf(i));
        this.tmpSet.clear();
        Point[] occupied3 = getOccupied(view);
        for (Point point3 : occupied3) {
            View view2 = this.mCells[(point3.y * this.mColumns) + point3.x];
            if (view2 != null) {
                this.tmpSet.add(view2);
            }
        }
        Iterator<View> it = this.tmpSet.iterator();
        while (it.hasNext()) {
            final View next = it.next();
            for (Point point4 : getOccupied(next)) {
                this.mFreeList.add(point4);
                this.mCells[(point4.y * this.mColumns) + point4.x] = null;
            }
            if (next != view) {
                next.setTag(33554433, null);
                if (z) {
                    next.animate().withLayer().scaleX(0.5f).scaleY(0.5f).alpha(0.0f).setDuration(500L).setInterpolator(new AccelerateInterpolator()).setListener(new Animator.AnimatorListener() { // from class: com.android.systemui.DessertCaseView.4
                        @Override // android.animation.Animator.AnimatorListener
                        public void onAnimationStart(Animator animator) {
                        }

                        @Override // android.animation.Animator.AnimatorListener
                        public void onAnimationEnd(Animator animator) {
                            DessertCaseView.this.removeView(next);
                        }

                        @Override // android.animation.Animator.AnimatorListener
                        public void onAnimationCancel(Animator animator) {
                        }

                        @Override // android.animation.Animator.AnimatorListener
                        public void onAnimationRepeat(Animator animator) {
                        }
                    }).start();
                } else {
                    removeView(next);
                }
            }
        }
        for (Point point5 : occupied3) {
            this.mCells[(point5.y * this.mColumns) + point5.x] = view;
            this.mFreeList.remove(point5);
        }
        float irand = irand(0, 4) * 90.0f;
        if (z) {
            view.bringToFront();
            AnimatorSet animatorSet = new AnimatorSet();
            float f = i;
            animatorSet.playTogether(ObjectAnimator.ofFloat(view, View.SCALE_X, f), ObjectAnimator.ofFloat(view, View.SCALE_Y, f));
            animatorSet.setInterpolator(new AnticipateOvershootInterpolator());
            animatorSet.setDuration(500L);
            AnimatorSet animatorSet2 = new AnimatorSet();
            int i4 = i - 1;
            animatorSet2.playTogether(ObjectAnimator.ofFloat(view, View.ROTATION, irand), ObjectAnimator.ofFloat(view, View.X, (i2 * this.mCellSize) + ((this.mCellSize * i4) / 2)), ObjectAnimator.ofFloat(view, View.Y, (i3 * this.mCellSize) + ((i4 * this.mCellSize) / 2)));
            animatorSet2.setInterpolator(new DecelerateInterpolator());
            animatorSet2.setDuration(500L);
            animatorSet.addListener(makeHardwareLayerListener(view));
            animatorSet.start();
            animatorSet2.start();
        } else {
            int i5 = i - 1;
            view.setX((i2 * this.mCellSize) + ((this.mCellSize * i5) / 2));
            view.setY((i3 * this.mCellSize) + ((i5 * this.mCellSize) / 2));
            float f2 = i;
            view.setScaleX(f2);
            view.setScaleY(f2);
            view.setRotation(irand);
        }
    }

    private Point[] getOccupied(View view) {
        int intValue = ((Integer) view.getTag(33554434)).intValue();
        Point point = (Point) view.getTag(33554433);
        if (point == null || intValue == 0) {
            return new Point[0];
        }
        Point[] pointArr = new Point[intValue * intValue];
        int i = 0;
        int i2 = 0;
        while (i < intValue) {
            int i3 = i2;
            int i4 = 0;
            while (i4 < intValue) {
                pointArr[i3] = new Point(point.x + i, point.y + i4);
                i4++;
                i3++;
            }
            i++;
            i2 = i3;
        }
        return pointArr;
    }

    static float frand() {
        return (float) Math.random();
    }

    static float frand(float f, float f2) {
        return (frand() * (f2 - f)) + f;
    }

    static int irand(int i, int i2) {
        return (int) frand(i, i2);
    }

    @Override // android.view.View
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    /* loaded from: classes.dex */
    public static class RescalingContainer extends FrameLayout {
        private DessertCaseView mView;

        public RescalingContainer(Context context) {
            super(context);
            setSystemUiVisibility(5638);
        }

        public void setView(DessertCaseView dessertCaseView) {
            addView(dessertCaseView);
            this.mView = dessertCaseView;
        }

        @Override // android.widget.FrameLayout, android.view.ViewGroup, android.view.View
        protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
            float f = i3 - i;
            float f2 = i4 - i2;
            DessertCaseView dessertCaseView = this.mView;
            int i5 = (int) ((f / 0.25f) / 2.0f);
            DessertCaseView dessertCaseView2 = this.mView;
            int i6 = (int) ((f2 / 0.25f) / 2.0f);
            int i7 = (int) (i + (f * 0.5f));
            int i8 = (int) (i2 + (f2 * 0.5f));
            this.mView.layout(i7 - i5, i8 - i6, i7 + i5, i8 + i6);
        }
    }
}
