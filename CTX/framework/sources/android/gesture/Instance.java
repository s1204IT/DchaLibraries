package android.gesture;

class Instance {
    private static final float[] ORIENTATIONS = {0.0f, 0.7853982f, 1.5707964f, 2.3561945f, 3.1415927f, 0.0f, -0.7853982f, -1.5707964f, -2.3561945f, -3.1415927f};
    private static final int PATCH_SAMPLE_SIZE = 16;
    private static final int SEQUENCE_SAMPLE_SIZE = 16;
    final long id;
    final String label;
    final float[] vector;

    private Instance(long j, float[] fArr, String str) {
        this.id = j;
        this.vector = fArr;
        this.label = str;
    }

    private void normalize() {
        float[] fArr = this.vector;
        int length = fArr.length;
        float f = 0.0f;
        for (int i = 0; i < length; i++) {
            f += fArr[i] * fArr[i];
        }
        float fSqrt = (float) Math.sqrt(f);
        for (int i2 = 0; i2 < length; i2++) {
            fArr[i2] = fArr[i2] / fSqrt;
        }
    }

    static Instance createInstance(int i, int i2, Gesture gesture, String str) {
        if (i == 2) {
            Instance instance = new Instance(gesture.getID(), temporalSampler(i2, gesture), str);
            instance.normalize();
            return instance;
        }
        return new Instance(gesture.getID(), spatialSampler(gesture), str);
    }

    private static float[] spatialSampler(Gesture gesture) {
        return GestureUtils.spatialSampling(gesture, 16, false);
    }

    private static float[] temporalSampler(int i, Gesture gesture) {
        float[] fArrTemporalSampling = GestureUtils.temporalSampling(gesture.getStrokes().get(0), 16);
        float[] fArrComputeCentroid = GestureUtils.computeCentroid(fArrTemporalSampling);
        float fAtan2 = (float) Math.atan2(fArrTemporalSampling[1] - fArrComputeCentroid[1], fArrTemporalSampling[0] - fArrComputeCentroid[0]);
        float f = -fAtan2;
        if (i != 1) {
            int length = ORIENTATIONS.length;
            float f2 = f;
            for (int i2 = 0; i2 < length; i2++) {
                float f3 = ORIENTATIONS[i2] - fAtan2;
                if (Math.abs(f3) < Math.abs(f2)) {
                    f2 = f3;
                }
            }
            f = f2;
        }
        GestureUtils.translate(fArrTemporalSampling, -fArrComputeCentroid[0], -fArrComputeCentroid[1]);
        GestureUtils.rotate(fArrTemporalSampling, f);
        return fArrTemporalSampling;
    }
}
