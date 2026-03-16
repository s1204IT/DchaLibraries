package org.apache.harmony.security.asn1;

import java.io.IOException;
import libcore.util.EmptyArray;

public class ASN1BitString extends ASN1StringType {
    private static final ASN1BitString ASN1 = new ASN1BitString();

    public ASN1BitString() {
        super(3);
    }

    public static ASN1BitString getInstance() {
        return ASN1;
    }

    @Override
    public Object decode(BerInputStream in) throws IOException {
        in.readBitString();
        if (in.isVerify) {
            return null;
        }
        return getDecodedObject(in);
    }

    @Override
    public Object getDecodedObject(BerInputStream in) throws IOException {
        byte[] bytes = new byte[in.length - 1];
        System.arraycopy(in.buffer, in.contentOffset + 1, bytes, 0, in.length - 1);
        return new BitString(bytes, in.buffer[in.contentOffset]);
    }

    @Override
    public void encodeContent(BerOutputStream out) {
        out.encodeBitString();
    }

    @Override
    public void setEncodingContent(BerOutputStream out) {
        out.length = ((BitString) out.content).bytes.length + 1;
    }

    public static class ASN1NamedBitList extends ASN1BitString {
        private static final int INDEFINITE_SIZE = -1;
        private static final byte[] SET_MASK = {Byte.MIN_VALUE, 64, 32, 16, 8, 4, 2, 1};
        private static final BitString emptyString = new BitString(EmptyArray.BYTE, 0);
        private final int maxBits = -1;
        private final int minBits;

        public ASN1NamedBitList(int minBits) {
            this.minBits = minBits;
        }

        @Override
        public Object getDecodedObject(BerInputStream in) throws IOException {
            boolean[] value;
            int unusedBits = in.buffer[in.contentOffset];
            int bitsNumber = ((in.length - 1) * 8) - unusedBits;
            if (this.maxBits == -1) {
                if (this.minBits == -1) {
                    value = new boolean[bitsNumber];
                } else if (bitsNumber > this.minBits) {
                    value = new boolean[bitsNumber];
                } else {
                    value = new boolean[this.minBits];
                }
            } else {
                if (bitsNumber > this.maxBits) {
                    throw new ASN1Exception("ASN.1 Named Bitstring: size constraints");
                }
                value = new boolean[this.maxBits];
            }
            if (bitsNumber != 0) {
                int i = 1;
                int j = 0;
                byte octet = in.buffer[in.contentOffset + 1];
                int size = in.length - 1;
                while (i < size) {
                    int k = 0;
                    while (k < 8) {
                        value[j] = (SET_MASK[k] & octet) != 0;
                        k++;
                        j++;
                    }
                    int i2 = i + 1;
                    octet = in.buffer[in.contentOffset + i2];
                    i = i2 + 1;
                }
                int k2 = 0;
                while (k2 < 8 - unusedBits) {
                    value[j] = (SET_MASK[k2] & octet) != 0;
                    k2++;
                    j++;
                }
            }
            return value;
        }

        @Override
        public void setEncodingContent(BerOutputStream out) {
            boolean[] toEncode = (boolean[]) out.content;
            int index = toEncode.length - 1;
            while (index > -1 && !toEncode[index]) {
                index--;
            }
            if (index == -1) {
                out.content = emptyString;
                out.length = 1;
                return;
            }
            int unusedBits = 7 - (index % 8);
            byte[] bytes = new byte[(index / 8) + 1];
            int j = 0;
            int index2 = bytes.length - 1;
            for (int i = 0; i < index2; i++) {
                int k = 0;
                while (k < 8) {
                    if (toEncode[j]) {
                        bytes[i] = (byte) (bytes[i] | SET_MASK[k]);
                    }
                    k++;
                    j++;
                }
            }
            int k2 = 0;
            while (k2 < 8 - unusedBits) {
                if (toEncode[j]) {
                    bytes[index2] = (byte) (bytes[index2] | SET_MASK[k2]);
                }
                k2++;
                j++;
            }
            out.content = new BitString(bytes, unusedBits);
            out.length = bytes.length + 1;
        }
    }
}
