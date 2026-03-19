package com.android.photos;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.BenesseExtension;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AbsListView;
import android.widget.ShareActionProvider;
import com.android.gallery3d.R;
import com.android.gallery3d.app.TrimVideo;
import com.android.gallery3d.filtershow.FilterShowActivity;
import com.android.photos.SelectionManager;
import java.util.ArrayList;
import java.util.Iterator;
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
        if (this.mDelegate == delegate) {
            return;
        }
        if (this.mActionMode != null) {
            this.mActionMode.finish();
        }
        this.mDelegate = delegate;
    }

    @Override
    public ArrayList<Uri> getSelectedShareableUris() {
        return this.mSelectedShareableUrisArray;
    }

    private void updateSelectedTitle(ActionMode actionMode) {
        int selectedItemCount = this.mDelegate.getSelectedItemCount();
        actionMode.setTitle(this.mContext.getResources().getQuantityString(R.plurals.number_of_items_selected, selectedItemCount, Integer.valueOf(selectedItemCount)));
    }

    private String getItemMimetype(Object obj) {
        int itemMediaType = this.mDelegate.getItemMediaType(obj);
        if (itemMediaType == 1) {
            return "image/*";
        }
        if (itemMediaType == 3) {
            return "video/*";
        }
        return "*/*";
    }

    @Override
    public void onItemCheckedStateChanged(ActionMode actionMode, int i, long j, boolean z) {
        updateSelectedTitle(actionMode);
        Object itemAtPosition = this.mDelegate.getItemAtPosition(i);
        int itemSupportedOperations = this.mDelegate.getItemSupportedOperations(itemAtPosition);
        if ((itemSupportedOperations & 4) > 0) {
            ArrayList<Uri> subItemUrisForItem = this.mDelegate.getSubItemUrisForItem(itemAtPosition);
            if (z) {
                this.mSelectedShareableUrisArray.addAll(subItemUrisForItem);
            } else {
                this.mSelectedShareableUrisArray.removeAll(subItemUrisForItem);
            }
        }
        this.mSelectionManager.onItemSelectedStateChanged(this.mShareActionProvider, this.mDelegate.getItemMediaType(itemAtPosition), itemSupportedOperations, z);
        updateActionItemVisibilities(actionMode.getMenu(), this.mSelectionManager.getSupportedOperations());
    }

    private void updateActionItemVisibilities(Menu menu, int i) {
        MenuItem menuItemFindItem = menu.findItem(R.id.menu_edit);
        MenuItem menuItemFindItem2 = menu.findItem(R.id.menu_delete);
        MenuItem menuItemFindItem3 = menu.findItem(R.id.menu_share);
        MenuItem menuItemFindItem4 = menu.findItem(R.id.menu_crop);
        MenuItem menuItemFindItem5 = menu.findItem(R.id.menu_trim);
        MenuItem menuItemFindItem6 = menu.findItem(R.id.menu_mute);
        MenuItem menuItemFindItem7 = menu.findItem(R.id.menu_set_as);
        menuItemFindItem.setVisible((i & 512) > 0);
        menuItemFindItem2.setVisible((i & 1) > 0);
        menuItemFindItem3.setVisible((i & 4) > 0);
        menuItemFindItem4.setVisible((i & 8) > 0);
        menuItemFindItem5.setVisible((i & 2048) > 0);
        menuItemFindItem6.setVisible((65536 & i) > 0);
        menuItemFindItem7.setVisible((i & 32) > 0);
    }

    @Override
    public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
        this.mSelectionManager.setSelectedUriSource(this);
        this.mActionMode = actionMode;
        actionMode.getMenuInflater().inflate(R.menu.gallery_multiselect, menu);
        this.mShareActionProvider = (ShareActionProvider) menu.findItem(R.id.menu_share).getActionProvider();
        this.mShareActionProvider.setOnShareTargetSelectedListener(this);
        updateSelectedTitle(actionMode);
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode actionMode) {
        this.mSelectedShareableUrisArray = new ArrayList<>();
        this.mSelectionManager.onClearSelection();
        this.mSelectionManager.setSelectedUriSource(null);
        this.mShareActionProvider = null;
        this.mActionMode = null;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
        updateSelectedTitle(actionMode);
        return false;
    }

    @Override
    public boolean onShareTargetSelected(ShareActionProvider shareActionProvider, Intent intent) {
        this.mActionMode.finish();
        return false;
    }

    private static class BulkDeleteTask extends AsyncTask<Void, Void, Void> {
        private Delegate mDelegate;
        private List<Object> mPaths;

        public BulkDeleteTask(Delegate delegate, List<Object> list) {
            this.mDelegate = delegate;
            this.mPaths = list;
        }

        @Override
        protected Void doInBackground(Void... voidArr) {
            Iterator<Object> it = this.mPaths.iterator();
            while (it.hasNext()) {
                this.mDelegate.deleteItemWithPath(it.next());
            }
            return null;
        }
    }

    @Override
    public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        switch (itemId) {
            case R.id.menu_edit:
            case R.id.menu_crop:
            case R.id.menu_trim:
            case R.id.menu_mute:
            case R.id.menu_set_as:
                singleItemAction(getSelectedItem(), itemId);
                actionMode.finish();
                return true;
            case R.id.menu_delete:
                new BulkDeleteTask(this.mDelegate, getPathsForSelectedItems()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
                actionMode.finish();
                return true;
            default:
                return false;
        }
    }

    private void singleItemAction(Object obj, int i) {
        Intent intent = new Intent();
        String itemMimetype = getItemMimetype(obj);
        Uri itemUri = this.mDelegate.getItemUri(obj);
        switch (i) {
            case R.id.menu_edit:
                if (BenesseExtension.getDchaState() == 0) {
                    intent.setDataAndType(itemUri, itemMimetype).setFlags(1).setAction("android.intent.action.EDIT");
                    this.mContext.startActivity(Intent.createChooser(intent, null));
                    break;
                }
                break;
            case R.id.menu_crop:
                intent.setDataAndType(itemUri, itemMimetype).setFlags(1).setAction("com.android.camera.action.CROP").setClass(this.mContext, FilterShowActivity.class);
                this.mContext.startActivity(intent);
                break;
            case R.id.menu_trim:
                intent.setData(itemUri).setClass(this.mContext, TrimVideo.class);
                this.mContext.startActivity(intent);
                break;
            case R.id.menu_set_as:
                intent.setDataAndType(itemUri, itemMimetype).setFlags(1).setAction("android.intent.action.ATTACH_DATA").putExtra("mimeType", itemMimetype);
                this.mContext.startActivity(Intent.createChooser(intent, this.mContext.getString(R.string.set_as)));
                break;
        }
    }

    private List<Object> getPathsForSelectedItems() {
        ArrayList arrayList = new ArrayList();
        SparseBooleanArray selectedItemPositions = this.mDelegate.getSelectedItemPositions();
        for (int i = 0; i < selectedItemPositions.size(); i++) {
            if (selectedItemPositions.valueAt(i)) {
                arrayList.add(this.mDelegate.getPathForItemAtPosition(i));
            }
        }
        return arrayList;
    }

    public Object getSelectedItem() {
        if (this.mDelegate.getSelectedItemCount() != 1) {
            return null;
        }
        SparseBooleanArray selectedItemPositions = this.mDelegate.getSelectedItemPositions();
        for (int i = 0; i < selectedItemPositions.size(); i++) {
            if (selectedItemPositions.valueAt(i)) {
                return this.mDelegate.getItemAtPosition(selectedItemPositions.keyAt(i));
            }
        }
        return null;
    }
}
