package android.support.v17.leanback.widget;

import android.support.v17.leanback.widget.Parallax;
import java.util.ArrayList;
import java.util.List;

public abstract class ParallaxEffect {
    final List<Parallax.PropertyMarkerValue> mMarkerValues = new ArrayList(2);
    final List<Float> mWeights = new ArrayList(2);
    final List<Float> mTotalWeights = new ArrayList(2);
    final List<ParallaxTarget> mTargets = new ArrayList(4);

    abstract Number calculateDirectValue(Parallax parallax);

    abstract float calculateFraction(Parallax parallax);

    ParallaxEffect() {
    }

    public final void performMapping(Parallax source) {
        if (this.mMarkerValues.size() < 2) {
            return;
        }
        if (this instanceof IntEffect) {
            source.verifyIntProperties();
        } else {
            source.verifyFloatProperties();
        }
        boolean fractionCalculated = false;
        float fraction = 0.0f;
        Number directValue = null;
        for (int i = 0; i < this.mTargets.size(); i++) {
            ParallaxTarget target = this.mTargets.get(i);
            if (target.isDirectMapping()) {
                if (directValue == null) {
                    directValue = calculateDirectValue(source);
                }
                target.directUpdate(directValue);
            } else {
                if (!fractionCalculated) {
                    fractionCalculated = true;
                    fraction = calculateFraction(source);
                }
                target.update(fraction);
            }
        }
    }

    final float getFractionWithWeightAdjusted(float fraction, int markerValueIndex) {
        if (this.mMarkerValues.size() >= 3) {
            boolean hasWeightsDefined = this.mWeights.size() == this.mMarkerValues.size() - 1;
            if (hasWeightsDefined) {
                float allWeights = this.mTotalWeights.get(this.mTotalWeights.size() - 1).floatValue();
                float fraction2 = (this.mWeights.get(markerValueIndex - 1).floatValue() * fraction) / allWeights;
                if (markerValueIndex >= 2) {
                    fraction2 += this.mTotalWeights.get(markerValueIndex - 2).floatValue() / allWeights;
                }
                return fraction2;
            }
            float allWeights2 = this.mMarkerValues.size() - 1;
            float fraction3 = fraction / allWeights2;
            if (markerValueIndex >= 2) {
                return fraction3 + ((markerValueIndex - 1) / allWeights2);
            }
            return fraction3;
        }
        return fraction;
    }

    static final class IntEffect extends ParallaxEffect {
        IntEffect() {
        }

        @Override
        Number calculateDirectValue(Parallax source) {
            if (this.mMarkerValues.size() != 2) {
                throw new RuntimeException("Must use two marker values for direct mapping");
            }
            if (this.mMarkerValues.get(0).getProperty() != this.mMarkerValues.get(1).getProperty()) {
                throw new RuntimeException("Marker value must use same Property for direct mapping");
            }
            int value1 = ((Parallax.IntPropertyMarkerValue) this.mMarkerValues.get(0)).getMarkerValue(source);
            int value2 = ((Parallax.IntPropertyMarkerValue) this.mMarkerValues.get(1)).getMarkerValue(source);
            if (value1 > value2) {
                value2 = value1;
                value1 = value2;
            }
            Number currentValue = ((Parallax.IntProperty) this.mMarkerValues.get(0).getProperty()).get(source);
            if (currentValue.intValue() < value1) {
                return Integer.valueOf(value1);
            }
            if (currentValue.intValue() > value2) {
                return Integer.valueOf(value2);
            }
            return currentValue;
        }

        @Override
        float calculateFraction(Parallax source) {
            float fraction;
            int lastIndex = 0;
            int lastValue = 0;
            int lastMarkerValue = 0;
            for (int i = 0; i < this.mMarkerValues.size(); i++) {
                Parallax.IntPropertyMarkerValue k = (Parallax.IntPropertyMarkerValue) this.mMarkerValues.get(i);
                int index = k.getProperty().getIndex();
                int markerValue = k.getMarkerValue(source);
                int currentValue = source.getIntPropertyValue(index);
                if (i == 0) {
                    if (currentValue >= markerValue) {
                        return 0.0f;
                    }
                } else {
                    if (lastIndex == index && lastMarkerValue < markerValue) {
                        throw new IllegalStateException("marker value of same variable must be descendant order");
                    }
                    if (currentValue == Integer.MAX_VALUE) {
                        float fraction2 = (lastMarkerValue - lastValue) / source.getMaxValue();
                        return getFractionWithWeightAdjusted(fraction2, i);
                    }
                    if (currentValue >= markerValue) {
                        if (lastIndex == index) {
                            fraction = (lastMarkerValue - currentValue) / (lastMarkerValue - markerValue);
                        } else if (lastValue != Integer.MIN_VALUE) {
                            int lastMarkerValue2 = lastMarkerValue + (currentValue - lastValue);
                            fraction = (lastMarkerValue2 - currentValue) / (lastMarkerValue2 - markerValue);
                        } else {
                            fraction = 1.0f - ((currentValue - markerValue) / source.getMaxValue());
                        }
                        return getFractionWithWeightAdjusted(fraction, i);
                    }
                }
                lastValue = currentValue;
                lastIndex = index;
                lastMarkerValue = markerValue;
            }
            return 1.0f;
        }
    }
}
