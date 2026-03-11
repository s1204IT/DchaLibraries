package com.android.settings.nfc;

import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import com.android.settings.InstrumentedFragment;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.ShowAdminSupportDetailsDialog;
import com.android.settings.widget.SwitchBar;
import com.android.settingslib.HelpUtils;
import com.android.settingslib.RestrictedLockUtils;

public class AndroidBeam extends InstrumentedFragment implements SwitchBar.OnSwitchChangeListener {
    private boolean mBeamDisallowedByBase;
    private boolean mBeamDisallowedByOnlyAdmin;
    private NfcAdapter mNfcAdapter;
    private CharSequence mOldActivityTitle;
    private SwitchBar mSwitchBar;
    private View mView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mNfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        HelpUtils.prepareHelpMenuItem(getActivity(), menu, R.string.help_uri_beam, getClass().getName());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        RestrictedLockUtils.EnforcedAdmin admin = RestrictedLockUtils.checkIfRestrictionEnforced(getActivity(), "no_outgoing_beam", UserHandle.myUserId());
        UserManager.get(getActivity());
        this.mBeamDisallowedByBase = RestrictedLockUtils.hasBaseUserRestriction(getActivity(), "no_outgoing_beam", UserHandle.myUserId());
        if (!this.mBeamDisallowedByBase && admin != null) {
            View view = inflater.inflate(R.layout.admin_support_details_empty_view, (ViewGroup) null);
            ShowAdminSupportDetailsDialog.setAdminSupportDetails(getActivity(), view, admin, false);
            view.setVisibility(0);
            this.mBeamDisallowedByOnlyAdmin = true;
            return view;
        }
        this.mView = inflater.inflate(R.layout.android_beam, container, false);
        return this.mView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        SettingsActivity activity = (SettingsActivity) getActivity();
        this.mOldActivityTitle = activity.getActionBar().getTitle();
        this.mSwitchBar = activity.getSwitchBar();
        if (this.mBeamDisallowedByOnlyAdmin) {
            this.mSwitchBar.hide();
            return;
        }
        this.mSwitchBar.setChecked(!this.mBeamDisallowedByBase ? this.mNfcAdapter.isNdefPushEnabled() : false);
        this.mSwitchBar.addOnSwitchChangeListener(this);
        this.mSwitchBar.setEnabled(this.mBeamDisallowedByBase ? false : true);
        this.mSwitchBar.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (this.mOldActivityTitle != null) {
            getActivity().getActionBar().setTitle(this.mOldActivityTitle);
        }
        if (this.mBeamDisallowedByOnlyAdmin) {
            return;
        }
        this.mSwitchBar.removeOnSwitchChangeListener(this);
        this.mSwitchBar.hide();
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean desiredState) {
        boolean success;
        this.mSwitchBar.setEnabled(false);
        if (desiredState) {
            success = this.mNfcAdapter.enableNdefPush();
        } else {
            success = this.mNfcAdapter.disableNdefPush();
        }
        if (success) {
            this.mSwitchBar.setChecked(desiredState);
        }
        this.mSwitchBar.setEnabled(true);
    }

    @Override
    protected int getMetricsCategory() {
        return 69;
    }
}
