package android.animation;

public class IntEvaluator implements TypeEvaluator<Integer> {
    @Override
    public Integer evaluate(float fraction, Integer startValue, Integer endValue) {
        int startInt = startValue.intValue();
        return Integer.valueOf((int) (startInt + ((endValue.intValue() - startInt) * fraction)));
    }
}
