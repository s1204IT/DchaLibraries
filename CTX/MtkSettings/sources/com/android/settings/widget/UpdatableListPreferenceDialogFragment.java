package com.android.settings.widget;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.v14.preference.PreferenceDialogFragment;
import android.support.v7.preference.ListPreference;
import android.widget.ArrayAdapter;
import com.android.internal.R;
import com.android.settingslib.core.instrumentation.Instrumentable;
import java.util.ArrayList;

public class UpdatableListPreferenceDialogFragment extends PreferenceDialogFragment implements Instrumentable {
    private ArrayAdapter mAdapter;
    private int mClickedDialogEntryIndex;
    private ArrayList<CharSequence> mEntries;
    private CharSequence[] mEntryValues;
    private int mMetricsCategory = 0;

    public static UpdatableListPreferenceDialogFragment newInstance(String str, int i) {
        UpdatableListPreferenceDialogFragment updatableListPreferenceDialogFragment = new UpdatableListPreferenceDialogFragment();
        Bundle bundle = new Bundle(1);
        bundle.putString("key", str);
        bundle.putInt("metrics_category_key", i);
        updatableListPreferenceDialogFragment.setArguments(bundle);
        return updatableListPreferenceDialogFragment;
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mMetricsCategory = getArguments().getInt("metrics_category_key", 0);
        if (bundle != null) {
            this.mClickedDialogEntryIndex = bundle.getInt("UpdatableListPreferenceDialogFragment.index", 0);
            this.mEntries = bundle.getCharSequenceArrayList("UpdatableListPreferenceDialogFragment.entries");
            this.mEntryValues = bundle.getCharSequenceArray("UpdatableListPreferenceDialogFragment.entryValues");
        } else {
            this.mEntries = new ArrayList<>();
            setPreferenceData(getListPreference());
        }
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putInt("UpdatableListPreferenceDialogFragment.index", this.mClickedDialogEntryIndex);
        bundle.putCharSequenceArrayList("UpdatableListPreferenceDialogFragment.entries", this.mEntries);
        bundle.putCharSequenceArray("UpdatableListPreferenceDialogFragment.entryValues", this.mEntryValues);
    }

    @Override
    public void onDialogClosed(boolean z) {
        ListPreference listPreference = getListPreference();
        if (z && this.mClickedDialogEntryIndex >= 0) {
            String string = this.mEntryValues[this.mClickedDialogEntryIndex].toString();
            if (listPreference.callChangeListener(string)) {
                listPreference.setValue(string);
            }
        }
    }

    void setAdapter(ArrayAdapter arrayAdapter) {
        this.mAdapter = arrayAdapter;
    }

    void setEntries(ArrayList<CharSequence> arrayList) {
        this.mEntries = arrayList;
    }

    ArrayAdapter getAdapter() {
        return this.mAdapter;
    }

    void setMetricsCategory(Bundle bundle) {
        this.mMetricsCategory = bundle.getInt("metrics_category_key", 0);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
        TypedArray typedArrayObtainStyledAttributes = getContext().obtainStyledAttributes(null, R.styleable.AlertDialog, android.R.attr.alertDialogStyle, 0);
        this.mAdapter = new ArrayAdapter(getContext(), typedArrayObtainStyledAttributes.getResourceId(21, android.R.layout.select_dialog_singlechoice), this.mEntries);
        builder.setSingleChoiceItems(this.mAdapter, this.mClickedDialogEntryIndex, new DialogInterface.OnClickListener() {
            @Override
            public final void onClick(DialogInterface dialogInterface, int i) {
                UpdatableListPreferenceDialogFragment.lambda$onPrepareDialogBuilder$0(this.f$0, dialogInterface, i);
            }
        });
        builder.setPositiveButton((CharSequence) null, (DialogInterface.OnClickListener) null);
        typedArrayObtainStyledAttributes.recycle();
    }

    public static void lambda$onPrepareDialogBuilder$0(UpdatableListPreferenceDialogFragment updatableListPreferenceDialogFragment, DialogInterface dialogInterface, int i) {
        updatableListPreferenceDialogFragment.mClickedDialogEntryIndex = i;
        updatableListPreferenceDialogFragment.onClick(dialogInterface, -1);
        dialogInterface.dismiss();
    }

    @Override
    public int getMetricsCategory() {
        return this.mMetricsCategory;
    }

    private ListPreference getListPreference() {
        return (ListPreference) getPreference();
    }

    private void setPreferenceData(ListPreference listPreference) {
        this.mEntries.clear();
        this.mClickedDialogEntryIndex = listPreference.findIndexOfValue(listPreference.getValue());
        for (CharSequence charSequence : listPreference.getEntries()) {
            this.mEntries.add(charSequence);
        }
        this.mEntryValues = listPreference.getEntryValues();
    }

    public void onListPreferenceUpdated(ListPreference listPreference) {
        if (this.mAdapter != null) {
            setPreferenceData(listPreference);
            this.mAdapter.notifyDataSetChanged();
        }
    }
}
