package java.util.function;

@FunctionalInterface
public interface UnaryOperator<T> extends Function<T, T> {
    static <T> UnaryOperator<T> identity() {
        return new UnaryOperator() {
            @Override
            public final Object apply(Object obj) {
                return UnaryOperator.lambda$identity$0(obj);
            }
        };
    }

    static Object lambda$identity$0(Object obj) {
        return obj;
    }
}
