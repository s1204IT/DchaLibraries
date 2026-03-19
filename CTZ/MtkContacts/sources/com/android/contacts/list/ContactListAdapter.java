package com.android.contacts.list;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.view.ViewGroup;
import com.android.contacts.ContactPhotoManager;
import com.android.contacts.ContactSaveService;
import com.android.contacts.R;
import com.android.contacts.compat.ContactsCompat;
import com.android.contacts.list.ContactListItemView;
import com.google.common.collect.Lists;
import com.mediatek.contacts.util.ContactsPortableUtils;
import java.util.ArrayList;
import java.util.HashSet;

public abstract class ContactListAdapter extends MultiSelectEntryContactListAdapter {
    private ContactListItemView.PhotoPosition mPhotoPosition;
    private long mSelectedContactDirectoryId;
    private long mSelectedContactId;
    private String mSelectedContactLookupKey;
    private CharSequence mUnknownNameText;

    public static class ContactQuery {
        private static final String[] CONTACT_PROJECTION_ALTERNATIVE;
        private static final String[] CONTACT_PROJECTION_ALTERNATIVE_INTERNAL;
        private static final String[] CONTACT_PROJECTION_PRIMARY;
        public static final String[] CONTACT_PROJECTION_PRIMARY_INTERNAL = {"_id", "display_name", "contact_presence", "contact_status", "photo_id", "photo_thumb_uri", "lookup", "phonetic_name", ContactSaveService.EXTRA_STARRED_FLAG};
        private static final String[] FILTER_PROJECTION_ALTERNATIVE;
        private static final String[] FILTER_PROJECTION_ALTERNATIVE_INTERNAL;
        private static final String[] FILTER_PROJECTION_PRIMARY;
        private static final String[] FILTER_PROJECTION_PRIMARY_INTERNAL;

        static {
            ArrayList arrayListNewArrayList = Lists.newArrayList(CONTACT_PROJECTION_PRIMARY_INTERNAL);
            if (ContactsPortableUtils.MTK_PHONE_BOOK_SUPPORT) {
                arrayListNewArrayList.add("indicate_phone_or_sim_contact");
                arrayListNewArrayList.add("index_in_sim");
                arrayListNewArrayList.add("is_sdn_contact");
            }
            CONTACT_PROJECTION_PRIMARY = (String[]) arrayListNewArrayList.toArray(new String[arrayListNewArrayList.size()]);
            CONTACT_PROJECTION_ALTERNATIVE_INTERNAL = new String[]{"_id", "display_name_alt", "contact_presence", "contact_status", "photo_id", "photo_thumb_uri", "lookup", "phonetic_name", ContactSaveService.EXTRA_STARRED_FLAG};
            ArrayList arrayListNewArrayList2 = Lists.newArrayList(CONTACT_PROJECTION_ALTERNATIVE_INTERNAL);
            if (ContactsPortableUtils.MTK_PHONE_BOOK_SUPPORT) {
                arrayListNewArrayList2.add("indicate_phone_or_sim_contact");
                arrayListNewArrayList2.add("index_in_sim");
                arrayListNewArrayList2.add("is_sdn_contact");
            }
            CONTACT_PROJECTION_ALTERNATIVE = (String[]) arrayListNewArrayList2.toArray(new String[arrayListNewArrayList2.size()]);
            FILTER_PROJECTION_PRIMARY_INTERNAL = new String[]{"_id", "display_name", "contact_presence", "contact_status", "photo_id", "photo_thumb_uri", "lookup", "phonetic_name", ContactSaveService.EXTRA_STARRED_FLAG, "snippet"};
            ArrayList arrayListNewArrayList3 = Lists.newArrayList(FILTER_PROJECTION_PRIMARY_INTERNAL);
            if (ContactsPortableUtils.MTK_PHONE_BOOK_SUPPORT) {
                arrayListNewArrayList3.add("indicate_phone_or_sim_contact");
                arrayListNewArrayList3.add("index_in_sim");
                arrayListNewArrayList3.add("is_sdn_contact");
            }
            FILTER_PROJECTION_PRIMARY = (String[]) arrayListNewArrayList3.toArray(new String[arrayListNewArrayList3.size()]);
            FILTER_PROJECTION_ALTERNATIVE_INTERNAL = new String[]{"_id", "display_name_alt", "contact_presence", "contact_status", "photo_id", "photo_thumb_uri", "lookup", "phonetic_name", ContactSaveService.EXTRA_STARRED_FLAG, "snippet"};
            ArrayList arrayListNewArrayList4 = Lists.newArrayList(FILTER_PROJECTION_ALTERNATIVE_INTERNAL);
            if (ContactsPortableUtils.MTK_PHONE_BOOK_SUPPORT) {
                arrayListNewArrayList4.add("indicate_phone_or_sim_contact");
                arrayListNewArrayList4.add("index_in_sim");
                arrayListNewArrayList4.add("is_sdn_contact");
            }
            FILTER_PROJECTION_ALTERNATIVE = (String[]) arrayListNewArrayList4.toArray(new String[arrayListNewArrayList4.size()]);
        }
    }

    public ContactListAdapter(Context context) {
        super(context, 0);
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

    public void setSelectedContact(long j, String str, long j2) {
        this.mSelectedContactDirectoryId = j;
        this.mSelectedContactLookupKey = str;
        this.mSelectedContactId = j2;
    }

    protected static Uri buildSectionIndexerUri(Uri uri) {
        return uri.buildUpon().appendQueryParameter("android.provider.extra.ADDRESS_BOOK_INDEX", "true").build();
    }

    public Uri getContactUri(int i) {
        int partitionForPosition = getPartitionForPosition(i);
        Cursor cursor = (Cursor) getItem(i);
        if (cursor != null) {
            return getContactUri(partitionForPosition, cursor);
        }
        return null;
    }

    public Uri getContactUri(int i, Cursor cursor) {
        Uri lookupUri = ContactsContract.Contacts.getLookupUri(cursor.getLong(0), cursor.getString(6));
        long directoryId = ((DirectoryPartition) getPartition(i)).getDirectoryId();
        if (lookupUri != null && directoryId != 0) {
            return lookupUri.buildUpon().appendQueryParameter("directory", String.valueOf(directoryId)).build();
        }
        return lookupUri;
    }

    public long getContactId(int i) {
        Cursor cursor = (Cursor) getItem(i);
        if (cursor == null) {
            return -1L;
        }
        return cursor.getLong(0);
    }

    public boolean isEnterpriseContact(int i) {
        Cursor cursor = (Cursor) getItem(i);
        if (cursor == null) {
            return false;
        }
        return ContactsCompat.isEnterpriseContactId(cursor.getLong(0));
    }

    public boolean isSelectedContact(int i, Cursor cursor) {
        long directoryId = ((DirectoryPartition) getPartition(i)).getDirectoryId();
        if (getSelectedContactDirectoryId() != directoryId) {
            return false;
        }
        String selectedContactLookupKey = getSelectedContactLookupKey();
        if (selectedContactLookupKey == null || !TextUtils.equals(selectedContactLookupKey, cursor.getString(6))) {
            return (directoryId == 0 || directoryId == 1 || getSelectedContactId() != cursor.getLong(0)) ? false : true;
        }
        return true;
    }

    @Override
    protected ContactListItemView newView(Context context, int i, Cursor cursor, int i2, ViewGroup viewGroup) {
        ContactListItemView contactListItemViewNewView = super.newView(context, i, cursor, i2, viewGroup);
        contactListItemViewNewView.setUnknownNameText(this.mUnknownNameText);
        contactListItemViewNewView.setQuickContactEnabled(isQuickContactEnabled());
        contactListItemViewNewView.setAdjustSelectionBoundsEnabled(isAdjustSelectionBoundsEnabled());
        contactListItemViewNewView.setActivatedStateSupported(isSelectionVisible());
        if (this.mPhotoPosition != null) {
            contactListItemViewNewView.setPhotoPosition(this.mPhotoPosition);
        }
        return contactListItemViewNewView;
    }

    protected void bindSectionHeaderAndDivider(ContactListItemView contactListItemView, int i, Cursor cursor) {
        contactListItemView.setIsSectionHeaderEnabled(isSectionHeaderDisplayEnabled());
        if (isSectionHeaderDisplayEnabled()) {
            contactListItemView.setSectionHeader(getItemPlacementInSection(i).sectionHeader);
        } else {
            contactListItemView.setSectionHeader(null);
        }
    }

    protected void bindPhoto(ContactListItemView contactListItemView, int i, Cursor cursor) {
        Uri uri;
        if (!isPhotoSupported(i)) {
            contactListItemView.removePhotoView();
            return;
        }
        long j = !cursor.isNull(4) ? cursor.getLong(4) : 0L;
        if (j != 0) {
            getPhotoLoader().loadThumbnail(contactListItemView.getPhotoView(), j, false, getCircularPhotos(), null);
            return;
        }
        String string = cursor.getString(5);
        ContactPhotoManager.DefaultImageRequest defaultImageRequestFromCursor = null;
        if (string != null) {
            uri = Uri.parse(string);
        } else {
            uri = null;
        }
        if (uri == null) {
            defaultImageRequestFromCursor = getDefaultImageRequestFromCursor(cursor, 1, 6);
        }
        getPhotoLoader().loadDirectoryPhoto(contactListItemView.getPhotoView(), uri, false, getCircularPhotos(), defaultImageRequestFromCursor);
    }

    protected void bindNameAndViewId(ContactListItemView contactListItemView, Cursor cursor) {
        contactListItemView.showDisplayName(cursor, 1, getContactNameDisplayOrder());
        bindViewId(contactListItemView, cursor, 0);
    }

    protected void bindPresenceAndStatusMessage(ContactListItemView contactListItemView, Cursor cursor) {
        contactListItemView.showPresenceAndStatusMessage(cursor, 2, 3);
    }

    protected void bindSearchSnippet(ContactListItemView contactListItemView, Cursor cursor) {
        contactListItemView.showSnippet(cursor, 9);
    }

    public int getSelectedContactPosition() {
        Cursor cursor;
        int position;
        if (this.mSelectedContactLookupKey == null && this.mSelectedContactId == 0) {
            return -1;
        }
        int partitionCount = getPartitionCount();
        int i = 0;
        while (true) {
            if (i >= partitionCount) {
                i = -1;
                break;
            }
            if (((DirectoryPartition) getPartition(i)).getDirectoryId() == this.mSelectedContactDirectoryId) {
                break;
            }
            i++;
        }
        if (i == -1 || (cursor = getCursor(i)) == null) {
            return -1;
        }
        cursor.moveToPosition(-1);
        while (true) {
            if (cursor.moveToNext()) {
                if (this.mSelectedContactLookupKey != null) {
                    if (this.mSelectedContactLookupKey.equals(cursor.getString(6))) {
                        position = cursor.getPosition();
                        break;
                    }
                    if (this.mSelectedContactId != 0 && (this.mSelectedContactDirectoryId == 0 || this.mSelectedContactDirectoryId == 1)) {
                        if (cursor.getLong(0) == this.mSelectedContactId) {
                            position = cursor.getPosition();
                            break;
                        }
                    }
                }
            } else {
                position = -1;
                break;
            }
        }
        if (position == -1) {
            return -1;
        }
        int positionForPartition = getPositionForPartition(i) + position;
        if (hasHeader(i)) {
            return positionForPartition + 1;
        }
        return positionForPartition;
    }

    public Uri getFirstContactUri() {
        Cursor cursor;
        int partitionCount = getPartitionCount();
        for (int i = 0; i < partitionCount; i++) {
            if (!((DirectoryPartition) getPartition(i)).isLoading() && (cursor = getCursor(i)) != null && cursor.moveToFirst()) {
                return getContactUri(i, cursor);
            }
        }
        return null;
    }

    @Override
    public void changeCursor(int i, Cursor cursor) {
        super.changeCursor(i, cursor);
        if (cursor != null && cursor.moveToFirst() && shouldIncludeFavorites()) {
            if (cursor.getInt(8) == 1) {
                HashSet hashSet = new HashSet();
                hashSet.add(Integer.valueOf(cursor.getInt(0)));
                while (cursor != null && cursor.moveToNext() && cursor.getInt(8) == 1 && !hashSet.contains(Integer.valueOf(cursor.getInt(0)))) {
                    hashSet.add(Integer.valueOf(cursor.getInt(0)));
                }
                setFavoritesSectionHeader(hashSet.size());
                return;
            }
            setFavoritesSectionHeader(0);
        }
    }

    protected final String[] getProjection(boolean z) {
        int contactNameDisplayOrder = getContactNameDisplayOrder();
        return z ? contactNameDisplayOrder == 1 ? ContactQuery.FILTER_PROJECTION_PRIMARY : ContactQuery.FILTER_PROJECTION_ALTERNATIVE : contactNameDisplayOrder == 1 ? ContactQuery.CONTACT_PROJECTION_PRIMARY : ContactQuery.CONTACT_PROJECTION_ALTERNATIVE;
    }

    protected final String[] getDataProjectionForContacts(boolean z) {
        int contactNameDisplayOrder = getContactNameDisplayOrder();
        return z ? contactNameDisplayOrder == 1 ? replaceFirstString(ContactQuery.FILTER_PROJECTION_PRIMARY) : replaceFirstString(ContactQuery.FILTER_PROJECTION_ALTERNATIVE) : contactNameDisplayOrder == 1 ? replaceFirstString(ContactQuery.CONTACT_PROJECTION_PRIMARY) : replaceFirstString(ContactQuery.CONTACT_PROJECTION_ALTERNATIVE);
    }

    private String[] replaceFirstString(String[] strArr) {
        String[] strArr2 = (String[]) strArr.clone();
        strArr2[0] = "contact_id";
        return strArr2;
    }
}
