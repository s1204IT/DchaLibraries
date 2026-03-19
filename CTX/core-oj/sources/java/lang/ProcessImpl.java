package java.lang;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.ProcessBuilder;
import java.util.Map;

final class ProcessImpl {
    static final boolean $assertionsDisabled = false;

    private ProcessImpl() {
    }

    private static byte[] toCString(String str) {
        if (str == null) {
            return null;
        }
        byte[] bytes = str.getBytes();
        byte[] bArr = new byte[bytes.length + 1];
        System.arraycopy(bytes, 0, bArr, 0, bytes.length);
        bArr[bArr.length - 1] = 0;
        return bArr;
    }

    static Process start(String[] strArr, Map<String, String> map, String str, ProcessBuilder.Redirect[] redirectArr, boolean z) throws Throwable {
        FileOutputStream fileOutputStream;
        FileInputStream fileInputStream;
        FileOutputStream fileOutputStream2;
        int[] iArr;
        FileOutputStream fileOutputStream3;
        byte[][] bArr = new byte[strArr.length - 1][];
        int length = bArr.length;
        int i = 0;
        while (i < bArr.length) {
            int i2 = i + 1;
            bArr[i] = strArr[i2].getBytes();
            length += bArr[i].length;
            i = i2;
        }
        byte[] bArr2 = new byte[length];
        int length2 = 0;
        for (byte[] bArr3 : bArr) {
            System.arraycopy(bArr3, 0, bArr2, length2, bArr3.length);
            length2 += bArr3.length + 1;
        }
        int[] iArr2 = new int[1];
        byte[] environmentBlock = ProcessEnvironment.toEnvironmentBlock(map, iArr2);
        FileOutputStream fileOutputStream4 = null;
        FileInputStream fileInputStream2 = null;
        fileOutputStream4 = null;
        try {
            if (redirectArr == null) {
                iArr = new int[]{-1, -1, -1};
                fileOutputStream3 = null;
                fileOutputStream = null;
            } else {
                int[] iArr3 = new int[3];
                if (redirectArr[0] == ProcessBuilder.Redirect.PIPE) {
                    iArr3[0] = -1;
                } else if (redirectArr[0] == ProcessBuilder.Redirect.INHERIT) {
                    iArr3[0] = 0;
                } else {
                    fileInputStream = new FileInputStream(redirectArr[0].file());
                    try {
                        iArr3[0] = fileInputStream.getFD().getInt$();
                        if (redirectArr[1] != ProcessBuilder.Redirect.PIPE) {
                            iArr3[1] = -1;
                        } else if (redirectArr[1] == ProcessBuilder.Redirect.INHERIT) {
                            iArr3[1] = 1;
                        } else {
                            fileOutputStream2 = new FileOutputStream(redirectArr[1].file(), redirectArr[1].append());
                            try {
                                iArr3[1] = fileOutputStream2.getFD().getInt$();
                                if (redirectArr[2] == ProcessBuilder.Redirect.PIPE) {
                                    iArr3[2] = -1;
                                } else if (redirectArr[2] == ProcessBuilder.Redirect.INHERIT) {
                                    iArr3[2] = 2;
                                } else {
                                    FileOutputStream fileOutputStream5 = new FileOutputStream(redirectArr[2].file(), redirectArr[2].append());
                                    try {
                                        iArr3[2] = fileOutputStream5.getFD().getInt$();
                                        iArr = iArr3;
                                        fileInputStream2 = fileInputStream;
                                        fileOutputStream3 = fileOutputStream2;
                                        fileOutputStream = fileOutputStream5;
                                    } catch (Throwable th) {
                                        th = th;
                                        fileOutputStream4 = fileOutputStream2;
                                        fileOutputStream = fileOutputStream5;
                                        if (fileInputStream != null) {
                                        }
                                        if (fileOutputStream4 != null) {
                                        }
                                        if (fileOutputStream != null) {
                                        }
                                        throw th;
                                    }
                                }
                                iArr = iArr3;
                                fileOutputStream = null;
                                fileInputStream2 = fileInputStream;
                                fileOutputStream3 = fileOutputStream2;
                            } catch (Throwable th2) {
                                th = th2;
                                fileOutputStream = null;
                                fileOutputStream4 = fileOutputStream2;
                                if (fileInputStream != null) {
                                }
                                if (fileOutputStream4 != null) {
                                }
                                if (fileOutputStream != null) {
                                }
                                throw th;
                            }
                        }
                        fileOutputStream2 = null;
                        if (redirectArr[2] == ProcessBuilder.Redirect.PIPE) {
                        }
                        iArr = iArr3;
                        fileOutputStream = null;
                        fileInputStream2 = fileInputStream;
                        fileOutputStream3 = fileOutputStream2;
                    } catch (Throwable th3) {
                        th = th3;
                        fileOutputStream = null;
                        if (fileInputStream != null) {
                            try {
                                fileInputStream.close();
                            } catch (Throwable th4) {
                                if (fileOutputStream4 != null) {
                                    try {
                                        fileOutputStream4.close();
                                    } finally {
                                        if (fileOutputStream != null) {
                                            fileOutputStream.close();
                                        }
                                    }
                                }
                                if (fileOutputStream != null) {
                                    fileOutputStream.close();
                                }
                                throw th4;
                            }
                        }
                        if (fileOutputStream4 != null) {
                            try {
                                fileOutputStream4.close();
                            } finally {
                                if (fileOutputStream != null) {
                                    fileOutputStream.close();
                                }
                            }
                        }
                        if (fileOutputStream != null) {
                            fileOutputStream.close();
                        }
                        throw th;
                    }
                }
                fileInputStream = null;
                if (redirectArr[1] != ProcessBuilder.Redirect.PIPE) {
                }
                fileOutputStream2 = null;
                if (redirectArr[2] == ProcessBuilder.Redirect.PIPE) {
                }
                iArr = iArr3;
                fileOutputStream = null;
                fileInputStream2 = fileInputStream;
                fileOutputStream3 = fileOutputStream2;
            }
            try {
                UNIXProcess uNIXProcess = new UNIXProcess(toCString(strArr[0]), bArr2, bArr.length, environmentBlock, iArr2[0], toCString(str), iArr, z);
                if (fileInputStream2 != null) {
                    try {
                        fileInputStream2.close();
                    } catch (Throwable th5) {
                        if (fileOutputStream3 != null) {
                            try {
                                fileOutputStream3.close();
                            } finally {
                            }
                        }
                        throw th5;
                    }
                }
                if (fileOutputStream3 != null) {
                    try {
                        fileOutputStream3.close();
                    } finally {
                        if (fileOutputStream != null) {
                            fileOutputStream.close();
                        }
                    }
                }
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
                return uNIXProcess;
            } catch (Throwable th6) {
                th = th6;
                fileInputStream = fileInputStream2;
                fileOutputStream4 = fileOutputStream3;
                if (fileInputStream != null) {
                }
                if (fileOutputStream4 != null) {
                }
                if (fileOutputStream != null) {
                }
                throw th;
            }
        } catch (Throwable th7) {
            th = th7;
            fileOutputStream = null;
            fileInputStream = null;
        }
    }
}
