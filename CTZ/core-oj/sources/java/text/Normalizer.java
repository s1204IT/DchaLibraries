package java.text;

import android.icu.text.Normalizer;

public final class Normalizer {
    private Normalizer() {
    }

    public enum Form {
        NFD(android.icu.text.Normalizer.NFD),
        NFC(android.icu.text.Normalizer.NFC),
        NFKD(android.icu.text.Normalizer.NFKD),
        NFKC(android.icu.text.Normalizer.NFKC);

        private final Normalizer.Mode icuMode;

        Form(Normalizer.Mode mode) {
            this.icuMode = mode;
        }
    }

    public static String normalize(CharSequence charSequence, Form form) {
        return android.icu.text.Normalizer.normalize(charSequence.toString(), form.icuMode);
    }

    public static boolean isNormalized(CharSequence charSequence, Form form) {
        return android.icu.text.Normalizer.isNormalized(charSequence.toString(), form.icuMode, 0);
    }
}
