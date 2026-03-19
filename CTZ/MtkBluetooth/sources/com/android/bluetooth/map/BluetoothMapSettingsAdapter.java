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
import com.android.bluetooth.mapapi.BluetoothMapContract;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class BluetoothMapSettingsAdapter extends BaseExpandableListAdapter {
    private static final boolean D = BluetoothMapService.DEBUG;
    private static final String TAG = "BluetoothMapSettingsAdapter";
    private static final boolean V = false;
    public Activity mActivity;
    private int[] mGroupStatus;
    public LayoutInflater mInflater;
    private ArrayList<BluetoothMapAccountItem> mMainGroup;
    ArrayList<Boolean> mPositionArray;
    private LinkedHashMap<BluetoothMapAccountItem, ArrayList<BluetoothMapAccountItem>> mProupList;
    private boolean mCheckAll = true;
    private int mSlotsLeft = 10;

    public BluetoothMapSettingsAdapter(Activity activity, ExpandableListView expandableListView, LinkedHashMap<BluetoothMapAccountItem, ArrayList<BluetoothMapAccountItem>> linkedHashMap, int i) {
        this.mActivity = activity;
        this.mProupList = linkedHashMap;
        this.mInflater = activity.getLayoutInflater();
        this.mGroupStatus = new int[linkedHashMap.size()];
        this.mSlotsLeft -= i;
        expandableListView.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
            @Override
            public void onGroupExpand(int i2) {
                if (((ArrayList) BluetoothMapSettingsAdapter.this.mProupList.get((BluetoothMapAccountItem) BluetoothMapSettingsAdapter.this.mMainGroup.get(i2))).size() > 0) {
                    BluetoothMapSettingsAdapter.this.mGroupStatus[i2] = 1;
                }
            }
        });
        this.mMainGroup = new ArrayList<>();
        Iterator<Map.Entry<BluetoothMapAccountItem, ArrayList<BluetoothMapAccountItem>>> it = this.mProupList.entrySet().iterator();
        while (it.hasNext()) {
            this.mMainGroup.add(it.next().getKey());
        }
    }

    @Override
    public BluetoothMapAccountItem getChild(int i, int i2) {
        return this.mProupList.get(this.mMainGroup.get(i)).get(i2);
    }

    private ArrayList<BluetoothMapAccountItem> getChild(BluetoothMapAccountItem bluetoothMapAccountItem) {
        return this.mProupList.get(bluetoothMapAccountItem);
    }

    @Override
    public long getChildId(int i, int i2) {
        return 0L;
    }

    @Override
    public View getChildView(final int i, int i2, boolean z, View view, ViewGroup viewGroup) {
        ChildHolder childHolder;
        if (view == null) {
            view = this.mInflater.inflate(R.layout.bluetooth_map_settings_account_item, (ViewGroup) null);
            childHolder = new ChildHolder();
            childHolder.cb = (CheckBox) view.findViewById(R.id.bluetooth_map_settings_item_check);
            childHolder.title = (TextView) view.findViewById(R.id.bluetooth_map_settings_item_text_view);
            view.setTag(childHolder);
        } else {
            childHolder = (ChildHolder) view.getTag();
        }
        final BluetoothMapAccountItem child = getChild(i, i2);
        childHolder.cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean z2) {
                boolean z3;
                BluetoothMapAccountItem group = BluetoothMapSettingsAdapter.this.getGroup(i);
                boolean z4 = child.mIsChecked;
                child.mIsChecked = z2;
                if (z2) {
                    ArrayList child2 = BluetoothMapSettingsAdapter.this.getChild(group);
                    int iIndexOf = child2.indexOf(child);
                    if (BluetoothMapSettingsAdapter.this.mSlotsLeft - child2.size() < 0) {
                        BluetoothMapSettingsAdapter.this.showWarning(BluetoothMapSettingsAdapter.this.mActivity.getString(R.string.bluetooth_map_settings_no_account_slots_left));
                        child.mIsChecked = false;
                    } else {
                        for (int i3 = 0; i3 < child2.size(); i3++) {
                            if (i3 != iIndexOf && !((BluetoothMapAccountItem) child2.get(i3)).mIsChecked) {
                                BluetoothMapSettingsDataHolder.sCheckedChilds.put(child.getName(), group.getName());
                            }
                        }
                        z3 = true;
                        if (z3) {
                            group.mIsChecked = true;
                            if (!BluetoothMapSettingsDataHolder.sCheckedChilds.containsKey(child.getName())) {
                                BluetoothMapSettingsDataHolder.sCheckedChilds.put(child.getName(), group.getName());
                            }
                            BluetoothMapSettingsAdapter.this.mCheckAll = false;
                        }
                    }
                    z3 = false;
                    if (z3) {
                    }
                } else if (!group.mIsChecked) {
                    BluetoothMapSettingsAdapter.this.mCheckAll = true;
                    BluetoothMapSettingsDataHolder.sCheckedChilds.remove(child.getName());
                } else {
                    group.mIsChecked = false;
                    BluetoothMapSettingsAdapter.this.mCheckAll = false;
                    BluetoothMapSettingsDataHolder.sCheckedChilds.remove(child.getName());
                }
                BluetoothMapSettingsAdapter.this.notifyDataSetChanged();
                if (child.mIsChecked != z4) {
                    BluetoothMapSettingsAdapter.this.updateAccount(child);
                }
            }
        });
        childHolder.cb.setChecked(child.mIsChecked);
        childHolder.title.setText(child.getName());
        if (D) {
            Log.i("childs are", BluetoothMapSettingsDataHolder.sCheckedChilds.toString());
        }
        return view;
    }

    @Override
    public int getChildrenCount(int i) {
        return this.mProupList.get(this.mMainGroup.get(i)).size();
    }

    @Override
    public BluetoothMapAccountItem getGroup(int i) {
        return this.mMainGroup.get(i);
    }

    @Override
    public int getGroupCount() {
        return this.mMainGroup.size();
    }

    @Override
    public void onGroupCollapsed(int i) {
        super.onGroupCollapsed(i);
    }

    @Override
    public void onGroupExpanded(int i) {
        super.onGroupExpanded(i);
    }

    @Override
    public long getGroupId(int i) {
        return 0L;
    }

    @Override
    public View getGroupView(int i, boolean z, View view, ViewGroup viewGroup) {
        GroupHolder groupHolder;
        if (view == null) {
            view = this.mInflater.inflate(R.layout.bluetooth_map_settings_account_group, (ViewGroup) null);
            groupHolder = new GroupHolder();
            groupHolder.cb = (CheckBox) view.findViewById(R.id.bluetooth_map_settings_group_checkbox);
            groupHolder.imageView = (ImageView) view.findViewById(R.id.bluetooth_map_settings_group_icon);
            groupHolder.title = (TextView) view.findViewById(R.id.bluetooth_map_settings_group_text_view);
            view.setTag(groupHolder);
        } else {
            groupHolder = (GroupHolder) view.getTag();
        }
        final BluetoothMapAccountItem group = getGroup(i);
        groupHolder.imageView.setImageDrawable(group.getIcon());
        groupHolder.title.setText(group.getName());
        groupHolder.cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean z2) {
                if (BluetoothMapSettingsAdapter.this.mCheckAll) {
                    for (BluetoothMapAccountItem bluetoothMapAccountItem : BluetoothMapSettingsAdapter.this.getChild(group)) {
                        boolean z3 = bluetoothMapAccountItem.mIsChecked;
                        if (BluetoothMapSettingsAdapter.this.mSlotsLeft <= 0) {
                            BluetoothMapSettingsAdapter.this.showWarning(BluetoothMapSettingsAdapter.this.mActivity.getString(R.string.bluetooth_map_settings_no_account_slots_left));
                            z2 = false;
                        } else {
                            bluetoothMapAccountItem.mIsChecked = z2;
                            if (z3 != bluetoothMapAccountItem.mIsChecked) {
                                BluetoothMapSettingsAdapter.this.updateAccount(bluetoothMapAccountItem);
                            }
                        }
                    }
                }
                group.mIsChecked = z2;
                BluetoothMapSettingsAdapter.this.notifyDataSetChanged();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!BluetoothMapSettingsAdapter.this.mCheckAll) {
                            BluetoothMapSettingsAdapter.this.mCheckAll = true;
                        }
                    }
                }, 50L);
            }
        });
        groupHolder.cb.setChecked(group.mIsChecked);
        return view;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public boolean isChildSelectable(int i, int i2) {
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

    public void updateAccount(BluetoothMapAccountItem bluetoothMapAccountItem) {
        updateSlotCounter(bluetoothMapAccountItem.mIsChecked);
        if (D) {
            Log.d(TAG, "Updating account settings for " + bluetoothMapAccountItem.getName() + ". Value is:" + bluetoothMapAccountItem.mIsChecked);
        }
        ContentResolver contentResolver = this.mActivity.getContentResolver();
        Uri uri = Uri.parse(bluetoothMapAccountItem.mBase_uri_no_account + "/" + BluetoothMapContract.TABLE_ACCOUNT);
        ContentValues contentValues = new ContentValues();
        contentValues.put(BluetoothMapContract.AccountColumns.FLAG_EXPOSE, Integer.valueOf(bluetoothMapAccountItem.mIsChecked ? 1 : 0));
        contentValues.put("_id", bluetoothMapAccountItem.getId());
        contentResolver.update(uri, contentValues, null, null);
    }

    private void updateSlotCounter(boolean z) {
        String string;
        if (z) {
            this.mSlotsLeft--;
        } else {
            this.mSlotsLeft++;
        }
        if (this.mSlotsLeft <= 0) {
            string = this.mActivity.getString(R.string.bluetooth_map_settings_no_account_slots_left);
        } else {
            string = this.mActivity.getString(R.string.bluetooth_map_settings_count) + " " + String.valueOf(this.mSlotsLeft);
        }
        Toast.makeText(this.mActivity, string, 0).show();
    }

    private void showWarning(String str) {
        Toast.makeText(this.mActivity, str, 0).show();
    }
}
