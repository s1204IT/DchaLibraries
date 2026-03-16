package android.support.v7.graphics;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.v7.graphics.Palette;
import android.util.SparseIntArray;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

final class ColorCutQuantizer {
    private static final String LOG_TAG = ColorCutQuantizer.class.getSimpleName();
    private static final Comparator<Vbox> VBOX_COMPARATOR_VOLUME = new Comparator<Vbox>() {
        @Override
        public int compare(Vbox lhs, Vbox rhs) {
            return rhs.getVolume() - lhs.getVolume();
        }
    };
    private final SparseIntArray mColorPopulations;
    private final int[] mColors;
    private final List<Palette.Swatch> mQuantizedColors;
    private final float[] mTempHsl = new float[3];

    static ColorCutQuantizer fromBitmap(Bitmap bitmap, int maxColors) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        return new ColorCutQuantizer(new ColorHistogram(pixels), maxColors);
    }

    private ColorCutQuantizer(ColorHistogram colorHistogram, int maxColors) {
        int validColorCount;
        int rawColorCount = colorHistogram.getNumberOfColors();
        int[] rawColors = colorHistogram.getColors();
        int[] rawColorCounts = colorHistogram.getColorCounts();
        this.mColorPopulations = new SparseIntArray(rawColorCount);
        for (int i = 0; i < rawColors.length; i++) {
            this.mColorPopulations.append(rawColors[i], rawColorCounts[i]);
        }
        this.mColors = new int[rawColorCount];
        int len$ = rawColors.length;
        int i$ = 0;
        int validColorCount2 = 0;
        while (i$ < len$) {
            int color = rawColors[i$];
            if (shouldIgnoreColor(color)) {
                validColorCount = validColorCount2;
            } else {
                validColorCount = validColorCount2 + 1;
                this.mColors[validColorCount2] = color;
            }
            i$++;
            validColorCount2 = validColorCount;
        }
        if (validColorCount2 <= maxColors) {
            this.mQuantizedColors = new ArrayList();
            int[] arr$ = this.mColors;
            for (int color2 : arr$) {
                this.mQuantizedColors.add(new Palette.Swatch(color2, this.mColorPopulations.get(color2)));
            }
            return;
        }
        this.mQuantizedColors = quantizePixels(validColorCount2 - 1, maxColors);
    }

    List<Palette.Swatch> getQuantizedColors() {
        return this.mQuantizedColors;
    }

    private List<Palette.Swatch> quantizePixels(int maxColorIndex, int maxColors) {
        PriorityQueue<Vbox> pq = new PriorityQueue<>(maxColors, VBOX_COMPARATOR_VOLUME);
        pq.offer(new Vbox(0, maxColorIndex));
        splitBoxes(pq, maxColors);
        return generateAverageColors(pq);
    }

    private void splitBoxes(PriorityQueue<Vbox> queue, int maxSize) {
        Vbox vbox;
        while (queue.size() < maxSize && (vbox = queue.poll()) != null && vbox.canSplit()) {
            queue.offer(vbox.splitBox());
            queue.offer(vbox);
        }
    }

    private List<Palette.Swatch> generateAverageColors(Collection<Vbox> vboxes) {
        ArrayList<Palette.Swatch> colors = new ArrayList<>(vboxes.size());
        for (Vbox vbox : vboxes) {
            Palette.Swatch color = vbox.getAverageColor();
            if (!shouldIgnoreColor(color)) {
                colors.add(color);
            }
        }
        return colors;
    }

    private class Vbox {
        private int mLowerIndex;
        private int mMaxBlue;
        private int mMaxGreen;
        private int mMaxRed;
        private int mMinBlue;
        private int mMinGreen;
        private int mMinRed;
        private int mUpperIndex;

        Vbox(int lowerIndex, int upperIndex) {
            this.mLowerIndex = lowerIndex;
            this.mUpperIndex = upperIndex;
            fitBox();
        }

        int getVolume() {
            return ((this.mMaxRed - this.mMinRed) + 1) * ((this.mMaxGreen - this.mMinGreen) + 1) * ((this.mMaxBlue - this.mMinBlue) + 1);
        }

        boolean canSplit() {
            return getColorCount() > 1;
        }

        int getColorCount() {
            return (this.mUpperIndex - this.mLowerIndex) + 1;
        }

        void fitBox() {
            this.mMinBlue = 255;
            this.mMinGreen = 255;
            this.mMinRed = 255;
            this.mMaxBlue = 0;
            this.mMaxGreen = 0;
            this.mMaxRed = 0;
            for (int i = this.mLowerIndex; i <= this.mUpperIndex; i++) {
                int color = ColorCutQuantizer.this.mColors[i];
                int r = Color.red(color);
                int g = Color.green(color);
                int b = Color.blue(color);
                if (r > this.mMaxRed) {
                    this.mMaxRed = r;
                }
                if (r < this.mMinRed) {
                    this.mMinRed = r;
                }
                if (g > this.mMaxGreen) {
                    this.mMaxGreen = g;
                }
                if (g < this.mMinGreen) {
                    this.mMinGreen = g;
                }
                if (b > this.mMaxBlue) {
                    this.mMaxBlue = b;
                }
                if (b < this.mMinBlue) {
                    this.mMinBlue = b;
                }
            }
        }

        Vbox splitBox() {
            if (!canSplit()) {
                throw new IllegalStateException("Can not split a box with only 1 color");
            }
            int splitPoint = findSplitPoint();
            Vbox newBox = ColorCutQuantizer.this.new Vbox(splitPoint + 1, this.mUpperIndex);
            this.mUpperIndex = splitPoint;
            fitBox();
            return newBox;
        }

        int getLongestColorDimension() {
            int redLength = this.mMaxRed - this.mMinRed;
            int greenLength = this.mMaxGreen - this.mMinGreen;
            int blueLength = this.mMaxBlue - this.mMinBlue;
            if (redLength >= greenLength && redLength >= blueLength) {
                return -3;
            }
            if (greenLength >= redLength && greenLength >= blueLength) {
                return -2;
            }
            return -1;
        }

        int findSplitPoint() {
            int longestDimension = getLongestColorDimension();
            ColorCutQuantizer.this.modifySignificantOctet(longestDimension, this.mLowerIndex, this.mUpperIndex);
            Arrays.sort(ColorCutQuantizer.this.mColors, this.mLowerIndex, this.mUpperIndex + 1);
            ColorCutQuantizer.this.modifySignificantOctet(longestDimension, this.mLowerIndex, this.mUpperIndex);
            int dimensionMidPoint = midPoint(longestDimension);
            for (int i = this.mLowerIndex; i <= this.mUpperIndex; i++) {
                int color = ColorCutQuantizer.this.mColors[i];
                switch (longestDimension) {
                    case -3:
                        if (Color.red(color) >= dimensionMidPoint) {
                            return i;
                        }
                        break;
                        break;
                    case -2:
                        if (Color.green(color) >= dimensionMidPoint) {
                            return i;
                        }
                        break;
                        break;
                    case -1:
                        if (Color.blue(color) > dimensionMidPoint) {
                            return i;
                        }
                        break;
                        break;
                }
            }
            int i2 = this.mLowerIndex;
            return i2;
        }

        Palette.Swatch getAverageColor() {
            int redSum = 0;
            int greenSum = 0;
            int blueSum = 0;
            int totalPopulation = 0;
            for (int i = this.mLowerIndex; i <= this.mUpperIndex; i++) {
                int color = ColorCutQuantizer.this.mColors[i];
                int colorPopulation = ColorCutQuantizer.this.mColorPopulations.get(color);
                totalPopulation += colorPopulation;
                redSum += Color.red(color) * colorPopulation;
                greenSum += Color.green(color) * colorPopulation;
                blueSum += Color.blue(color) * colorPopulation;
            }
            int redAverage = Math.round(redSum / totalPopulation);
            int greenAverage = Math.round(greenSum / totalPopulation);
            int blueAverage = Math.round(blueSum / totalPopulation);
            return new Palette.Swatch(redAverage, greenAverage, blueAverage, totalPopulation);
        }

        int midPoint(int dimension) {
            switch (dimension) {
                case -2:
                    return (this.mMinGreen + this.mMaxGreen) / 2;
                case -1:
                    return (this.mMinBlue + this.mMaxBlue) / 2;
                default:
                    return (this.mMinRed + this.mMaxRed) / 2;
            }
        }
    }

    private void modifySignificantOctet(int dimension, int lowerIndex, int upperIndex) {
        switch (dimension) {
            case -2:
                for (int i = lowerIndex; i <= upperIndex; i++) {
                    int color = this.mColors[i];
                    this.mColors[i] = Color.rgb((color >> 8) & 255, (color >> 16) & 255, color & 255);
                }
                break;
            case -1:
                for (int i2 = lowerIndex; i2 <= upperIndex; i2++) {
                    int color2 = this.mColors[i2];
                    this.mColors[i2] = Color.rgb(color2 & 255, (color2 >> 8) & 255, (color2 >> 16) & 255);
                }
                break;
        }
    }

    private boolean shouldIgnoreColor(int color) {
        ColorUtils.RGBtoHSL(Color.red(color), Color.green(color), Color.blue(color), this.mTempHsl);
        return shouldIgnoreColor(this.mTempHsl);
    }

    private static boolean shouldIgnoreColor(Palette.Swatch color) {
        return shouldIgnoreColor(color.getHsl());
    }

    private static boolean shouldIgnoreColor(float[] hslColor) {
        return isWhite(hslColor) || isBlack(hslColor) || isNearRedILine(hslColor);
    }

    private static boolean isBlack(float[] hslColor) {
        return hslColor[2] <= 0.05f;
    }

    private static boolean isWhite(float[] hslColor) {
        return hslColor[2] >= 0.95f;
    }

    private static boolean isNearRedILine(float[] hslColor) {
        return hslColor[0] >= 10.0f && hslColor[0] <= 37.0f && hslColor[1] <= 0.82f;
    }
}
