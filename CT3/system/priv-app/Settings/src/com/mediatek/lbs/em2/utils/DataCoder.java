package com.mediatek.lbs.em2.utils;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
/* loaded from: classes.dex */
public class DataCoder {
    public static void putBoolean(BufferedOutputStream out, boolean data) throws IOException {
        putByte(out, !data ? (byte) 0 : (byte) 1);
    }

    public static void putByte(BufferedOutputStream out, byte data) throws IOException {
        out.write(data);
    }

    public static void putShort(BufferedOutputStream out, short data) throws IOException {
        putByte(out, (byte) (data & 255));
        putByte(out, (byte) ((data >> 8) & 255));
    }

    public static void putInt(BufferedOutputStream out, int data) throws IOException {
        putShort(out, (short) (data & 65535));
        putShort(out, (short) ((data >> 16) & 65535));
    }

    public static void putString(BufferedOutputStream out, String data) throws IOException {
        if (data == null) {
            putByte(out, (byte) 0);
            return;
        }
        putByte(out, (byte) 1);
        byte[] output = data.getBytes();
        putInt(out, output.length + 1);
        out.write(output);
        putByte(out, (byte) 0);
    }

    public static boolean getBoolean(DataInputStream in) throws IOException {
        return in.readByte() != 0;
    }

    public static byte getByte(DataInputStream in) throws IOException {
        return in.readByte();
    }

    public static short getShort(DataInputStream in) throws IOException {
        short ret = (short) ((getByte(in) & 255) | 0);
        return (short) ((getByte(in) << 8) | ret);
    }

    public static int getInt(DataInputStream in) throws IOException {
        int ret = (getShort(in) & 65535) | 0;
        return ret | (getShort(in) << 16);
    }

    public static String getString(DataInputStream in) throws IOException {
        if (getByte(in) != 0) {
            int len = getInt(in);
            byte[] buff = new byte[len];
            in.readFully(buff, 0, len);
            return new String(buff).trim();
        }
        return null;
    }

    public static byte[] getBinary(DataInputStream in) throws IOException {
        int len = getInt(in);
        byte[] buff = new byte[len];
        in.readFully(buff, 0, len);
        return buff;
    }
}
