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
    public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data, ParcelFileDescriptor newState) throws Throwable {
        byte[] userDictionaryData = getDictionary();
        long[] stateChecksums = readOldChecksums(oldState);
        stateChecksums[0] = writeIfChanged(stateChecksums[0], "userdictionary", userDictionaryData, data);
        writeNewChecksums(stateChecksums, newState);
    }

    @Override
    public void onRestore(BackupDataInput data, int appVersionCode, ParcelFileDescriptor newState) throws IOException {
        while (data.readNextHeader()) {
            String key = data.getKey();
            data.getDataSize();
            if ("userdictionary".equals(key)) {
                restoreDictionary(data, UserDictionary.Words.CONTENT_URI);
            } else {
                data.skipEntityData();
            }
        }
    }

    private long[] readOldChecksums(ParcelFileDescriptor oldState) throws IOException {
        long[] stateChecksums = new long[1];
        DataInputStream dataInput = new DataInputStream(new FileInputStream(oldState.getFileDescriptor()));
        for (int i = 0; i < 1; i++) {
            try {
                stateChecksums[i] = dataInput.readLong();
            } catch (EOFException e) {
            }
        }
        dataInput.close();
        return stateChecksums;
    }

    private void writeNewChecksums(long[] checksums, ParcelFileDescriptor newState) throws IOException {
        DataOutputStream dataOutput = new DataOutputStream(new FileOutputStream(newState.getFileDescriptor()));
        for (int i = 0; i < 1; i++) {
            dataOutput.writeLong(checksums[i]);
        }
        dataOutput.close();
    }

    private long writeIfChanged(long oldChecksum, String key, byte[] data, BackupDataOutput output) {
        CRC32 checkSummer = new CRC32();
        checkSummer.update(data);
        long newChecksum = checkSummer.getValue();
        if (oldChecksum != newChecksum) {
            try {
                output.writeEntityHeader(key, data.length);
                output.writeEntityData(data, data.length);
            } catch (IOException e) {
            }
            return newChecksum;
        }
        return oldChecksum;
    }

    private byte[] getDictionary() throws Throwable {
        Cursor cursor = getContentResolver().query(UserDictionary.Words.CONTENT_URI, PROJECTION, null, null, "word");
        if (cursor == null) {
            return EMPTY_DATA;
        }
        if (!cursor.moveToFirst()) {
            Log.e("DictionaryBackupAgent", "Couldn't read from the cursor");
            cursor.close();
            return EMPTY_DATA;
        }
        byte[] sizeBytes = new byte[4];
        ByteArrayOutputStream baos = new ByteArrayOutputStream(cursor.getCount() * 10);
        GZIPOutputStream gzip = null;
        try {
            try {
                GZIPOutputStream gzip2 = new GZIPOutputStream(baos);
                while (!cursor.isAfterLast()) {
                    try {
                        String name = cursor.getString(1);
                        int frequency = cursor.getInt(2);
                        String locale = cursor.getString(3);
                        int appId = cursor.getInt(4);
                        String shortcut = cursor.getString(5);
                        if (TextUtils.isEmpty(shortcut)) {
                            shortcut = "";
                        }
                        String out = name + "|" + frequency + "|" + locale + "|" + appId + "|" + shortcut;
                        byte[] line = out.getBytes();
                        writeInt(sizeBytes, 0, line.length);
                        gzip2.write(sizeBytes);
                        gzip2.write(line);
                        cursor.moveToNext();
                    } catch (IOException e) {
                        ioe = e;
                        gzip = gzip2;
                        Log.e("DictionaryBackupAgent", "Couldn't compress the dictionary:\n" + ioe);
                        byte[] bArr = EMPTY_DATA;
                        IoUtils.closeQuietly(gzip);
                        cursor.close();
                        return bArr;
                    } catch (Throwable th) {
                        th = th;
                        gzip = gzip2;
                        IoUtils.closeQuietly(gzip);
                        cursor.close();
                        throw th;
                    }
                }
                gzip2.finish();
                IoUtils.closeQuietly(gzip2);
                cursor.close();
                return baos.toByteArray();
            } catch (IOException e2) {
                ioe = e2;
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }

    private void restoreDictionary(BackupDataInput data, Uri contentUri) {
        String word;
        String frequency;
        String locale;
        String shortcut;
        int frequencyInt;
        int appidInt;
        ContentValues cv = new ContentValues(2);
        byte[] dictCompressed = new byte[data.getDataSize()];
        try {
            data.readEntityData(dictCompressed, 0, dictCompressed.length);
            GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(dictCompressed));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] tempData = new byte[1024];
            while (true) {
                int got = gzip.read(tempData);
                if (got <= 0) {
                    break;
                } else {
                    baos.write(tempData, 0, got);
                }
            }
            gzip.close();
            byte[] dictionary = baos.toByteArray();
            int pos = 0;
            while (pos + 4 < dictionary.length) {
                int length = readInt(dictionary, pos);
                int pos2 = pos + 4;
                if (pos2 + length > dictionary.length) {
                    Log.e("DictionaryBackupAgent", "Insufficient data");
                }
                String line = new String(dictionary, pos2, length);
                pos = pos2 + length;
                StringTokenizer st = new StringTokenizer(line, "|");
                try {
                    word = st.nextToken();
                    frequency = st.nextToken();
                    locale = st.hasMoreTokens() ? st.nextToken() : null;
                    if ("null".equalsIgnoreCase(locale)) {
                        locale = null;
                    }
                    String appid = st.hasMoreTokens() ? st.nextToken() : null;
                    shortcut = st.hasMoreTokens() ? st.nextToken() : null;
                    if (TextUtils.isEmpty(shortcut)) {
                        shortcut = null;
                    }
                    frequencyInt = Integer.parseInt(frequency);
                    appidInt = appid != null ? Integer.parseInt(appid) : 0;
                } catch (NumberFormatException nfe) {
                    Log.e("DictionaryBackupAgent", "Number format error\n" + nfe);
                } catch (NoSuchElementException nsee) {
                    Log.e("DictionaryBackupAgent", "Token format error\n" + nsee);
                }
                if (!Objects.equals(word, null) || !Objects.equals(shortcut, null)) {
                    if (!TextUtils.isEmpty(frequency) && !TextUtils.isEmpty(word)) {
                        cv.clear();
                        cv.put("word", word);
                        cv.put("frequency", Integer.valueOf(frequencyInt));
                        cv.put("locale", locale);
                        cv.put("appid", Integer.valueOf(appidInt));
                        cv.put("shortcut", shortcut);
                        if (shortcut != null) {
                            getContentResolver().delete(contentUri, "word=? and shortcut=?", new String[]{word, shortcut});
                        } else {
                            getContentResolver().delete(contentUri, "word=? and shortcut is null", new String[0]);
                        }
                        getContentResolver().insert(contentUri, cv);
                    }
                }
            }
        } catch (IOException ioe) {
            Log.e("DictionaryBackupAgent", "Couldn't read and uncompress entity data:\n" + ioe);
        }
    }

    private int writeInt(byte[] out, int pos, int value) {
        out[pos + 0] = (byte) ((value >> 24) & 255);
        out[pos + 1] = (byte) ((value >> 16) & 255);
        out[pos + 2] = (byte) ((value >> 8) & 255);
        out[pos + 3] = (byte) ((value >> 0) & 255);
        return pos + 4;
    }

    private int readInt(byte[] in, int pos) {
        int result = ((in[pos] & 255) << 24) | ((in[pos + 1] & 255) << 16) | ((in[pos + 2] & 255) << 8) | ((in[pos + 3] & 255) << 0);
        return result;
    }
}
