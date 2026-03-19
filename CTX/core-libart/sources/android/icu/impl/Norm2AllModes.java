package android.icu.impl;

import android.icu.impl.Normalizer2Impl;
import android.icu.text.Normalizer;
import android.icu.text.Normalizer2;
import android.icu.util.ICUUncheckedIOException;
import java.io.IOException;
import java.nio.ByteBuffer;

public final class Norm2AllModes {
    public final ComposeNormalizer2 comp;
    public final DecomposeNormalizer2 decomp;
    public final ComposeNormalizer2 fcc;
    public final FCDNormalizer2 fcd;
    public final Normalizer2Impl impl;
    private static CacheBase<String, Norm2AllModes, ByteBuffer> cache = new SoftCache<String, Norm2AllModes, ByteBuffer>() {
        @Override
        protected Norm2AllModes createInstance(String str, ByteBuffer byteBuffer) {
            Normalizer2Impl normalizer2ImplLoad;
            if (byteBuffer == null) {
                normalizer2ImplLoad = new Normalizer2Impl().load(str + ".nrm");
            } else {
                normalizer2ImplLoad = new Normalizer2Impl().load(byteBuffer);
            }
            return new Norm2AllModes(normalizer2ImplLoad);
        }
    };
    public static final NoopNormalizer2 NOOP_NORMALIZER2 = new NoopNormalizer2();

    public static final class NoopNormalizer2 extends Normalizer2 {
        @Override
        public StringBuilder normalize(CharSequence charSequence, StringBuilder sb) {
            if (sb != charSequence) {
                sb.setLength(0);
                sb.append(charSequence);
                return sb;
            }
            throw new IllegalArgumentException();
        }

        @Override
        public Appendable normalize(CharSequence charSequence, Appendable appendable) {
            if (appendable != charSequence) {
                try {
                    return appendable.append(charSequence);
                } catch (IOException e) {
                    throw new ICUUncheckedIOException(e);
                }
            }
            throw new IllegalArgumentException();
        }

        @Override
        public StringBuilder normalizeSecondAndAppend(StringBuilder sb, CharSequence charSequence) {
            if (sb != charSequence) {
                sb.append(charSequence);
                return sb;
            }
            throw new IllegalArgumentException();
        }

        @Override
        public StringBuilder append(StringBuilder sb, CharSequence charSequence) {
            if (sb != charSequence) {
                sb.append(charSequence);
                return sb;
            }
            throw new IllegalArgumentException();
        }

        @Override
        public String getDecomposition(int i) {
            return null;
        }

        @Override
        public boolean isNormalized(CharSequence charSequence) {
            return true;
        }

        @Override
        public Normalizer.QuickCheckResult quickCheck(CharSequence charSequence) {
            return Normalizer.YES;
        }

        @Override
        public int spanQuickCheckYes(CharSequence charSequence) {
            return charSequence.length();
        }

        @Override
        public boolean hasBoundaryBefore(int i) {
            return true;
        }

        @Override
        public boolean hasBoundaryAfter(int i) {
            return true;
        }

        @Override
        public boolean isInert(int i) {
            return true;
        }
    }

    public static abstract class Normalizer2WithImpl extends Normalizer2 {
        public final Normalizer2Impl impl;

        public abstract int getQuickCheck(int i);

        protected abstract void normalize(CharSequence charSequence, Normalizer2Impl.ReorderingBuffer reorderingBuffer);

        protected abstract void normalizeAndAppend(CharSequence charSequence, boolean z, Normalizer2Impl.ReorderingBuffer reorderingBuffer);

        public Normalizer2WithImpl(Normalizer2Impl normalizer2Impl) {
            this.impl = normalizer2Impl;
        }

        @Override
        public StringBuilder normalize(CharSequence charSequence, StringBuilder sb) {
            if (sb == charSequence) {
                throw new IllegalArgumentException();
            }
            sb.setLength(0);
            normalize(charSequence, new Normalizer2Impl.ReorderingBuffer(this.impl, sb, charSequence.length()));
            return sb;
        }

        @Override
        public Appendable normalize(CharSequence charSequence, Appendable appendable) {
            if (appendable == charSequence) {
                throw new IllegalArgumentException();
            }
            Normalizer2Impl.ReorderingBuffer reorderingBuffer = new Normalizer2Impl.ReorderingBuffer(this.impl, appendable, charSequence.length());
            normalize(charSequence, reorderingBuffer);
            reorderingBuffer.flush();
            return appendable;
        }

        @Override
        public StringBuilder normalizeSecondAndAppend(StringBuilder sb, CharSequence charSequence) {
            return normalizeSecondAndAppend(sb, charSequence, true);
        }

        @Override
        public StringBuilder append(StringBuilder sb, CharSequence charSequence) {
            return normalizeSecondAndAppend(sb, charSequence, false);
        }

        public StringBuilder normalizeSecondAndAppend(StringBuilder sb, CharSequence charSequence, boolean z) {
            if (sb == charSequence) {
                throw new IllegalArgumentException();
            }
            normalizeAndAppend(charSequence, z, new Normalizer2Impl.ReorderingBuffer(this.impl, sb, sb.length() + charSequence.length()));
            return sb;
        }

        @Override
        public String getDecomposition(int i) {
            return this.impl.getDecomposition(i);
        }

        @Override
        public String getRawDecomposition(int i) {
            return this.impl.getRawDecomposition(i);
        }

        @Override
        public int composePair(int i, int i2) {
            return this.impl.composePair(i, i2);
        }

        @Override
        public int getCombiningClass(int i) {
            return this.impl.getCC(this.impl.getNorm16(i));
        }

        @Override
        public boolean isNormalized(CharSequence charSequence) {
            return charSequence.length() == spanQuickCheckYes(charSequence);
        }

        @Override
        public Normalizer.QuickCheckResult quickCheck(CharSequence charSequence) {
            return isNormalized(charSequence) ? Normalizer.YES : Normalizer.NO;
        }
    }

    public static final class DecomposeNormalizer2 extends Normalizer2WithImpl {
        public DecomposeNormalizer2(Normalizer2Impl normalizer2Impl) {
            super(normalizer2Impl);
        }

        @Override
        protected void normalize(CharSequence charSequence, Normalizer2Impl.ReorderingBuffer reorderingBuffer) {
            this.impl.decompose(charSequence, 0, charSequence.length(), reorderingBuffer);
        }

        @Override
        protected void normalizeAndAppend(CharSequence charSequence, boolean z, Normalizer2Impl.ReorderingBuffer reorderingBuffer) {
            this.impl.decomposeAndAppend(charSequence, z, reorderingBuffer);
        }

        @Override
        public int spanQuickCheckYes(CharSequence charSequence) {
            return this.impl.decompose(charSequence, 0, charSequence.length(), null);
        }

        @Override
        public int getQuickCheck(int i) {
            return this.impl.isDecompYes(this.impl.getNorm16(i)) ? 1 : 0;
        }

        @Override
        public boolean hasBoundaryBefore(int i) {
            return this.impl.hasDecompBoundaryBefore(i);
        }

        @Override
        public boolean hasBoundaryAfter(int i) {
            return this.impl.hasDecompBoundaryAfter(i);
        }

        @Override
        public boolean isInert(int i) {
            return this.impl.isDecompInert(i);
        }
    }

    public static final class ComposeNormalizer2 extends Normalizer2WithImpl {
        private final boolean onlyContiguous;

        public ComposeNormalizer2(Normalizer2Impl normalizer2Impl, boolean z) {
            super(normalizer2Impl);
            this.onlyContiguous = z;
        }

        @Override
        protected void normalize(CharSequence charSequence, Normalizer2Impl.ReorderingBuffer reorderingBuffer) {
            this.impl.compose(charSequence, 0, charSequence.length(), this.onlyContiguous, true, reorderingBuffer);
        }

        @Override
        protected void normalizeAndAppend(CharSequence charSequence, boolean z, Normalizer2Impl.ReorderingBuffer reorderingBuffer) {
            this.impl.composeAndAppend(charSequence, z, this.onlyContiguous, reorderingBuffer);
        }

        @Override
        public boolean isNormalized(CharSequence charSequence) {
            return this.impl.compose(charSequence, 0, charSequence.length(), this.onlyContiguous, false, new Normalizer2Impl.ReorderingBuffer(this.impl, new StringBuilder(), 5));
        }

        @Override
        public Normalizer.QuickCheckResult quickCheck(CharSequence charSequence) {
            int iComposeQuickCheck = this.impl.composeQuickCheck(charSequence, 0, charSequence.length(), this.onlyContiguous, false);
            if ((iComposeQuickCheck & 1) != 0) {
                return Normalizer.MAYBE;
            }
            if ((iComposeQuickCheck >>> 1) == charSequence.length()) {
                return Normalizer.YES;
            }
            return Normalizer.NO;
        }

        @Override
        public int spanQuickCheckYes(CharSequence charSequence) {
            return this.impl.composeQuickCheck(charSequence, 0, charSequence.length(), this.onlyContiguous, true) >>> 1;
        }

        @Override
        public int getQuickCheck(int i) {
            return this.impl.getCompQuickCheck(this.impl.getNorm16(i));
        }

        @Override
        public boolean hasBoundaryBefore(int i) {
            return this.impl.hasCompBoundaryBefore(i);
        }

        @Override
        public boolean hasBoundaryAfter(int i) {
            return this.impl.hasCompBoundaryAfter(i, this.onlyContiguous);
        }

        @Override
        public boolean isInert(int i) {
            return this.impl.isCompInert(i, this.onlyContiguous);
        }
    }

    public static final class FCDNormalizer2 extends Normalizer2WithImpl {
        public FCDNormalizer2(Normalizer2Impl normalizer2Impl) {
            super(normalizer2Impl);
        }

        @Override
        protected void normalize(CharSequence charSequence, Normalizer2Impl.ReorderingBuffer reorderingBuffer) {
            this.impl.makeFCD(charSequence, 0, charSequence.length(), reorderingBuffer);
        }

        @Override
        protected void normalizeAndAppend(CharSequence charSequence, boolean z, Normalizer2Impl.ReorderingBuffer reorderingBuffer) {
            this.impl.makeFCDAndAppend(charSequence, z, reorderingBuffer);
        }

        @Override
        public int spanQuickCheckYes(CharSequence charSequence) {
            return this.impl.makeFCD(charSequence, 0, charSequence.length(), null);
        }

        @Override
        public int getQuickCheck(int i) {
            return this.impl.isDecompYes(this.impl.getNorm16(i)) ? 1 : 0;
        }

        @Override
        public boolean hasBoundaryBefore(int i) {
            return this.impl.hasFCDBoundaryBefore(i);
        }

        @Override
        public boolean hasBoundaryAfter(int i) {
            return this.impl.hasFCDBoundaryAfter(i);
        }

        @Override
        public boolean isInert(int i) {
            return this.impl.isFCDInert(i);
        }
    }

    private Norm2AllModes(Normalizer2Impl normalizer2Impl) {
        this.impl = normalizer2Impl;
        this.comp = new ComposeNormalizer2(normalizer2Impl, false);
        this.decomp = new DecomposeNormalizer2(normalizer2Impl);
        this.fcd = new FCDNormalizer2(normalizer2Impl);
        this.fcc = new ComposeNormalizer2(normalizer2Impl, true);
    }

    private static Norm2AllModes getInstanceFromSingleton(Norm2AllModesSingleton norm2AllModesSingleton) {
        if (norm2AllModesSingleton.exception == null) {
            return norm2AllModesSingleton.allModes;
        }
        throw norm2AllModesSingleton.exception;
    }

    public static Norm2AllModes getNFCInstance() {
        return getInstanceFromSingleton(NFCSingleton.INSTANCE);
    }

    public static Norm2AllModes getNFKCInstance() {
        return getInstanceFromSingleton(NFKCSingleton.INSTANCE);
    }

    public static Norm2AllModes getNFKC_CFInstance() {
        return getInstanceFromSingleton(NFKC_CFSingleton.INSTANCE);
    }

    public static Normalizer2WithImpl getN2WithImpl(int i) {
        switch (i) {
            case 0:
                return getNFCInstance().decomp;
            case 1:
                return getNFKCInstance().decomp;
            case 2:
                return getNFCInstance().comp;
            case 3:
                return getNFKCInstance().comp;
            default:
                return null;
        }
    }

    public static Norm2AllModes getInstance(ByteBuffer byteBuffer, String str) {
        Norm2AllModesSingleton norm2AllModesSingleton;
        if (byteBuffer == null) {
            if (!str.equals("nfc")) {
                if (!str.equals("nfkc")) {
                    if (!str.equals("nfkc_cf")) {
                        norm2AllModesSingleton = null;
                    } else {
                        norm2AllModesSingleton = NFKC_CFSingleton.INSTANCE;
                    }
                } else {
                    norm2AllModesSingleton = NFKCSingleton.INSTANCE;
                }
            } else {
                norm2AllModesSingleton = NFCSingleton.INSTANCE;
            }
            if (norm2AllModesSingleton != null) {
                if (norm2AllModesSingleton.exception == null) {
                    return norm2AllModesSingleton.allModes;
                }
                throw norm2AllModesSingleton.exception;
            }
        }
        return cache.getInstance(str, byteBuffer);
    }

    public static Normalizer2 getFCDNormalizer2() {
        return getNFCInstance().fcd;
    }

    private static final class Norm2AllModesSingleton {
        private Norm2AllModes allModes;
        private RuntimeException exception;

        private Norm2AllModesSingleton(String str) {
            try {
                this.allModes = new Norm2AllModes(new Normalizer2Impl().load(str + ".nrm"));
            } catch (RuntimeException e) {
                this.exception = e;
            }
        }
    }

    private static final class NFCSingleton {
        private static final Norm2AllModesSingleton INSTANCE = new Norm2AllModesSingleton("nfc");

        private NFCSingleton() {
        }
    }

    private static final class NFKCSingleton {
        private static final Norm2AllModesSingleton INSTANCE = new Norm2AllModesSingleton("nfkc");

        private NFKCSingleton() {
        }
    }

    private static final class NFKC_CFSingleton {
        private static final Norm2AllModesSingleton INSTANCE = new Norm2AllModesSingleton("nfkc_cf");

        private NFKC_CFSingleton() {
        }
    }
}
