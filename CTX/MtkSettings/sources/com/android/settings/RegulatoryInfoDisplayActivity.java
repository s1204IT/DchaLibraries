package com.android.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.Locale;

public class RegulatoryInfoDisplayActivity extends Activity implements DialogInterface.OnDismissListener {
    private final String REGULATORY_INFO_RESOURCE = "regulatory_info";

    @Override
    protected void onCreate(Bundle bundle) {
        int resourceId;
        super.onCreate(bundle);
        Resources resources = getResources();
        if (!resources.getBoolean(R.bool.config_show_regulatory_info)) {
            finish();
        }
        AlertDialog.Builder onDismissListener = new AlertDialog.Builder(this).setTitle(R.string.regulatory_labels).setOnDismissListener(this);
        Bitmap bitmapDecodeFile = BitmapFactory.decodeFile(getRegulatoryInfoImageFileName());
        boolean z = false;
        boolean z2 = bitmapDecodeFile != null;
        if (!z2) {
            resourceId = getResourceId();
        } else {
            resourceId = 0;
        }
        if (resourceId != 0) {
            try {
                Drawable drawable = getDrawable(resourceId);
                if (drawable.getIntrinsicWidth() > 2) {
                    if (drawable.getIntrinsicHeight() > 2) {
                        z = true;
                    }
                }
            } catch (Resources.NotFoundException e) {
            }
        } else {
            z = z2;
        }
        CharSequence text = resources.getText(R.string.regulatory_info_text);
        if (!z) {
            if (text.length() > 0) {
                onDismissListener.setMessage(text);
                ((TextView) onDismissListener.show().findViewById(android.R.id.message)).setGravity(17);
                return;
            } else {
                finish();
                return;
            }
        }
        View viewInflate = getLayoutInflater().inflate(R.layout.regulatory_info, (ViewGroup) null);
        ImageView imageView = (ImageView) viewInflate.findViewById(R.id.regulatoryInfo);
        if (bitmapDecodeFile != null) {
            imageView.setImageBitmap(bitmapDecodeFile);
        } else {
            imageView.setImageResource(resourceId);
        }
        onDismissListener.setView(viewInflate);
        onDismissListener.show();
    }

    private int getResourceId() {
        int identifier = getResources().getIdentifier("regulatory_info", "drawable", getPackageName());
        String sku = getSku();
        if (TextUtils.isEmpty(sku)) {
            return identifier;
        }
        int identifier2 = getResources().getIdentifier("regulatory_info_" + sku.toLowerCase(), "drawable", getPackageName());
        return identifier2 != 0 ? identifier2 : identifier;
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        finish();
    }

    public static String getSku() {
        return SystemProperties.get("ro.boot.hardware.sku", "");
    }

    public static String getRegulatoryInfoImageFileName() {
        String sku = getSku();
        if (TextUtils.isEmpty(sku)) {
            return "/data/misc/elabel/regulatory_info.png";
        }
        return String.format(Locale.US, "/data/misc/elabel/regulatory_info_%s.png", sku.toLowerCase());
    }
}
