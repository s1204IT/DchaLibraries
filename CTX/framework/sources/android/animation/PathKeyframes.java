package android.animation;

import android.animation.Keyframes;
import android.graphics.Path;
import android.graphics.PointF;
import java.util.ArrayList;

public class PathKeyframes implements Keyframes {
    private static final ArrayList<Keyframe> EMPTY_KEYFRAMES = new ArrayList<>();
    private static final int FRACTION_OFFSET = 0;
    private static final int NUM_COMPONENTS = 3;
    private static final int X_OFFSET = 1;
    private static final int Y_OFFSET = 2;
    private float[] mKeyframeData;
    private PointF mTempPointF;

    public PathKeyframes(Path path) {
        this(path, 0.5f);
    }

    public PathKeyframes(Path path, float f) {
        this.mTempPointF = new PointF();
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("The path must not be null or empty");
        }
        this.mKeyframeData = path.approximate(f);
    }

    @Override
    public ArrayList<Keyframe> getKeyframes() {
        return EMPTY_KEYFRAMES;
    }

    @Override
    public Object getValue(float f) {
        int length = this.mKeyframeData.length / 3;
        if (f < 0.0f) {
            return interpolateInRange(f, 0, 1);
        }
        if (f > 1.0f) {
            return interpolateInRange(f, length - 2, length - 1);
        }
        if (f == 0.0f) {
            return pointForIndex(0);
        }
        if (f == 1.0f) {
            return pointForIndex(length - 1);
        }
        int i = length - 1;
        int i2 = 0;
        while (i2 <= i) {
            int i3 = (i2 + i) / 2;
            float f2 = this.mKeyframeData[(i3 * 3) + 0];
            if (f < f2) {
                i = i3 - 1;
            } else if (f > f2) {
                i2 = i3 + 1;
            } else {
                return pointForIndex(i3);
            }
        }
        return interpolateInRange(f, i, i2);
    }

    private PointF interpolateInRange(float f, int i, int i2) {
        int i3 = i * 3;
        int i4 = i2 * 3;
        float f2 = this.mKeyframeData[i3 + 0];
        float f3 = (f - f2) / (this.mKeyframeData[i4 + 0] - f2);
        float f4 = this.mKeyframeData[i3 + 1];
        float f5 = this.mKeyframeData[i4 + 1];
        float f6 = this.mKeyframeData[i3 + 2];
        float f7 = this.mKeyframeData[i4 + 2];
        this.mTempPointF.set(interpolate(f3, f4, f5), interpolate(f3, f6, f7));
        return this.mTempPointF;
    }

    @Override
    public void setEvaluator(TypeEvaluator typeEvaluator) {
    }

    @Override
    public Class getType() {
        return PointF.class;
    }

    @Override
    public Keyframes m4clone() {
        try {
            return (Keyframes) super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    private PointF pointForIndex(int i) {
        int i2 = i * 3;
        this.mTempPointF.set(this.mKeyframeData[i2 + 1], this.mKeyframeData[i2 + 2]);
        return this.mTempPointF;
    }

    private static float interpolate(float f, float f2, float f3) {
        return f2 + ((f3 - f2) * f);
    }

    public Keyframes.FloatKeyframes createXFloatKeyframes() {
        return new FloatKeyframesBase() {
            @Override
            public float getFloatValue(float f) {
                return ((PointF) PathKeyframes.this.getValue(f)).x;
            }
        };
    }

    public Keyframes.FloatKeyframes createYFloatKeyframes() {
        return new FloatKeyframesBase() {
            @Override
            public float getFloatValue(float f) {
                return ((PointF) PathKeyframes.this.getValue(f)).y;
            }
        };
    }

    public Keyframes.IntKeyframes createXIntKeyframes() {
        return new IntKeyframesBase() {
            @Override
            public int getIntValue(float f) {
                return Math.round(((PointF) PathKeyframes.this.getValue(f)).x);
            }
        };
    }

    public Keyframes.IntKeyframes createYIntKeyframes() {
        return new IntKeyframesBase() {
            @Override
            public int getIntValue(float f) {
                return Math.round(((PointF) PathKeyframes.this.getValue(f)).y);
            }
        };
    }

    private static abstract class SimpleKeyframes implements Keyframes {
        private SimpleKeyframes() {
        }

        @Override
        public void setEvaluator(TypeEvaluator typeEvaluator) {
        }

        @Override
        public ArrayList<Keyframe> getKeyframes() {
            return PathKeyframes.EMPTY_KEYFRAMES;
        }

        @Override
        public Keyframes m5clone() {
            try {
                return (Keyframes) super.clone();
            } catch (CloneNotSupportedException e) {
                return null;
            }
        }
    }

    static abstract class IntKeyframesBase extends SimpleKeyframes implements Keyframes.IntKeyframes {
        IntKeyframesBase() {
            super();
        }

        @Override
        public Class getType() {
            return Integer.class;
        }

        @Override
        public Object getValue(float f) {
            return Integer.valueOf(getIntValue(f));
        }
    }

    static abstract class FloatKeyframesBase extends SimpleKeyframes implements Keyframes.FloatKeyframes {
        FloatKeyframesBase() {
            super();
        }

        @Override
        public Class getType() {
            return Float.class;
        }

        @Override
        public Object getValue(float f) {
            return Float.valueOf(getFloatValue(f));
        }
    }
}
