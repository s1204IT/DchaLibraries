package libcore.icu;

import java.text.Normalizer;

public final class NativeNormalizer {
    private static native boolean isNormalizedImpl(String str, int i);

    private static native String normalizeImpl(String str, int i);

    public static boolean isNormalized(CharSequence src, Normalizer.Form form) {
        return isNormalizedImpl(src.toString(), toUNormalizationMode(form));
    }

    public static String normalize(CharSequence src, Normalizer.Form form) {
        return normalizeImpl(src.toString(), toUNormalizationMode(form));
    }

    private static int toUNormalizationMode(Normalizer.Form form) {
        switch (form) {
            case NFC:
                return 4;
            case NFD:
                return 2;
            case NFKC:
                return 5;
            case NFKD:
                return 3;
            default:
                throw new AssertionError("unknown Normalizer.Form " + form);
        }
    }

    private NativeNormalizer() {
    }
}
