package com.mediatek.plugin.zip;

import com.mediatek.plugin.utils.ReflectUtils;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipCenter {
    private static HashMap<String, Class<? extends ZipFile>> sFileList = new HashMap<>();
    private static ReadWriteLock sFileListLock = new ReentrantReadWriteLock();

    public static void registerZipFile(String str, Class<? extends ZipFile> cls) {
        sFileListLock.writeLock().lock();
        sFileList.put(str, cls);
        sFileListLock.writeLock().unlock();
    }

    public static ZipFile createZipFile(String str) {
        if (!isValidZipFileName(str)) {
            return null;
        }
        sFileListLock.readLock().lock();
        for (Map.Entry<String, Class<? extends ZipFile>> entry : sFileList.entrySet()) {
            if (str.endsWith(entry.getKey())) {
                Constructor<?> constructor = ReflectUtils.getConstructor(entry.getValue(), (Class<?>[]) new Class[]{str.getClass()});
                sFileListLock.readLock().unlock();
                return (ZipFile) ReflectUtils.createInstance(constructor, str);
            }
        }
        sFileListLock.readLock().unlock();
        return null;
    }

    private static boolean isValidZipFileName(String str) throws Throwable {
        FileInputStream fileInputStream;
        ZipInputStream zipInputStream;
        ZipInputStream zipInputStream2;
        ZipInputStream zipInputStream3;
        FileInputStream fileInputStream2;
        FileInputStream fileInputStream3;
        ZipEntry nextEntry;
        ZipInputStream zipInputStream4 = null;
        zipInputStream4 = null;
        FileInputStream fileInputStream4 = null;
        FileInputStream fileInputStream5 = null;
        try {
            try {
                fileInputStream = new FileInputStream(str);
                try {
                    ZipInputStream zipInputStream5 = new ZipInputStream(fileInputStream);
                    do {
                        try {
                            nextEntry = zipInputStream5.getNextEntry();
                            if (nextEntry == null) {
                                try {
                                    fileInputStream.close();
                                    zipInputStream5.close();
                                    return true;
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    return true;
                                }
                            }
                        } catch (FileNotFoundException e2) {
                            fileInputStream3 = fileInputStream;
                            zipInputStream2 = zipInputStream5;
                            e = e2;
                            fileInputStream4 = fileInputStream3;
                            e.printStackTrace();
                            if (fileInputStream4 != null) {
                                try {
                                    fileInputStream4.close();
                                } catch (IOException e3) {
                                    e3.printStackTrace();
                                    return false;
                                }
                            }
                            if (zipInputStream2 != null) {
                                zipInputStream2.close();
                            }
                            return false;
                        } catch (IOException e4) {
                            fileInputStream2 = fileInputStream;
                            zipInputStream = zipInputStream5;
                            e = e4;
                            fileInputStream5 = fileInputStream2;
                            e.printStackTrace();
                            if (fileInputStream5 != null) {
                                try {
                                    fileInputStream5.close();
                                } catch (IOException e5) {
                                    e5.printStackTrace();
                                    return false;
                                }
                            }
                            if (zipInputStream != null) {
                                zipInputStream.close();
                            }
                            return false;
                        } catch (Throwable th) {
                            zipInputStream4 = zipInputStream5;
                            th = th;
                            if (fileInputStream != null) {
                                try {
                                    fileInputStream.close();
                                } catch (IOException e6) {
                                    e6.printStackTrace();
                                    throw th;
                                }
                            }
                            if (zipInputStream4 != null) {
                                zipInputStream4.close();
                            }
                            throw th;
                        }
                    } while (!nextEntry.getName().contains("../"));
                    fileInputStream.close();
                    zipInputStream5.close();
                    return false;
                } catch (FileNotFoundException e7) {
                    e = e7;
                    fileInputStream3 = fileInputStream;
                    zipInputStream2 = null;
                } catch (IOException e8) {
                    e = e8;
                    fileInputStream2 = fileInputStream;
                    zipInputStream = null;
                } catch (Throwable th2) {
                    th = th2;
                }
            } catch (Throwable th3) {
                th = th3;
                fileInputStream = null;
                zipInputStream4 = zipInputStream3;
            }
        } catch (FileNotFoundException e9) {
            e = e9;
            zipInputStream2 = null;
        } catch (IOException e10) {
            e = e10;
            zipInputStream = null;
        } catch (Throwable th4) {
            th = th4;
            fileInputStream = null;
        }
    }
}
