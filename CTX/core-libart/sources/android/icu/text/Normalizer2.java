package android.icu.text;

import android.icu.impl.ICUBinary;
import android.icu.impl.Norm2AllModes;
import android.icu.text.Normalizer;
import android.icu.util.ICUUncheckedIOException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public abstract class Normalizer2 {

    public enum Mode {
        COMPOSE,
        DECOMPOSE,
        FCD,
        COMPOSE_CONTIGUOUS
    }

    public abstract StringBuilder append(StringBuilder sb, CharSequence charSequence);

    public abstract String getDecomposition(int i);

    public abstract boolean hasBoundaryAfter(int i);

    public abstract boolean hasBoundaryBefore(int i);

    public abstract boolean isInert(int i);

    public abstract boolean isNormalized(CharSequence charSequence);

    public abstract Appendable normalize(CharSequence charSequence, Appendable appendable);

    public abstract StringBuilder normalize(CharSequence charSequence, StringBuilder sb);

    public abstract StringBuilder normalizeSecondAndAppend(StringBuilder sb, CharSequence charSequence);

    public abstract Normalizer.QuickCheckResult quickCheck(CharSequence charSequence);

    public abstract int spanQuickCheckYes(CharSequence charSequence);

    public static Normalizer2 getNFCInstance() {
        return Norm2AllModes.getNFCInstance().comp;
    }

    public static Normalizer2 getNFDInstance() {
        return Norm2AllModes.getNFCInstance().decomp;
    }

    public static Normalizer2 getNFKCInstance() {
        return Norm2AllModes.getNFKCInstance().comp;
    }

    public static Normalizer2 getNFKDInstance() {
        return Norm2AllModes.getNFKCInstance().decomp;
    }

    public static Normalizer2 getNFKCCasefoldInstance() {
        return Norm2AllModes.getNFKC_CFInstance().comp;
    }

    public static Normalizer2 getInstance(InputStream inputStream, String str, Mode mode) {
        ByteBuffer byteBufferFromInputStreamAndCloseStream;
        if (inputStream != null) {
            try {
                byteBufferFromInputStreamAndCloseStream = ICUBinary.getByteBufferFromInputStreamAndCloseStream(inputStream);
            } catch (IOException e) {
                throw new ICUUncheckedIOException(e);
            }
        } else {
            byteBufferFromInputStreamAndCloseStream = null;
        }
        Norm2AllModes norm2AllModes = Norm2AllModes.getInstance(byteBufferFromInputStreamAndCloseStream, str);
        switch (mode) {
            case COMPOSE:
                return norm2AllModes.comp;
            case DECOMPOSE:
                return norm2AllModes.decomp;
            case FCD:
                return norm2AllModes.fcd;
            case COMPOSE_CONTIGUOUS:
                return norm2AllModes.fcc;
            default:
                return null;
        }
    }

    public String normalize(CharSequence charSequence) {
        if (charSequence instanceof String) {
            int iSpanQuickCheckYes = spanQuickCheckYes(charSequence);
            if (iSpanQuickCheckYes == charSequence.length()) {
                return (String) charSequence;
            }
            if (iSpanQuickCheckYes != 0) {
                StringBuilder sb = new StringBuilder(charSequence.length());
                sb.append(charSequence, 0, iSpanQuickCheckYes);
                return normalizeSecondAndAppend(sb, charSequence.subSequence(iSpanQuickCheckYes, charSequence.length())).toString();
            }
        }
        return normalize(charSequence, new StringBuilder(charSequence.length())).toString();
    }

    public String getRawDecomposition(int i) {
        return null;
    }

    public int composePair(int i, int i2) {
        return -1;
    }

    public int getCombiningClass(int i) {
        return 0;
    }

    @Deprecated
    protected Normalizer2() {
    }
}
