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
import com.mediatek.contacts.ExtensionManager;
import com.mediatek.contacts.util.ContactsPortableUtils;
import com.mediatek.contacts.util.Log;
import java.util.ArrayList;

public class PhoneNumbersPickerAdapter extends DataKindBasePickerAdapter {
    protected static final String[] PHONES_PROJECTION;
    protected static final String[] PHONES_PROJECTION_INTERNAL = {"_id", "data2", "data3", "data1", "display_name", "display_name_alt", "contact_id", "lookup", "photo_id", "phonetic_name"};
    private int mAlternativeDisplayNameColumnIndex;
    private Context mContext;
    private int mDisplayNameColumnIndex;
    private CharSequence mUnknownNameText;

    static {
        ArrayList arrayListNewArrayList = Lists.newArrayList(PHONES_PROJECTION_INTERNAL);
        if (ContactsPortableUtils.MTK_PHONE_BOOK_SUPPORT) {
            arrayListNewArrayList.add("indicate_phone_or_sim_contact");
            arrayListNewArrayList.add("is_sdn_contact");
        }
        PHONES_PROJECTION = (String[]) arrayListNewArrayList.toArray(new String[arrayListNewArrayList.size()]);
    }

    public PhoneNumbersPickerAdapter(Context context, ListView listView) {
        super(context, listView);
        this.mContext = context;
        this.mUnknownNameText = context.getText(R.string.unknownName);
        super.displayPhotoOnLeft();
    }

    @Override
    protected Uri configLoaderUri(long j) {
        boolean zIsSearchMode = isSearchMode();
        Log.i("PhoneNumbersPickerAdapter", "[configLoaderUri]directoryId = " + j + ",isSearchMode = " + zIsSearchMode);
        if (j != 0) {
            Log.w("PhoneNumbersPickerAdapter", "[configLoaderUri] PhoneNumberListAdapter is not ready for non-default directory ID (directoryId: " + j + ")");
        }
        if (zIsSearchMode) {
            String queryString = getQueryString();
            Uri.Builder builderBuildUpon = ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI.buildUpon();
            if (TextUtils.isEmpty(queryString)) {
                builderBuildUpon.appendPath("");
            } else {
                builderBuildUpon.appendPath(queryString);
            }
            builderBuildUpon.appendQueryParameter("directory", String.valueOf(j));
            builderBuildUpon.appendQueryParameter("checked_ids_arg", ContactsContract.CommonDataKinds.Phone.CONTENT_URI.toString());
            return builderBuildUpon.build();
        }
        Uri uriBuild = ContactsContract.CommonDataKinds.Phone.CONTENT_URI.buildUpon().appendQueryParameter("directory", String.valueOf(0L)).appendQueryParameter("checked_ids_arg", ContactsContract.CommonDataKinds.Phone.CONTENT_URI.toString()).build();
        if (isSectionHeaderDisplayEnabled()) {
            uriBuild = buildSectionIndexerUri(uriBuild);
        }
        ExtensionManager.getInstance();
        return ExtensionManager.getRcsExtension().configListUri(uriBuild);
    }

    @Override
    protected String[] configProjection() {
        return PHONES_PROJECTION;
    }

    @Override
    protected void configureSelection(CursorLoader cursorLoader, long j, ContactListFilter contactListFilter) {
        if (contactListFilter == null || j != 0) {
            Log.w("PhoneNumbersPickerAdapter", "[configureSelection]directoryId = " + j + ",filter = " + contactListFilter);
            return;
        }
        StringBuilder sb = new StringBuilder();
        ArrayList arrayList = new ArrayList();
        Log.i("PhoneNumbersPickerAdapter", "[configureSelection]filterType = " + contactListFilter.filterType);
        int i = contactListFilter.filterType;
        if (i != -5) {
            switch (i) {
                case -3:
                    sb.append("in_visible_group=1");
                    sb.append(" AND has_phone_number=1");
                    break;
                case -2:
                case -1:
                    break;
                case 0:
                    sb.append("(");
                    sb.append("account_type=? AND account_name=?");
                    arrayList.add(contactListFilter.accountType);
                    arrayList.add(contactListFilter.accountName);
                    if (contactListFilter.dataSet != null) {
                        sb.append(" AND data_set=?");
                        arrayList.add(contactListFilter.dataSet);
                    } else {
                        sb.append(" AND data_set IS NULL");
                    }
                    sb.append(")");
                    break;
                default:
                    Log.w("PhoneNumbersPickerAdapter", "Unsupported filter type came (type: " + contactListFilter.filterType + ", toString: " + contactListFilter + ") showing all contacts.");
                    break;
            }
        }
        ExtensionManager.getInstance();
        ExtensionManager.getRcsExtension().setListFilter(sb, this.mContext);
        cursorLoader.setSelection(sb.toString());
        cursorLoader.setSelectionArgs((String[]) arrayList.toArray(new String[0]));
    }

    @Override
    public void setContactNameDisplayOrder(int i) {
        super.setContactNameDisplayOrder(i);
        int contactNameDisplayOrder = getContactNameDisplayOrder();
        Log.i("PhoneNumbersPickerAdapter", "[setContactNameDisplayOrder]displayOrder = " + i + ",nameDisplayOrder = " + contactNameDisplayOrder);
        if (contactNameDisplayOrder == 1) {
            this.mDisplayNameColumnIndex = 4;
            this.mAlternativeDisplayNameColumnIndex = 5;
        } else {
            this.mDisplayNameColumnIndex = 5;
            this.mAlternativeDisplayNameColumnIndex = 4;
        }
    }

    @Override
    public void bindName(ContactListItemView contactListItemView, Cursor cursor) {
        contactListItemView.showDisplayName(cursor, this.mDisplayNameColumnIndex, this.mAlternativeDisplayNameColumnIndex);
        contactListItemView.showPhoneticName(cursor, getPhoneticNameColumnIndex());
    }

    @Override
    protected void bindData(ContactListItemView contactListItemView, Cursor cursor) {
        contactListItemView.showData(cursor, getDataColumnIndex());
    }

    @Override
    public void bindQuickContact(ContactListItemView contactListItemView, int i, Cursor cursor) {
    }

    @Override
    public int getDisplayNameColumnIdex() {
        return 4;
    }

    @Override
    public int getPhotoIDColumnIndex() {
        return 8;
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
    public int getContactIDColumnIndex() {
        return 6;
    }

    public int getPhoneticNameColumnIndex() {
        return 9;
    }

    @Override
    public long getDataId(int i) {
        Cursor cursor = (Cursor) getItem(i);
        if (cursor != null) {
            return cursor.getLong(0);
        }
        Log.w("PhoneNumbersPickerAdapter", "[getDataId] Cursor was null in getDataId() call. Returning 0 instead.");
        return 0L;
    }

    @Override
    public int getLookupKeyColumnIndex() {
        return 7;
    }
}
