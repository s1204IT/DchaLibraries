package com.mediatek.contacts.list;

import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.v7.widget.AppCompatCheckBox;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ListView;
import com.android.contacts.R;
import com.android.contacts.list.ContactListFilter;
import com.android.contacts.list.ContactListItemView;
import com.android.contacts.list.MultiSelectEntryContactListAdapter;
import com.android.contacts.list.PinnedHeaderListView;
import com.mediatek.contacts.GlobalEnv;
import com.mediatek.contacts.util.Log;
import com.mediatek.contacts.util.MtkToast;

public abstract class DataKindBasePickerAdapter extends MultiSelectEntryContactListAdapter {
    private final View.OnClickListener mCheckBoxClickListener;
    private Context mContext;
    private ListView mListView;
    private ContactListItemView.PhotoPosition mPhotoPosition;
    private SelectedContactsListener mSelectedContactsListener;

    public interface SelectedContactsListener {
        void onSelectedContactsChangedViaCheckBox();
    }

    public abstract void bindName(ContactListItemView contactListItemView, Cursor cursor);

    public abstract void bindQuickContact(ContactListItemView contactListItemView, int i, Cursor cursor);

    protected abstract Uri configLoaderUri(long j);

    protected abstract String[] configProjection();

    protected abstract void configureSelection(CursorLoader cursorLoader, long j, ContactListFilter contactListFilter);

    public abstract int getContactIDColumnIndex();

    public abstract int getDataColumnIndex();

    public abstract long getDataId(int i);

    public abstract int getDataLabelColumnIndex();

    public abstract int getDataTypeColumnIndex();

    public abstract int getDisplayNameColumnIdex();

    public abstract int getLookupKeyColumnIndex();

    public abstract int getPhotoIDColumnIndex();

    public DataKindBasePickerAdapter(Context context, ListView listView) {
        super(context, 0);
        this.mCheckBoxClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("DataKindBasePickerAdapter", "[mCheckBoxClickListener] onClick");
                CheckBox checkBox = (CheckBox) view;
                Long l = (Long) checkBox.getTag();
                boolean z = DataKindBasePickerAdapter.this.getSelectedContactIds().size() >= 3500;
                boolean zIsChecked = checkBox.isChecked();
                if (z && zIsChecked) {
                    Log.i("DataKindBasePickerAdapter", "[mCheckBoxClickListener] Current selected Contact cnt > 3500,cannot select more");
                    checkBox.setChecked(false);
                    MtkToast.toast(DataKindBasePickerAdapter.this.mContext.getApplicationContext(), DataKindBasePickerAdapter.this.mContext.getResources().getString(R.string.multichoice_contacts_limit, 3500));
                } else {
                    if (checkBox.isChecked()) {
                        DataKindBasePickerAdapter.this.getSelectedContactIds().add(l);
                    } else {
                        DataKindBasePickerAdapter.this.getSelectedContactIds().remove(l);
                    }
                    if (DataKindBasePickerAdapter.this.mSelectedContactsListener != null) {
                        DataKindBasePickerAdapter.this.mSelectedContactsListener.onSelectedContactsChangedViaCheckBox();
                    }
                }
            }
        };
        this.mListView = listView;
        this.mContext = context;
    }

    @Override
    public final void configureLoader(CursorLoader cursorLoader, long j) {
        String str;
        cursorLoader.setUri(configLoaderUri(j));
        cursorLoader.setProjection(configProjection());
        configureSelection(cursorLoader, j, getFilter());
        if (getSortOrder() == 1) {
            str = "sort_key";
        } else {
            str = "sort_key_alt";
        }
        cursorLoader.setSortOrder(str);
    }

    protected static Uri buildSectionIndexerUri(Uri uri) {
        return uri.buildUpon().appendQueryParameter("android.provider.extra.ADDRESS_BOOK_INDEX", "true").build();
    }

    @Override
    protected ContactListItemView newView(Context context, int i, Cursor cursor, int i2, ViewGroup viewGroup) {
        Log.i("DataKindBasePickerAdapter", "[newView]partition = " + i + ",position = " + i2);
        ContactListItemView contactListItemView = new ContactListItemView(context, null);
        contactListItemView.setUnknownNameText(context.getText(android.R.string.unknownName));
        contactListItemView.setQuickContactEnabled(isQuickContactEnabled());
        contactListItemView.setCheckable(true);
        contactListItemView.setActivatedStateSupported(isSelectionVisible());
        return contactListItemView;
    }

    public void displayPhotoOnLeft() {
        this.mPhotoPosition = ContactListItemView.PhotoPosition.LEFT;
    }

    @Override
    protected void bindView(View view, int i, Cursor cursor, int i2) {
        Log.d("DataKindBasePickerAdapter", "[bindView]position = " + i2 + ",partition = " + i);
        ContactListItemView contactListItemView = (ContactListItemView) view;
        cursor.moveToPosition(i2);
        long j = cursor.getLong(getContactIDColumnIndex());
        boolean z = (cursor.moveToPrevious() && !cursor.isBeforeFirst() && j == cursor.getLong(getContactIDColumnIndex())) ? false : true;
        cursor.moveToPosition(i2);
        if (!cursor.moveToNext() || cursor.isAfterLast() || j == cursor.getLong(getContactIDColumnIndex())) {
        }
        cursor.moveToPosition(i2);
        bindSectionHeaderAndDivider(contactListItemView, i2, cursor);
        if (z) {
            bindName(contactListItemView, cursor);
            if (isQuickContactEnabled()) {
                bindQuickContact(contactListItemView, i, cursor);
            } else {
                bindPhoto(contactListItemView, cursor);
            }
        } else {
            unbindName(contactListItemView);
            contactListItemView.removePhotoView(true, false);
        }
        bindData(contactListItemView, cursor);
        if (!isSearchMode()) {
            contactListItemView.setSnippet(null);
        }
        contactListItemView.hidePhoneticName();
        bindCheckBox(contactListItemView, cursor, i2);
    }

    protected void bindSectionHeaderAndDivider(ContactListItemView contactListItemView, int i, Cursor cursor) {
        if (isSectionHeaderDisplayEnabled()) {
            contactListItemView.setSectionHeader(getItemPlacementInSection(i).sectionHeader);
        } else {
            contactListItemView.setSectionHeader(null);
        }
    }

    protected void bindPhoto(ContactListItemView contactListItemView, Cursor cursor) {
        long j = !cursor.isNull(getPhotoIDColumnIndex()) ? cursor.getLong(getPhotoIDColumnIndex()) : 0L;
        getPhotoLoader().loadThumbnail(contactListItemView.getPhotoView(), j, false, true, j == 0 ? getDefaultImageRequestFromCursor(cursor, getDisplayNameColumnIdex(), getLookupKeyColumnIndex()) : null);
    }

    protected void bindData(ContactListItemView contactListItemView, Cursor cursor) {
        CharSequence typeLabel;
        if (!cursor.isNull(getDataTypeColumnIndex())) {
            int i = cursor.getInt(getDataTypeColumnIndex());
            String string = cursor.getString(getDataLabelColumnIndex());
            typeLabel = GlobalEnv.getSimAasEditor().getTypeLabel(i, string, (String) ContactsContract.CommonDataKinds.Phone.getTypeLabel(this.mContext.getResources(), i, string), cursor.getColumnIndex("indicate_phone_or_sim_contact"));
        } else {
            typeLabel = null;
        }
        Log.d("DataKindBasePickerAdapter", "[bindData] label: " + ((Object) typeLabel));
        contactListItemView.setLabel(typeLabel);
        contactListItemView.showData(cursor, getDataColumnIndex());
    }

    protected void unbindName(ContactListItemView contactListItemView) {
        contactListItemView.hideDisplayName();
        contactListItemView.hidePhoneticName();
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

    @Override
    public long getItemId(int i) {
        return getDataId(i);
    }

    private void bindCheckBox(ContactListItemView contactListItemView, Cursor cursor, int i) {
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
