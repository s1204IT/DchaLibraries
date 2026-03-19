package com.android.contacts.list;

import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.view.View;
import com.android.contacts.compat.ContactsCompat;
import com.android.contacts.model.account.AccountWithDataSet;
import com.android.contacts.preference.ContactsPreferences;
import com.mediatek.contacts.util.ContactsCommonListUtils;
import com.mediatek.contacts.util.ContactsPortableUtils;
import com.mediatek.contacts.util.Log;
import java.util.ArrayList;
import java.util.List;

public class DefaultContactListAdapter extends ContactListAdapter {
    private boolean mOnlyShowPhoneContacts;

    public DefaultContactListAdapter(Context context) {
        super(context);
        this.mOnlyShowPhoneContacts = false;
    }

    @Override
    public void configureLoader(CursorLoader cursorLoader, long j) {
        String str;
        if (cursorLoader instanceof FavoritesAndContactsLoader) {
            if (ContactsPortableUtils.MTK_PHONE_BOOK_SUPPORT) {
                this.mSDNLoader = (FavoritesAndContactsLoader) cursorLoader;
            }
            ((FavoritesAndContactsLoader) cursorLoader).setLoadFavorites(shouldIncludeFavorites());
        }
        String str2 = null;
        Log.d("DefaultContactListAdapter", "[configureLoader] loader:" + cursorLoader + ",isSearchMode:" + isSearchMode());
        if (isSearchMode()) {
            String queryString = getQueryString();
            if (queryString == null) {
                queryString = "";
            }
            String strTrim = queryString.trim();
            if (TextUtils.isEmpty(strTrim)) {
                cursorLoader.setUri(ContactsContract.Contacts.CONTENT_URI);
                cursorLoader.setProjection(getProjection(false));
                cursorLoader.setSelection("0");
            } else if (isGroupMembersFilter()) {
                configureUri(cursorLoader, j, getFilter());
                cursorLoader.setProjection(getProjection(false));
                cursorLoader.setSelection("display_name LIKE ?1 OR display_name_alt LIKE ?1");
                cursorLoader.setSelectionArgs(new String[]{strTrim + "%"});
            } else {
                Uri.Builder builderBuildUpon = ContactsCompat.getContentUri().buildUpon();
                appendSearchParameters(builderBuildUpon, strTrim, j);
                cursorLoader.setUri(builderBuildUpon.build());
                cursorLoader.setProjection(getProjection(true));
                str2 = "(CASE WHEN (strftime('%s', 'now') - last_time_contacted/1000) < 259200 THEN 0  WHEN (strftime('%s', 'now') - last_time_contacted/1000) < 604800 THEN 1  WHEN (strftime('%s', 'now') - last_time_contacted/1000) < 1209600 THEN 2  WHEN (strftime('%s', 'now') - last_time_contacted/1000) < 2592000 THEN 3  ELSE 4 END), times_contacted DESC, starred DESC";
            }
        } else {
            ContactListFilter filter = getFilter();
            configureUri(cursorLoader, j, filter);
            if (filter != null && filter.filterType == -8) {
                cursorLoader.setProjection(getDataProjectionForContacts(false));
            } else {
                cursorLoader.setProjection(getProjection(false));
            }
            configureSelection(cursorLoader, j, filter);
        }
        if (this.mOnlyShowPhoneContacts) {
            ContactsCommonListUtils.configureOnlyShowPhoneContactsSelection(cursorLoader, j, getFilter());
        }
        if (getSortOrder() == 1) {
            if (str2 == null) {
                str = "sort_key";
            } else {
                str = str2 + ", sort_key";
            }
        } else if (str2 == null) {
            str = "sort_key_alt";
        } else {
            str = str2 + ", sort_key_alt";
        }
        cursorLoader.setSortOrder(str);
    }

    private boolean isGroupMembersFilter() {
        ContactListFilter filter = getFilter();
        return filter != null && filter.filterType == -7;
    }

    private void appendSearchParameters(Uri.Builder builder, String str, long j) {
        builder.appendPath(str);
        builder.appendQueryParameter("directory", String.valueOf(j));
        if (j != 0 && j != 1) {
            builder.appendQueryParameter("limit", String.valueOf(getDirectoryResultLimit(getDirectoryById(j))));
        }
        builder.appendQueryParameter("deferred_snippeting", "1");
    }

    protected void configureUri(CursorLoader cursorLoader, long j, ContactListFilter contactListFilter) {
        Uri uriBuild = ContactsContract.Contacts.CONTENT_URI;
        if (contactListFilter != null) {
            if (contactListFilter.filterType == -6) {
                String selectedContactLookupKey = getSelectedContactLookupKey();
                uriBuild = selectedContactLookupKey != null ? Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, selectedContactLookupKey) : ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, getSelectedContactId());
            } else if (contactListFilter.filterType == -8) {
                uriBuild = ContactsContract.Data.CONTENT_URI;
            }
        }
        if (j == 0 && isSectionHeaderDisplayEnabled()) {
            uriBuild = ContactListAdapter.buildSectionIndexerUri(uriBuild);
        }
        if (contactListFilter != null && contactListFilter.filterType != -3 && contactListFilter.filterType != -6) {
            Uri.Builder builderBuildUpon = uriBuild.buildUpon();
            if (contactListFilter.filterType == -7) {
                contactListFilter.addAccountQueryParameterToUrl(builderBuildUpon);
            }
            uriBuild = builderBuildUpon.build();
        }
        cursorLoader.setUri(uriBuild);
    }

    protected void configureSelection(CursorLoader cursorLoader, long j, ContactListFilter contactListFilter) {
        if (contactListFilter == null) {
            Log.d("DefaultContactListAdapter", "[configureSelection] filter is null !!");
            return;
        }
        if (j != 0) {
            Log.d("DefaultContactListAdapter", "[configureSelection] directoryId is not DEFAULT: " + j);
            return;
        }
        StringBuilder sb = new StringBuilder();
        ArrayList arrayList = new ArrayList();
        int i = contactListFilter.filterType;
        if (i == 0) {
            buildSelectionForFilterAccount(contactListFilter, sb, arrayList);
        } else {
            switch (i) {
                case -8:
                    if (contactListFilter.accountType != null) {
                        sb.append("account_type");
                        sb.append("=?");
                        arrayList.add(contactListFilter.accountType);
                        if (contactListFilter.accountName != null) {
                            sb.append(" AND ");
                            sb.append("account_name");
                            sb.append("=?");
                            arrayList.add(contactListFilter.accountName);
                        }
                        if (ContactsPortableUtils.MTK_PHONE_BOOK_SUPPORT) {
                            sb.append(" AND is_sdn_contact < 1");
                        }
                    } else {
                        sb.append(AccountWithDataSet.LOCAL_ACCOUNT_SELECTION);
                    }
                    break;
                case -7:
                    if (ContactsPortableUtils.MTK_PHONE_BOOK_SUPPORT) {
                        sb.append("is_sdn_contact < 1");
                    }
                    break;
                case -5:
                    sb.append("has_phone_number=1");
                    if (ContactsPortableUtils.MTK_PHONE_BOOK_SUPPORT) {
                        sb.append(" AND is_sdn_contact < 1");
                    }
                    break;
                case -4:
                    sb.append("starred!=0");
                    break;
                case -3:
                    sb.append("in_visible_group=1");
                    if (isCustomFilterForPhoneNumbersOnly()) {
                        sb.append(" AND has_phone_number=1");
                    }
                    if (ContactsPortableUtils.MTK_PHONE_BOOK_SUPPORT) {
                        sb.append(" AND is_sdn_contact < 1");
                    }
                    break;
                case -2:
                    if (ContactsPortableUtils.MTK_PHONE_BOOK_SUPPORT) {
                        sb.append("is_sdn_contact < 1");
                    }
                    break;
            }
        }
        Log.d("DefaultContactListAdapter", "[configureSelection] selection: " + sb.toString() + ", filter.filterType: " + contactListFilter.filterType);
        cursorLoader.setSelection(sb.toString());
        cursorLoader.setSelectionArgs((String[]) arrayList.toArray(new String[0]));
    }

    @Override
    protected void bindView(View view, int i, Cursor cursor, int i2) {
        super.bindView(view, i, cursor, i2);
        ContactListItemView contactListItemView = (ContactListItemView) view;
        contactListItemView.setHighlightedPrefix(isSearchMode() ? getUpperCaseQueryString() : null);
        if (isSelectionVisible()) {
            contactListItemView.setActivated(isSelectedContact(i, cursor));
        }
        bindSectionHeaderAndDivider(contactListItemView, i2, cursor);
        contactListItemView.bindDataForCommonPresenceView(cursor.getLong(0));
        if (isQuickContactEnabled()) {
            bindQuickContact(contactListItemView, i, cursor, 4, 5, 0, 6, 1);
        } else if (getDisplayPhotos()) {
            bindPhoto(contactListItemView, i, cursor);
        }
        bindNameAndViewId(contactListItemView, cursor);
        bindPresenceAndStatusMessage(contactListItemView, cursor);
        if (isSearchMode()) {
            bindSearchSnippet(contactListItemView, cursor);
        } else {
            contactListItemView.setSnippet(null);
        }
    }

    private boolean isCustomFilterForPhoneNumbersOnly() {
        return PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean(ContactsPreferences.PREF_DISPLAY_ONLY_PHONES, false);
    }

    public void setOnlyShowPhoneContacts(boolean z) {
        this.mOnlyShowPhoneContacts = z;
    }

    private void buildSelectionForFilterAccount(ContactListFilter contactListFilter, StringBuilder sb, List<String> list) {
        if ("Local Phone Account".equals(contactListFilter.accountType)) {
            sb.append("EXISTS (SELECT DISTINCT contact_id FROM view_raw_contacts WHERE ( ");
            if (ContactsPortableUtils.MTK_PHONE_BOOK_SUPPORT) {
                sb.append("is_sdn_contact < 1 AND ");
            }
            sb.append("contact_id = view_contacts._id AND (account_type IS NULL  AND account_name IS NULL  AND data_set IS NULL  OR account_type=?  AND account_name=? ");
        } else {
            sb.append("EXISTS (SELECT DISTINCT contact_id FROM view_raw_contacts WHERE ( ");
            if (ContactsPortableUtils.MTK_PHONE_BOOK_SUPPORT) {
                sb.append("is_sdn_contact < 1 AND ");
            }
            sb.append("contact_id = view_contacts._id AND (account_type=? AND account_name=?");
        }
        ContactsCommonListUtils.buildSelectionForFilterAccount(contactListFilter, sb, list);
    }
}
