package com.android.contacts.list;

import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import com.android.contacts.CallUtil;
import com.android.contacts.ContactPhotoManager;
import com.android.contacts.ContactsUtils;
import com.android.contacts.GeoUtil;
import com.android.contacts.R;
import com.android.contacts.compat.CallableCompat;
import com.android.contacts.compat.CompatUtils;
import com.android.contacts.compat.DirectoryCompat;
import com.android.contacts.compat.PhoneCompat;
import com.android.contacts.extensions.ExtendedPhoneDirectoriesManager;
import com.android.contacts.extensions.ExtensionsFactory;
import com.android.contacts.list.ContactListItemView;
import com.android.contacts.list.IndexerListAdapter;
import com.google.common.collect.Lists;
import com.mediatek.contacts.ExtensionManager;
import com.mediatek.contacts.GlobalEnv;
import com.mediatek.contacts.util.ContactsCommonListUtils;
import com.mediatek.contacts.util.ContactsPortableUtils;
import com.mediatek.contacts.util.Log;
import java.util.ArrayList;
import java.util.List;

public class PhoneNumberListAdapter extends ContactEntryListAdapter {
    private static final String TAG = PhoneNumberListAdapter.class.getSimpleName();
    private final String mCountryIso;
    private final List<DirectoryPartition> mExtendedDirectories;
    private long mFirstExtendedDirectoryId;
    private boolean mIsPresenceEnabled;
    private boolean mIsVideoEnabled;
    private Listener mListener;
    private ContactListItemView.PhotoPosition mPhotoPosition;
    private final CharSequence mUnknownNameText;
    private boolean mUseCallableUri;

    public interface Listener {
        void onVideoCallIconClicked(int i);
    }

    public static class PhoneQuery {
        public static int CARRIER_PRESENCE;
        public static final String[] PROJECTION_ALTERNATIVE;
        public static final String[] PROJECTION_ALTERNATIVE_INTERNAL;
        public static final String[] PROJECTION_PRIMARY;
        public static final String[] PROJECTION_PRIMARY_INTERNAL = {"_id", "data2", "data3", "data1", "contact_id", "lookup", "photo_id", "display_name", "photo_thumb_uri"};

        static {
            ArrayList arrayListNewArrayList = Lists.newArrayList(PROJECTION_PRIMARY_INTERNAL);
            if (ContactsPortableUtils.MTK_PHONE_BOOK_SUPPORT) {
                arrayListNewArrayList.add("indicate_phone_or_sim_contact");
                arrayListNewArrayList.add("is_sdn_contact");
            }
            if (CompatUtils.isMarshmallowCompatible()) {
                arrayListNewArrayList.add("carrier_presence");
                CARRIER_PRESENCE = arrayListNewArrayList.size() - 1;
            }
            PROJECTION_PRIMARY = (String[]) arrayListNewArrayList.toArray(new String[arrayListNewArrayList.size()]);
            PROJECTION_ALTERNATIVE_INTERNAL = new String[]{"_id", "data2", "data3", "data1", "contact_id", "lookup", "photo_id", "display_name_alt", "photo_thumb_uri"};
            ArrayList arrayListNewArrayList2 = Lists.newArrayList(PROJECTION_ALTERNATIVE_INTERNAL);
            if (ContactsPortableUtils.MTK_PHONE_BOOK_SUPPORT) {
                arrayListNewArrayList2.add("indicate_phone_or_sim_contact");
                arrayListNewArrayList2.add("is_sdn_contact");
            }
            if (CompatUtils.isMarshmallowCompatible()) {
                arrayListNewArrayList2.add("carrier_presence");
                CARRIER_PRESENCE = arrayListNewArrayList2.size() - 1;
            }
            PROJECTION_ALTERNATIVE = (String[]) arrayListNewArrayList2.toArray(new String[arrayListNewArrayList2.size()]);
        }
    }

    public PhoneNumberListAdapter(Context context) {
        super(context);
        this.mFirstExtendedDirectoryId = Long.MAX_VALUE;
        setDefaultFilterHeaderText(R.string.list_filter_phones);
        this.mUnknownNameText = context.getText(android.R.string.unknownName);
        this.mCountryIso = GeoUtil.getCurrentCountryIso(context);
        ExtendedPhoneDirectoriesManager extendedPhoneDirectoriesManager = ExtensionsFactory.getExtendedPhoneDirectoriesManager();
        if (extendedPhoneDirectoriesManager != null) {
            this.mExtendedDirectories = extendedPhoneDirectoriesManager.getExtendedDirectories(this.mContext);
        } else {
            this.mExtendedDirectories = new ArrayList();
        }
        int videoCallingAvailability = CallUtil.getVideoCallingAvailability(context);
        this.mIsVideoEnabled = (videoCallingAvailability & 1) != 0;
        this.mIsPresenceEnabled = (videoCallingAvailability & 2) != 0;
    }

    @Override
    public void configureLoader(CursorLoader cursorLoader, long j) {
        Uri.Builder builderAppendQueryParameter;
        String str;
        Uri contentFilterUri;
        String queryString = getQueryString();
        if (queryString == null) {
            queryString = "";
        }
        if (isExtendedDirectory(j)) {
            DirectoryPartition extendedDirectoryFromId = getExtendedDirectoryFromId(j);
            String contentUri = extendedDirectoryFromId.getContentUri();
            if (contentUri == null) {
                throw new IllegalStateException("Extended directory must have a content URL: " + extendedDirectoryFromId);
            }
            Uri.Builder builderBuildUpon = Uri.parse(contentUri).buildUpon();
            builderBuildUpon.appendPath(queryString);
            builderBuildUpon.appendQueryParameter("limit", String.valueOf(getDirectoryResultLimit(extendedDirectoryFromId)));
            cursorLoader.setUri(builderBuildUpon.build());
            cursorLoader.setProjection(PhoneQuery.PROJECTION_PRIMARY);
            return;
        }
        boolean zIsRemoteDirectoryId = DirectoryCompat.isRemoteDirectoryId(j);
        if (isSearchMode()) {
            if (!zIsRemoteDirectoryId && this.mUseCallableUri) {
                contentFilterUri = CallableCompat.getContentFilterUri();
            } else {
                contentFilterUri = PhoneCompat.getContentFilterUri();
            }
            Uri.Builder builderBuildUpon2 = contentFilterUri.buildUpon();
            builderBuildUpon2.appendPath(queryString);
            builderBuildUpon2.appendQueryParameter("directory", String.valueOf(j));
            if (zIsRemoteDirectoryId) {
                builderBuildUpon2.appendQueryParameter("limit", String.valueOf(getDirectoryResultLimit(getDirectoryById(j))));
            }
            builderAppendQueryParameter = builderBuildUpon2;
        } else {
            builderAppendQueryParameter = (this.mUseCallableUri ? ContactsContract.CommonDataKinds.Callable.CONTENT_URI : ContactsContract.CommonDataKinds.Phone.CONTENT_URI).buildUpon().appendQueryParameter("directory", String.valueOf(0L));
            if (isSectionHeaderDisplayEnabled()) {
                builderAppendQueryParameter.appendQueryParameter("android.provider.extra.ADDRESS_BOOK_INDEX", "true");
            }
            applyFilter(cursorLoader, builderAppendQueryParameter, j, getFilter());
        }
        String selection = cursorLoader.getSelection();
        if (!TextUtils.isEmpty(selection)) {
            str = selection + " AND length(data1) < 1000";
        } else {
            str = "length(data1) < 1000";
        }
        cursorLoader.setSelection(str);
        builderAppendQueryParameter.appendQueryParameter("remove_duplicate_entries", "true");
        cursorLoader.setUri(builderAppendQueryParameter.build());
        if (getContactNameDisplayOrder() == 1) {
            cursorLoader.setProjection(PhoneQuery.PROJECTION_PRIMARY);
        } else {
            cursorLoader.setProjection(PhoneQuery.PROJECTION_ALTERNATIVE);
        }
        if (getSortOrder() == 1) {
            cursorLoader.setSortOrder("sort_key");
        } else {
            cursorLoader.setSortOrder("sort_key_alt");
        }
    }

    protected boolean isExtendedDirectory(long j) {
        return j >= this.mFirstExtendedDirectoryId;
    }

    private DirectoryPartition getExtendedDirectoryFromId(long j) {
        return this.mExtendedDirectories.get((int) (j - this.mFirstExtendedDirectoryId));
    }

    private void applyFilter(CursorLoader cursorLoader, Uri.Builder builder, long j, ContactListFilter contactListFilter) {
        if (contactListFilter == null || j != 0) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        ArrayList arrayList = new ArrayList();
        int i = contactListFilter.filterType;
        if (i != -5) {
            switch (i) {
                case -3:
                    sb.append("in_visible_group=1");
                    sb.append(" AND has_phone_number=1");
                    break;
                case -2:
                case -1:
                    break;
                case 0:
                    buildSelectionForFilterAccount(contactListFilter, sb, arrayList);
                    break;
                default:
                    Log.w(TAG, "Unsupported filter type came (type: " + contactListFilter.filterType + ", toString: " + contactListFilter + ") showing all contacts.");
                    break;
            }
        }
        cursorLoader.setSelection(sb.toString());
        cursorLoader.setSelectionArgs((String[]) arrayList.toArray(new String[0]));
    }

    public String getPhoneNumber(int i) {
        Cursor cursor = (Cursor) getItem(i);
        if (cursor != null) {
            return cursor.getString(3);
        }
        return null;
    }

    public Uri getDataUri(int i) {
        int partitionForPosition = getPartitionForPosition(i);
        Cursor cursor = (Cursor) getItem(i);
        if (cursor != null) {
            return getDataUri(partitionForPosition, cursor);
        }
        return null;
    }

    public Uri getDataUri(int i, Cursor cursor) {
        long directoryId = ((DirectoryPartition) getPartition(i)).getDirectoryId();
        if (DirectoryCompat.isRemoteDirectoryId(directoryId) || DirectoryCompat.isEnterpriseDirectoryId(directoryId)) {
            return null;
        }
        return ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, cursor.getLong(0));
    }

    @Override
    protected ContactListItemView newView(Context context, int i, Cursor cursor, int i2, ViewGroup viewGroup) {
        ContactListItemView contactListItemViewNewView = super.newView(context, i, cursor, i2, viewGroup);
        contactListItemViewNewView.setUnknownNameText(this.mUnknownNameText);
        contactListItemViewNewView.setQuickContactEnabled(isQuickContactEnabled());
        contactListItemViewNewView.setPhotoPosition(this.mPhotoPosition);
        return contactListItemViewNewView;
    }

    protected void setHighlight(ContactListItemView contactListItemView, Cursor cursor) {
        contactListItemView.setHighlightedPrefix(isSearchMode() ? getUpperCaseQueryString() : null);
    }

    @Override
    protected void bindView(View view, int i, Cursor cursor, int i2) {
        super.bindView(view, i, cursor, i2);
        ContactListItemView contactListItemView = (ContactListItemView) view;
        setHighlight(contactListItemView, cursor);
        cursor.moveToPosition(i2);
        long j = cursor.getLong(4);
        boolean z = (cursor.moveToPrevious() && !cursor.isBeforeFirst() && j == cursor.getLong(4)) ? false : true;
        cursor.moveToPosition(i2);
        if (!cursor.moveToNext() || cursor.isAfterLast() || j == cursor.getLong(4)) {
        }
        cursor.moveToPosition(i2);
        bindViewId(contactListItemView, cursor, 0);
        bindSectionHeaderAndDivider(contactListItemView, i2);
        if (z) {
            bindName(contactListItemView, cursor);
            if (isQuickContactEnabled()) {
                bindQuickContact(contactListItemView, i, cursor, 6, 8, 4, 5, 7);
            } else if (getDisplayPhotos()) {
                bindPhoto(contactListItemView, i, cursor);
            }
        } else {
            unbindName(contactListItemView);
            contactListItemView.removePhotoView(true, false);
        }
        bindPhoneNumber(contactListItemView, cursor, ((DirectoryPartition) getPartition(i)).isDisplayNumber(), i2);
    }

    protected void bindPhoneNumber(ContactListItemView contactListItemView, Cursor cursor, boolean z, int i) {
        CharSequence typeLabel;
        String string;
        if (z && !cursor.isNull(1)) {
            int i2 = cursor.getInt(1);
            String string2 = cursor.getString(2);
            typeLabel = ContactsContract.CommonDataKinds.Phone.getTypeLabel(getContext().getResources(), i2, string2);
            if (ContactsPortableUtils.MTK_PHONE_BOOK_SUPPORT) {
                typeLabel = GlobalEnv.getSimAasEditor().getTypeLabel(i2, string2, (String) typeLabel, cursor.getInt(9));
            }
        } else {
            typeLabel = null;
        }
        contactListItemView.setLabel(typeLabel);
        if (z) {
            string = cursor.getString(3);
        } else {
            string = cursor.getString(2);
            if (string == null) {
                string = GeoUtil.getGeocodedLocationFor(this.mContext, cursor.getString(3));
            }
        }
        contactListItemView.setPhoneNumber(string, this.mCountryIso);
        if (CompatUtils.isVideoCompatible()) {
            boolean z2 = false;
            boolean z3 = (cursor.getInt(PhoneQuery.CARRIER_PRESENCE) & 1) != 0;
            if (this.mIsVideoEnabled && ((this.mIsPresenceEnabled && z3) || !this.mIsPresenceEnabled)) {
                z2 = true;
            }
            contactListItemView.setShowVideoCallIcon(z2, this.mListener, i);
        }
        ExtensionManager.getInstance();
        ExtensionManager.getContactsCommonPresenceExtension().bindPhoneNumber(contactListItemView, cursor);
    }

    protected void bindSectionHeaderAndDivider(ContactListItemView contactListItemView, int i) {
        if (isSectionHeaderDisplayEnabled()) {
            IndexerListAdapter.Placement itemPlacementInSection = getItemPlacementInSection(i);
            contactListItemView.setSectionHeader(itemPlacementInSection.firstInSection ? itemPlacementInSection.sectionHeader : null);
        } else {
            contactListItemView.setSectionHeader(null);
        }
    }

    protected void bindName(ContactListItemView contactListItemView, Cursor cursor) {
        contactListItemView.showDisplayName(cursor, 7, getContactNameDisplayOrder());
    }

    protected void unbindName(ContactListItemView contactListItemView) {
        contactListItemView.hideDisplayName();
    }

    @Override
    protected void bindWorkProfileIcon(ContactListItemView contactListItemView, int i) {
        long directoryId = ((DirectoryPartition) getPartition(i)).getDirectoryId();
        contactListItemView.setWorkProfileIconEnabled(!isExtendedDirectory(directoryId) && ContactsUtils.determineUserType(Long.valueOf(directoryId), null) == 1);
    }

    protected void bindPhoto(ContactListItemView contactListItemView, int i, Cursor cursor) {
        Uri uri;
        if (!isPhotoSupported(i)) {
            contactListItemView.removePhotoView();
            return;
        }
        long j = !cursor.isNull(6) ? cursor.getLong(6) : 0L;
        if (j != 0) {
            getPhotoLoader().loadThumbnail(contactListItemView.getPhotoView(), j, false, getCircularPhotos(), null);
            return;
        }
        String string = cursor.getString(8);
        ContactPhotoManager.DefaultImageRequest defaultImageRequest = null;
        if (string != null) {
            uri = Uri.parse(string);
        } else {
            uri = null;
        }
        if (uri == null) {
            defaultImageRequest = ContactsCommonListUtils.getDefaultImageRequest(cursor, cursor.getString(7), cursor.getString(5), getCircularPhotos());
        }
        getPhotoLoader().loadDirectoryPhoto(contactListItemView.getPhotoView(), uri, false, getCircularPhotos(), defaultImageRequest);
    }

    public void setPhotoPosition(ContactListItemView.PhotoPosition photoPosition) {
        this.mPhotoPosition = photoPosition;
    }

    public void setUseCallableUri(boolean z) {
        this.mUseCallableUri = z;
    }

    @Override
    public void changeDirectories(Cursor cursor) {
        super.changeDirectories(cursor);
        if (getDirectorySearchMode() == 0) {
            return;
        }
        int size = this.mExtendedDirectories.size();
        if (getPartitionCount() == cursor.getCount() + size) {
            return;
        }
        this.mFirstExtendedDirectoryId = Long.MAX_VALUE;
        if (size > 0) {
            int partitionCount = getPartitionCount();
            long j = 1;
            int i = 0;
            for (int i2 = 0; i2 < partitionCount; i2++) {
                long directoryId = ((DirectoryPartition) getPartition(i2)).getDirectoryId();
                if (directoryId > j) {
                    j = directoryId;
                }
                if (!DirectoryCompat.isRemoteDirectoryId(directoryId)) {
                    i = i2 + 1;
                }
            }
            this.mFirstExtendedDirectoryId = j + 1;
            for (int i3 = 0; i3 < size; i3++) {
                long j2 = this.mFirstExtendedDirectoryId + ((long) i3);
                DirectoryPartition directoryPartition = this.mExtendedDirectories.get(i3);
                if (getPartitionByDirectoryId(j2) == -1) {
                    addPartition(i, directoryPartition);
                    directoryPartition.setDirectoryId(j2);
                }
            }
        }
    }

    @Override
    protected Uri getContactUri(int i, Cursor cursor, int i2, int i3) {
        DirectoryPartition directoryPartition = (DirectoryPartition) getPartition(i);
        long directoryId = directoryPartition.getDirectoryId();
        if (!isExtendedDirectory(directoryId)) {
            return super.getContactUri(i, cursor, i2, i3);
        }
        return ContactsContract.Contacts.CONTENT_LOOKUP_URI.buildUpon().appendPath("encoded").appendQueryParameter("displayName", directoryPartition.getLabel()).appendQueryParameter("directory", String.valueOf(directoryId)).encodedFragment(cursor.getString(i3)).build();
    }

    private void buildSelectionForFilterAccount(ContactListFilter contactListFilter, StringBuilder sb, List<String> list) {
        if ("Local Phone Account".equals(contactListFilter.accountType)) {
            sb.append("((account_type IS NULL  AND account_name IS NULL ) OR (account_type=? AND account_name=? )");
        } else {
            sb.append("(account_type=? AND account_name=? ");
        }
        ContactsCommonListUtils.buildSelectionForFilterAccount(contactListFilter, sb, list);
    }
}
