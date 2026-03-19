package com.android.gallery3d.common;

import android.content.Context;
import android.media.ExifInterface;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ExifOrientation {
    public static final int readRotation(InputStream inputStream, Context context) throws Throwable {
        DataOutputStream dataOutputStream;
        File file;
        File fileCreateTempFile;
        DataOutputStream dataOutputStream2;
        File file2 = null;
        file2 = null;
        DataOutputStream dataOutputStream3 = null;
        file2 = null;
        try {
            DataInputStream dataInputStream = new DataInputStream(inputStream);
            if (dataInputStream.readShort() == -40) {
                short s = dataInputStream.readShort();
                int i = 4;
                dataOutputStream = null;
                while (s >= -32 && s <= -17 && i < 131072) {
                    try {
                        int unsignedShort = dataInputStream.readUnsignedShort();
                        if (unsignedShort < 2) {
                            throw new IOException("Invalid header size");
                        }
                        if (unsignedShort > 2) {
                            if (s == -31) {
                                if (file2 == null) {
                                    fileCreateTempFile = File.createTempFile("ExifOrientation", ".jpg", context.getCacheDir());
                                    try {
                                        dataOutputStream2 = new DataOutputStream(new FileOutputStream(fileCreateTempFile));
                                        try {
                                            dataOutputStream2.writeShort(-40);
                                        } catch (IOException e) {
                                            dataOutputStream = dataOutputStream2;
                                            file2 = fileCreateTempFile;
                                            Utils.closeSilently(inputStream);
                                            Utils.closeSilently(dataOutputStream);
                                            if (file2 != null) {
                                                return 0;
                                            }
                                            file2.delete();
                                            return 0;
                                        } catch (Throwable th) {
                                            th = th;
                                            dataOutputStream = dataOutputStream2;
                                            file2 = fileCreateTempFile;
                                            Utils.closeSilently(inputStream);
                                            Utils.closeSilently(dataOutputStream);
                                            if (file2 != null) {
                                                file2.delete();
                                            }
                                            throw th;
                                        }
                                    } catch (IOException e2) {
                                        file2 = fileCreateTempFile;
                                        Utils.closeSilently(inputStream);
                                        Utils.closeSilently(dataOutputStream);
                                        if (file2 != null) {
                                        }
                                    } catch (Throwable th2) {
                                        th = th2;
                                        file2 = fileCreateTempFile;
                                        Utils.closeSilently(inputStream);
                                        Utils.closeSilently(dataOutputStream);
                                        if (file2 != null) {
                                        }
                                        throw th;
                                    }
                                } else {
                                    fileCreateTempFile = file2;
                                    dataOutputStream2 = dataOutputStream;
                                }
                                dataOutputStream2.writeShort(s);
                                dataOutputStream2.writeShort(unsignedShort);
                                byte[] bArr = new byte[unsignedShort - 2];
                                dataInputStream.read(bArr);
                                dataOutputStream2.write(bArr);
                                dataOutputStream = dataOutputStream2;
                                file2 = fileCreateTempFile;
                            } else {
                                dataInputStream.skip(unsignedShort - 2);
                            }
                        }
                        s = dataInputStream.readShort();
                        i = i + unsignedShort + 2;
                    } catch (IOException e3) {
                    } catch (Throwable th3) {
                        th = th3;
                    }
                }
                if (dataOutputStream != null) {
                    dataOutputStream.writeShort(-38);
                    dataOutputStream.writeShort(2);
                    dataOutputStream.writeShort(-39);
                    dataOutputStream.close();
                    int rotation = readRotation(file2.getAbsolutePath());
                    Utils.closeSilently(inputStream);
                    Utils.closeSilently(dataOutputStream);
                    if (file2 != null) {
                        file2.delete();
                    }
                    return rotation;
                }
                file = file2;
                dataOutputStream3 = dataOutputStream;
            } else {
                file = null;
            }
            Utils.closeSilently(inputStream);
            Utils.closeSilently(dataOutputStream3);
            if (file == null) {
                return 0;
            }
            file.delete();
            return 0;
        } catch (IOException e4) {
            dataOutputStream = null;
        } catch (Throwable th4) {
            th = th4;
            dataOutputStream = null;
        }
    }

    public static final int readRotation(String str) {
        try {
            int attributeInt = new ExifInterface(str).getAttributeInt("Orientation", 0);
            if (attributeInt == 3) {
                return 180;
            }
            if (attributeInt == 6) {
                return 90;
            }
            if (attributeInt != 8) {
                return 0;
            }
            return 270;
        } catch (IOException e) {
            return 0;
        }
    }
}
