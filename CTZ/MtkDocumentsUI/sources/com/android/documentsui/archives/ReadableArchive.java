package com.android.documentsui.archives;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Point;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.os.storage.StorageManager;
import android.util.Log;
import android.util.jar.StrictJarFile;
import com.android.documentsui.archives.Archive;
import com.android.internal.util.Preconditions;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.zip.ZipEntry;
import libcore.io.IoUtils;

public class ReadableArchive extends Archive {
    private final StorageManager mStorageManager;
    private final StrictJarFile mZipFile;

    private ReadableArchive(Context context, File file, FileDescriptor fileDescriptor, Uri uri, int i, Uri uri2) throws IOException {
        StrictJarFile strictJarFile;
        super(context, uri, i, uri2);
        if (!supportsAccessMode(i)) {
            throw new IllegalStateException("Unsupported access mode.");
        }
        this.mStorageManager = (StorageManager) this.mContext.getSystemService(StorageManager.class);
        if (file == null) {
            strictJarFile = new StrictJarFile(fileDescriptor, false, false);
        } else {
            strictJarFile = new StrictJarFile(file.getPath(), false, false);
        }
        this.mZipFile = strictJarFile;
        Stack stack = new Stack();
        for (ZipEntry zipEntry : this.mZipFile) {
            if (zipEntry.isDirectory() != zipEntry.getName().endsWith("/")) {
                throw new IOException("Directories must have a trailing slash, and files must not.");
            }
            String entryPath = getEntryPath(zipEntry);
            if (this.mEntries.containsKey(entryPath)) {
                throw new IOException("Multiple entries with the same name are not supported.");
            }
            this.mEntries.put(entryPath, zipEntry);
            if (zipEntry.isDirectory()) {
                this.mTree.put(entryPath, new ArrayList());
            }
            if (!"/".equals(entryPath)) {
                stack.push(zipEntry);
            }
        }
        while (stack.size() > 0) {
            ZipEntry zipEntry2 = (ZipEntry) stack.pop();
            String entryPath2 = getEntryPath(zipEntry2);
            String str = entryPath2.substring(0, entryPath2.lastIndexOf(47, zipEntry2.isDirectory() ? entryPath2.length() - 2 : entryPath2.length() - 1)) + "/";
            List<ZipEntry> arrayList = this.mTree.get(str);
            if (arrayList == null) {
                ZipEntry zipEntry3 = new ZipEntry(str);
                zipEntry3.setSize(0L);
                zipEntry3.setTime(zipEntry2.getTime());
                this.mEntries.put(str, zipEntry3);
                if (!"/".equals(str)) {
                    stack.push(zipEntry3);
                }
                arrayList = new ArrayList<>();
                this.mTree.put(str, arrayList);
            }
            arrayList.add(zipEntry2);
        }
    }

    public static boolean supportsAccessMode(int i) {
        return i == 268435456;
    }

    public static ReadableArchive createForParcelFileDescriptor(Context context, ParcelFileDescriptor parcelFileDescriptor, Uri uri, int i, Uri uri2) throws Exception {
        ParcelFileDescriptor parcelFileDescriptor2;
        File fileCreateTempFile;
        Throwable th;
        Throwable th2;
        FileDescriptor fileDescriptor = null;
        try {
            try {
                if (canSeek(parcelFileDescriptor)) {
                    FileDescriptor fileDescriptor2 = new FileDescriptor();
                    try {
                        fileDescriptor2.setInt$(parcelFileDescriptor.detachFd());
                        return new ReadableArchive(context, null, fileDescriptor2, uri, i, uri2);
                    } catch (Exception e) {
                        e = e;
                        parcelFileDescriptor2 = parcelFileDescriptor;
                        fileDescriptor = fileDescriptor2;
                    }
                } else {
                    try {
                        fileCreateTempFile = File.createTempFile("com.android.documentsui.snapshot{", "}.zip", context.getCacheDir());
                        try {
                            try {
                                ParcelFileDescriptor.AutoCloseOutputStream autoCloseOutputStream = new ParcelFileDescriptor.AutoCloseOutputStream(ParcelFileDescriptor.open(fileCreateTempFile, 536870912));
                                try {
                                } catch (Throwable th3) {
                                    th = th3;
                                }
                                try {
                                    ParcelFileDescriptor.AutoCloseInputStream autoCloseInputStream = new ParcelFileDescriptor.AutoCloseInputStream(parcelFileDescriptor);
                                    try {
                                        byte[] bArr = new byte[32768];
                                        while (true) {
                                            int i2 = autoCloseInputStream.read(bArr);
                                            if (i2 == -1) {
                                                break;
                                            }
                                            autoCloseOutputStream.write(bArr, 0, i2);
                                        }
                                        autoCloseOutputStream.flush();
                                        $closeResource(null, autoCloseInputStream);
                                        $closeResource(null, autoCloseOutputStream);
                                        ReadableArchive readableArchive = new ReadableArchive(context, fileCreateTempFile, null, uri, i, uri2);
                                        if (fileCreateTempFile != null) {
                                            fileCreateTempFile.delete();
                                        }
                                        return readableArchive;
                                    } catch (Throwable th4) {
                                        th2 = th4;
                                        try {
                                            throw th2;
                                        } catch (Throwable th5) {
                                            th = th5;
                                            $closeResource(th2, autoCloseInputStream);
                                            throw th;
                                        }
                                    }
                                } catch (Throwable th6) {
                                    th = th6;
                                    th = null;
                                    $closeResource(th, autoCloseOutputStream);
                                    throw th;
                                }
                            } catch (Throwable th7) {
                                th = th7;
                                if (fileCreateTempFile != null) {
                                    fileCreateTempFile.delete();
                                }
                                throw th;
                            }
                        } catch (Throwable th8) {
                            th = th8;
                            if (fileCreateTempFile != null) {
                            }
                            throw th;
                        }
                    } catch (Throwable th9) {
                        th = th9;
                        fileCreateTempFile = null;
                    }
                }
            } catch (Exception e2) {
                e = e2;
            }
        } catch (Exception e3) {
            e = e3;
            parcelFileDescriptor2 = parcelFileDescriptor;
        }
        IoUtils.closeQuietly(parcelFileDescriptor2);
        IoUtils.closeQuietly(fileDescriptor);
        throw e;
    }

    private static void $closeResource(Throwable th, AutoCloseable autoCloseable) throws Exception {
        if (th == null) {
            autoCloseable.close();
            return;
        }
        try {
            autoCloseable.close();
        } catch (Throwable th2) {
            th.addSuppressed(th2);
        }
    }

    @Override
    public ParcelFileDescriptor openDocument(String str, String str2, CancellationSignal cancellationSignal) throws FileNotFoundException {
        Archive.MorePreconditions.checkArgumentEquals("r", str2, "Invalid mode. Only reading \"r\" supported, but got: \"%s\".");
        ArchiveId archiveIdFromDocumentId = ArchiveId.fromDocumentId(str);
        Archive.MorePreconditions.checkArgumentEquals(this.mArchiveUri, archiveIdFromDocumentId.mArchiveUri, "Mismatching archive Uri. Expected: %s, actual: %s.");
        ZipEntry zipEntry = this.mEntries.get(archiveIdFromDocumentId.mPath);
        if (zipEntry == null) {
            throw new FileNotFoundException();
        }
        try {
            return this.mStorageManager.openProxyFileDescriptor(268435456, new Proxy(this.mZipFile, zipEntry));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public AssetFileDescriptor openDocumentThumbnail(String str, Point point, CancellationSignal cancellationSignal) throws Throwable {
        ?? inputStream;
        ArchiveId archiveIdFromDocumentId = ArchiveId.fromDocumentId(str);
        Archive.MorePreconditions.checkArgumentEquals(this.mArchiveUri, archiveIdFromDocumentId.mArchiveUri, "Mismatching archive Uri. Expected: %s, actual: %s.");
        Preconditions.checkArgument(getDocumentType(str).startsWith("image/"), "Thumbnails only supported for image/* MIME type.");
        ZipEntry zipEntry = this.mEntries.get(archiveIdFromDocumentId.mPath);
        if (zipEntry == null) {
            throw new FileNotFoundException();
        }
        String str2 = null;
        Bundle bundle = null;
        ?? r5 = 0;
        try {
            try {
                inputStream = this.mZipFile.getInputStream(zipEntry);
            } catch (Throwable th) {
                th = th;
                inputStream = str2;
            }
        } catch (IOException e) {
            e = e;
        }
        try {
            ExifInterface exifInterface = new ExifInterface((InputStream) inputStream);
            if (exifInterface.hasThumbnail()) {
                int attributeInt = exifInterface.getAttributeInt("Orientation", -1);
                if (attributeInt == 3) {
                    bundle = new Bundle(1);
                    bundle.putInt("android.provider.extra.ORIENTATION", 180);
                } else if (attributeInt == 6) {
                    bundle = new Bundle(1);
                    bundle.putInt("android.provider.extra.ORIENTATION", 90);
                } else if (attributeInt == 8) {
                    bundle = new Bundle(1);
                    bundle.putInt("android.provider.extra.ORIENTATION", 270);
                }
                Bundle bundle2 = bundle;
                long[] thumbnailRange = exifInterface.getThumbnailRange();
                AssetFileDescriptor assetFileDescriptor = new AssetFileDescriptor(openDocument(str, "r", cancellationSignal), thumbnailRange[0], thumbnailRange[1], bundle2);
                IoUtils.closeQuietly((AutoCloseable) inputStream);
                return assetFileDescriptor;
            }
            IoUtils.closeQuietly((AutoCloseable) inputStream);
        } catch (IOException e2) {
            e = e2;
            r5 = inputStream;
            Log.e("ReadableArchive", "Failed to obtain thumbnail from EXIF.", e);
            IoUtils.closeQuietly((AutoCloseable) r5);
        } catch (Throwable th2) {
            th = th2;
            IoUtils.closeQuietly((AutoCloseable) inputStream);
            throw th;
        }
        str2 = "r";
        return new AssetFileDescriptor(openDocument(str, "r", cancellationSignal), 0L, zipEntry.getSize(), null);
    }

    @Override
    public void close() {
        try {
            this.mZipFile.close();
        } catch (IOException e) {
        }
    }
}
