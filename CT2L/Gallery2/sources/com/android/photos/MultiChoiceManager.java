package com.android.photos;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.AbsListView;
import android.widget.ShareActionProvider;
import com.android.gallery3d.R;
import com.android.gallery3d.app.TrimVideo;
import com.android.gallery3d.filtershow.FilterShowActivity;
import com.android.photos.SelectionManager;
import java.util.ArrayList;
import java.util.List;

public class MultiChoiceManager implements AbsListView.MultiChoiceModeListener, ShareActionProvider.OnShareTargetSelectedListener, SelectionManager.SelectedUriSource {
    private ActionMode mActionMode;
    private Context mContext;
    private Delegate mDelegate;
    private ArrayList<Uri> mSelectedShareableUrisArray = new ArrayList<>();
    private SelectionManager mSelectionManager;
    private ShareActionProvider mShareActionProvider;

    public interface Delegate {
        void deleteItemWithPath(Object obj);

        Object getItemAtPosition(int i);

        int getItemMediaType(Object obj);

        int getItemSupportedOperations(Object obj);

        Uri getItemUri(Object obj);

        Object getPathForItemAtPosition(int i);

        int getSelectedItemCount();

        SparseBooleanArray getSelectedItemPositions();

        ArrayList<Uri> getSubItemUrisForItem(Object obj);
    }

    public interface Provider {
        MultiChoiceManager getMultiChoiceManager();
    }

    public MultiChoiceManager(Activity activity) {
        this.mContext = activity;
        this.mSelectionManager = new SelectionManager(activity);
    }

    public void setDelegate(Delegate delegate) {
        if (this.mDelegate != delegate) {
            if (this.mActionMode != null) {
                this.mActionMode.finish();
            }
            this.mDelegate = delegate;
        }
    }

    @Override
    public ArrayList<Uri> getSelectedShareableUris() {
        return this.mSelectedShareableUrisArray;
    }

    private void updateSelectedTitle(ActionMode mode) {
        int count = this.mDelegate.getSelectedItemCount();
        mode.setTitle(this.mContext.getResources().getQuantityString(R.plurals.number_of_items_selected, count, Integer.valueOf(count)));
    }

    private String getItemMimetype(Object item) {
        int type = this.mDelegate.getItemMediaType(item);
        if (type == 1) {
            return "image/*";
        }
        if (type == 3) {
            return "video/*";
        }
        return "*/*";
    }

    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
        updateSelectedTitle(mode);
        Object item = this.mDelegate.getItemAtPosition(position);
        int supported = this.mDelegate.getItemSupportedOperations(item);
        if ((supported & 4) > 0) {
            ArrayList<Uri> subItems = this.mDelegate.getSubItemUrisForItem(item);
            if (checked) {
                this.mSelectedShareableUrisArray.addAll(subItems);
            } else {
                this.mSelectedShareableUrisArray.removeAll(subItems);
            }
        }
        this.mSelectionManager.onItemSelectedStateChanged(this.mShareActionProvider, this.mDelegate.getItemMediaType(item), supported, checked);
        updateActionItemVisibilities(mode.getMenu(), this.mSelectionManager.getSupportedOperations());
    }

    private void updateActionItemVisibilities(Menu menu, int supportedOperations) {
        MenuItem editItem = menu.findItem(R.id.menu_edit);
        MenuItem deleteItem = menu.findItem(R.id.menu_delete);
        MenuItem shareItem = menu.findItem(R.id.menu_share);
        MenuItem cropItem = menu.findItem(R.id.menu_crop);
        MenuItem trimItem = menu.findItem(R.id.menu_trim);
        MenuItem muteItem = menu.findItem(R.id.menu_mute);
        MenuItem setAsItem = menu.findItem(R.id.menu_set_as);
        editItem.setVisible((supportedOperations & NotificationCompat.FLAG_GROUP_SUMMARY) > 0);
        deleteItem.setVisible((supportedOperations & 1) > 0);
        shareItem.setVisible((supportedOperations & 4) > 0);
        cropItem.setVisible((supportedOperations & 8) > 0);
        trimItem.setVisible((supportedOperations & 2048) > 0);
        muteItem.setVisible((65536 & supportedOperations) > 0);
        setAsItem.setVisible((supportedOperations & 32) > 0);
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        this.mSelectionManager.setSelectedUriSource(this);
        this.mActionMode = mode;
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.gallery_multiselect, menu);
        MenuItem menuItem = menu.findItem(R.id.menu_share);
        this.mShareActionProvider = (ShareActionProvider) menuItem.getActionProvider();
        this.mShareActionProvider.setOnShareTargetSelectedListener(this);
        updateSelectedTitle(mode);
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        this.mSelectedShareableUrisArray = new ArrayList<>();
        this.mSelectionManager.onClearSelection();
        this.mSelectionManager.setSelectedUriSource(null);
        this.mShareActionProvider = null;
        this.mActionMode = null;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        updateSelectedTitle(mode);
        return false;
    }

    @Override
    public boolean onShareTargetSelected(ShareActionProvider provider, Intent intent) {
        this.mActionMode.finish();
        return false;
    }

    private static class BulkDeleteTask extends AsyncTask<Void, Void, Void> {
        private Delegate mDelegate;
        private List<Object> mPaths;

        public BulkDeleteTask(Delegate delegate, List<Object> paths) {
            this.mDelegate = delegate;
            this.mPaths = paths;
        }

        @Override
        protected Void doInBackground(Void... ignored) {
            for (Object path : this.mPaths) {
                this.mDelegate.deleteItemWithPath(path);
            }
            return null;
        }
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        int actionItemId = item.getItemId();
        switch (actionItemId) {
            case R.id.menu_edit:
            case R.id.menu_crop:
            case R.id.menu_trim:
            case R.id.menu_mute:
            case R.id.menu_set_as:
                singleItemAction(getSelectedItem(), actionItemId);
                mode.finish();
                return true;
            case R.id.menu_delete:
                BulkDeleteTask deleteTask = new BulkDeleteTask(this.mDelegate, getPathsForSelectedItems());
                deleteTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
                mode.finish();
                return true;
            default:
                return false;
        }
    }

    private void singleItemAction(Object item, int actionItemId) {
        Intent intent = new Intent();
        String mime = getItemMimetype(item);
        Uri uri = this.mDelegate.getItemUri(item);
        switch (actionItemId) {
            case R.id.menu_edit:
                intent.setDataAndType(uri, mime).setFlags(1).setAction("android.intent.action.EDIT");
                this.mContext.startActivity(Intent.createChooser(intent, null));
                break;
            case R.id.menu_crop:
                intent.setDataAndType(uri, mime).setFlags(1).setAction("com.android.camera.action.CROP").setClass(this.mContext, FilterShowActivity.class);
                this.mContext.startActivity(intent);
                break;
            case R.id.menu_trim:
                intent.setData(uri).setClass(this.mContext, TrimVideo.class);
                this.mContext.startActivity(intent);
                break;
            case R.id.menu_set_as:
                intent.setDataAndType(uri, mime).setFlags(1).setAction("android.intent.action.ATTACH_DATA").putExtra("mimeType", mime);
                this.mContext.startActivity(Intent.createChooser(intent, this.mContext.getString(R.string.set_as)));
                break;
        }
    }

    private List<Object> getPathsForSelectedItems() {
        List<Object> paths = new ArrayList<>();
        SparseBooleanArray selected = this.mDelegate.getSelectedItemPositions();
        for (int i = 0; i < selected.size(); i++) {
            if (selected.valueAt(i)) {
                paths.add(this.mDelegate.getPathForItemAtPosition(i));
            }
        }
        return paths;
    }

    public Object getSelectedItem() {
        if (this.mDelegate.getSelectedItemCount() != 1) {
            return null;
        }
        SparseBooleanArray selected = this.mDelegate.getSelectedItemPositions();
        for (int i = 0; i < selected.size(); i++) {
            if (selected.valueAt(i)) {
                return this.mDelegate.getItemAtPosition(selected.keyAt(i));
            }
        }
        return null;
    }
}
