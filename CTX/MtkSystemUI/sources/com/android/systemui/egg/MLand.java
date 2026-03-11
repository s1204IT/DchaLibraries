package com.android.systemui.egg;

import android.animation.LayoutTransition;
import android.animation.TimeAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
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
import java.util.Iterator;

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

    static int access$210(MLand mLand) {
        int i = mLand.mCountdown;
        mLand.mCountdown = i - 1;
        return i;
    }

    public static void L(String str, Object... objArr) {
        if (DEBUG) {
            if (objArr.length != 0) {
                str = String.format(str, objArr);
            }
            Log.d("MLand", str);
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

        public Params(Resources resources) {
            this.TRANSLATION_PER_SEC = resources.getDimension(R.dimen.translation_per_sec);
            this.OBSTACLE_SPACING = resources.getDimensionPixelSize(R.dimen.obstacle_spacing);
            this.OBSTACLE_PERIOD = (int) (this.OBSTACLE_SPACING / this.TRANSLATION_PER_SEC);
            this.BOOST_DV = resources.getDimensionPixelSize(R.dimen.boost_dv);
            this.PLAYER_HIT_SIZE = resources.getDimensionPixelSize(R.dimen.player_hit_size);
            this.PLAYER_SIZE = resources.getDimensionPixelSize(R.dimen.player_size);
            this.OBSTACLE_WIDTH = resources.getDimensionPixelSize(R.dimen.obstacle_width);
            this.OBSTACLE_STEM_WIDTH = resources.getDimensionPixelSize(R.dimen.obstacle_stem_width);
            this.OBSTACLE_GAP = resources.getDimensionPixelSize(R.dimen.obstacle_gap);
            this.OBSTACLE_MIN = resources.getDimensionPixelSize(R.dimen.obstacle_height_min);
            this.BUILDING_HEIGHT_MIN = resources.getDimensionPixelSize(R.dimen.building_height_min);
            this.BUILDING_WIDTH_MIN = resources.getDimensionPixelSize(R.dimen.building_width_min);
            this.BUILDING_WIDTH_MAX = resources.getDimensionPixelSize(R.dimen.building_width_max);
            this.CLOUD_SIZE_MIN = resources.getDimensionPixelSize(R.dimen.cloud_size_min);
            this.CLOUD_SIZE_MAX = resources.getDimensionPixelSize(R.dimen.cloud_size_max);
            this.STAR_SIZE_MIN = resources.getDimensionPixelSize(R.dimen.star_size_min);
            this.STAR_SIZE_MAX = resources.getDimensionPixelSize(R.dimen.star_size_max);
            this.G = resources.getDimensionPixelSize(R.dimen.G);
            this.MAX_V = resources.getDimensionPixelSize(R.dimen.max_v);
            this.SCENERY_Z = resources.getDimensionPixelSize(R.dimen.scenery_z);
            this.OBSTACLE_Z = resources.getDimensionPixelSize(R.dimen.obstacle_z);
            this.PLAYER_Z = resources.getDimensionPixelSize(R.dimen.player_z);
            this.PLAYER_Z_BOOST = resources.getDimensionPixelSize(R.dimen.player_z_boost);
            this.HUD_Z = resources.getDimensionPixelSize(R.dimen.hud_z);
            if (this.OBSTACLE_MIN <= this.OBSTACLE_WIDTH / 2) {
                MLand.L("error: obstacles might be too short, adjusting", new Object[0]);
                this.OBSTACLE_MIN = (this.OBSTACLE_WIDTH / 2) + 1;
            }
        }
    }

    public MLand(Context context) {
        this(context, null);
    }

    public MLand(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public MLand(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
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
        this.mPlayerTracePaint.setStrokeWidth(2.0f * dp);
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

    public void setScoreFieldHolder(ViewGroup viewGroup) {
        this.mScoreFields = viewGroup;
        if (viewGroup != null) {
            LayoutTransition layoutTransition = new LayoutTransition();
            layoutTransition.setDuration(250L);
            this.mScoreFields.setLayoutTransition(layoutTransition);
        }
        Iterator<Player> it = this.mPlayers.iterator();
        while (it.hasNext()) {
            this.mScoreFields.addView(it.next().mScoreField, new ViewGroup.MarginLayoutParams(-2, -1));
        }
    }

    public void setSplash(View view) {
        this.mSplash = view;
    }

    public static boolean isGamePad(InputDevice inputDevice) {
        int sources = inputDevice.getSources();
        return (sources & 1025) == 1025 || (sources & 16777232) == 16777232;
    }

    public ArrayList getGameControllers() {
        this.mGameControllers.clear();
        for (int i : InputDevice.getDeviceIds()) {
            if (isGamePad(InputDevice.getDevice(i)) && !this.mGameControllers.contains(Integer.valueOf(i))) {
                this.mGameControllers.add(Integer.valueOf(i));
            }
        }
        return this.mGameControllers;
    }

    public int getControllerPlayer(int i) {
        int iIndexOf = this.mGameControllers.indexOf(Integer.valueOf(i));
        if (iIndexOf < 0 || iIndexOf >= this.mPlayers.size()) {
            return 0;
        }
        return iIndexOf;
    }

    @Override
    protected void onSizeChanged(int i, int i2, int i3, int i4) {
        dp = getResources().getDisplayMetrics().density;
        stop();
        reset();
        start(false);
    }

    public static float luma(int i) {
        return ((0.2126f * (16711680 & i)) / 1.671168E7f) + ((0.7152f * (65280 & i)) / 65280.0f) + ((0.0722f * (i & 255)) / 255.0f);
    }

    public Player getPlayer(int i) {
        if (i < this.mPlayers.size()) {
            return this.mPlayers.get(i);
        }
        return null;
    }

    private int addPlayerInternal(Player player) {
        this.mPlayers.add(player);
        realignPlayers();
        TextView textView = (TextView) LayoutInflater.from(getContext()).inflate(R.layout.mland_scorefield, (ViewGroup) null);
        if (this.mScoreFields != null) {
            this.mScoreFields.addView(textView, new ViewGroup.MarginLayoutParams(-2, -1));
        }
        player.setScoreField(textView);
        return this.mPlayers.size() - 1;
    }

    private void removePlayerInternal(Player player) {
        if (this.mPlayers.remove(player)) {
            removeView(player);
            this.mScoreFields.removeView(player.mScoreField);
            realignPlayers();
        }
    }

    private void realignPlayers() {
        int size = this.mPlayers.size();
        float f = (this.mWidth - ((size - 1) * PARAMS.PLAYER_SIZE)) / 2;
        for (int i = 0; i < size; i++) {
            this.mPlayers.get(i).setX(f);
            f += PARAMS.PLAYER_SIZE;
        }
    }

    private void clearPlayers() {
        while (this.mPlayers.size() > 0) {
            removePlayerInternal(this.mPlayers.get(0));
        }
    }

    public void setupPlayers(int i) {
        clearPlayers();
        for (int i2 = 0; i2 < i; i2++) {
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

    private void thump(int i, long j) {
        InputDevice device;
        if (this.mAudioManager.getRingerMode() == 0) {
            return;
        }
        if (i < this.mGameControllers.size() && (device = InputDevice.getDevice(this.mGameControllers.get(i).intValue())) != null && device.getVibrator().hasVibrator()) {
            device.getVibrator().vibrate((long) (j * 2.0f), this.mAudioAttrs);
        } else {
            this.mVibrator.vibrate(j, this.mAudioAttrs);
        }
    }

    public void reset() {
        Scenery cactus;
        L("reset", new Object[0]);
        Drawable gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, SKIES[this.mTimeOfDay]);
        boolean z = true;
        gradientDrawable.setDither(true);
        setBackground(gradientDrawable);
        this.mFlipped = frand() > 0.5f;
        setScaleX(this.mFlipped ? -1.0f : 1.0f);
        int childCount = getChildCount();
        while (true) {
            int i = childCount - 1;
            if (childCount <= 0) {
                break;
            }
            if (getChildAt(i) instanceof GameView) {
                removeViewAt(i);
            }
            childCount = i;
        }
        this.mObstaclesInPlay.clear();
        this.mCurrentPipeId = 0;
        this.mWidth = getWidth();
        this.mHeight = getHeight();
        boolean z2 = (this.mTimeOfDay == 0 || this.mTimeOfDay == 3) && ((double) frand()) > 0.25d;
        if (z2) {
            Star star = new Star(getContext());
            star.setBackgroundResource(R.drawable.sun);
            int dimensionPixelSize = getResources().getDimensionPixelSize(R.dimen.sun_size);
            float f = dimensionPixelSize;
            star.setTranslationX(frand(f, this.mWidth - dimensionPixelSize));
            if (this.mTimeOfDay == 0) {
                star.setTranslationY(frand(f, this.mHeight * 0.66f));
                star.getBackground().setTint(0);
            } else {
                star.setTranslationY(frand(this.mHeight * 0.66f, this.mHeight - dimensionPixelSize));
                star.getBackground().setTintMode(PorterDuff.Mode.SRC_ATOP);
                star.getBackground().setTint(-1056997376);
            }
            addView(star, new FrameLayout.LayoutParams(dimensionPixelSize, dimensionPixelSize));
        }
        if (!z2) {
            boolean z3 = this.mTimeOfDay == 1 || this.mTimeOfDay == 2;
            float fFrand = frand();
            if ((z3 && fFrand < 0.75f) || fFrand < 0.5f) {
                Star star2 = new Star(getContext());
                star2.setBackgroundResource(R.drawable.moon);
                star2.getBackground().setAlpha(z3 ? 255 : 128);
                star2.setScaleX(((double) frand()) <= 0.5d ? 1.0f : -1.0f);
                star2.setRotation(star2.getScaleX() * frand(5.0f, 30.0f));
                int dimensionPixelSize2 = getResources().getDimensionPixelSize(R.dimen.sun_size);
                float f2 = dimensionPixelSize2;
                star2.setTranslationX(frand(f2, this.mWidth - dimensionPixelSize2));
                star2.setTranslationY(frand(f2, this.mHeight - dimensionPixelSize2));
                addView(star2, new FrameLayout.LayoutParams(dimensionPixelSize2, dimensionPixelSize2));
            }
        }
        int i2 = this.mHeight / 6;
        if (frand() >= 0.25d) {
            z = false;
        }
        for (int i3 = 0; i3 < 20; i3++) {
            double dFrand = frand();
            if (dFrand < 0.3d && this.mTimeOfDay != 0) {
                cactus = new Star(getContext());
            } else if (dFrand < 0.6d && !z) {
                cactus = new Cloud(getContext());
            } else {
                switch (this.mScene) {
                    case 1:
                        cactus = new Cactus(getContext());
                        break;
                    case 2:
                        cactus = new Mountain(getContext());
                        break;
                    default:
                        cactus = new Building(getContext());
                        break;
                }
                cactus.z = i3 / 20.0f;
                cactus.v = 0.85f * cactus.z;
                if (this.mScene == 0) {
                    cactus.setBackgroundColor(-7829368);
                    cactus.h = irand(PARAMS.BUILDING_HEIGHT_MIN, i2);
                }
                int i4 = (int) (255.0f * cactus.z);
                Drawable background = cactus.getBackground();
                if (background != null) {
                    background.setColorFilter(Color.rgb(i4, i4, i4), PorterDuff.Mode.MULTIPLY);
                }
            }
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(cactus.w, cactus.h);
            if (cactus instanceof Building) {
                layoutParams.gravity = 80;
            } else {
                layoutParams.gravity = 48;
                float fFrand2 = frand();
                if (!(cactus instanceof Star)) {
                    layoutParams.topMargin = ((int) (1.0f - (((fFrand2 * fFrand2) * this.mHeight) / 2.0f))) + (this.mHeight / 2);
                } else {
                    layoutParams.topMargin = (int) (fFrand2 * fFrand2 * this.mHeight);
                }
            }
            addView(cactus, layoutParams);
            cactus.setTranslationX(frand(-layoutParams.width, this.mWidth + layoutParams.width));
        }
        for (Player player : this.mPlayers) {
            addView(player);
            player.reset();
        }
        realignPlayers();
        if (this.mAnim != null) {
            this.mAnim.cancel();
        }
        this.mAnim = new TimeAnimator();
        this.mAnim.setTimeListener(new TimeAnimator.TimeListener() {
            @Override
            public void onTimeUpdate(TimeAnimator timeAnimator, long j, long j2) {
                MLand.this.step(j, j2);
            }
        });
    }

    public void start(boolean z) {
        Object[] objArr = new Object[1];
        objArr[0] = z ? "true" : "false";
        L("start(startPlaying=%s)", objArr);
        if (z && this.mCountdown <= 0) {
            showSplash();
            this.mSplash.findViewById(R.id.play_button).setEnabled(false);
            View viewFindViewById = this.mSplash.findViewById(R.id.play_button_image);
            final TextView textView = (TextView) this.mSplash.findViewById(R.id.play_button_text);
            viewFindViewById.animate().alpha(0.0f);
            textView.animate().alpha(1.0f);
            this.mCountdown = 3;
            post(new Runnable() {
                @Override
                public void run() {
                    if (MLand.this.mCountdown == 0) {
                        MLand.this.startPlaying();
                    } else {
                        MLand.this.postDelayed(this, 500L);
                    }
                    textView.setText(String.valueOf(MLand.this.mCountdown));
                    MLand.access$210(MLand.this);
                }
            });
        }
        Iterator<Player> it = this.mPlayers.iterator();
        while (it.hasNext()) {
            it.next().setVisibility(4);
        }
        if (!this.mAnimating) {
            this.mAnim.start();
            this.mAnimating = true;
        }
    }

    public void hideSplash() {
        if (this.mSplash != null && this.mSplash.getVisibility() == 0) {
            this.mSplash.setClickable(false);
            this.mSplash.animate().alpha(0.0f).translationZ(0.0f).setDuration(300L).withEndAction(new Runnable() {
                @Override
                public void run() {
                    MLand.this.mSplash.setVisibility(8);
                }
            });
        }
    }

    public void showSplash() {
        if (this.mSplash != null && this.mSplash.getVisibility() != 0) {
            this.mSplash.setClickable(true);
            this.mSplash.setAlpha(0.0f);
            this.mSplash.setVisibility(0);
            this.mSplash.animate().alpha(1.0f).setDuration(1000L);
            this.mSplash.findViewById(R.id.play_button_image).setAlpha(1.0f);
            this.mSplash.findViewById(R.id.play_button_text).setAlpha(0.0f);
            this.mSplash.findViewById(R.id.play_button).setEnabled(true);
            this.mSplash.findViewById(R.id.play_button).requestFocus();
        }
    }

    public void startPlaying() {
        this.mPlaying = true;
        this.t = 0.0f;
        this.mLastPipeTime = getGameTime() - PARAMS.OBSTACLE_PERIOD;
        hideSplash();
        realignPlayers();
        this.mTaps = 0;
        int size = this.mPlayers.size();
        MetricsLogger.histogram(getContext(), "egg_mland_players", size);
        for (int i = 0; i < size; i++) {
            Player player = this.mPlayers.get(i);
            player.setVisibility(0);
            player.reset();
            player.start();
            player.boost(-1.0f, -1.0f);
            player.unboost();
        }
    }

    public void stop() {
        if (this.mAnimating) {
            this.mAnim.cancel();
            this.mAnim = null;
            this.mAnimating = false;
            this.mPlaying = false;
            this.mTimeOfDay = irand(0, SKIES.length - 1);
            this.mScene = irand(0, 3);
            this.mFrozen = true;
            Iterator<Player> it = this.mPlayers.iterator();
            while (it.hasNext()) {
                it.next().die();
            }
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    MLand.this.mFrozen = false;
                }
            }, 250L);
        }
    }

    public static final float lerp(float f, float f2, float f3) {
        return ((f3 - f2) * f) + f2;
    }

    public static final float rlerp(float f, float f2, float f3) {
        return (f - f2) / (f3 - f2);
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

    public static final float frand(float f, float f2) {
        return lerp(frand(), f, f2);
    }

    public static final int irand(int i, int i2) {
        return Math.round(frand(i, i2));
    }

    public static int pick(int[] iArr) {
        return iArr[irand(0, iArr.length - 1)];
    }

    public void step(long j, long j2) {
        this.t = j / 1000.0f;
        this.dt = j2 / 1000.0f;
        if (DEBUG) {
            this.t *= 0.5f;
            this.dt *= 0.5f;
        }
        int childCount = getChildCount();
        int i = 0;
        while (i < childCount) {
            KeyEvent.Callback childAt = getChildAt(i);
            if (childAt instanceof GameView) {
                ((GameView) childAt).step(j, j2, this.t, this.dt);
            }
            i++;
        }
        if (this.mPlaying) {
            int i2 = 0;
            i = 0;
            while (i < this.mPlayers.size()) {
                Player player = getPlayer(i);
                if (player.mAlive) {
                    if (player.below(this.mHeight)) {
                        if (DEBUG_IDDQD) {
                            poke(i);
                            unpoke(i);
                        } else {
                            L("player %d hit the floor", Integer.valueOf(i));
                            thump(i, 80L);
                            player.die();
                        }
                    }
                    int size = this.mObstaclesInPlay.size();
                    int iMax = 0;
                    while (true) {
                        int i3 = size - 1;
                        if (size <= 0) {
                            break;
                        }
                        Obstacle obstacle = this.mObstaclesInPlay.get(i3);
                        if (obstacle.intersects(player) && !DEBUG_IDDQD) {
                            L("player hit an obstacle", new Object[0]);
                            thump(i, 80L);
                            player.die();
                        } else if (obstacle.cleared(player) && (obstacle instanceof Stem)) {
                            iMax = Math.max(iMax, ((Stem) obstacle).id);
                        }
                        size = i3;
                    }
                    if (iMax > player.mScore) {
                        player.addScore(1);
                    }
                }
                if (player.mAlive) {
                    i2++;
                }
                i++;
            }
            if (i2 == 0) {
                stop();
                MetricsLogger.count(getContext(), "egg_mland_taps", this.mTaps);
                this.mTaps = 0;
                int size2 = this.mPlayers.size();
                for (int i4 = 0; i4 < size2; i4++) {
                    MetricsLogger.histogram(getContext(), "egg_mland_score", this.mPlayers.get(i4).getScore());
                }
            }
        }
        while (true) {
            int i5 = i - 1;
            if (i <= 0) {
                break;
            }
            View childAt2 = getChildAt(i5);
            if (childAt2 instanceof Obstacle) {
                if (childAt2.getTranslationX() + childAt2.getWidth() < 0.0f) {
                    removeViewAt(i5);
                    this.mObstaclesInPlay.remove(childAt2);
                }
            } else if ((childAt2 instanceof Scenery) && childAt2.getTranslationX() + ((Scenery) childAt2).w < 0.0f) {
                childAt2.setTranslationX(getWidth());
            }
            i = i5;
        }
        if (this.mPlaying && this.t - this.mLastPipeTime > PARAMS.OBSTACLE_PERIOD) {
            this.mLastPipeTime = this.t;
            this.mCurrentPipeId++;
            int iFrand = ((int) (frand() * ((this.mHeight - (PARAMS.OBSTACLE_MIN * 2)) - PARAMS.OBSTACLE_GAP))) + PARAMS.OBSTACLE_MIN;
            int i6 = (PARAMS.OBSTACLE_WIDTH - PARAMS.OBSTACLE_STEM_WIDTH) / 2;
            int i7 = PARAMS.OBSTACLE_WIDTH / 2;
            int iIrand = irand(0, 250);
            Stem stem = new Stem(getContext(), iFrand - i7, false);
            addView(stem, new FrameLayout.LayoutParams(PARAMS.OBSTACLE_STEM_WIDTH, (int) stem.h, 51));
            stem.setTranslationX(this.mWidth + i6);
            float f = i7;
            stem.setTranslationY((-stem.h) - f);
            stem.setTranslationZ(PARAMS.OBSTACLE_Z * 0.75f);
            long j3 = iIrand;
            stem.animate().translationY(0.0f).setStartDelay(j3).setDuration(250L);
            this.mObstaclesInPlay.add(stem);
            Pop pop = new Pop(getContext(), PARAMS.OBSTACLE_WIDTH);
            addView(pop, new FrameLayout.LayoutParams(PARAMS.OBSTACLE_WIDTH, PARAMS.OBSTACLE_WIDTH, 51));
            pop.setTranslationX(this.mWidth);
            pop.setTranslationY(-PARAMS.OBSTACLE_WIDTH);
            pop.setTranslationZ(PARAMS.OBSTACLE_Z);
            pop.setScaleX(0.25f);
            pop.setScaleY(-0.25f);
            pop.animate().translationY(stem.h - i6).scaleX(1.0f).scaleY(-1.0f).setStartDelay(j3).setDuration(250L);
            this.mObstaclesInPlay.add(pop);
            int iIrand2 = irand(0, 250);
            Stem stem2 = new Stem(getContext(), ((this.mHeight - iFrand) - PARAMS.OBSTACLE_GAP) - i7, true);
            addView(stem2, new FrameLayout.LayoutParams(PARAMS.OBSTACLE_STEM_WIDTH, (int) stem2.h, 51));
            stem2.setTranslationX(this.mWidth + i6);
            stem2.setTranslationY(this.mHeight + i7);
            stem2.setTranslationZ(PARAMS.OBSTACLE_Z * 0.75f);
            long j4 = iIrand2;
            stem2.animate().translationY(this.mHeight - stem2.h).setStartDelay(j4).setDuration(400L);
            this.mObstaclesInPlay.add(stem2);
            Pop pop2 = new Pop(getContext(), PARAMS.OBSTACLE_WIDTH);
            addView(pop2, new FrameLayout.LayoutParams(PARAMS.OBSTACLE_WIDTH, PARAMS.OBSTACLE_WIDTH, 51));
            pop2.setTranslationX(this.mWidth);
            pop2.setTranslationY(this.mHeight);
            pop2.setTranslationZ(PARAMS.OBSTACLE_Z);
            pop2.setScaleX(0.25f);
            pop2.setScaleY(0.25f);
            pop2.animate().translationY((this.mHeight - stem2.h) - f).scaleX(1.0f).scaleY(1.0f).setStartDelay(j4).setDuration(400L);
            this.mObstaclesInPlay.add(pop2);
        }
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        L("touch: %s", motionEvent);
        int actionIndex = motionEvent.getActionIndex();
        float x = motionEvent.getX(actionIndex);
        float y = motionEvent.getY(actionIndex);
        int numPlayers = (int) (getNumPlayers() * (x / getWidth()));
        if (this.mFlipped) {
            numPlayers = (getNumPlayers() - 1) - numPlayers;
        }
        switch (motionEvent.getActionMasked()) {
            case 0:
            case 5:
                poke(numPlayers, x, y);
                return true;
            case 1:
            case 6:
                unpoke(numPlayers);
                return true;
            case 2:
            case 3:
            case 4:
            default:
                return false;
        }
    }

    @Override
    public boolean onTrackballEvent(MotionEvent motionEvent) {
        L("trackball: %s", motionEvent);
        switch (motionEvent.getAction()) {
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
    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        L("keyDown: %d", Integer.valueOf(i));
        if (i != 19 && i != 23 && i != 62 && i != 66 && i != 96) {
            return false;
        }
        poke(getControllerPlayer(keyEvent.getDeviceId()));
        return true;
    }

    @Override
    public boolean onKeyUp(int i, KeyEvent keyEvent) {
        L("keyDown: %d", Integer.valueOf(i));
        if (i != 19 && i != 23 && i != 62 && i != 66 && i != 96) {
            return false;
        }
        unpoke(getControllerPlayer(keyEvent.getDeviceId()));
        return true;
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent motionEvent) {
        L("generic: %s", motionEvent);
        return false;
    }

    private void poke(int i) {
        poke(i, -1.0f, -1.0f);
    }

    private void poke(int i, float f, float f2) {
        L("poke(%d)", Integer.valueOf(i));
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
        Player player = getPlayer(i);
        if (player == null) {
            return;
        }
        player.boost(f, f2);
        this.mTaps++;
        if (DEBUG) {
            player.dv *= 0.5f;
            player.animate().setDuration(400L);
        }
    }

    private void unpoke(int i) {
        Player player;
        L("unboost(%d)", Integer.valueOf(i));
        if (this.mFrozen || !this.mAnimating || !this.mPlaying || (player = getPlayer(i)) == null) {
            return;
        }
        player.unboost();
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (Player player : this.mPlayers) {
            if (player.mTouchX > 0.0f) {
                this.mTouchPaint.setColor(player.color & (-2130706433));
                this.mPlayerTracePaint.setColor(player.color & (-2130706433));
                float f = player.mTouchX;
                float f2 = player.mTouchY;
                canvas.drawCircle(f, f2, 100.0f, this.mTouchPaint);
                float x = player.getX() + player.getPivotX();
                float y = player.getY() + player.getPivotY();
                double dAtan2 = 1.5707964f - ((float) Math.atan2(x - f, y - f2));
                canvas.drawLine((float) (((double) f) + (Math.cos(dAtan2) * 100.0d)), (float) (((double) f2) + (100.0d * Math.sin(dAtan2))), x, y, this.mPlayerTracePaint);
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

        public static Player create(MLand mLand) {
            Player player = new Player(mLand.getContext());
            player.mLand = mLand;
            player.reset();
            player.setVisibility(4);
            mLand.addView(player, new FrameLayout.LayoutParams(MLand.PARAMS.PLAYER_SIZE, MLand.PARAMS.PLAYER_SIZE));
            return player;
        }

        private void setScore(int i) {
            this.mScore = i;
            if (this.mScoreField != null) {
                this.mScoreField.setText(MLand.DEBUG_IDDQD ? "??" : String.valueOf(i));
            }
        }

        public int getScore() {
            return this.mScore;
        }

        public void addScore(int i) {
            setScore(this.mScore + i);
        }

        public void setScoreField(TextView textView) {
            this.mScoreField = textView;
            if (textView != null) {
                setScore(this.mScore);
                this.mScoreField.getBackground().setColorFilter(this.color, PorterDuff.Mode.SRC_ATOP);
                this.mScoreField.setTextColor(MLand.luma(this.color) > 0.7f ? -16777216 : -1);
            }
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
            setBackgroundResource(R.drawable.f0android);
            getBackground().setTintMode(PorterDuff.Mode.SRC_ATOP);
            int[] iArr = this.sColors;
            int i = sNextColor;
            sNextColor = i + 1;
            this.color = iArr[i % this.sColors.length];
            getBackground().setTint(this.color);
            setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    int width = view.getWidth();
                    int height = view.getHeight();
                    int i2 = (int) (width * 0.3f);
                    int i3 = (int) (height * 0.2f);
                    outline.setRect(i2, i3, width - i2, height - i3);
                }
            });
        }

        public void prepareCheckIntersections() {
            int i = (MLand.PARAMS.PLAYER_SIZE - MLand.PARAMS.PLAYER_HIT_SIZE) / 2;
            int i2 = MLand.PARAMS.PLAYER_HIT_SIZE;
            int length = this.sHull.length / 2;
            for (int i3 = 0; i3 < length; i3++) {
                int i4 = i3 * 2;
                float f = i2;
                float f2 = i;
                this.corners[i4] = (this.sHull[i4] * f) + f2;
                int i5 = i4 + 1;
                this.corners[i5] = (f * this.sHull[i5]) + f2;
            }
            getMatrix().mapPoints(this.corners);
        }

        public boolean below(int i) {
            int length = this.corners.length / 2;
            for (int i2 = 0; i2 < length; i2++) {
                if (((int) this.corners[(i2 * 2) + 1]) >= i) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void step(long j, long j2, float f, float f2) {
            if (!this.mAlive) {
                setTranslationX(getTranslationX() - (MLand.PARAMS.TRANSLATION_PER_SEC * f2));
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
            float translationY = getTranslationY() + (this.dv * f2);
            if (translationY < 0.0f) {
                translationY = 0.0f;
            }
            setTranslationY(translationY);
            setRotation(90.0f + MLand.lerp(MLand.clamp(MLand.rlerp(this.dv, MLand.PARAMS.MAX_V, (-1) * MLand.PARAMS.MAX_V)), 90.0f, -90.0f));
            prepareCheckIntersections();
        }

        public void boost(float f, float f2) {
            this.mTouchX = f;
            this.mTouchY = f2;
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
            TextView textView = this.mScoreField;
        }

        public void start() {
            this.mAlive = true;
        }
    }

    private class Obstacle extends View implements GameView {
        public float h;
        public final Rect hitRect;

        public Obstacle(Context context, float f) {
            super(context);
            this.hitRect = new Rect();
            setBackgroundColor(-65536);
            this.h = f;
        }

        public boolean intersects(Player player) {
            int length = player.corners.length / 2;
            for (int i = 0; i < length; i++) {
                int i2 = i * 2;
                if (this.hitRect.contains((int) player.corners[i2], (int) player.corners[i2 + 1])) {
                    return true;
                }
            }
            return false;
        }

        public boolean cleared(Player player) {
            int length = player.corners.length / 2;
            for (int i = 0; i < length; i++) {
                if (this.hitRect.right >= ((int) player.corners[i * 2])) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public void step(long j, long j2, float f, float f2) {
            setTranslationX(getTranslationX() - (MLand.PARAMS.TRANSLATION_PER_SEC * f2));
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

        public Pop(Context context, float f) {
            super(context, f);
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
                    int width = (int) ((Pop.this.getWidth() * 1.0f) / 6.0f);
                    outline.setOval(width, width, Pop.this.getWidth() - width, Pop.this.getHeight() - width);
                }
            });
        }

        @Override
        public boolean intersects(Player player) {
            int length = player.corners.length / 2;
            for (int i = 0; i < length; i++) {
                int i2 = i * 2;
                if (Math.hypot(((int) player.corners[i2]) - this.cx, ((int) player.corners[i2 + 1]) - this.cy) <= this.r) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void step(long j, long j2, float f, float f2) {
            super.step(j, j2, f, f2);
            if (this.mRotate != 0) {
                setRotation(getRotation() + (f2 * 45.0f * this.mRotate));
            }
            this.cx = (this.hitRect.left + this.hitRect.right) / 2;
            this.cy = (this.hitRect.top + this.hitRect.bottom) / 2;
            this.r = getWidth() / 3;
        }

        @Override
        public void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (this.antenna != null) {
                this.antenna.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                this.antenna.draw(canvas);
            }
            if (this.eyes != null) {
                this.eyes.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                this.eyes.draw(canvas);
            }
            if (this.mouth != null) {
                this.mouth.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                this.mouth.draw(canvas);
            }
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

        public Stem(Context context, float f, boolean z) {
            super(context, f);
            this.mPaint = new Paint();
            this.mShadow = new Path();
            this.mGradient = new GradientDrawable();
            this.id = MLand.this.mCurrentPipeId;
            this.mDrawShadow = z;
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
        public void onDraw(Canvas canvas) {
            int width = canvas.getWidth();
            int height = canvas.getHeight();
            float f = width;
            this.mGradient.setGradientCenter(0.75f * f, 0.0f);
            int i = 0;
            this.mGradient.setBounds(0, 0, width, height);
            this.mGradient.draw(canvas);
            if (this.mJandystripe != null) {
                this.mJandystripe.reset();
                this.mJandystripe.moveTo(0.0f, f);
                this.mJandystripe.lineTo(f, 0.0f);
                this.mJandystripe.lineTo(f, 2 * width);
                this.mJandystripe.lineTo(0.0f, 3 * width);
                this.mJandystripe.close();
                while (i < height) {
                    canvas.drawPath(this.mJandystripe, this.mPaint2);
                    int i2 = 4 * width;
                    this.mJandystripe.offset(0.0f, i2);
                    i += i2;
                }
            }
            if (this.mDrawShadow) {
                this.mShadow.reset();
                this.mShadow.moveTo(0.0f, 0.0f);
                this.mShadow.lineTo(f, 0.0f);
                this.mShadow.lineTo(f, (MLand.PARAMS.OBSTACLE_WIDTH * 0.4f) + (1.5f * f));
                this.mShadow.lineTo(0.0f, MLand.PARAMS.OBSTACLE_WIDTH * 0.4f);
                this.mShadow.close();
                canvas.drawPath(this.mShadow, this.mPaint);
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
        public void step(long j, long j2, float f, float f2) {
            setTranslationX(getTranslationX() - ((MLand.PARAMS.TRANSLATION_PER_SEC * f2) * this.v));
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
