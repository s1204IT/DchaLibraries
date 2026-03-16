package jp.co.omronsoft.iwnnime.ml.controlpanel;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.widget.ListView;
import java.util.HashMap;
import jp.co.omronsoft.iwnnime.ml.R;
import jp.co.omronsoft.iwnnime.ml.WnnUtility;

public class Keyboard50KeyTypeListPreference extends KeyboardTypeBaseListPreference {
    public static final HashMap<Integer, Integer> ICON_TABLE_50KEY_TYPE = new HashMap<Integer, Integer>() {
        {
            put(0, Integer.valueOf(R.drawable.keyboard_type_preview_50key_vertical_right));
            put(1, Integer.valueOf(R.drawable.keyboard_type_preview_50key_vertical_left));
            put(2, Integer.valueOf(R.drawable.keyboard_type_preview_50key_horizontal));
        }
    };
    public static final String PREF_50KEY_TYPE = "keyboard_50key_type";
    private SharedPreferences m50KeyTypeCommonPref;

    public Keyboard50KeyTypeListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.m50KeyTypeCommonPref = null;
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        CharSequence[] itemTitles = getEntries();
        CharSequence[] entryValues = getEntryValues();
        this.m50KeyTypeCommonPref = getSharedPreferences();
        if (itemTitles == null || entryValues == null || this.m50KeyTypeCommonPref == null) {
            throw new IllegalStateException("ListPreference requires an entries array and an entryValues array.");
        }
        String key = getKey();
        Context context = getContext();
        int length = entryValues.length;
        int[] itemValues = new int[length];
        for (int i = 0; i < length; i++) {
            itemValues[i] = Integer.parseInt(entryValues[i].toString());
        }
        Resources res = context.getResources();
        String defaultValue = res.getString(R.string.keyboard_50key_type_default_value);
        this.mCurrentSelectValue = this.m50KeyTypeCommonPref.getString(key, defaultValue);
        int clickedDialogEntryIndex = findIndexOfValue(this.mCurrentSelectValue);
        this.mWnnArrayAdapter = WnnUtility.makeTitleListWithIcon(context, itemTitles, itemValues, ICON_TABLE_50KEY_TYPE, clickedDialogEntryIndex);
        ListView listView = WnnUtility.makeSingleChoiceListView(context, this.mWnnArrayAdapter, clickedDialogEntryIndex, this.listener);
        builder.setView(listView);
        builder.setPositiveButton(R.string.ti_dialog_button_ok_txt, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Keyboard50KeyTypeListPreference.this.onDialogClosed(true);
            }
        });
    }
}
