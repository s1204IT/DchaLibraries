package jp.co.omronsoft.iwnnime.ml;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MultiChoicePreference extends DialogPreference {
    private boolean[] mChecked;
    private CharSequence[] mEntries;
    private CharSequence[] mEntryValues;
    private Set<String> mValues;

    public MultiChoicePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, new int[]{android.R.attr.entries, android.R.attr.entryValues}, 0, 0);
        this.mEntries = a.getTextArray(0);
        this.mEntryValues = a.getTextArray(1);
        a.recycle();
    }

    public void setEntries(CharSequence[] entries) {
        this.mEntries = (CharSequence[]) entries.clone();
    }

    public void setEntryValues(CharSequence[] entryValues) {
        this.mEntryValues = (CharSequence[]) entryValues.clone();
    }

    public void setValues(Set<String> values) {
        this.mValues = new HashSet(values);
        SharedPreferences.Editor editor = getEditor();
        if (editor != null) {
            editor.putStringSet(getKey(), values);
            editor.apply();
        }
    }

    public Set<String> getValues() {
        if (this.mValues == null) {
            return null;
        }
        return new HashSet(this.mValues);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
        if (this.mEntryValues != null) {
            this.mChecked = new boolean[this.mEntryValues.length];
            List<CharSequence> list = Arrays.asList(this.mEntryValues);
            SharedPreferences sharedPref = getSharedPreferences();
            if (sharedPref != null) {
                this.mValues = sharedPref.getStringSet(getKey(), null);
                if (this.mValues != null) {
                    for (String value : this.mValues) {
                        int index = list.indexOf(value);
                        if (index != -1) {
                            this.mChecked[index] = true;
                        }
                    }
                }
                builder.setMultiChoiceItems(this.mEntries, this.mChecked, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        MultiChoicePreference.this.onMultiChoiceItemsClick(which, isChecked);
                    }
                });
            }
        }
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (this.mEntryValues != null && positiveResult) {
            Set<String> newValues = new HashSet<>();
            for (int i = 0; i < this.mEntryValues.length; i++) {
                if (this.mChecked[i]) {
                    newValues.add(this.mEntryValues[i].toString());
                }
            }
            setValues(newValues);
        }
    }

    public void onMultiChoiceItemsClick(int which, boolean isChecked) {
        if (this.mChecked != null) {
            this.mChecked[which] = isChecked;
        }
    }

    public int getCheckedCount() {
        int checkedCount = 0;
        if (this.mChecked != null) {
            for (int i = 0; i < this.mChecked.length; i++) {
                if (this.mChecked[i]) {
                    checkedCount++;
                }
            }
        }
        return checkedCount;
    }
}
