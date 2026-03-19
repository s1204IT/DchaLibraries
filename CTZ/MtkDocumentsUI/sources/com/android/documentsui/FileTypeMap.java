package com.android.documentsui;

import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import com.android.documentsui.base.Lookup;
import com.android.documentsui.base.MimeTypes;
import java.util.HashMap;
import libcore.net.MimeUtils;

public class FileTypeMap implements Lookup<String, String> {
    static final boolean $assertionsDisabled = false;
    private final Resources mRes;
    private final SparseArray<Integer> mMediaTypeStringMap = new SparseArray<>();
    private final HashMap<String, Integer> mFileTypeMap = new HashMap<>();
    private final HashMap<String, String> mArchiveTypeMap = new HashMap<>();
    private final HashMap<String, Integer> mSpecialMediaMimeType = new HashMap<>();

    FileTypeMap(Context context) {
        this.mRes = context.getResources();
        this.mMediaTypeStringMap.put(R.string.video_file_type, Integer.valueOf(R.string.video_extension_file_type));
        this.mMediaTypeStringMap.put(R.string.audio_file_type, Integer.valueOf(R.string.audio_extension_file_type));
        this.mMediaTypeStringMap.put(R.string.image_file_type, Integer.valueOf(R.string.image_extension_file_type));
        this.mFileTypeMap.put("application/vnd.android.package-archive", Integer.valueOf(R.string.apk_file_type));
        this.mFileTypeMap.put("text/plain", Integer.valueOf(R.string.txt_file_type));
        this.mFileTypeMap.put("text/html", Integer.valueOf(R.string.html_file_type));
        this.mFileTypeMap.put("application/xhtml+xml", Integer.valueOf(R.string.html_file_type));
        this.mFileTypeMap.put("application/pdf", Integer.valueOf(R.string.pdf_file_type));
        this.mFileTypeMap.put("application/msword", Integer.valueOf(R.string.word_file_type));
        this.mFileTypeMap.put("application/vnd.openxmlformats-officedocument.wordprocessingml.document", Integer.valueOf(R.string.word_file_type));
        this.mFileTypeMap.put("application/vnd.ms-powerpoint", Integer.valueOf(R.string.ppt_file_type));
        this.mFileTypeMap.put("application/vnd.openxmlformats-officedocument.presentationml.presentation", Integer.valueOf(R.string.ppt_file_type));
        this.mFileTypeMap.put("application/vnd.ms-excel", Integer.valueOf(R.string.excel_file_type));
        this.mFileTypeMap.put("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", Integer.valueOf(R.string.excel_file_type));
        this.mFileTypeMap.put("application/vnd.google-apps.document", Integer.valueOf(R.string.gdoc_file_type));
        this.mFileTypeMap.put("application/vnd.google-apps.spreadsheet", Integer.valueOf(R.string.gsheet_file_type));
        this.mFileTypeMap.put("application/vnd.google-apps.presentation", Integer.valueOf(R.string.gslides_file_type));
        this.mFileTypeMap.put("application/vnd.google-apps.drawing", Integer.valueOf(R.string.gdraw_file_type));
        this.mFileTypeMap.put("application/vnd.google-apps.fusiontable", Integer.valueOf(R.string.gtable_file_type));
        this.mFileTypeMap.put("application/vnd.google-apps.form", Integer.valueOf(R.string.gform_file_type));
        this.mFileTypeMap.put("application/vnd.google-apps.map", Integer.valueOf(R.string.gmap_file_type));
        this.mFileTypeMap.put("application/vnd.google-apps.sites", Integer.valueOf(R.string.gsite_file_type));
        this.mFileTypeMap.put("vnd.android.document/directory", Integer.valueOf(R.string.directory_type));
        this.mArchiveTypeMap.put("application/rar", "RAR");
        this.mArchiveTypeMap.put("application/zip", "Zip");
        this.mArchiveTypeMap.put("application/x-tar", "Tar");
        this.mArchiveTypeMap.put("application/gzip", "Gzip");
        this.mArchiveTypeMap.put("application/x-7z-compressed", "7z");
        this.mArchiveTypeMap.put("application/x-rar-compressed", "RAR");
        this.mSpecialMediaMimeType.put("application/ogg", Integer.valueOf(R.string.audio_file_type));
        this.mSpecialMediaMimeType.put("application/x-flac", Integer.valueOf(R.string.audio_file_type));
    }

    @Override
    public String lookup(String str) {
        if (this.mFileTypeMap.containsKey(str)) {
            return getPredefinedFileTypeString(str);
        }
        if (this.mArchiveTypeMap.containsKey(str)) {
            return buildArchiveTypeString(str);
        }
        if (this.mSpecialMediaMimeType.containsKey(str)) {
            int iIntValue = this.mSpecialMediaMimeType.get(str).intValue();
            return getFileTypeString(str, this.mMediaTypeStringMap.get(iIntValue).intValue(), iIntValue);
        }
        String[] strArrSplitMimeType = MimeTypes.splitMimeType(str);
        if (strArrSplitMimeType == null) {
            Log.w("FileTypeMap", "Unexpected mime type " + str);
            return getGenericFileTypeString();
        }
        byte b = 0;
        String str2 = strArrSplitMimeType[0];
        int iHashCode = str2.hashCode();
        if (iHashCode != 93166550) {
            if (iHashCode != 100313435) {
                b = (iHashCode == 112202875 && str2.equals("video")) ? (byte) 2 : (byte) -1;
            } else if (!str2.equals("image")) {
            }
        } else if (str2.equals("audio")) {
            b = 1;
        }
        switch (b) {
            case 0:
                return getFileTypeString(str, R.string.image_extension_file_type, R.string.image_file_type);
            case 1:
                return getFileTypeString(str, R.string.audio_extension_file_type, R.string.audio_file_type);
            case 2:
                return getFileTypeString(str, R.string.video_extension_file_type, R.string.video_file_type);
            default:
                return getFileTypeString(str, R.string.generic_extention_file_type, R.string.generic_file_type);
        }
    }

    private String buildArchiveTypeString(String str) {
        return String.format(this.mRes.getString(R.string.archive_file_type), this.mArchiveTypeMap.get(str));
    }

    private String getPredefinedFileTypeString(String str) {
        return this.mRes.getString(this.mFileTypeMap.get(str).intValue());
    }

    private String getFileTypeString(String str, int i, int i2) {
        String strGuessExtensionFromMimeType = MimeUtils.guessExtensionFromMimeType(str);
        return TextUtils.isEmpty(strGuessExtensionFromMimeType) ? this.mRes.getString(i2) : String.format(this.mRes.getString(i), strGuessExtensionFromMimeType.toUpperCase());
    }

    private String getGenericFileTypeString() {
        return this.mRes.getString(R.string.generic_file_type);
    }
}
