package com.android.documentsui.archives;

import android.content.Context;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.OperationCanceledException;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import com.android.documentsui.archives.Archive;
import com.android.internal.annotations.GuardedBy;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import libcore.io.IoUtils;

public class WriteableArchive extends Archive {
    static final boolean $assertionsDisabled = false;
    private final ExecutorService mExecutor;
    private final ParcelFileDescriptor.AutoCloseOutputStream mOutputStream;

    @GuardedBy("mEntries")
    private final Set<String> mPendingEntries;

    @GuardedBy("mEntries")
    private final ZipOutputStream mZipOutputStream;

    private WriteableArchive(Context context, ParcelFileDescriptor parcelFileDescriptor, Uri uri, int i, Uri uri2) throws IOException {
        super(context, uri, i, uri2);
        this.mPendingEntries = new HashSet();
        this.mExecutor = Executors.newSingleThreadExecutor();
        if (!supportsAccessMode(i)) {
            throw new IllegalStateException("Unsupported access mode.");
        }
        addEntry(null, new ZipEntry("/"));
        this.mOutputStream = new ParcelFileDescriptor.AutoCloseOutputStream(parcelFileDescriptor);
        this.mZipOutputStream = new ZipOutputStream(this.mOutputStream);
    }

    private void addEntry(ZipEntry zipEntry, ZipEntry zipEntry2) {
        String entryPath = getEntryPath(zipEntry2);
        synchronized (this.mEntries) {
            if (zipEntry2.isDirectory() && !this.mTree.containsKey(entryPath)) {
                this.mTree.put(entryPath, new ArrayList());
            }
            this.mEntries.put(entryPath, zipEntry2);
            if (zipEntry != null) {
                this.mTree.get(getEntryPath(zipEntry)).add(zipEntry2);
            }
        }
    }

    public static boolean supportsAccessMode(int i) {
        return i == 536870912;
    }

    public static WriteableArchive createForParcelFileDescriptor(Context context, ParcelFileDescriptor parcelFileDescriptor, Uri uri, int i, Uri uri2) throws Exception {
        try {
            return new WriteableArchive(context, parcelFileDescriptor, uri, i, uri2);
        } catch (Exception e) {
            IoUtils.closeQuietly(parcelFileDescriptor);
            throw e;
        }
    }

    @Override
    public String createDocument(String str, String str2, String str3) throws FileNotFoundException {
        ZipEntry zipEntry;
        String entryPath;
        ArchiveId archiveIdFromDocumentId = ArchiveId.fromDocumentId(str);
        Archive.MorePreconditions.checkArgumentEquals(this.mArchiveUri, archiveIdFromDocumentId.mArchiveUri, "Mismatching archive Uri. Expected: %s, actual: %s.");
        boolean zEquals = "vnd.android.document/directory".equals(str2);
        synchronized (this.mEntries) {
            ZipEntry zipEntry2 = this.mEntries.get(archiveIdFromDocumentId.mPath);
            if (zipEntry2 == null) {
                throw new FileNotFoundException();
            }
            if (str3.indexOf("/") != -1 || ".".equals(str3) || "..".equals(str3)) {
                throw new IllegalStateException("Display name contains invalid characters.");
            }
            if ("".equals(str3)) {
                throw new IllegalStateException("Display name cannot be empty.");
            }
            String name = "/".equals(zipEntry2.getName()) ? "" : zipEntry2.getName();
            StringBuilder sb = new StringBuilder();
            sb.append(name);
            sb.append(str3);
            sb.append(zEquals ? "/" : "");
            zipEntry = new ZipEntry(sb.toString());
            entryPath = getEntryPath(zipEntry);
            zipEntry.setSize(0L);
            if (this.mEntries.get(entryPath) != null) {
                throw new IllegalStateException("The document already exist: " + entryPath);
            }
            addEntry(zipEntry2, zipEntry);
        }
        if (!zEquals) {
            synchronized (this.mEntries) {
                this.mPendingEntries.add(entryPath);
            }
        } else {
            try {
                synchronized (this.mEntries) {
                    this.mZipOutputStream.putNextEntry(zipEntry);
                }
            } catch (IOException e) {
                throw new IllegalStateException("Failed to create a file in the archive: " + entryPath, e);
            }
        }
        return createArchiveId(entryPath).toDocumentId();
    }

    @Override
    public ParcelFileDescriptor openDocument(String str, String str2, final CancellationSignal cancellationSignal) throws FileNotFoundException {
        final ZipEntry zipEntry;
        Archive.MorePreconditions.checkArgumentEquals("w", str2, "Invalid mode. Only writing \"w\" supported, but got: \"%s\".");
        ArchiveId archiveIdFromDocumentId = ArchiveId.fromDocumentId(str);
        Archive.MorePreconditions.checkArgumentEquals(this.mArchiveUri, archiveIdFromDocumentId.mArchiveUri, "Mismatching archive Uri. Expected: %s, actual: %s.");
        synchronized (this.mEntries) {
            zipEntry = this.mEntries.get(archiveIdFromDocumentId.mPath);
            if (zipEntry == null) {
                throw new FileNotFoundException();
            }
            if (!this.mPendingEntries.contains(archiveIdFromDocumentId.mPath)) {
                throw new IllegalStateException("Files can be written only once.");
            }
            this.mPendingEntries.remove(archiveIdFromDocumentId.mPath);
        }
        try {
            ParcelFileDescriptor[] parcelFileDescriptorArrCreateReliablePipe = ParcelFileDescriptor.createReliablePipe();
            final ParcelFileDescriptor parcelFileDescriptor = parcelFileDescriptorArrCreateReliablePipe[0];
            try {
                this.mExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            ParcelFileDescriptor.AutoCloseInputStream autoCloseInputStream = new ParcelFileDescriptor.AutoCloseInputStream(parcelFileDescriptor);
                            Throwable th = null;
                            try {
                                try {
                                } catch (IOException e) {
                                    try {
                                        Log.e("WriteableArchive", "Failed while writing to a file.", e);
                                        parcelFileDescriptor.closeWithError("Writing failure.");
                                    } catch (IOException e2) {
                                        Log.e("WriteableArchive", "Failed to close the pipe after an error.", e2);
                                    }
                                }
                                synchronized (WriteableArchive.this.mEntries) {
                                    WriteableArchive.this.mZipOutputStream.putNextEntry(zipEntry);
                                    byte[] bArr = new byte[32768];
                                    long j = 0;
                                    while (true) {
                                        int i = autoCloseInputStream.read(bArr);
                                        if (i == -1) {
                                            break;
                                        }
                                        if (cancellationSignal != null) {
                                            cancellationSignal.throwIfCanceled();
                                        }
                                        WriteableArchive.this.mZipOutputStream.write(bArr, 0, i);
                                        j += (long) i;
                                        Log.e("WriteableArchive", "Failed while writing to a file.", e);
                                        parcelFileDescriptor.closeWithError("Writing failure.");
                                        autoCloseInputStream.close();
                                    }
                                    zipEntry.setSize(j);
                                    WriteableArchive.this.mZipOutputStream.closeEntry();
                                }
                                autoCloseInputStream.close();
                            } catch (Throwable th2) {
                                if (0 != 0) {
                                    try {
                                        autoCloseInputStream.close();
                                    } catch (Throwable th3) {
                                        th.addSuppressed(th3);
                                    }
                                } else {
                                    autoCloseInputStream.close();
                                }
                                throw th2;
                            }
                        } catch (OperationCanceledException e3) {
                        } catch (IOException e4) {
                        }
                    }
                });
                return parcelFileDescriptorArrCreateReliablePipe[1];
            } catch (RejectedExecutionException e) {
                IoUtils.closeQuietly(parcelFileDescriptorArrCreateReliablePipe[0]);
                IoUtils.closeQuietly(parcelFileDescriptorArrCreateReliablePipe[1]);
                throw new IllegalStateException("Failed to initialize pipe.");
            }
        } catch (IOException e2) {
            throw new IllegalStateException("Failed to open the document.", e2);
        }
    }

    @Override
    public void close() {
        this.mExecutor.shutdown();
        try {
            this.mExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Log.e("WriteableArchive", "Opened files failed to be fullly written.", e);
        }
        synchronized (this.mEntries) {
            Iterator<String> it = this.mPendingEntries.iterator();
            while (it.hasNext()) {
                try {
                    this.mZipOutputStream.putNextEntry(this.mEntries.get(it.next()));
                    this.mZipOutputStream.closeEntry();
                } catch (IOException e2) {
                    Log.e("WriteableArchive", "Failed to flush empty entries.", e2);
                }
            }
            try {
                this.mZipOutputStream.close();
            } catch (IOException e3) {
                Log.e("WriteableArchive", "Failed while closing the ZIP file.", e3);
            }
        }
        IoUtils.closeQuietly(this.mOutputStream);
    }
}
