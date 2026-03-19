package com.android.org.bouncycastle.util.io.pem;

import com.android.org.bouncycastle.util.Strings;
import com.android.org.bouncycastle.util.encoders.Base64;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;

public class PemWriter extends BufferedWriter {
    private static final int LINE_LENGTH = 64;
    private char[] buf;
    private final int nlLength;

    public PemWriter(Writer writer) {
        super(writer);
        this.buf = new char[64];
        String strLineSeparator = Strings.lineSeparator();
        if (strLineSeparator != null) {
            this.nlLength = strLineSeparator.length();
        } else {
            this.nlLength = 2;
        }
    }

    public int getOutputSize(PemObject pemObject) {
        int length = ((pemObject.getType().length() + 10 + this.nlLength) * 2) + 6 + 4;
        if (!pemObject.getHeaders().isEmpty()) {
            for (PemHeader pemHeader : pemObject.getHeaders()) {
                length += pemHeader.getName().length() + ": ".length() + pemHeader.getValue().length() + this.nlLength;
            }
            length += this.nlLength;
        }
        return length + (((pemObject.getContent().length + 2) / 3) * 4) + ((((r7 + 64) - 1) / 64) * this.nlLength);
    }

    public void writeObject(PemObjectGenerator pemObjectGenerator) throws IOException {
        PemObject pemObjectGenerate = pemObjectGenerator.generate();
        writePreEncapsulationBoundary(pemObjectGenerate.getType());
        if (!pemObjectGenerate.getHeaders().isEmpty()) {
            for (PemHeader pemHeader : pemObjectGenerate.getHeaders()) {
                write(pemHeader.getName());
                write(": ");
                write(pemHeader.getValue());
                newLine();
            }
            newLine();
        }
        writeEncoded(pemObjectGenerate.getContent());
        writePostEncapsulationBoundary(pemObjectGenerate.getType());
    }

    private void writeEncoded(byte[] bArr) throws IOException {
        int i;
        byte[] bArrEncode = Base64.encode(bArr);
        int length = 0;
        while (length < bArrEncode.length) {
            int i2 = 0;
            while (i2 != this.buf.length && (i = length + i2) < bArrEncode.length) {
                this.buf[i2] = (char) bArrEncode[i];
                i2++;
            }
            write(this.buf, 0, i2);
            newLine();
            length += this.buf.length;
        }
    }

    private void writePreEncapsulationBoundary(String str) throws IOException {
        write("-----BEGIN " + str + "-----");
        newLine();
    }

    private void writePostEncapsulationBoundary(String str) throws IOException {
        write("-----END " + str + "-----");
        newLine();
    }
}
