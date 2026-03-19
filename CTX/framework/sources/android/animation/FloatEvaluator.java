package android.animation;

public class FloatEvaluator implements TypeEvaluator<Number> {
    @Override
    public Float evaluate(float f, Number number, Number number2) {
        float fFloatValue = number.floatValue();
        return Float.valueOf(fFloatValue + (f * (number2.floatValue() - fFloatValue)));
    }
}
