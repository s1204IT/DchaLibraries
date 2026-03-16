package org.apache.harmony.security.asn1;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public abstract class ASN1StringType extends ASN1Type {
    public static final ASN1StringType BMPSTRING = new ASN1StringType(30) {
    };
    public static final ASN1StringType IA5STRING = new ASN1StringType(22) {
    };
    public static final ASN1StringType GENERALSTRING = new ASN1StringType(27) {
    };
    public static final ASN1StringType PRINTABLESTRING = new ASN1StringType(19) {
    };
    public static final ASN1StringType TELETEXSTRING = new ASN1StringUTF8Type(20) {
    };
    public static final ASN1StringType UNIVERSALSTRING = new ASN1StringType(28) {
    };
    public static final ASN1StringType UTF8STRING = new ASN1StringUTF8Type(12) {
    };

    private static class ASN1StringUTF8Type extends ASN1StringType {
        public ASN1StringUTF8Type(int tagNumber) {
            super(tagNumber);
        }

        @Override
        public Object getDecodedObject(BerInputStream in) throws IOException {
            return new String(in.buffer, in.contentOffset, in.length, StandardCharsets.UTF_8);
        }

        @Override
        public void setEncodingContent(BerOutputStream out) {
            byte[] bytes = ((String) out.content).getBytes(StandardCharsets.UTF_8);
            out.content = bytes;
            out.length = bytes.length;
        }
    }

    public ASN1StringType(int tagNumber) {
        super(tagNumber);
    }

    @Override
    public final boolean checkTag(int identifier) {
        return this.id == identifier || this.constrId == identifier;
    }

    @Override
    public Object decode(BerInputStream in) throws IOException {
        in.readString(this);
        if (in.isVerify) {
            return null;
        }
        return getDecodedObject(in);
    }

    @Override
    public Object getDecodedObject(BerInputStream in) throws IOException {
        return new String(in.buffer, in.contentOffset, in.length, StandardCharsets.ISO_8859_1);
    }

    @Override
    public void encodeASN(BerOutputStream out) {
        out.encodeTag(this.id);
        encodeContent(out);
    }

    @Override
    public void encodeContent(BerOutputStream out) {
        out.encodeString();
    }

    @Override
    public void setEncodingContent(BerOutputStream out) {
        byte[] bytes = ((String) out.content).getBytes(StandardCharsets.UTF_8);
        out.content = bytes;
        out.length = bytes.length;
    }
}
