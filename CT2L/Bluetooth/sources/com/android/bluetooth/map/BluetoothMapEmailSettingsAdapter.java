package com.android.bluetooth.map;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.android.bluetooth.R;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class BluetoothMapEmailSettingsAdapter extends BaseExpandableListAdapter {
    private static final boolean D = true;
    private static final String TAG = "BluetoothMapEmailSettingsAdapter";
    private static final boolean V = false;
    public Activity mActivity;
    private int[] mGroupStatus;
    public LayoutInflater mInflater;
    private ArrayList<BluetoothMapEmailSettingsItem> mMainGroup;
    ArrayList<Boolean> mPositionArray;
    private LinkedHashMap<BluetoothMapEmailSettingsItem, ArrayList<BluetoothMapEmailSettingsItem>> mProupList;
    private boolean mCheckAll = true;
    private int mSlotsLeft = 10;

    public BluetoothMapEmailSettingsAdapter(Activity act, ExpandableListView listView, LinkedHashMap<BluetoothMapEmailSettingsItem, ArrayList<BluetoothMapEmailSettingsItem>> groupsList, int enabledAccountsCounts) {
        this.mActivity = act;
        this.mProupList = groupsList;
        this.mInflater = act.getLayoutInflater();
        this.mGroupStatus = new int[groupsList.size()];
        this.mSlotsLeft -= enabledAccountsCounts;
        listView.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
            @Override
            public void onGroupExpand(int groupPosition) {
                BluetoothMapEmailSettingsItem group = (BluetoothMapEmailSettingsItem) BluetoothMapEmailSettingsAdapter.this.mMainGroup.get(groupPosition);
                if (((ArrayList) BluetoothMapEmailSettingsAdapter.this.mProupList.get(group)).size() > 0) {
                    BluetoothMapEmailSettingsAdapter.this.mGroupStatus[groupPosition] = 1;
                }
            }
        });
        this.mMainGroup = new ArrayList<>();
        for (Map.Entry<BluetoothMapEmailSettingsItem, ArrayList<BluetoothMapEmailSettingsItem>> mapEntry : this.mProupList.entrySet()) {
            this.mMainGroup.add(mapEntry.getKey());
        }
    }

    @Override
    public BluetoothMapEmailSettingsItem getChild(int groupPosition, int childPosition) {
        BluetoothMapEmailSettingsItem item = this.mMainGroup.get(groupPosition);
        return this.mProupList.get(item).get(childPosition);
    }

    private ArrayList<BluetoothMapEmailSettingsItem> getChild(BluetoothMapEmailSettingsItem group) {
        return this.mProupList.get(group);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return 0L;
    }

    @Override
    public View getChildView(final int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        ChildHolder holder;
        if (convertView == null) {
            convertView = this.mInflater.inflate(R.layout.bluetooth_map_email_settings_account_item, (ViewGroup) null);
            holder = new ChildHolder();
            holder.cb = (CheckBox) convertView.findViewById(R.id.bluetooth_map_email_settings_item_check);
            holder.title = (TextView) convertView.findViewById(R.id.bluetooth_map_email_settings_item_text_view);
            convertView.setTag(holder);
        } else {
            holder = (ChildHolder) convertView.getTag();
        }
        final BluetoothMapEmailSettingsItem child = getChild(groupPosition, childPosition);
        holder.cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                BluetoothMapEmailSettingsItem parentGroup = BluetoothMapEmailSettingsAdapter.this.getGroup(groupPosition);
                boolean oldIsChecked = child.mIsChecked;
                child.mIsChecked = isChecked;
                if (isChecked) {
                    ArrayList<BluetoothMapEmailSettingsItem> childList = BluetoothMapEmailSettingsAdapter.this.getChild(parentGroup);
                    int childIndex = childList.indexOf(child);
                    boolean isAllChildClicked = true;
                    if (BluetoothMapEmailSettingsAdapter.this.mSlotsLeft - childList.size() < 0) {
                        BluetoothMapEmailSettingsAdapter.this.showWarning(BluetoothMapEmailSettingsAdapter.this.mActivity.getString(R.string.bluetooth_map_email_settings_no_account_slots_left));
                        isAllChildClicked = false;
                        child.mIsChecked = false;
                    } else {
                        int i = 0;
                        while (true) {
                            if (i >= childList.size()) {
                                break;
                            }
                            if (i != childIndex) {
                                BluetoothMapEmailSettingsItem siblings = childList.get(i);
                                if (!siblings.mIsChecked) {
                                    isAllChildClicked = false;
                                    BluetoothMapEmailSettingsDataHolder.mCheckedChilds.put(child.getName(), parentGroup.getName());
                                    break;
                                }
                            }
                            i++;
                        }
                    }
                    if (isAllChildClicked) {
                        parentGroup.mIsChecked = true;
                        if (!BluetoothMapEmailSettingsDataHolder.mCheckedChilds.containsKey(child.getName())) {
                            BluetoothMapEmailSettingsDataHolder.mCheckedChilds.put(child.getName(), parentGroup.getName());
                        }
                        BluetoothMapEmailSettingsAdapter.this.mCheckAll = false;
                    }
                } else if (!parentGroup.mIsChecked) {
                    BluetoothMapEmailSettingsAdapter.this.mCheckAll = true;
                    BluetoothMapEmailSettingsDataHolder.mCheckedChilds.remove(child.getName());
                } else {
                    parentGroup.mIsChecked = false;
                    BluetoothMapEmailSettingsAdapter.this.mCheckAll = false;
                    BluetoothMapEmailSettingsDataHolder.mCheckedChilds.remove(child.getName());
                }
                BluetoothMapEmailSettingsAdapter.this.notifyDataSetChanged();
                if (child.mIsChecked != oldIsChecked) {
                    BluetoothMapEmailSettingsAdapter.this.updateAccount(child);
                }
            }
        });
        holder.cb.setChecked(child.mIsChecked);
        holder.title.setText(child.getName());
        Log.i("childs are", BluetoothMapEmailSettingsDataHolder.mCheckedChilds.toString());
        return convertView;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        BluetoothMapEmailSettingsItem item = this.mMainGroup.get(groupPosition);
        return this.mProupList.get(item).size();
    }

    @Override
    public BluetoothMapEmailSettingsItem getGroup(int groupPosition) {
        return this.mMainGroup.get(groupPosition);
    }

    @Override
    public int getGroupCount() {
        return this.mMainGroup.size();
    }

    @Override
    public void onGroupCollapsed(int groupPosition) {
        super.onGroupCollapsed(groupPosition);
    }

    @Override
    public void onGroupExpanded(int groupPosition) {
        super.onGroupExpanded(groupPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return 0L;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        GroupHolder holder;
        if (convertView == null) {
            convertView = this.mInflater.inflate(R.layout.bluetooth_map_email_settings_account_group, (ViewGroup) null);
            holder = new GroupHolder();
            holder.cb = (CheckBox) convertView.findViewById(R.id.bluetooth_map_email_settings_group_checkbox);
            holder.imageView = (ImageView) convertView.findViewById(R.id.bluetooth_map_email_settings_group_icon);
            holder.title = (TextView) convertView.findViewById(R.id.bluetooth_map_email_settings_group_text_view);
            convertView.setTag(holder);
        } else {
            holder = (GroupHolder) convertView.getTag();
        }
        final BluetoothMapEmailSettingsItem groupItem = getGroup(groupPosition);
        holder.imageView.setImageDrawable(groupItem.getIcon());
        holder.title.setText(groupItem.getName());
        holder.cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (BluetoothMapEmailSettingsAdapter.this.mCheckAll) {
                    ArrayList<BluetoothMapEmailSettingsItem> childItem = BluetoothMapEmailSettingsAdapter.this.getChild(groupItem);
                    for (BluetoothMapEmailSettingsItem children : childItem) {
                        boolean oldIsChecked = children.mIsChecked;
                        if (BluetoothMapEmailSettingsAdapter.this.mSlotsLeft <= 0) {
                            BluetoothMapEmailSettingsAdapter.this.showWarning(BluetoothMapEmailSettingsAdapter.this.mActivity.getString(R.string.bluetooth_map_email_settings_no_account_slots_left));
                            isChecked = false;
                        } else {
                            children.mIsChecked = isChecked;
                            if (oldIsChecked != children.mIsChecked) {
                                BluetoothMapEmailSettingsAdapter.this.updateAccount(children);
                            }
                        }
                    }
                }
                groupItem.mIsChecked = isChecked;
                BluetoothMapEmailSettingsAdapter.this.notifyDataSetChanged();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!BluetoothMapEmailSettingsAdapter.this.mCheckAll) {
                            BluetoothMapEmailSettingsAdapter.this.mCheckAll = true;
                        }
                    }
                }, 50L);
            }
        });
        holder.cb.setChecked(groupItem.mIsChecked);
        return convertView;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    private class GroupHolder {
        public CheckBox cb;
        public ImageView imageView;
        public TextView title;

        private GroupHolder() {
        }
    }

    private class ChildHolder {
        public CheckBox cb;
        public TextView title;

        private ChildHolder() {
        }
    }

    public void updateAccount(BluetoothMapEmailSettingsItem account) {
        updateSlotCounter(account.mIsChecked);
        Log.d(TAG, "Updating account settings for " + account.getName() + ". Value is:" + account.mIsChecked);
        ContentResolver mResolver = this.mActivity.getContentResolver();
        Uri uri = Uri.parse(account.mBase_uri_no_account + "/Account");
        ContentValues values = new ContentValues();
        values.put("flag_expose", Integer.valueOf(account.mIsChecked ? 1 : 0));
        values.put("_id", account.getId());
        mResolver.update(uri, values, null, null);
    }

    private void updateSlotCounter(boolean isChecked) {
        CharSequence text;
        if (isChecked) {
            this.mSlotsLeft--;
        } else {
            this.mSlotsLeft++;
        }
        if (this.mSlotsLeft <= 0) {
            text = this.mActivity.getString(R.string.bluetooth_map_email_settings_no_account_slots_left);
        } else {
            text = this.mActivity.getString(R.string.bluetooth_map_email_settings_count) + " " + String.valueOf(this.mSlotsLeft);
        }
        Toast toast = Toast.makeText(this.mActivity, text, 0);
        toast.show();
    }

    private void showWarning(String text) {
        Toast toast = Toast.makeText(this.mActivity, text, 0);
        toast.show();
    }
}
