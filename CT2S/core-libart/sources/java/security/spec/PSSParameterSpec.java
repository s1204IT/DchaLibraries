package java.security.spec;

public class PSSParameterSpec implements AlgorithmParameterSpec {
    public static final PSSParameterSpec DEFAULT = new PSSParameterSpec(20);
    private final String mdName;
    private final String mgfName;
    private final AlgorithmParameterSpec mgfSpec;
    private final int saltLen;
    private final int trailerField;

    public PSSParameterSpec(int saltLen) {
        if (saltLen < 0) {
            throw new IllegalArgumentException("saltLen < 0");
        }
        this.saltLen = saltLen;
        this.mdName = "SHA-1";
        this.mgfName = "MGF1";
        this.mgfSpec = MGF1ParameterSpec.SHA1;
        this.trailerField = 1;
    }

    public PSSParameterSpec(String mdName, String mgfName, AlgorithmParameterSpec mgfSpec, int saltLen, int trailerField) {
        if (mdName == null) {
            throw new NullPointerException("mdName == null");
        }
        if (mgfName == null) {
            throw new NullPointerException("mgfName == null");
        }
        if (saltLen < 0) {
            throw new IllegalArgumentException("saltLen < 0");
        }
        if (trailerField < 0) {
            throw new IllegalArgumentException("trailerField < 0");
        }
        this.mdName = mdName;
        this.mgfName = mgfName;
        this.mgfSpec = mgfSpec;
        this.saltLen = saltLen;
        this.trailerField = trailerField;
    }

    public int getSaltLength() {
        return this.saltLen;
    }

    public String getDigestAlgorithm() {
        return this.mdName;
    }

    public String getMGFAlgorithm() {
        return this.mgfName;
    }

    public AlgorithmParameterSpec getMGFParameters() {
        return this.mgfSpec;
    }

    public int getTrailerField() {
        return this.trailerField;
    }
}
