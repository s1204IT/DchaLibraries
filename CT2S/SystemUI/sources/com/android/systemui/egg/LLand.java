package com.android.systemui.egg;

import android.animation.TimeAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Slog;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.systemui.R;
import java.util.ArrayList;

public class LLand extends FrameLayout {
    private static Params PARAMS;
    private float dt;
    final float[] hsv;
    private TimeAnimator mAnim;
    private boolean mAnimating;
    private final AudioAttributes mAudioAttrs;
    private AudioManager mAudioManager;
    private Player mDroid;
    private boolean mFlipped;
    private boolean mFrozen;
    private int mHeight;
    private float mLastPipeTime;
    private ArrayList<Obstacle> mObstaclesInPlay;
    private boolean mPlaying;
    private int mScore;
    private TextView mScoreField;
    private View mSplash;
    private int mTimeOfDay;
    private Vibrator mVibrator;
    private int mWidth;
    private float t;
    public static final boolean DEBUG = Log.isLoggable("LLand", 3);
    public static final boolean DEBUG_IDDQD = Log.isLoggable("LLand.iddqd", 3);
    static final int[] POPS = {R.drawable.pop_belt, 0, 255, R.drawable.pop_droid, 0, 255, R.drawable.pop_pizza, 1, 255, R.drawable.pop_stripes, 0, 255, R.drawable.pop_swirl, 1, 255, R.drawable.pop_vortex, 1, 255, R.drawable.pop_vortex2, 1, 255, R.drawable.pop_ball, 0, 190};
    private static final int[][] SKIES = {new int[]{-4144897, -6250241}, new int[]{-16777200, -16777216}, new int[]{-16777152, -16777200}, new int[]{-6258656, -14663552}};
    static final Rect sTmpRect = new Rect();

    private interface GameView {
        void step(long j, long j2, float f, float f2);
    }

    public static void L(String s, Object... objects) {
        if (DEBUG) {
            if (objects.length != 0) {
                s = String.format(s, objects);
            }
            Slog.d("LLand", s);
        }
    }

    private static class Params {
        public int BOOST_DV;
        public int BUILDING_HEIGHT_MIN;
        public int BUILDING_WIDTH_MAX;
        public int BUILDING_WIDTH_MIN;
        public int CLOUD_SIZE_MAX;
        public int CLOUD_SIZE_MIN;
        public int G;
        public float HUD_Z;
        public int MAX_V;
        public int OBSTACLE_GAP;
        public int OBSTACLE_MIN;
        public int OBSTACLE_PERIOD;
        public int OBSTACLE_SPACING;
        public int OBSTACLE_STEM_WIDTH;
        public int OBSTACLE_WIDTH;
        public float OBSTACLE_Z;
        public int PLAYER_HIT_SIZE;
        public int PLAYER_SIZE;
        public float PLAYER_Z;
        public float PLAYER_Z_BOOST;
        public float SCENERY_Z;
        public int STAR_SIZE_MAX;
        public int STAR_SIZE_MIN;
        public float TRANSLATION_PER_SEC;

        public Params(Resources res) {
            this.TRANSLATION_PER_SEC = res.getDimension(R.dimen.translation_per_sec);
            this.OBSTACLE_SPACING = res.getDimensionPixelSize(R.dimen.obstacle_spacing);
            this.OBSTACLE_PERIOD = (int) (this.OBSTACLE_SPACING / this.TRANSLATION_PER_SEC);
            this.BOOST_DV = res.getDimensionPixelSize(R.dimen.boost_dv);
            this.PLAYER_HIT_SIZE = res.getDimensionPixelSize(R.dimen.player_hit_size);
            this.PLAYER_SIZE = res.getDimensionPixelSize(R.dimen.player_size);
            this.OBSTACLE_WIDTH = res.getDimensionPixelSize(R.dimen.obstacle_width);
            this.OBSTACLE_STEM_WIDTH = res.getDimensionPixelSize(R.dimen.obstacle_stem_width);
            this.OBSTACLE_GAP = res.getDimensionPixelSize(R.dimen.obstacle_gap);
            this.OBSTACLE_MIN = res.getDimensionPixelSize(R.dimen.obstacle_height_min);
            this.BUILDING_HEIGHT_MIN = res.getDimensionPixelSize(R.dimen.building_height_min);
            this.BUILDING_WIDTH_MIN = res.getDimensionPixelSize(R.dimen.building_width_min);
            this.BUILDING_WIDTH_MAX = res.getDimensionPixelSize(R.dimen.building_width_max);
            this.CLOUD_SIZE_MIN = res.getDimensionPixelSize(R.dimen.cloud_size_min);
            this.CLOUD_SIZE_MAX = res.getDimensionPixelSize(R.dimen.cloud_size_max);
            this.STAR_SIZE_MIN = res.getDimensionPixelSize(R.dimen.star_size_min);
            this.STAR_SIZE_MAX = res.getDimensionPixelSize(R.dimen.star_size_max);
            this.G = res.getDimensionPixelSize(R.dimen.G);
            this.MAX_V = res.getDimensionPixelSize(R.dimen.max_v);
            this.SCENERY_Z = res.getDimensionPixelSize(R.dimen.scenery_z);
            this.OBSTACLE_Z = res.getDimensionPixelSize(R.dimen.obstacle_z);
            this.PLAYER_Z = res.getDimensionPixelSize(R.dimen.player_z);
            this.PLAYER_Z_BOOST = res.getDimensionPixelSize(R.dimen.player_z_boost);
            this.HUD_Z = res.getDimensionPixelSize(R.dimen.hud_z);
            if (this.OBSTACLE_MIN <= this.OBSTACLE_WIDTH / 2) {
                Slog.e("LLand", "error: obstacles might be too short, adjusting");
                this.OBSTACLE_MIN = (this.OBSTACLE_WIDTH / 2) + 1;
            }
        }
    }

    public LLand(Context context) {
        this(context, null);
    }

    public LLand(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LLand(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mAudioAttrs = new AudioAttributes.Builder().setUsage(14).build();
        this.mObstaclesInPlay = new ArrayList<>();
        this.hsv = new float[]{0.0f, 0.0f, 0.0f};
        this.mVibrator = (Vibrator) context.getSystemService("vibrator");
        this.mAudioManager = (AudioManager) context.getSystemService("audio");
        setFocusable(true);
        PARAMS = new Params(getResources());
        this.mTimeOfDay = irand(0, SKIES.length);
        setLayoutDirection(0);
    }

    @Override
    public boolean willNotDraw() {
        return !DEBUG;
    }

    public float getGameTime() {
        return this.t;
    }

    public void setScoreField(TextView tv) {
        this.mScoreField = tv;
        if (tv != null) {
            tv.setTranslationZ(PARAMS.HUD_Z);
            if (!this.mAnimating || !this.mPlaying) {
                tv.setTranslationY(-500.0f);
            }
        }
    }

    public void setSplash(View v) {
        this.mSplash = v;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        stop();
        reset();
        start(false);
    }

    private void thump() {
        if (this.mAudioManager.getRingerMode() != 0) {
            this.mVibrator.vibrate(80L, this.mAudioAttrs);
        }
    }

    public void reset() {
        Scenery s;
        L("reset", new Object[0]);
        Drawable sky = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, SKIES[this.mTimeOfDay]);
        sky.setDither(true);
        setBackground(sky);
        this.mFlipped = frand() > 0.5f;
        setScaleX(this.mFlipped ? -1.0f : 1.0f);
        setScore(0);
        int i = getChildCount();
        while (true) {
            int i2 = i;
            i = i2 - 1;
            if (i2 <= 0) {
                break;
            }
            View v = getChildAt(i);
            if (v instanceof GameView) {
                removeViewAt(i);
            }
        }
        this.mObstaclesInPlay.clear();
        this.mWidth = getWidth();
        this.mHeight = getHeight();
        boolean showingSun = (this.mTimeOfDay == 0 || this.mTimeOfDay == 3) && ((double) frand()) > 0.25d;
        if (showingSun) {
            Star sun = new Star(getContext());
            sun.setBackgroundResource(R.drawable.sun);
            int w = getResources().getDimensionPixelSize(R.dimen.sun_size);
            sun.setTranslationX(frand(w, this.mWidth - w));
            if (this.mTimeOfDay == 0) {
                sun.setTranslationY(frand(w, this.mHeight * 0.66f));
                sun.getBackground().setTint(0);
            } else {
                sun.setTranslationY(frand(this.mHeight * 0.66f, this.mHeight - w));
                sun.getBackground().setTintMode(PorterDuff.Mode.SRC_ATOP);
                sun.getBackground().setTint(-1056997376);
            }
            addView(sun, new FrameLayout.LayoutParams(w, w));
        }
        if (!showingSun) {
            boolean dark = this.mTimeOfDay == 1 || this.mTimeOfDay == 2;
            float ff = frand();
            if ((dark && ff < 0.75f) || ff < 0.5f) {
                Star moon = new Star(getContext());
                moon.setBackgroundResource(R.drawable.moon);
                moon.getBackground().setAlpha(dark ? 255 : 128);
                moon.setScaleX(((double) frand()) > 0.5d ? -1.0f : 1.0f);
                moon.setRotation(moon.getScaleX() * frand(5.0f, 30.0f));
                int w2 = getResources().getDimensionPixelSize(R.dimen.sun_size);
                moon.setTranslationX(frand(w2, this.mWidth - w2));
                moon.setTranslationY(frand(w2, this.mHeight - w2));
                addView(moon, new FrameLayout.LayoutParams(w2, w2));
            }
        }
        int mh = this.mHeight / 6;
        boolean cloudless = ((double) frand()) < 0.25d;
        for (int i3 = 0; i3 < 20; i3++) {
            float r1 = frand();
            if (r1 < 0.3d && this.mTimeOfDay != 0) {
                s = new Star(getContext());
            } else if (r1 < 0.6d && !cloudless) {
                s = new Cloud(getContext());
            } else {
                s = new Building(getContext());
                s.z = i3 / 20.0f;
                s.setTranslationZ(PARAMS.SCENERY_Z * (1.0f + s.z));
                s.v = 0.85f * s.z;
                this.hsv[0] = 175.0f;
                this.hsv[1] = 0.25f;
                this.hsv[2] = 1.0f * s.z;
                s.setBackgroundColor(Color.HSVToColor(this.hsv));
                s.h = irand(PARAMS.BUILDING_HEIGHT_MIN, mh);
            }
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(s.w, s.h);
            if (s instanceof Building) {
                lp.gravity = 80;
            } else {
                lp.gravity = 48;
                float r = frand();
                if (s instanceof Star) {
                    lp.topMargin = (int) (r * r * this.mHeight);
                } else {
                    lp.topMargin = ((int) (1.0f - (((r * r) * this.mHeight) / 2.0f))) + (this.mHeight / 2);
                }
            }
            addView(s, lp);
            s.setTranslationX(frand(-lp.width, this.mWidth + lp.width));
        }
        this.mDroid = new Player(getContext());
        this.mDroid.setX(this.mWidth / 2);
        this.mDroid.setY(this.mHeight / 2);
        addView(this.mDroid, new FrameLayout.LayoutParams(PARAMS.PLAYER_SIZE, PARAMS.PLAYER_SIZE));
        this.mAnim = new TimeAnimator();
        this.mAnim.setTimeListener(new TimeAnimator.TimeListener() {
            @Override
            public void onTimeUpdate(TimeAnimator timeAnimator, long t, long dt) {
                LLand.this.step(t, dt);
            }
        });
    }

    private void setScore(int score) {
        this.mScore = score;
        if (this.mScoreField != null) {
            this.mScoreField.setText(DEBUG_IDDQD ? "??" : String.valueOf(score));
        }
    }

    private void addScore(int incr) {
        setScore(this.mScore + incr);
    }

    public void start(boolean startPlaying) {
        Object[] objArr = new Object[1];
        objArr[0] = startPlaying ? "true" : "false";
        L("start(startPlaying=%s)", objArr);
        if (startPlaying) {
            this.mPlaying = true;
            this.t = 0.0f;
            this.mLastPipeTime = getGameTime() - PARAMS.OBSTACLE_PERIOD;
            if (this.mSplash != null && this.mSplash.getAlpha() > 0.0f) {
                this.mSplash.setTranslationZ(PARAMS.HUD_Z);
                this.mSplash.animate().alpha(0.0f).translationZ(0.0f).setDuration(400L);
                this.mScoreField.animate().translationY(0.0f).setInterpolator(new DecelerateInterpolator()).setDuration(1500L);
            }
            this.mScoreField.setTextColor(-5592406);
            this.mScoreField.setBackgroundResource(R.drawable.scorecard);
            this.mDroid.setVisibility(0);
            this.mDroid.setX(this.mWidth / 2);
            this.mDroid.setY(this.mHeight / 2);
        } else {
            this.mDroid.setVisibility(8);
        }
        if (!this.mAnimating) {
            this.mAnim.start();
            this.mAnimating = true;
        }
    }

    public void stop() {
        if (this.mAnimating) {
            this.mAnim.cancel();
            this.mAnim = null;
            this.mAnimating = false;
            this.mScoreField.setTextColor(-1);
            this.mScoreField.setBackgroundResource(R.drawable.scorecard_gameover);
            this.mTimeOfDay = irand(0, SKIES.length);
            this.mFrozen = true;
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    LLand.this.mFrozen = false;
                }
            }, 250L);
        }
    }

    public static final float lerp(float x, float a, float b) {
        return ((b - a) * x) + a;
    }

    public static final float rlerp(float v, float a, float b) {
        return (v - a) / (b - a);
    }

    public static final float clamp(float f) {
        if (f < 0.0f) {
            return 0.0f;
        }
        if (f > 1.0f) {
            return 1.0f;
        }
        return f;
    }

    public static final float frand() {
        return (float) Math.random();
    }

    public static final float frand(float a, float b) {
        return lerp(frand(), a, b);
    }

    public static final int irand(int a, int b) {
        return (int) lerp(frand(), a, b);
    }

    private void step(long t_ms, long dt_ms) {
        this.t = t_ms / 1000.0f;
        this.dt = dt_ms / 1000.0f;
        if (DEBUG) {
            this.t *= 1.0f;
            this.dt *= 1.0f;
        }
        int N = getChildCount();
        int i = 0;
        while (i < N) {
            KeyEvent.Callback childAt = getChildAt(i);
            if (childAt instanceof GameView) {
                ((GameView) childAt).step(t_ms, dt_ms, this.t, this.dt);
            }
            i++;
        }
        if (this.mPlaying && this.mDroid.below(this.mHeight)) {
            if (DEBUG_IDDQD) {
                poke();
                unpoke();
            } else {
                L("player hit the floor", new Object[0]);
                thump();
                stop();
            }
        }
        boolean passedBarrier = false;
        int j = this.mObstaclesInPlay.size();
        while (true) {
            int j2 = j;
            j = j2 - 1;
            if (j2 <= 0) {
                break;
            }
            Obstacle ob = this.mObstaclesInPlay.get(j);
            if (this.mPlaying && ob.intersects(this.mDroid) && !DEBUG_IDDQD) {
                L("player hit an obstacle", new Object[0]);
                thump();
                stop();
            } else if (ob.cleared(this.mDroid)) {
                if (ob instanceof Stem) {
                    passedBarrier = true;
                }
                this.mObstaclesInPlay.remove(j);
            }
        }
        if (this.mPlaying && passedBarrier) {
            addScore(1);
        }
        while (true) {
            int i2 = i;
            i = i2 - 1;
            if (i2 <= 0) {
                break;
            }
            View v = getChildAt(i);
            if (v instanceof Obstacle) {
                if (v.getTranslationX() + v.getWidth() < 0.0f) {
                    removeViewAt(i);
                }
            } else if (v instanceof Scenery) {
                Scenery s = (Scenery) v;
                if (v.getTranslationX() + s.w < 0.0f) {
                    v.setTranslationX(getWidth());
                }
            }
        }
        if (this.mPlaying && this.t - this.mLastPipeTime > PARAMS.OBSTACLE_PERIOD) {
            this.mLastPipeTime = this.t;
            int obstacley = ((int) (frand() * ((this.mHeight - (PARAMS.OBSTACLE_MIN * 2)) - PARAMS.OBSTACLE_GAP))) + PARAMS.OBSTACLE_MIN;
            int inset = (PARAMS.OBSTACLE_WIDTH - PARAMS.OBSTACLE_STEM_WIDTH) / 2;
            int yinset = PARAMS.OBSTACLE_WIDTH / 2;
            int d1 = irand(0, 250);
            Obstacle s1 = new Stem(getContext(), obstacley - yinset, false);
            addView(s1, new FrameLayout.LayoutParams(PARAMS.OBSTACLE_STEM_WIDTH, (int) s1.h, 51));
            s1.setTranslationX(this.mWidth + inset);
            s1.setTranslationY((-s1.h) - yinset);
            s1.setTranslationZ(PARAMS.OBSTACLE_Z * 0.75f);
            s1.animate().translationY(0.0f).setStartDelay(d1).setDuration(250L);
            this.mObstaclesInPlay.add(s1);
            Obstacle p1 = new Pop(getContext(), PARAMS.OBSTACLE_WIDTH);
            addView(p1, new FrameLayout.LayoutParams(PARAMS.OBSTACLE_WIDTH, PARAMS.OBSTACLE_WIDTH, 51));
            p1.setTranslationX(this.mWidth);
            p1.setTranslationY(-PARAMS.OBSTACLE_WIDTH);
            p1.setTranslationZ(PARAMS.OBSTACLE_Z);
            p1.setScaleX(0.25f);
            p1.setScaleY(0.25f);
            p1.animate().translationY(s1.h - inset).scaleX(1.0f).scaleY(1.0f).setStartDelay(d1).setDuration(250L);
            this.mObstaclesInPlay.add(p1);
            int d2 = irand(0, 250);
            Obstacle s2 = new Stem(getContext(), ((this.mHeight - obstacley) - PARAMS.OBSTACLE_GAP) - yinset, true);
            addView(s2, new FrameLayout.LayoutParams(PARAMS.OBSTACLE_STEM_WIDTH, (int) s2.h, 51));
            s2.setTranslationX(this.mWidth + inset);
            s2.setTranslationY(this.mHeight + yinset);
            s2.setTranslationZ(PARAMS.OBSTACLE_Z * 0.75f);
            s2.animate().translationY(this.mHeight - s2.h).setStartDelay(d2).setDuration(400L);
            this.mObstaclesInPlay.add(s2);
            Obstacle p2 = new Pop(getContext(), PARAMS.OBSTACLE_WIDTH);
            addView(p2, new FrameLayout.LayoutParams(PARAMS.OBSTACLE_WIDTH, PARAMS.OBSTACLE_WIDTH, 51));
            p2.setTranslationX(this.mWidth);
            p2.setTranslationY(this.mHeight);
            p2.setTranslationZ(PARAMS.OBSTACLE_Z);
            p2.setScaleX(0.25f);
            p2.setScaleY(0.25f);
            p2.animate().translationY((this.mHeight - s2.h) - yinset).scaleX(1.0f).scaleY(1.0f).setStartDelay(d2).setDuration(400L);
            this.mObstaclesInPlay.add(p2);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        L("touch: %s", ev);
        switch (ev.getAction()) {
            case 0:
                poke();
                return true;
            case 1:
                unpoke();
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean onTrackballEvent(MotionEvent ev) {
        L("trackball: %s", ev);
        switch (ev.getAction()) {
            case 0:
                poke();
                return true;
            case 1:
                unpoke();
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent ev) {
        L("keyDown: %d", Integer.valueOf(keyCode));
        switch (keyCode) {
            case 19:
            case 23:
            case 62:
            case 66:
            case 96:
                poke();
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent ev) {
        L("keyDown: %d", Integer.valueOf(keyCode));
        switch (keyCode) {
            case 19:
            case 23:
            case 62:
            case 66:
            case 96:
                unpoke();
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent ev) {
        L("generic: %s", ev);
        return false;
    }

    private void poke() {
        L("poke", new Object[0]);
        if (!this.mFrozen) {
            if (!this.mAnimating) {
                reset();
                start(true);
            } else if (!this.mPlaying) {
                start(true);
            }
            this.mDroid.boost();
            if (DEBUG) {
                this.mDroid.dv *= 1.0f;
                this.mDroid.animate().setDuration(200L);
            }
        }
    }

    private void unpoke() {
        L("unboost", new Object[0]);
        if (!this.mFrozen && this.mAnimating) {
            this.mDroid.unboost();
        }
    }

    @Override
    public void onDraw(Canvas c) {
        super.onDraw(c);
    }

    private class Player extends ImageView implements GameView {
        public final float[] corners;
        public float dv;
        private boolean mBoosting;
        private final int[] sColors;
        private final float[] sHull;

        public Player(Context context) {
            super(context);
            this.sColors = new int[]{-8862377};
            this.sHull = new float[]{0.3f, 0.0f, 0.7f, 0.0f, 0.92f, 0.33f, 0.92f, 0.75f, 0.6f, 1.0f, 0.4f, 1.0f, 0.08f, 0.75f, 0.08f, 0.33f};
            this.corners = new float[this.sHull.length];
            setBackgroundResource(R.drawable.android);
            getBackground().setTintMode(PorterDuff.Mode.SRC_ATOP);
            getBackground().setTint(this.sColors[0]);
            setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    int w = view.getWidth();
                    int h = view.getHeight();
                    int ix = (int) (w * 0.3f);
                    int iy = (int) (h * 0.2f);
                    outline.setRect(ix, iy, w - ix, h - iy);
                }
            });
        }

        public void prepareCheckIntersections() {
            int inset = (LLand.PARAMS.PLAYER_SIZE - LLand.PARAMS.PLAYER_HIT_SIZE) / 2;
            int scale = LLand.PARAMS.PLAYER_HIT_SIZE;
            int N = this.sHull.length / 2;
            for (int i = 0; i < N; i++) {
                this.corners[i * 2] = (scale * this.sHull[i * 2]) + inset;
                this.corners[(i * 2) + 1] = (scale * this.sHull[(i * 2) + 1]) + inset;
            }
            Matrix m = getMatrix();
            m.mapPoints(this.corners);
        }

        public boolean below(int h) {
            int N = this.corners.length / 2;
            for (int i = 0; i < N; i++) {
                int y = (int) this.corners[(i * 2) + 1];
                if (y >= h) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void step(long t_ms, long dt_ms, float t, float dt) {
            if (getVisibility() == 0) {
                if (this.mBoosting) {
                    this.dv = -LLand.PARAMS.BOOST_DV;
                } else {
                    this.dv += LLand.PARAMS.G;
                }
                if (this.dv < (-LLand.PARAMS.MAX_V)) {
                    this.dv = -LLand.PARAMS.MAX_V;
                } else if (this.dv > LLand.PARAMS.MAX_V) {
                    this.dv = LLand.PARAMS.MAX_V;
                }
                float y = getTranslationY() + (this.dv * dt);
                if (y < 0.0f) {
                    y = 0.0f;
                }
                setTranslationY(y);
                setRotation(LLand.lerp(LLand.clamp(LLand.rlerp(this.dv, LLand.PARAMS.MAX_V, LLand.PARAMS.MAX_V * (-1))), 90.0f, -90.0f) + 90.0f);
                prepareCheckIntersections();
            }
        }

        public void boost() {
            this.mBoosting = true;
            this.dv = -LLand.PARAMS.BOOST_DV;
            animate().cancel();
            animate().scaleX(1.25f).scaleY(1.25f).translationZ(LLand.PARAMS.PLAYER_Z_BOOST).setDuration(100L);
            setScaleX(1.25f);
            setScaleY(1.25f);
        }

        public void unboost() {
            this.mBoosting = false;
            animate().cancel();
            animate().scaleX(1.0f).scaleY(1.0f).translationZ(LLand.PARAMS.PLAYER_Z).setDuration(200L);
        }
    }

    private class Obstacle extends View implements GameView {
        public float h;
        public final Rect hitRect;

        public Obstacle(Context context, float h) {
            super(context);
            this.hitRect = new Rect();
            setBackgroundColor(-65536);
            this.h = h;
        }

        public boolean intersects(Player p) {
            int N = p.corners.length / 2;
            for (int i = 0; i < N; i++) {
                int x = (int) p.corners[i * 2];
                int y = (int) p.corners[(i * 2) + 1];
                if (this.hitRect.contains(x, y)) {
                    return true;
                }
            }
            return false;
        }

        public boolean cleared(Player p) {
            int N = p.corners.length / 2;
            for (int i = 0; i < N; i++) {
                int x = (int) p.corners[i * 2];
                if (this.hitRect.right >= x) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public void step(long t_ms, long dt_ms, float t, float dt) {
            setTranslationX(getTranslationX() - (LLand.PARAMS.TRANSLATION_PER_SEC * dt));
            getHitRect(this.hitRect);
        }
    }

    private class Pop extends Obstacle {
        int cx;
        int cy;
        int mRotate;
        int r;

        public Pop(Context context, float h) {
            super(context, h);
            int idx = LLand.irand(0, LLand.POPS.length / 3) * 3;
            setBackgroundResource(LLand.POPS[idx]);
            setAlpha(LLand.POPS[idx + 2] / 255.0f);
            setScaleX(LLand.frand() < 0.5f ? -1.0f : 1.0f);
            this.mRotate = LLand.POPS[idx + 1] == 0 ? 0 : LLand.frand() < 0.5f ? -1 : 1;
            setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    int pad = (int) (Pop.this.getWidth() * 0.02f);
                    outline.setOval(pad, pad, Pop.this.getWidth() - pad, Pop.this.getHeight() - pad);
                }
            });
        }

        @Override
        public boolean intersects(Player p) {
            int N = p.corners.length / 2;
            for (int i = 0; i < N; i++) {
                int x = (int) p.corners[i * 2];
                int y = (int) p.corners[(i * 2) + 1];
                if (Math.hypot(x - this.cx, y - this.cy) <= this.r) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void step(long t_ms, long dt_ms, float t, float dt) {
            super.step(t_ms, dt_ms, t, dt);
            if (this.mRotate != 0) {
                setRotation(getRotation() + (45.0f * dt * this.mRotate));
            }
            this.cx = (this.hitRect.left + this.hitRect.right) / 2;
            this.cy = (this.hitRect.top + this.hitRect.bottom) / 2;
            this.r = getWidth() / 2;
        }
    }

    private class Stem extends Obstacle {
        boolean mDrawShadow;
        Paint mPaint;
        Path mShadow;

        public Stem(Context context, float h, boolean drawShadow) {
            super(context, h);
            this.mPaint = new Paint();
            this.mShadow = new Path();
            this.mDrawShadow = drawShadow;
            this.mPaint.setColor(-5592406);
            setBackground(null);
        }

        @Override
        public void onAttachedToWindow() {
            super.onAttachedToWindow();
            setWillNotDraw(false);
            setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRect(0, 0, Stem.this.getWidth(), Stem.this.getHeight());
                }
            });
        }

        @Override
        public void onDraw(Canvas c) {
            int w = c.getWidth();
            int h = c.getHeight();
            GradientDrawable g = new GradientDrawable();
            g.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
            g.setGradientCenter(w * 0.75f, 0.0f);
            g.setColors(new int[]{-1, -5592406});
            g.setBounds(0, 0, w, h);
            g.draw(c);
            if (this.mDrawShadow) {
                this.mShadow.reset();
                this.mShadow.moveTo(0.0f, 0.0f);
                this.mShadow.lineTo(w, 0.0f);
                this.mShadow.lineTo(w, (LLand.PARAMS.OBSTACLE_WIDTH / 2) + (w * 1.5f));
                this.mShadow.lineTo(0.0f, LLand.PARAMS.OBSTACLE_WIDTH / 2);
                this.mShadow.close();
                c.drawPath(this.mShadow, this.mPaint);
            }
        }
    }

    private class Scenery extends FrameLayout implements GameView {
        public int h;
        public float v;
        public int w;
        public float z;

        public Scenery(Context context) {
            super(context);
        }

        @Override
        public void step(long t_ms, long dt_ms, float t, float dt) {
            setTranslationX(getTranslationX() - ((LLand.PARAMS.TRANSLATION_PER_SEC * dt) * this.v));
        }
    }

    private class Building extends Scenery {
        public Building(Context context) {
            super(context);
            this.w = LLand.irand(LLand.PARAMS.BUILDING_WIDTH_MIN, LLand.PARAMS.BUILDING_WIDTH_MAX);
            this.h = 0;
            setTranslationZ(LLand.PARAMS.SCENERY_Z);
        }
    }

    private class Cloud extends Scenery {
        public Cloud(Context context) {
            super(context);
            setBackgroundResource(LLand.frand() < 0.01f ? R.drawable.cloud_off : R.drawable.cloud);
            getBackground().setAlpha(64);
            int iIrand = LLand.irand(LLand.PARAMS.CLOUD_SIZE_MIN, LLand.PARAMS.CLOUD_SIZE_MAX);
            this.h = iIrand;
            this.w = iIrand;
            this.z = 0.0f;
            this.v = LLand.frand(0.15f, 0.5f);
        }
    }

    private class Star extends Scenery {
        public Star(Context context) {
            super(context);
            setBackgroundResource(R.drawable.star);
            int iIrand = LLand.irand(LLand.PARAMS.STAR_SIZE_MIN, LLand.PARAMS.STAR_SIZE_MAX);
            this.h = iIrand;
            this.w = iIrand;
            this.z = 0.0f;
            this.v = 0.0f;
        }
    }
}
