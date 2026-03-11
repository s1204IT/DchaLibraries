package com.android.settings.slices;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import com.android.settings.bluetooth.BluetoothSliceBuilder;
import com.android.settings.location.LocationSliceBuilder;
import com.android.settings.notification.ZenModeSliceBuilder;
import com.android.settings.wifi.WifiSliceBuilder;
import java.net.URISyntaxException;

public class SliceDeepLinkSpringBoard extends Activity {
    @Override
    protected void onCreate(Bundle bundle) {
        Intent contentIntent;
        super.onCreate(bundle);
        Uri data = getIntent().getData();
        if (data == null) {
            Log.e("DeeplinkSpringboard", "No data found");
            finish();
            return;
        }
        try {
            Intent intent = parse(data, getPackageName());
            if ("com.android.settings.action.VIEW_SLICE".equals(intent.getAction())) {
                Uri uri = Uri.parse(intent.getStringExtra("slice"));
                if (WifiSliceBuilder.WIFI_URI.equals(uri)) {
                    contentIntent = WifiSliceBuilder.getIntent(this);
                } else if (ZenModeSliceBuilder.ZEN_MODE_URI.equals(uri)) {
                    contentIntent = ZenModeSliceBuilder.getIntent(this);
                } else if (BluetoothSliceBuilder.BLUETOOTH_URI.equals(uri)) {
                    contentIntent = BluetoothSliceBuilder.getIntent(this);
                } else if (LocationSliceBuilder.LOCATION_URI.equals(uri)) {
                    contentIntent = LocationSliceBuilder.getIntent(this);
                } else {
                    contentIntent = SliceBuilderUtils.getContentIntent(this, new SlicesDatabaseAccessor(this).getSliceDataFromUri(uri));
                }
                startActivity(contentIntent);
            } else {
                startActivity(intent);
            }
            finish();
        } catch (IllegalStateException e) {
            Log.w("DeeplinkSpringboard", "Couldn't launch Slice intent", e);
            startActivity(new Intent("android.settings.SETTINGS"));
            finish();
        } catch (URISyntaxException e2) {
            Log.e("DeeplinkSpringboard", "Error decoding uri", e2);
            finish();
        }
    }

    public static Intent parse(Uri uri, String str) throws URISyntaxException {
        Intent uri2 = Intent.parseUri(uri.getQueryParameter("intent"), 2);
        uri2.setComponent(null);
        if (uri2.getExtras() != null) {
            uri2.getExtras().clear();
        }
        uri2.setPackage(str);
        return uri2;
    }
}
