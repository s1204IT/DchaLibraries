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
import com.mediatek.contacts.GlobalEnv;
import com.mediatek.contacts.util.ContactsPortableUtils;
import com.mediatek.contacts.util.Log;
import java.util.ArrayList;

public class PhoneAndEmailsPickerAdapter extends DataKindBasePickerAdapter {
    static final String[] PHONE_EMAIL_PROJECTION;
    private int mAlternativeDisplayNameColumnIndex;
    private Context mContext;
    private int mDisplayNameColumnIndex;
    private CharSequence mUnknownNameText;
    public static final Uri PICK_PHONE_EMAIL_URI = Uri.parse("content://com.android.contacts/data/phone_email");
    public static final Uri PICK_PHONE_EMAIL_FILTER_URI = Uri.withAppendedPath(PICK_PHONE_EMAIL_URI, "filter");
    static final String[] PHONE_EMAIL_PROJECTION_INTERNAL = {"_id", "data2", "data3", "data1", "display_name", "display_name_alt", "contact_id", "lookup", "photo_id", "phonetic_name", "mimetype"};

    static {
        ArrayList arrayListNewArrayList = Lists.newArrayList(PHONE_EMAIL_PROJECTION_INTERNAL);
        if (ContactsPortableUtils.MTK_PHONE_BOOK_SUPPORT) {
            arrayListNewArrayList.add("indicate_phone_or_sim_contact");
            arrayListNewArrayList.add("is_sdn_contact");
        }
        PHONE_EMAIL_PROJECTION = (String[]) arrayListNewArrayList.toArray(new String[arrayListNewArrayList.size()]);
    }

    public PhoneAndEmailsPickerAdapter(Context context, ListView listView) {
        super(context, listView);
        this.mContext = context;
        this.mUnknownNameText = context.getText(R.string.unknownName);
        super.displayPhotoOnLeft();
    }

    @Override
    protected Uri configLoaderUri(long j) {
        Uri.Builder builderBuildUpon;
        boolean zIsSearchMode = isSearchMode();
        Log.i("PhoneAndEmailsPickerAdapter", "[configLoaderUri]directoryId = " + j + ",isSearchMode = " + zIsSearchMode);
        if (zIsSearchMode) {
            String queryString = getQueryString();
            if (queryString == null) {
                queryString = "";
            }
            String strTrim = queryString.trim();
            if (TextUtils.isEmpty(strTrim)) {
                builderBuildUpon = PICK_PHONE_EMAIL_URI.buildUpon();
            } else {
                Uri.Builder builderBuildUpon2 = PICK_PHONE_EMAIL_FILTER_URI.buildUpon();
                builderBuildUpon2.appendPath(strTrim);
                builderBuildUpon2.appendQueryParameter("directory", String.valueOf(j));
                if (j != 0 && j != 1) {
                    builderBuildUpon2.appendQueryParameter("limit", String.valueOf(getDirectoryResultLimit()));
                }
                builderBuildUpon2.appendQueryParameter("snippet_args", "\u0001,\u0001,…,5");
                builderBuildUpon2.appendQueryParameter("deferred_snippeting", "1");
                builderBuildUpon = builderBuildUpon2;
            }
        } else {
            Uri.Builder builderBuildUpon3 = PICK_PHONE_EMAIL_URI.buildUpon();
            builderBuildUpon3.appendQueryParameter("directory", String.valueOf(j));
            builderBuildUpon = builderBuildUpon3;
        }
        builderBuildUpon.appendQueryParameter("checked_ids_arg", PICK_PHONE_EMAIL_URI.toString());
        Uri uriBuild = builderBuildUpon.build();
        if (isSectionHeaderDisplayEnabled()) {
            return buildSectionIndexerUri(uriBuild);
        }
        return uriBuild;
    }

    @Override
    protected String[] configProjection() {
        return PHONE_EMAIL_PROJECTION;
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
        Log.i("PhoneAndEmailsPickerAdapter", "[setContactNameDisplayOrder]displayOrder = " + i + ",mDisplayNameColumnIndex = " + this.mDisplayNameColumnIndex);
    }

    @Override
    public void bindName(ContactListItemView contactListItemView, Cursor cursor) {
        contactListItemView.showDisplayName(cursor, this.mDisplayNameColumnIndex, this.mAlternativeDisplayNameColumnIndex);
        contactListItemView.showPhoneticName(cursor, getPhoneticNameColumnIndex());
    }

    @Override
    protected void bindData(ContactListItemView contactListItemView, Cursor cursor) {
        CharSequence typeLabel;
        if (!cursor.isNull(getDataTypeColumnIndex())) {
            int i = cursor.getInt(getDataTypeColumnIndex());
            String string = cursor.getString(getDataLabelColumnIndex());
            String string2 = cursor.getString(10);
            Log.i("PhoneAndEmailsPickerAdapter", "[bindData]type = " + i + ",customLabel = " + string + ",mimeType = " + string2);
            if (string2.equals("vnd.android.cursor.item/email_v2")) {
                typeLabel = ContactsContract.CommonDataKinds.Email.getTypeLabel(this.mContext.getResources(), i, string);
            } else {
                typeLabel = ContactsContract.CommonDataKinds.Phone.getTypeLabel(this.mContext.getResources(), i, string);
            }
            GlobalEnv.getSimAasEditor().getLabelForBindData(this.mContext.getResources(), i, string, string2, cursor, typeLabel);
        }
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
        if (((Cursor) getItem(i)) != null) {
            return ((Cursor) getItem(i)).getLong(0);
        }
        Log.w("PhoneAndEmailsPickerAdapter", "[getDataId] The getItem is null");
        return -1L;
    }

    @Override
    public int getDisplayNameColumnIdex() {
        return 4;
    }

    public int getPhoneticNameColumnIndex() {
        return 9;
    }

    @Override
    public int getPhotoIDColumnIndex() {
        return 8;
    }

    @Override
    public int getLookupKeyColumnIndex() {
        return 7;
    }
}
