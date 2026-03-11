package com.android.systemui.plugins;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;

public abstract class PluginFragment extends Fragment implements Plugin {
    private Context mPluginContext;

    @Override
    public void onCreate(Context context, Context context2) {
        this.mPluginContext = context2;
    }

    @Override
    public LayoutInflater onGetLayoutInflater(Bundle bundle) {
        return super.onGetLayoutInflater(bundle).cloneInContext(getContext());
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
    }

    @Override
    public Context getContext() {
        return this.mPluginContext;
    }
}
