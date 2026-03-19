package com.android.documentsui.inspector;

import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import com.android.documentsui.R;
import com.android.internal.util.Preconditions;

public class InspectorFragment extends Fragment {
    private InspectorController mController;
    private ScrollView mView;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        RuntimeDataSupplier runtimeDataSupplier = new RuntimeDataSupplier(getActivity(), getLoaderManager());
        this.mView = (ScrollView) layoutInflater.inflate(R.layout.inspector_fragment, viewGroup, false);
        this.mController = new InspectorController(getActivity(), runtimeDataSupplier, this.mView, getArguments());
        return this.mView;
    }

    @Override
    public void onStart() {
        super.onStart();
        this.mController.loadInfo((Uri) getArguments().get("docUri"));
    }

    @Override
    public void onStop() {
        super.onStop();
        this.mController.reset();
    }

    public static InspectorFragment newInstance(Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras == null) {
            extras = Bundle.EMPTY;
        }
        Bundle bundleDeepCopy = extras.deepCopy();
        Uri data = intent.getData();
        Preconditions.checkArgument(data.getScheme().equals("content"));
        bundleDeepCopy.putParcelable("docUri", data);
        InspectorFragment inspectorFragment = new InspectorFragment();
        inspectorFragment.setArguments(bundleDeepCopy);
        return inspectorFragment;
    }
}
