package com.mediatek.contacts.list;

import android.R;
import android.content.Context;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.CheckBox;
import com.android.contacts.group.GroupListItem;
import com.mediatek.contacts.group.GroupBrowseListAdapter;
import com.mediatek.contacts.util.Log;

public class ContactGroupListAdapter extends GroupBrowseListAdapter {
    private SparseBooleanArray mSparseBooleanArray;

    public ContactGroupListAdapter(Context context) {
        super(context);
    }

    public void setSparseBooleanArray(SparseBooleanArray sparseBooleanArray) {
        this.mSparseBooleanArray = sparseBooleanArray;
    }

    @Override
    protected void setViewWithCheckBox(View view, int i) {
        if (this.mSparseBooleanArray == null) {
            Log.w("ContactGroupListAdapter", "[setViewWithCheckBox]mSparseBooleanArray is null!");
        } else {
            ((CheckBox) view.findViewById(R.id.checkbox)).setChecked(this.mSparseBooleanArray.get(i));
        }
    }

    @Override
    public long getItemId(int i) {
        GroupListItem item = getItem(i);
        if (item == null) {
            return -1L;
        }
        return item.getGroupId();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}
