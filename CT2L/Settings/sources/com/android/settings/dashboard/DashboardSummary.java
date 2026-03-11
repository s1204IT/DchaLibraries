package com.android.settings.dashboard;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import java.util.List;

public class DashboardSummary extends Fragment {
    private ViewGroup mDashboard;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    Context context = DashboardSummary.this.getActivity();
                    DashboardSummary.this.rebuildUI(context);
                    break;
            }
        }
    };
    private HomePackageReceiver mHomePackageReceiver = new HomePackageReceiver();
    private LayoutInflater mLayoutInflater;

    private class HomePackageReceiver extends BroadcastReceiver {
        private HomePackageReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            DashboardSummary.this.rebuildUI(context);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        sendRebuildUI();
        IntentFilter filter = new IntentFilter("android.intent.action.PACKAGE_ADDED");
        filter.addAction("android.intent.action.PACKAGE_REMOVED");
        filter.addAction("android.intent.action.PACKAGE_CHANGED");
        filter.addAction("android.intent.action.PACKAGE_REPLACED");
        filter.addDataScheme("package");
        getActivity().registerReceiver(this.mHomePackageReceiver, filter);
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(this.mHomePackageReceiver);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.mLayoutInflater = inflater;
        View rootView = inflater.inflate(R.layout.dashboard, container, false);
        this.mDashboard = (ViewGroup) rootView.findViewById(R.id.dashboard_container);
        return rootView;
    }

    public void rebuildUI(Context context) {
        if (!isAdded()) {
            Log.w("DashboardSummary", "Cannot build the DashboardSummary UI yet as the Fragment is not added");
            return;
        }
        long start = System.currentTimeMillis();
        Resources res = getResources();
        this.mDashboard.removeAllViews();
        List<DashboardCategory> categories = ((SettingsActivity) context).getDashboardCategories(true);
        int count = categories.size();
        for (int n = 0; n < count; n++) {
            DashboardCategory category = categories.get(n);
            View categoryView = this.mLayoutInflater.inflate(R.layout.dashboard_category, this.mDashboard, false);
            TextView categoryLabel = (TextView) categoryView.findViewById(R.id.category_title);
            categoryLabel.setText(category.getTitle(res));
            ViewGroup categoryContent = (ViewGroup) categoryView.findViewById(R.id.category_content);
            int tilesCount = category.getTilesCount();
            for (int i = 0; i < tilesCount; i++) {
                DashboardTile tile = category.getTile(i);
                DashboardTileView tileView = new DashboardTileView(context);
                updateTileView(context, res, tile, tileView.getImageView(), tileView.getTitleTextView(), tileView.getStatusTextView());
                tileView.setTile(tile);
                categoryContent.addView(tileView);
            }
            this.mDashboard.addView(categoryView);
        }
        long delta = System.currentTimeMillis() - start;
        Log.d("DashboardSummary", "rebuildUI took: " + delta + " ms");
    }

    private void updateTileView(Context context, Resources res, DashboardTile tile, ImageView tileIcon, TextView tileTextView, TextView statusTextView) {
        if (tile.iconRes > 0) {
            tileIcon.setImageResource(tile.iconRes);
        } else {
            tileIcon.setImageDrawable(null);
            tileIcon.setBackground(null);
        }
        tileTextView.setText(tile.getTitle(res));
        CharSequence summary = tile.getSummary(res);
        if (!TextUtils.isEmpty(summary)) {
            statusTextView.setVisibility(0);
            statusTextView.setText(summary);
        } else {
            statusTextView.setVisibility(8);
        }
    }

    private void sendRebuildUI() {
        if (!this.mHandler.hasMessages(1)) {
            this.mHandler.sendEmptyMessage(1);
        }
    }
}
