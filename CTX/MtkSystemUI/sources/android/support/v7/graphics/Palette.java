package android.support.v7.graphics;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.support.v4.graphics.ColorUtils;
import android.support.v4.util.ArrayMap;
import android.util.SparseBooleanArray;
import android.util.TimingLogger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class Palette {
    static final Filter DEFAULT_FILTER = new Filter() {
        @Override
        public boolean isAllowed(int rgb, float[] hsl) {
            return (isWhite(hsl) || isBlack(hsl) || isNearRedILine(hsl)) ? false : true;
        }

        private boolean isBlack(float[] hslColor) {
            return hslColor[2] <= 0.05f;
        }

        private boolean isWhite(float[] hslColor) {
            return hslColor[2] >= 0.95f;
        }

        private boolean isNearRedILine(float[] hslColor) {
            return hslColor[0] >= 10.0f && hslColor[0] <= 37.0f && hslColor[1] <= 0.82f;
        }
    };
    private final List<Swatch> mSwatches;
    private final List<Target> mTargets;
    private final SparseBooleanArray mUsedColors = new SparseBooleanArray();
    private final Map<Target, Swatch> mSelectedSwatches = new ArrayMap();
    private final Swatch mDominantSwatch = findDominantSwatch();

    public interface Filter {
        boolean isAllowed(int i, float[] fArr);
    }

    public static Builder from(Bitmap bitmap) {
        return new Builder(bitmap);
    }

    Palette(List<Swatch> swatches, List<Target> targets) {
        this.mSwatches = swatches;
        this.mTargets = targets;
    }

    public List<Swatch> getSwatches() {
        return Collections.unmodifiableList(this.mSwatches);
    }

    public Swatch getVibrantSwatch() {
        return getSwatchForTarget(Target.VIBRANT);
    }

    public Swatch getLightVibrantSwatch() {
        return getSwatchForTarget(Target.LIGHT_VIBRANT);
    }

    public Swatch getDarkVibrantSwatch() {
        return getSwatchForTarget(Target.DARK_VIBRANT);
    }

    public Swatch getMutedSwatch() {
        return getSwatchForTarget(Target.MUTED);
    }

    public Swatch getLightMutedSwatch() {
        return getSwatchForTarget(Target.LIGHT_MUTED);
    }

    public Swatch getDarkMutedSwatch() {
        return getSwatchForTarget(Target.DARK_MUTED);
    }

    public Swatch getSwatchForTarget(Target target) {
        return this.mSelectedSwatches.get(target);
    }

    public Swatch getDominantSwatch() {
        return this.mDominantSwatch;
    }

    void generate() {
        int count = this.mTargets.size();
        for (int i = 0; i < count; i++) {
            Target target = this.mTargets.get(i);
            target.normalizeWeights();
            this.mSelectedSwatches.put(target, generateScoredTarget(target));
        }
        this.mUsedColors.clear();
    }

    private Swatch generateScoredTarget(Target target) {
        Swatch maxScoreSwatch = getMaxScoredSwatchForTarget(target);
        if (maxScoreSwatch != null && target.isExclusive()) {
            this.mUsedColors.append(maxScoreSwatch.getRgb(), true);
        }
        return maxScoreSwatch;
    }

    private Swatch getMaxScoredSwatchForTarget(Target target) {
        float maxScore = 0.0f;
        Swatch maxScoreSwatch = null;
        int count = this.mSwatches.size();
        for (int i = 0; i < count; i++) {
            Swatch swatch = this.mSwatches.get(i);
            if (shouldBeScoredForTarget(swatch, target)) {
                float score = generateScore(swatch, target);
                if (maxScoreSwatch == null || score > maxScore) {
                    maxScoreSwatch = swatch;
                    maxScore = score;
                }
            }
        }
        return maxScoreSwatch;
    }

    private boolean shouldBeScoredForTarget(Swatch swatch, Target target) {
        float[] hsl = swatch.getHsl();
        return hsl[1] >= target.getMinimumSaturation() && hsl[1] <= target.getMaximumSaturation() && hsl[2] >= target.getMinimumLightness() && hsl[2] <= target.getMaximumLightness() && !this.mUsedColors.get(swatch.getRgb());
    }

    private float generateScore(Swatch swatch, Target target) {
        float[] hsl = swatch.getHsl();
        float saturationScore = 0.0f;
        float luminanceScore = 0.0f;
        float populationScore = 0.0f;
        int maxPopulation = this.mDominantSwatch != null ? this.mDominantSwatch.getPopulation() : 1;
        if (target.getSaturationWeight() > 0.0f) {
            saturationScore = target.getSaturationWeight() * (1.0f - Math.abs(hsl[1] - target.getTargetSaturation()));
        }
        if (target.getLightnessWeight() > 0.0f) {
            luminanceScore = target.getLightnessWeight() * (1.0f - Math.abs(hsl[2] - target.getTargetLightness()));
        }
        if (target.getPopulationWeight() > 0.0f) {
            populationScore = target.getPopulationWeight() * (swatch.getPopulation() / maxPopulation);
        }
        return saturationScore + luminanceScore + populationScore;
    }

    private Swatch findDominantSwatch() {
        int maxPop = Integer.MIN_VALUE;
        Swatch maxSwatch = null;
        int count = this.mSwatches.size();
        for (int i = 0; i < count; i++) {
            Swatch swatch = this.mSwatches.get(i);
            if (swatch.getPopulation() > maxPop) {
                maxSwatch = swatch;
                maxPop = swatch.getPopulation();
            }
        }
        return maxSwatch;
    }

    public static final class Swatch {
        private final int mBlue;
        private int mBodyTextColor;
        private boolean mGeneratedTextColors;
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

        public int getRgb() {
            return this.mRgb;
        }

        public float[] getHsl() {
            if (this.mHsl == null) {
                this.mHsl = new float[3];
            }
            ColorUtils.RGBToHSL(this.mRed, this.mGreen, this.mBlue, this.mHsl);
            return this.mHsl;
        }

        public int getPopulation() {
            return this.mPopulation;
        }

        public int getTitleTextColor() {
            ensureTextColorsGenerated();
            return this.mTitleTextColor;
        }

        public int getBodyTextColor() {
            ensureTextColorsGenerated();
            return this.mBodyTextColor;
        }

        private void ensureTextColorsGenerated() {
            int alphaComponent;
            int alphaComponent2;
            if (!this.mGeneratedTextColors) {
                int lightBodyAlpha = ColorUtils.calculateMinimumAlpha(-1, this.mRgb, 4.5f);
                int lightTitleAlpha = ColorUtils.calculateMinimumAlpha(-1, this.mRgb, 3.0f);
                if (lightBodyAlpha != -1 && lightTitleAlpha != -1) {
                    this.mBodyTextColor = ColorUtils.setAlphaComponent(-1, lightBodyAlpha);
                    this.mTitleTextColor = ColorUtils.setAlphaComponent(-1, lightTitleAlpha);
                    this.mGeneratedTextColors = true;
                    return;
                }
                int darkBodyAlpha = ColorUtils.calculateMinimumAlpha(-16777216, this.mRgb, 4.5f);
                int darkTitleAlpha = ColorUtils.calculateMinimumAlpha(-16777216, this.mRgb, 3.0f);
                if (darkBodyAlpha != -1 && darkTitleAlpha != -1) {
                    this.mBodyTextColor = ColorUtils.setAlphaComponent(-16777216, darkBodyAlpha);
                    this.mTitleTextColor = ColorUtils.setAlphaComponent(-16777216, darkTitleAlpha);
                    this.mGeneratedTextColors = true;
                    return;
                }
                if (lightBodyAlpha != -1) {
                    alphaComponent = ColorUtils.setAlphaComponent(-1, lightBodyAlpha);
                } else {
                    alphaComponent = ColorUtils.setAlphaComponent(-16777216, darkBodyAlpha);
                }
                this.mBodyTextColor = alphaComponent;
                if (lightTitleAlpha != -1) {
                    alphaComponent2 = ColorUtils.setAlphaComponent(-1, lightTitleAlpha);
                } else {
                    alphaComponent2 = ColorUtils.setAlphaComponent(-16777216, darkTitleAlpha);
                }
                this.mTitleTextColor = alphaComponent2;
                this.mGeneratedTextColors = true;
            }
        }

        public String toString() {
            return getClass().getSimpleName() + " [RGB: #" + Integer.toHexString(getRgb()) + "] [HSL: " + Arrays.toString(getHsl()) + "] [Population: " + this.mPopulation + "] [Title Text: #" + Integer.toHexString(getTitleTextColor()) + "] [Body Text: #" + Integer.toHexString(getBodyTextColor()) + ']';
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Swatch swatch = (Swatch) o;
            if (this.mPopulation == swatch.mPopulation && this.mRgb == swatch.mRgb) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            return (31 * this.mRgb) + this.mPopulation;
        }
    }

    public static final class Builder {
        private final Bitmap mBitmap;
        private Rect mRegion;
        private final List<Swatch> mSwatches;
        private final List<Target> mTargets = new ArrayList();
        private int mMaxColors = 16;
        private int mResizeArea = 12544;
        private int mResizeMaxDimension = -1;
        private final List<Filter> mFilters = new ArrayList();

        public Builder(Bitmap bitmap) {
            if (bitmap == null || bitmap.isRecycled()) {
                throw new IllegalArgumentException("Bitmap is not valid");
            }
            this.mFilters.add(Palette.DEFAULT_FILTER);
            this.mBitmap = bitmap;
            this.mSwatches = null;
            this.mTargets.add(Target.LIGHT_VIBRANT);
            this.mTargets.add(Target.VIBRANT);
            this.mTargets.add(Target.DARK_VIBRANT);
            this.mTargets.add(Target.LIGHT_MUTED);
            this.mTargets.add(Target.MUTED);
            this.mTargets.add(Target.DARK_MUTED);
        }

        public Builder maximumColorCount(int colors) {
            this.mMaxColors = colors;
            return this;
        }

        public Builder resizeBitmapArea(int area) {
            this.mResizeArea = area;
            this.mResizeMaxDimension = -1;
            return this;
        }

        public Builder clearFilters() {
            this.mFilters.clear();
            return this;
        }

        public Builder addFilter(Filter filter) {
            if (filter != null) {
                this.mFilters.add(filter);
            }
            return this;
        }

        public Builder setRegion(int left, int top, int right, int bottom) {
            if (this.mBitmap != null) {
                if (this.mRegion == null) {
                    this.mRegion = new Rect();
                }
                this.mRegion.set(0, 0, this.mBitmap.getWidth(), this.mBitmap.getHeight());
                if (!this.mRegion.intersect(left, top, right, bottom)) {
                    throw new IllegalArgumentException("The given region must intersect with the Bitmap's dimensions.");
                }
            }
            return this;
        }

        public Palette generate() {
            List<Swatch> swatches;
            TimingLogger logger = null;
            if (this.mBitmap != null) {
                Bitmap bitmap = scaleBitmapDown(this.mBitmap);
                if (0 != 0) {
                    logger.addSplit("Processed Bitmap");
                }
                Rect region = this.mRegion;
                if (bitmap != this.mBitmap && region != null) {
                    double scale = ((double) bitmap.getWidth()) / ((double) this.mBitmap.getWidth());
                    region.left = (int) Math.floor(((double) region.left) * scale);
                    region.top = (int) Math.floor(((double) region.top) * scale);
                    region.right = Math.min((int) Math.ceil(((double) region.right) * scale), bitmap.getWidth());
                    region.bottom = Math.min((int) Math.ceil(((double) region.bottom) * scale), bitmap.getHeight());
                }
                ColorCutQuantizer quantizer = new ColorCutQuantizer(getPixelsFromBitmap(bitmap), this.mMaxColors, this.mFilters.isEmpty() ? null : (Filter[]) this.mFilters.toArray(new Filter[this.mFilters.size()]));
                if (bitmap != this.mBitmap) {
                    bitmap.recycle();
                }
                swatches = quantizer.getQuantizedColors();
                if (0 != 0) {
                    logger.addSplit("Color quantization completed");
                }
            } else if (this.mSwatches != null) {
                swatches = this.mSwatches;
            } else {
                throw new AssertionError();
            }
            Palette p = new Palette(swatches, this.mTargets);
            p.generate();
            if (0 != 0) {
                logger.addSplit("Created Palette");
                logger.dumpToLog();
            }
            return p;
        }

        private int[] getPixelsFromBitmap(Bitmap bitmap) {
            int bitmapWidth = bitmap.getWidth();
            int bitmapHeight = bitmap.getHeight();
            int[] pixels = new int[bitmapWidth * bitmapHeight];
            bitmap.getPixels(pixels, 0, bitmapWidth, 0, 0, bitmapWidth, bitmapHeight);
            if (this.mRegion == null) {
                return pixels;
            }
            int regionWidth = this.mRegion.width();
            int regionHeight = this.mRegion.height();
            int[] subsetPixels = new int[regionWidth * regionHeight];
            for (int row = 0; row < regionHeight; row++) {
                System.arraycopy(pixels, ((this.mRegion.top + row) * bitmapWidth) + this.mRegion.left, subsetPixels, row * regionWidth, regionWidth);
            }
            return subsetPixels;
        }

        private Bitmap scaleBitmapDown(Bitmap bitmap) {
            int maxDimension;
            double scaleRatio = -1.0d;
            if (this.mResizeArea > 0) {
                int bitmapArea = bitmap.getWidth() * bitmap.getHeight();
                if (bitmapArea > this.mResizeArea) {
                    scaleRatio = Math.sqrt(((double) this.mResizeArea) / ((double) bitmapArea));
                }
            } else if (this.mResizeMaxDimension > 0 && (maxDimension = Math.max(bitmap.getWidth(), bitmap.getHeight())) > this.mResizeMaxDimension) {
                scaleRatio = ((double) this.mResizeMaxDimension) / ((double) maxDimension);
            }
            if (scaleRatio <= 0.0d) {
                return bitmap;
            }
            return Bitmap.createScaledBitmap(bitmap, (int) Math.ceil(((double) bitmap.getWidth()) * scaleRatio), (int) Math.ceil(((double) bitmap.getHeight()) * scaleRatio), false);
        }
    }
}
