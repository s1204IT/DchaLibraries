package android.support.v17.leanback.widget;

import android.util.Property;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Parallax<PropertyT extends Property> {
    final List<PropertyT> mProperties = new ArrayList();
    final List<PropertyT> mPropertiesReadOnly = Collections.unmodifiableList(this.mProperties);
    private int[] mValues = new int[4];
    private float[] mFloatValues = new float[4];
    private final List<ParallaxEffect> mEffects = new ArrayList(4);

    public abstract float getMaxValue();

    public static class PropertyMarkerValue<PropertyT> {
        private final PropertyT mProperty;

        public PropertyT getProperty() {
            return this.mProperty;
        }
    }

    public static class IntProperty extends Property<Parallax, Integer> {
        private final int mIndex;

        @Override
        public final Integer get(Parallax object) {
            return Integer.valueOf(object.getIntPropertyValue(this.mIndex));
        }

        @Override
        public final void set(Parallax object, Integer value) {
            object.setIntPropertyValue(this.mIndex, value.intValue());
        }

        public final int getIndex() {
            return this.mIndex;
        }
    }

    static class IntPropertyMarkerValue extends PropertyMarkerValue<IntProperty> {
        private final float mFactionOfMax;
        private final int mValue;

        final int getMarkerValue(Parallax source) {
            return this.mFactionOfMax == 0.0f ? this.mValue : this.mValue + Math.round(source.getMaxValue() * this.mFactionOfMax);
        }
    }

    final int getIntPropertyValue(int index) {
        return this.mValues[index];
    }

    final void setIntPropertyValue(int index, int value) {
        if (index >= this.mProperties.size()) {
            throw new ArrayIndexOutOfBoundsException();
        }
        this.mValues[index] = value;
    }

    void verifyIntProperties() throws IllegalStateException {
        if (this.mProperties.size() < 2) {
            return;
        }
        int last = getIntPropertyValue(0);
        int last2 = last;
        for (int last3 = 1; last3 < this.mProperties.size(); last3++) {
            int v = getIntPropertyValue(last3);
            if (v < last2) {
                throw new IllegalStateException(String.format("Parallax Property[%d]\"%s\" is smaller than Property[%d]\"%s\"", Integer.valueOf(last3), this.mProperties.get(last3).getName(), Integer.valueOf(last3 - 1), this.mProperties.get(last3 - 1).getName()));
            }
            if (last2 == Integer.MIN_VALUE && v == Integer.MAX_VALUE) {
                throw new IllegalStateException(String.format("Parallax Property[%d]\"%s\" is UNKNOWN_BEFORE and Property[%d]\"%s\" is UNKNOWN_AFTER", Integer.valueOf(last3 - 1), this.mProperties.get(last3 - 1).getName(), Integer.valueOf(last3), this.mProperties.get(last3).getName()));
            }
            last2 = v;
        }
    }

    final void verifyFloatProperties() throws IllegalStateException {
        if (this.mProperties.size() < 2) {
            return;
        }
        float last = getFloatPropertyValue(0);
        float last2 = last;
        for (int i = 1; i < this.mProperties.size(); i++) {
            float v = getFloatPropertyValue(i);
            if (v < last2) {
                throw new IllegalStateException(String.format("Parallax Property[%d]\"%s\" is smaller than Property[%d]\"%s\"", Integer.valueOf(i), this.mProperties.get(i).getName(), Integer.valueOf(i - 1), this.mProperties.get(i - 1).getName()));
            }
            if (last2 == -3.4028235E38f && v == Float.MAX_VALUE) {
                throw new IllegalStateException(String.format("Parallax Property[%d]\"%s\" is UNKNOWN_BEFORE and Property[%d]\"%s\" is UNKNOWN_AFTER", Integer.valueOf(i - 1), this.mProperties.get(i - 1).getName(), Integer.valueOf(i), this.mProperties.get(i).getName()));
            }
            last2 = v;
        }
    }

    final float getFloatPropertyValue(int index) {
        return this.mFloatValues[index];
    }

    public void updateValues() {
        for (int i = 0; i < this.mEffects.size(); i++) {
            this.mEffects.get(i).performMapping(this);
        }
    }
}
