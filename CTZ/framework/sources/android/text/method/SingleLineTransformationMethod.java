package android.text.method;

public class SingleLineTransformationMethod extends ReplacementTransformationMethod {
    private static char[] ORIGINAL = {'\n', '\r'};
    private static char[] REPLACEMENT = {' ', 65279};
    private static SingleLineTransformationMethod sInstance;

    @Override
    protected char[] getOriginal() {
        return ORIGINAL;
    }

    @Override
    protected char[] getReplacement() {
        return REPLACEMENT;
    }

    public static SingleLineTransformationMethod getInstance() {
        if (sInstance != null) {
            return sInstance;
        }
        sInstance = new SingleLineTransformationMethod();
        return sInstance;
    }
}
