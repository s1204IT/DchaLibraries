package com.android.settings.support.actionbar;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import com.android.settingslib.HelpUtils;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.ObservableFragment;
import com.android.settingslib.core.lifecycle.ObservablePreferenceFragment;
import com.android.settingslib.core.lifecycle.events.OnCreateOptionsMenu;

public class HelpMenuController implements LifecycleObserver, OnCreateOptionsMenu {
    private final Fragment mHost;

    public static void init(ObservablePreferenceFragment observablePreferenceFragment) {
        observablePreferenceFragment.getLifecycle().addObserver(new HelpMenuController(observablePreferenceFragment));
    }

    public static void init(ObservableFragment observableFragment) {
        observableFragment.getLifecycle().addObserver(new HelpMenuController(observableFragment));
    }

    private HelpMenuController(Fragment fragment) {
        this.mHost = fragment;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        int helpResource;
        Bundle arguments = this.mHost.getArguments();
        if (arguments != null && arguments.containsKey("help_uri_resource")) {
            helpResource = arguments.getInt("help_uri_resource");
        } else if (this.mHost instanceof HelpResourceProvider) {
            helpResource = ((HelpResourceProvider) this.mHost).getHelpResource();
        } else {
            helpResource = 0;
        }
        String string = helpResource != 0 ? this.mHost.getContext().getString(helpResource) : null;
        Activity activity = this.mHost.getActivity();
        if (string != null && activity != null) {
            HelpUtils.prepareHelpMenuItem(activity, menu, string, this.mHost.getClass().getName());
        }
    }
}
