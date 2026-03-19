package com.android.contacts.list;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.AppCompatCheckBox;
import android.view.View;
import com.android.contacts.ContactPhotoManager;
import com.android.contacts.group.GroupUtil;
import com.mediatek.contacts.util.ContactsPortableUtils;
import java.util.TreeSet;

public abstract class MultiSelectEntryContactListAdapter extends ContactEntryListAdapter {
    private final int mContactIdColumnIndex;
    private DeleteContactListener mDeleteContactListener;
    private boolean mDisplayCheckBoxes;
    public FavoritesAndContactsLoader mSDNLoader;
    private TreeSet<Long> mSelectedContactIds;
    private SelectedContactsListener mSelectedContactsListener;

    public interface DeleteContactListener {
        void onContactDeleteClicked(int i);
    }

    public interface SelectedContactsListener {
        void onSelectedContactsChanged();
    }

    public MultiSelectEntryContactListAdapter(Context context, int i) {
        super(context);
        this.mSelectedContactIds = new TreeSet<>();
        this.mSDNLoader = null;
        this.mContactIdColumnIndex = i;
    }

    public int getContactColumnIdIndex() {
        return this.mContactIdColumnIndex;
    }

    public DeleteContactListener getDeleteContactListener() {
        return this.mDeleteContactListener;
    }

    public void setDeleteContactListener(DeleteContactListener deleteContactListener) {
        this.mDeleteContactListener = deleteContactListener;
    }

    public void setSelectedContactsListener(SelectedContactsListener selectedContactsListener) {
        this.mSelectedContactsListener = selectedContactsListener;
    }

    public TreeSet<Long> getSelectedContactIds() {
        return this.mSelectedContactIds;
    }

    public boolean hasSelectedItems() {
        return this.mSelectedContactIds.size() > 0;
    }

    public long[] getSelectedContactIdsArray() {
        return GroupUtil.convertLongSetToLongArray(this.mSelectedContactIds);
    }

    public void setSelectedContactIds(TreeSet<Long> treeSet) {
        this.mSelectedContactIds = treeSet;
        notifyDataSetChanged();
        if (this.mSelectedContactsListener != null) {
            this.mSelectedContactsListener.onSelectedContactsChanged();
        }
    }

    public void setDisplayCheckBoxes(boolean z) {
        this.mDisplayCheckBoxes = z;
        notifyDataSetChanged();
        if (this.mSelectedContactsListener != null) {
            this.mSelectedContactsListener.onSelectedContactsChanged();
        }
    }

    public boolean isDisplayingCheckBoxes() {
        return this.mDisplayCheckBoxes;
    }

    public void toggleSelectionOfContactId(long j) {
        if (this.mSelectedContactIds.contains(Long.valueOf(j))) {
            this.mSelectedContactIds.remove(Long.valueOf(j));
        } else {
            this.mSelectedContactIds.add(Long.valueOf(j));
        }
        notifyDataSetChanged();
        if (this.mSelectedContactsListener != null) {
            this.mSelectedContactsListener.onSelectedContactsChanged();
        }
    }

    @Override
    public long getItemId(int i) {
        Cursor cursor = (Cursor) getItem(i);
        if (cursor != null) {
            return cursor.getLong(getContactColumnIdIndex());
        }
        return 0L;
    }

    @Override
    protected void bindView(View view, int i, Cursor cursor, int i2) {
        super.bindView(view, i, cursor, i2);
        ContactListItemView contactListItemView = (ContactListItemView) view;
        bindViewId(contactListItemView, cursor, getContactColumnIdIndex());
        bindCheckBox(contactListItemView, cursor, ((long) i) == 0);
    }

    protected void bindPhoto(ContactListItemView contactListItemView, Cursor cursor, int i, int i2, int i3) {
        long j;
        MultiSelectEntryContactListAdapter multiSelectEntryContactListAdapter;
        ContactPhotoManager.DefaultImageRequest defaultImageRequestFromCursor;
        if (!cursor.isNull(i)) {
            j = cursor.getLong(i);
        } else {
            j = 0;
        }
        if (j == 0) {
            multiSelectEntryContactListAdapter = this;
            defaultImageRequestFromCursor = multiSelectEntryContactListAdapter.getDefaultImageRequestFromCursor(cursor, i3, i2);
        } else {
            multiSelectEntryContactListAdapter = this;
            defaultImageRequestFromCursor = null;
        }
        multiSelectEntryContactListAdapter.getPhotoLoader().loadThumbnail(contactListItemView.getPhotoView(), j, false, multiSelectEntryContactListAdapter.getCircularPhotos(), defaultImageRequestFromCursor);
    }

    private void bindCheckBox(ContactListItemView contactListItemView, Cursor cursor, boolean z) {
        contactListItemView.setClickable(!z && this.mDisplayCheckBoxes);
        if (!this.mDisplayCheckBoxes || !z) {
            contactListItemView.hideCheckBox();
            return;
        }
        if (ContactsPortableUtils.MTK_PHONE_BOOK_SUPPORT && cursor.getInt(cursor.getColumnIndex("is_sdn_contact")) == 1) {
            contactListItemView.setClickable(false);
            contactListItemView.hideCheckBox();
            return;
        }
        AppCompatCheckBox checkBox = contactListItemView.getCheckBox();
        long j = cursor.getLong(this.mContactIdColumnIndex);
        checkBox.setChecked(this.mSelectedContactIds.contains(Long.valueOf(j)));
        checkBox.setClickable(false);
        checkBox.setTag(Long.valueOf(j));
    }

    @Override
    public void updateIndexer(Cursor cursor) {
        super.updateIndexer(cursor);
        ContactsSectionIndexer contactsSectionIndexer = (ContactsSectionIndexer) getIndexer();
        if (this.mSDNLoader != null && this.mSDNLoader.getSdnContactCount() > 0) {
            contactsSectionIndexer.setSdnHeader("SDN", this.mSDNLoader.getSdnContactCount());
        }
    }

    public int getSdnNumber() {
        if (this.mSDNLoader != null) {
            return this.mSDNLoader.getSdnContactCount();
        }
        return 0;
    }

    public void notifySelectedContactsChanged() {
        notifyDataSetChanged();
        if (this.mSelectedContactsListener != null) {
            this.mSelectedContactsListener.onSelectedContactsChanged();
        }
    }
}
