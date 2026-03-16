package android.support.v7.graphics;

import android.graphics.Bitmap;
import android.graphics.Color;
import java.util.Arrays;
import java.util.List;

public final class Palette {
    private final List<Swatch> mSwatches;
    private final int mHighestPopulation = findMaxPopulation();
    private Swatch mVibrantSwatch = findColor(0.5f, 0.3f, 0.7f, 1.0f, 0.35f, 1.0f);
    private Swatch mLightVibrantSwatch = findColor(0.74f, 0.55f, 1.0f, 1.0f, 0.35f, 1.0f);
    private Swatch mDarkVibrantSwatch = findColor(0.26f, 0.0f, 0.45f, 1.0f, 0.35f, 1.0f);
    private Swatch mMutedSwatch = findColor(0.5f, 0.3f, 0.7f, 0.3f, 0.0f, 0.4f);
    private Swatch mLightMutedColor = findColor(0.74f, 0.55f, 1.0f, 0.3f, 0.0f, 0.4f);
    private Swatch mDarkMutedSwatch = findColor(0.26f, 0.0f, 0.45f, 0.3f, 0.0f, 0.4f);

    public static Palette generate(Bitmap bitmap, int numColors) {
        checkBitmapParam(bitmap);
        checkNumberColorsParam(numColors);
        Bitmap scaledBitmap = scaleBitmapDown(bitmap);
        ColorCutQuantizer quantizer = ColorCutQuantizer.fromBitmap(scaledBitmap, numColors);
        if (scaledBitmap != bitmap) {
            scaledBitmap.recycle();
        }
        return new Palette(quantizer.getQuantizedColors());
    }

    private Palette(List<Swatch> swatches) {
        this.mSwatches = swatches;
        generateEmptySwatches();
    }

    public Swatch getVibrantSwatch() {
        return this.mVibrantSwatch;
    }

    private boolean isAlreadySelected(Swatch swatch) {
        return this.mVibrantSwatch == swatch || this.mDarkVibrantSwatch == swatch || this.mLightVibrantSwatch == swatch || this.mMutedSwatch == swatch || this.mDarkMutedSwatch == swatch || this.mLightMutedColor == swatch;
    }

    private Swatch findColor(float targetLuma, float minLuma, float maxLuma, float targetSaturation, float minSaturation, float maxSaturation) {
        Swatch max = null;
        float maxValue = 0.0f;
        for (Swatch swatch : this.mSwatches) {
            float sat = swatch.getHsl()[1];
            float luma = swatch.getHsl()[2];
            if (sat >= minSaturation && sat <= maxSaturation && luma >= minLuma && luma <= maxLuma && !isAlreadySelected(swatch)) {
                float thisValue = createComparisonValue(sat, targetSaturation, luma, targetLuma, swatch.getPopulation(), this.mHighestPopulation);
                if (max == null || thisValue > maxValue) {
                    max = swatch;
                    maxValue = thisValue;
                }
            }
        }
        return max;
    }

    private void generateEmptySwatches() {
        if (this.mVibrantSwatch == null && this.mDarkVibrantSwatch != null) {
            float[] newHsl = copyHslValues(this.mDarkVibrantSwatch);
            newHsl[2] = 0.5f;
            this.mVibrantSwatch = new Swatch(ColorUtils.HSLtoRGB(newHsl), 0);
        }
        if (this.mDarkVibrantSwatch == null && this.mVibrantSwatch != null) {
            float[] newHsl2 = copyHslValues(this.mVibrantSwatch);
            newHsl2[2] = 0.26f;
            this.mDarkVibrantSwatch = new Swatch(ColorUtils.HSLtoRGB(newHsl2), 0);
        }
    }

    private int findMaxPopulation() {
        int population = 0;
        for (Swatch swatch : this.mSwatches) {
            population = Math.max(population, swatch.getPopulation());
        }
        return population;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Palette palette = (Palette) o;
        if (this.mSwatches == null ? palette.mSwatches != null : !this.mSwatches.equals(palette.mSwatches)) {
            return false;
        }
        if (this.mDarkMutedSwatch == null ? palette.mDarkMutedSwatch != null : !this.mDarkMutedSwatch.equals(palette.mDarkMutedSwatch)) {
            return false;
        }
        if (this.mDarkVibrantSwatch == null ? palette.mDarkVibrantSwatch != null : !this.mDarkVibrantSwatch.equals(palette.mDarkVibrantSwatch)) {
            return false;
        }
        if (this.mLightMutedColor == null ? palette.mLightMutedColor != null : !this.mLightMutedColor.equals(palette.mLightMutedColor)) {
            return false;
        }
        if (this.mLightVibrantSwatch == null ? palette.mLightVibrantSwatch != null : !this.mLightVibrantSwatch.equals(palette.mLightVibrantSwatch)) {
            return false;
        }
        if (this.mMutedSwatch == null ? palette.mMutedSwatch != null : !this.mMutedSwatch.equals(palette.mMutedSwatch)) {
            return false;
        }
        if (this.mVibrantSwatch != null) {
            if (this.mVibrantSwatch.equals(palette.mVibrantSwatch)) {
                return true;
            }
        } else if (palette.mVibrantSwatch == null) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        int result = this.mSwatches != null ? this.mSwatches.hashCode() : 0;
        return (((((((((((result * 31) + (this.mVibrantSwatch != null ? this.mVibrantSwatch.hashCode() : 0)) * 31) + (this.mMutedSwatch != null ? this.mMutedSwatch.hashCode() : 0)) * 31) + (this.mDarkVibrantSwatch != null ? this.mDarkVibrantSwatch.hashCode() : 0)) * 31) + (this.mDarkMutedSwatch != null ? this.mDarkMutedSwatch.hashCode() : 0)) * 31) + (this.mLightVibrantSwatch != null ? this.mLightVibrantSwatch.hashCode() : 0)) * 31) + (this.mLightMutedColor != null ? this.mLightMutedColor.hashCode() : 0);
    }

    private static Bitmap scaleBitmapDown(Bitmap bitmap) {
        int minDimension = Math.min(bitmap.getWidth(), bitmap.getHeight());
        if (minDimension > 100) {
            float scaleRatio = 100.0f / minDimension;
            return Bitmap.createScaledBitmap(bitmap, Math.round(bitmap.getWidth() * scaleRatio), Math.round(bitmap.getHeight() * scaleRatio), false);
        }
        return bitmap;
    }

    private static float createComparisonValue(float saturation, float targetSaturation, float luma, float targetLuma, int population, int highestPopulation) {
        return weightedMean(invertDiff(saturation, targetSaturation), 3.0f, invertDiff(luma, targetLuma), 6.0f, population / highestPopulation, 1.0f);
    }

    private static float[] copyHslValues(Swatch color) {
        float[] newHsl = new float[3];
        System.arraycopy(color.getHsl(), 0, newHsl, 0, 3);
        return newHsl;
    }

    private static float invertDiff(float value, float targetValue) {
        return 1.0f - Math.abs(value - targetValue);
    }

    private static float weightedMean(float... values) {
        float sum = 0.0f;
        float sumWeight = 0.0f;
        for (int i = 0; i < values.length; i += 2) {
            float value = values[i];
            float weight = values[i + 1];
            sum += value * weight;
            sumWeight += weight;
        }
        return sum / sumWeight;
    }

    private static void checkBitmapParam(Bitmap bitmap) {
        if (bitmap == null) {
            throw new IllegalArgumentException("bitmap can not be null");
        }
        if (bitmap.isRecycled()) {
            throw new IllegalArgumentException("bitmap can not be recycled");
        }
    }

    private static void checkNumberColorsParam(int numColors) {
        if (numColors < 1) {
            throw new IllegalArgumentException("numColors must be 1 of greater");
        }
    }

    public static final class Swatch {
        private final int mBlue;
        private int mBodyTextColor;
        private final int mGreen;
        private float[] mHsl;
        private final int mPopulation;
        private final int mRed;
        private final int mRgb;
        private int mTitleTextColor;

        public Swatch(int color, int population) {
            this.mRed = Color.red(color);
            this.mGreen = Color.green(color);
            this.mBlue = Color.blue(color);
            this.mRgb = color;
            this.mPopulation = population;
        }

        Swatch(int red, int green, int blue, int population) {
            this.mRed = red;
            this.mGreen = green;
            this.mBlue = blue;
            this.mRgb = Color.rgb(red, green, blue);
            this.mPopulation = population;
        }

        public int getRgb() {
            return this.mRgb;
        }

        public float[] getHsl() {
            if (this.mHsl == null) {
                this.mHsl = new float[3];
                ColorUtils.RGBtoHSL(this.mRed, this.mGreen, this.mBlue, this.mHsl);
            }
            return this.mHsl;
        }

        public int getPopulation() {
            return this.mPopulation;
        }

        public String toString() {
            return getClass().getSimpleName() + " [RGB: #" + Integer.toHexString(getRgb()) + "] [HSL: " + Arrays.toString(getHsl()) + "] [Population: " + this.mPopulation + "] [Title Text: #" + Integer.toHexString(this.mTitleTextColor) + "] [Body Text: #" + Integer.toHexString(this.mBodyTextColor) + ']';
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Swatch swatch = (Swatch) o;
            return this.mPopulation == swatch.mPopulation && this.mRgb == swatch.mRgb;
        }

        public int hashCode() {
            return (this.mRgb * 31) + this.mPopulation;
        }
    }
}
