package com.android.settings.deviceinfo;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.os.Bundle;
import android.os.storage.StorageVolume;
import android.text.format.Formatter;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.ListView;
import com.android.settings.R;
import com.android.settings.deviceinfo.StorageMeasurement;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MiscFilesHandler extends ListActivity {
    private MemoryMearurementAdapter mAdapter;
    private LayoutInflater mInflater;
    private String mNumBytesSelectedFormat;
    private String mNumSelectedFormat;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setFinishOnTouchOutside(true);
        setTitle(R.string.misc_files);
        this.mNumSelectedFormat = getString(R.string.misc_files_selected_count);
        this.mNumBytesSelectedFormat = getString(R.string.misc_files_selected_count_bytes);
        this.mAdapter = new MemoryMearurementAdapter(this);
        this.mInflater = (LayoutInflater) getSystemService("layout_inflater");
        setContentView(R.layout.settings_storage_miscfiles_list);
        ListView lv = getListView();
        lv.setItemsCanFocus(true);
        lv.setChoiceMode(3);
        lv.setMultiChoiceModeListener(new ModeCallback(this));
        setListAdapter(this.mAdapter);
    }

    private class ModeCallback implements AbsListView.MultiChoiceModeListener {
        private final Context mContext;
        private int mDataCount;

        public ModeCallback(Context context) {
            this.mContext = context;
            this.mDataCount = MiscFilesHandler.this.mAdapter.getCount();
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = MiscFilesHandler.this.getMenuInflater();
            inflater.inflate(R.menu.misc_files_menu, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            ListView lv = MiscFilesHandler.this.getListView();
            switch (item.getItemId()) {
                case R.id.action_delete:
                    SparseBooleanArray checkedItems = lv.getCheckedItemPositions();
                    int checkedCount = MiscFilesHandler.this.getListView().getCheckedItemCount();
                    if (checkedCount > this.mDataCount) {
                        throw new IllegalStateException("checked item counts do not match. checkedCount: " + checkedCount + ", dataSize: " + this.mDataCount);
                    }
                    if (this.mDataCount > 0) {
                        ArrayList<Object> toRemove = new ArrayList<>();
                        for (int i = 0; i < this.mDataCount; i++) {
                            if (checkedItems.get(i)) {
                                if (StorageMeasurement.LOGV) {
                                    Log.i("MemorySettings", "deleting: " + MiscFilesHandler.this.mAdapter.getItem(i));
                                }
                                File file = new File(MiscFilesHandler.this.mAdapter.getItem(i).mFileName);
                                if (file.isDirectory()) {
                                    deleteDir(file);
                                } else {
                                    file.delete();
                                }
                                toRemove.add(MiscFilesHandler.this.mAdapter.getItem(i));
                            }
                        }
                        MiscFilesHandler.this.mAdapter.removeAll(toRemove);
                        MiscFilesHandler.this.mAdapter.notifyDataSetChanged();
                        this.mDataCount = MiscFilesHandler.this.mAdapter.getCount();
                    }
                    mode.finish();
                    return true;
                case R.id.action_select_all:
                    for (int i2 = 0; i2 < this.mDataCount; i2++) {
                        lv.setItemChecked(i2, true);
                    }
                    onItemCheckedStateChanged(mode, 1, 0L, true);
                    return true;
                default:
                    return true;
            }
        }

        private boolean deleteDir(File dir) {
            String[] children = dir.list();
            if (children != null) {
                for (String str : children) {
                    boolean success = deleteDir(new File(dir, str));
                    if (!success) {
                        return false;
                    }
                }
            }
            return dir.delete();
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
        }

        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
            ListView lv = MiscFilesHandler.this.getListView();
            int numChecked = lv.getCheckedItemCount();
            mode.setTitle(String.format(MiscFilesHandler.this.mNumSelectedFormat, Integer.valueOf(numChecked), Integer.valueOf(MiscFilesHandler.this.mAdapter.getCount())));
            SparseBooleanArray checkedItems = lv.getCheckedItemPositions();
            long selectedDataSize = 0;
            if (numChecked > 0) {
                for (int i = 0; i < this.mDataCount; i++) {
                    if (checkedItems.get(i)) {
                        selectedDataSize += MiscFilesHandler.this.mAdapter.getItem(i).mSize;
                    }
                }
            }
            mode.setSubtitle(String.format(MiscFilesHandler.this.mNumBytesSelectedFormat, Formatter.formatFileSize(this.mContext, selectedDataSize), Formatter.formatFileSize(this.mContext, MiscFilesHandler.this.mAdapter.getDataSize())));
        }
    }

    class MemoryMearurementAdapter extends BaseAdapter {
        private Context mContext;
        private ArrayList<StorageMeasurement.FileInfo> mData;
        private long mDataSize = 0;

        public MemoryMearurementAdapter(Activity activity) {
            this.mData = null;
            this.mContext = activity;
            StorageVolume storageVolume = (StorageVolume) activity.getIntent().getParcelableExtra("storage_volume");
            StorageMeasurement mMeasurement = StorageMeasurement.getInstance(activity, storageVolume);
            if (mMeasurement != null) {
                this.mData = (ArrayList) mMeasurement.mFileInfoForMisc;
                if (this.mData != null) {
                    for (StorageMeasurement.FileInfo info : this.mData) {
                        this.mDataSize += info.mSize;
                    }
                }
            }
        }

        @Override
        public int getCount() {
            if (this.mData == null) {
                return 0;
            }
            return this.mData.size();
        }

        @Override
        public StorageMeasurement.FileInfo getItem(int position) {
            if (this.mData == null || this.mData.size() <= position) {
                return null;
            }
            return this.mData.get(position);
        }

        @Override
        public long getItemId(int position) {
            if (this.mData == null || this.mData.size() <= position) {
                return 0L;
            }
            return this.mData.get(position).mId;
        }

        public void removeAll(List<Object> objs) {
            if (this.mData != null) {
                for (Object o : objs) {
                    this.mData.remove(o);
                    this.mDataSize -= ((StorageMeasurement.FileInfo) o).mSize;
                }
            }
        }

        public long getDataSize() {
            return this.mDataSize;
        }

        @Override
        public void notifyDataSetChanged() {
            super.notifyDataSetChanged();
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            final FileItemInfoLayout view;
            if (convertView == null) {
                view = (FileItemInfoLayout) MiscFilesHandler.this.mInflater.inflate(R.layout.settings_storage_miscfiles, parent, false);
            } else {
                view = (FileItemInfoLayout) convertView;
            }
            StorageMeasurement.FileInfo item = getItem(position);
            view.setFileName(item.mFileName);
            view.setFileSize(Formatter.formatFileSize(this.mContext, item.mSize));
            final ListView listView = (ListView) parent;
            view.getCheckBox().setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    listView.setItemChecked(position, isChecked);
                }
            });
            view.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (listView.getCheckedItemCount() > 0) {
                        return false;
                    }
                    listView.setItemChecked(position, view.isChecked() ? false : true);
                    return true;
                }
            });
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listView.getCheckedItemCount() > 0) {
                        listView.setItemChecked(position, !view.isChecked());
                    }
                }
            });
            return view;
        }
    }
}
