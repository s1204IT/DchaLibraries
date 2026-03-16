package com.android.bluetooth.opp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import com.android.bluetooth.R;

public class BluetoothOppLiveFolder extends Activity {
    public static final Uri CONTENT_URI = Uri.parse("content://com.android.bluetooth.opp/live_folders/received");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        String action = intent.getAction();
        if ("android.intent.action.CREATE_LIVE_FOLDER".equals(action)) {
            setResult(-1, createLiveFolder(this, CONTENT_URI, getString(R.string.btopp_live_folder), R.drawable.ic_launcher_folder_bluetooth));
        } else {
            setResult(0);
        }
        finish();
    }

    private static Intent createLiveFolder(Context context, Uri uri, String name, int icon) {
        Intent intent = new Intent();
        intent.setDataAndNormalize(uri);
        intent.putExtra("android.intent.extra.livefolder.BASE_INTENT", new Intent(Constants.ACTION_OPEN, BluetoothShare.CONTENT_URI));
        intent.putExtra("android.intent.extra.livefolder.NAME", name);
        intent.putExtra("android.intent.extra.livefolder.ICON", Intent.ShortcutIconResource.fromContext(context, icon));
        intent.putExtra("android.intent.extra.livefolder.DISPLAY_MODE", 2);
        return intent;
    }
}
