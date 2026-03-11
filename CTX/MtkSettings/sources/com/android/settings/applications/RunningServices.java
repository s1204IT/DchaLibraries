package com.android.settings.applications;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.widget.LoadingViewController;

public class RunningServices extends SettingsPreferenceFragment {
    private View mLoadingContainer;
    private LoadingViewController mLoadingViewController;
    private Menu mOptionsMenu;
    private final Runnable mRunningProcessesAvail = new Runnable() {
        @Override
        public void run() {
            RunningServices.this.mLoadingViewController.showContent(true);
        }
    };
    private RunningProcessesView mRunningProcessesView;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        getActivity().setTitle(R.string.runningservices_settings_title);
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        View viewInflate = layoutInflater.inflate(R.layout.manage_applications_running, (ViewGroup) null);
        this.mRunningProcessesView = (RunningProcessesView) viewInflate.findViewById(R.id.running_processes);
        this.mRunningProcessesView.doCreate();
        this.mLoadingContainer = viewInflate.findViewById(R.id.loading_container);
        this.mLoadingViewController = new LoadingViewController(this.mLoadingContainer, this.mRunningProcessesView);
        return viewInflate;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        this.mOptionsMenu = menu;
        menu.add(0, 1, 1, R.string.show_running_services).setShowAsAction(0);
        menu.add(0, 2, 2, R.string.show_background_processes).setShowAsAction(0);
        updateOptionsMenu();
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mLoadingViewController.handleLoadingContainer(this.mRunningProcessesView.doResume(this, this.mRunningProcessesAvail), false);
    }

    @Override
    public void onPause() {
        super.onPause();
        this.mRunningProcessesView.doPause();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case 1:
                this.mRunningProcessesView.mAdapter.setShowBackground(false);
                break;
            case 2:
                this.mRunningProcessesView.mAdapter.setShowBackground(true);
                break;
            default:
                return false;
        }
        updateOptionsMenu();
        return true;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        updateOptionsMenu();
    }

    private void updateOptionsMenu() {
        boolean showBackground = this.mRunningProcessesView.mAdapter.getShowBackground();
        this.mOptionsMenu.findItem(1).setVisible(showBackground);
        this.mOptionsMenu.findItem(2).setVisible(!showBackground);
    }

    @Override
    public int getMetricsCategory() {
        return 404;
    }
}
