package com.android.contacts.list;

import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.database.MergeCursor;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.provider.ContactsContract;
import com.google.common.collect.Lists;
import com.mediatek.contacts.util.ContactsCommonListUtils;
import com.mediatek.contacts.util.ContactsPortableUtils;
import com.mediatek.contacts.util.Log;
import java.util.ArrayList;

public class FavoritesAndContactsLoader extends CursorLoader {
    private boolean mLoadFavorites;
    private String[] mProjection;
    private int mSdnContactCount;

    public FavoritesAndContactsLoader(Context context) {
        super(context);
        this.mSdnContactCount = 0;
    }

    public void setLoadFavorites(boolean z) {
        this.mLoadFavorites = z;
    }

    @Override
    public void setProjection(String[] strArr) {
        super.setProjection(strArr);
        this.mProjection = strArr;
    }

    @Override
    public Cursor loadInBackground() {
        Log.d("FavoritesAndContactsLoader", "[loadInBackground]");
        ArrayList arrayListNewArrayList = Lists.newArrayList();
        if (this.mLoadFavorites) {
            arrayListNewArrayList.add(loadFavoritesContacts());
        }
        this.mSdnContactCount = 0;
        if (ContactsPortableUtils.MTK_PHONE_BOOK_SUPPORT) {
            this.mSdnContactCount = ContactsCommonListUtils.addCursorAndSetSelection(getContext(), this, arrayListNewArrayList, this.mSdnContactCount);
        }
        final Cursor cursorLoadContacts = loadContacts();
        arrayListNewArrayList.add(cursorLoadContacts);
        return new MergeCursor((Cursor[]) arrayListNewArrayList.toArray(new Cursor[arrayListNewArrayList.size()])) {
            @Override
            public Bundle getExtras() {
                return cursorLoadContacts == null ? new Bundle() : cursorLoadContacts.getExtras();
            }
        };
    }

    private Cursor loadContacts() {
        try {
            return super.loadInBackground();
        } catch (SQLiteException | NullPointerException | SecurityException e) {
            return null;
        }
    }

    private Cursor loadFavoritesContacts() {
        StringBuilder sb = new StringBuilder();
        sb.append("starred=?");
        ContactListFilter filter = ContactListFilterController.getInstance(getContext()).getFilter();
        if (filter != null && filter.filterType == -3) {
            sb.append(" AND ");
            sb.append("in_visible_group=1");
        }
        Log.d("FavoritesAndContactsLoader", "[loadFavoritesContacts] projection: " + sb.toString());
        return getContext().getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, this.mProjection, sb.toString(), new String[]{"1"}, getSortOrder());
    }

    public int getSdnContactCount() {
        return this.mSdnContactCount;
    }

    @Override
    protected void onStartLoading() {
        forceLoad();
    }
}
