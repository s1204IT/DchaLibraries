package jp.co.omronsoft.iwnnime.ml.controlpanel;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Resources;
import android.preference.ListPreference;
import android.util.AttributeSet;
import jp.co.omronsoft.iwnnime.ml.R;

public class AutoCursorMovementListPreference extends ListPreference {
    public AutoCursorMovementListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        Resources res = getContext().getResources();
        String[] entries = res.getStringArray(R.array.auto_cursor_movement);
        String[] entryValues = res.getStringArray(R.array.auto_cursor_movement_id);
        setEntries(entries);
        setEntryValues(entryValues);
        super.onPrepareDialogBuilder(builder);
    }
}
