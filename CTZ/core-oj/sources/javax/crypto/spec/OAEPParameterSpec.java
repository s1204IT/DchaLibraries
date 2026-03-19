package javax.crypto.spec;

import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.MGF1ParameterSpec;
import javax.crypto.spec.PSource;

public class OAEPParameterSpec implements AlgorithmParameterSpec {
    public static final OAEPParameterSpec DEFAULT = new OAEPParameterSpec();
    private String mdName;
    private String mgfName;
    private AlgorithmParameterSpec mgfSpec;
    private PSource pSrc;

    private OAEPParameterSpec() {
        this.mdName = "SHA-1";
        this.mgfName = "MGF1";
        this.mgfSpec = MGF1ParameterSpec.SHA1;
        this.pSrc = PSource.PSpecified.DEFAULT;
    }

    public OAEPParameterSpec(String str, String str2, AlgorithmParameterSpec algorithmParameterSpec, PSource pSource) {
        this.mdName = "SHA-1";
        this.mgfName = "MGF1";
        this.mgfSpec = MGF1ParameterSpec.SHA1;
        this.pSrc = PSource.PSpecified.DEFAULT;
        if (str == null) {
            throw new NullPointerException("digest algorithm is null");
        }
        if (str2 == null) {
            throw new NullPointerException("mask generation function algorithm is null");
        }
        if (pSource == null) {
            throw new NullPointerException("source of the encoding input is null");
        }
        this.mdName = str;
        this.mgfName = str2;
        this.mgfSpec = algorithmParameterSpec;
        this.pSrc = pSource;
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

    public PSource getPSource() {
        return this.pSrc;
    }
}
