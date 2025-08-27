package com.android.browser;

import android.app.backup.BackupAgent;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import com.android.browser.provider.BrowserContract;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.CRC32;

/* loaded from: classes.dex */
public class BrowserBackupAgent extends BackupAgent {
    @Override // android.app.backup.BackupAgent
    public void onBackup(ParcelFileDescriptor parcelFileDescriptor, BackupDataOutput backupDataOutput, ParcelFileDescriptor parcelFileDescriptor2) throws IOException {
        DataInputStream dataInputStream = new DataInputStream(new FileInputStream(parcelFileDescriptor.getFileDescriptor()));
        try {
            long j = dataInputStream.readLong();
            long j2 = dataInputStream.readLong();
            dataInputStream.readInt();
            dataInputStream.close();
            writeBackupState(j, j2, parcelFileDescriptor2);
        } catch (EOFException e) {
            dataInputStream.close();
        } catch (Throwable th) {
            dataInputStream.close();
            throw th;
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [149=4] */
    @Override // android.app.backup.BackupAgent
    public void onRestore(BackupDataInput backupDataInput, int i, ParcelFileDescriptor parcelFileDescriptor) throws IOException {
        long j;
        File fileCreateTempFile = File.createTempFile("rst", null, getFilesDir());
        long jCopyBackupToFile = -1;
        while (backupDataInput.readNextHeader()) {
            try {
                if ("_bookmarks_".equals(backupDataInput.getKey())) {
                    jCopyBackupToFile = copyBackupToFile(backupDataInput, fileCreateTempFile, backupDataInput.getDataSize());
                    DataInputStream dataInputStream = new DataInputStream(new FileInputStream(fileCreateTempFile));
                    try {
                        try {
                            int i2 = dataInputStream.readInt();
                            ArrayList arrayList = new ArrayList(i2);
                            char c = 0;
                            for (int i3 = 0; i3 < i2; i3++) {
                                Bookmark bookmark = new Bookmark();
                                bookmark.url = dataInputStream.readUTF();
                                bookmark.visits = dataInputStream.readInt();
                                bookmark.date = dataInputStream.readLong();
                                bookmark.created = dataInputStream.readLong();
                                bookmark.title = dataInputStream.readUTF();
                                arrayList.add(bookmark);
                            }
                            int size = arrayList.size();
                            String[] strArr = {"url"};
                            int i4 = 0;
                            int i5 = 0;
                            while (i5 < size) {
                                Bookmark bookmark2 = (Bookmark) arrayList.get(i5);
                                ContentResolver contentResolver = getContentResolver();
                                Uri uri = BrowserContract.Bookmarks.CONTENT_URI;
                                String[] strArr2 = new String[1];
                                strArr2[c] = bookmark2.url;
                                int i6 = i4;
                                int i7 = i5;
                                Cursor cursorQuery = contentResolver.query(uri, strArr, "url == ?", strArr2, null);
                                if (cursorQuery.getCount() <= 0) {
                                    addBookmark(bookmark2);
                                    i4 = i6 + 1;
                                } else {
                                    i4 = i6;
                                }
                                cursorQuery.close();
                                i5 = i7 + 1;
                                c = 0;
                            }
                            Log.i("BrowserBackupAgent", "Restored " + i4 + " of " + size + " bookmarks");
                        } finally {
                        }
                    } catch (IOException e) {
                        Log.w("BrowserBackupAgent", "Bad backup data; not restoring");
                        dataInputStream.close();
                        j = -1;
                    }
                }
                j = jCopyBackupToFile;
                writeBackupState(fileCreateTempFile.length(), j, parcelFileDescriptor);
                jCopyBackupToFile = j;
            } finally {
                fileCreateTempFile.delete();
            }
        }
    }

    void addBookmark(Bookmark bookmark) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("title", bookmark.title);
        contentValues.put("url", bookmark.url);
        contentValues.put("folder", (Integer) 0);
        contentValues.put("created", Long.valueOf(bookmark.created));
        contentValues.put("modified", Long.valueOf(bookmark.date));
        getContentResolver().insert(BrowserContract.Bookmarks.CONTENT_URI, contentValues);
    }

    static class Bookmark {
        public long created;
        public long date;
        public String title;
        public String url;
        public int visits;

        Bookmark() {
        }
    }

    private long copyBackupToFile(BackupDataInput backupDataInput, File file, int i) throws IOException {
        byte[] bArr = new byte[8192];
        CRC32 crc32 = new CRC32();
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        while (i > 0) {
            try {
                int entityData = backupDataInput.readEntityData(bArr, 0, 8192);
                crc32.update(bArr, 0, entityData);
                fileOutputStream.write(bArr, 0, entityData);
                i -= entityData;
            } catch (Throwable th) {
                fileOutputStream.close();
                throw th;
            }
        }
        fileOutputStream.close();
        return crc32.getValue();
    }

    private void writeBackupState(long j, long j2, ParcelFileDescriptor parcelFileDescriptor) throws IOException {
        DataOutputStream dataOutputStream = new DataOutputStream(new FileOutputStream(parcelFileDescriptor.getFileDescriptor()));
        try {
            dataOutputStream.writeLong(j);
            dataOutputStream.writeLong(j2);
            dataOutputStream.writeInt(0);
        } finally {
            dataOutputStream.close();
        }
    }
}
