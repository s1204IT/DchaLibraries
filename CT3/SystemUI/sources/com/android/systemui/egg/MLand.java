package com.android.systemui.egg;

import android.animation.LayoutTransition;
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
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.R;
import java.util.ArrayList;

public class MLand extends FrameLayout {
    private static Params PARAMS;
    private float dt;
    private TimeAnimator mAnim;
    private boolean mAnimating;
    private final AudioAttributes mAudioAttrs;
    private AudioManager mAudioManager;
    private int mCountdown;
    private int mCurrentPipeId;
    private boolean mFlipped;
    private boolean mFrozen;
    private ArrayList<Integer> mGameControllers;
    private int mHeight;
    private float mLastPipeTime;
    private ArrayList<Obstacle> mObstaclesInPlay;
    private Paint mPlayerTracePaint;
    private ArrayList<Player> mPlayers;
    private boolean mPlaying;
    private int mScene;
    private ViewGroup mScoreFields;
    private View mSplash;
    private int mTaps;
    private int mTimeOfDay;
    private Paint mTouchPaint;
    private Vibrator mVibrator;
    private int mWidth;
    private float t;
    public static final boolean DEBUG = Log.isLoggable("MLand", 3);
    public static final boolean DEBUG_IDDQD = Log.isLoggable("MLand.iddqd", 3);
    private static final int[][] SKIES = {new int[]{-4144897, -6250241}, new int[]{-16777200, -16777216}, new int[]{-16777152, -16777200}, new int[]{-6258656, -14663552}};
    private static float dp = 1.0f;
    static final float[] hsv = {0.0f, 0.0f, 0.0f};
    static final Rect sTmpRect = new Rect();
    static final int[] ANTENNAE = {R.drawable.mm_antennae, R.drawable.mm_antennae2};
    static final int[] EYES = {R.drawable.mm_eyes, R.drawable.mm_eyes2};
    static final int[] MOUTHS = {R.drawable.mm_mouth1, R.drawable.mm_mouth2, R.drawable.mm_mouth3, R.drawable.mm_mouth4};
    static final int[] CACTI = {R.drawable.cactus1, R.drawable.cactus2, R.drawable.cactus3};
    static final int[] MOUNTAINS = {R.drawable.mountain1, R.drawable.mountain2, R.drawable.mountain3};

    private interface GameView {
        void step(long j, long j2, float f, float f2);
    }

    public static void L(String s, Object... objects) {
        if (!DEBUG) {
            return;
        }
        if (objects.length != 0) {
            s = String.format(s, objects);
        }
        Log.d("MLand", s);
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
            if (this.OBSTACLE_MIN > this.OBSTACLE_WIDTH / 2) {
                return;
            }
            MLand.L("error: obstacles might be too short, adjusting", new Object[0]);
            this.OBSTACLE_MIN = (this.OBSTACLE_WIDTH / 2) + 1;
        }
    }

    public MLand(Context context) {
        this(context, null);
    }

    public MLand(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MLand(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mAudioAttrs = new AudioAttributes.Builder().setUsage(14).build();
        this.mPlayers = new ArrayList<>();
        this.mObstaclesInPlay = new ArrayList<>();
        this.mCountdown = 0;
        this.mGameControllers = new ArrayList<>();
        this.mVibrator = (Vibrator) context.getSystemService("vibrator");
        this.mAudioManager = (AudioManager) context.getSystemService("audio");
        setFocusable(true);
        PARAMS = new Params(getResources());
        this.mTimeOfDay = irand(0, SKIES.length - 1);
        this.mScene = irand(0, 3);
        this.mTouchPaint = new Paint(1);
        this.mTouchPaint.setColor(-2130706433);
        this.mTouchPaint.setStyle(Paint.Style.FILL);
        this.mPlayerTracePaint = new Paint(1);
        this.mPlayerTracePaint.setColor(-2130706433);
        this.mPlayerTracePaint.setStyle(Paint.Style.STROKE);
        this.mPlayerTracePaint.setStrokeWidth(dp * 2.0f);
        setLayoutDirection(0);
        setupPlayers(1);
        MetricsLogger.count(getContext(), "egg_mland_create", 1);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        dp = getResources().getDisplayMetrics().density;
        reset();
        start(false);
    }

    @Override
    public boolean willNotDraw() {
        return !DEBUG;
    }

    public float getGameTime() {
        return this.t;
    }

    public void setScoreFieldHolder(ViewGroup vg) {
        this.mScoreFields = vg;
        if (vg != null) {
            LayoutTransition lt = new LayoutTransition();
            lt.setDuration(250L);
            this.mScoreFields.setLayoutTransition(lt);
        }
        for (Player p : this.mPlayers) {
            this.mScoreFields.addView(p.mScoreField, new ViewGroup.MarginLayoutParams(-2, -1));
        }
    }

    public void setSplash(View v) {
        this.mSplash = v;
    }

    public static boolean isGamePad(InputDevice dev) {
        int sources = dev.getSources();
        return (sources & 1025) == 1025 || (sources & 16777232) == 16777232;
    }

    public ArrayList getGameControllers() {
        this.mGameControllers.clear();
        int[] deviceIds = InputDevice.getDeviceIds();
        for (int deviceId : deviceIds) {
            InputDevice dev = InputDevice.getDevice(deviceId);
            if (isGamePad(dev) && !this.mGameControllers.contains(Integer.valueOf(deviceId))) {
                this.mGameControllers.add(Integer.valueOf(deviceId));
            }
        }
        return this.mGameControllers;
    }

    public int getControllerPlayer(int id) {
        int player = this.mGameControllers.indexOf(Integer.valueOf(id));
        if (player < 0 || player >= this.mPlayers.size()) {
            return 0;
        }
        return player;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        dp = getResources().getDisplayMetrics().density;
        stop();
        reset();
        start(false);
    }

    public static float luma(int bgcolor) {
        return (((16711680 & bgcolor) * 0.2126f) / 1.671168E7f) + (((65280 & bgcolor) * 0.7152f) / 65280.0f) + (((bgcolor & 255) * 0.0722f) / 255.0f);
    }

    public Player getPlayer(int i) {
        if (i < this.mPlayers.size()) {
            return this.mPlayers.get(i);
        }
        return null;
    }

    private int addPlayerInternal(Player p) {
        this.mPlayers.add(p);
        realignPlayers();
        TextView scoreField = (TextView) LayoutInflater.from(getContext()).inflate(R.layout.mland_scorefield, (ViewGroup) null);
        if (this.mScoreFields != null) {
            this.mScoreFields.addView(scoreField, new ViewGroup.MarginLayoutParams(-2, -1));
        }
        p.setScoreField(scoreField);
        return this.mPlayers.size() - 1;
    }

    private void removePlayerInternal(Player p) {
        if (!this.mPlayers.remove(p)) {
            return;
        }
        removeView(p);
        this.mScoreFields.removeView(p.mScoreField);
        realignPlayers();
    }

    private void realignPlayers() {
        int N = this.mPlayers.size();
        float x = (this.mWidth - ((N - 1) * PARAMS.PLAYER_SIZE)) / 2;
        for (int i = 0; i < N; i++) {
            Player p = this.mPlayers.get(i);
            p.setX(x);
            x += PARAMS.PLAYER_SIZE;
        }
    }

    private void clearPlayers() {
        while (this.mPlayers.size() > 0) {
            removePlayerInternal(this.mPlayers.get(0));
        }
    }

    public void setupPlayers(int num) {
        clearPlayers();
        for (int i = 0; i < num; i++) {
            addPlayerInternal(Player.create(this));
        }
    }

    public void addPlayer() {
        if (getNumPlayers() == 6) {
            return;
        }
        addPlayerInternal(Player.create(this));
    }

    public int getNumPlayers() {
        return this.mPlayers.size();
    }

    public void removePlayer() {
        if (getNumPlayers() == 1) {
            return;
        }
        removePlayerInternal(this.mPlayers.get(this.mPlayers.size() - 1));
    }

    private void thump(int playerIndex, long ms) {
        if (this.mAudioManager.getRingerMode() == 0) {
            return;
        }
        if (playerIndex < this.mGameControllers.size()) {
            int controllerId = this.mGameControllers.get(playerIndex).intValue();
            InputDevice dev = InputDevice.getDevice(controllerId);
            if (dev != null && dev.getVibrator().hasVibrator()) {
                dev.getVibrator().vibrate((long) (ms * 2.0f), this.mAudioAttrs);
                return;
            }
        }
        this.mVibrator.vibrate(ms, this.mAudioAttrs);
    }

    public void reset() {
        boolean showingSun;
        Scenery s;
        L("reset", new Object[0]);
        Drawable sky = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, SKIES[this.mTimeOfDay]);
        sky.setDither(true);
        setBackground(sky);
        this.mFlipped = frand() > 0.5f;
        setScaleX(this.mFlipped ? -1 : 1);
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
        this.mCurrentPipeId = 0;
        this.mWidth = getWidth();
        this.mHeight = getHeight();
        if (this.mTimeOfDay == 0 || this.mTimeOfDay == 3) {
            showingSun = ((double) frand()) > 0.25d;
        } else {
            showingSun = false;
        }
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
                moon.setScaleX(((double) frand()) > 0.5d ? -1 : 1);
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
                switch (this.mScene) {
                    case 1:
                        s = new Cactus(getContext());
                        break;
                    case 2:
                        s = new Mountain(getContext());
                        break;
                    default:
                        s = new Building(getContext());
                        break;
                }
                s.z = i3 / 20.0f;
                s.v = s.z * 0.85f;
                if (this.mScene == 0) {
                    s.setBackgroundColor(-7829368);
                    s.h = irand(PARAMS.BUILDING_HEIGHT_MIN, mh);
                }
                int c = (int) (s.z * 255.0f);
                Drawable bg = s.getBackground();
                if (bg != null) {
                    bg.setColorFilter(Color.rgb(c, c, c), PorterDuff.Mode.MULTIPLY);
                }
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
        for (Player p : this.mPlayers) {
            addView(p);
            p.reset();
        }
        realignPlayers();
        if (this.mAnim != null) {
            this.mAnim.cancel();
        }
        this.mAnim = new TimeAnimator();
        this.mAnim.setTimeListener(new TimeAnimator.TimeListener() {
            @Override
            public void onTimeUpdate(TimeAnimator timeAnimator, long t, long dt) {
                MLand.this.step(t, dt);
            }
        });
    }

    public void start(boolean startPlaying) {
        Object[] objArr = new Object[1];
        objArr[0] = startPlaying ? "true" : "false";
        L("start(startPlaying=%s)", objArr);
        if (startPlaying && this.mCountdown <= 0) {
            showSplash();
            this.mSplash.findViewById(R.id.play_button).setEnabled(false);
            View playImage = this.mSplash.findViewById(R.id.play_button_image);
            final TextView playText = (TextView) this.mSplash.findViewById(R.id.play_button_text);
            playImage.animate().alpha(0.0f);
            playText.animate().alpha(1.0f);
            this.mCountdown = 3;
            post(new Runnable() {
                @Override
                public void run() {
                    if (MLand.this.mCountdown == 0) {
                        MLand.this.startPlaying();
                    } else {
                        MLand.this.postDelayed(this, 500L);
                    }
                    playText.setText(String.valueOf(MLand.this.mCountdown));
                    MLand mLand = MLand.this;
                    mLand.mCountdown--;
                }
            });
        }
        for (Player p : this.mPlayers) {
            p.setVisibility(4);
        }
        if (this.mAnimating) {
            return;
        }
        this.mAnim.start();
        this.mAnimating = true;
    }

    public void hideSplash() {
        if (this.mSplash == null || this.mSplash.getVisibility() != 0) {
            return;
        }
        this.mSplash.setClickable(false);
        this.mSplash.animate().alpha(0.0f).translationZ(0.0f).setDuration(300L).withEndAction(new Runnable() {
            @Override
            public void run() {
                MLand.this.mSplash.setVisibility(8);
            }
        });
    }

    public void showSplash() {
        if (this.mSplash == null || this.mSplash.getVisibility() == 0) {
            return;
        }
        this.mSplash.setClickable(true);
        this.mSplash.setAlpha(0.0f);
        this.mSplash.setVisibility(0);
        this.mSplash.animate().alpha(1.0f).setDuration(1000L);
        this.mSplash.findViewById(R.id.play_button_image).setAlpha(1.0f);
        this.mSplash.findViewById(R.id.play_button_text).setAlpha(0.0f);
        this.mSplash.findViewById(R.id.play_button).setEnabled(true);
        this.mSplash.findViewById(R.id.play_button).requestFocus();
    }

    public void startPlaying() {
        this.mPlaying = true;
        this.t = 0.0f;
        this.mLastPipeTime = getGameTime() - PARAMS.OBSTACLE_PERIOD;
        hideSplash();
        realignPlayers();
        this.mTaps = 0;
        int N = this.mPlayers.size();
        MetricsLogger.histogram(getContext(), "egg_mland_players", N);
        for (int i = 0; i < N; i++) {
            Player p = this.mPlayers.get(i);
            p.setVisibility(0);
            p.reset();
            p.start();
            p.boost(-1.0f, -1.0f);
            p.unboost();
        }
    }

    public void stop() {
        if (!this.mAnimating) {
            return;
        }
        this.mAnim.cancel();
        this.mAnim = null;
        this.mAnimating = false;
        this.mPlaying = false;
        this.mTimeOfDay = irand(0, SKIES.length - 1);
        this.mScene = irand(0, 3);
        this.mFrozen = true;
        for (Player p : this.mPlayers) {
            p.die();
        }
        postDelayed(new Runnable() {
            @Override
            public void run() {
                MLand.this.mFrozen = false;
            }
        }, 250L);
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
        return Math.round(frand(a, b));
    }

    public static int pick(int[] l) {
        return l[irand(0, l.length - 1)];
    }

    public void step(long t_ms, long dt_ms) {
        this.t = t_ms / 1000.0f;
        this.dt = dt_ms / 1000.0f;
        if (DEBUG) {
            this.t *= 0.5f;
            this.dt *= 0.5f;
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
        if (this.mPlaying) {
            int livingPlayers = 0;
            i = 0;
            while (i < this.mPlayers.size()) {
                Player p = getPlayer(i);
                if (p.mAlive) {
                    if (p.below(this.mHeight)) {
                        if (DEBUG_IDDQD) {
                            poke(i);
                            unpoke(i);
                        } else {
                            L("player %d hit the floor", Integer.valueOf(i));
                            thump(i, 80L);
                            p.die();
                        }
                    }
                    int maxPassedStem = 0;
                    int j = this.mObstaclesInPlay.size();
                    while (true) {
                        int j2 = j;
                        j = j2 - 1;
                        if (j2 <= 0) {
                            break;
                        }
                        Obstacle ob = this.mObstaclesInPlay.get(j);
                        if (ob.intersects(p) && !DEBUG_IDDQD) {
                            L("player hit an obstacle", new Object[0]);
                            thump(i, 80L);
                            p.die();
                        } else if (ob.cleared(p) && (ob instanceof Stem)) {
                            maxPassedStem = Math.max(maxPassedStem, ((Stem) ob).id);
                        }
                    }
                    if (maxPassedStem > p.mScore) {
                        p.addScore(1);
                    }
                }
                if (p.mAlive) {
                    livingPlayers++;
                }
                i++;
            }
            if (livingPlayers == 0) {
                stop();
                MetricsLogger.count(getContext(), "egg_mland_taps", this.mTaps);
                this.mTaps = 0;
                int playerCount = this.mPlayers.size();
                for (int pi = 0; pi < playerCount; pi++) {
                    MetricsLogger.histogram(getContext(), "egg_mland_score", this.mPlayers.get(pi).getScore());
                }
            }
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
                    this.mObstaclesInPlay.remove(v);
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
            this.mCurrentPipeId++;
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
            p1.setScaleY(-0.25f);
            p1.animate().translationY(s1.h - inset).scaleX(1.0f).scaleY(-1.0f).setStartDelay(d1).setDuration(250L);
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
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        L("touch: %s", ev);
        int actionIndex = ev.getActionIndex();
        float x = ev.getX(actionIndex);
        float y = ev.getY(actionIndex);
        int playerIndex = (int) (getNumPlayers() * (x / getWidth()));
        if (this.mFlipped) {
            playerIndex = (getNumPlayers() - 1) - playerIndex;
        }
        switch (ev.getActionMasked()) {
            case 0:
            case 5:
                poke(playerIndex, x, y);
                return true;
            case 1:
            case 6:
                unpoke(playerIndex);
                return true;
            case 2:
            case 3:
            case 4:
            default:
                return false;
        }
    }

    @Override
    public boolean onTrackballEvent(MotionEvent ev) {
        L("trackball: %s", ev);
        switch (ev.getAction()) {
            case 0:
                poke(0);
                return true;
            case 1:
                unpoke(0);
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
                int player = getControllerPlayer(ev.getDeviceId());
                poke(player);
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
                int player = getControllerPlayer(ev.getDeviceId());
                unpoke(player);
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

    private void poke(int playerIndex) {
        poke(playerIndex, -1.0f, -1.0f);
    }

    private void poke(int playerIndex, float x, float y) {
        L("poke(%d)", Integer.valueOf(playerIndex));
        if (this.mFrozen) {
            return;
        }
        if (!this.mAnimating) {
            reset();
        }
        if (!this.mPlaying) {
            start(true);
            return;
        }
        Player p = getPlayer(playerIndex);
        if (p == null) {
            return;
        }
        p.boost(x, y);
        this.mTaps++;
        if (!DEBUG) {
            return;
        }
        p.dv *= 0.5f;
        p.animate().setDuration(400L);
    }

    private void unpoke(int playerIndex) {
        Player p;
        L("unboost(%d)", Integer.valueOf(playerIndex));
        if (this.mFrozen || !this.mAnimating || !this.mPlaying || (p = getPlayer(playerIndex)) == null) {
            return;
        }
        p.unboost();
    }

    @Override
    public void onDraw(Canvas c) {
        super.onDraw(c);
        for (Player p : this.mPlayers) {
            if (p.mTouchX > 0.0f) {
                this.mTouchPaint.setColor(p.color & (-2130706433));
                this.mPlayerTracePaint.setColor(p.color & (-2130706433));
                float x1 = p.mTouchX;
                float y1 = p.mTouchY;
                c.drawCircle(x1, y1, 100.0f, this.mTouchPaint);
                float x2 = p.getX() + p.getPivotX();
                float y2 = p.getY() + p.getPivotY();
                float angle = 1.5707964f - ((float) Math.atan2(x2 - x1, y2 - y1));
                c.drawLine((float) (((double) x1) + (Math.cos(angle) * 100.0d)), (float) (((double) y1) + (Math.sin(angle) * 100.0d)), x2, y2, this.mPlayerTracePaint);
            }
        }
    }

    private static class Player extends ImageView implements GameView {
        static int sNextColor = 0;
        public int color;
        public final float[] corners;
        public float dv;
        private boolean mAlive;
        private boolean mBoosting;
        private MLand mLand;
        private int mScore;
        private TextView mScoreField;
        private float mTouchX;
        private float mTouchY;
        private final int[] sColors;
        private final float[] sHull;

        public static Player create(MLand land) {
            Player p = new Player(land.getContext());
            p.mLand = land;
            p.reset();
            p.setVisibility(4);
            land.addView(p, new FrameLayout.LayoutParams(MLand.PARAMS.PLAYER_SIZE, MLand.PARAMS.PLAYER_SIZE));
            return p;
        }

        private void setScore(int score) {
            this.mScore = score;
            if (this.mScoreField == null) {
                return;
            }
            this.mScoreField.setText(MLand.DEBUG_IDDQD ? "??" : String.valueOf(score));
        }

        public int getScore() {
            return this.mScore;
        }

        public void addScore(int incr) {
            setScore(this.mScore + incr);
        }

        public void setScoreField(TextView tv) {
            this.mScoreField = tv;
            if (tv == null) {
                return;
            }
            setScore(this.mScore);
            this.mScoreField.getBackground().setColorFilter(this.color, PorterDuff.Mode.SRC_ATOP);
            this.mScoreField.setTextColor(MLand.luma(this.color) > 0.7f ? -16777216 : -1);
        }

        public void reset() {
            setY(((this.mLand.mHeight / 2) + ((int) (Math.random() * ((double) MLand.PARAMS.PLAYER_SIZE)))) - (MLand.PARAMS.PLAYER_SIZE / 2));
            setScore(0);
            setScoreField(this.mScoreField);
            this.mBoosting = false;
            this.dv = 0.0f;
        }

        public Player(Context context) {
            super(context);
            this.mTouchX = -1.0f;
            this.mTouchY = -1.0f;
            this.sColors = new int[]{-2407369, -12879641, -740352, -15753896, -8710016, -6381922};
            this.sHull = new float[]{0.3f, 0.0f, 0.7f, 0.0f, 0.92f, 0.33f, 0.92f, 0.75f, 0.6f, 1.0f, 0.4f, 1.0f, 0.08f, 0.75f, 0.08f, 0.33f};
            this.corners = new float[this.sHull.length];
            setBackgroundResource(R.drawable.android);
            getBackground().setTintMode(PorterDuff.Mode.SRC_ATOP);
            int[] iArr = this.sColors;
            int i = sNextColor;
            sNextColor = i + 1;
            this.color = iArr[i % this.sColors.length];
            getBackground().setTint(this.color);
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
            int inset = (MLand.PARAMS.PLAYER_SIZE - MLand.PARAMS.PLAYER_HIT_SIZE) / 2;
            int scale = MLand.PARAMS.PLAYER_HIT_SIZE;
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
            if (!this.mAlive) {
                setTranslationX(getTranslationX() - (MLand.PARAMS.TRANSLATION_PER_SEC * dt));
                return;
            }
            if (this.mBoosting) {
                this.dv = -MLand.PARAMS.BOOST_DV;
            } else {
                this.dv += MLand.PARAMS.G;
            }
            if (this.dv < (-MLand.PARAMS.MAX_V)) {
                this.dv = -MLand.PARAMS.MAX_V;
            } else if (this.dv > MLand.PARAMS.MAX_V) {
                this.dv = MLand.PARAMS.MAX_V;
            }
            float y = getTranslationY() + (this.dv * dt);
            if (y < 0.0f) {
                y = 0.0f;
            }
            setTranslationY(y);
            setRotation(MLand.lerp(MLand.clamp(MLand.rlerp(this.dv, MLand.PARAMS.MAX_V, MLand.PARAMS.MAX_V * (-1))), 90.0f, -90.0f) + 90.0f);
            prepareCheckIntersections();
        }

        public void boost(float x, float y) {
            this.mTouchX = x;
            this.mTouchY = y;
            boost();
        }

        public void boost() {
            this.mBoosting = true;
            this.dv = -MLand.PARAMS.BOOST_DV;
            animate().cancel();
            animate().scaleX(1.25f).scaleY(1.25f).translationZ(MLand.PARAMS.PLAYER_Z_BOOST).setDuration(100L);
            setScaleX(1.25f);
            setScaleY(1.25f);
        }

        public void unboost() {
            this.mBoosting = false;
            this.mTouchY = -1.0f;
            this.mTouchX = -1.0f;
            animate().cancel();
            animate().scaleX(1.0f).scaleY(1.0f).translationZ(MLand.PARAMS.PLAYER_Z).setDuration(200L);
        }

        public void die() {
            this.mAlive = false;
            if (this.mScoreField != null) {
            }
        }

        public void start() {
            this.mAlive = true;
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
            setTranslationX(getTranslationX() - (MLand.PARAMS.TRANSLATION_PER_SEC * dt));
            getHitRect(this.hitRect);
        }
    }

    private class Pop extends Obstacle {
        Drawable antenna;
        int cx;
        int cy;
        Drawable eyes;
        int mRotate;
        Drawable mouth;
        int r;

        public Pop(Context context, float h) {
            super(context, h);
            setBackgroundResource(R.drawable.mm_head);
            this.antenna = context.getDrawable(MLand.pick(MLand.ANTENNAE));
            if (MLand.frand() > 0.5f) {
                this.eyes = context.getDrawable(MLand.pick(MLand.EYES));
                if (MLand.frand() > 0.8f) {
                    this.mouth = context.getDrawable(MLand.pick(MLand.MOUTHS));
                }
            }
            setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    int pad = (int) ((Pop.this.getWidth() * 1.0f) / 6.0f);
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
            this.r = getWidth() / 3;
        }

        @Override
        public void onDraw(Canvas c) {
            super.onDraw(c);
            if (this.antenna != null) {
                this.antenna.setBounds(0, 0, c.getWidth(), c.getHeight());
                this.antenna.draw(c);
            }
            if (this.eyes != null) {
                this.eyes.setBounds(0, 0, c.getWidth(), c.getHeight());
                this.eyes.draw(c);
            }
            if (this.mouth == null) {
                return;
            }
            this.mouth.setBounds(0, 0, c.getWidth(), c.getHeight());
            this.mouth.draw(c);
        }
    }

    private class Stem extends Obstacle {
        int id;
        boolean mDrawShadow;
        GradientDrawable mGradient;
        Path mJandystripe;
        Paint mPaint;
        Paint mPaint2;
        Path mShadow;

        public Stem(Context context, float h, boolean drawShadow) {
            super(context, h);
            this.mPaint = new Paint();
            this.mShadow = new Path();
            this.mGradient = new GradientDrawable();
            this.id = MLand.this.mCurrentPipeId;
            this.mDrawShadow = drawShadow;
            setBackground(null);
            this.mGradient.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
            this.mPaint.setColor(-16777216);
            this.mPaint.setColorFilter(new PorterDuffColorFilter(570425344, PorterDuff.Mode.MULTIPLY));
            if (MLand.frand() < 0.01f) {
                this.mGradient.setColors(new int[]{-1, -2236963});
                this.mJandystripe = new Path();
                this.mPaint2 = new Paint();
                this.mPaint2.setColor(-65536);
                this.mPaint2.setColorFilter(new PorterDuffColorFilter(-65536, PorterDuff.Mode.MULTIPLY));
                return;
            }
            this.mGradient.setColors(new int[]{-4412764, -6190977});
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
            this.mGradient.setGradientCenter(w * 0.75f, 0.0f);
            this.mGradient.setBounds(0, 0, w, h);
            this.mGradient.draw(c);
            if (this.mJandystripe != null) {
                this.mJandystripe.reset();
                this.mJandystripe.moveTo(0.0f, w);
                this.mJandystripe.lineTo(w, 0.0f);
                this.mJandystripe.lineTo(w, w * 2);
                this.mJandystripe.lineTo(0.0f, w * 3);
                this.mJandystripe.close();
                for (int y = 0; y < h; y += w * 4) {
                    c.drawPath(this.mJandystripe, this.mPaint2);
                    this.mJandystripe.offset(0.0f, w * 4);
                }
            }
            if (this.mDrawShadow) {
                this.mShadow.reset();
                this.mShadow.moveTo(0.0f, 0.0f);
                this.mShadow.lineTo(w, 0.0f);
                this.mShadow.lineTo(w, (MLand.PARAMS.OBSTACLE_WIDTH * 0.4f) + (w * 1.5f));
                this.mShadow.lineTo(0.0f, MLand.PARAMS.OBSTACLE_WIDTH * 0.4f);
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
            setTranslationX(getTranslationX() - ((MLand.PARAMS.TRANSLATION_PER_SEC * dt) * this.v));
        }
    }

    private class Building extends Scenery {
        public Building(Context context) {
            super(context);
            this.w = MLand.irand(MLand.PARAMS.BUILDING_WIDTH_MIN, MLand.PARAMS.BUILDING_WIDTH_MAX);
            this.h = 0;
        }
    }

    private class Cactus extends Building {
        public Cactus(Context context) {
            super(context);
            setBackgroundResource(MLand.pick(MLand.CACTI));
            int iIrand = MLand.irand(MLand.PARAMS.BUILDING_WIDTH_MAX / 4, MLand.PARAMS.BUILDING_WIDTH_MAX / 2);
            this.h = iIrand;
            this.w = iIrand;
        }
    }

    private class Mountain extends Building {
        public Mountain(Context context) {
            super(context);
            setBackgroundResource(MLand.pick(MLand.MOUNTAINS));
            int iIrand = MLand.irand(MLand.PARAMS.BUILDING_WIDTH_MAX / 2, MLand.PARAMS.BUILDING_WIDTH_MAX);
            this.h = iIrand;
            this.w = iIrand;
            this.z = 0.0f;
        }
    }

    private class Cloud extends Scenery {
        public Cloud(Context context) {
            super(context);
            setBackgroundResource(MLand.frand() < 0.01f ? R.drawable.cloud_off : R.drawable.cloud);
            getBackground().setAlpha(64);
            int iIrand = MLand.irand(MLand.PARAMS.CLOUD_SIZE_MIN, MLand.PARAMS.CLOUD_SIZE_MAX);
            this.h = iIrand;
            this.w = iIrand;
            this.z = 0.0f;
            this.v = MLand.frand(0.15f, 0.5f);
        }
    }

    private class Star extends Scenery {
        public Star(Context context) {
            super(context);
            setBackgroundResource(R.drawable.star);
            int iIrand = MLand.irand(MLand.PARAMS.STAR_SIZE_MIN, MLand.PARAMS.STAR_SIZE_MAX);
            this.h = iIrand;
            this.w = iIrand;
            this.z = 0.0f;
            this.v = 0.0f;
        }
    }
}
