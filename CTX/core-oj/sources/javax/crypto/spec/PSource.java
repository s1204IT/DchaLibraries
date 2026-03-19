package javax.crypto.spec;

public class PSource {
    private String pSrcName;

    protected PSource(String str) {
        if (str == null) {
            throw new NullPointerException("pSource algorithm is null");
        }
        this.pSrcName = str;
    }

    public String getAlgorithm() {
        return this.pSrcName;
    }

    public static final class PSpecified extends PSource {
        public static final PSpecified DEFAULT = new PSpecified(new byte[0]);
        private byte[] p;

        public PSpecified(byte[] bArr) {
            super("PSpecified");
            this.p = new byte[0];
            this.p = (byte[]) bArr.clone();
        }

        public byte[] getValue() {
            return this.p.length == 0 ? this.p : (byte[]) this.p.clone();
        }
    }
}
