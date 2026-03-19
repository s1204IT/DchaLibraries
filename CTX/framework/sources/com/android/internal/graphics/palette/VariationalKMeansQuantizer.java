package com.android.internal.graphics.palette;

import com.android.internal.graphics.ColorUtils;
import com.android.internal.graphics.palette.Palette;
import com.android.internal.ml.clustering.KMeans;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class VariationalKMeansQuantizer implements Quantizer {
    private static final boolean DEBUG = false;
    private static final String TAG = "KMeansQuantizer";
    private final int mInitializations;
    private final KMeans mKMeans;
    private final float mMinClusterSqDistance;
    private List<Palette.Swatch> mQuantizedColors;

    public VariationalKMeansQuantizer() {
        this(0.25f);
    }

    public VariationalKMeansQuantizer(float f) {
        this(f, 1);
    }

    public VariationalKMeansQuantizer(float f, int i) {
        this.mKMeans = new KMeans(new Random(0L), 30, 0.0f);
        this.mMinClusterSqDistance = f * f;
        this.mInitializations = i;
    }

    @Override
    public void quantize(int[] iArr, int i, Palette.Filter[] filterArr) {
        float[] fArr = {0.0f, 0.0f, 0.0f};
        float[][] fArr2 = (float[][]) Array.newInstance((Class<?>) float.class, iArr.length, 3);
        for (int i2 = 0; i2 < iArr.length; i2++) {
            ColorUtils.colorToHSL(iArr[i2], fArr);
            fArr2[i2][0] = fArr[0] / 360.0f;
            fArr2[i2][1] = fArr[1];
            fArr2[i2][2] = fArr[2];
        }
        List<KMeans.Mean> optimalKMeans = getOptimalKMeans(i, fArr2);
        int i3 = 0;
        while (i3 < optimalKMeans.size()) {
            KMeans.Mean mean = optimalKMeans.get(i3);
            float[] centroid = mean.getCentroid();
            i3++;
            int i4 = i3;
            while (i4 < optimalKMeans.size()) {
                KMeans.Mean mean2 = optimalKMeans.get(i4);
                float[] centroid2 = mean2.getCentroid();
                if (KMeans.sqDistance(centroid, centroid2) < this.mMinClusterSqDistance) {
                    optimalKMeans.remove(mean2);
                    mean.getItems().addAll(mean2.getItems());
                    for (int i5 = 0; i5 < centroid.length; i5++) {
                        centroid[i5] = (float) (((double) centroid[i5]) + (((double) (centroid2[i5] - centroid[i5])) / 2.0d));
                    }
                    i4--;
                }
                i4++;
            }
        }
        this.mQuantizedColors = new ArrayList();
        for (KMeans.Mean mean3 : optimalKMeans) {
            if (mean3.getItems().size() != 0) {
                float[] centroid3 = mean3.getCentroid();
                this.mQuantizedColors.add(new Palette.Swatch(new float[]{centroid3[0] * 360.0f, centroid3[1], centroid3[2]}, mean3.getItems().size()));
            }
        }
    }

    private List<KMeans.Mean> getOptimalKMeans(int i, float[][] fArr) {
        List<KMeans.Mean> list = null;
        double d = -1.7976931348623157E308d;
        for (int i2 = this.mInitializations; i2 > 0; i2--) {
            List<KMeans.Mean> listPredict = this.mKMeans.predict(i, fArr);
            double dScore = KMeans.score(listPredict);
            if (list == null || dScore > d) {
                list = listPredict;
                d = dScore;
            }
        }
        return list;
    }

    @Override
    public List<Palette.Swatch> getQuantizedColors() {
        return this.mQuantizedColors;
    }
}
