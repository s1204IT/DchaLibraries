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
    @Override
    public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data, ParcelFileDescriptor newState) throws IOException {
        DataInputStream in = new DataInputStream(new FileInputStream(oldState.getFileDescriptor()));
        try {
            long savedFileSize = in.readLong();
            long savedCrc = in.readLong();
            in.readInt();
            if (in != null) {
                in.close();
            }
            writeBackupState(savedFileSize, savedCrc, newState);
        } catch (EOFException e) {
            if (in == null) {
                return;
            }
            in.close();
        } catch (Throwable th) {
            if (in != null) {
                in.close();
            }
            throw th;
        }
    }

    @Override
    public void onRestore(BackupDataInput data, int appVersionCode, ParcelFileDescriptor newState) throws IOException {
        long crc = -1;
        File tmpfile = File.createTempFile("rst", null, getFilesDir());
        while (data.readNextHeader()) {
            try {
                if ("_bookmarks_".equals(data.getKey())) {
                    crc = copyBackupToFile(data, tmpfile, data.getDataSize());
                    FileInputStream infstream = new FileInputStream(tmpfile);
                    DataInputStream in = new DataInputStream(infstream);
                    try {
                        try {
                            int count = in.readInt();
                            ArrayList<Bookmark> bookmarks = new ArrayList<>(count);
                            for (int i = 0; i < count; i++) {
                                Bookmark mark = new Bookmark();
                                mark.url = in.readUTF();
                                mark.visits = in.readInt();
                                mark.date = in.readLong();
                                mark.created = in.readLong();
                                mark.title = in.readUTF();
                                bookmarks.add(mark);
                            }
                            int N = bookmarks.size();
                            int nUnique = 0;
                            String[] urlCol = {"url"};
                            for (int i2 = 0; i2 < N; i2++) {
                                Bookmark mark2 = bookmarks.get(i2);
                                Cursor cursor = getContentResolver().query(BrowserContract.Bookmarks.CONTENT_URI, urlCol, "url == ?", new String[]{mark2.url}, null);
                                if (cursor.getCount() <= 0) {
                                    addBookmark(mark2);
                                    nUnique++;
                                }
                                cursor.close();
                            }
                            Log.i("BrowserBackupAgent", "Restored " + nUnique + " of " + N + " bookmarks");
                            if (in != null) {
                                in.close();
                            }
                        } catch (IOException e) {
                            Log.w("BrowserBackupAgent", "Bad backup data; not restoring");
                            crc = -1;
                            if (in != null) {
                                in.close();
                            }
                        }
                    } catch (Throwable th) {
                        if (in != null) {
                            in.close();
                        }
                        throw th;
                    }
                }
                writeBackupState(tmpfile.length(), crc, newState);
            } finally {
                tmpfile.delete();
            }
        }
    }

    void addBookmark(Bookmark mark) {
        ContentValues values = new ContentValues();
        values.put("title", mark.title);
        values.put("url", mark.url);
        values.put("folder", (Integer) 0);
        values.put("created", Long.valueOf(mark.created));
        values.put("modified", Long.valueOf(mark.date));
        getContentResolver().insert(BrowserContract.Bookmarks.CONTENT_URI, values);
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

    private long copyBackupToFile(BackupDataInput data, File file, int toRead) throws IOException {
        byte[] buf = new byte[8192];
        CRC32 crc = new CRC32();
        FileOutputStream out = new FileOutputStream(file);
        while (toRead > 0) {
            try {
                int numRead = data.readEntityData(buf, 0, 8192);
                crc.update(buf, 0, numRead);
                out.write(buf, 0, numRead);
                toRead -= numRead;
            } finally {
                if (out != null) {
                    out.close();
                }
            }
        }
        return crc.getValue();
    }

    private void writeBackupState(long fileSize, long crc, ParcelFileDescriptor stateFile) throws IOException {
        DataOutputStream out = new DataOutputStream(new FileOutputStream(stateFile.getFileDescriptor()));
        try {
            out.writeLong(fileSize);
            out.writeLong(crc);
            out.writeInt(0);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }
}
