package com.android.commands.monkey;

import android.content.ComponentName;
import android.graphics.PointF;
import android.hardware.display.DisplayManagerGlobal;
import android.os.SystemClock;
import android.view.Display;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import java.util.List;
import java.util.Random;

public class MonkeySourceRandom implements MonkeyEventSource {
    public static final int FACTORZ_COUNT = 12;
    public static final int FACTOR_ANYTHING = 11;
    public static final int FACTOR_APPSWITCH = 9;
    public static final int FACTOR_FLIP = 10;
    public static final int FACTOR_MAJORNAV = 7;
    public static final int FACTOR_MOTION = 1;
    public static final int FACTOR_NAV = 6;
    public static final int FACTOR_PERMISSION = 5;
    public static final int FACTOR_PINCHZOOM = 2;
    public static final int FACTOR_ROTATION = 4;
    public static final int FACTOR_SYSOPS = 8;
    public static final int FACTOR_TOUCH = 0;
    public static final int FACTOR_TRACKBALL = 3;
    private static final int GESTURE_DRAG = 1;
    private static final int GESTURE_PINCH_OR_ZOOM = 2;
    private static final int GESTURE_TAP = 0;
    private static final int[] SCREEN_ROTATION_DEGREES;
    private List<ComponentName> mMainApps;
    private MonkeyPermissionUtil mPermissionUtil;
    private MonkeyEventQueue mQ;
    private Random mRandom;
    private static final int[] NAV_KEYS = {19, 20, 21, 22};
    private static final int[] MAJOR_NAV_KEYS = {82, 23};
    private static final int[] SYS_KEYS = {3, 4, 5, 6, 24, 25, 164, 91};
    private static final boolean[] PHYSICAL_KEY_EXISTS = new boolean[KeyEvent.getMaxKeyCode() + 1];
    private float[] mFactors = new float[12];
    private int mEventCount = 0;
    private int mVerbose = 0;
    private long mThrottle = 0;
    private boolean mKeyboardOpen = false;

    static {
        for (int i = 0; i < PHYSICAL_KEY_EXISTS.length; i++) {
            PHYSICAL_KEY_EXISTS[i] = true;
        }
        for (int i2 = 0; i2 < SYS_KEYS.length; i2++) {
            PHYSICAL_KEY_EXISTS[SYS_KEYS[i2]] = KeyCharacterMap.deviceHasKey(SYS_KEYS[i2]);
        }
        SCREEN_ROTATION_DEGREES = new int[]{0, 1, 2, 3};
    }

    public static String getKeyName(int i) {
        return KeyEvent.keyCodeToString(i);
    }

    public static int getKeyCode(String str) {
        return KeyEvent.keyCodeFromString(str);
    }

    public MonkeySourceRandom(Random random, List<ComponentName> list, long j, boolean z, boolean z2) {
        this.mFactors[0] = 15.0f;
        this.mFactors[1] = 10.0f;
        this.mFactors[3] = 15.0f;
        this.mFactors[4] = 0.0f;
        this.mFactors[6] = 25.0f;
        this.mFactors[7] = 15.0f;
        this.mFactors[8] = 2.0f;
        this.mFactors[9] = 2.0f;
        this.mFactors[10] = 1.0f;
        this.mFactors[5] = 0.0f;
        this.mFactors[11] = 13.0f;
        this.mFactors[2] = 2.0f;
        this.mRandom = random;
        this.mMainApps = list;
        this.mQ = new MonkeyEventQueue(random, j, z);
        this.mPermissionUtil = new MonkeyPermissionUtil();
        this.mPermissionUtil.setTargetSystemPackages(z2);
    }

    private boolean adjustEventFactors() {
        float f = 0.0f;
        float f2 = 0.0f;
        float f3 = 0.0f;
        int i = 0;
        for (int i2 = 0; i2 < 12; i2++) {
            if (this.mFactors[i2] <= 0.0f) {
                f2 -= this.mFactors[i2];
            } else {
                f3 += this.mFactors[i2];
                i++;
            }
        }
        if (f2 > 100.0f) {
            Logger.err.println("** Event weights > 100%");
            return false;
        }
        if (i == 0 && (f2 < 99.9f || f2 > 100.1f)) {
            Logger.err.println("** Event weights != 100%");
            return false;
        }
        float f4 = (100.0f - f2) / f3;
        for (int i3 = 0; i3 < 12; i3++) {
            if (this.mFactors[i3] <= 0.0f) {
                this.mFactors[i3] = -this.mFactors[i3];
            } else {
                float[] fArr = this.mFactors;
                fArr[i3] = fArr[i3] * f4;
            }
        }
        if (this.mVerbose > 0) {
            Logger.out.println("// Event percentages:");
            for (int i4 = 0; i4 < 12; i4++) {
                Logger.out.println("//   " + i4 + ": " + this.mFactors[i4] + "%");
            }
        }
        if (!validateKeys()) {
            return false;
        }
        for (int i5 = 0; i5 < 12; i5++) {
            f += this.mFactors[i5] / 100.0f;
            this.mFactors[i5] = f;
        }
        return true;
    }

    private static boolean validateKeyCategory(String str, int[] iArr, float f) {
        if (f < 0.1f) {
            return true;
        }
        for (int i : iArr) {
            if (PHYSICAL_KEY_EXISTS[i]) {
                return true;
            }
        }
        Logger.err.println("** " + str + " has no physical keys but with factor " + f + "%.");
        return false;
    }

    private boolean validateKeys() {
        return validateKeyCategory("NAV_KEYS", NAV_KEYS, this.mFactors[6]) && validateKeyCategory("MAJOR_NAV_KEYS", MAJOR_NAV_KEYS, this.mFactors[7]) && validateKeyCategory("SYS_KEYS", SYS_KEYS, this.mFactors[8]);
    }

    public void setFactors(float[] fArr) {
        int length = fArr.length < 12 ? fArr.length : 12;
        for (int i = 0; i < length; i++) {
            this.mFactors[i] = fArr[i];
        }
    }

    public void setFactors(int i, float f) {
        this.mFactors[i] = f;
    }

    private void generatePointerEvent(Random random, int i) {
        int i2 = 0;
        Display realDisplay = DisplayManagerGlobal.getInstance().getRealDisplay(0);
        PointF pointFRandomPoint = randomPoint(random, realDisplay);
        PointF pointFRandomVector = randomVector(random);
        long jUptimeMillis = SystemClock.uptimeMillis();
        this.mQ.addLast((MonkeyEvent) new MonkeyTouchEvent(0).setDownTime(jUptimeMillis).addPointer(0, pointFRandomPoint.x, pointFRandomPoint.y).setIntermediateNote(false));
        int i3 = 1;
        if (i != 1) {
            if (i == 2) {
                PointF pointFRandomPoint2 = randomPoint(random, realDisplay);
                PointF pointFRandomVector2 = randomVector(random);
                randomWalk(random, realDisplay, pointFRandomPoint, pointFRandomVector);
                this.mQ.addLast((MonkeyEvent) new MonkeyTouchEvent(261).setDownTime(jUptimeMillis).addPointer(0, pointFRandomPoint.x, pointFRandomPoint.y).addPointer(1, pointFRandomPoint2.x, pointFRandomPoint2.y).setIntermediateNote(true));
                int iNextInt = random.nextInt(10);
                int i4 = 0;
                while (i4 < iNextInt) {
                    randomWalk(random, realDisplay, pointFRandomPoint, pointFRandomVector);
                    randomWalk(random, realDisplay, pointFRandomPoint2, pointFRandomVector2);
                    this.mQ.addLast((MonkeyEvent) new MonkeyTouchEvent(2).setDownTime(jUptimeMillis).addPointer(i2, pointFRandomPoint.x, pointFRandomPoint.y).addPointer(1, pointFRandomPoint2.x, pointFRandomPoint2.y).setIntermediateNote(true));
                    i4++;
                    i2 = 0;
                }
                randomWalk(random, realDisplay, pointFRandomPoint, pointFRandomVector);
                randomWalk(random, realDisplay, pointFRandomPoint2, pointFRandomVector2);
                i3 = 1;
                this.mQ.addLast((MonkeyEvent) new MonkeyTouchEvent(262).setDownTime(jUptimeMillis).addPointer(0, pointFRandomPoint.x, pointFRandomPoint.y).addPointer(1, pointFRandomPoint2.x, pointFRandomPoint2.y).setIntermediateNote(true));
            }
        } else {
            int iNextInt2 = random.nextInt(10);
            for (int i5 = 0; i5 < iNextInt2; i5++) {
                randomWalk(random, realDisplay, pointFRandomPoint, pointFRandomVector);
                this.mQ.addLast((MonkeyEvent) new MonkeyTouchEvent(2).setDownTime(jUptimeMillis).addPointer(0, pointFRandomPoint.x, pointFRandomPoint.y).setIntermediateNote(true));
            }
        }
        randomWalk(random, realDisplay, pointFRandomPoint, pointFRandomVector);
        this.mQ.addLast((MonkeyEvent) new MonkeyTouchEvent(i3).setDownTime(jUptimeMillis).addPointer(0, pointFRandomPoint.x, pointFRandomPoint.y).setIntermediateNote(false));
    }

    private PointF randomPoint(Random random, Display display) {
        return new PointF(random.nextInt(display.getWidth()), random.nextInt(display.getHeight()));
    }

    private PointF randomVector(Random random) {
        return new PointF((random.nextFloat() - 0.5f) * 50.0f, (random.nextFloat() - 0.5f) * 50.0f);
    }

    private void randomWalk(Random random, Display display, PointF pointF, PointF pointF2) {
        pointF.x = Math.max(Math.min(pointF.x + (random.nextFloat() * pointF2.x), display.getWidth()), 0.0f);
        pointF.y = Math.max(Math.min(pointF.y + (random.nextFloat() * pointF2.y), display.getHeight()), 0.0f);
    }

    private void generateTrackballEvent(Random random) {
        int i = 0;
        while (true) {
            boolean z = true;
            if (i >= 10) {
                break;
            }
            int iNextInt = random.nextInt(10) - 5;
            int iNextInt2 = random.nextInt(10) - 5;
            MonkeyEventQueue monkeyEventQueue = this.mQ;
            MonkeyMotionEvent monkeyMotionEventAddPointer = new MonkeyTrackballEvent(2).addPointer(0, iNextInt, iNextInt2);
            if (i <= 0) {
                z = false;
            }
            monkeyEventQueue.addLast((MonkeyEvent) monkeyMotionEventAddPointer.setIntermediateNote(z));
            i++;
        }
        if (random.nextInt(10) == 0) {
            long jUptimeMillis = SystemClock.uptimeMillis();
            this.mQ.addLast((MonkeyEvent) new MonkeyTrackballEvent(0).setDownTime(jUptimeMillis).addPointer(0, 0.0f, 0.0f).setIntermediateNote(true));
            this.mQ.addLast((MonkeyEvent) new MonkeyTrackballEvent(1).setDownTime(jUptimeMillis).addPointer(0, 0.0f, 0.0f).setIntermediateNote(false));
        }
    }

    private void generateRotationEvent(Random random) {
        this.mQ.addLast((MonkeyEvent) new MonkeyRotationEvent(SCREEN_ROTATION_DEGREES[random.nextInt(SCREEN_ROTATION_DEGREES.length)], random.nextBoolean()));
    }

    private void generateEvents() {
        int iNextInt;
        float fNextFloat = this.mRandom.nextFloat();
        if (fNextFloat < this.mFactors[0]) {
            generatePointerEvent(this.mRandom, 0);
            return;
        }
        if (fNextFloat < this.mFactors[1]) {
            generatePointerEvent(this.mRandom, 1);
            return;
        }
        if (fNextFloat < this.mFactors[2]) {
            generatePointerEvent(this.mRandom, 2);
            return;
        }
        if (fNextFloat < this.mFactors[3]) {
            generateTrackballEvent(this.mRandom);
            return;
        }
        if (fNextFloat < this.mFactors[4]) {
            generateRotationEvent(this.mRandom);
            return;
        }
        if (fNextFloat < this.mFactors[5]) {
            this.mQ.add(this.mPermissionUtil.generateRandomPermissionEvent(this.mRandom));
            return;
        }
        while (true) {
            if (fNextFloat < this.mFactors[6]) {
                iNextInt = NAV_KEYS[this.mRandom.nextInt(NAV_KEYS.length)];
            } else if (fNextFloat < this.mFactors[7]) {
                iNextInt = MAJOR_NAV_KEYS[this.mRandom.nextInt(MAJOR_NAV_KEYS.length)];
            } else if (fNextFloat < this.mFactors[8]) {
                iNextInt = SYS_KEYS[this.mRandom.nextInt(SYS_KEYS.length)];
            } else if (fNextFloat < this.mFactors[9]) {
                this.mQ.addLast((MonkeyEvent) new MonkeyActivityEvent(this.mMainApps.get(this.mRandom.nextInt(this.mMainApps.size()))));
                return;
            } else {
                if (fNextFloat < this.mFactors[10]) {
                    MonkeyFlipEvent monkeyFlipEvent = new MonkeyFlipEvent(this.mKeyboardOpen);
                    this.mKeyboardOpen = !this.mKeyboardOpen;
                    this.mQ.addLast((MonkeyEvent) monkeyFlipEvent);
                    return;
                }
                iNextInt = this.mRandom.nextInt(KeyEvent.getMaxKeyCode() - 1) + 1;
            }
            if (iNextInt != 26 && iNextInt != 6 && iNextInt != 223 && iNextInt != 276 && PHYSICAL_KEY_EXISTS[iNextInt]) {
                this.mQ.addLast((MonkeyEvent) new MonkeyKeyEvent(0, iNextInt));
                this.mQ.addLast((MonkeyEvent) new MonkeyKeyEvent(1, iNextInt));
                return;
            }
        }
    }

    @Override
    public boolean validate() {
        boolean zPopulatePermissionsMapping = true;
        if (this.mFactors[5] != 0.0f && ((zPopulatePermissionsMapping = true & this.mPermissionUtil.populatePermissionsMapping())) && this.mVerbose >= 2) {
            this.mPermissionUtil.dump();
        }
        return adjustEventFactors() & zPopulatePermissionsMapping;
    }

    @Override
    public void setVerbose(int i) {
        this.mVerbose = i;
    }

    public void generateActivity() {
        this.mQ.addLast((MonkeyEvent) new MonkeyActivityEvent(this.mMainApps.get(this.mRandom.nextInt(this.mMainApps.size()))));
    }

    @Override
    public MonkeyEvent getNextEvent() {
        if (this.mQ.isEmpty()) {
            generateEvents();
        }
        this.mEventCount++;
        MonkeyEvent first = this.mQ.getFirst();
        this.mQ.removeFirst();
        return first;
    }
}
