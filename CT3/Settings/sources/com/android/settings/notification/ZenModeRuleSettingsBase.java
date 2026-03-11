package com.android.settings.notification;

import android.app.AlertDialog;
import android.app.AutomaticZenRule;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.preference.DropDownPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Switch;
import android.widget.Toast;
import com.android.internal.logging.MetricsLogger;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.widget.SwitchBar;

public abstract class ZenModeRuleSettingsBase extends ZenModeSettingsBase implements SwitchBar.OnSwitchChangeListener {
    protected static final boolean DEBUG = ZenModeSettingsBase.DEBUG;
    protected Context mContext;
    private boolean mDeleting;
    protected boolean mDisableListeners;
    private Toast mEnabledToast;
    protected String mId;
    protected AutomaticZenRule mRule;
    private Preference mRuleName;
    private SwitchBar mSwitchBar;
    private DropDownPreference mZenMode;

    protected abstract int getEnabledToastText();

    protected abstract String getZenModeDependency();

    protected abstract void onCreateInternal();

    protected abstract boolean setRule(AutomaticZenRule automaticZenRule);

    protected abstract void updateControlsInternal();

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        this.mContext = getActivity();
        Intent intent = getActivity().getIntent();
        if (DEBUG) {
            Log.d("ZenModeSettings", "onCreate getIntent()=" + intent);
        }
        if (intent == null) {
            Log.w("ZenModeSettings", "No intent");
            toastAndFinish();
            return;
        }
        this.mId = intent.getStringExtra("android.service.notification.extra.RULE_ID");
        if (DEBUG) {
            Log.d("ZenModeSettings", "mId=" + this.mId);
        }
        if (refreshRuleOrFinish()) {
            return;
        }
        setHasOptionsMenu(true);
        onCreateInternal();
        PreferenceScreen root = getPreferenceScreen();
        this.mRuleName = root.findPreference("rule_name");
        this.mRuleName.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                ZenModeRuleSettingsBase.this.showRuleNameDialog();
                return true;
            }
        });
        this.mZenMode = (DropDownPreference) root.findPreference("zen_mode");
        this.mZenMode.setEntries(new CharSequence[]{getString(R.string.zen_mode_option_important_interruptions), getString(R.string.zen_mode_option_alarms), getString(R.string.zen_mode_option_no_interruptions)});
        this.mZenMode.setEntryValues(new CharSequence[]{Integer.toString(2), Integer.toString(4), Integer.toString(3)});
        this.mZenMode.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                int zenMode;
                if (ZenModeRuleSettingsBase.this.mDisableListeners || (zenMode = Integer.parseInt((String) newValue)) == ZenModeRuleSettingsBase.this.mRule.getInterruptionFilter()) {
                    return false;
                }
                if (ZenModeRuleSettingsBase.DEBUG) {
                    Log.d("ZenModeSettings", "onPrefChange zenMode=" + zenMode);
                }
                ZenModeRuleSettingsBase.this.mRule.setInterruptionFilter(zenMode);
                ZenModeRuleSettingsBase.this.setZenRule(ZenModeRuleSettingsBase.this.mId, ZenModeRuleSettingsBase.this.mRule);
                return true;
            }
        });
        this.mZenMode.setOrder(10);
        this.mZenMode.setDependency(getZenModeDependency());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isUiRestricted()) {
            return;
        }
        updateControls();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        SettingsActivity activity = (SettingsActivity) getActivity();
        this.mSwitchBar = activity.getSwitchBar();
        this.mSwitchBar.addOnSwitchChangeListener(this);
        this.mSwitchBar.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        this.mSwitchBar.removeOnSwitchChangeListener(this);
        this.mSwitchBar.hide();
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        if (DEBUG) {
            Log.d("ZenModeSettings", "onSwitchChanged " + isChecked);
        }
        if (this.mDisableListeners || isChecked == this.mRule.isEnabled()) {
            return;
        }
        MetricsLogger.action(this.mContext, 176, isChecked);
        if (DEBUG) {
            Log.d("ZenModeSettings", "onSwitchChanged enabled=" + isChecked);
        }
        this.mRule.setEnabled(isChecked);
        setZenRule(this.mId, this.mRule);
        if (isChecked) {
            int toastText = getEnabledToastText();
            if (toastText == 0) {
                return;
            }
            this.mEnabledToast = Toast.makeText(this.mContext, toastText, 0);
            this.mEnabledToast.show();
            return;
        }
        if (this.mEnabledToast == null) {
            return;
        }
        this.mEnabledToast.cancel();
    }

    protected void updateRule(Uri newConditionId) {
        this.mRule.setConditionId(newConditionId);
        setZenRule(this.mId, this.mRule);
    }

    @Override
    protected void onZenModeChanged() {
    }

    @Override
    protected void onZenModeConfigChanged() {
        if (refreshRuleOrFinish()) {
            return;
        }
        updateControls();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (DEBUG) {
            Log.d("ZenModeSettings", "onCreateOptionsMenu");
        }
        inflater.inflate(R.menu.zen_mode_rule, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (DEBUG) {
            Log.d("ZenModeSettings", "onOptionsItemSelected " + item.getItemId());
        }
        if (item.getItemId() == R.id.delete) {
            MetricsLogger.action(this.mContext, 174);
            showDeleteRuleDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void showRuleNameDialog() {
        new ZenRuleNameDialog(this.mContext, this.mRule.getName()) {
            @Override
            public void onOk(String ruleName) {
                ZenModeRuleSettingsBase.this.mRule.setName(ruleName);
                ZenModeRuleSettingsBase.this.setZenRule(ZenModeRuleSettingsBase.this.mId, ZenModeRuleSettingsBase.this.mRule);
            }
        }.show();
    }

    private boolean refreshRuleOrFinish() {
        this.mRule = getZenRule();
        if (DEBUG) {
            Log.d("ZenModeSettings", "mRule=" + this.mRule);
        }
        if (!setRule(this.mRule)) {
            toastAndFinish();
            return true;
        }
        return false;
    }

    private void showDeleteRuleDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this.mContext).setMessage(getString(R.string.zen_mode_delete_rule_confirmation, new Object[]{this.mRule.getName()})).setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener) null).setPositiveButton(R.string.zen_mode_delete_rule_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog2, int which) {
                MetricsLogger.action(ZenModeRuleSettingsBase.this.mContext, 175);
                ZenModeRuleSettingsBase.this.mDeleting = true;
                ZenModeRuleSettingsBase.this.removeZenRule(ZenModeRuleSettingsBase.this.mId);
            }
        }).show();
        View messageView = dialog.findViewById(android.R.id.message);
        if (messageView != null) {
            messageView.setTextDirection(5);
        }
    }

    private void toastAndFinish() {
        if (!this.mDeleting) {
            Toast.makeText(this.mContext, R.string.zen_mode_rule_not_found_text, 0).show();
        }
        getActivity().finish();
    }

    private void updateRuleName() {
        getActivity().setTitle(this.mRule.getName());
        this.mRuleName.setSummary(this.mRule.getName());
    }

    private AutomaticZenRule getZenRule() {
        return NotificationManager.from(this.mContext).getAutomaticZenRule(this.mId);
    }

    private void updateControls() {
        this.mDisableListeners = true;
        updateRuleName();
        updateControlsInternal();
        this.mZenMode.setValue(Integer.toString(this.mRule.getInterruptionFilter()));
        if (this.mSwitchBar != null) {
            this.mSwitchBar.setChecked(this.mRule.isEnabled());
        }
        this.mDisableListeners = false;
    }
}
