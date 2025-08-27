package com.android.settings.search.actionbar;

import android.app.Fragment;
import android.view.Menu;
import android.view.MenuInflater;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.ObservableFragment;
import com.android.settingslib.core.lifecycle.ObservablePreferenceFragment;
import com.android.settingslib.core.lifecycle.events.OnCreateOptionsMenu;

/* loaded from: classes.dex */
public class SearchMenuController implements LifecycleObserver, OnCreateOptionsMenu {
    private final Fragment mHost;

    public static void init(ObservablePreferenceFragment observablePreferenceFragment) {
        observablePreferenceFragment.getLifecycle().addObserver(new SearchMenuController(observablePreferenceFragment));
    }

    public static void init(ObservableFragment observableFragment) {
        observableFragment.getLifecycle().addObserver(new SearchMenuController(observableFragment));
    }

    private SearchMenuController(Fragment fragment) {
        this.mHost = fragment;
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnCreateOptionsMenu
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
    }
}
