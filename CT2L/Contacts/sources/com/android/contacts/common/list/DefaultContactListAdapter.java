package com.android.contacts.common.list;

import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

public class DefaultContactListAdapter extends ContactListAdapter {
    private ArrayList<ContactListFilter> mFilters;

    public DefaultContactListAdapter(Context context) {
        super(context);
    }

    @Override
    public void configureLoader(CursorLoader loader, long directoryId) {
        String sortOrder;
        if (loader instanceof ProfileAndContactsLoader) {
            ((ProfileAndContactsLoader) loader).setLoadProfile(shouldIncludeProfile());
        }
        ContactListFilter filter = getFilter();
        if (isSearchMode()) {
            String query = getQueryString();
            if (query == null) {
                query = "";
            }
            String query2 = query.trim();
            if (TextUtils.isEmpty(query2)) {
                loader.setUri(ContactsContract.Contacts.CONTENT_URI);
                loader.setProjection(getProjection(false));
                loader.setSelection("0");
            } else {
                Uri.Builder builder = ContactsContract.Contacts.CONTENT_FILTER_URI.buildUpon();
                builder.appendPath(query2);
                builder.appendQueryParameter("directory", String.valueOf(directoryId));
                if (directoryId != 0 && directoryId != 1) {
                    builder.appendQueryParameter("limit", String.valueOf(getDirectoryResultLimit(getDirectoryById(directoryId))));
                }
                builder.appendQueryParameter("deferred_snippeting", "1");
                loader.setUri(builder.build());
                loader.setProjection(getProjection(true));
            }
        } else {
            configureUri(loader, directoryId, filter);
            loader.setProjection(getProjection(false));
            configureSelection(loader, directoryId, filter);
        }
        configureAccountsSelection(loader);
        if (getSortOrder() == 1) {
            sortOrder = "sort_key";
        } else {
            sortOrder = "sort_key_alt";
        }
        loader.setSortOrder(sortOrder);
    }

    protected void configureAccountsSelection(CursorLoader loader) {
        StringBuilder selectionBuilder;
        if (this.mFilters != null && this.mFilters.size() != 0) {
            String selection = loader.getSelection();
            if (selection != null) {
                selectionBuilder = new StringBuilder(selection);
            } else {
                selectionBuilder = new StringBuilder();
            }
            for (int i = 0; i < this.mFilters.size(); i++) {
                if (this.mFilters.get(i).filterType == 0) {
                    Log.d("DefaultContactListAdapter", "mFilters.get(" + i + ").accountType = " + this.mFilters.get(i).accountType);
                    if (selectionBuilder.length() == 0) {
                        selectionBuilder.append("account_type==\"" + this.mFilters.get(i).accountType + "\"");
                    } else {
                        selectionBuilder.append(" OR account_type==\"" + this.mFilters.get(i).accountType + "\"");
                    }
                }
            }
            loader.setSelection(selectionBuilder.toString());
        }
    }

    protected void configureUri(CursorLoader loader, long directoryId, ContactListFilter filter) {
        Uri uri = ContactsContract.Contacts.CONTENT_URI;
        if (filter != null && filter.filterType == -6) {
            String lookupKey = getSelectedContactLookupKey();
            uri = lookupKey != null ? Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, lookupKey) : ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, getSelectedContactId());
        }
        if (directoryId == 0 && isSectionHeaderDisplayEnabled()) {
            uri = ContactListAdapter.buildSectionIndexerUri(uri);
        }
        if (filter != null && filter.filterType != -3 && filter.filterType != -6) {
            Uri.Builder builder = uri.buildUpon();
            builder.appendQueryParameter("directory", String.valueOf(0L));
            if (filter.filterType == 0) {
                filter.addAccountQueryParameterToUrl(builder);
            }
            uri = builder.build();
        }
        Log.d("DefaultContactListAdapter", "uri = " + uri);
        loader.setUri(uri);
    }

    private void configureSelection(CursorLoader loader, long directoryId, ContactListFilter filter) {
        if (filter != null && directoryId == 0) {
            StringBuilder selection = new StringBuilder();
            List<String> selectionArgs = new ArrayList<>();
            switch (filter.filterType) {
                case -5:
                    selection.append("has_phone_number=1");
                    break;
                case -4:
                    selection.append("starred!=0");
                    break;
                case -3:
                    selection.append("in_visible_group=1");
                    if (isCustomFilterForPhoneNumbersOnly()) {
                        selection.append(" AND has_phone_number=1");
                    }
                    break;
            }
            loader.setSelection(selection.toString());
            loader.setSelectionArgs((String[]) selectionArgs.toArray(new String[0]));
        }
    }

    @Override
    protected void bindView(View itemView, int partition, Cursor cursor, int position) {
        boolean isSimAccount;
        super.bindView(itemView, partition, cursor, position);
        ContactListItemView view = (ContactListItemView) itemView;
        long contactId = cursor.getLong(0);
        String accountType = cursor.getString(8);
        int subId = Integer.MAX_VALUE;
        Log.d("DefaultContactListAdapter", "bindView() cursor = " + cursor + " accountType = " + accountType + " contacts_id = " + contactId);
        view.setHighlightedPrefix(isSearchMode() ? getUpperCaseQueryString() : null);
        if (isSelectionVisible()) {
            view.setActivated(isSelectedContact(partition, cursor));
        }
        bindSectionHeaderAndDivider(view, position, cursor);
        if (accountType != null && accountType.equals("com.android.contact.sim")) {
            isSimAccount = true;
            int[] subs = SubscriptionManager.getSubId(0);
            if (subs != null) {
                subId = subs[0];
            }
        } else if (accountType != null && accountType.equals("com.android.contact.sim2")) {
            isSimAccount = true;
            int[] subs2 = SubscriptionManager.getSubId(1);
            if (subs2 != null) {
                subId = subs2[0];
            }
        } else {
            isSimAccount = false;
        }
        if (isQuickContactEnabled()) {
            if (isSimAccount) {
                bindSimQuickContact(subId, view, partition, cursor, 4, 5, 0, 6);
            } else {
                bindQuickContact(view, partition, cursor, 4, 5, 0, 6, 1);
            }
        } else if (getDisplayPhotos()) {
            if (isSimAccount) {
                bindSimPhoto(subId, view, partition, cursor);
            } else {
                bindPhoto(view, partition, cursor);
            }
        }
        if (isSimAccount) {
            bindSimName(subId, view, cursor);
        } else {
            bindNameAndViewId(view, cursor);
        }
        bindPresenceAndStatusMessage(view, cursor);
        if (isSearchMode()) {
            bindSearchSnippet(view, cursor);
        } else {
            view.setSnippet(null);
        }
    }

    private boolean isCustomFilterForPhoneNumbersOnly() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        return prefs.getBoolean("only_phones", false);
    }

    public void setFilters(ArrayList<ContactListFilter> filters) {
        this.mFilters = filters;
    }
}
