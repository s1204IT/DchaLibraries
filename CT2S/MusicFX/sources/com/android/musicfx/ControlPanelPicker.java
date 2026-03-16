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
        public void onClick(DialogInterface dialog, int which) {
            ControlPanelPicker.this.mAlertParams.mCheckedItem = which;
        }
    };

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String[] cols = {"_id", "title", "package", "name"};
        MatrixCursor c = new MatrixCursor(cols);
        PackageManager pmgr = getPackageManager();
        Intent i = new Intent("android.media.action.DISPLAY_AUDIO_EFFECT_CONTROL_PANEL");
        List<ResolveInfo> ris = pmgr.queryIntentActivities(i, 512);
        SharedPreferences pref = getSharedPreferences("musicfx", 0);
        String savedDefPackage = pref.getString("defaultpanelpackage", null);
        String savedDefName = pref.getString("defaultpanelname", null);
        int cnt = -1;
        int defpanelidx = 0;
        for (ResolveInfo foo : ris) {
            if (!foo.activityInfo.name.equals(Compatibility.Redirector.class.getName())) {
                CharSequence name = pmgr.getApplicationLabel(foo.activityInfo.applicationInfo);
                c.addRow(new Object[]{0, name, foo.activityInfo.packageName, foo.activityInfo.name});
                cnt++;
                if (foo.activityInfo.name.equals(savedDefName) && foo.activityInfo.packageName.equals(savedDefPackage) && foo.activityInfo.enabled) {
                    defpanelidx = cnt;
                }
            }
        }
        AlertController.AlertParams p = this.mAlertParams;
        p.mCursor = c;
        p.mOnClickListener = this.mItemClickListener;
        p.mLabelColumn = "title";
        p.mIsSingleChoice = true;
        p.mPositiveButtonText = getString(android.R.string.ok);
        p.mPositiveButtonListener = this;
        p.mNegativeButtonText = getString(android.R.string.cancel);
        p.mOnPrepareListViewListener = this;
        p.mTitle = getString(R.string.picker_title);
        p.mCheckedItem = defpanelidx;
        setupAlert();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == -1) {
            Intent updateIntent = new Intent((Context) this, (Class<?>) Compatibility.Service.class);
            Cursor c = this.mAlertParams.mCursor;
            c.moveToPosition(this.mAlertParams.mCheckedItem);
            updateIntent.putExtra("defPackage", c.getString(2));
            updateIntent.putExtra("defName", c.getString(3));
            startService(updateIntent);
        }
    }

    public void onPrepareListView(ListView listView) {
    }
}
