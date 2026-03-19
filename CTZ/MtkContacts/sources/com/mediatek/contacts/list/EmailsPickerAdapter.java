package com.mediatek.contacts.list;

import android.R;
import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.widget.ListView;
import com.android.contacts.list.ContactListFilter;
import com.android.contacts.list.ContactListItemView;
import com.google.common.collect.Lists;
import com.mediatek.contacts.util.ContactsPortableUtils;
import com.mediatek.contacts.util.Log;
import java.util.ArrayList;

public class EmailsPickerAdapter extends DataKindBasePickerAdapter {
    static final String[] EMAILS_PROJECTION;
    static final String[] EMAILS_PROJECTION_INTERNAL = {"_id", "data2", "data3", "data1", "display_name", "display_name_alt", "contact_id", "lookup", "photo_id", "phonetic_name"};
    private int mAlternativeDisplayNameColumnIndex;
    private int mDisplayNameColumnIndex;
    private CharSequence mUnknownNameText;

    static {
        ArrayList arrayListNewArrayList = Lists.newArrayList(EMAILS_PROJECTION_INTERNAL);
        if (ContactsPortableUtils.MTK_PHONE_BOOK_SUPPORT) {
            arrayListNewArrayList.add("indicate_phone_or_sim_contact");
            arrayListNewArrayList.add("is_sdn_contact");
        }
        EMAILS_PROJECTION = (String[]) arrayListNewArrayList.toArray(new String[arrayListNewArrayList.size()]);
    }

    public EmailsPickerAdapter(Context context, ListView listView) {
        super(context, listView);
        this.mUnknownNameText = context.getText(R.string.unknownName);
        super.displayPhotoOnLeft();
    }

    @Override
    public Uri configLoaderUri(long j) {
        Uri.Builder builderBuildUpon;
        boolean zIsSearchMode = isSearchMode();
        Log.i("EmailsPickerAdapter", "[configLoaderUri]directoryId = " + j + ",isSearchMode = " + zIsSearchMode);
        if (zIsSearchMode) {
            builderBuildUpon = ContactsContract.CommonDataKinds.Email.CONTENT_FILTER_URI.buildUpon();
            String queryString = getQueryString();
            if (TextUtils.isEmpty(queryString)) {
                queryString = "";
            }
            builderBuildUpon.appendPath(queryString);
        } else {
            builderBuildUpon = ContactsContract.CommonDataKinds.Email.CONTENT_URI.buildUpon();
        }
        builderBuildUpon.appendQueryParameter("directory", String.valueOf(j));
        builderBuildUpon.appendQueryParameter("checked_ids_arg", ContactsContract.CommonDataKinds.Email.CONTENT_URI.toString());
        Uri uriBuild = builderBuildUpon.build();
        if (isSectionHeaderDisplayEnabled()) {
            return buildSectionIndexerUri(uriBuild);
        }
        return uriBuild;
    }

    @Override
    public String[] configProjection() {
        return EMAILS_PROJECTION;
    }

    @Override
    protected void configureSelection(CursorLoader cursorLoader, long j, ContactListFilter contactListFilter) {
    }

    @Override
    public void setContactNameDisplayOrder(int i) {
        super.setContactNameDisplayOrder(i);
        if (getContactNameDisplayOrder() == 1) {
            this.mDisplayNameColumnIndex = 4;
            this.mAlternativeDisplayNameColumnIndex = 5;
        } else {
            this.mDisplayNameColumnIndex = 5;
            this.mAlternativeDisplayNameColumnIndex = 4;
        }
    }

    @Override
    public void bindName(ContactListItemView contactListItemView, Cursor cursor) {
        Log.d("EmailsPickerAdapter", "[bindName]");
        contactListItemView.showDisplayName(cursor, this.mDisplayNameColumnIndex, this.mAlternativeDisplayNameColumnIndex);
        contactListItemView.showPhoneticName(cursor, getPhoneticNameColumnIndex());
    }

    @Override
    protected void bindData(ContactListItemView contactListItemView, Cursor cursor) {
        contactListItemView.getLabelView().setText("");
        contactListItemView.showData(cursor, getDataColumnIndex());
    }

    @Override
    public void bindQuickContact(ContactListItemView contactListItemView, int i, Cursor cursor) {
    }

    @Override
    public int getContactIDColumnIndex() {
        return 6;
    }

    @Override
    public int getDataColumnIndex() {
        return 3;
    }

    @Override
    public int getDataLabelColumnIndex() {
        return 2;
    }

    @Override
    public int getDataTypeColumnIndex() {
        return 1;
    }

    @Override
    public long getDataId(int i) {
        return ((Cursor) getItem(i)).getLong(0);
    }

    @Override
    public int getDisplayNameColumnIdex() {
        return 4;
    }

    @Override
    public int getPhotoIDColumnIndex() {
        return 8;
    }

    public int getPhoneticNameColumnIndex() {
        return 9;
    }

    @Override
    public int getLookupKeyColumnIndex() {
        return 7;
    }
}
