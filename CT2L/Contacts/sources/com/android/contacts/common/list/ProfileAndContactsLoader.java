package com.android.contacts.common.list;

import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import com.google.common.collect.Lists;
import java.util.List;

public class ProfileAndContactsLoader extends CursorLoader {
    private boolean mLoadProfile;
    private String[] mProjection;

    public ProfileAndContactsLoader(Context context) {
        super(context);
    }

    public void setLoadProfile(boolean flag) {
        this.mLoadProfile = flag;
    }

    @Override
    public void setProjection(String[] projection) {
        super.setProjection(projection);
        this.mProjection = projection;
    }

    @Override
    public Cursor loadInBackground() {
        List<Cursor> cursors = Lists.newArrayList();
        if (this.mLoadProfile) {
            cursors.add(loadProfile());
        }
        Cursor cursor = null;
        try {
            cursor = super.loadInBackground();
        } catch (NullPointerException e) {
        } catch (SecurityException e2) {
        }
        final Cursor contactsCursor = cursor;
        cursors.add(contactsCursor);
        return new MergeCursor((Cursor[]) cursors.toArray(new Cursor[cursors.size()])) {
            @Override
            public Bundle getExtras() {
                return contactsCursor == null ? new Bundle() : contactsCursor.getExtras();
            }
        };
    }

    private MatrixCursor loadProfile() {
        Cursor cursor = getContext().getContentResolver().query(ContactsContract.Profile.CONTENT_URI, this.mProjection, null, null, null);
        if (cursor == null) {
            return null;
        }
        try {
            MatrixCursor matrix = new MatrixCursor(this.mProjection);
            Object[] row = new Object[this.mProjection.length];
            while (cursor.moveToNext()) {
                for (int i = 0; i < row.length; i++) {
                    row[i] = cursor.getString(i);
                }
                matrix.addRow(row);
            }
            return matrix;
        } finally {
            cursor.close();
        }
    }
}
