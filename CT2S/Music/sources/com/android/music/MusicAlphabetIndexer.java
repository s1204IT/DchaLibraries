package com.android.music;

import android.database.Cursor;
import android.provider.MediaStore;
import android.widget.AlphabetIndexer;

class MusicAlphabetIndexer extends AlphabetIndexer {
    public MusicAlphabetIndexer(Cursor cursor, int sortedColumnIndex, CharSequence alphabet) {
        super(cursor, sortedColumnIndex, alphabet);
    }

    @Override
    protected int compare(String word, String letter) {
        String wordKey = MediaStore.Audio.keyFor(word);
        String letterKey = MediaStore.Audio.keyFor(letter);
        if (wordKey.startsWith(letter)) {
            return 0;
        }
        return wordKey.compareTo(letterKey);
    }
}
