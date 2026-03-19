package com.android.contacts.list;

import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.QuickContactBadge;
import android.widget.SectionIndexer;
import android.widget.TextView;
import com.android.common.widget.CompositeCursorAdapter;
import com.android.contacts.ContactPhotoManager;
import com.android.contacts.ContactsUtils;
import com.android.contacts.R;
import com.android.contacts.compat.CompatUtils;
import com.android.contacts.compat.DirectoryCompat;
import com.android.contacts.util.SearchUtil;
import com.mediatek.contacts.util.ContactsCommonListUtils;
import com.mediatek.contacts.util.Log;
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
    private boolean mIncludeFavorites;
    private int mNumberOfFavorites;
    private ContactPhotoManager mPhotoLoader;
    private String mQueryString;
    private boolean mQuickContactEnabled;
    private boolean mSearchMode;
    private boolean mSelectionVisible;
    public boolean mShowSdnNumber;
    private int mSortOrder;
    private String mUpperCaseQueryString;

    public abstract void configureLoader(CursorLoader cursorLoader, long j);

    public ContactEntryListAdapter(Context context) {
        super(context);
        this.mCircularPhotos = true;
        this.mDirectoryResultLimit = Integer.MAX_VALUE;
        this.mEmptyListEnabled = true;
        this.mDarkTheme = false;
        this.mShowSdnNumber = true;
        setDefaultFilterHeaderText(R.string.local_search_label);
        addPartitions();
    }

    protected void setFragmentRootView(View view) {
        this.mFragmentRootView = view;
    }

    protected void setDefaultFilterHeaderText(int i) {
        this.mDefaultFilterHeaderText = getContext().getResources().getText(i);
    }

    @Override
    protected ContactListItemView newView(Context context, int i, Cursor cursor, int i2, ViewGroup viewGroup) {
        ContactListItemView contactListItemView = new ContactListItemView(context, null);
        contactListItemView.setIsSectionHeaderEnabled(isSectionHeaderDisplayEnabled());
        contactListItemView.setAdjustSelectionBoundsEnabled(isAdjustSelectionBoundsEnabled());
        return contactListItemView;
    }

    @Override
    protected void bindView(View view, int i, Cursor cursor, int i2) {
        ContactListItemView contactListItemView = (ContactListItemView) view;
        contactListItemView.setIsSectionHeaderEnabled(isSectionHeaderDisplayEnabled());
        bindWorkProfileIcon(contactListItemView, i);
    }

    @Override
    protected View createPinnedSectionHeaderView(Context context, ViewGroup viewGroup) {
        return new ContactListPinnedHeaderView(context, null, viewGroup);
    }

    @Override
    protected void setPinnedSectionTitle(View view, String str) {
        ((ContactListPinnedHeaderView) view).setSectionHeaderTitle(str);
    }

    protected void addPartitions() {
        addPartition(createDefaultDirectoryPartition());
    }

    protected DirectoryPartition createDefaultDirectoryPartition() {
        DirectoryPartition directoryPartition = new DirectoryPartition(true, true);
        directoryPartition.setDirectoryId(0L);
        directoryPartition.setDirectoryType(getContext().getString(R.string.contactsList));
        directoryPartition.setPriorityDirectory(true);
        directoryPartition.setPhotoSupported(true);
        directoryPartition.setLabel(this.mDefaultFilterHeaderText.toString());
        return directoryPartition;
    }

    public void removeDirectoriesAfterDefault() {
        for (int partitionCount = getPartitionCount() - 1; partitionCount >= 0; partitionCount--) {
            CompositeCursorAdapter.Partition partition = getPartition(partitionCount);
            if (!(partition instanceof DirectoryPartition) || ((DirectoryPartition) partition).getDirectoryId() != 0) {
                removePartition(partitionCount);
            } else {
                return;
            }
        }
    }

    protected int getPartitionByDirectoryId(long j) {
        int partitionCount = getPartitionCount();
        for (int i = 0; i < partitionCount; i++) {
            CompositeCursorAdapter.Partition partition = getPartition(i);
            if ((partition instanceof DirectoryPartition) && ((DirectoryPartition) partition).getDirectoryId() == j) {
                return i;
            }
        }
        return -1;
    }

    protected DirectoryPartition getDirectoryById(long j) {
        int partitionCount = getPartitionCount();
        for (int i = 0; i < partitionCount; i++) {
            CompositeCursorAdapter.Partition partition = getPartition(i);
            if (partition instanceof DirectoryPartition) {
                DirectoryPartition directoryPartition = (DirectoryPartition) partition;
                if (directoryPartition.getDirectoryId() == j) {
                    return directoryPartition;
                }
            }
        }
        return null;
    }

    public void onDataReload() {
        int partitionCount = getPartitionCount();
        boolean z = false;
        for (int i = 0; i < partitionCount; i++) {
            CompositeCursorAdapter.Partition partition = getPartition(i);
            if (partition instanceof DirectoryPartition) {
                DirectoryPartition directoryPartition = (DirectoryPartition) partition;
                if (!directoryPartition.isLoading()) {
                    z = true;
                }
                directoryPartition.setStatus(0);
            }
        }
        if (z) {
            notifyDataSetChanged();
        }
    }

    @Override
    public void clearPartitions() {
        int partitionCount = getPartitionCount();
        for (int i = 0; i < partitionCount; i++) {
            CompositeCursorAdapter.Partition partition = getPartition(i);
            if (partition instanceof DirectoryPartition) {
                ((DirectoryPartition) partition).setStatus(0);
            }
        }
        super.clearPartitions();
    }

    public boolean isSearchMode() {
        return this.mSearchMode;
    }

    public void setSearchMode(boolean z) {
        this.mSearchMode = z;
    }

    public String getQueryString() {
        return this.mQueryString;
    }

    public void setQueryString(String str) {
        this.mQueryString = str;
        if (TextUtils.isEmpty(str)) {
            this.mUpperCaseQueryString = null;
        } else {
            this.mUpperCaseQueryString = SearchUtil.cleanStartAndEndOfSearchQuery(str.toUpperCase());
        }
    }

    public String getUpperCaseQueryString() {
        return this.mUpperCaseQueryString;
    }

    public int getDirectorySearchMode() {
        return this.mDirectorySearchMode;
    }

    public void setDirectorySearchMode(int i) {
        this.mDirectorySearchMode = i;
    }

    public int getDirectoryResultLimit() {
        return this.mDirectoryResultLimit;
    }

    public int getDirectoryResultLimit(DirectoryPartition directoryPartition) {
        int resultLimit = directoryPartition.getResultLimit();
        return resultLimit == -1 ? this.mDirectoryResultLimit : resultLimit;
    }

    public void setDirectoryResultLimit(int i) {
        this.mDirectoryResultLimit = i;
    }

    public int getContactNameDisplayOrder() {
        return this.mDisplayOrder;
    }

    public void setContactNameDisplayOrder(int i) {
        this.mDisplayOrder = i;
    }

    public int getSortOrder() {
        return this.mSortOrder;
    }

    public void setSortOrder(int i) {
        this.mSortOrder = i;
    }

    public void setPhotoLoader(ContactPhotoManager contactPhotoManager) {
        this.mPhotoLoader = contactPhotoManager;
    }

    protected ContactPhotoManager getPhotoLoader() {
        return this.mPhotoLoader;
    }

    public boolean getDisplayPhotos() {
        return this.mDisplayPhotos;
    }

    public void setDisplayPhotos(boolean z) {
        this.mDisplayPhotos = z;
    }

    public boolean getCircularPhotos() {
        return this.mCircularPhotos;
    }

    public void setEmptyListEnabled(boolean z) {
        this.mEmptyListEnabled = z;
    }

    public boolean isSelectionVisible() {
        return this.mSelectionVisible;
    }

    public void setSelectionVisible(boolean z) {
        this.mSelectionVisible = z;
    }

    public boolean isQuickContactEnabled() {
        return this.mQuickContactEnabled;
    }

    public void setQuickContactEnabled(boolean z) {
        this.mQuickContactEnabled = z;
    }

    public boolean isAdjustSelectionBoundsEnabled() {
        return this.mAdjustSelectionBoundsEnabled;
    }

    public void setAdjustSelectionBoundsEnabled(boolean z) {
        this.mAdjustSelectionBoundsEnabled = z;
    }

    public boolean shouldIncludeFavorites() {
        return this.mIncludeFavorites;
    }

    public void setIncludeFavorites(boolean z) {
        this.mIncludeFavorites = z;
    }

    public void setFavoritesSectionHeader(int i) {
        if (this.mIncludeFavorites) {
            this.mNumberOfFavorites = i;
            setSectionHeader(i);
        }
    }

    public int getNumberOfFavorites() {
        return this.mNumberOfFavorites;
    }

    private void setSectionHeader(int i) {
        SectionIndexer indexer = getIndexer();
        if (indexer != null) {
            ((ContactsSectionIndexer) indexer).setFavoritesHeader(i);
        }
    }

    public void setDarkTheme(boolean z) {
        this.mDarkTheme = z;
    }

    public void changeDirectories(Cursor cursor) {
        if (cursor.getCount() == 0) {
            Log.e("ContactEntryListAdapter", "Directory search loader returned an empty cursor, which implies we have no directory entries.", new RuntimeException());
            return;
        }
        HashSet hashSet = new HashSet();
        int columnIndex = cursor.getColumnIndex("_id");
        int columnIndex2 = cursor.getColumnIndex("directoryType");
        int columnIndex3 = cursor.getColumnIndex("displayName");
        int columnIndex4 = cursor.getColumnIndex("photoSupport");
        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            long j = cursor.getLong(columnIndex);
            hashSet.add(Long.valueOf(j));
            if (getPartitionByDirectoryId(j) == -1) {
                DirectoryPartition directoryPartition = new DirectoryPartition(false, true);
                directoryPartition.setDirectoryId(j);
                if (DirectoryCompat.isRemoteDirectoryId(j)) {
                    if (DirectoryCompat.isEnterpriseDirectoryId(j)) {
                        directoryPartition.setLabel(this.mContext.getString(R.string.directory_search_label_work));
                    } else {
                        directoryPartition.setLabel(this.mContext.getString(R.string.directory_search_label));
                    }
                } else if (DirectoryCompat.isEnterpriseDirectoryId(j)) {
                    directoryPartition.setLabel(this.mContext.getString(R.string.list_filter_phones_work));
                } else {
                    directoryPartition.setLabel(this.mDefaultFilterHeaderText.toString());
                }
                directoryPartition.setDirectoryType(cursor.getString(columnIndex2));
                directoryPartition.setDisplayName(cursor.getString(columnIndex3));
                int i = cursor.getInt(columnIndex4);
                directoryPartition.setPhotoSupported(i == 1 || i == 3);
                addPartition(directoryPartition);
            }
        }
        int partitionCount = getPartitionCount();
        while (true) {
            partitionCount--;
            if (partitionCount >= 0) {
                CompositeCursorAdapter.Partition partition = getPartition(partitionCount);
                if ((partition instanceof DirectoryPartition) && !hashSet.contains(Long.valueOf(((DirectoryPartition) partition).getDirectoryId()))) {
                    removePartition(partitionCount);
                }
            } else {
                invalidate();
                notifyDataSetChanged();
                return;
            }
        }
    }

    @Override
    public void changeCursor(int i, Cursor cursor) {
        if (i >= getPartitionCount()) {
            return;
        }
        CompositeCursorAdapter.Partition partition = getPartition(i);
        if (partition instanceof DirectoryPartition) {
            ((DirectoryPartition) partition).setStatus(2);
        }
        if (this.mDisplayPhotos && this.mPhotoLoader != null && isPhotoSupported(i)) {
            this.mPhotoLoader.refreshCache();
        }
        super.changeCursor(i, cursor);
        if (isSectionHeaderDisplayEnabled() && i == getIndexedPartition()) {
            updateIndexer(cursor);
        }
        this.mPhotoLoader.cancelPendingRequests(this.mFragmentRootView);
    }

    protected void updateIndexer(Cursor cursor) {
        if (cursor == null || cursor.isClosed()) {
            setIndexer(null);
            return;
        }
        Bundle extras = cursor.getExtras();
        if (extras.containsKey("android.provider.extra.ADDRESS_BOOK_INDEX_TITLES") && extras.containsKey("android.provider.extra.ADDRESS_BOOK_INDEX_COUNTS")) {
            String[] stringArray = extras.getStringArray("android.provider.extra.ADDRESS_BOOK_INDEX_TITLES");
            int[] intArray = extras.getIntArray("android.provider.extra.ADDRESS_BOOK_INDEX_COUNTS");
            if (getExtraStartingSection()) {
                String[] strArr = new String[stringArray.length + 1];
                int[] iArr = new int[intArray.length + 1];
                int i = 0;
                while (i < stringArray.length) {
                    int i2 = i + 1;
                    strArr[i2] = stringArray[i];
                    iArr[i2] = intArray[i];
                    i = i2;
                }
                iArr[0] = 1;
                strArr[0] = "";
                setIndexer(new ContactsSectionIndexer(strArr, iArr));
                return;
            }
            setIndexer(new ContactsSectionIndexer(stringArray, intArray));
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
    public int getItemViewType(int i, int i2) {
        int itemViewType = super.getItemViewType(i, i2);
        return (isSectionHeaderDisplayEnabled() && i == getIndexedPartition() && !getItemPlacementInSection(i2).firstInSection) ? itemViewType + getItemViewTypeCount() : itemViewType;
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
        int partitionCount = getPartitionCount();
        for (int i = 0; i < partitionCount; i++) {
            CompositeCursorAdapter.Partition partition = getPartition(i);
            if ((partition instanceof DirectoryPartition) && ((DirectoryPartition) partition).isLoading()) {
                return true;
            }
        }
        return false;
    }

    public boolean areAllPartitionsEmpty() {
        int partitionCount = getPartitionCount();
        for (int i = 0; i < partitionCount; i++) {
            if (!isPartitionEmpty(i)) {
                return false;
            }
        }
        return true;
    }

    public void configureDefaultPartition(boolean z, boolean z2) {
        int partitionCount = getPartitionCount();
        int i = 0;
        while (true) {
            if (i < partitionCount) {
                CompositeCursorAdapter.Partition partition = getPartition(i);
                if ((partition instanceof DirectoryPartition) && ((DirectoryPartition) partition).getDirectoryId() == 0) {
                    break;
                } else {
                    i++;
                }
            } else {
                i = -1;
                break;
            }
        }
        if (i != -1) {
            setShowIfEmpty(i, z);
            setHasHeader(i, z2);
        }
    }

    @Override
    protected View newHeaderView(Context context, int i, Cursor cursor, ViewGroup viewGroup) {
        View viewInflate = LayoutInflater.from(context).inflate(R.layout.directory_header, viewGroup, false);
        if (!getPinnedPartitionHeadersEnabled()) {
            viewInflate.setBackground(null);
        }
        return viewInflate;
    }

    protected void bindWorkProfileIcon(ContactListItemView contactListItemView, int i) {
        CompositeCursorAdapter.Partition partition = getPartition(i);
        if (partition instanceof DirectoryPartition) {
            contactListItemView.setWorkProfileIconEnabled(ContactsUtils.determineUserType(Long.valueOf(((DirectoryPartition) partition).getDirectoryId()), null) == 1);
        }
    }

    @Override
    protected void bindHeaderView(View view, int i, Cursor cursor) {
        CompositeCursorAdapter.Partition partition = getPartition(i);
        if (!(partition instanceof DirectoryPartition)) {
            return;
        }
        DirectoryPartition directoryPartition = (DirectoryPartition) partition;
        long directoryId = directoryPartition.getDirectoryId();
        TextView textView = (TextView) view.findViewById(R.id.label);
        TextView textView2 = (TextView) view.findViewById(R.id.display_name);
        textView.setText(directoryPartition.getLabel());
        if (!DirectoryCompat.isRemoteDirectoryId(directoryId)) {
            textView2.setText((CharSequence) null);
        } else {
            String displayName = directoryPartition.getDisplayName();
            if (TextUtils.isEmpty(displayName)) {
                displayName = directoryPartition.getDirectoryType();
            }
            textView2.setText(displayName);
        }
        view.setPaddingRelative(view.getPaddingStart(), (i == 1 && getPartition(0).isEmpty()) ? 0 : getContext().getResources().getDimensionPixelOffset(R.dimen.directory_header_extra_top_padding), view.getPaddingEnd(), view.getPaddingBottom());
    }

    public boolean isPhotoSupported(int i) {
        CompositeCursorAdapter.Partition partition = getPartition(i);
        if (partition instanceof DirectoryPartition) {
            return ((DirectoryPartition) partition).isPhotoSupported();
        }
        return true;
    }

    public ContactListFilter getFilter() {
        return this.mFilter;
    }

    public void setFilter(ContactListFilter contactListFilter) {
        this.mFilter = contactListFilter;
    }

    protected void bindQuickContact(ContactListItemView contactListItemView, int i, Cursor cursor, int i2, int i3, int i4, int i5, int i6) {
        long j;
        Uri uri;
        ContactPhotoManager.DefaultImageRequest defaultImageRequestFromCursor;
        if (!cursor.isNull(i2)) {
            j = cursor.getLong(i2);
        } else {
            j = 0;
        }
        QuickContactBadge quickContact = contactListItemView.getQuickContact();
        quickContact.assignContactUri(getContactUri(i, cursor, i4, i5));
        if (CompatUtils.hasPrioritizedMimeType()) {
            quickContact.setPrioritizedMimeType("vnd.android.cursor.item/phone_v2");
        }
        if (j != 0 || i3 == -1) {
            getPhotoLoader().loadThumbnail(quickContact, j, this.mDarkTheme, this.mCircularPhotos, null);
            return;
        }
        String string = cursor.getString(i3);
        if (string != null) {
            uri = Uri.parse(string);
        } else {
            uri = null;
        }
        if (uri != null) {
            defaultImageRequestFromCursor = null;
        } else {
            defaultImageRequestFromCursor = getDefaultImageRequestFromCursor(cursor, i6, i5);
        }
        getPhotoLoader().loadPhoto(quickContact, uri, -1, this.mDarkTheme, this.mCircularPhotos, defaultImageRequestFromCursor);
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    protected void bindViewId(ContactListItemView contactListItemView, Cursor cursor, int i) {
        contactListItemView.setId((int) (cursor.getLong(i) % 2147483647L));
    }

    protected Uri getContactUri(int i, Cursor cursor, int i2, int i3) {
        long j = cursor.getLong(i2);
        String string = cursor.getString(i3);
        long directoryId = ((DirectoryPartition) getPartition(i)).getDirectoryId();
        Uri lookupUri = ContactsContract.Contacts.getLookupUri(j, string);
        if (lookupUri != null && directoryId != 0) {
            return lookupUri.buildUpon().appendQueryParameter("directory", String.valueOf(directoryId)).build();
        }
        return lookupUri;
    }

    public ContactPhotoManager.DefaultImageRequest getDefaultImageRequestFromCursor(Cursor cursor, int i, int i2) {
        return ContactsCommonListUtils.getDefaultImageRequest(cursor, cursor.getString(i), cursor.getString(i2), this.mCircularPhotos);
    }

    public void setShowSdnNumber(boolean z) {
        this.mShowSdnNumber = z;
    }

    public boolean isSdnNumber(int i) {
        int partitionForPosition = getPartitionForPosition(i);
        boolean z = false;
        if (partitionForPosition >= 0) {
            int position = getCursor(partitionForPosition).getPosition();
            Cursor cursor = (Cursor) getItem(i);
            if (cursor != null && cursor.getPosition() >= 0 && cursor.getPosition() < cursor.getCount()) {
                if (cursor.getInt(cursor.getColumnIndex("is_sdn_contact")) == 1) {
                    z = true;
                }
                cursor.moveToPosition(position);
            }
        }
        if (z) {
            Log.d("ContactEntryListAdapter", "[isSdnNumber] isSdnNumber = " + z);
        }
        return z;
    }
}
