package com.android.contacts.list;

import android.content.Context;
import android.database.Cursor;
import com.android.contacts.common.list.ContactEntry;
import com.android.contacts.common.list.ContactTileAdapter;
import com.android.contacts.common.list.ContactTileView;
import com.google.common.collect.Lists;
import java.util.ArrayList;

public class GroupMemberTileAdapter extends ContactTileAdapter {
    public GroupMemberTileAdapter(Context context, ContactTileView.Listener listener, int numCols) {
        super(context, listener, numCols, ContactTileAdapter.DisplayType.GROUP_MEMBERS);
    }

    @Override
    protected void bindColumnIndices() {
        this.mIdIndex = 0;
        this.mLookupIndex = 2;
        this.mPhotoUriIndex = 1;
        this.mNameIndex = 3;
        this.mPresenceIndex = 4;
        this.mStatusIndex = 5;
    }

    @Override
    protected void saveNumFrequentsFromCursor(Cursor cursor) {
        this.mNumFrequents = 0;
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    protected int getDividerPosition(Cursor cursor) {
        return -1;
    }

    @Override
    public int getCount() {
        if (this.mContactCursor == null || this.mContactCursor.isClosed()) {
            return 0;
        }
        return getRowCount(this.mContactCursor.getCount());
    }

    @Override
    public ArrayList<ContactEntry> getItem(int position) {
        ArrayList<ContactEntry> resultList = Lists.newArrayListWithCapacity(this.mColumnCount);
        int contactIndex = position * this.mColumnCount;
        for (int columnCounter = 0; columnCounter < this.mColumnCount; columnCounter++) {
            resultList.add(createContactEntryFromCursor(this.mContactCursor, contactIndex));
            contactIndex++;
        }
        return resultList;
    }
}
