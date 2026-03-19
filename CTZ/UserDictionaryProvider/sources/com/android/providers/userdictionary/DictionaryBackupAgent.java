package com.android.providers.userdictionary;

import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.UserDictionary;
import android.text.TextUtils;
import android.util.Log;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.zip.CRC32;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import libcore.io.IoUtils;

public class DictionaryBackupAgent extends BackupAgentHelper {
    private static final byte[] EMPTY_DATA = new byte[0];
    private static final String[] PROJECTION = {"_id", "word", "frequency", "locale", "appid", "shortcut"};

    @Override
    public void onBackup(ParcelFileDescriptor parcelFileDescriptor, BackupDataOutput backupDataOutput, ParcelFileDescriptor parcelFileDescriptor2) throws Throwable {
        byte[] dictionary = getDictionary();
        long[] oldChecksums = readOldChecksums(parcelFileDescriptor);
        oldChecksums[0] = writeIfChanged(oldChecksums[0], "userdictionary", dictionary, backupDataOutput);
        writeNewChecksums(oldChecksums, parcelFileDescriptor2);
    }

    @Override
    public void onRestore(BackupDataInput backupDataInput, int i, ParcelFileDescriptor parcelFileDescriptor) throws IOException {
        while (backupDataInput.readNextHeader()) {
            String key = backupDataInput.getKey();
            backupDataInput.getDataSize();
            if ("userdictionary".equals(key)) {
                restoreDictionary(backupDataInput, UserDictionary.Words.CONTENT_URI);
            } else {
                backupDataInput.skipEntityData();
            }
        }
    }

    private long[] readOldChecksums(ParcelFileDescriptor parcelFileDescriptor) throws IOException {
        long[] jArr = new long[1];
        DataInputStream dataInputStream = new DataInputStream(new FileInputStream(parcelFileDescriptor.getFileDescriptor()));
        for (int i = 0; i < 1; i++) {
            try {
                jArr[i] = dataInputStream.readLong();
            } catch (EOFException e) {
            }
        }
        dataInputStream.close();
        return jArr;
    }

    private void writeNewChecksums(long[] jArr, ParcelFileDescriptor parcelFileDescriptor) throws IOException {
        DataOutputStream dataOutputStream = new DataOutputStream(new FileOutputStream(parcelFileDescriptor.getFileDescriptor()));
        for (int i = 0; i < 1; i++) {
            dataOutputStream.writeLong(jArr[i]);
        }
        dataOutputStream.close();
    }

    private long writeIfChanged(long j, String str, byte[] bArr, BackupDataOutput backupDataOutput) {
        CRC32 crc32 = new CRC32();
        crc32.update(bArr);
        long value = crc32.getValue();
        if (j == value) {
            return j;
        }
        try {
            backupDataOutput.writeEntityHeader(str, bArr.length);
            backupDataOutput.writeEntityData(bArr, bArr.length);
        } catch (IOException e) {
        }
        return value;
    }

    private byte[] getDictionary() throws Throwable {
        GZIPOutputStream gZIPOutputStream;
        Cursor cursorQuery = getContentResolver().query(UserDictionary.Words.CONTENT_URI, PROJECTION, null, null, "word");
        if (cursorQuery == null) {
            return EMPTY_DATA;
        }
        if (!cursorQuery.moveToFirst()) {
            Log.e("DictionaryBackupAgent", "Couldn't read from the cursor");
            cursorQuery.close();
            return EMPTY_DATA;
        }
        byte[] bArr = new byte[4];
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(cursorQuery.getCount() * 10);
        GZIPOutputStream gZIPOutputStream2 = null;
        try {
            try {
                gZIPOutputStream = new GZIPOutputStream(byteArrayOutputStream);
                while (!cursorQuery.isAfterLast()) {
                    try {
                        String string = cursorQuery.getString(1);
                        int i = cursorQuery.getInt(2);
                        String string2 = cursorQuery.getString(3);
                        int i2 = cursorQuery.getInt(4);
                        String string3 = cursorQuery.getString(5);
                        if (TextUtils.isEmpty(string3)) {
                            string3 = "";
                        }
                        byte[] bytes = (string + "|" + i + "|" + string2 + "|" + i2 + "|" + string3).getBytes();
                        writeInt(bArr, 0, bytes.length);
                        gZIPOutputStream.write(bArr);
                        gZIPOutputStream.write(bytes);
                        cursorQuery.moveToNext();
                    } catch (IOException e) {
                        e = e;
                        gZIPOutputStream2 = gZIPOutputStream;
                        Log.e("DictionaryBackupAgent", "Couldn't compress the dictionary:\n" + e);
                        byte[] bArr2 = EMPTY_DATA;
                        IoUtils.closeQuietly(gZIPOutputStream2);
                        cursorQuery.close();
                        return bArr2;
                    } catch (Throwable th) {
                        th = th;
                        IoUtils.closeQuietly(gZIPOutputStream);
                        cursorQuery.close();
                        throw th;
                    }
                }
                gZIPOutputStream.finish();
                IoUtils.closeQuietly(gZIPOutputStream);
                cursorQuery.close();
                return byteArrayOutputStream.toByteArray();
            } catch (IOException e2) {
                e = e2;
            }
        } catch (Throwable th2) {
            th = th2;
            gZIPOutputStream = gZIPOutputStream2;
        }
    }

    private void restoreDictionary(BackupDataInput backupDataInput, Uri uri) {
        ContentValues contentValues = new ContentValues(2);
        byte[] bArr = new byte[backupDataInput.getDataSize()];
        try {
            backupDataInput.readEntityData(bArr, 0, bArr.length);
            GZIPInputStream gZIPInputStream = new GZIPInputStream(new ByteArrayInputStream(bArr));
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] bArr2 = new byte[1024];
            while (true) {
                int i = gZIPInputStream.read(bArr2);
                if (i <= 0) {
                    break;
                } else {
                    byteArrayOutputStream.write(bArr2, 0, i);
                }
            }
            gZIPInputStream.close();
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            int i2 = 0;
            while (true) {
                int i3 = i2 + 4;
                if (i3 < byteArray.length) {
                    int i4 = readInt(byteArray, i2);
                    int i5 = i3 + i4;
                    if (i5 > byteArray.length) {
                        Log.e("DictionaryBackupAgent", "Insufficient data");
                    }
                    StringTokenizer stringTokenizer = new StringTokenizer(new String(byteArray, i3, i4), "|");
                    try {
                        String strNextToken = stringTokenizer.nextToken();
                        String strNextToken2 = stringTokenizer.nextToken();
                        String strNextToken3 = stringTokenizer.hasMoreTokens() ? stringTokenizer.nextToken() : null;
                        if ("null".equalsIgnoreCase(strNextToken3)) {
                            strNextToken3 = null;
                        }
                        String strNextToken4 = stringTokenizer.hasMoreTokens() ? stringTokenizer.nextToken() : null;
                        String strNextToken5 = stringTokenizer.hasMoreTokens() ? stringTokenizer.nextToken() : null;
                        if (TextUtils.isEmpty(strNextToken5)) {
                            strNextToken5 = null;
                        }
                        int i6 = Integer.parseInt(strNextToken2);
                        int i7 = strNextToken4 != null ? Integer.parseInt(strNextToken4) : 0;
                        if ((!Objects.equals(strNextToken, null) || !Objects.equals(strNextToken5, null)) && !TextUtils.isEmpty(strNextToken2) && !TextUtils.isEmpty(strNextToken)) {
                            contentValues.clear();
                            contentValues.put("word", strNextToken);
                            contentValues.put("frequency", Integer.valueOf(i6));
                            contentValues.put("locale", strNextToken3);
                            contentValues.put("appid", Integer.valueOf(i7));
                            contentValues.put("shortcut", strNextToken5);
                            if (strNextToken5 != null) {
                                getContentResolver().delete(uri, "word=? and shortcut=?", new String[]{strNextToken, strNextToken5});
                            } else {
                                getContentResolver().delete(uri, "word=? and shortcut is null", new String[0]);
                            }
                            getContentResolver().insert(uri, contentValues);
                        }
                    } catch (NumberFormatException e) {
                        Log.e("DictionaryBackupAgent", "Number format error\n" + e);
                    } catch (NoSuchElementException e2) {
                        Log.e("DictionaryBackupAgent", "Token format error\n" + e2);
                    }
                    i2 = i5;
                } else {
                    return;
                }
            }
        } catch (IOException e3) {
            Log.e("DictionaryBackupAgent", "Couldn't read and uncompress entity data:\n" + e3);
        }
    }

    private int writeInt(byte[] bArr, int i, int i2) {
        bArr[i + 0] = (byte) ((i2 >> 24) & 255);
        bArr[i + 1] = (byte) ((i2 >> 16) & 255);
        bArr[i + 2] = (byte) ((i2 >> 8) & 255);
        bArr[i + 3] = (byte) ((i2 >> 0) & 255);
        return i + 4;
    }

    private int readInt(byte[] bArr, int i) {
        return ((bArr[i + 3] & 255) << 0) | ((bArr[i] & 255) << 24) | ((bArr[i + 1] & 255) << 16) | ((bArr[i + 2] & 255) << 8);
    }
}
