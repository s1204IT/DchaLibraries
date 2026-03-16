package com.android.camera.settings;

import com.android.camera.util.ApiHelper;
import com.android.ex.camera2.portability.Size;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class ResolutionUtil {
    public static final String NEXUS_5_LARGE_16_BY_9 = "1836x3264";
    public static final float NEXUS_5_LARGE_16_BY_9_ASPECT_RATIO = 1.7777778f;
    private static final float RATIO_TOLERANCE = 0.05f;
    public static Size NEXUS_5_LARGE_16_BY_9_SIZE = new Size(1836, 3264);
    private static Float[] sDesiredAspectRatios = {Float.valueOf(1.7777778f), Float.valueOf(1.3333334f)};
    private static Size[] sDesiredAspectRatioSizes = {new Size(16, 9), new Size(4, 3)};

    private static class ResolutionBucket {
        public Float aspectRatio;
        public Size largest;
        public Integer maxPixels;
        public List<Size> sizes;

        private ResolutionBucket() {
            this.sizes = new LinkedList();
            this.maxPixels = 0;
        }

        public void add(Size size) {
            this.sizes.add(size);
            Collections.sort(this.sizes, new Comparator<Size>() {
                @Override
                public int compare(Size size2, Size size22) {
                    return Integer.compare(size22.width() * size22.height(), size2.width() * size2.height());
                }
            });
            this.maxPixels = Integer.valueOf(this.sizes.get(0).height() * this.sizes.get(0).width());
        }
    }

    public static List<Size> getDisplayableSizesFromSupported(List<Size> sizes, boolean isBackCamera) {
        List<ResolutionBucket> buckets = parseAvailableSizes(sizes, isBackCamera);
        List<Float> sortedDesiredAspectRatios = new ArrayList<>();
        sortedDesiredAspectRatios.add(Float.valueOf(buckets.get(0).aspectRatio.floatValue()));
        Iterator<ResolutionBucket> it = buckets.iterator();
        while (it.hasNext()) {
            Float aspectRatio = it.next().aspectRatio;
            if (Arrays.asList(sDesiredAspectRatios).contains(aspectRatio) && !sortedDesiredAspectRatios.contains(aspectRatio)) {
                sortedDesiredAspectRatios.add(aspectRatio);
            }
        }
        List<Size> result = new ArrayList<>(sizes.size());
        for (Float targetRatio : sortedDesiredAspectRatios) {
            for (ResolutionBucket bucket : buckets) {
                if (Math.abs(bucket.aspectRatio.floatValue() - targetRatio.floatValue()) <= RATIO_TOLERANCE) {
                    result.addAll(pickUpToThree(bucket.sizes));
                }
            }
        }
        return result;
    }

    private static int area(Size size) {
        if (size == null) {
            return 0;
        }
        return size.width() * size.height();
    }

    private static List<Size> pickUpToThree(List<Size> sizes) {
        List<Size> result = new ArrayList<>();
        Size largest = sizes.get(0);
        result.add(largest);
        Size lastSize = largest;
        for (Size size : sizes) {
            double targetArea = Math.pow(0.5d, result.size()) * ((double) area(largest));
            if (area(size) < targetArea) {
                if (!result.contains(lastSize) && targetArea - ((double) area(lastSize)) < ((double) area(size)) - targetArea) {
                    result.add(lastSize);
                } else {
                    result.add(size);
                }
            }
            lastSize = size;
            if (result.size() == 3) {
                break;
            }
        }
        if (result.size() < 3 && !result.contains(lastSize)) {
            result.add(lastSize);
        }
        return result;
    }

    private static float fuzzAspectRatio(float aspectRatio) {
        Float[] arr$ = sDesiredAspectRatios;
        for (Float f : arr$) {
            float desiredAspectRatio = f.floatValue();
            if (Math.abs(aspectRatio - desiredAspectRatio) < RATIO_TOLERANCE) {
                return desiredAspectRatio;
            }
        }
        return aspectRatio;
    }

    private static List<ResolutionBucket> parseAvailableSizes(List<Size> sizes, boolean isBackCamera) {
        HashMap<Float, ResolutionBucket> aspectRatioToBuckets = new HashMap<>();
        for (Size size : sizes) {
            Float aspectRatio = Float.valueOf(fuzzAspectRatio(Float.valueOf(size.width() / size.height()).floatValue()));
            ResolutionBucket bucket = aspectRatioToBuckets.get(aspectRatio);
            if (bucket == null) {
                bucket = new ResolutionBucket();
                bucket.aspectRatio = aspectRatio;
                aspectRatioToBuckets.put(aspectRatio, bucket);
            }
            bucket.add(size);
        }
        if (ApiHelper.IS_NEXUS_5 && isBackCamera) {
            aspectRatioToBuckets.get(Float.valueOf(1.7777778f)).add(NEXUS_5_LARGE_16_BY_9_SIZE);
        }
        List<ResolutionBucket> sortedBuckets = new ArrayList<>(aspectRatioToBuckets.values());
        Collections.sort(sortedBuckets, new Comparator<ResolutionBucket>() {
            @Override
            public int compare(ResolutionBucket resolutionBucket, ResolutionBucket resolutionBucket2) {
                return Integer.compare(resolutionBucket2.maxPixels.intValue(), resolutionBucket.maxPixels.intValue());
            }
        });
        return sortedBuckets;
    }

    public static String aspectRatioDescription(Size size) {
        Size aspectRatio = reduce(size);
        return aspectRatio.width() + "x" + aspectRatio.height();
    }

    public static Size reduce(Size aspectRatio) {
        BigInteger width = BigInteger.valueOf(aspectRatio.width());
        BigInteger height = BigInteger.valueOf(aspectRatio.height());
        BigInteger gcd = width.gcd(height);
        int numerator = Math.max(width.intValue(), height.intValue()) / gcd.intValue();
        int denominator = Math.min(width.intValue(), height.intValue()) / gcd.intValue();
        return new Size(numerator, denominator);
    }

    public static int aspectRatioNumerator(Size size) {
        Size aspectRatio = reduce(size);
        return aspectRatio.width();
    }

    public static Size getApproximateSize(Size size) {
        Size aspectRatio = reduce(size);
        float fuzzy = fuzzAspectRatio(size.width() / size.height());
        int index = Arrays.asList(sDesiredAspectRatios).indexOf(Float.valueOf(fuzzy));
        if (index != -1) {
            Size aspectRatio2 = new Size(sDesiredAspectRatioSizes[index]);
            return aspectRatio2;
        }
        return aspectRatio;
    }

    public static com.android.camera.util.Size getApproximateSize(com.android.camera.util.Size size) {
        Size result = getApproximateSize(new Size(size.getWidth(), size.getHeight()));
        return new com.android.camera.util.Size(result.width(), result.height());
    }

    public static int aspectRatioDenominator(Size size) {
        BigInteger width = BigInteger.valueOf(size.width());
        BigInteger height = BigInteger.valueOf(size.height());
        BigInteger gcd = width.gcd(height);
        int denominator = Math.min(width.intValue(), height.intValue()) / gcd.intValue();
        return denominator;
    }
}
