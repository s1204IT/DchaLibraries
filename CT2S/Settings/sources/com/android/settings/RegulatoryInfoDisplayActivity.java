package com.android.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class RegulatoryInfoDisplayActivity extends Activity implements DialogInterface.OnDismissListener {
    private final String REGULATORY_INFO_RESOURCE = "regulatory_info";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Resources resources = getResources();
        if (!resources.getBoolean(R.bool.config_show_regulatory_info)) {
            finish();
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle(R.string.regulatory_information).setOnDismissListener(this);
        boolean regulatoryInfoDrawableExists = false;
        int resId = getResourceId();
        if (resId != 0) {
            try {
                Drawable d = getDrawable(resId);
                if (d.getIntrinsicWidth() > 2) {
                    regulatoryInfoDrawableExists = d.getIntrinsicHeight() > 2;
                }
            } catch (Resources.NotFoundException e) {
                regulatoryInfoDrawableExists = false;
            }
        }
        CharSequence regulatoryText = resources.getText(R.string.regulatory_info_text);
        if (regulatoryInfoDrawableExists) {
            View view = getLayoutInflater().inflate(R.layout.regulatory_info, (ViewGroup) null);
            ImageView image = (ImageView) view.findViewById(R.id.regulatoryInfo);
            image.setImageResource(resId);
            builder.setView(view);
            builder.show();
            return;
        }
        if (regulatoryText.length() > 0) {
            builder.setMessage(regulatoryText);
            AlertDialog dialog = builder.show();
            TextView messageText = (TextView) dialog.findViewById(android.R.id.message);
            messageText.setGravity(17);
            return;
        }
        finish();
    }

    private int getResourceId() {
        int resId = getResources().getIdentifier("regulatory_info", "drawable", getPackageName());
        String sku = SystemProperties.get("ro.boot.hardware.sku", "");
        if (!TextUtils.isEmpty(sku)) {
            String regulatory_info_res = "regulatory_info_" + sku.toLowerCase();
            int id = getResources().getIdentifier(regulatory_info_res, "drawable", getPackageName());
            if (id != 0) {
                return id;
            }
            return resId;
        }
        return resId;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        finish();
    }
}
