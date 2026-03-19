package com.android.documentsui.picker;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import com.android.documentsui.base.DocumentStack;
import com.android.documentsui.base.Shared;
import com.android.documentsui.base.State;
import com.android.documentsui.roots.ProvidersAccess;
import java.io.IOException;
import libcore.io.IoUtils;

public interface LastAccessedStorage {
    DocumentStack getLastAccessed(Activity activity, ProvidersAccess providersAccess, State state);

    void setLastAccessed(Activity activity, DocumentStack documentStack);

    void setLastAccessedToExternalApp(Activity activity);

    static LastAccessedStorage create() {
        return new RuntimeLastAccessedStorage();
    }

    public static class RuntimeLastAccessedStorage implements LastAccessedStorage {
        private RuntimeLastAccessedStorage() {
        }

        @Override
        public DocumentStack getLastAccessed(Activity activity, ProvidersAccess providersAccess, State state) {
            Uri uriBuildLastAccessed = LastAccessedProvider.buildLastAccessed(Shared.getCallingPackageName(activity));
            ContentResolver contentResolver = activity.getContentResolver();
            Cursor cursorQuery = contentResolver.query(uriBuildLastAccessed, null, null, null, null);
            try {
                try {
                    return DocumentStack.fromLastAccessedCursor(cursorQuery, providersAccess.getMatchingRootsBlocking(state), contentResolver);
                } catch (IOException e) {
                    Log.w("LastAccessedStorage", "Failed to resume: ", e);
                    IoUtils.closeQuietly(cursorQuery);
                    return null;
                }
            } finally {
                IoUtils.closeQuietly(cursorQuery);
            }
        }

        @Override
        public void setLastAccessed(Activity activity, DocumentStack documentStack) {
            LastAccessedProvider.setLastAccessed(activity.getContentResolver(), Shared.getCallingPackageName(activity), documentStack);
        }

        @Override
        public void setLastAccessedToExternalApp(Activity activity) {
            String callingPackageName = Shared.getCallingPackageName(activity);
            ContentValues contentValues = new ContentValues();
            contentValues.put("external", (Integer) 1);
            activity.getContentResolver().insert(LastAccessedProvider.buildLastAccessed(callingPackageName), contentValues);
        }
    }
}
