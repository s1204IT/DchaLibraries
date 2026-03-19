package com.android.server.display;

import android.R;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.hardware.display.BrightnessConfiguration;
import android.util.MathUtils;
import android.util.Pair;
import android.util.Slog;
import android.util.Spline;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;
import com.android.server.display.utils.Plog;
import java.io.PrintWriter;
import java.util.Arrays;

public abstract class BrightnessMappingStrategy {
    private static final boolean DEBUG = false;
    private static final float LUX_GRAD_SMOOTHING = 0.25f;
    private static final float MAX_GRAD = 1.0f;
    private static final String TAG = "BrightnessMappingStrategy";
    private static final Plog PLOG = Plog.createSystemPlog(TAG);

    public abstract void addUserDataPoint(float f, float f2);

    public abstract void clearUserDataPoints();

    public abstract float convertToNits(int i);

    public abstract void dump(PrintWriter printWriter);

    public abstract float getAutoBrightnessAdjustment();

    public abstract float getBrightness(float f);

    public abstract BrightnessConfiguration getDefaultConfig();

    public abstract boolean hasUserDataPoints();

    public abstract boolean isDefaultConfig();

    public abstract boolean setAutoBrightnessAdjustment(float f);

    public abstract boolean setBrightnessConfiguration(BrightnessConfiguration brightnessConfiguration);

    public static BrightnessMappingStrategy create(Resources resources) {
        float[] luxLevels = getLuxLevels(resources.getIntArray(R.array.config_angleAllowList));
        int[] intArray = resources.getIntArray(R.array.config_ambientThresholdsOfPeakRefreshRate);
        float[] floatArray = getFloatArray(resources.obtainTypedArray(R.array.config_ambientDarkeningThresholds));
        float fraction = resources.getFraction(R.fraction.config_autoBrightnessAdjustmentMaxGamma, 1, 1);
        float[] floatArray2 = getFloatArray(resources.obtainTypedArray(R.array.config_defaultImperceptibleKillingExemptionProcStates));
        int[] intArray2 = resources.getIntArray(R.array.config_defaultImperceptibleKillingExemptionPkgs);
        if (isValidMapping(floatArray2, intArray2) && isValidMapping(luxLevels, floatArray)) {
            int integer = resources.getInteger(R.integer.config_dozeWakeLockScreenDebounce);
            int integer2 = resources.getInteger(R.integer.config_downloadDataDirSize);
            if (intArray2[0] > integer || intArray2[intArray2.length - 1] < integer2) {
                Slog.w(TAG, "Screen brightness mapping does not cover whole range of available backlight values, autobrightness functionality may be impaired.");
            }
            BrightnessConfiguration.Builder builder = new BrightnessConfiguration.Builder();
            builder.setCurve(luxLevels, floatArray);
            return new PhysicalMappingStrategy(builder.build(), floatArray2, intArray2, fraction);
        }
        if (isValidMapping(luxLevels, intArray)) {
            return new SimpleMappingStrategy(luxLevels, intArray, fraction);
        }
        return null;
    }

    private static float[] getLuxLevels(int[] iArr) {
        float[] fArr = new float[iArr.length + 1];
        int i = 0;
        while (i < iArr.length) {
            int i2 = i + 1;
            fArr[i2] = iArr[i];
            i = i2;
        }
        return fArr;
    }

    private static float[] getFloatArray(TypedArray typedArray) {
        int length = typedArray.length();
        float[] fArr = new float[length];
        for (int i = 0; i < length; i++) {
            fArr[i] = typedArray.getFloat(i, -1.0f);
        }
        typedArray.recycle();
        return fArr;
    }

    private static boolean isValidMapping(float[] fArr, float[] fArr2) {
        if (fArr == null || fArr2 == null || fArr.length == 0 || fArr2.length == 0 || fArr.length != fArr2.length) {
            return false;
        }
        int length = fArr.length;
        float f = fArr[0];
        float f2 = fArr2[0];
        if (f < 0.0f || f2 < 0.0f || Float.isNaN(f) || Float.isNaN(f2)) {
            return false;
        }
        float f3 = f2;
        float f4 = f;
        for (int i = 1; i < length; i++) {
            if (f4 >= fArr[i] || f3 > fArr2[i] || Float.isNaN(fArr[i]) || Float.isNaN(fArr2[i])) {
                return false;
            }
            f4 = fArr[i];
            f3 = fArr2[i];
        }
        return true;
    }

    private static boolean isValidMapping(float[] fArr, int[] iArr) {
        if (fArr == null || iArr == null || fArr.length == 0 || iArr.length == 0 || fArr.length != iArr.length) {
            return false;
        }
        int length = fArr.length;
        float f = fArr[0];
        int i = iArr[0];
        if (f < 0.0f || i < 0 || Float.isNaN(f)) {
            return false;
        }
        int i2 = i;
        float f2 = f;
        for (int i3 = 1; i3 < length; i3++) {
            if (f2 >= fArr[i3] || i2 > iArr[i3] || Float.isNaN(fArr[i3])) {
                return false;
            }
            f2 = fArr[i3];
            i2 = iArr[i3];
        }
        return true;
    }

    private static float normalizeAbsoluteBrightness(int i) {
        return MathUtils.constrain(i, 0, 255) / 255.0f;
    }

    private static Pair<float[], float[]> insertControlPoint(float[] fArr, float[] fArr2, float f, float f2) {
        float[] fArrCopyOf;
        float[] fArrCopyOf2;
        int iFindInsertionPoint = findInsertionPoint(fArr, f);
        if (iFindInsertionPoint == fArr.length) {
            fArrCopyOf2 = Arrays.copyOf(fArr, fArr.length + 1);
            fArrCopyOf = Arrays.copyOf(fArr2, fArr2.length + 1);
            fArrCopyOf2[iFindInsertionPoint] = f;
            fArrCopyOf[iFindInsertionPoint] = f2;
        } else if (fArr[iFindInsertionPoint] == f) {
            fArrCopyOf2 = Arrays.copyOf(fArr, fArr.length);
            fArrCopyOf = Arrays.copyOf(fArr2, fArr2.length);
            fArrCopyOf[iFindInsertionPoint] = f2;
        } else {
            float[] fArrCopyOf3 = Arrays.copyOf(fArr, fArr.length + 1);
            int i = iFindInsertionPoint + 1;
            System.arraycopy(fArrCopyOf3, iFindInsertionPoint, fArrCopyOf3, i, fArr.length - iFindInsertionPoint);
            fArrCopyOf3[iFindInsertionPoint] = f;
            float[] fArrCopyOf4 = Arrays.copyOf(fArr2, fArr2.length + 1);
            System.arraycopy(fArrCopyOf4, iFindInsertionPoint, fArrCopyOf4, i, fArr2.length - iFindInsertionPoint);
            fArrCopyOf4[iFindInsertionPoint] = f2;
            fArrCopyOf = fArrCopyOf4;
            fArrCopyOf2 = fArrCopyOf3;
        }
        smoothCurve(fArrCopyOf2, fArrCopyOf, iFindInsertionPoint);
        return Pair.create(fArrCopyOf2, fArrCopyOf);
    }

    private static int findInsertionPoint(float[] fArr, float f) {
        for (int i = 0; i < fArr.length; i++) {
            if (f <= fArr[i]) {
                return i;
            }
        }
        return fArr.length;
    }

    private static void smoothCurve(float[] fArr, float[] fArr2, int i) {
        float f = fArr[i];
        float fConstrain = fArr2[i];
        int i2 = i + 1;
        while (i2 < fArr.length) {
            float f2 = fArr[i2];
            float f3 = fArr2[i2];
            fConstrain = MathUtils.constrain(f3, fConstrain, permissibleRatio(f2, f) * fConstrain);
            if (fConstrain == f3) {
                break;
            }
            fArr2[i2] = fConstrain;
            i2++;
            f = f2;
        }
        float f4 = fArr[i];
        float fConstrain2 = fArr2[i];
        int i3 = i - 1;
        while (i3 >= 0) {
            float f5 = fArr[i3];
            float f6 = fArr2[i3];
            fConstrain2 = MathUtils.constrain(f6, permissibleRatio(f5, f4) * fConstrain2, fConstrain2);
            if (fConstrain2 != f6) {
                fArr2[i3] = fConstrain2;
                i3--;
                f4 = f5;
            } else {
                return;
            }
        }
    }

    private static float permissibleRatio(float f, float f2) {
        return MathUtils.exp(1.0f * (MathUtils.log(f + LUX_GRAD_SMOOTHING) - MathUtils.log(f2 + LUX_GRAD_SMOOTHING)));
    }

    private static float inferAutoBrightnessAdjustment(float f, float f2, float f3) {
        float fLog;
        if (f3 <= 0.1f || f3 >= 0.9f) {
            fLog = f2 - f3;
        } else {
            fLog = f2 == 0.0f ? -1.0f : f2 == 1.0f ? 1.0f : (-MathUtils.log(MathUtils.log(f2) / MathUtils.log(f3))) / MathUtils.log(f);
        }
        return MathUtils.constrain(fLog, -1.0f, 1.0f);
    }

    private static Pair<float[], float[]> getAdjustedCurve(float[] fArr, float[] fArr2, float f, float f2, float f3, float f4) {
        float[] fArrCopyOf = Arrays.copyOf(fArr2, fArr2.length);
        float fPow = MathUtils.pow(f4, -MathUtils.constrain(f3, -1.0f, 1.0f));
        if (fPow != 1.0f) {
            for (int i = 0; i < fArrCopyOf.length; i++) {
                fArrCopyOf[i] = MathUtils.pow(fArrCopyOf[i], fPow);
            }
        }
        if (f != -1.0f) {
            Pair<float[], float[]> pairInsertControlPoint = insertControlPoint(fArr, fArrCopyOf, f, f2);
            float[] fArr3 = (float[]) pairInsertControlPoint.first;
            fArrCopyOf = (float[]) pairInsertControlPoint.second;
            fArr = fArr3;
        }
        return Pair.create(fArr, fArrCopyOf);
    }

    private static class SimpleMappingStrategy extends BrightnessMappingStrategy {
        private float mAutoBrightnessAdjustment;
        private final float[] mBrightness;
        private final float[] mLux;
        private float mMaxGamma;
        private Spline mSpline;
        private float mUserBrightness;
        private float mUserLux;

        public SimpleMappingStrategy(float[] fArr, int[] iArr, float f) {
            Preconditions.checkArgument((fArr.length == 0 || iArr.length == 0) ? false : true, "Lux and brightness arrays must not be empty!");
            Preconditions.checkArgument(fArr.length == iArr.length, "Lux and brightness arrays must be the same length!");
            Preconditions.checkArrayElementsInRange(fArr, 0.0f, Float.MAX_VALUE, "lux");
            Preconditions.checkArrayElementsInRange(iArr, 0, Integer.MAX_VALUE, "brightness");
            int length = iArr.length;
            this.mLux = new float[length];
            this.mBrightness = new float[length];
            for (int i = 0; i < length; i++) {
                this.mLux[i] = fArr[i];
                this.mBrightness[i] = BrightnessMappingStrategy.normalizeAbsoluteBrightness(iArr[i]);
            }
            this.mMaxGamma = f;
            this.mAutoBrightnessAdjustment = 0.0f;
            this.mUserLux = -1.0f;
            this.mUserBrightness = -1.0f;
            computeSpline();
        }

        @Override
        public boolean setBrightnessConfiguration(BrightnessConfiguration brightnessConfiguration) {
            return false;
        }

        @Override
        public float getBrightness(float f) {
            return this.mSpline.interpolate(f);
        }

        @Override
        public float getAutoBrightnessAdjustment() {
            return this.mAutoBrightnessAdjustment;
        }

        @Override
        public boolean setAutoBrightnessAdjustment(float f) {
            float fConstrain = MathUtils.constrain(f, -1.0f, 1.0f);
            if (fConstrain == this.mAutoBrightnessAdjustment) {
                return false;
            }
            this.mAutoBrightnessAdjustment = fConstrain;
            computeSpline();
            return true;
        }

        @Override
        public float convertToNits(int i) {
            return -1.0f;
        }

        @Override
        public void addUserDataPoint(float f, float f2) {
            this.mAutoBrightnessAdjustment = BrightnessMappingStrategy.inferAutoBrightnessAdjustment(this.mMaxGamma, f2, getUnadjustedBrightness(f));
            this.mUserLux = f;
            this.mUserBrightness = f2;
            computeSpline();
        }

        @Override
        public void clearUserDataPoints() {
            if (this.mUserLux != -1.0f) {
                this.mAutoBrightnessAdjustment = 0.0f;
                this.mUserLux = -1.0f;
                this.mUserBrightness = -1.0f;
                computeSpline();
            }
        }

        @Override
        public boolean hasUserDataPoints() {
            return this.mUserLux != -1.0f;
        }

        @Override
        public boolean isDefaultConfig() {
            return true;
        }

        @Override
        public BrightnessConfiguration getDefaultConfig() {
            return null;
        }

        @Override
        public void dump(PrintWriter printWriter) {
            printWriter.println("SimpleMappingStrategy");
            printWriter.println("  mSpline=" + this.mSpline);
            printWriter.println("  mMaxGamma=" + this.mMaxGamma);
            printWriter.println("  mAutoBrightnessAdjustment=" + this.mAutoBrightnessAdjustment);
            printWriter.println("  mUserLux=" + this.mUserLux);
            printWriter.println("  mUserBrightness=" + this.mUserBrightness);
        }

        private void computeSpline() {
            Pair adjustedCurve = BrightnessMappingStrategy.getAdjustedCurve(this.mLux, this.mBrightness, this.mUserLux, this.mUserBrightness, this.mAutoBrightnessAdjustment, this.mMaxGamma);
            this.mSpline = Spline.createSpline((float[]) adjustedCurve.first, (float[]) adjustedCurve.second);
        }

        private float getUnadjustedBrightness(float f) {
            return Spline.createSpline(this.mLux, this.mBrightness).interpolate(f);
        }
    }

    @VisibleForTesting
    static class PhysicalMappingStrategy extends BrightnessMappingStrategy {
        private float mAutoBrightnessAdjustment;
        private Spline mBacklightToNitsSpline;
        private Spline mBrightnessSpline;
        private BrightnessConfiguration mConfig;
        private final BrightnessConfiguration mDefaultConfig;
        private float mMaxGamma;
        private final Spline mNitsToBacklightSpline;
        private float mUserBrightness;
        private float mUserLux;

        public PhysicalMappingStrategy(BrightnessConfiguration brightnessConfiguration, float[] fArr, int[] iArr, float f) {
            Preconditions.checkArgument((fArr.length == 0 || iArr.length == 0) ? false : true, "Nits and backlight arrays must not be empty!");
            Preconditions.checkArgument(fArr.length == iArr.length, "Nits and backlight arrays must be the same length!");
            Preconditions.checkNotNull(brightnessConfiguration);
            Preconditions.checkArrayElementsInRange(fArr, 0.0f, Float.MAX_VALUE, "nits");
            Preconditions.checkArrayElementsInRange(iArr, 0, 255, "backlight");
            this.mMaxGamma = f;
            this.mAutoBrightnessAdjustment = 0.0f;
            this.mUserLux = -1.0f;
            this.mUserBrightness = -1.0f;
            int length = fArr.length;
            float[] fArr2 = new float[length];
            for (int i = 0; i < length; i++) {
                fArr2[i] = BrightnessMappingStrategy.normalizeAbsoluteBrightness(iArr[i]);
            }
            this.mNitsToBacklightSpline = Spline.createSpline(fArr, fArr2);
            this.mBacklightToNitsSpline = Spline.createSpline(fArr2, fArr);
            this.mDefaultConfig = brightnessConfiguration;
            this.mConfig = brightnessConfiguration;
            computeSpline();
        }

        @Override
        public boolean setBrightnessConfiguration(BrightnessConfiguration brightnessConfiguration) {
            if (brightnessConfiguration == null) {
                brightnessConfiguration = this.mDefaultConfig;
            }
            if (brightnessConfiguration.equals(this.mConfig)) {
                return false;
            }
            this.mConfig = brightnessConfiguration;
            computeSpline();
            return true;
        }

        @Override
        public float getBrightness(float f) {
            return this.mNitsToBacklightSpline.interpolate(this.mBrightnessSpline.interpolate(f));
        }

        @Override
        public float getAutoBrightnessAdjustment() {
            return this.mAutoBrightnessAdjustment;
        }

        @Override
        public boolean setAutoBrightnessAdjustment(float f) {
            float fConstrain = MathUtils.constrain(f, -1.0f, 1.0f);
            if (fConstrain == this.mAutoBrightnessAdjustment) {
                return false;
            }
            this.mAutoBrightnessAdjustment = fConstrain;
            computeSpline();
            return true;
        }

        @Override
        public float convertToNits(int i) {
            return this.mBacklightToNitsSpline.interpolate(BrightnessMappingStrategy.normalizeAbsoluteBrightness(i));
        }

        @Override
        public void addUserDataPoint(float f, float f2) {
            this.mAutoBrightnessAdjustment = BrightnessMappingStrategy.inferAutoBrightnessAdjustment(this.mMaxGamma, f2, getUnadjustedBrightness(f));
            this.mUserLux = f;
            this.mUserBrightness = f2;
            computeSpline();
        }

        @Override
        public void clearUserDataPoints() {
            if (this.mUserLux != -1.0f) {
                this.mAutoBrightnessAdjustment = 0.0f;
                this.mUserLux = -1.0f;
                this.mUserBrightness = -1.0f;
                computeSpline();
            }
        }

        @Override
        public boolean hasUserDataPoints() {
            return this.mUserLux != -1.0f;
        }

        @Override
        public boolean isDefaultConfig() {
            return this.mDefaultConfig.equals(this.mConfig);
        }

        @Override
        public BrightnessConfiguration getDefaultConfig() {
            return this.mDefaultConfig;
        }

        @Override
        public void dump(PrintWriter printWriter) {
            printWriter.println("PhysicalMappingStrategy");
            printWriter.println("  mConfig=" + this.mConfig);
            printWriter.println("  mBrightnessSpline=" + this.mBrightnessSpline);
            printWriter.println("  mNitsToBacklightSpline=" + this.mNitsToBacklightSpline);
            printWriter.println("  mMaxGamma=" + this.mMaxGamma);
            printWriter.println("  mAutoBrightnessAdjustment=" + this.mAutoBrightnessAdjustment);
            printWriter.println("  mUserLux=" + this.mUserLux);
            printWriter.println("  mUserBrightness=" + this.mUserBrightness);
        }

        private void computeSpline() {
            Pair curve = this.mConfig.getCurve();
            float[] fArr = (float[]) curve.first;
            float[] fArr2 = (float[]) curve.second;
            float[] fArr3 = new float[fArr2.length];
            for (int i = 0; i < fArr3.length; i++) {
                fArr3[i] = this.mNitsToBacklightSpline.interpolate(fArr2[i]);
            }
            Pair adjustedCurve = BrightnessMappingStrategy.getAdjustedCurve(fArr, fArr3, this.mUserLux, this.mUserBrightness, this.mAutoBrightnessAdjustment, this.mMaxGamma);
            float[] fArr4 = (float[]) adjustedCurve.first;
            float[] fArr5 = (float[]) adjustedCurve.second;
            float[] fArr6 = new float[fArr5.length];
            for (int i2 = 0; i2 < fArr6.length; i2++) {
                fArr6[i2] = this.mBacklightToNitsSpline.interpolate(fArr5[i2]);
            }
            this.mBrightnessSpline = Spline.createSpline(fArr4, fArr6);
        }

        private float getUnadjustedBrightness(float f) {
            Pair curve = this.mConfig.getCurve();
            return this.mNitsToBacklightSpline.interpolate(Spline.createSpline((float[]) curve.first, (float[]) curve.second).interpolate(f));
        }
    }
}
