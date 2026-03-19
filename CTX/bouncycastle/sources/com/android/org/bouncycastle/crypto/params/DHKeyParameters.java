package com.android.org.bouncycastle.crypto.params;

public class DHKeyParameters extends AsymmetricKeyParameter {
    private DHParameters params;

    protected DHKeyParameters(boolean z, DHParameters dHParameters) {
        super(z);
        this.params = dHParameters;
    }

    public DHParameters getParameters() {
        return this.params;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof DHKeyParameters)) {
            return false;
        }
        DHKeyParameters dHKeyParameters = (DHKeyParameters) obj;
        if (this.params == null) {
            return dHKeyParameters.getParameters() == null;
        }
        return this.params.equals(dHKeyParameters.getParameters());
    }

    public int hashCode() {
        int i = !isPrivate() ? 1 : 0;
        if (this.params != null) {
            return i ^ this.params.hashCode();
        }
        return i;
    }
}
