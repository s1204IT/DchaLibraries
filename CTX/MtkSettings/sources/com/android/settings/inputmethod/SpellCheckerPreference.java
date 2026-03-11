package com.android.settings.inputmethod;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.TextUtils;
import android.view.View;
import android.view.textservice.SpellCheckerInfo;
import com.android.settings.CustomListPreference;
import com.android.settings.R;

class SpellCheckerPreference extends CustomListPreference {
    private Intent mIntent;
    private final SpellCheckerInfo[] mScis;

    public SpellCheckerPreference(Context context, SpellCheckerInfo[] spellCheckerInfoArr) {
        super(context, null);
        this.mScis = spellCheckerInfoArr;
        setWidgetLayoutResource(R.layout.preference_widget_gear);
        CharSequence[] charSequenceArr = new CharSequence[spellCheckerInfoArr.length];
        CharSequence[] charSequenceArr2 = new CharSequence[spellCheckerInfoArr.length];
        for (int i = 0; i < spellCheckerInfoArr.length; i++) {
            charSequenceArr[i] = spellCheckerInfoArr[i].loadLabel(context.getPackageManager());
            charSequenceArr2[i] = String.valueOf(i);
        }
        setEntries(charSequenceArr);
        setEntryValues(charSequenceArr2);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder, DialogInterface.OnClickListener onClickListener) {
        builder.setTitle(R.string.choose_spell_checker);
        builder.setSingleChoiceItems(getEntries(), findIndexOfValue(getValue()), onClickListener);
    }

    public void setSelected(SpellCheckerInfo spellCheckerInfo) {
        if (spellCheckerInfo == null) {
            setValue(null);
            return;
        }
        for (int i = 0; i < this.mScis.length; i++) {
            if (this.mScis[i].getId().equals(spellCheckerInfo.getId())) {
                setValueIndex(i);
                return;
            }
        }
    }

    @Override
    public void setValue(String str) {
        super.setValue(str);
        int i = str != null ? Integer.parseInt(str) : -1;
        if (i == -1) {
            this.mIntent = null;
            return;
        }
        SpellCheckerInfo spellCheckerInfo = this.mScis[i];
        String settingsActivity = spellCheckerInfo.getSettingsActivity();
        if (TextUtils.isEmpty(settingsActivity)) {
            this.mIntent = null;
        } else {
            this.mIntent = new Intent("android.intent.action.MAIN");
            this.mIntent.setClassName(spellCheckerInfo.getPackageName(), settingsActivity);
        }
    }

    @Override
    public boolean callChangeListener(Object obj) {
        return super.callChangeListener(obj != null ? this.mScis[Integer.parseInt((String) obj)] : null);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
        super.onBindViewHolder(preferenceViewHolder);
        View viewFindViewById = preferenceViewHolder.findViewById(R.id.settings_button);
        viewFindViewById.setVisibility(this.mIntent != null ? 0 : 4);
        viewFindViewById.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SpellCheckerPreference.this.onSettingsButtonClicked();
            }
        });
    }

    public void onSettingsButtonClicked() {
        Context context = getContext();
        try {
            Intent intent = this.mIntent;
            if (intent != null) {
                context.startActivity(intent);
            }
        } catch (ActivityNotFoundException e) {
        }
    }
}
