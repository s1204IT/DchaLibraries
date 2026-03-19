package com.android.providers.blockednumber;

import android.app.backup.BackupAgent;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.ParcelFileDescriptor;
import android.provider.BlockedNumberContract;
import android.util.Log;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;
import libcore.io.IoUtils;

public class BlockedNumberBackupAgent extends BackupAgent {
    private static final String[] BLOCKED_NUMBERS_PROJECTION = {"_id", "original_number", "e164_number"};

    @Override
    public void onBackup(ParcelFileDescriptor parcelFileDescriptor, BackupDataOutput backupDataOutput, ParcelFileDescriptor parcelFileDescriptor2) throws IOException {
        logV("Backing up blocked numbers.");
        DataInputStream dataInputStream = new DataInputStream(new FileInputStream(parcelFileDescriptor.getFileDescriptor()));
        try {
            BackupState state = readState(dataInputStream);
            IoUtils.closeQuietly(dataInputStream);
            runBackup(state, backupDataOutput, getAllBlockedNumbers());
            DataOutputStream dataOutputStream = new DataOutputStream(new FileOutputStream(parcelFileDescriptor2.getFileDescriptor()));
            try {
                writeNewState(dataOutputStream, state);
            } finally {
                dataOutputStream.close();
            }
        } catch (Throwable th) {
            IoUtils.closeQuietly(dataInputStream);
            throw th;
        }
    }

    @Override
    public void onRestore(BackupDataInput backupDataInput, int i, ParcelFileDescriptor parcelFileDescriptor) throws IOException {
        logV("Restoring blocked numbers.");
        while (backupDataInput.readNextHeader()) {
            BackedUpBlockedNumber blockedNumberFromData = readBlockedNumberFromData(backupDataInput);
            if (blockedNumberFromData != null) {
                writeToProvider(blockedNumberFromData);
            }
        }
    }

    private BackupState readState(DataInputStream dataInputStream) throws IOException {
        int i;
        if (dataInputStream.available() > 0) {
            i = dataInputStream.readInt();
        } else {
            i = 1;
        }
        BackupState backupState = new BackupState(i, new TreeSet());
        while (dataInputStream.available() > 0) {
            backupState.ids.add(Integer.valueOf(dataInputStream.readInt()));
        }
        return backupState;
    }

    private void runBackup(BackupState backupState, BackupDataOutput backupDataOutput, Iterable<BackedUpBlockedNumber> iterable) throws IOException {
        TreeSet treeSet = new TreeSet((SortedSet) backupState.ids);
        for (BackedUpBlockedNumber backedUpBlockedNumber : iterable) {
            if (backupState.ids.contains(Integer.valueOf(backedUpBlockedNumber.id))) {
                treeSet.remove(Integer.valueOf(backedUpBlockedNumber.id));
            } else {
                logV("Adding blocked number to backup: " + backedUpBlockedNumber);
                addToBackup(backupDataOutput, backedUpBlockedNumber);
                backupState.ids.add(Integer.valueOf(backedUpBlockedNumber.id));
            }
        }
        Iterator it = treeSet.iterator();
        while (it.hasNext()) {
            int iIntValue = ((Integer) it.next()).intValue();
            logV("Removing blocked number from backup: " + iIntValue);
            removeFromBackup(backupDataOutput, iIntValue);
            backupState.ids.remove(Integer.valueOf(iIntValue));
        }
    }

    private void addToBackup(BackupDataOutput backupDataOutput, BackedUpBlockedNumber backedUpBlockedNumber) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        dataOutputStream.writeInt(1);
        writeString(dataOutputStream, backedUpBlockedNumber.originalNumber);
        writeString(dataOutputStream, backedUpBlockedNumber.e164Number);
        dataOutputStream.flush();
        backupDataOutput.writeEntityHeader(Integer.toString(backedUpBlockedNumber.id), byteArrayOutputStream.size());
        backupDataOutput.writeEntityData(byteArrayOutputStream.toByteArray(), byteArrayOutputStream.size());
    }

    private void writeString(DataOutputStream dataOutputStream, String str) throws IOException {
        if (str == null) {
            dataOutputStream.writeBoolean(false);
        } else {
            dataOutputStream.writeBoolean(true);
            dataOutputStream.writeUTF(str);
        }
    }

    private String readString(DataInputStream dataInputStream) throws IOException {
        if (dataInputStream.readBoolean()) {
            return dataInputStream.readUTF();
        }
        return null;
    }

    private void removeFromBackup(BackupDataOutput backupDataOutput, int i) throws IOException {
        backupDataOutput.writeEntityHeader(Integer.toString(i), -1);
    }

    private Iterable<BackedUpBlockedNumber> getAllBlockedNumbers() {
        ArrayList arrayList = new ArrayList();
        Cursor cursorQuery = getContentResolver().query(BlockedNumberContract.BlockedNumbers.CONTENT_URI, BLOCKED_NUMBERS_PROJECTION, null, null, null);
        if (cursorQuery != null) {
            while (cursorQuery.moveToNext()) {
                try {
                    arrayList.add(createBlockedNumberFromCursor(cursorQuery));
                } finally {
                    cursorQuery.close();
                }
            }
        }
        return arrayList;
    }

    private BackedUpBlockedNumber createBlockedNumberFromCursor(Cursor cursor) {
        return new BackedUpBlockedNumber(cursor.getInt(0), cursor.getString(1), cursor.getString(2));
    }

    private void writeNewState(DataOutputStream dataOutputStream, BackupState backupState) throws IOException {
        dataOutputStream.writeInt(1);
        Iterator<Integer> it = backupState.ids.iterator();
        while (it.hasNext()) {
            dataOutputStream.writeInt(it.next().intValue());
        }
    }

    private BackedUpBlockedNumber readBlockedNumberFromData(BackupDataInput backupDataInput) {
        try {
            int i = Integer.parseInt(backupDataInput.getKey());
            try {
                byte[] bArr = new byte[backupDataInput.getDataSize()];
                backupDataInput.readEntityData(bArr, 0, bArr.length);
                DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(bArr));
                dataInputStream.readInt();
                BackedUpBlockedNumber backedUpBlockedNumber = new BackedUpBlockedNumber(i, readString(dataInputStream), readString(dataInputStream));
                logV("Restoring blocked number: " + backedUpBlockedNumber);
                return backedUpBlockedNumber;
            } catch (IOException e) {
                Log.e("BlockedNumberBackup", "Error reading blocked number for: " + i + ": " + e.getMessage());
                return null;
            }
        } catch (NumberFormatException e2) {
            Log.e("BlockedNumberBackup", "Unexpected key found in restore: " + backupDataInput.getKey());
            return null;
        }
    }

    private void writeToProvider(BackedUpBlockedNumber backedUpBlockedNumber) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("original_number", backedUpBlockedNumber.originalNumber);
        contentValues.put("e164_number", backedUpBlockedNumber.e164Number);
        try {
            getContentResolver().insert(BlockedNumberContract.BlockedNumbers.CONTENT_URI, contentValues);
        } catch (Exception e) {
            Log.e("BlockedNumberBackup", "Unable to insert blocked number " + backedUpBlockedNumber + " :" + e.getMessage());
        }
    }

    private static boolean isDebug() {
        return Log.isLoggable("BlockedNumberBackup", 3);
    }

    private static void logV(String str) {
    }

    private static class BackupState {
        final SortedSet<Integer> ids;
        final int version;

        BackupState(int i, SortedSet<Integer> sortedSet) {
            this.version = i;
            this.ids = sortedSet;
        }
    }

    private static class BackedUpBlockedNumber {
        final String e164Number;
        final int id;
        final String originalNumber;

        BackedUpBlockedNumber(int i, String str, String str2) {
            this.id = i;
            this.originalNumber = str;
            this.e164Number = str2;
        }

        public String toString() {
            if (BlockedNumberBackupAgent.isDebug()) {
                return String.format("[%d, original number: %s, e164 number: %s]", Integer.valueOf(this.id), this.originalNumber, this.e164Number);
            }
            return String.format("[%d]", Integer.valueOf(this.id));
        }
    }
}
