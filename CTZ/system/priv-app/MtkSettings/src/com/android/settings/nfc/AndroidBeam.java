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
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.core.InstrumentedFragment;
import com.android.settings.enterprise.ActionDisabledByAdminDialogHelper;
import com.android.settings.widget.SwitchBar;
import com.android.settingslib.HelpUtils;
import com.android.settingslib.RestrictedLockUtils;

/* loaded from: classes.dex */
public class AndroidBeam extends InstrumentedFragment implements SwitchBar.OnSwitchChangeListener {
    private boolean mBeamDisallowedByBase;
    private boolean mBeamDisallowedByOnlyAdmin;
    private NfcAdapter mNfcAdapter;
    private CharSequence mOldActivityTitle;
    private SwitchBar mSwitchBar;
    private View mView;

    @Override // com.android.settingslib.core.lifecycle.ObservableFragment, android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mNfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());
        setHasOptionsMenu(true);
    }

    @Override // com.android.settingslib.core.lifecycle.ObservableFragment, android.app.Fragment
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater);
        HelpUtils.prepareHelpMenuItem(getActivity(), menu, R.string.help_uri_beam, getClass().getName());
    }

    @Override // android.app.Fragment
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        RestrictedLockUtils.EnforcedAdmin enforcedAdminCheckIfRestrictionEnforced = RestrictedLockUtils.checkIfRestrictionEnforced(getActivity(), "no_outgoing_beam", UserHandle.myUserId());
        UserManager.get(getActivity());
        this.mBeamDisallowedByBase = RestrictedLockUtils.hasBaseUserRestriction(getActivity(), "no_outgoing_beam", UserHandle.myUserId());
        if (!this.mBeamDisallowedByBase && enforcedAdminCheckIfRestrictionEnforced != null) {
            new ActionDisabledByAdminDialogHelper(getActivity()).prepareDialogBuilder("no_outgoing_beam", enforcedAdminCheckIfRestrictionEnforced).show();
            this.mBeamDisallowedByOnlyAdmin = true;
            return new View(getContext());
        }
        this.mView = layoutInflater.inflate(R.layout.android_beam, viewGroup, false);
        return this.mView;
    }

    @Override // android.app.Fragment
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        SettingsActivity settingsActivity = (SettingsActivity) getActivity();
        this.mOldActivityTitle = settingsActivity.getActionBar().getTitle();
        this.mSwitchBar = settingsActivity.getSwitchBar();
        if (this.mBeamDisallowedByOnlyAdmin) {
            this.mSwitchBar.hide();
        } else {
            this.mSwitchBar.setChecked(!this.mBeamDisallowedByBase && this.mNfcAdapter.isNdefPushEnabled());
            this.mSwitchBar.addOnSwitchChangeListener(this);
            this.mSwitchBar.setEnabled(!this.mBeamDisallowedByBase);
            this.mSwitchBar.show();
        }
        settingsActivity.setTitle(R.string.android_beam_settings_title);
    }

    @Override // android.app.Fragment
    public void onDestroyView() {
        super.onDestroyView();
        if (this.mOldActivityTitle != null) {
            getActivity().getActionBar().setTitle(this.mOldActivityTitle);
        }
        if (!this.mBeamDisallowedByOnlyAdmin) {
            this.mSwitchBar.removeOnSwitchChangeListener(this);
            this.mSwitchBar.hide();
        }
    }

    @Override // com.android.settings.widget.SwitchBar.OnSwitchChangeListener
    public void onSwitchChanged(Switch r2, boolean z) {
        boolean zDisableNdefPush;
        this.mSwitchBar.setEnabled(false);
        if (z) {
            zDisableNdefPush = this.mNfcAdapter.enableNdefPush();
        } else {
            zDisableNdefPush = this.mNfcAdapter.disableNdefPush();
        }
        if (zDisableNdefPush) {
            this.mSwitchBar.setChecked(z);
        }
        this.mSwitchBar.setEnabled(true);
    }

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 69;
    }
}
