package javax.crypto.spec;

import libcore.util.EmptyArray;

public class PSource {
    private String pSrcName;

    private PSource() {
    }

    protected PSource(String pSrcName) {
        if (pSrcName == null) {
            throw new NullPointerException("pSrcName == null");
        }
        this.pSrcName = pSrcName;
    }

    public String getAlgorithm() {
        return this.pSrcName;
    }

    public static final class PSpecified extends PSource {
        public static final PSpecified DEFAULT = new PSpecified();
        private final byte[] p;

        private PSpecified() {
            super("PSpecified");
            this.p = EmptyArray.BYTE;
        }

        public PSpecified(byte[] p) {
            super("PSpecified");
            if (p == null) {
                throw new NullPointerException("p == null");
            }
            this.p = new byte[p.length];
            System.arraycopy(p, 0, this.p, 0, p.length);
        }

        public byte[] getValue() {
            byte[] result = new byte[this.p.length];
            System.arraycopy(this.p, 0, result, 0, this.p.length);
            return result;
        }
    }
}
