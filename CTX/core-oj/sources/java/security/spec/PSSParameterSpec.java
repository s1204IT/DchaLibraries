package java.security.spec;

public class PSSParameterSpec implements AlgorithmParameterSpec {
    public static final PSSParameterSpec DEFAULT = new PSSParameterSpec();
    private String mdName;
    private String mgfName;
    private AlgorithmParameterSpec mgfSpec;
    private int saltLen;
    private int trailerField;

    private PSSParameterSpec() {
        this.mdName = "SHA-1";
        this.mgfName = "MGF1";
        this.mgfSpec = MGF1ParameterSpec.SHA1;
        this.saltLen = 20;
        this.trailerField = 1;
    }

    public PSSParameterSpec(String str, String str2, AlgorithmParameterSpec algorithmParameterSpec, int i, int i2) {
        this.mdName = "SHA-1";
        this.mgfName = "MGF1";
        this.mgfSpec = MGF1ParameterSpec.SHA1;
        this.saltLen = 20;
        this.trailerField = 1;
        if (str == null) {
            throw new NullPointerException("digest algorithm is null");
        }
        if (str2 == null) {
            throw new NullPointerException("mask generation function algorithm is null");
        }
        if (i < 0) {
            throw new IllegalArgumentException("negative saltLen value: " + i);
        }
        if (i2 < 0) {
            throw new IllegalArgumentException("negative trailerField: " + i2);
        }
        this.mdName = str;
        this.mgfName = str2;
        this.mgfSpec = algorithmParameterSpec;
        this.saltLen = i;
        this.trailerField = i2;
    }

    public PSSParameterSpec(int i) {
        this.mdName = "SHA-1";
        this.mgfName = "MGF1";
        this.mgfSpec = MGF1ParameterSpec.SHA1;
        this.saltLen = 20;
        this.trailerField = 1;
        if (i < 0) {
            throw new IllegalArgumentException("negative saltLen value: " + i);
        }
        this.saltLen = i;
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

    public int getSaltLength() {
        return this.saltLen;
    }

    public int getTrailerField() {
        return this.trailerField;
    }
}
