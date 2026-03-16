package com.android.contacts.common.list;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.ContactsContract;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.android.contacts.R;
import com.android.contacts.common.ContactPhotoManager;
import com.android.contacts.common.list.ContactListItemView;
import com.android.contacts.common.list.IndexerListAdapter;

public abstract class ContactListAdapter extends ContactEntryListAdapter {
    private ContactListItemView.PhotoPosition mPhotoPosition;
    private long mSelectedContactDirectoryId;
    private long mSelectedContactId;
    private String mSelectedContactLookupKey;
    private CharSequence mUnknownNameText;

    protected static class ContactQuery {
        private static final String[] CONTACT_PROJECTION_PRIMARY = {"_id", "display_name", "contact_presence", "contact_status", "photo_id", "photo_thumb_uri", "lookup", "is_user_profile", "account_type"};
        private static final String[] CONTACT_PROJECTION_ALTERNATIVE = {"_id", "display_name_alt", "contact_presence", "contact_status", "photo_id", "photo_thumb_uri", "lookup", "is_user_profile", "account_type"};
        private static final String[] FILTER_PROJECTION_PRIMARY = {"_id", "display_name", "contact_presence", "contact_status", "photo_id", "photo_thumb_uri", "lookup", "is_user_profile", "account_type", "snippet"};
        private static final String[] FILTER_PROJECTION_ALTERNATIVE = {"_id", "display_name_alt", "contact_presence", "contact_status", "photo_id", "photo_thumb_uri", "lookup", "is_user_profile", "account_type", "snippet"};
        protected static final String[] PROJECTION_ADN = {"name", "number", "emails"};
    }

    public ContactListAdapter(Context context) {
        super(context);
        this.mUnknownNameText = context.getText(R.string.missing_name);
    }

    public void setPhotoPosition(ContactListItemView.PhotoPosition photoPosition) {
        this.mPhotoPosition = photoPosition;
    }

    public long getSelectedContactDirectoryId() {
        return this.mSelectedContactDirectoryId;
    }

    public String getSelectedContactLookupKey() {
        return this.mSelectedContactLookupKey;
    }

    public long getSelectedContactId() {
        return this.mSelectedContactId;
    }

    public void setSelectedContact(long selectedDirectoryId, String lookupKey, long contactId) {
        this.mSelectedContactDirectoryId = selectedDirectoryId;
        this.mSelectedContactLookupKey = lookupKey;
        this.mSelectedContactId = contactId;
    }

    protected static Uri buildSectionIndexerUri(Uri uri) {
        return uri.buildUpon().appendQueryParameter("android.provider.extra.ADDRESS_BOOK_INDEX", "true").build();
    }

    public Uri getContactUri(int position) {
        int partitionIndex = getPartitionForPosition(position);
        Cursor item = (Cursor) getItem(position);
        if (item != null) {
            return getContactUri(partitionIndex, item);
        }
        return null;
    }

    public Uri getContactUri(int partitionIndex, Cursor cursor) {
        long contactId = cursor.getLong(0);
        String lookupKey = cursor.getString(6);
        Uri uri = ContactsContract.Contacts.getLookupUri(contactId, lookupKey);
        long directoryId = ((DirectoryPartition) getPartition(partitionIndex)).getDirectoryId();
        if (directoryId != 0) {
            return uri.buildUpon().appendQueryParameter("directory", String.valueOf(directoryId)).build();
        }
        return uri;
    }

    public boolean isSelectedContact(int partitionIndex, Cursor cursor) {
        long directoryId = ((DirectoryPartition) getPartition(partitionIndex)).getDirectoryId();
        if (getSelectedContactDirectoryId() != directoryId) {
            return false;
        }
        String lookupKey = getSelectedContactLookupKey();
        if (lookupKey == null || !TextUtils.equals(lookupKey, cursor.getString(6))) {
            return (directoryId == 0 || directoryId == 1 || getSelectedContactId() != cursor.getLong(0)) ? false : true;
        }
        return true;
    }

    @Override
    protected ContactListItemView newView(Context context, int partition, Cursor cursor, int position, ViewGroup parent) {
        ContactListItemView view = super.newView(context, partition, cursor, position, parent);
        view.setUnknownNameText(this.mUnknownNameText);
        view.setQuickContactEnabled(isQuickContactEnabled());
        view.setAdjustSelectionBoundsEnabled(isAdjustSelectionBoundsEnabled());
        view.setActivatedStateSupported(isSelectionVisible());
        if (this.mPhotoPosition != null) {
            view.setPhotoPosition(this.mPhotoPosition);
        }
        return view;
    }

    protected void bindSectionHeaderAndDivider(ContactListItemView view, int position, Cursor cursor) {
        view.setIsSectionHeaderEnabled(isSectionHeaderDisplayEnabled());
        if (isSectionHeaderDisplayEnabled()) {
            IndexerListAdapter.Placement placement = getItemPlacementInSection(position);
            view.setSectionHeader(placement.sectionHeader);
        } else {
            view.setSectionHeader(null);
        }
    }

    protected void bindPhoto(ContactListItemView view, int partitionIndex, Cursor cursor) {
        if (!isPhotoSupported(partitionIndex)) {
            view.removePhotoView();
            return;
        }
        long photoId = 0;
        if (!cursor.isNull(4)) {
            photoId = cursor.getLong(4);
        }
        if (photoId != 0) {
            getPhotoLoader().loadThumbnail(view.getPhotoView(), photoId, false, getCircularPhotos(), null);
            return;
        }
        String photoUriString = cursor.getString(5);
        Uri photoUri = photoUriString == null ? null : Uri.parse(photoUriString);
        ContactPhotoManager.DefaultImageRequest request = null;
        if (photoUri == null) {
            request = getDefaultImageRequestFromCursor(cursor, 1, 6);
        }
        getPhotoLoader().loadDirectoryPhoto(view.getPhotoView(), photoUri, false, getCircularPhotos(), request);
    }

    protected void bindSimPhoto(int subId, ContactListItemView view, int partitionIndex, Cursor cursor) {
        if (!isPhotoSupported(partitionIndex)) {
            view.removePhotoView();
            return;
        }
        SubscriptionManager mSubscriptionManager = SubscriptionManager.from(this.mContext);
        ImageView photo = view.getPhotoView();
        SubscriptionInfo subinfo = mSubscriptionManager.getActiveSubscriptionInfo(subId);
        if (subinfo != null) {
            Bitmap iconBitmap = subinfo.createIconBitmap(this.mContext);
            photo.setImageBitmap(iconBitmap);
        }
    }

    protected void bindNameAndViewId(ContactListItemView view, Cursor cursor) {
        view.showDisplayName(cursor, 1, getContactNameDisplayOrder());
        bindViewId(view, cursor, 0);
    }

    protected void bindSimName(int subId, ContactListItemView view, Cursor cursor) {
        view.showSimDisplayName(subId, cursor, 1, getContactNameDisplayOrder());
    }

    protected void bindPresenceAndStatusMessage(ContactListItemView view, Cursor cursor) {
        view.showPresenceAndStatusMessage(cursor, 2, 3);
    }

    protected void bindSearchSnippet(ContactListItemView view, Cursor cursor) {
        view.showSnippet(cursor, 9);
    }

    public int getSelectedContactPosition() {
        Cursor cursor;
        if (this.mSelectedContactLookupKey == null && this.mSelectedContactId == 0) {
            return -1;
        }
        int partitionIndex = -1;
        int partitionCount = getPartitionCount();
        int i = 0;
        while (true) {
            if (i >= partitionCount) {
                break;
            }
            DirectoryPartition partition = (DirectoryPartition) getPartition(i);
            if (partition.getDirectoryId() != this.mSelectedContactDirectoryId) {
                i++;
            } else {
                partitionIndex = i;
                break;
            }
        }
        if (partitionIndex == -1 || (cursor = getCursor(partitionIndex)) == null) {
            return -1;
        }
        cursor.moveToPosition(-1);
        int offset = -1;
        while (true) {
            if (!cursor.moveToNext()) {
                break;
            }
            if (this.mSelectedContactLookupKey != null) {
                String lookupKey = cursor.getString(6);
                if (this.mSelectedContactLookupKey.equals(lookupKey)) {
                    offset = cursor.getPosition();
                    break;
                }
                if (this.mSelectedContactId != 0 && (this.mSelectedContactDirectoryId == 0 || this.mSelectedContactDirectoryId == 1)) {
                    long contactId = cursor.getLong(0);
                    if (contactId == this.mSelectedContactId) {
                        offset = cursor.getPosition();
                        break;
                    }
                }
            }
        }
        if (offset == -1) {
            return -1;
        }
        int position = getPositionForPartition(partitionIndex) + offset;
        if (hasHeader(partitionIndex)) {
            return position + 1;
        }
        return position;
    }

    public Uri getFirstContactUri() {
        Cursor cursor;
        int partitionCount = getPartitionCount();
        for (int i = 0; i < partitionCount; i++) {
            DirectoryPartition partition = (DirectoryPartition) getPartition(i);
            if (!partition.isLoading() && (cursor = getCursor(i)) != null && cursor.moveToFirst()) {
                return getContactUri(i, cursor);
            }
        }
        return null;
    }

    @Override
    public void changeCursor(int partitionIndex, Cursor cursor) {
        super.changeCursor(partitionIndex, cursor);
        if (cursor != null && cursor.moveToFirst()) {
            setProfileExists(cursor.getInt(7) == 1);
        }
    }

    protected final String[] getProjection(boolean forSearch) {
        int sortOrder = getContactNameDisplayOrder();
        return forSearch ? sortOrder == 1 ? ContactQuery.FILTER_PROJECTION_PRIMARY : ContactQuery.FILTER_PROJECTION_ALTERNATIVE : sortOrder == 1 ? ContactQuery.CONTACT_PROJECTION_PRIMARY : ContactQuery.CONTACT_PROJECTION_ALTERNATIVE;
    }
}
