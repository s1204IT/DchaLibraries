package com.android.packageinstaller.television;

import android.os.Bundle;
import android.support.v17.leanback.app.GuidedStepFragment;
import android.support.v17.leanback.widget.GuidanceStylist;
import android.support.v17.leanback.widget.GuidedAction;
import com.android.packageinstaller.R;
import com.android.packageinstaller.UninstallerActivity;
import java.util.List;

public class ErrorFragment extends GuidedStepFragment {
    @Override
    public int onProvideTheme() {
        return R.style.Theme_Leanback_GuidedStep;
    }

    @Override
    public GuidanceStylist.Guidance onCreateGuidance(Bundle bundle) {
        return new GuidanceStylist.Guidance(getString(getArguments().getInt("com.android.packageinstaller.arg.title")), getString(getArguments().getInt("com.android.packageinstaller.arg.text")), null, null);
    }

    @Override
    public void onCreateActions(List<GuidedAction> list, Bundle bundle) {
        list.add(new GuidedAction.Builder(getContext()).clickAction(-4L).build());
    }

    @Override
    public void onGuidedActionClicked(GuidedAction guidedAction) {
        if (isAdded()) {
            if (getActivity() instanceof UninstallerActivity) {
                ((UninstallerActivity) getActivity()).dispatchAborted();
            }
            getActivity().setResult(1);
            getActivity().finish();
        }
    }
}
