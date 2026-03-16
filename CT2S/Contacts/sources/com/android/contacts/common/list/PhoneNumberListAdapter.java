package com.android.contacts.common.list;

import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.ContactsContract;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.android.contacts.R;
import com.android.contacts.common.ContactPhotoManager;
import com.android.contacts.common.GeoUtil;
import com.android.contacts.common.extensions.ExtendedPhoneDirectoriesManager;
import com.android.contacts.common.extensions.ExtensionsFactory;
import com.android.contacts.common.list.ContactListItemView;
import com.android.contacts.common.list.IndexerListAdapter;
import java.util.ArrayList;
import java.util.List;

public class PhoneNumberListAdapter extends ContactEntryListAdapter {
    private static final String TAG = PhoneNumberListAdapter.class.getSimpleName();
    private final String mCountryIso;
    private final List<DirectoryPartition> mExtendedDirectories;
    private long mFirstExtendedDirectoryId;
    private ContactListItemView.PhotoPosition mPhotoPosition;
    private final CharSequence mUnknownNameText;
    private boolean mUseCallableUri;

    public static class PhoneQuery {
        public static final String[] PROJECTION_PRIMARY = {"_id", "data2", "data3", "data1", "contact_id", "lookup", "photo_id", "display_name", "photo_thumb_uri", "account_type"};
        public static final String[] PROJECTION_ALTERNATIVE = {"_id", "data2", "data3", "data1", "contact_id", "lookup", "photo_id", "display_name_alt", "photo_thumb_uri", "account_type"};
    }

    public PhoneNumberListAdapter(Context context) {
        super(context);
        this.mFirstExtendedDirectoryId = Long.MAX_VALUE;
        setDefaultFilterHeaderText(R.string.list_filter_phones);
        this.mUnknownNameText = context.getText(android.R.string.unknownName);
        this.mCountryIso = GeoUtil.getCurrentCountryIso(context);
        ExtendedPhoneDirectoriesManager manager = ExtensionsFactory.getExtendedPhoneDirectoriesManager();
        if (manager != null) {
            this.mExtendedDirectories = manager.getExtendedDirectories(this.mContext);
        } else {
            this.mExtendedDirectories = new ArrayList();
        }
    }

    @Override
    public void configureLoader(CursorLoader loader, long directoryId) {
        Uri.Builder builder;
        String newSelection;
        Uri baseUri;
        String query = getQueryString();
        if (query == null) {
            query = "";
        }
        if (isExtendedDirectory(directoryId)) {
            DirectoryPartition directory = getExtendedDirectoryFromId(directoryId);
            String contentUri = directory.getContentUri();
            if (contentUri == null) {
                throw new IllegalStateException("Extended directory must have a content URL: " + directory);
            }
            Uri.Builder builder2 = Uri.parse(contentUri).buildUpon();
            builder2.appendPath(query);
            builder2.appendQueryParameter("limit", String.valueOf(getDirectoryResultLimit(directory)));
            loader.setUri(builder2.build());
            loader.setProjection(PhoneQuery.PROJECTION_PRIMARY);
            return;
        }
        boolean isRemoteDirectoryQuery = isRemoteDirectory(directoryId);
        if (isSearchMode()) {
            if (!isRemoteDirectoryQuery && this.mUseCallableUri) {
                baseUri = ContactsContract.CommonDataKinds.Callable.CONTENT_FILTER_URI;
            } else {
                baseUri = ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI;
            }
            builder = baseUri.buildUpon();
            builder.appendPath(query);
            builder.appendQueryParameter("directory", String.valueOf(directoryId));
            if (isRemoteDirectoryQuery) {
                builder.appendQueryParameter("limit", String.valueOf(getDirectoryResultLimit(getDirectoryById(directoryId))));
            }
        } else {
            Uri baseUri2 = this.mUseCallableUri ? ContactsContract.CommonDataKinds.Callable.CONTENT_URI : ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
            builder = baseUri2.buildUpon().appendQueryParameter("directory", String.valueOf(0L));
            if (isSectionHeaderDisplayEnabled()) {
                builder.appendQueryParameter("android.provider.extra.ADDRESS_BOOK_INDEX", "true");
            }
            applyFilter(loader, builder, directoryId, getFilter());
        }
        String prevSelection = loader.getSelection();
        if (!TextUtils.isEmpty(prevSelection)) {
            newSelection = prevSelection + " AND length(data1) < 1000";
        } else {
            newSelection = "length(data1) < 1000";
        }
        loader.setSelection(newSelection);
        builder.appendQueryParameter("remove_duplicate_entries", "true");
        loader.setUri(builder.build());
        if (getContactNameDisplayOrder() == 1) {
            loader.setProjection(PhoneQuery.PROJECTION_PRIMARY);
        } else {
            loader.setProjection(PhoneQuery.PROJECTION_ALTERNATIVE);
        }
        if (getSortOrder() == 1) {
            loader.setSortOrder("sort_key");
        } else {
            loader.setSortOrder("sort_key_alt");
        }
    }

    protected boolean isExtendedDirectory(long directoryId) {
        return directoryId >= this.mFirstExtendedDirectoryId;
    }

    private DirectoryPartition getExtendedDirectoryFromId(long directoryId) {
        int directoryIndex = (int) (directoryId - this.mFirstExtendedDirectoryId);
        return this.mExtendedDirectories.get(directoryIndex);
    }

    private void applyFilter(CursorLoader loader, Uri.Builder uriBuilder, long directoryId, ContactListFilter filter) {
        if (filter != null && directoryId == 0) {
            StringBuilder selection = new StringBuilder();
            List<String> selectionArgs = new ArrayList<>();
            switch (filter.filterType) {
                case -5:
                case -2:
                case -1:
                    break;
                case -4:
                default:
                    Log.w(TAG, "Unsupported filter type came (type: " + filter.filterType + ", toString: " + filter + ") showing all contacts.");
                    break;
                case -3:
                    selection.append("in_visible_group=1");
                    selection.append(" AND has_phone_number=1");
                    break;
                case 0:
                    filter.addAccountQueryParameterToUrl(uriBuilder);
                    break;
            }
            loader.setSelection(selection.toString());
            loader.setSelectionArgs((String[]) selectionArgs.toArray(new String[0]));
        }
    }

    public String getPhoneNumber(int position) {
        Cursor item = (Cursor) getItem(position);
        if (item != null) {
            return item.getString(3);
        }
        return null;
    }

    public Uri getDataUri(int position) {
        int partitionIndex = getPartitionForPosition(position);
        Cursor item = (Cursor) getItem(position);
        if (item != null) {
            return getDataUri(partitionIndex, item);
        }
        return null;
    }

    public Uri getDataUri(int partitionIndex, Cursor cursor) {
        long directoryId = ((DirectoryPartition) getPartition(partitionIndex)).getDirectoryId();
        if (isRemoteDirectory(directoryId)) {
            return null;
        }
        long phoneId = cursor.getLong(0);
        return ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, phoneId);
    }

    @Override
    protected ContactListItemView newView(Context context, int partition, Cursor cursor, int position, ViewGroup parent) {
        ContactListItemView view = super.newView(context, partition, cursor, position, parent);
        view.setUnknownNameText(this.mUnknownNameText);
        view.setQuickContactEnabled(isQuickContactEnabled());
        view.setPhotoPosition(this.mPhotoPosition);
        return view;
    }

    protected void setHighlight(ContactListItemView view, Cursor cursor) {
        view.setHighlightedPrefix(isSearchMode() ? getUpperCaseQueryString() : null);
    }

    @Override
    protected void bindView(View itemView, int partition, Cursor cursor, int position) {
        boolean isSimAccount;
        super.bindView(itemView, partition, cursor, position);
        ContactListItemView view = (ContactListItemView) itemView;
        setHighlight(view, cursor);
        cursor.moveToPosition(position);
        boolean isFirstEntry = true;
        long currentContactId = cursor.getLong(4);
        if (cursor.moveToPrevious() && !cursor.isBeforeFirst()) {
            long previousContactId = cursor.getLong(4);
            if (currentContactId == previousContactId) {
                isFirstEntry = false;
            }
        }
        cursor.moveToPosition(position);
        if (cursor.moveToNext() && !cursor.isAfterLast()) {
            long nextContactId = cursor.getLong(4);
            if (currentContactId == nextContactId) {
            }
        }
        cursor.moveToPosition(position);
        bindViewId(view, cursor, 0);
        bindSectionHeaderAndDivider(view, position);
        if (isFirstEntry) {
            bindName(view, cursor);
            String accountType = cursor.getString(9);
            int subId = Integer.MAX_VALUE;
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
                    bindSimQuickContact(subId, view, partition, cursor, 6, -1, 4, 5);
                } else {
                    bindQuickContact(view, partition, cursor, 6, -1, 4, 5, 7);
                }
            } else if (getDisplayPhotos()) {
                if (isSimAccount) {
                    bindSimPhoto(subId, view, cursor);
                } else {
                    bindPhoto(view, partition, cursor);
                }
            }
        } else {
            unbindName(view);
            view.removePhotoView(true, false);
        }
        DirectoryPartition directory = (DirectoryPartition) getPartition(partition);
        bindPhoneNumber(view, cursor, directory.isDisplayNumber());
    }

    protected void bindPhoneNumber(ContactListItemView view, Cursor cursor, boolean displayNumber) {
        String text;
        CharSequence label = null;
        if (displayNumber && !cursor.isNull(1)) {
            int type = cursor.getInt(1);
            String customLabel = cursor.getString(2);
            label = ContactsContract.CommonDataKinds.Phone.getTypeLabel(getContext().getResources(), type, customLabel);
        }
        view.setLabel(label);
        if (displayNumber) {
            text = cursor.getString(3);
        } else {
            String phoneLabel = cursor.getString(2);
            if (phoneLabel != null) {
                text = phoneLabel;
            } else {
                String phoneNumber = cursor.getString(3);
                text = GeoUtil.getGeocodedLocationFor(this.mContext, phoneNumber);
            }
        }
        view.setPhoneNumber(text, this.mCountryIso);
    }

    protected void bindSectionHeaderAndDivider(ContactListItemView view, int position) {
        if (isSectionHeaderDisplayEnabled()) {
            IndexerListAdapter.Placement placement = getItemPlacementInSection(position);
            view.setSectionHeader(placement.firstInSection ? placement.sectionHeader : null);
        } else {
            view.setSectionHeader(null);
        }
    }

    protected void bindName(ContactListItemView view, Cursor cursor) {
        view.showDisplayName(cursor, 7, getContactNameDisplayOrder());
    }

    protected void unbindName(ContactListItemView view) {
        view.hideDisplayName();
    }

    protected void bindPhoto(ContactListItemView view, int partitionIndex, Cursor cursor) {
        if (!isPhotoSupported(partitionIndex)) {
            view.removePhotoView();
            return;
        }
        long photoId = 0;
        if (!cursor.isNull(6)) {
            photoId = cursor.getLong(6);
        }
        if (photoId != 0) {
            getPhotoLoader().loadThumbnail(view.getPhotoView(), photoId, false, getCircularPhotos(), null);
            return;
        }
        String photoUriString = cursor.getString(8);
        Uri photoUri = photoUriString == null ? null : Uri.parse(photoUriString);
        ContactPhotoManager.DefaultImageRequest request = null;
        if (photoUri == null) {
            String displayName = cursor.getString(7);
            String lookupKey = cursor.getString(5);
            request = new ContactPhotoManager.DefaultImageRequest(displayName, lookupKey, getCircularPhotos());
        }
        getPhotoLoader().loadDirectoryPhoto(view.getPhotoView(), photoUri, false, getCircularPhotos(), request);
    }

    protected void bindSimPhoto(int subId, ContactListItemView view, Cursor cursor) {
        ImageView photo = view.getPhotoView();
        SubscriptionManager mSubscriptionManager = SubscriptionManager.from(this.mContext);
        SubscriptionInfo subinfo = mSubscriptionManager.getActiveSubscriptionInfo(subId);
        if (subinfo != null) {
            Bitmap iconBitmap = subinfo.createIconBitmap(this.mContext);
            photo.setImageBitmap(iconBitmap);
        }
    }

    public void setPhotoPosition(ContactListItemView.PhotoPosition photoPosition) {
        this.mPhotoPosition = photoPosition;
    }

    public void setUseCallableUri(boolean useCallableUri) {
        this.mUseCallableUri = useCallableUri;
    }

    @Override
    public void changeDirectories(Cursor cursor) {
        super.changeDirectories(cursor);
        if (getDirectorySearchMode() != 0) {
            int numExtendedDirectories = this.mExtendedDirectories.size();
            if (getPartitionCount() != cursor.getCount() + numExtendedDirectories) {
                this.mFirstExtendedDirectoryId = Long.MAX_VALUE;
                if (numExtendedDirectories > 0) {
                    long maxId = 1;
                    int insertIndex = 0;
                    int n = getPartitionCount();
                    for (int i = 0; i < n; i++) {
                        DirectoryPartition partition = (DirectoryPartition) getPartition(i);
                        long id = partition.getDirectoryId();
                        if (id > maxId) {
                            maxId = id;
                        }
                        if (!isRemoteDirectory(id)) {
                            insertIndex = i + 1;
                        }
                    }
                    this.mFirstExtendedDirectoryId = 1 + maxId;
                    for (int i2 = 0; i2 < numExtendedDirectories; i2++) {
                        long id2 = this.mFirstExtendedDirectoryId + ((long) i2);
                        DirectoryPartition directory = this.mExtendedDirectories.get(i2);
                        if (getPartitionByDirectoryId(id2) == -1) {
                            addPartition(insertIndex, directory);
                            directory.setDirectoryId(id2);
                        }
                    }
                }
            }
        }
    }

    @Override
    protected Uri getContactUri(int partitionIndex, Cursor cursor, int contactIdColumn, int lookUpKeyColumn) {
        DirectoryPartition directory = (DirectoryPartition) getPartition(partitionIndex);
        long directoryId = directory.getDirectoryId();
        return !isExtendedDirectory(directoryId) ? super.getContactUri(partitionIndex, cursor, contactIdColumn, lookUpKeyColumn) : ContactsContract.Contacts.CONTENT_LOOKUP_URI.buildUpon().appendPath("encoded").appendQueryParameter("displayName", directory.getLabel()).appendQueryParameter("directory", String.valueOf(directoryId)).encodedFragment(cursor.getString(lookUpKeyColumn)).build();
    }
}
