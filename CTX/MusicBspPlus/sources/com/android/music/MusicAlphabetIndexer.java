package com.android.music;

import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.provider.MediaStore;
import android.util.SparseIntArray;
import android.widget.AlphabetIndexer;

class MusicAlphabetIndexer extends AlphabetIndexer {
    private final SparseIntArray mAlphabetMap;

    public MusicAlphabetIndexer(Cursor cursor, int i, String str) {
        super(cursor, i, str);
        String lowerCase = str.toLowerCase();
        int length = lowerCase.length();
        this.mAlphabetMap = new SparseIntArray(length);
        for (int i2 = 0; i2 < length; i2++) {
            this.mAlphabetMap.put(lowerCase.charAt(i2), i2);
        }
    }

    @Override
    protected int compare(String str, String str2) {
        String strKeyFor = MediaStore.Audio.keyFor(str);
        String strKeyFor2 = MediaStore.Audio.keyFor(str2);
        if (strKeyFor.startsWith(str2)) {
            MusicLogUtils.v("MusicAlphabetIndexer", "startsWith return 0 ");
            return 0;
        }
        return strKeyFor.compareTo(strKeyFor2);
    }

    @Override
    public int getPositionForSection(int i) {
        if (i >= this.mAlphabet.length()) {
            return this.mDataCursor.getCount();
        }
        return super.getPositionForSection(i);
    }

    @Override
    public int getSectionForPosition(int i) {
        int position = this.mDataCursor.getPosition();
        this.mDataCursor.moveToPosition(i);
        try {
            if (this.mColumnIndex < 0 || this.mColumnIndex >= this.mDataCursor.getCount()) {
                return 0;
            }
            String string = this.mDataCursor.getString(this.mColumnIndex);
            this.mDataCursor.moveToPosition(position);
            return this.mAlphabetMap.get(convertToSpecName(string).charAt(0), this.mAlphabetMap.size() - 1);
        } catch (CursorIndexOutOfBoundsException e) {
            MusicLogUtils.v("MusicAlphabetIndexer", "CursorIndexOutOfBoundsException");
            return 0;
        }
    }

    public static String convertToSpecName(String str) {
        if (str == null || str.equals("<unknown>")) {
            return " ";
        }
        String lowerCase = str.trim().toLowerCase();
        if (lowerCase.startsWith("the ")) {
            lowerCase = lowerCase.substring(4);
        }
        if (lowerCase.startsWith("an ")) {
            lowerCase = lowerCase.substring(3);
        }
        if (lowerCase.startsWith("a ")) {
            lowerCase = lowerCase.substring(2);
        }
        String strTrim = lowerCase.replaceAll("[\\[\\]\\(\\)\"'.,?!]", "").trim();
        if (strTrim.isEmpty() || strTrim.charAt(0) < 'A') {
            return " ";
        }
        return strTrim;
    }
}
