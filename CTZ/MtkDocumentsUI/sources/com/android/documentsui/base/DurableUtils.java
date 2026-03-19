package com.android.documentsui.base;

import android.os.BadParcelableException;
import android.os.Parcel;
import android.util.Log;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class DurableUtils {
    public static <D extends Durable> byte[] writeToArray(D d) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        d.write(new DataOutputStream(byteArrayOutputStream));
        return byteArrayOutputStream.toByteArray();
    }

    public static <D extends Durable> D readFromArray(byte[] bArr, D d) throws IOException {
        if (bArr == null) {
            throw new IOException("Missing data");
        }
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bArr);
        d.reset();
        try {
            d.read(new DataInputStream(byteArrayInputStream));
            return d;
        } catch (IOException e) {
            d.reset();
            throw e;
        }
    }

    public static <D extends Durable> byte[] writeToArrayOrNull(D d) {
        try {
            return writeToArray(d);
        } catch (IOException e) {
            Log.w("Documents", "Failed to write", e);
            return null;
        }
    }

    public static <D extends Durable> void writeToParcel(Parcel parcel, D d) {
        try {
            parcel.writeByteArray(writeToArray(d));
        } catch (IOException e) {
            throw new BadParcelableException(e);
        }
    }

    public static <D extends Durable> D readFromParcel(Parcel parcel, D d) {
        try {
            return (D) readFromArray(parcel.createByteArray(), d);
        } catch (IOException e) {
            throw new BadParcelableException(e);
        }
    }

    public static void writeNullableString(DataOutputStream dataOutputStream, String str) throws IOException {
        if (str != null) {
            dataOutputStream.write(1);
            dataOutputStream.writeUTF(str);
        } else {
            dataOutputStream.write(0);
        }
    }

    public static String readNullableString(DataInputStream dataInputStream) throws IOException {
        if (dataInputStream.read() != 0) {
            return dataInputStream.readUTF();
        }
        return null;
    }
}
