package com.mediatek.contacts.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import com.android.contacts.R;
import com.mediatek.contacts.util.Log;

public class ImportExportItem extends LinearLayout {
    private TextView mAccountUserName;
    private ImageView mIcon;
    private RadioButton mRadioButton;

    public ImportExportItem(Context context) {
        super(context);
    }

    public ImportExportItem(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override
    public void setActivated(boolean z) {
        super.setActivated(z);
        if (this.mRadioButton != null) {
            this.mRadioButton.setChecked(z);
        } else {
            Log.w("ImportExportItem", "[setActivated]radio-button cannot be activated because it is null");
        }
    }

    public void bindView(Drawable drawable, String str, String str2) {
        Log.d("ImportExportItem", "[bindView]text: " + str + ",path = " + Log.anonymize(str2));
        this.mAccountUserName = (TextView) findViewById(R.id.accountUserName);
        this.mIcon = (ImageView) findViewById(R.id.icon);
        this.mRadioButton = (RadioButton) findViewById(R.id.radioButton);
        if (drawable != null && str2 == null) {
            this.mIcon.setImageDrawable(drawable);
        } else if (str2 != null) {
            if (!isExternalStoragePath(str2)) {
                this.mIcon.setImageResource(R.drawable.mtk_contact_phone_storage);
            } else {
                this.mIcon.setImageResource(R.drawable.mtk_contact_sd_card_icon);
            }
        } else {
            this.mIcon.setImageResource(R.drawable.unknown_source);
        }
        this.mAccountUserName.setText(str);
    }

    private boolean isExternalStoragePath(String str) {
        if (str == null || str.isEmpty()) {
            Log.d("ImportExportItem", "[isExternalStoragePath]empty path: " + Log.anonymize(str));
            return false;
        }
        StorageManager storageManager = (StorageManager) getContext().getSystemService("storage");
        if (storageManager == null) {
            Log.d("ImportExportItem", "[isExternalStoragePath]failed to get StorageManager");
            return false;
        }
        StorageVolume[] volumeList = storageManager.getVolumeList();
        if (volumeList != null) {
            for (StorageVolume storageVolume : volumeList) {
                String path = storageVolume.getPath();
                boolean zIsRemovable = storageVolume.isRemovable();
                Log.d("ImportExportItem", "[isExternalStoragePath]isExternal=" + zIsRemovable + ", path=" + Log.anonymize(path));
                if (zIsRemovable && str.equals(path)) {
                    return true;
                }
            }
        }
        return false;
    }
}
