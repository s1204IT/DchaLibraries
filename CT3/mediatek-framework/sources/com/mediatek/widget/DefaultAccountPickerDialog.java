package com.mediatek.widget;

import android.R;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import com.mediatek.widget.CustomAccountRemoteViews;
import java.util.ArrayList;
import java.util.List;

public class DefaultAccountPickerDialog extends DialogFragment {
    private static final String ACCOUNT_INFO_LIST_KEY = "AccountInfoList";
    private static final int NO_ITEM_SELECT = -1;
    public static final String TAG = "DefaultAccountPickerDialog";
    private int mSelection = -1;

    public static DefaultAccountPickerDialog build(Context context) {
        DefaultAccountPickerDialog dialogFragment = new DefaultAccountPickerDialog();
        return dialogFragment;
    }

    public DefaultAccountPickerDialog setData(List<CustomAccountRemoteViews.AccountInfo> data) {
        Bundle b = new Bundle();
        ArrayList<CustomAccountRemoteViews.AccountInfo> accountInfo = new ArrayList<>();
        for (CustomAccountRemoteViews.AccountInfo ai : data) {
            accountInfo.add(new CustomAccountRemoteViews.AccountInfo(ai.getIconId(), ai.getIcon(), ai.getLabel(), ai.getNumber(), ai.getIntent(), ai.isActive(), ai.isSimAccount()));
        }
        b.putParcelableArrayList(ACCOUNT_INFO_LIST_KEY, accountInfo);
        setArguments(b);
        return this;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(134545621);
        final DefaultAccountPickerAdapter adapter = new DefaultAccountPickerAdapter(getActivity());
        if (getArguments().containsKey(ACCOUNT_INFO_LIST_KEY)) {
            List<CustomAccountRemoteViews.AccountInfo> data = getArguments().getParcelableArrayList(ACCOUNT_INFO_LIST_KEY);
            adapter.setItemData(data);
            Log.d(TAG, "onCreateDialog get data form args : " + data);
        }
        this.mSelection = adapter.getActivePosition();
        builder.setSingleChoiceItems(adapter, this.mSelection, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                DefaultAccountPickerDialog.this.mSelection = which;
                adapter.setActiveStatus(DefaultAccountPickerDialog.this.mSelection);
                Log.d(DefaultAccountPickerDialog.TAG, "onClick position: " + DefaultAccountPickerDialog.this.mSelection);
                adapter.notifyDataSetChanged();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (DefaultAccountPickerDialog.this.mSelection == -1) {
                    Log.d(DefaultAccountPickerDialog.TAG, "--- No item is selected ---");
                    return;
                }
                Intent intent = adapter.getItem(DefaultAccountPickerDialog.this.mSelection).getIntent();
                if (intent == null || DefaultAccountPickerDialog.this.getActivity() == null) {
                    return;
                }
                Log.d(DefaultAccountPickerDialog.TAG, "sent broadcast: " + DefaultAccountPickerDialog.this.mSelection);
                DefaultAccountPickerDialog.this.getActivity().sendBroadcast(intent);
            }
        });
        return builder.create();
    }
}
