package com.android.browser;

import android.app.backup.BackupAgent;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.content.ContentValues;
import android.database.Cursor;
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

public class BrowserBackupAgent extends BackupAgent {

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

    void addBookmark(Bookmark bookmark) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("title", bookmark.title);
        contentValues.put("url", bookmark.url);
        contentValues.put("folder", (Integer) 0);
        contentValues.put("created", Long.valueOf(bookmark.created));
        contentValues.put("modified", Long.valueOf(bookmark.date));
        getContentResolver().insert(BrowserContract.Bookmarks.CONTENT_URI, contentValues);
    }

    @Override
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

    @Override
    public void onRestore(BackupDataInput backupDataInput, int i, ParcelFileDescriptor parcelFileDescriptor) throws IOException {
        int i2;
        File fileCreateTempFile = File.createTempFile("rst", null, getFilesDir());
        long j = -1;
        while (backupDataInput.readNextHeader()) {
            try {
                if ("_bookmarks_".equals(backupDataInput.getKey())) {
                    long jCopyBackupToFile = copyBackupToFile(backupDataInput, fileCreateTempFile, backupDataInput.getDataSize());
                    DataInputStream dataInputStream = new DataInputStream(new FileInputStream(fileCreateTempFile));
                    try {
                        try {
                            int i3 = dataInputStream.readInt();
                            ArrayList arrayList = new ArrayList(i3);
                            for (int i4 = 0; i4 < i3; i4++) {
                                Bookmark bookmark = new Bookmark();
                                bookmark.url = dataInputStream.readUTF();
                                bookmark.visits = dataInputStream.readInt();
                                bookmark.date = dataInputStream.readLong();
                                bookmark.created = dataInputStream.readLong();
                                bookmark.title = dataInputStream.readUTF();
                                arrayList.add(bookmark);
                            }
                            int size = arrayList.size();
                            int i5 = 0;
                            int i6 = 0;
                            while (i5 < size) {
                                Bookmark bookmark2 = (Bookmark) arrayList.get(i5);
                                Cursor cursorQuery = getContentResolver().query(BrowserContract.Bookmarks.CONTENT_URI, new String[]{"url"}, "url == ?", new String[]{bookmark2.url}, null);
                                if (cursorQuery.getCount() <= 0) {
                                    addBookmark(bookmark2);
                                    i2 = i6 + 1;
                                } else {
                                    i2 = i6;
                                }
                                cursorQuery.close();
                                i5++;
                                i6 = i2;
                            }
                            Log.i("BrowserBackupAgent", "Restored " + i6 + " of " + size + " bookmarks");
                            dataInputStream.close();
                            j = jCopyBackupToFile;
                        } finally {
                        }
                    } catch (IOException e) {
                        Log.w("BrowserBackupAgent", "Bad backup data; not restoring");
                        dataInputStream.close();
                        j = -1;
                    }
                }
                writeBackupState(fileCreateTempFile.length(), j, parcelFileDescriptor);
            } finally {
                fileCreateTempFile.delete();
            }
        }
    }
}
