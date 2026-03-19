package com.mediatek.media.ringtone;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.provider.MediaStore;

public class RingtoneManagerEx {
    public void preFilterDrmFilesForFlType(Context context, Uri uri) {
    }

    public String appendDrmToWhereClause(Activity activity) {
        return "";
    }

    public String[] getMtkMediaColumns() {
        return new String[]{"_id", "title", "\"" + MediaStore.Audio.Media.EXTERNAL_CONTENT_URI + "\"", "title_key"};
    }
}
