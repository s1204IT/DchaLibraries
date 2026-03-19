package android.animation;

public class IntEvaluator implements TypeEvaluator<Integer> {
    @Override
    public Integer evaluate(float f, Integer num, Integer num2) {
        return Integer.valueOf((int) (num.intValue() + (f * (num2.intValue() - r3))));
    }
}
