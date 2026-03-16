package com.android.internal.policy.impl;

import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyCharacterMap;
import java.net.URISyntaxException;

class ShortcutManager extends ContentObserver {
    private static final int COLUMN_INTENT = 1;
    private static final int COLUMN_SHORTCUT = 0;
    private static final String TAG = "ShortcutManager";
    private static final String[] sProjection = {"shortcut", "intent"};
    private Context mContext;
    private Cursor mCursor;
    private SparseArray<Intent> mShortcutIntents;

    public ShortcutManager(Context context, Handler handler) {
        super(handler);
        this.mContext = context;
        this.mShortcutIntents = new SparseArray<>();
    }

    public void observe() {
        this.mCursor = this.mContext.getContentResolver().query(Settings.Bookmarks.CONTENT_URI, sProjection, null, null, null);
        this.mCursor.registerContentObserver(this);
        updateShortcuts();
    }

    @Override
    public void onChange(boolean selfChange) {
        updateShortcuts();
    }

    private void updateShortcuts() {
        Cursor c = this.mCursor;
        if (!c.requery()) {
            Log.e(TAG, "ShortcutObserver could not re-query shortcuts.");
            return;
        }
        this.mShortcutIntents.clear();
        while (c.moveToNext()) {
            int shortcut = c.getInt(COLUMN_SHORTCUT);
            if (shortcut != 0) {
                String intentURI = c.getString(1);
                Intent intent = null;
                try {
                    intent = Intent.getIntent(intentURI);
                } catch (URISyntaxException e) {
                    Log.w(TAG, "Intent URI for shortcut invalid.", e);
                }
                if (intent != null) {
                    this.mShortcutIntents.put(shortcut, intent);
                }
            }
        }
    }

    public Intent getIntent(KeyCharacterMap kcm, int keyCode, int metaState) {
        int shortcut;
        Intent intent = null;
        int shortcut2 = kcm.get(keyCode, metaState);
        if (shortcut2 != 0) {
            Intent intent2 = this.mShortcutIntents.get(shortcut2);
            intent = intent2;
        }
        if (intent == null && (shortcut = Character.toLowerCase(kcm.getDisplayLabel(keyCode))) != 0) {
            Intent intent3 = this.mShortcutIntents.get(shortcut);
            return intent3;
        }
        return intent;
    }
}
