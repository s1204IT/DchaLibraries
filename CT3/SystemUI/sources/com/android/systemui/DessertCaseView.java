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
import android.util.Property;
import android.util.SparseArray;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import java.util.HashSet;
import java.util.Set;

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

    public DessertCaseView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DessertCaseView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mDrawables = new SparseArray<>(NUM_PASTRIES);
        this.mFreeList = new HashSet();
        this.mHandler = new Handler();
        this.mJuggle = new Runnable() {
            @Override
            public void run() {
                int N = DessertCaseView.this.getChildCount();
                for (int i = 0; i < 1; i++) {
                    View child = DessertCaseView.this.getChildAt((int) (Math.random() * ((double) N)));
                    DessertCaseView.this.place(child, true);
                }
                DessertCaseView.this.fillFreeList();
                if (!DessertCaseView.this.mStarted) {
                    return;
                }
                DessertCaseView.this.mHandler.postDelayed(DessertCaseView.this.mJuggle, 2000L);
            }
        };
        this.hsv = new float[]{0.0f, 1.0f, 0.85f};
        this.tmpSet = new HashSet<>();
        Resources res = getResources();
        this.mStarted = false;
        this.mCellSize = res.getDimensionPixelSize(R.dimen.dessert_case_cell_size);
        BitmapFactory.Options opts = new BitmapFactory.Options();
        if (this.mCellSize < 512) {
            opts.inSampleSize = 2;
        }
        opts.inMutable = true;
        Bitmap loaded = null;
        for (int[] list : new int[][]{PASTRIES, RARE_PASTRIES, XRARE_PASTRIES, XXRARE_PASTRIES}) {
            for (int resid : list) {
                opts.inBitmap = loaded;
                loaded = BitmapFactory.decodeResource(res, resid, opts);
                BitmapDrawable d = new BitmapDrawable(res, convertToAlphaMask(loaded));
                d.setColorFilter(new ColorMatrixColorFilter(ALPHA_MASK));
                d.setBounds(0, 0, this.mCellSize, this.mCellSize);
                this.mDrawables.append(resid, d);
            }
        }
    }

    private static Bitmap convertToAlphaMask(Bitmap b) {
        Bitmap a = Bitmap.createBitmap(b.getWidth(), b.getHeight(), Bitmap.Config.ALPHA_8);
        Canvas c = new Canvas(a);
        Paint pt = new Paint();
        pt.setColorFilter(new ColorMatrixColorFilter(MASK));
        c.drawBitmap(b, 0.0f, 0.0f, pt);
        return a;
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

    int pick(int[] a) {
        return a[(int) (Math.random() * ((double) a.length))];
    }

    int random_color() {
        this.hsv[0] = irand(0, 12) * 30.0f;
        return Color.HSVToColor(this.hsv);
    }

    @Override
    protected synchronized void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (this.mWidth == w && this.mHeight == h) {
            return;
        }
        boolean wasStarted = this.mStarted;
        if (wasStarted) {
            stop();
        }
        this.mWidth = w;
        this.mHeight = h;
        this.mCells = null;
        removeAllViewsInLayout();
        this.mFreeList.clear();
        this.mRows = this.mHeight / this.mCellSize;
        this.mColumns = this.mWidth / this.mCellSize;
        this.mCells = new View[this.mRows * this.mColumns];
        setScaleX(0.25f);
        setScaleY(0.25f);
        setTranslationX((this.mWidth - (this.mCellSize * this.mColumns)) * 0.5f * 0.25f);
        setTranslationY((this.mHeight - (this.mCellSize * this.mRows)) * 0.5f * 0.25f);
        for (int j = 0; j < this.mRows; j++) {
            for (int i = 0; i < this.mColumns; i++) {
                this.mFreeList.add(new Point(i, j));
            }
        }
        if (wasStarted) {
            start();
        }
    }

    public void fillFreeList() {
        fillFreeList(500);
    }

    public synchronized void fillFreeList(int animationLen) {
        Drawable d;
        Context ctx = getContext();
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(this.mCellSize, this.mCellSize);
        while (!this.mFreeList.isEmpty()) {
            Point pt = this.mFreeList.iterator().next();
            this.mFreeList.remove(pt);
            int i = pt.x;
            int j = pt.y;
            if (this.mCells[(this.mColumns * j) + i] == null) {
                final ImageView v = new ImageView(ctx);
                v.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        DessertCaseView.this.place(v, true);
                        DessertCaseView.this.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                DessertCaseView.this.fillFreeList();
                            }
                        }, 250L);
                    }
                });
                int c = random_color();
                v.setBackgroundColor(c);
                float which = frand();
                if (which < 5.0E-4f) {
                    d = this.mDrawables.get(pick(XXRARE_PASTRIES));
                } else if (which < 0.005f) {
                    d = this.mDrawables.get(pick(XRARE_PASTRIES));
                } else if (which < 0.5f) {
                    d = this.mDrawables.get(pick(RARE_PASTRIES));
                } else if (which < 0.7f) {
                    d = this.mDrawables.get(pick(PASTRIES));
                } else {
                    d = null;
                }
                if (d != null) {
                    v.getOverlay().add(d);
                }
                int i2 = this.mCellSize;
                lp.height = i2;
                lp.width = i2;
                addView(v, lp);
                place(v, pt, false);
                if (animationLen > 0) {
                    float s = ((Integer) v.getTag(33554434)).intValue();
                    v.setScaleX(0.5f * s);
                    v.setScaleY(0.5f * s);
                    v.setAlpha(0.0f);
                    v.animate().withLayer().scaleX(s).scaleY(s).alpha(1.0f).setDuration(animationLen);
                }
            }
        }
    }

    public void place(View v, boolean animate) {
        place(v, new Point(irand(0, this.mColumns), irand(0, this.mRows)), animate);
    }

    private final Animator.AnimatorListener makeHardwareLayerListener(final View v) {
        return new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animator) {
                if (v.isAttachedToWindow()) {
                    v.setLayerType(2, null);
                    v.buildLayer();
                }
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                if (v.isAttachedToWindow()) {
                    v.setLayerType(0, null);
                }
            }
        };
    }

    public synchronized void place(View v, Point pt, boolean animate) {
        int i = pt.x;
        int j = pt.y;
        float rnd = frand();
        if (v.getTag(33554433) != null) {
            for (Point oc : getOccupied(v)) {
                this.mFreeList.add(oc);
                this.mCells[(oc.y * this.mColumns) + oc.x] = null;
            }
        }
        int scale = 1;
        if (rnd < 0.01f) {
            if (i < this.mColumns - 3 && j < this.mRows - 3) {
                scale = 4;
            }
        } else if (rnd < 0.1f) {
            if (i < this.mColumns - 2 && j < this.mRows - 2) {
                scale = 3;
            }
        } else if (rnd < 0.33f && i != this.mColumns - 1 && j != this.mRows - 1) {
            scale = 2;
        }
        v.setTag(33554433, pt);
        v.setTag(33554434, Integer.valueOf(scale));
        this.tmpSet.clear();
        Point[] occupied = getOccupied(v);
        for (Point oc2 : occupied) {
            View squatter = this.mCells[(oc2.y * this.mColumns) + oc2.x];
            if (squatter != null) {
                this.tmpSet.add(squatter);
            }
        }
        for (final View squatter2 : this.tmpSet) {
            for (Point sq : getOccupied(squatter2)) {
                this.mFreeList.add(sq);
                this.mCells[(sq.y * this.mColumns) + sq.x] = null;
            }
            if (squatter2 != v) {
                squatter2.setTag(33554433, null);
                if (animate) {
                    squatter2.animate().withLayer().scaleX(0.5f).scaleY(0.5f).alpha(0.0f).setDuration(500L).setInterpolator(new AccelerateInterpolator()).setListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animator) {
                        }

                        @Override
                        public void onAnimationEnd(Animator animator) {
                            DessertCaseView.this.removeView(squatter2);
                        }

                        @Override
                        public void onAnimationCancel(Animator animator) {
                        }

                        @Override
                        public void onAnimationRepeat(Animator animator) {
                        }
                    }).start();
                } else {
                    removeView(squatter2);
                }
            }
        }
        for (Point oc3 : occupied) {
            this.mCells[(oc3.y * this.mColumns) + oc3.x] = v;
            this.mFreeList.remove(oc3);
        }
        float rot = irand(0, 4) * 90.0f;
        if (animate) {
            v.bringToFront();
            AnimatorSet set1 = new AnimatorSet();
            set1.playTogether(ObjectAnimator.ofFloat(v, (Property<View, Float>) View.SCALE_X, scale), ObjectAnimator.ofFloat(v, (Property<View, Float>) View.SCALE_Y, scale));
            set1.setInterpolator(new AnticipateOvershootInterpolator());
            set1.setDuration(500L);
            AnimatorSet set2 = new AnimatorSet();
            set2.playTogether(ObjectAnimator.ofFloat(v, (Property<View, Float>) View.ROTATION, rot), ObjectAnimator.ofFloat(v, (Property<View, Float>) View.X, (this.mCellSize * i) + (((scale - 1) * this.mCellSize) / 2)), ObjectAnimator.ofFloat(v, (Property<View, Float>) View.Y, (this.mCellSize * j) + (((scale - 1) * this.mCellSize) / 2)));
            set2.setInterpolator(new DecelerateInterpolator());
            set2.setDuration(500L);
            set1.addListener(makeHardwareLayerListener(v));
            set1.start();
            set2.start();
        } else {
            v.setX((this.mCellSize * i) + (((scale - 1) * this.mCellSize) / 2));
            v.setY((this.mCellSize * j) + (((scale - 1) * this.mCellSize) / 2));
            v.setScaleX(scale);
            v.setScaleY(scale);
            v.setRotation(rot);
        }
    }

    private Point[] getOccupied(View v) {
        int scale = ((Integer) v.getTag(33554434)).intValue();
        Point pt = (Point) v.getTag(33554433);
        if (pt == null || scale == 0) {
            return new Point[0];
        }
        Point[] result = new Point[scale * scale];
        int p = 0;
        int i = 0;
        while (i < scale) {
            int j = 0;
            int p2 = p;
            while (j < scale) {
                result[p2] = new Point(pt.x + i, pt.y + j);
                j++;
                p2++;
            }
            i++;
            p = p2;
        }
        return result;
    }

    static float frand() {
        return (float) Math.random();
    }

    static float frand(float a, float b) {
        return (frand() * (b - a)) + a;
    }

    static int irand(int a, int b) {
        return (int) frand(a, b);
    }

    @Override
    public void onDraw(Canvas c) {
        super.onDraw(c);
    }

    public static class RescalingContainer extends FrameLayout {
        private DessertCaseView mView;

        public RescalingContainer(Context context) {
            super(context);
            setSystemUiVisibility(5638);
        }

        public void setView(DessertCaseView v) {
            addView(v);
            this.mView = v;
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            float w = right - left;
            float h = bottom - top;
            int w2 = (int) ((w / 0.25f) / 2.0f);
            int h2 = (int) ((h / 0.25f) / 2.0f);
            int cx = (int) (left + (0.5f * w));
            int cy = (int) (top + (0.5f * h));
            this.mView.layout(cx - w2, cy - h2, cx + w2, cy + h2);
        }
    }
}
