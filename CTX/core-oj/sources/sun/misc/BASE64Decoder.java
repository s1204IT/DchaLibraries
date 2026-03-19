package sun.misc;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.util.Locale;

public class BASE64Decoder extends CharacterDecoder {
    private static final char[] pem_array = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', Locale.PRIVATE_USE_EXTENSION, 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/'};
    private static final byte[] pem_convert_array = new byte[256];
    byte[] decode_buffer = new byte[4];

    @Override
    protected int bytesPerAtom() {
        return 4;
    }

    @Override
    protected int bytesPerLine() {
        return 72;
    }

    static {
        for (int i = 0; i < 255; i++) {
            pem_convert_array[i] = -1;
        }
        for (int i2 = 0; i2 < pem_array.length; i2++) {
            pem_convert_array[pem_array[i2]] = (byte) i2;
        }
    }

    @Override
    protected void decodeAtom(PushbackInputStream pushbackInputStream, OutputStream outputStream, int i) throws IOException {
        byte b;
        byte b2;
        byte b3;
        byte b4;
        if (i < 2) {
            throw new CEFormatException("BASE64Decoder: Not enough bytes for an atom.");
        }
        while (true) {
            int i2 = pushbackInputStream.read();
            byte b5 = -1;
            if (i2 == -1) {
                throw new CEStreamExhausted();
            }
            if (i2 != 10 && i2 != 13) {
                this.decode_buffer[0] = (byte) i2;
                if (readFully(pushbackInputStream, this.decode_buffer, 1, i - 1) == -1) {
                    throw new CEStreamExhausted();
                }
                if (i > 3 && this.decode_buffer[3] == 61) {
                    i = 3;
                }
                if (i > 2 && this.decode_buffer[2] == 61) {
                    i = 2;
                }
                switch (i) {
                    case 2:
                        b = -1;
                        b2 = pem_convert_array[this.decode_buffer[1] & Character.DIRECTIONALITY_UNDEFINED];
                        b3 = b;
                        b4 = b5;
                        b5 = pem_convert_array[this.decode_buffer[0] & Character.DIRECTIONALITY_UNDEFINED];
                        break;
                    case 4:
                        b5 = pem_convert_array[this.decode_buffer[3] & Character.DIRECTIONALITY_UNDEFINED];
                    case 3:
                        byte b6 = b5;
                        b5 = pem_convert_array[this.decode_buffer[2] & Character.DIRECTIONALITY_UNDEFINED];
                        b = b6;
                        b2 = pem_convert_array[this.decode_buffer[1] & Character.DIRECTIONALITY_UNDEFINED];
                        b3 = b;
                        b4 = b5;
                        b5 = pem_convert_array[this.decode_buffer[0] & Character.DIRECTIONALITY_UNDEFINED];
                        break;
                    default:
                        b4 = -1;
                        b2 = -1;
                        b3 = -1;
                        break;
                }
                switch (i) {
                    case 2:
                        outputStream.write((byte) (((b5 << 2) & 252) | ((b2 >>> 4) & 3)));
                        return;
                    case 3:
                        outputStream.write((byte) (((b5 << 2) & 252) | (3 & (b2 >>> 4))));
                        outputStream.write((byte) (((b4 >>> 2) & 15) | ((b2 << 4) & 240)));
                        return;
                    case 4:
                        outputStream.write((byte) (((b5 << 2) & 252) | ((b2 >>> 4) & 3)));
                        outputStream.write((byte) (((b2 << 4) & 240) | ((b4 >>> 2) & 15)));
                        outputStream.write((byte) (((b4 << 6) & 192) | (b3 & 63)));
                        return;
                    default:
                        return;
                }
            }
        }
    }
}
