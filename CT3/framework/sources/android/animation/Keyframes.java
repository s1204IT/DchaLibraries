package android.animation;

import java.util.List;

public interface Keyframes extends Cloneable {

    public interface FloatKeyframes extends Keyframes {
        float getFloatValue(float f);
    }

    public interface IntKeyframes extends Keyframes {
        int getIntValue(float f);
    }

    @Override
    Keyframes mo46clone();

    List<Keyframe> getKeyframes();

    Class getType();

    Object getValue(float f);

    void invalidateCache();

    void setEvaluator(TypeEvaluator typeEvaluator);
}
