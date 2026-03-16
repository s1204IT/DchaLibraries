package com.android.settings.sim;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.SearchIndexableResource;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import com.android.internal.telephony.Dsds;
import com.android.internal.telephony.PhoneConstants;
import com.android.settings.R;
import com.android.settings.RestrictedSettingsFragment;
import com.android.settings.Utils;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import java.util.ArrayList;
import java.util.List;

public class SimSettings extends RestrictedSettingsFragment implements Indexable {
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean enabled) {
            ArrayList<SearchIndexableResource> result = new ArrayList<>();
            if (Utils.showSimCardTile(context)) {
                SearchIndexableResource sir = new SearchIndexableResource(context);
                sir.xmlResId = R.xml.sim_settings;
                result.add(sir);
            }
            return result;
        }
    };
    private List<SubscriptionInfo> mAvailableSubInfos;
    private SubscriptionInfo mCalls;
    private SubscriptionInfo mCellularData;
    private SubscriptionInfo mSMS;
    private List<SubscriptionInfo> mSelectableSubInfos;
    private PreferenceScreen mSimCards;
    private SimEnabler[] mSimEnabler;
    private List<SubscriptionInfo> mSubInfoList;
    private SubscriptionManager mSubscriptionManager;

    public SimSettings() {
        super("no_config_sim");
        this.mAvailableSubInfos = null;
        this.mSubInfoList = null;
        this.mSelectableSubInfos = null;
        this.mCellularData = null;
        this.mCalls = null;
        this.mSMS = null;
        this.mSimCards = null;
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mSubscriptionManager = SubscriptionManager.from(getActivity());
        if (this.mSubInfoList == null) {
            this.mSubInfoList = this.mSubscriptionManager.getActiveSubscriptionInfoList();
        }
        createPreferences();
        updateAllOptions();
        SimBootReceiver.cancelNotification(getActivity());
    }

    private void createPreferences() {
        TelephonyManager tm = (TelephonyManager) getActivity().getSystemService("phone");
        addPreferencesFromResource(R.xml.sim_settings);
        this.mSimCards = (PreferenceScreen) findPreference("sim_cards");
        int numSlots = tm.getSimCount();
        this.mAvailableSubInfos = new ArrayList(numSlots);
        this.mSelectableSubInfos = new ArrayList();
        for (int i = 0; i < numSlots; i++) {
            SubscriptionInfo sir = Utils.findRecordBySlotId(getActivity(), i);
            SimPreference simPreference = new SimPreference(getActivity(), sir, i);
            simPreference.setOrder(i - numSlots);
            this.mSimCards.addPreference(simPreference);
            this.mAvailableSubInfos.add(sir);
            if (sir != null) {
                this.mSelectableSubInfos.add(sir);
            }
        }
        updateActivitesCategory();
        CreateSimFuncCategory();
    }

    private void updateAvailableSubInfos() {
        TelephonyManager tm = (TelephonyManager) getActivity().getSystemService("phone");
        int numSlots = tm.getSimCount();
        this.mAvailableSubInfos = new ArrayList(numSlots);
        for (int i = 0; i < numSlots; i++) {
            SubscriptionInfo sir = Utils.findRecordBySlotId(getActivity(), i);
            this.mAvailableSubInfos.add(sir);
            if (sir != null) {
            }
        }
    }

    private void CreateSimFuncCategory() {
        TelephonyManager tm = (TelephonyManager) getActivity().getSystemService("phone");
        int numSlots = tm.getSimCount();
        if (this.mSimEnabler == null) {
            this.mSimEnabler = new SimEnabler[numSlots];
        }
        PreferenceCategory simEnable = (PreferenceCategory) findPreference("sim_function");
        simEnable.removeAll();
        for (int i = 0; i < numSlots; i++) {
            SwitchPreference enablePref = new MySwitchPreference(getActivity());
            enablePref.setOrder(i);
            simEnable.addPreference(enablePref);
            this.mSimEnabler[i] = new SimEnabler(getActivity(), enablePref, null, i);
        }
    }

    private void updateAllOptions() {
        updateSimSlotValues();
        updateActivitesCategory();
    }

    private void updateSimSlotValues() {
        this.mSubscriptionManager.getAllSubscriptionInfoList();
        int prefSize = this.mSimCards.getPreferenceCount();
        for (int i = 0; i < prefSize; i++) {
            Preference pref = this.mSimCards.getPreference(i);
            if (pref instanceof SimPreference) {
                ((SimPreference) pref).update();
            }
        }
    }

    private void updateActivitesCategory() {
        updateCellularDataValues();
        updateCallValues();
        updateMasterValues();
    }

    private void updateMasterValues() {
        Preference masterPref = findPreference("sim_master");
        SubscriptionInfo sir = Utils.findRecordBySubId(getActivity(), Dsds.getMasterSubId());
        masterPref.setTitle(R.string.master_title);
        if (sir != null) {
            masterPref.setSummary(sir.getDisplayName());
        } else if (sir == null) {
            masterPref.setSummary(R.string.sim_selection_required_pref);
        }
        int slaveId = Dsds.isSim2Master() ? PhoneConstants.SimId.SIM1.ordinal() : PhoneConstants.SimId.SIM2.ordinal();
        Dsds.MasterUiccPref slavePref = Dsds.getMasterUiccPref(getActivity(), slaveId);
        masterPref.setEnabled(this.mSelectableSubInfos.size() > 1 && slavePref != Dsds.MasterUiccPref.MASTER_UICC_RESTRICTED);
    }

    private void updateCellularDataValues() {
        Preference simPref = findPreference("sim_cellular_data");
        Activity activity = getActivity();
        SubscriptionManager subscriptionManager = this.mSubscriptionManager;
        SubscriptionInfo sir = Utils.findRecordBySubId(activity, SubscriptionManager.getDefaultDataSubId());
        simPref.setTitle(R.string.cellular_data_title);
        if (sir != null) {
            simPref.setSummary(sir.getDisplayName());
        } else if (sir == null) {
            simPref.setSummary(R.string.sim_selection_required_pref);
        }
        simPref.setEnabled(this.mSelectableSubInfos.size() >= 1);
    }

    private void updateCallValues() {
        Preference simPref = findPreference("sim_calls");
        TelecomManager telecomManager = TelecomManager.from(getActivity());
        PhoneAccountHandle phoneAccount = telecomManager.getUserSelectedOutgoingPhoneAccount();
        simPref.setTitle(R.string.calls_title);
        simPref.setSummary(phoneAccount == null ? getResources().getString(R.string.sim_calls_ask_first_prefs_title) : (String) telecomManager.getPhoneAccount(phoneAccount).getLabel());
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mSubInfoList = this.mSubscriptionManager.getActiveSubscriptionInfoList();
        updateAvailableSubInfos();
        updateAllOptions();
        TelephonyManager tm = (TelephonyManager) getActivity().getSystemService("phone");
        int numSlots = tm.getSimCount();
        for (int i = 0; i < numSlots; i++) {
            if (this.mSimEnabler != null && this.mSimEnabler.length > i && this.mSimEnabler[i] != null) {
                this.mSimEnabler[i].resume();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        TelephonyManager tm = (TelephonyManager) getActivity().getSystemService("phone");
        int numSlots = tm.getSimCount();
        for (int i = 0; i < numSlots; i++) {
            if (this.mSimEnabler != null && this.mSimEnabler.length > i && this.mSimEnabler[i] != null) {
                this.mSimEnabler[i].pause();
            }
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        Context context = getActivity();
        Intent intent = new Intent(context, (Class<?>) SimDialogActivity.class);
        intent.addFlags(268435456);
        if (preference instanceof SimPreference) {
            ((SimPreference) preference).createEditDialog((SimPreference) preference);
        } else if (findPreference("sim_cellular_data") == preference) {
            intent.putExtra(SimDialogActivity.DIALOG_TYPE_KEY, 0);
            context.startActivity(intent);
        } else if (findPreference("sim_calls") == preference) {
            intent.putExtra(SimDialogActivity.DIALOG_TYPE_KEY, 1);
            context.startActivity(intent);
        } else if (findPreference("sim_master") == preference) {
            intent.putExtra(SimDialogActivity.DIALOG_TYPE_KEY, 10);
            context.startActivity(intent);
        }
        return true;
    }

    private class SimPreference extends Preference {
        private String[] mColorStrings;
        Context mContext;
        private int mSlotId;
        private SubscriptionInfo mSubInfoRecord;
        private int[] mTintArr;
        private int mTintSelectorPos;

        public SimPreference(Context context, SubscriptionInfo subInfoRecord, int slotId) {
            super(context);
            this.mContext = context;
            this.mSubInfoRecord = subInfoRecord;
            this.mSlotId = slotId;
            setKey("sim" + this.mSlotId);
            update();
            this.mTintArr = context.getResources().getIntArray(android.R.array.config_allowedGlobalInstantAppSettings);
            this.mColorStrings = context.getResources().getStringArray(R.array.color_picker);
            this.mTintSelectorPos = 0;
        }

        public void update() {
            Resources res = SimSettings.this.getResources();
            setTitle(String.format(SimSettings.this.getResources().getString(R.string.sim_editor_title), Integer.valueOf(this.mSlotId + 1)));
            if (this.mSubInfoRecord != null) {
                if (!TextUtils.isEmpty(SimSettings.this.getPhoneNumber(this.mSubInfoRecord))) {
                    setSummary(((Object) this.mSubInfoRecord.getDisplayName()) + " - " + SimSettings.this.getPhoneNumber(this.mSubInfoRecord));
                    setEnabled(true);
                } else {
                    setSummary(this.mSubInfoRecord.getDisplayName());
                }
                setIcon(new BitmapDrawable(res, this.mSubInfoRecord.createIconBitmap(this.mContext)));
                return;
            }
            setSummary(R.string.sim_slot_empty);
            setFragment(null);
            setEnabled(false);
        }

        public void createEditDialog(SimPreference simPref) {
            Resources res = SimSettings.this.getResources();
            AlertDialog.Builder builder = new AlertDialog.Builder(SimSettings.this.getActivity());
            final View dialogLayout = SimSettings.this.getActivity().getLayoutInflater().inflate(R.layout.multi_sim_dialog, (ViewGroup) null);
            builder.setView(dialogLayout);
            EditText nameText = (EditText) dialogLayout.findViewById(R.id.sim_name);
            nameText.setText(this.mSubInfoRecord.getDisplayName());
            final Spinner tintSpinner = (Spinner) dialogLayout.findViewById(R.id.spinner);
            SelectColorAdapter adapter = new SelectColorAdapter(getContext(), R.layout.settings_color_picker_item, this.mColorStrings);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            tintSpinner.setAdapter((SpinnerAdapter) adapter);
            int i = 0;
            while (true) {
                if (i >= this.mTintArr.length) {
                    break;
                }
                if (this.mTintArr[i] != this.mSubInfoRecord.getIconTint()) {
                    i++;
                } else {
                    tintSpinner.setSelection(i);
                    this.mTintSelectorPos = i;
                    break;
                }
            }
            tintSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                    tintSpinner.setSelection(pos);
                    SimPreference.this.mTintSelectorPos = pos;
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
            TextView numberView = (TextView) dialogLayout.findViewById(R.id.number);
            String rawNumber = SimSettings.this.getPhoneNumber(this.mSubInfoRecord);
            if (TextUtils.isEmpty(rawNumber)) {
                numberView.setText(res.getString(android.R.string.unknownName));
            } else {
                numberView.setText(PhoneNumberUtils.formatNumber(rawNumber));
            }
            TextView carrierView = (TextView) dialogLayout.findViewById(R.id.carrier);
            carrierView.setText(this.mSubInfoRecord.getCarrierName());
            builder.setTitle(String.format(res.getString(R.string.sim_editor_title), Integer.valueOf(this.mSubInfoRecord.getSimSlotIndex() + 1)));
            builder.setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int whichButton) {
                    EditText nameText2 = (EditText) dialogLayout.findViewById(R.id.sim_name);
                    String displayName = nameText2.getText().toString();
                    int subId = SimPreference.this.mSubInfoRecord.getSubscriptionId();
                    SimPreference.this.mSubInfoRecord.setDisplayName(displayName);
                    SimSettings.this.mSubscriptionManager.setDisplayName(displayName, subId, 2L);
                    Utils.findRecordBySubId(SimSettings.this.getActivity(), subId).setDisplayName(displayName);
                    int tintSelected = tintSpinner.getSelectedItemPosition();
                    int subscriptionId = SimPreference.this.mSubInfoRecord.getSubscriptionId();
                    int tint = SimPreference.this.mTintArr[tintSelected];
                    SimPreference.this.mSubInfoRecord.setIconTint(tint);
                    SimSettings.this.mSubscriptionManager.setIconTint(tint, subscriptionId);
                    Utils.findRecordBySubId(SimSettings.this.getActivity(), subscriptionId).setIconTint(tint);
                    SimSettings.this.updateAllOptions();
                    SimPreference.this.update();
                }
            });
            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int whichButton) {
                    dialog.dismiss();
                }
            });
            builder.create().show();
        }

        private class SelectColorAdapter extends ArrayAdapter<CharSequence> {
            private Context mContext;
            private int mResId;

            public SelectColorAdapter(Context context, int resource, String[] arr) {
                super(context, resource, arr);
                this.mContext = context;
                this.mResId = resource;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View rowView;
                ViewHolder holder;
                LayoutInflater inflater = (LayoutInflater) this.mContext.getSystemService("layout_inflater");
                Resources res = SimSettings.this.getResources();
                int iconSize = res.getDimensionPixelSize(R.dimen.color_swatch_size);
                int strokeWidth = res.getDimensionPixelSize(R.dimen.color_swatch_stroke_width);
                if (convertView == null) {
                    rowView = inflater.inflate(this.mResId, (ViewGroup) null);
                    holder = new ViewHolder();
                    ShapeDrawable drawable = new ShapeDrawable(new OvalShape());
                    drawable.setIntrinsicHeight(iconSize);
                    drawable.setIntrinsicWidth(iconSize);
                    drawable.getPaint().setStrokeWidth(strokeWidth);
                    holder.label = (TextView) rowView.findViewById(R.id.color_text);
                    holder.icon = (ImageView) rowView.findViewById(R.id.color_icon);
                    holder.swatch = drawable;
                    rowView.setTag(holder);
                } else {
                    rowView = convertView;
                    holder = (ViewHolder) rowView.getTag();
                }
                holder.label.setText(getItem(position));
                holder.swatch.getPaint().setColor(SimPreference.this.mTintArr[position]);
                holder.swatch.getPaint().setStyle(Paint.Style.FILL_AND_STROKE);
                holder.icon.setVisibility(0);
                holder.icon.setImageDrawable(holder.swatch);
                return rowView;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View rowView = getView(position, convertView, parent);
                ViewHolder holder = (ViewHolder) rowView.getTag();
                if (SimPreference.this.mTintSelectorPos == position) {
                    holder.swatch.getPaint().setStyle(Paint.Style.FILL_AND_STROKE);
                } else {
                    holder.swatch.getPaint().setStyle(Paint.Style.STROKE);
                }
                holder.icon.setVisibility(0);
                return rowView;
            }

            private class ViewHolder {
                ImageView icon;
                TextView label;
                ShapeDrawable swatch;

                private ViewHolder() {
                }
            }
        }
    }

    private String getPhoneNumber(SubscriptionInfo info) {
        TelephonyManager tm = (TelephonyManager) getActivity().getSystemService("phone");
        return tm.getLine1NumberForSubscriber(info.getSubscriptionId());
    }
}
