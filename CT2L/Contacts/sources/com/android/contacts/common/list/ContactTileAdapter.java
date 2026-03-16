package com.android.contacts.common.list;

import android.content.ContentUris;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.ContactsContract;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.android.contacts.R;
import com.android.contacts.common.ContactPhotoManager;
import com.android.contacts.common.ContactPresenceIconUtil;
import com.android.contacts.common.ContactStatusUtil;
import com.android.contacts.common.MoreContactUtils;
import com.android.contacts.common.list.ContactTileView;
import java.util.ArrayList;

public class ContactTileAdapter extends BaseAdapter {
    private static final String TAG = ContactTileAdapter.class.getSimpleName();
    protected int mColumnCount;
    private Context mContext;
    private DisplayType mDisplayType;
    private int mDividerPosition;
    protected int mIdIndex;
    private ContactTileView.Listener mListener;
    protected int mLookupIndex;
    protected int mNameIndex;
    protected int mNumFrequents;
    private final int mPaddingInPixels;
    private ContactPhotoManager mPhotoManager;
    protected int mPhotoUriIndex;
    protected int mPresenceIndex;
    private Resources mResources;
    private int mStarredIndex;
    protected int mStatusIndex;
    private final int mWhitespaceStartEnd;
    protected Cursor mContactCursor = null;
    private boolean mIsQuickContactEnabled = false;

    public enum DisplayType {
        STREQUENT,
        STARRED_ONLY,
        FREQUENT_ONLY,
        GROUP_MEMBERS
    }

    public ContactTileAdapter(Context context, ContactTileView.Listener listener, int numCols, DisplayType displayType) {
        this.mListener = listener;
        this.mContext = context;
        this.mResources = context.getResources();
        this.mColumnCount = displayType == DisplayType.FREQUENT_ONLY ? 1 : numCols;
        this.mDisplayType = displayType;
        this.mNumFrequents = 0;
        this.mPaddingInPixels = this.mContext.getResources().getDimensionPixelSize(R.dimen.contact_tile_divider_padding);
        this.mWhitespaceStartEnd = this.mContext.getResources().getDimensionPixelSize(R.dimen.contact_tile_start_end_whitespace);
        bindColumnIndices();
    }

    public void setPhotoLoader(ContactPhotoManager photoLoader) {
        this.mPhotoManager = photoLoader;
    }

    public void setDisplayType(DisplayType displayType) {
        this.mDisplayType = displayType;
    }

    protected void bindColumnIndices() {
        this.mIdIndex = 0;
        this.mLookupIndex = 4;
        this.mPhotoUriIndex = 3;
        this.mNameIndex = 1;
        this.mStarredIndex = 2;
        this.mPresenceIndex = 5;
        this.mStatusIndex = 6;
    }

    private static boolean cursorIsValid(Cursor cursor) {
        return (cursor == null || cursor.isClosed()) ? false : true;
    }

    protected void saveNumFrequentsFromCursor(Cursor cursor) {
        switch (this.mDisplayType) {
            case STARRED_ONLY:
                this.mNumFrequents = 0;
                return;
            case STREQUENT:
                this.mNumFrequents = cursorIsValid(cursor) ? cursor.getCount() - this.mDividerPosition : 0;
                return;
            case FREQUENT_ONLY:
                this.mNumFrequents = cursorIsValid(cursor) ? cursor.getCount() : 0;
                return;
            default:
                throw new IllegalArgumentException("Unrecognized DisplayType " + this.mDisplayType);
        }
    }

    public void setContactCursor(Cursor cursor) {
        this.mContactCursor = cursor;
        this.mDividerPosition = getDividerPosition(cursor);
        saveNumFrequentsFromCursor(cursor);
        notifyDataSetChanged();
    }

    protected int getDividerPosition(Cursor cursor) {
        switch (this.mDisplayType) {
            case STARRED_ONLY:
                return -1;
            case STREQUENT:
                if (!cursorIsValid(cursor)) {
                    return 0;
                }
                cursor.moveToPosition(-1);
                while (cursor.moveToNext()) {
                    if (cursor.getInt(this.mStarredIndex) == 0) {
                        return cursor.getPosition();
                    }
                }
                return cursor.getCount();
            case FREQUENT_ONLY:
                return 0;
            default:
                throw new IllegalStateException("Unrecognized DisplayType " + this.mDisplayType);
        }
    }

    protected ContactEntry createContactEntryFromCursor(Cursor cursor, int position) {
        if (!cursorIsValid(cursor) || cursor.getCount() <= position) {
            return null;
        }
        cursor.moveToPosition(position);
        long id = cursor.getLong(this.mIdIndex);
        String photoUri = cursor.getString(this.mPhotoUriIndex);
        String lookupKey = cursor.getString(this.mLookupIndex);
        ContactEntry contact = new ContactEntry();
        String name = cursor.getString(this.mNameIndex);
        if (name == null) {
            name = this.mResources.getString(R.string.missing_name);
        }
        contact.name = name;
        contact.status = cursor.getString(this.mStatusIndex);
        contact.photoUri = photoUri != null ? Uri.parse(photoUri) : null;
        contact.lookupKey = lookupKey;
        contact.lookupUri = ContentUris.withAppendedId(Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, lookupKey), id);
        contact.isFavorite = cursor.getInt(this.mStarredIndex) > 0;
        Drawable icon = null;
        int presence = 0;
        if (!cursor.isNull(this.mPresenceIndex)) {
            presence = cursor.getInt(this.mPresenceIndex);
            icon = ContactPresenceIconUtil.getPresenceIcon(this.mContext, presence);
        }
        contact.presenceIcon = icon;
        String statusMessage = null;
        if (this.mStatusIndex != 0 && !cursor.isNull(this.mStatusIndex)) {
            statusMessage = cursor.getString(this.mStatusIndex);
        }
        if (statusMessage == null && presence != 0) {
            statusMessage = ContactStatusUtil.getStatusString(this.mContext, presence);
        }
        contact.status = statusMessage;
        return contact;
    }

    public int getNumFrequents() {
        return this.mNumFrequents;
    }

    @Override
    public int getCount() {
        if (!cursorIsValid(this.mContactCursor)) {
            return 0;
        }
        switch (this.mDisplayType) {
            case STARRED_ONLY:
                return getRowCount(this.mContactCursor.getCount());
            case STREQUENT:
                int starredRowCount = getRowCount(this.mDividerPosition);
                int frequentRowCount = this.mNumFrequents != 0 ? this.mNumFrequents + 1 : 0;
                return frequentRowCount + starredRowCount;
            case FREQUENT_ONLY:
                return this.mContactCursor.getCount();
            default:
                throw new IllegalArgumentException("Unrecognized DisplayType " + this.mDisplayType);
        }
    }

    protected int getRowCount(int entryCount) {
        if (entryCount == 0) {
            return 0;
        }
        return ((entryCount - 1) / this.mColumnCount) + 1;
    }

    public int getColumnCount() {
        return this.mColumnCount;
    }

    @Override
    public ArrayList<ContactEntry> getItem(int position) {
        ArrayList<ContactEntry> resultList = new ArrayList<>(this.mColumnCount);
        int contactIndex = position * this.mColumnCount;
        switch (this.mDisplayType) {
            case STARRED_ONLY:
                for (int columnCounter = 0; columnCounter < this.mColumnCount; columnCounter++) {
                    resultList.add(createContactEntryFromCursor(this.mContactCursor, contactIndex));
                    contactIndex++;
                }
                return resultList;
            case STREQUENT:
                if (position < getRowCount(this.mDividerPosition)) {
                    for (int columnCounter2 = 0; columnCounter2 < this.mColumnCount && contactIndex != this.mDividerPosition; columnCounter2++) {
                        resultList.add(createContactEntryFromCursor(this.mContactCursor, contactIndex));
                        contactIndex++;
                    }
                } else {
                    resultList.add(createContactEntryFromCursor(this.mContactCursor, ((position - getRowCount(this.mDividerPosition)) - 1) + this.mDividerPosition));
                }
                return resultList;
            case FREQUENT_ONLY:
                resultList.add(createContactEntryFromCursor(this.mContactCursor, position));
                return resultList;
            default:
                throw new IllegalStateException("Unrecognized DisplayType " + this.mDisplayType);
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return this.mDisplayType != DisplayType.STREQUENT;
    }

    @Override
    public boolean isEnabled(int position) {
        return position != getRowCount(this.mDividerPosition);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        int itemViewType = getItemViewType(position);
        if (itemViewType == 1) {
            TextView textView = (TextView) (convertView == null ? getDivider() : convertView);
            setDividerPadding(textView, position == 0);
            return textView;
        }
        ContactTileRow contactTileRowView = (ContactTileRow) convertView;
        ArrayList<ContactEntry> contactList = getItem(position);
        if (contactTileRowView == null) {
            contactTileRowView = new ContactTileRow(this.mContext, itemViewType);
        }
        contactTileRowView.configureRow(contactList, position == getCount() + (-1));
        return contactTileRowView;
    }

    private TextView getDivider() {
        return MoreContactUtils.createHeaderView(this.mContext, R.string.favoritesFrequentContacted);
    }

    private void setDividerPadding(TextView headerTextView, boolean isFirstRow) {
        MoreContactUtils.setHeaderViewBottomPadding(this.mContext, headerTextView, isFirstRow);
    }

    private int getLayoutResourceId(int viewType) {
        switch (viewType) {
            case 0:
                return this.mIsQuickContactEnabled ? R.layout.contact_tile_starred_quick_contact : R.layout.contact_tile_starred;
            case 1:
            default:
                throw new IllegalArgumentException("Unrecognized viewType " + viewType);
            case 2:
                return R.layout.contact_tile_frequent;
        }
    }

    @Override
    public int getViewTypeCount() {
        return 4;
    }

    @Override
    public int getItemViewType(int position) {
        switch (this.mDisplayType) {
            case STARRED_ONLY:
                return 0;
            case STREQUENT:
                if (position < getRowCount(this.mDividerPosition)) {
                    return 0;
                }
                return position == getRowCount(this.mDividerPosition) ? 1 : 2;
            case FREQUENT_ONLY:
                return 2;
            default:
                throw new IllegalStateException("Unrecognized DisplayType " + this.mDisplayType);
        }
    }

    private class ContactTileRow extends FrameLayout {
        private int mItemViewType;
        private int mLayoutResId;

        public ContactTileRow(Context context, int itemViewType) {
            super(context);
            this.mItemViewType = itemViewType;
            this.mLayoutResId = ContactTileAdapter.this.getLayoutResourceId(this.mItemViewType);
            setImportantForAccessibility(2);
        }

        public void configureRow(ArrayList<ContactEntry> list, boolean isLastRow) {
            int columnCount = this.mItemViewType == 2 ? 1 : ContactTileAdapter.this.mColumnCount;
            int columnCounter = 0;
            while (columnCounter < columnCount) {
                ContactEntry entry = columnCounter < list.size() ? list.get(columnCounter) : null;
                addTileFromEntry(entry, columnCounter, isLastRow);
                columnCounter++;
            }
        }

        private void addTileFromEntry(ContactEntry entry, int childIndex, boolean isLastRow) {
            ContactTileView contactTile;
            if (getChildCount() <= childIndex) {
                contactTile = (ContactTileView) inflate(this.mContext, this.mLayoutResId, null);
                this.mContext.getResources();
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(-1, -2);
                params.setMargins(ContactTileAdapter.this.mWhitespaceStartEnd, 0, ContactTileAdapter.this.mWhitespaceStartEnd, 0);
                contactTile.setLayoutParams(params);
                contactTile.setPhotoManager(ContactTileAdapter.this.mPhotoManager);
                contactTile.setListener(ContactTileAdapter.this.mListener);
                addView(contactTile);
            } else {
                contactTile = (ContactTileView) getChildAt(childIndex);
            }
            contactTile.loadFromContact(entry);
            switch (this.mItemViewType) {
                case 0:
                    contactTile.setPaddingRelative((ContactTileAdapter.this.mPaddingInPixels + 1) / 2, 0, ContactTileAdapter.this.mPaddingInPixels / 2, 0);
                    break;
                case 2:
                    contactTile.setHorizontalDividerVisibility(isLastRow ? 8 : 0);
                    break;
            }
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            switch (this.mItemViewType) {
                case 0:
                    onLayoutForTiles();
                    break;
                default:
                    super.onLayout(changed, left, top, right, bottom);
                    break;
            }
        }

        private void onLayoutForTiles() {
            int count = getChildCount();
            int childLeft = ContactTileAdapter.this.mWhitespaceStartEnd - ((ContactTileAdapter.this.mPaddingInPixels + 1) / 2);
            for (int i = 0; i < count; i++) {
                int rtlAdjustedIndex = isLayoutRtl() ? (count - i) - 1 : i;
                View child = getChildAt(rtlAdjustedIndex);
                int childWidth = child.getMeasuredWidth();
                child.layout(childLeft, 0, childLeft + childWidth, child.getMeasuredHeight());
                childLeft += childWidth;
            }
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            switch (this.mItemViewType) {
                case 0:
                    onMeasureForTiles(widthMeasureSpec);
                    break;
                default:
                    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                    break;
            }
        }

        private void onMeasureForTiles(int widthMeasureSpec) {
            int width = View.MeasureSpec.getSize(widthMeasureSpec);
            int childCount = getChildCount();
            if (childCount != 0) {
                int totalWhitespaceInPixels = ((ContactTileAdapter.this.mColumnCount - 1) * ContactTileAdapter.this.mPaddingInPixels) + (ContactTileAdapter.this.mWhitespaceStartEnd * 2);
                int imageSize = (width - totalWhitespaceInPixels) / ContactTileAdapter.this.mColumnCount;
                int remainder = (width - (ContactTileAdapter.this.mColumnCount * imageSize)) - totalWhitespaceInPixels;
                int i = 0;
                while (i < childCount) {
                    View child = getChildAt(i);
                    int childWidth = child.getPaddingLeft() + child.getPaddingRight() + imageSize + (i < remainder ? 1 : 0);
                    child.measure(View.MeasureSpec.makeMeasureSpec(childWidth, 1073741824), View.MeasureSpec.makeMeasureSpec(0, 0));
                    i++;
                }
                setMeasuredDimension(width, getChildAt(0).getMeasuredHeight());
                return;
            }
            setMeasuredDimension(width, 0);
        }
    }
}
