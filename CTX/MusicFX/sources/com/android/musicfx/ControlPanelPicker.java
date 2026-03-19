package com.android.musicfx;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.Bundle;
import android.widget.ListView;
import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;
import com.android.musicfx.Compatibility;
import java.util.List;

public class ControlPanelPicker extends AlertActivity implements DialogInterface.OnClickListener, AlertController.AlertParams.OnPrepareListViewListener {
    private DialogInterface.OnClickListener mItemClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            ControlPanelPicker.this.mAlertParams.mCheckedItem = i;
        }
    };

    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        MatrixCursor matrixCursor = new MatrixCursor(new String[]{"_id", "title", "package", "name"});
        PackageManager packageManager = getPackageManager();
        List<ResolveInfo> listQueryIntentActivities = packageManager.queryIntentActivities(new Intent("android.media.action.DISPLAY_AUDIO_EFFECT_CONTROL_PANEL"), 512);
        SharedPreferences sharedPreferences = getSharedPreferences("musicfx", 0);
        String string = sharedPreferences.getString("defaultpanelpackage", null);
        String string2 = sharedPreferences.getString("defaultpanelname", null);
        int i = -1;
        int i2 = 0;
        for (ResolveInfo resolveInfo : listQueryIntentActivities) {
            if (!resolveInfo.activityInfo.name.equals(Compatibility.Redirector.class.getName())) {
                matrixCursor.addRow(new Object[]{0, packageManager.getApplicationLabel(resolveInfo.activityInfo.applicationInfo), resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name});
                i++;
                if (resolveInfo.activityInfo.name.equals(string2) && resolveInfo.activityInfo.packageName.equals(string) && resolveInfo.activityInfo.enabled) {
                    i2 = i;
                }
            }
        }
        AlertController.AlertParams alertParams = this.mAlertParams;
        alertParams.mCursor = matrixCursor;
        alertParams.mOnClickListener = this.mItemClickListener;
        alertParams.mLabelColumn = "title";
        alertParams.mIsSingleChoice = true;
        alertParams.mPositiveButtonText = getString(android.R.string.ok);
        alertParams.mPositiveButtonListener = this;
        alertParams.mNegativeButtonText = getString(android.R.string.cancel);
        alertParams.mOnPrepareListViewListener = this;
        alertParams.mTitle = getString(R.string.picker_title);
        alertParams.mCheckedItem = i2;
        setupAlert();
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        if (i == -1) {
            Intent intent = new Intent((Context) this, (Class<?>) Compatibility.Service.class);
            Cursor cursor = this.mAlertParams.mCursor;
            cursor.moveToPosition(this.mAlertParams.mCheckedItem);
            intent.putExtra("defPackage", cursor.getString(2));
            intent.putExtra("defName", cursor.getString(3));
            startService(intent);
        }
    }

    public void onPrepareListView(ListView listView) {
    }
}
