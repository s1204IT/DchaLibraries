package org.apache.harmony.security.x509;

import org.apache.harmony.security.utils.Array;

public class ExtensionValue {
    protected byte[] encoding;

    public ExtensionValue() {
    }

    public ExtensionValue(byte[] encoding) {
        this.encoding = encoding;
    }

    public byte[] getEncoded() {
        return this.encoding;
    }

    public void dumpValue(StringBuilder sb, String prefix) {
        sb.append(prefix).append("Unparseable extension value:\n");
        if (this.encoding == null) {
            this.encoding = getEncoded();
        }
        if (this.encoding == null) {
            sb.append("NULL\n");
        } else {
            sb.append(Array.toString(this.encoding, prefix));
        }
    }

    public void dumpValue(StringBuilder sb) {
        dumpValue(sb, "");
    }
}
