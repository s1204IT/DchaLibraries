package android.util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class BackupUtils {
    public static final int NOT_NULL = 1;
    public static final int NULL = 0;

    public static class BadVersionException extends Exception {
        public BadVersionException(String str) {
            super(str);
        }
    }

    public static String readString(DataInputStream dataInputStream) throws IOException {
        if (dataInputStream.readByte() == 1) {
            return dataInputStream.readUTF();
        }
        return null;
    }

    public static void writeString(DataOutputStream dataOutputStream, String str) throws IOException {
        if (str != null) {
            dataOutputStream.writeByte(1);
            dataOutputStream.writeUTF(str);
        } else {
            dataOutputStream.writeByte(0);
        }
    }
}
