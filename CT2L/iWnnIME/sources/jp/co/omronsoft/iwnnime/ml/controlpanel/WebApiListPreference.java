package jp.co.omronsoft.iwnnime.ml.controlpanel;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.AttributeSet;
import android.widget.Toast;
import com.android.common.speech.LoggingEvents;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import jp.co.omronsoft.iwnnime.ml.R;
import jp.co.omronsoft.iwnnime.ml.WebAPIWnnEngine;

public class WebApiListPreference extends MultiChoicePreference {
    public static final int MAX_ADD_WEBAPI_LIST = 5;

    public WebApiListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        PackageManager pm = getContext().getPackageManager();
        Intent intent = new Intent(WebAPIWnnEngine.WEBAPI_ACTION_CODE);
        List<ResolveInfo> resolveInfo = pm.queryBroadcastReceivers(intent, 0);
        Collections.sort(resolveInfo, new ResolveInfo.DisplayNameComparator(pm));
        int infoSize = resolveInfo.size();
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
                entryValues[i] = actInfo.name;
            } else {
                entryValues[i] = LoggingEvents.EXTRA_CALLING_APP_NAME;
            }
        }
        setEntries(entries);
        setEntryValues(entryValues);
        super.onPrepareDialogBuilder(builder);
    }

    public static boolean isEnableWebApi(Context context) {
        PackageManager pm = context.getPackageManager();
        Intent intent = new Intent(WebAPIWnnEngine.WEBAPI_ACTION_CODE);
        List<ResolveInfo> resolveInfo = pm.queryBroadcastReceivers(intent, 0);
        return resolveInfo.size() != 0;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        Set<String> oldValue = getValues();
        super.onDialogClosed(positiveResult);
        final Set<String> selectedValue = getValues();
        if (positiveResult) {
            if ((oldValue == null || oldValue.isEmpty()) && selectedValue != null && !selectedValue.isEmpty()) {
                setValues(new HashSet());
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext()).setTitle(R.string.ti_preference_attention_dialog_title_txt).setIcon(android.R.drawable.ic_dialog_alert).setMessage(R.string.ti_preference_webapi_dialog_message_txt).setPositiveButton(R.string.ti_dialog_button_ok_txt, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        WebApiListPreference.this.setValues(selectedValue);
                    }
                }).setNegativeButton(R.string.ti_dialog_button_cancel_txt, (DialogInterface.OnClickListener) null);
                AlertDialog optionsDialog = builder.create();
                optionsDialog.show();
            }
        }
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
