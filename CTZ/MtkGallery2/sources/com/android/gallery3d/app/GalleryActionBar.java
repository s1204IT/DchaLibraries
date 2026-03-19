package com.android.gallery3d.app;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ShareActionProvider;
import android.widget.TextView;
import android.widget.TwoLineListItem;
import com.android.gallery3d.R;
import com.mediatek.galleryportable.ActivityChooserModelWrapper;
import com.mediatek.galleryportable.ActivityChooserViewWrapper;
import java.util.ArrayList;

public class GalleryActionBar implements ActionBar.OnNavigationListener {
    private static final ActionItem[] sClusterItems = {new ActionItem(1, true, false, R.string.albums, R.string.group_by_album), new ActionItem(4, true, false, R.string.locations, R.string.location, R.string.group_by_location), new ActionItem(2, true, false, R.string.times, R.string.time, R.string.group_by_time)};
    private ActionBar mActionBar;
    private Menu mActionBarMenu;
    private ArrayList<Integer> mActions;
    private AbstractGalleryActivity mActivity;
    private ActivityChooserViewWrapper mActivityChooserView;
    private AlbumModeAdapter mAlbumModeAdapter;
    private OnAlbumModeSelectedListener mAlbumModeListener;
    private CharSequence[] mAlbumModes;
    private ClusterRunner mClusterRunner;
    private Context mContext;
    private ActivityChooserModelWrapper mDataModel;
    private LayoutInflater mInflater;
    private int mLastAlbumModeSelected;
    ActivityChooserModelWrapper.OnChooseActivityListenerWrapper mOnChooseActivityListener;
    private ShareActionProvider mShareActionProvider;
    private Intent mShareIntent;
    private CharSequence[] mTitles;
    private ClusterAdapter mAdapter = new ClusterAdapter();
    private String mTitleValue = "";
    private int mCurrentIndex = 0;

    public interface ClusterRunner {
        void doCluster(int i);
    }

    public interface OnAlbumModeSelectedListener {
        void onAlbumModeSelected(int i);
    }

    private static class ActionItem {
        public int action;
        public int clusterBy;
        public int dialogTitle;
        public boolean enabled;
        public int spinnerTitle;
        public boolean visible;

        public ActionItem(int i, boolean z, boolean z2, int i2, int i3) {
            this(i, z, z2, i2, i2, i3);
        }

        public ActionItem(int i, boolean z, boolean z2, int i2, int i3, int i4) {
            this.action = i;
            this.enabled = z2;
            this.spinnerTitle = i2;
            this.dialogTitle = i3;
            this.clusterBy = i4;
            this.visible = true;
        }
    }

    private class ClusterAdapter extends BaseAdapter {
        private ClusterAdapter() {
        }

        @Override
        public int getCount() {
            return GalleryActionBar.sClusterItems.length;
        }

        @Override
        public Object getItem(int i) {
            return GalleryActionBar.sClusterItems[i];
        }

        @Override
        public long getItemId(int i) {
            return GalleryActionBar.sClusterItems[i].action;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = GalleryActionBar.this.mInflater.inflate(R.layout.action_bar_text, viewGroup, false);
            }
            ((TextView) view).setText(GalleryActionBar.sClusterItems[i].spinnerTitle);
            return view;
        }
    }

    private class AlbumModeAdapter extends BaseAdapter {
        private AlbumModeAdapter() {
        }

        @Override
        public int getCount() {
            return GalleryActionBar.this.mAlbumModes.length;
        }

        @Override
        public Object getItem(int i) {
            return GalleryActionBar.this.mAlbumModes[i];
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = GalleryActionBar.this.mInflater.inflate(R.layout.action_bar_two_line_text, viewGroup, false);
            }
            TwoLineListItem twoLineListItem = (TwoLineListItem) view;
            twoLineListItem.getText1().setText(GalleryActionBar.this.mTitleValue);
            twoLineListItem.getText2().setText((CharSequence) getItem(i));
            return view;
        }

        @Override
        public View getDropDownView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = GalleryActionBar.this.mInflater.inflate(R.layout.action_bar_text, viewGroup, false);
            }
            ((TextView) view).setText((CharSequence) getItem(i));
            return view;
        }
    }

    public static String getClusterByTypeString(Context context, int i) {
        for (ActionItem actionItem : sClusterItems) {
            if (actionItem.action == i) {
                return context.getString(actionItem.clusterBy);
            }
        }
        return null;
    }

    public GalleryActionBar(AbstractGalleryActivity abstractGalleryActivity) {
        this.mActionBar = abstractGalleryActivity.getActionBar();
        this.mContext = abstractGalleryActivity.getAndroidContext();
        this.mActivity = abstractGalleryActivity;
        this.mInflater = this.mActivity.getLayoutInflater();
    }

    private void createDialogData() {
        ArrayList arrayList = new ArrayList();
        this.mActions = new ArrayList<>();
        for (ActionItem actionItem : sClusterItems) {
            if (actionItem.enabled && actionItem.visible) {
                arrayList.add(this.mContext.getString(actionItem.dialogTitle));
                this.mActions.add(Integer.valueOf(actionItem.action));
            }
        }
        this.mTitles = new CharSequence[arrayList.size()];
        arrayList.toArray(this.mTitles);
    }

    public int getHeight() {
        if (this.mActionBar != null) {
            return this.mActionBar.getHeight();
        }
        return 0;
    }

    public void setClusterItemEnabled(int i, boolean z) {
        for (ActionItem actionItem : sClusterItems) {
            if (actionItem.action == i) {
                actionItem.enabled = z;
                return;
            }
        }
    }

    public void setClusterItemVisibility(int i, boolean z) {
        for (ActionItem actionItem : sClusterItems) {
            if (actionItem.action == i) {
                actionItem.visible = z;
                return;
            }
        }
    }

    public int getClusterTypeAction() {
        return sClusterItems[this.mCurrentIndex].action;
    }

    public void enableClusterMenu(int i, ClusterRunner clusterRunner) {
        if (this.mActionBar != null) {
            this.mClusterRunner = null;
            this.mActionBar.setListNavigationCallbacks(this.mAdapter, this);
            this.mActionBar.setNavigationMode(1);
            setSelectedAction(i);
            this.mClusterRunner = clusterRunner;
        }
    }

    public void disableClusterMenu(boolean z) {
        if (this.mActionBar != null) {
            this.mClusterRunner = null;
            if (z) {
                this.mActionBar.setNavigationMode(0);
            }
        }
    }

    public void onConfigurationChanged() {
        if (this.mActionBar != null && this.mAlbumModeListener != null) {
            enableAlbumModeMenu(this.mLastAlbumModeSelected, this.mAlbumModeListener);
        }
    }

    public void enableAlbumModeMenu(int i, OnAlbumModeSelectedListener onAlbumModeSelectedListener) {
        if (this.mActionBar != null) {
            if (this.mAlbumModeAdapter == null) {
                Resources resources = this.mActivity.getResources();
                this.mAlbumModes = new CharSequence[]{resources.getString(R.string.switch_photo_filmstrip), resources.getString(R.string.switch_photo_grid)};
                this.mAlbumModeAdapter = new AlbumModeAdapter();
            }
            this.mAlbumModeListener = null;
            this.mLastAlbumModeSelected = i;
            this.mActionBar.setListNavigationCallbacks(this.mAlbumModeAdapter, this);
            this.mActionBar.setNavigationMode(1);
            this.mActionBar.setSelectedNavigationItem(i);
            this.mAlbumModeListener = onAlbumModeSelectedListener;
        }
    }

    public void disableAlbumModeMenu(boolean z) {
        if (this.mActionBar != null) {
            this.mAlbumModeListener = null;
            if (z) {
                this.mActionBar.setNavigationMode(0);
            }
        }
    }

    public void showClusterDialog(final ClusterRunner clusterRunner) {
        createDialogData();
        final ArrayList<Integer> arrayList = this.mActions;
        new AlertDialog.Builder(this.mContext).setTitle(R.string.group_by).setItems(this.mTitles, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                GalleryActionBar.this.mActivity.getGLRoot().lockRenderThread();
                try {
                    clusterRunner.doCluster(((Integer) arrayList.get(i)).intValue());
                } finally {
                    GalleryActionBar.this.mActivity.getGLRoot().unlockRenderThread();
                }
            }
        }).create().show();
    }

    public void setDisplayOptions(boolean z, boolean z2) {
        if (this.mActionBar == null) {
            return;
        }
        int i = z ? 4 : 0;
        if (z2) {
            i |= 8;
        }
        this.mActionBar.setDisplayOptions(i, 12);
        this.mActionBar.setHomeButtonEnabled(z);
    }

    public void setTitle(String str) {
        if (this.mActionBar != null) {
            this.mTitleValue = str;
            this.mActionBar.setTitle(str);
        }
    }

    public void setTitle(int i) {
        if (this.mActionBar != null) {
            this.mTitleValue = this.mContext.getString(i);
            this.mActionBar.setTitle(this.mContext.getString(i));
        }
    }

    public void setSubtitle(String str) {
        if (this.mActionBar != null) {
            this.mActionBar.setSubtitle(str);
        }
    }

    public void show() {
        if (this.mActionBar != null) {
            this.mActionBar.show();
        }
    }

    public void hide() {
        if (this.mActionBar != null) {
            this.mActionBar.hide();
        }
    }

    public void addOnMenuVisibilityListener(ActionBar.OnMenuVisibilityListener onMenuVisibilityListener) {
        if (this.mActionBar != null) {
            this.mActionBar.addOnMenuVisibilityListener(onMenuVisibilityListener);
        }
    }

    public void removeOnMenuVisibilityListener(ActionBar.OnMenuVisibilityListener onMenuVisibilityListener) {
        if (this.mActionBar != null) {
            this.mActionBar.removeOnMenuVisibilityListener(onMenuVisibilityListener);
        }
    }

    public boolean setSelectedAction(int i) {
        if (this.mActionBar == null) {
            return false;
        }
        int length = sClusterItems.length;
        for (int i2 = 0; i2 < length; i2++) {
            if (sClusterItems[i2].action == i) {
                this.mActionBar.setSelectedNavigationItem(i2);
                this.mCurrentIndex = i2;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onNavigationItemSelected(int i, long j) {
        if ((i != this.mCurrentIndex && this.mClusterRunner != null) || this.mAlbumModeListener != null) {
            this.mActivity.getGLRoot().lockRenderThread();
            try {
                if (this.mAlbumModeListener != null) {
                    this.mAlbumModeListener.onAlbumModeSelected(i);
                } else {
                    this.mClusterRunner.doCluster(sClusterItems[i].action);
                }
                return false;
            } finally {
                this.mActivity.getGLRoot().unlockRenderThread();
            }
        }
        return false;
    }

    public void createActionBarMenu(int i, Menu menu) {
        this.mActivity.getMenuInflater().inflate(i, menu);
        this.mActionBarMenu = menu;
        menu.findItem(R.id.action_share_panorama);
        MenuItem menuItemFindItem = menu.findItem(R.id.action_share);
        if (menuItemFindItem != null) {
            this.mShareActionProvider = (ShareActionProvider) menuItemFindItem.getActionProvider();
            this.mShareActionProvider.setShareHistoryFileName("share_history.xml");
            this.mActivityChooserView = new ActivityChooserViewWrapper(menuItemFindItem);
            this.mDataModel = new ActivityChooserModelWrapper(this.mActivity, "share_history.xml");
            if (this.mDataModel != null && this.mOnChooseActivityListener != null) {
                this.mDataModel.setOnChooseActivityListener(this.mOnChooseActivityListener);
            }
            this.mShareActionProvider.setShareIntent(this.mShareIntent);
        }
        if (this.mActivityChooserView != null) {
            this.mActivity.getTheme().resolveAttribute(this.mActivity.getResources().getIdentifier("actionModeShareDrawable", "attr", "com.android.internal"), new TypedValue(), true);
            this.mActivityChooserView.setExpandActivityOverflowButtonDrawable(this.mActivity.getApplicationContext().getResources().getDrawable(R.drawable.ic_menu_share_holo_light));
        }
    }

    public Menu getMenu() {
        return this.mActionBarMenu;
    }

    public void setShareIntents(Intent intent, Intent intent2, ActivityChooserModelWrapper.OnChooseActivityListenerWrapper onChooseActivityListenerWrapper) {
        this.mShareIntent = intent2;
        if (this.mShareActionProvider != null) {
            this.mShareActionProvider.setShareIntent(intent2);
            if (this.mDataModel != null) {
                Log.d("Gallery2/GalleryActionBar", "mDataModel.setOnChooseActivityListener(onChooseListener)");
                this.mDataModel.setOnChooseActivityListener(onChooseActivityListenerWrapper);
            }
            this.mOnChooseActivityListener = onChooseActivityListenerWrapper;
        }
    }

    public void resetOnChooseActivityListener() {
        if (this.mDataModel != null) {
            Log.d("Gallery2/GalleryActionBar", "reset mDataModel#OnChooseActivityListener(null)");
            this.mDataModel.setOnChooseActivityListener(null);
        }
        this.mOnChooseActivityListener = null;
    }

    public void removeAlbumModeListener() {
        if (this.mActionBar != null) {
            this.mAlbumModeListener = null;
            Log.d("Gallery2/GalleryActionBar", "<removeAlbumModeListener> removeAlbumModeListener to doCluster");
        }
    }

    public final void notifyDataSetChanged() {
        if (this.mAlbumModeAdapter != null) {
            this.mAlbumModeAdapter.notifyDataSetChanged();
        }
    }
}
