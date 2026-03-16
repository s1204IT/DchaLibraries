package com.android.contacts.common.list;

import android.content.Context;
import android.content.CursorLoader;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.QuickContactBadge;
import android.widget.SectionIndexer;
import android.widget.TextView;
import com.android.common.widget.CompositeCursorAdapter;
import com.android.contacts.R;
import com.android.contacts.common.ContactPhotoManager;
import com.android.contacts.common.list.IndexerListAdapter;
import com.android.contacts.common.util.SearchUtil;
import java.util.HashSet;

public abstract class ContactEntryListAdapter extends IndexerListAdapter {
    private boolean mAdjustSelectionBoundsEnabled;
    private boolean mCircularPhotos;
    private boolean mDarkTheme;
    private CharSequence mDefaultFilterHeaderText;
    private int mDirectoryResultLimit;
    private int mDirectorySearchMode;
    private int mDisplayOrder;
    private boolean mDisplayPhotos;
    private boolean mEmptyListEnabled;
    private ContactListFilter mFilter;
    private View mFragmentRootView;
    private boolean mIncludeProfile;
    private ContactPhotoManager mPhotoLoader;
    private boolean mProfileExists;
    private String mQueryString;
    private boolean mQuickContactEnabled;
    private boolean mSearchMode;
    private boolean mSelectionVisible;
    private int mSortOrder;
    private String mUpperCaseQueryString;

    public abstract void configureLoader(CursorLoader cursorLoader, long j);

    public ContactEntryListAdapter(Context context) {
        super(context);
        this.mCircularPhotos = true;
        this.mDirectoryResultLimit = Integer.MAX_VALUE;
        this.mEmptyListEnabled = true;
        this.mDarkTheme = false;
        setDefaultFilterHeaderText(R.string.local_search_label);
        addPartitions();
    }

    protected void setFragmentRootView(View fragmentRootView) {
        this.mFragmentRootView = fragmentRootView;
    }

    protected void setDefaultFilterHeaderText(int resourceId) {
        this.mDefaultFilterHeaderText = getContext().getResources().getText(resourceId);
    }

    @Override
    protected ContactListItemView newView(Context context, int partition, Cursor cursor, int position, ViewGroup parent) {
        ContactListItemView view = new ContactListItemView(context, null);
        view.setIsSectionHeaderEnabled(isSectionHeaderDisplayEnabled());
        view.setAdjustSelectionBoundsEnabled(isAdjustSelectionBoundsEnabled());
        return view;
    }

    @Override
    protected void bindView(View itemView, int partition, Cursor cursor, int position) {
        ContactListItemView view = (ContactListItemView) itemView;
        view.setIsSectionHeaderEnabled(isSectionHeaderDisplayEnabled());
    }

    @Override
    protected View createPinnedSectionHeaderView(Context context, ViewGroup parent) {
        return new ContactListPinnedHeaderView(context, null, parent);
    }

    @Override
    protected void setPinnedSectionTitle(View pinnedHeaderView, String title) {
        ((ContactListPinnedHeaderView) pinnedHeaderView).setSectionHeaderTitle(title);
    }

    protected void addPartitions() {
        addPartition(createDefaultDirectoryPartition());
    }

    protected DirectoryPartition createDefaultDirectoryPartition() {
        DirectoryPartition partition = new DirectoryPartition(true, true);
        partition.setDirectoryId(0L);
        partition.setDirectoryType(getContext().getString(R.string.contactsList));
        partition.setPriorityDirectory(true);
        partition.setPhotoSupported(true);
        partition.setLabel(this.mDefaultFilterHeaderText.toString());
        return partition;
    }

    public void removeDirectoriesAfterDefault() {
        int partitionCount = getPartitionCount();
        for (int i = partitionCount - 1; i >= 0; i--) {
            CompositeCursorAdapter.Partition partition = getPartition(i);
            if (!(partition instanceof DirectoryPartition) || ((DirectoryPartition) partition).getDirectoryId() != 0) {
                removePartition(i);
            } else {
                return;
            }
        }
    }

    protected int getPartitionByDirectoryId(long id) {
        int count = getPartitionCount();
        for (int i = 0; i < count; i++) {
            CompositeCursorAdapter.Partition partition = getPartition(i);
            if ((partition instanceof DirectoryPartition) && ((DirectoryPartition) partition).getDirectoryId() == id) {
                return i;
            }
        }
        return -1;
    }

    protected DirectoryPartition getDirectoryById(long id) {
        int count = getPartitionCount();
        for (int i = 0; i < count; i++) {
            CompositeCursorAdapter.Partition partition = getPartition(i);
            if (partition instanceof DirectoryPartition) {
                DirectoryPartition directoryPartition = (DirectoryPartition) partition;
                if (directoryPartition.getDirectoryId() == id) {
                    return directoryPartition;
                }
            }
        }
        return null;
    }

    public void onDataReload() {
        boolean notify = false;
        int count = getPartitionCount();
        for (int i = 0; i < count; i++) {
            CompositeCursorAdapter.Partition partition = getPartition(i);
            if (partition instanceof DirectoryPartition) {
                DirectoryPartition directoryPartition = (DirectoryPartition) partition;
                if (!directoryPartition.isLoading()) {
                    notify = true;
                }
                directoryPartition.setStatus(0);
            }
        }
        if (notify) {
            notifyDataSetChanged();
        }
    }

    @Override
    public void clearPartitions() {
        int count = getPartitionCount();
        for (int i = 0; i < count; i++) {
            CompositeCursorAdapter.Partition partition = getPartition(i);
            if (partition instanceof DirectoryPartition) {
                DirectoryPartition directoryPartition = (DirectoryPartition) partition;
                directoryPartition.setStatus(0);
            }
        }
        super.clearPartitions();
    }

    public boolean isSearchMode() {
        return this.mSearchMode;
    }

    public void setSearchMode(boolean flag) {
        this.mSearchMode = flag;
    }

    public String getQueryString() {
        return this.mQueryString;
    }

    public void setQueryString(String queryString) {
        this.mQueryString = queryString;
        if (TextUtils.isEmpty(queryString)) {
            this.mUpperCaseQueryString = null;
        } else {
            this.mUpperCaseQueryString = SearchUtil.cleanStartAndEndOfSearchQuery(queryString.toUpperCase());
        }
    }

    public String getUpperCaseQueryString() {
        return this.mUpperCaseQueryString;
    }

    public int getDirectorySearchMode() {
        return this.mDirectorySearchMode;
    }

    public void setDirectorySearchMode(int mode) {
        this.mDirectorySearchMode = mode;
    }

    public int getDirectoryResultLimit(DirectoryPartition directoryPartition) {
        int limit = directoryPartition.getResultLimit();
        return limit == -1 ? this.mDirectoryResultLimit : limit;
    }

    public void setDirectoryResultLimit(int limit) {
        this.mDirectoryResultLimit = limit;
    }

    public int getContactNameDisplayOrder() {
        return this.mDisplayOrder;
    }

    public void setContactNameDisplayOrder(int displayOrder) {
        this.mDisplayOrder = displayOrder;
    }

    public int getSortOrder() {
        return this.mSortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.mSortOrder = sortOrder;
    }

    public void setPhotoLoader(ContactPhotoManager photoLoader) {
        this.mPhotoLoader = photoLoader;
    }

    protected ContactPhotoManager getPhotoLoader() {
        return this.mPhotoLoader;
    }

    public boolean getDisplayPhotos() {
        return this.mDisplayPhotos;
    }

    public void setDisplayPhotos(boolean displayPhotos) {
        this.mDisplayPhotos = displayPhotos;
    }

    public boolean getCircularPhotos() {
        return this.mCircularPhotos;
    }

    public void setEmptyListEnabled(boolean flag) {
        this.mEmptyListEnabled = flag;
    }

    public boolean isSelectionVisible() {
        return this.mSelectionVisible;
    }

    public void setSelectionVisible(boolean flag) {
        this.mSelectionVisible = flag;
    }

    public boolean isQuickContactEnabled() {
        return this.mQuickContactEnabled;
    }

    public void setQuickContactEnabled(boolean quickContactEnabled) {
        this.mQuickContactEnabled = quickContactEnabled;
    }

    public boolean isAdjustSelectionBoundsEnabled() {
        return this.mAdjustSelectionBoundsEnabled;
    }

    public void setAdjustSelectionBoundsEnabled(boolean enabled) {
        this.mAdjustSelectionBoundsEnabled = enabled;
    }

    public boolean shouldIncludeProfile() {
        return this.mIncludeProfile;
    }

    public void setIncludeProfile(boolean includeProfile) {
        this.mIncludeProfile = includeProfile;
    }

    public void setProfileExists(boolean exists) {
        SectionIndexer indexer;
        this.mProfileExists = exists;
        if (exists && (indexer = getIndexer()) != null) {
            ((ContactsSectionIndexer) indexer).setProfileHeader(getContext().getString(R.string.user_profile_contacts_list_header));
        }
    }

    public boolean hasProfile() {
        return this.mProfileExists;
    }

    public void setDarkTheme(boolean value) {
        this.mDarkTheme = value;
    }

    public void changeDirectories(Cursor cursor) {
        if (cursor.getCount() == 0) {
            Log.e("ContactEntryListAdapter", "Directory search loader returned an empty cursor, which implies we have no directory entries.", new RuntimeException());
            return;
        }
        HashSet<Long> directoryIds = new HashSet<>();
        int idColumnIndex = cursor.getColumnIndex("_id");
        int directoryTypeColumnIndex = cursor.getColumnIndex("directoryType");
        int displayNameColumnIndex = cursor.getColumnIndex("displayName");
        int photoSupportColumnIndex = cursor.getColumnIndex("photoSupport");
        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            long id = cursor.getLong(idColumnIndex);
            directoryIds.add(Long.valueOf(id));
            if (getPartitionByDirectoryId(id) == -1) {
                DirectoryPartition partition = new DirectoryPartition(false, true);
                partition.setDirectoryId(id);
                if (isRemoteDirectory(id)) {
                    partition.setLabel(this.mContext.getString(R.string.directory_search_label));
                } else {
                    partition.setLabel(this.mDefaultFilterHeaderText.toString());
                }
                partition.setDirectoryType(cursor.getString(directoryTypeColumnIndex));
                partition.setDisplayName(cursor.getString(displayNameColumnIndex));
                int photoSupport = cursor.getInt(photoSupportColumnIndex);
                partition.setPhotoSupported(photoSupport == 1 || photoSupport == 3);
                addPartition(partition);
            }
        }
        int count = getPartitionCount();
        int i = count;
        while (true) {
            i--;
            if (i >= 0) {
                CompositeCursorAdapter.Partition partition2 = getPartition(i);
                if ((partition2 instanceof DirectoryPartition) && !directoryIds.contains(Long.valueOf(((DirectoryPartition) partition2).getDirectoryId()))) {
                    removePartition(i);
                }
            } else {
                invalidate();
                notifyDataSetChanged();
                return;
            }
        }
    }

    @Override
    public void changeCursor(int partitionIndex, Cursor cursor) {
        if (partitionIndex < getPartitionCount()) {
            CompositeCursorAdapter.Partition partition = getPartition(partitionIndex);
            if (partition instanceof DirectoryPartition) {
                ((DirectoryPartition) partition).setStatus(2);
            }
            if (this.mDisplayPhotos && this.mPhotoLoader != null && isPhotoSupported(partitionIndex)) {
                this.mPhotoLoader.refreshCache();
            }
            super.changeCursor(partitionIndex, cursor);
            if (isSectionHeaderDisplayEnabled() && partitionIndex == getIndexedPartition()) {
                updateIndexer(cursor);
            }
            this.mPhotoLoader.cancelPendingRequests(this.mFragmentRootView);
        }
    }

    private void updateIndexer(Cursor cursor) {
        if (cursor == null) {
            setIndexer(null);
            return;
        }
        Bundle bundle = cursor.getExtras();
        if (bundle.containsKey("android.provider.extra.ADDRESS_BOOK_INDEX_TITLES") && bundle.containsKey("android.provider.extra.ADDRESS_BOOK_INDEX_COUNTS")) {
            String[] sections = bundle.getStringArray("android.provider.extra.ADDRESS_BOOK_INDEX_TITLES");
            int[] counts = bundle.getIntArray("android.provider.extra.ADDRESS_BOOK_INDEX_COUNTS");
            if (getExtraStartingSection()) {
                String[] allSections = new String[sections.length + 1];
                int[] allCounts = new int[counts.length + 1];
                for (int i = 0; i < sections.length; i++) {
                    allSections[i + 1] = sections[i];
                    allCounts[i + 1] = counts[i];
                }
                allCounts[0] = 1;
                allSections[0] = "";
                setIndexer(new ContactsSectionIndexer(allSections, allCounts));
                return;
            }
            setIndexer(new ContactsSectionIndexer(sections, counts));
            return;
        }
        setIndexer(null);
    }

    protected boolean getExtraStartingSection() {
        return false;
    }

    @Override
    public int getViewTypeCount() {
        return (getItemViewTypeCount() * 2) + 1;
    }

    @Override
    public int getItemViewType(int partitionIndex, int position) {
        int type = super.getItemViewType(partitionIndex, position);
        if (!isUserProfile(position) && isSectionHeaderDisplayEnabled() && partitionIndex == getIndexedPartition()) {
            IndexerListAdapter.Placement placement = getItemPlacementInSection(position);
            if (!placement.firstInSection) {
                return type + getItemViewTypeCount();
            }
            return type;
        }
        return type;
    }

    @Override
    public boolean isEmpty() {
        if (!this.mEmptyListEnabled) {
            return false;
        }
        if (isSearchMode()) {
            return TextUtils.isEmpty(getQueryString());
        }
        return super.isEmpty();
    }

    public boolean isLoading() {
        int count = getPartitionCount();
        for (int i = 0; i < count; i++) {
            CompositeCursorAdapter.Partition partition = getPartition(i);
            if ((partition instanceof DirectoryPartition) && ((DirectoryPartition) partition).isLoading()) {
                return true;
            }
        }
        return false;
    }

    public boolean areAllPartitionsEmpty() {
        int count = getPartitionCount();
        for (int i = 0; i < count; i++) {
            if (!isPartitionEmpty(i)) {
                return false;
            }
        }
        return true;
    }

    public void configureDefaultPartition(boolean showIfEmpty, boolean hasHeader) {
        int defaultPartitionIndex = -1;
        int count = getPartitionCount();
        int i = 0;
        while (true) {
            if (i >= count) {
                break;
            }
            CompositeCursorAdapter.Partition partition = getPartition(i);
            if (!(partition instanceof DirectoryPartition) || ((DirectoryPartition) partition).getDirectoryId() != 0) {
                i++;
            } else {
                defaultPartitionIndex = i;
                break;
            }
        }
        if (defaultPartitionIndex != -1) {
            setShowIfEmpty(defaultPartitionIndex, showIfEmpty);
            setHasHeader(defaultPartitionIndex, hasHeader);
        }
    }

    @Override
    protected View newHeaderView(Context context, int partition, Cursor cursor, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.directory_header, parent, false);
        if (!getPinnedPartitionHeadersEnabled()) {
            view.setBackground(null);
        }
        return view;
    }

    @Override
    protected void bindHeaderView(View view, int partitionIndex, Cursor cursor) {
        CompositeCursorAdapter.Partition partition = getPartition(partitionIndex);
        if (partition instanceof DirectoryPartition) {
            DirectoryPartition directoryPartition = (DirectoryPartition) partition;
            long directoryId = directoryPartition.getDirectoryId();
            TextView labelTextView = (TextView) view.findViewById(R.id.label);
            TextView displayNameTextView = (TextView) view.findViewById(R.id.display_name);
            labelTextView.setText(directoryPartition.getLabel());
            if (!isRemoteDirectory(directoryId)) {
                displayNameTextView.setText((CharSequence) null);
            } else {
                String directoryName = directoryPartition.getDisplayName();
                String displayName = !TextUtils.isEmpty(directoryName) ? directoryName : directoryPartition.getDirectoryType();
                displayNameTextView.setText(displayName);
            }
            Resources res = getContext().getResources();
            int headerPaddingTop = (partitionIndex == 1 && getPartition(0).isEmpty()) ? 0 : res.getDimensionPixelOffset(R.dimen.directory_header_extra_top_padding);
            view.setPaddingRelative(view.getPaddingStart(), headerPaddingTop, view.getPaddingEnd(), view.getPaddingBottom());
        }
    }

    protected boolean isUserProfile(int position) {
        int partition;
        boolean isUserProfile = false;
        if (position == 0 && (partition = getPartitionForPosition(position)) >= 0) {
            int offset = getCursor(partition).getPosition();
            Cursor cursor = (Cursor) getItem(position);
            if (cursor != null) {
                int profileColumnIndex = cursor.getColumnIndex("is_user_profile");
                if (profileColumnIndex != -1) {
                    isUserProfile = cursor.getInt(profileColumnIndex) == 1;
                }
                cursor.moveToPosition(offset);
            }
        }
        return isUserProfile;
    }

    public boolean isPhotoSupported(int partitionIndex) {
        CompositeCursorAdapter.Partition partition = getPartition(partitionIndex);
        if (partition instanceof DirectoryPartition) {
            return ((DirectoryPartition) partition).isPhotoSupported();
        }
        return true;
    }

    public ContactListFilter getFilter() {
        return this.mFilter;
    }

    public void setFilter(ContactListFilter filter) {
        this.mFilter = filter;
    }

    protected void bindQuickContact(ContactListItemView view, int partitionIndex, Cursor cursor, int photoIdColumn, int photoUriColumn, int contactIdColumn, int lookUpKeyColumn, int displayNameColumn) {
        long photoId = 0;
        if (!cursor.isNull(photoIdColumn)) {
            photoId = cursor.getLong(photoIdColumn);
        }
        QuickContactBadge quickContact = view.getQuickContact();
        quickContact.assignContactUri(getContactUri(partitionIndex, cursor, contactIdColumn, lookUpKeyColumn));
        if (photoId != 0 || photoUriColumn == -1) {
            getPhotoLoader().loadThumbnail(quickContact, photoId, this.mDarkTheme, this.mCircularPhotos, null);
            return;
        }
        String photoUriString = cursor.getString(photoUriColumn);
        Uri photoUri = photoUriString == null ? null : Uri.parse(photoUriString);
        ContactPhotoManager.DefaultImageRequest request = null;
        if (photoUri == null) {
            request = getDefaultImageRequestFromCursor(cursor, displayNameColumn, lookUpKeyColumn);
        }
        getPhotoLoader().loadPhoto(quickContact, photoUri, -1, this.mDarkTheme, this.mCircularPhotos, request);
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    protected void bindViewId(ContactListItemView view, Cursor cursor, int idColumn) {
        long contactId = cursor.getLong(idColumn);
        view.setId((int) (contactId % 2147483647L));
    }

    protected void bindSimQuickContact(int subId, ContactListItemView view, int partitionIndex, Cursor cursor, int photoIdColumn, int photoUriColumn, int contactIdColumn, int lookUpKeyColumn) {
        QuickContactBadge quickContact = view.getQuickContact();
        quickContact.assignContactUri(getContactUri(partitionIndex, cursor, contactIdColumn, lookUpKeyColumn));
        SubscriptionManager mSubscriptionManager = SubscriptionManager.from(this.mContext);
        SubscriptionInfo subinfo = mSubscriptionManager.getActiveSubscriptionInfo(subId);
        if (subinfo != null) {
            Bitmap iconBitmap = subinfo.createIconBitmap(this.mContext);
            quickContact.setImageBitmap(iconBitmap);
        }
    }

    protected Uri getContactUri(int partitionIndex, Cursor cursor, int contactIdColumn, int lookUpKeyColumn) {
        long contactId = cursor.getLong(contactIdColumn);
        String lookupKey = cursor.getString(lookUpKeyColumn);
        long directoryId = ((DirectoryPartition) getPartition(partitionIndex)).getDirectoryId();
        if (TextUtils.isEmpty(lookupKey) && isRemoteDirectory(directoryId)) {
            return null;
        }
        Uri uri = ContactsContract.Contacts.getLookupUri(contactId, lookupKey);
        if (directoryId != 0) {
            return uri.buildUpon().appendQueryParameter("directory", String.valueOf(directoryId)).build();
        }
        return uri;
    }

    public static boolean isRemoteDirectory(long directoryId) {
        return (directoryId == 0 || directoryId == 1) ? false : true;
    }

    public ContactPhotoManager.DefaultImageRequest getDefaultImageRequestFromCursor(Cursor cursor, int displayNameColumn, int lookupKeyColumn) {
        String displayName = cursor.getString(displayNameColumn);
        String lookupKey = cursor.getString(lookupKeyColumn);
        return new ContactPhotoManager.DefaultImageRequest(displayName, lookupKey, this.mCircularPhotos);
    }
}
