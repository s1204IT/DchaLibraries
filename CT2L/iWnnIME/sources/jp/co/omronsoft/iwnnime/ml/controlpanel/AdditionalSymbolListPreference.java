package jp.co.omronsoft.iwnnime.ml.controlpanel;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.AttributeSet;
import android.widget.Toast;
import com.android.common.speech.LoggingEvents;
import java.util.List;
import jp.co.omronsoft.iwnnime.ml.AdditionalSymbolList;
import jp.co.omronsoft.iwnnime.ml.R;

public class AdditionalSymbolListPreference extends MultiChoicePreference {
    public static final int MAX_ADD_SYMBOL_LIST = 5;

    public AdditionalSymbolListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        PackageManager pm = getContext().getPackageManager();
        List<ResolveInfo> resolveInfo = AdditionalSymbolList.getAdditionalSymbolListInfo(getContext());
        int infoSize = resolveInfo.size();
        if (infoSize > 0) {
            CharSequence[] entries = new CharSequence[infoSize];
            CharSequence[] entryValues = new CharSequence[infoSize];
            for (int i = 0; i < infoSize; i++) {
                ResolveInfo info = resolveInfo.get(i);
                ActivityInfo actInfo = info.activityInfo;
                CharSequence label = info.loadLabel(pm);
                if (label == null) {
                    if (actInfo != null) {
                        label = actInfo.name;
                    } else {
                        label = LoggingEvents.EXTRA_CALLING_APP_NAME;
                    }
                }
                entries[i] = label;
                if (actInfo != null) {
                    entryValues[i] = actInfo.packageName;
                } else {
                    entryValues[i] = LoggingEvents.EXTRA_CALLING_APP_NAME;
                }
            }
            setEntries(entries);
            setEntryValues(entryValues);
        }
        super.onPrepareDialogBuilder(builder);
    }

    @Override
    public void onMultiChoiceItemsClick(DialogInterface dialog, int which, boolean isChecked) {
        super.onMultiChoiceItemsClick(dialog, which, isChecked);
        if (getCheckedCount() > 5 && isChecked) {
            ((AlertDialog) dialog).getListView().setItemChecked(which, false);
            setChecked(which, false);
            Toast.makeText(getContext(), R.string.ti_preference_max_selected_error_txt, 0).show();
        }
    }
}
