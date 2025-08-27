package com.android.settings.applications;

import android.content.res.Resources;
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

/* loaded from: classes.dex */
public class RunningServices extends SettingsPreferenceFragment {
    private View mLoadingContainer;
    private LoadingViewController mLoadingViewController;
    private Menu mOptionsMenu;
    private final Runnable mRunningProcessesAvail = new Runnable() { // from class: com.android.settings.applications.RunningServices.1
        @Override // java.lang.Runnable
        public void run() throws Resources.NotFoundException {
            RunningServices.this.mLoadingViewController.showContent(true);
        }
    };
    private RunningProcessesView mRunningProcessesView;

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        getActivity().setTitle(R.string.runningservices_settings_title);
    }

    @Override // com.android.settings.SettingsPreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        View viewInflate = layoutInflater.inflate(R.layout.manage_applications_running, (ViewGroup) null);
        this.mRunningProcessesView = (RunningProcessesView) viewInflate.findViewById(R.id.running_processes);
        this.mRunningProcessesView.doCreate();
        this.mLoadingContainer = viewInflate.findViewById(R.id.loading_container);
        this.mLoadingViewController = new LoadingViewController(this.mLoadingContainer, this.mRunningProcessesView);
        return viewInflate;
    }

    @Override // com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        this.mOptionsMenu = menu;
        menu.add(0, 1, 1, R.string.show_running_services).setShowAsAction(0);
        menu.add(0, 2, 2, R.string.show_background_processes).setShowAsAction(0);
        updateOptionsMenu();
    }

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settings.core.InstrumentedPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onResume() throws Resources.NotFoundException {
        super.onResume();
        this.mLoadingViewController.handleLoadingContainer(this.mRunningProcessesView.doResume(this, this.mRunningProcessesAvail), false);
    }

    @Override // com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onPause() {
        super.onPause();
        this.mRunningProcessesView.doPause();
    }

    @Override // com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
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

    @Override // com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onPrepareOptionsMenu(Menu menu) {
        updateOptionsMenu();
    }

    private void updateOptionsMenu() {
        boolean showBackground = this.mRunningProcessesView.mAdapter.getShowBackground();
        this.mOptionsMenu.findItem(1).setVisible(showBackground);
        this.mOptionsMenu.findItem(2).setVisible(!showBackground);
    }

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 404;
    }
}
