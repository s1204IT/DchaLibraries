package com.android.gallery3d.app;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
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
import java.util.ArrayList;

public class GalleryActionBar implements ActionBar.OnNavigationListener {
    private static final ActionItem[] sClusterItems = {new ActionItem(1, true, false, R.string.albums, R.string.group_by_album), new ActionItem(4, true, false, R.string.locations, R.string.location, R.string.group_by_location), new ActionItem(2, true, false, R.string.times, R.string.time, R.string.group_by_time), new ActionItem(32, true, false, R.string.people, R.string.group_by_faces), new ActionItem(8, true, false, R.string.tags, R.string.group_by_tags)};
    private ActionBar mActionBar;
    private Menu mActionBarMenu;
    private ArrayList<Integer> mActions;
    private AbstractGalleryActivity mActivity;
    private AlbumModeAdapter mAlbumModeAdapter;
    private OnAlbumModeSelectedListener mAlbumModeListener;
    private CharSequence[] mAlbumModes;
    private ClusterRunner mClusterRunner;
    private Context mContext;
    private LayoutInflater mInflater;
    private int mLastAlbumModeSelected;
    private ShareActionProvider mShareActionProvider;
    private Intent mShareIntent;
    private ShareActionProvider mSharePanoramaActionProvider;
    private Intent mSharePanoramaIntent;
    private CharSequence[] mTitles;
    private ClusterAdapter mAdapter = new ClusterAdapter();
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

        public ActionItem(int action, boolean applied, boolean enabled, int title, int clusterBy) {
            this(action, applied, enabled, title, title, clusterBy);
        }

        public ActionItem(int action, boolean applied, boolean enabled, int spinnerTitle, int dialogTitle, int clusterBy) {
            this.action = action;
            this.enabled = enabled;
            this.spinnerTitle = spinnerTitle;
            this.dialogTitle = dialogTitle;
            this.clusterBy = clusterBy;
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
        public Object getItem(int position) {
            return GalleryActionBar.sClusterItems[position];
        }

        @Override
        public long getItemId(int position) {
            return GalleryActionBar.sClusterItems[position].action;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = GalleryActionBar.this.mInflater.inflate(R.layout.action_bar_text, parent, false);
            }
            TextView view = (TextView) convertView;
            view.setText(GalleryActionBar.sClusterItems[position].spinnerTitle);
            return convertView;
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
        public Object getItem(int position) {
            return GalleryActionBar.this.mAlbumModes[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = GalleryActionBar.this.mInflater.inflate(R.layout.action_bar_two_line_text, parent, false);
            }
            TwoLineListItem view = (TwoLineListItem) convertView;
            view.getText1().setText(GalleryActionBar.this.mActionBar.getTitle());
            view.getText2().setText((CharSequence) getItem(position));
            return convertView;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = GalleryActionBar.this.mInflater.inflate(R.layout.action_bar_text, parent, false);
            }
            TextView view = (TextView) convertView;
            view.setText((CharSequence) getItem(position));
            return convertView;
        }
    }

    public static String getClusterByTypeString(Context context, int type) {
        ActionItem[] arr$ = sClusterItems;
        for (ActionItem item : arr$) {
            if (item.action == type) {
                return context.getString(item.clusterBy);
            }
        }
        return null;
    }

    public GalleryActionBar(AbstractGalleryActivity activity) {
        this.mActionBar = activity.getActionBar();
        this.mContext = activity.getAndroidContext();
        this.mActivity = activity;
        this.mInflater = this.mActivity.getLayoutInflater();
    }

    private void createDialogData() {
        ArrayList<CharSequence> titles = new ArrayList<>();
        this.mActions = new ArrayList<>();
        ActionItem[] arr$ = sClusterItems;
        for (ActionItem item : arr$) {
            if (item.enabled && item.visible) {
                titles.add(this.mContext.getString(item.dialogTitle));
                this.mActions.add(Integer.valueOf(item.action));
            }
        }
        this.mTitles = new CharSequence[titles.size()];
        titles.toArray(this.mTitles);
    }

    public int getHeight() {
        if (this.mActionBar != null) {
            return this.mActionBar.getHeight();
        }
        return 0;
    }

    public void setClusterItemEnabled(int id, boolean enabled) {
        ActionItem[] arr$ = sClusterItems;
        for (ActionItem item : arr$) {
            if (item.action == id) {
                item.enabled = enabled;
                return;
            }
        }
    }

    public void setClusterItemVisibility(int id, boolean visible) {
        ActionItem[] arr$ = sClusterItems;
        for (ActionItem item : arr$) {
            if (item.action == id) {
                item.visible = visible;
                return;
            }
        }
    }

    public int getClusterTypeAction() {
        return sClusterItems[this.mCurrentIndex].action;
    }

    public void enableClusterMenu(int action, ClusterRunner runner) {
        if (this.mActionBar != null) {
            this.mClusterRunner = null;
            this.mActionBar.setListNavigationCallbacks(this.mAdapter, this);
            this.mActionBar.setNavigationMode(1);
            setSelectedAction(action);
            this.mClusterRunner = runner;
        }
    }

    public void disableClusterMenu(boolean hideMenu) {
        if (this.mActionBar != null) {
            this.mClusterRunner = null;
            if (hideMenu) {
                this.mActionBar.setNavigationMode(0);
            }
        }
    }

    public void onConfigurationChanged() {
        if (this.mActionBar != null && this.mAlbumModeListener != null) {
            OnAlbumModeSelectedListener listener = this.mAlbumModeListener;
            enableAlbumModeMenu(this.mLastAlbumModeSelected, listener);
        }
    }

    public void enableAlbumModeMenu(int selected, OnAlbumModeSelectedListener listener) {
        if (this.mActionBar != null) {
            if (this.mAlbumModeAdapter == null) {
                Resources res = this.mActivity.getResources();
                this.mAlbumModes = new CharSequence[]{res.getString(R.string.switch_photo_filmstrip), res.getString(R.string.switch_photo_grid)};
                this.mAlbumModeAdapter = new AlbumModeAdapter();
            }
            this.mAlbumModeListener = null;
            this.mLastAlbumModeSelected = selected;
            this.mActionBar.setListNavigationCallbacks(this.mAlbumModeAdapter, this);
            this.mActionBar.setNavigationMode(1);
            this.mActionBar.setSelectedNavigationItem(selected);
            this.mAlbumModeListener = listener;
        }
    }

    public void disableAlbumModeMenu(boolean hideMenu) {
        if (this.mActionBar != null) {
            this.mAlbumModeListener = null;
            if (hideMenu) {
                this.mActionBar.setNavigationMode(0);
            }
        }
    }

    public void showClusterDialog(final ClusterRunner clusterRunner) {
        createDialogData();
        final ArrayList<Integer> actions = this.mActions;
        new AlertDialog.Builder(this.mContext).setTitle(R.string.group_by).setItems(this.mTitles, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                GalleryActionBar.this.mActivity.getGLRoot().lockRenderThread();
                try {
                    clusterRunner.doCluster(((Integer) actions.get(which)).intValue());
                } finally {
                    GalleryActionBar.this.mActivity.getGLRoot().unlockRenderThread();
                }
            }
        }).create().show();
    }

    public void setDisplayOptions(boolean displayHomeAsUp, boolean showTitle) {
        if (this.mActionBar != null) {
            int options = displayHomeAsUp ? 0 | 4 : 0;
            if (showTitle) {
                options |= 8;
            }
            this.mActionBar.setDisplayOptions(options, 12);
            this.mActionBar.setHomeButtonEnabled(displayHomeAsUp);
        }
    }

    public void setTitle(String title) {
        if (this.mActionBar != null) {
            this.mActionBar.setTitle(title);
        }
    }

    public void setTitle(int titleId) {
        if (this.mActionBar != null) {
            this.mActionBar.setTitle(this.mContext.getString(titleId));
        }
    }

    public void setSubtitle(String title) {
        if (this.mActionBar != null) {
            this.mActionBar.setSubtitle(title);
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

    public void addOnMenuVisibilityListener(ActionBar.OnMenuVisibilityListener listener) {
        if (this.mActionBar != null) {
            this.mActionBar.addOnMenuVisibilityListener(listener);
        }
    }

    public void removeOnMenuVisibilityListener(ActionBar.OnMenuVisibilityListener listener) {
        if (this.mActionBar != null) {
            this.mActionBar.removeOnMenuVisibilityListener(listener);
        }
    }

    public boolean setSelectedAction(int type) {
        if (this.mActionBar == null) {
            return false;
        }
        int n = sClusterItems.length;
        for (int i = 0; i < n; i++) {
            ActionItem item = sClusterItems[i];
            if (item.action == type) {
                this.mActionBar.setSelectedNavigationItem(i);
                this.mCurrentIndex = i;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        if ((itemPosition != this.mCurrentIndex && this.mClusterRunner != null) || this.mAlbumModeListener != null) {
            this.mActivity.getGLRoot().lockRenderThread();
            try {
                if (this.mAlbumModeListener != null) {
                    this.mAlbumModeListener.onAlbumModeSelected(itemPosition);
                } else {
                    this.mClusterRunner.doCluster(sClusterItems[itemPosition].action);
                }
                return false;
            } finally {
                this.mActivity.getGLRoot().unlockRenderThread();
            }
        }
        return false;
    }

    public void createActionBarMenu(int menuRes, Menu menu) {
        this.mActivity.getMenuInflater().inflate(menuRes, menu);
        this.mActionBarMenu = menu;
        MenuItem item = menu.findItem(R.id.action_share_panorama);
        if (item != null) {
            this.mSharePanoramaActionProvider = (ShareActionProvider) item.getActionProvider();
            this.mSharePanoramaActionProvider.setShareHistoryFileName("panorama_share_history.xml");
            this.mSharePanoramaActionProvider.setShareIntent(this.mSharePanoramaIntent);
        }
        MenuItem item2 = menu.findItem(R.id.action_share);
        if (item2 != null) {
            this.mShareActionProvider = (ShareActionProvider) item2.getActionProvider();
            this.mShareActionProvider.setShareHistoryFileName("share_history.xml");
            this.mShareActionProvider.setShareIntent(this.mShareIntent);
        }
    }

    public Menu getMenu() {
        return this.mActionBarMenu;
    }

    public void setShareIntents(Intent sharePanoramaIntent, Intent shareIntent, ShareActionProvider.OnShareTargetSelectedListener onShareListener) {
        this.mSharePanoramaIntent = sharePanoramaIntent;
        if (this.mSharePanoramaActionProvider != null) {
            this.mSharePanoramaActionProvider.setShareIntent(sharePanoramaIntent);
        }
        this.mShareIntent = shareIntent;
        if (this.mShareActionProvider != null) {
            this.mShareActionProvider.setShareIntent(shareIntent);
            this.mShareActionProvider.setOnShareTargetSelectedListener(onShareListener);
        }
    }
}
