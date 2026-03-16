package com.android.settings.nfc;

import android.app.ActionBar;
import android.app.Fragment;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.UserManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.widget.SwitchBar;

public class AndroidBeam extends Fragment implements SwitchBar.OnSwitchChangeListener {
    private boolean mBeamDisallowed;
    private NfcAdapter mNfcAdapter;
    private CharSequence mOldActivityTitle;
    private SwitchBar mSwitchBar;
    private View mView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getActivity().getActionBar();
        this.mOldActivityTitle = actionBar.getTitle();
        actionBar.setTitle(R.string.android_beam_settings_title);
        this.mNfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());
        this.mBeamDisallowed = ((UserManager) getActivity().getSystemService("user")).hasUserRestriction("no_outgoing_beam");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.mView = inflater.inflate(R.layout.android_beam, container, false);
        return this.mView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        SettingsActivity activity = (SettingsActivity) getActivity();
        this.mSwitchBar = activity.getSwitchBar();
        this.mSwitchBar.setChecked(!this.mBeamDisallowed && this.mNfcAdapter.isNdefPushEnabled());
        this.mSwitchBar.addOnSwitchChangeListener(this);
        this.mSwitchBar.setEnabled(this.mBeamDisallowed ? false : true);
        this.mSwitchBar.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (this.mOldActivityTitle != null) {
            getActivity().getActionBar().setTitle(this.mOldActivityTitle);
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
}
