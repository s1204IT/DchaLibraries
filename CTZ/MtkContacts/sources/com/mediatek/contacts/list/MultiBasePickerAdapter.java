package com.mediatek.contacts.list;

import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.v7.widget.AppCompatCheckBox;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ListView;
import com.android.contacts.R;
import com.android.contacts.list.ContactListFilter;
import com.android.contacts.list.ContactListItemView;
import com.android.contacts.list.DefaultContactListAdapter;
import com.android.contacts.list.PinnedHeaderListView;
import com.mediatek.contacts.util.ContactsPortableUtils;
import com.mediatek.contacts.util.Log;
import com.mediatek.contacts.util.MtkToast;
import java.util.HashMap;

public class MultiBasePickerAdapter extends DefaultContactListAdapter {
    private final View.OnClickListener mCheckBoxClickListener;
    private int mFilterAccountOptions;
    private ListView mListView;
    private CursorLoader mLoader;
    private PickListItemCache mPickListItemCache;
    private SelectedContactsListener mSelectedContactsListener;

    public interface SelectedContactsListener {
        void onSelectedContactsChangedViaCheckBox();
    }

    public final class PickListItemCache {
        private HashMap<Long, PickListItemData> mMap = new HashMap<>();

        public PickListItemCache() {
        }

        public final class PickListItemData {
            public int contactIndicator;
            public String displayName;
            public String lookupUri;
            public int simIndex;

            public PickListItemData(int i, int i2, String str, String str2) {
                this.contactIndicator = i;
                this.simIndex = i2;
                this.displayName = str;
                this.lookupUri = str2;
            }

            public String toString() {
                return "[PickListItemData]@" + hashCode() + " contactIndicator: " + this.contactIndicator + ", simIndex: " + this.simIndex + ", displayName: " + this.displayName + ", lookupUri: " + this.lookupUri;
            }
        }

        public void add(Cursor cursor) {
            int i;
            int i2;
            long j = cursor.getInt(0);
            if (ContactsPortableUtils.MTK_PHONE_BOOK_SUPPORT) {
                int i3 = cursor.getInt(cursor.getColumnIndexOrThrow("indicate_phone_or_sim_contact"));
                i2 = cursor.getInt(cursor.getColumnIndexOrThrow("index_in_sim"));
                i = i3;
            } else {
                i = -1;
                i2 = -1;
            }
            this.mMap.put(Long.valueOf(j), new PickListItemData(i, i2, cursor.getString(1), cursor.getString(6)));
        }

        public boolean isEmpty() {
            return this.mMap.isEmpty();
        }

        public PickListItemData getItemData(long j) {
            return this.mMap.get(Long.valueOf(j));
        }
    }

    public MultiBasePickerAdapter(Context context, ListView listView) {
        super(context);
        this.mPickListItemCache = new PickListItemCache();
        this.mCheckBoxClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CheckBox checkBox = (CheckBox) view;
                Long l = (Long) checkBox.getTag();
                boolean z = MultiBasePickerAdapter.this.getSelectedContactIds().size() >= 3500;
                boolean zIsChecked = checkBox.isChecked();
                if (z && zIsChecked) {
                    Log.i("MultiBasePickerAdapter", "[mCheckBoxClickListener] Current selected Contact cnt > 3500,cannot select more");
                    checkBox.setChecked(false);
                    MtkToast.toast(MultiBasePickerAdapter.this.mContext.getApplicationContext(), MultiBasePickerAdapter.this.mContext.getResources().getString(R.string.multichoice_contacts_limit, 3500));
                } else {
                    if (checkBox.isChecked()) {
                        MultiBasePickerAdapter.this.getSelectedContactIds().add(l);
                    } else {
                        MultiBasePickerAdapter.this.getSelectedContactIds().remove(l);
                    }
                    if (MultiBasePickerAdapter.this.mSelectedContactsListener != null) {
                        MultiBasePickerAdapter.this.mSelectedContactsListener.onSelectedContactsChangedViaCheckBox();
                    }
                }
            }
        };
        this.mListView = listView;
    }

    @Override
    protected ContactListItemView newView(Context context, int i, Cursor cursor, int i2, ViewGroup viewGroup) {
        Log.i("MultiBasePickerAdapter", "[newView]partition = " + i + ",position = " + i2);
        ContactListItemView contactListItemViewNewView = super.newView(context, i, cursor, i2, viewGroup);
        contactListItemViewNewView.setCheckable(true);
        contactListItemViewNewView.setActivatedStateSupported(isSelectionVisible());
        return contactListItemViewNewView;
    }

    @Override
    protected void bindView(View view, int i, Cursor cursor, int i2) {
        Log.d("MultiBasePickerAdapter", "[bindView]partition = " + i + ",position = " + i2);
        ContactListItemView contactListItemView = (ContactListItemView) view;
        contactListItemView.setHighlightedPrefix(isSearchMode() ? getUpperCaseQueryString() : null);
        if (isSelectionVisible()) {
            contactListItemView.setActivated(isSelectedContact(i, cursor));
        }
        bindSectionHeaderAndDivider(contactListItemView, i2, cursor);
        contactListItemView.setIsSectionHeaderEnabled(false);
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
        bindCheckBox(contactListItemView, cursor, i2);
    }

    @Override
    public void configureLoader(CursorLoader cursorLoader, long j) {
        super.configureLoader(cursorLoader, j);
        ContactListFilter filter = getFilter();
        Log.d("MultiBasePickerAdapter", "[configureLoader]getFilter()=" + filter);
        if (isSearchMode()) {
            String queryString = getQueryString();
            if (queryString == null) {
                queryString = "";
            }
            if (!TextUtils.isEmpty(queryString.trim())) {
                configureSelection(cursorLoader, j, filter);
            }
        }
        Uri.Builder builderBuildUpon = cursorLoader.getUri().buildUpon();
        builderBuildUpon.appendQueryParameter("checked_ids_arg", ContactsContract.Contacts.CONTENT_URI.toString());
        cursorLoader.setUri(builderBuildUpon.build());
        Log.d("MultiBasePickerAdapter", "[configureLoader]setUri()=" + builderBuildUpon.build());
        this.mLoader = cursorLoader;
    }

    @Override
    protected void configureSelection(CursorLoader cursorLoader, long j, ContactListFilter contactListFilter) {
        if (contactListFilter == null) {
            Log.i("MultiBasePickerAdapter", "[configureSelection]filter is null,return.");
            return;
        }
        if (j != 0) {
            Log.i("MultiBasePickerAdapter", "[configureSelection]return,directoryId = " + j);
            return;
        }
        super.configureSelection(cursorLoader, j, contactListFilter);
        StringBuilder sb = new StringBuilder();
        Log.d("MultiBasePickerAdapter", "[configureSelection]getSelection=" + cursorLoader.getSelection());
        sb.append(cursorLoader.getSelection());
        if (this.mFilterAccountOptions == 100 || this.mFilterAccountOptions == 101) {
            sb.append(" AND has_phone_number=1");
        }
        if (!this.mShowSdnNumber) {
            sb.append(" AND is_sdn_contact=0");
        }
        Log.d("MultiBasePickerAdapter", "[configureSelection]setSelection=" + sb.toString());
        cursorLoader.setSelection(sb.toString());
    }

    public int getContactID(int i) {
        Cursor cursor = (Cursor) getItem(i);
        if (cursor == null) {
            return 0;
        }
        return cursor.getInt(cursor.getColumnIndexOrThrow("_id"));
    }

    public void cacheDataItem(Cursor cursor) {
        this.mPickListItemCache.add(cursor);
    }

    public PickListItemCache getListItemCache() {
        return this.mPickListItemCache;
    }

    @Override
    public void configurePinnedHeaders(PinnedHeaderListView pinnedHeaderListView) {
        super.configurePinnedHeaders(pinnedHeaderListView);
        pinnedHeaderListView.setDrawPinnedHeader(false);
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    private void bindCheckBox(ContactListItemView contactListItemView, Cursor cursor, int i) {
        Log.d("MultiBasePickerAdapter", "[bindCheckBox]position = " + i);
        AppCompatCheckBox checkBox = contactListItemView.getCheckBox();
        long j = cursor.getLong(0);
        checkBox.setChecked(getSelectedContactIds().contains(Long.valueOf(j)));
        checkBox.setTag(Long.valueOf(j));
        checkBox.setOnClickListener(this.mCheckBoxClickListener);
    }

    public void setSelectedContactsListener(SelectedContactsListener selectedContactsListener) {
        this.mSelectedContactsListener = selectedContactsListener;
    }
}
