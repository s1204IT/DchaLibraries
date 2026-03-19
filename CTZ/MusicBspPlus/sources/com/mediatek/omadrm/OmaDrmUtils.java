package com.mediatek.omadrm;

import android.R;
import android.app.Activity;
import android.app.ActivityThread;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.drm.DrmInfo;
import android.drm.DrmInfoRequest;
import android.drm.DrmManagerClient;
import android.media.MediaFile;
import android.net.Uri;
import android.os.BenesseExtension;
import android.os.Binder;
import android.os.Build;
import android.os.SystemProperties;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class OmaDrmUtils {
    private static final boolean DEBUG;
    private static final Uri FILE_URI;
    private static DialogFragment sConsumeDialog;
    private static boolean sIsOmaDrmEnabled;
    private static DialogFragment sProtectionInfoDialog;

    static {
        DEBUG = Log.isLoggable("OmaDrmUtils", 3) || "eng".equals(Build.TYPE);
        FILE_URI = MediaStore.Files.getContentUri("external");
        sConsumeDialog = null;
        sProtectionInfoDialog = null;
        sIsOmaDrmEnabled = SystemProperties.getBoolean("ro.vendor.mtk_oma_drm_support", false);
    }

    public static boolean isOmaDrmEnabled() {
        return sIsOmaDrmEnabled;
    }

    public static boolean isDrm(DrmManagerClient drmManagerClient, Uri uri) throws Throwable {
        IOException e;
        InputStream inputStreamOpenInputStream;
        String str;
        Exception e2;
        StringBuilder sb;
        Integer asInteger;
        boolean zIsDrm = false;
        try {
            ContentValues metadata = drmManagerClient.getMetadata(uri);
            return (metadata == null || (asInteger = metadata.getAsInteger("isdrm")) == null || asInteger.intValue() <= 0) ? false : true;
        } catch (IllegalArgumentException e3) {
            Log.e("OmaDrmUtils", "isDrm: getMetadata fail with " + uri, e3);
            ?? CurrentApplication = ActivityThread.currentApplication();
            if (CurrentApplication != 0) {
                try {
                    try {
                        inputStreamOpenInputStream = CurrentApplication.getContentResolver().openInputStream(uri);
                        if (inputStreamOpenInputStream != null) {
                            try {
                                byte[] bArr = new byte[128];
                                if (128 == inputStreamOpenInputStream.read(bArr)) {
                                    zIsDrm = isDrm(bArr);
                                }
                            } catch (IOException e4) {
                                e = e4;
                                Log.e("OmaDrmUtils", "isDrm: IOException fail with " + uri, e);
                                if (inputStreamOpenInputStream != null) {
                                    try {
                                        inputStreamOpenInputStream.close();
                                    } catch (Exception e5) {
                                        e2 = e5;
                                        str = "OmaDrmUtils";
                                        sb = new StringBuilder();
                                        sb.append("isDrm: close input stream fail with ");
                                        sb.append(e2);
                                        Log.e(str, sb.toString());
                                        Log.d("OmaDrmUtils", "isDrm: check from file with result = " + zIsDrm);
                                        return zIsDrm;
                                    }
                                }
                                Log.d("OmaDrmUtils", "isDrm: check from file with result = " + zIsDrm);
                                return zIsDrm;
                            }
                        }
                        if (inputStreamOpenInputStream != null) {
                            try {
                                inputStreamOpenInputStream.close();
                            } catch (Exception e6) {
                                e2 = e6;
                                str = "OmaDrmUtils";
                                sb = new StringBuilder();
                                sb.append("isDrm: close input stream fail with ");
                                sb.append(e2);
                                Log.e(str, sb.toString());
                                Log.d("OmaDrmUtils", "isDrm: check from file with result = " + zIsDrm);
                                return zIsDrm;
                            }
                        }
                    } catch (Throwable th) {
                        th = th;
                        if (CurrentApplication != 0) {
                            try {
                                CurrentApplication.close();
                            } catch (Exception e7) {
                                Log.e("OmaDrmUtils", "isDrm: close input stream fail with " + e7);
                            }
                        }
                        throw th;
                    }
                } catch (IOException e8) {
                    e = e8;
                    inputStreamOpenInputStream = null;
                } catch (Throwable th2) {
                    th = th2;
                    CurrentApplication = 0;
                    if (CurrentApplication != 0) {
                    }
                    throw th;
                }
            }
            Log.d("OmaDrmUtils", "isDrm: check from file with result = " + zIsDrm);
            return zIsDrm;
        }
    }

    private static boolean isDrm(byte[] bArr) {
        if (bArr == null || bArr.length < 128) {
            return false;
        }
        if (bArr[0] != 1) {
            Log.d("OmaDrmUtils", "isDrmFile: version is not dcf version 1, no oma drm file");
            return false;
        }
        byte b = bArr[1];
        byte b2 = bArr[2];
        if (b <= 0 || b + 3 > 128 || b2 <= 0 || b2 > 128) {
            Log.d("OmaDrmUtils", "isDrmFile: content type or uri len invalid, not oma drm file, contentType[" + ((int) b) + "] contentUri[" + ((int) b2) + "]");
            return false;
        }
        String str = new String(bArr, 3, (int) b);
        if (!str.contains("/")) {
            Log.d("OmaDrmUtils", "isDrmFile: content type not right, not oma drm file");
            return false;
        }
        Log.d("OmaDrmUtils", "this is a oma drm file: " + str);
        return true;
    }

    public static int getActionByMimetype(String str) {
        int i = 0;
        if (!TextUtils.isEmpty(str)) {
            if (str.startsWith("image/")) {
                i = 7;
            } else if (str.startsWith("video/") || str.startsWith("audio/")) {
                i = 1;
            }
        }
        if (DEBUG) {
            Log.d("OmaDrmUtils", "getActionByMimetype: mimetype=" + str + ", action=" + i);
        }
        return i;
    }

    public static void showConsumerDialog(Context context, DrmManagerClient drmManagerClient, Uri uri, DialogInterface.OnClickListener onClickListener) {
        showConsumerDialog(context, drmManagerClient, convertUriToPath(context, uri), onClickListener);
    }

    public static void showConsumerDialog(Context context, final DrmManagerClient drmManagerClient, String str, final DialogInterface.OnClickListener onClickListener) {
        if (DEBUG) {
            Log.d("OmaDrmUtils", "showConsumerDialog: path = " + str);
        }
        if (onClickListener == null) {
            Log.e("OmaDrmUtils", "showConsumerDialog, onClickListener is null.");
            return;
        }
        if (!sIsOmaDrmEnabled) {
            Log.d("OmaDrmUtils", "showConsumerDialog, oma drm disable.");
            onClickListener.onClick(null, -1);
            return;
        }
        if (TextUtils.isEmpty(str)) {
            Log.e("OmaDrmUtils", "showConsumerDialog: Given path is invalid");
            return;
        }
        if (!(context instanceof Activity)) {
            Log.e("OmaDrmUtils", "showConsumerDialog : not an acitivty context");
            onClickListener.onClick(null, -2);
            return;
        }
        ContentValues metadata = drmManagerClient.getMetadata(str);
        if (DEBUG) {
            Log.d("OmaDrmUtils", "showConsumerDialog: metadata = " + metadata);
        }
        if (metadata == null) {
            Log.d("OmaDrmUtils", "showConsumerDialog, get metadata is null, it's not drm file");
            onClickListener.onClick(null, -1);
            return;
        }
        Integer asInteger = metadata.getAsInteger("isdrm");
        if (asInteger == null || asInteger.intValue() <= 0) {
            Log.d("OmaDrmUtils", "showConsumerDialog, get metadata is null, it's not drm file");
            onClickListener.onClick(null, -1);
            return;
        }
        final String asString = metadata.getAsString("drm_mime_type");
        int actionByMimetype = getActionByMimetype(asString);
        if (drmManagerClient.checkRightsStatus(str, actionByMimetype) != 0) {
            if (DEBUG) {
                Log.d("OmaDrmUtils", "showConsumerDialog: rights is invalid, play directly");
            }
            onClickListener.onClick(null, -1);
            return;
        }
        final String asString2 = metadata.getAsString("drm_content_uri");
        DialogInterface.OnClickListener onClickListener2 = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (i == -1 && (asString.startsWith("image/") || asString.startsWith("video/"))) {
                    OmaDrmUtils.markAsConsumeInAppClient(drmManagerClient, asString2);
                }
                if (OmaDrmUtils.sConsumeDialog != null || dialogInterface == null) {
                    onClickListener.onClick(dialogInterface, i);
                }
                DialogFragment unused = OmaDrmUtils.sConsumeDialog = null;
            }
        };
        ContentValues constraints = drmManagerClient.getConstraints(str, actionByMimetype);
        if (DEBUG) {
            Log.d("OmaDrmUtils", "showConsumerDialog : constraints = " + constraints);
        }
        if (constraints == null || constraints.size() == 0) {
            Log.e("OmaDrmUtils", "showConsumerDialog : constraints is null, no rights");
            onClickListener2.onClick(null, -1);
            return;
        }
        Resources resources = context.getResources();
        StringBuilder sb = new StringBuilder();
        Long asLong = constraints.getAsLong("max_repeat_count");
        Long asLong2 = constraints.getAsLong("remaining_repeat_count");
        Long asLong3 = constraints.getAsLong("license_start_time");
        Long asLong4 = constraints.getAsLong("license_expiry_time");
        Long asLong5 = constraints.getAsLong("license_available_time");
        if (asLong == null || asLong3 == null || asLong5 == null) {
            Log.w("OmaDrmUtils", "showConsumerDialog: max count or start time or available time is null");
            onClickListener2.onClick(null, -1);
            return;
        }
        if (asLong.longValue() > 0) {
            if (asLong2 == asLong) {
                sb.append(resources.getString(134545501));
                sb.append(" ");
                sb.append(resources.getString(134545502));
                sb.append("\n");
                sb.append(resources.getString(134545478));
                sb.append(" ");
                sb.append(asLong2);
            } else if (asLong2.longValue() <= 2) {
                sb.append(resources.getString(134545478));
                sb.append(" ");
                sb.append(asLong2);
                sb.append("\n");
                sb.append(resources.getString(134545502));
            } else {
                onClickListener2.onClick(null, -1);
                return;
            }
        } else if (asLong5.longValue() <= 0) {
            onClickListener2.onClick(null, -1);
            return;
        } else if (asLong3.longValue() == -1 && asLong4.longValue() == -1) {
            sb.append(resources.getString(134545503, toTimeString(asLong5)));
            sb.append(" ");
            sb.append(resources.getString(134545502));
        } else {
            onClickListener2.onClick(null, -1);
            return;
        }
        if (DEBUG) {
            Log.d("OmaDrmUtils", "showConsumerDialog with message: " + ((Object) sb));
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setIcon(R.drawable.ic_dialog_info);
        builder.setTitle(134545499);
        builder.setPositiveButton(R.string.ok, onClickListener2);
        builder.setNegativeButton(R.string.cancel, onClickListener2);
        builder.setMessage(sb);
        ConsumeDialogFragment consumeDialogFragmentNewInstance = ConsumeDialogFragment.newInstance(sb);
        consumeDialogFragmentNewInstance.setOnClickListener(onClickListener2);
        if (sConsumeDialog != null) {
            sConsumeDialog.dismissAllowingStateLoss();
        }
        sConsumeDialog = consumeDialogFragmentNewInstance;
        FragmentTransaction fragmentTransactionBeginTransaction = ((Activity) context).getFragmentManager().beginTransaction();
        fragmentTransactionBeginTransaction.add(sConsumeDialog, "consume_rights_dialog");
        fragmentTransactionBeginTransaction.commitAllowingStateLoss();
        if (DEBUG) {
            Log.d("OmaDrmUtils", "showConsumerDialog: begin show dialog fragment");
        }
    }

    public static void clearProtectionInfoDialog() {
        sProtectionInfoDialog = null;
    }

    public static void showProtectionInfoDialog(Context context, DrmManagerClient drmManagerClient, Uri uri) {
        showProtectionInfoDialog(context, drmManagerClient, convertUriToPath(context, uri));
    }

    public static void showProtectionInfoDialog(final Context context, DrmManagerClient drmManagerClient, String str) {
        if (DEBUG) {
            Log.d("OmaDrmUtils", "showProtectionInfoDialog: path=" + str + ", context=" + context);
        }
        if (!sIsOmaDrmEnabled) {
            Log.d("OmaDrmUtils", "showProtectionInfoDialog, oma drm is disable");
            return;
        }
        if (TextUtils.isEmpty(str)) {
            Log.e("OmaDrmUtils", "showProtectionInfoDialog: Given path is invalid");
            return;
        }
        if (!(context instanceof Activity)) {
            Log.e("OmaDrmUtils", "showConsumerDialog : not an Acitivty context");
            return;
        }
        ContentValues metadata = drmManagerClient.getMetadata(str);
        if (DEBUG) {
            Log.d("OmaDrmUtils", "showProtectionInfoDialog: metadata = " + metadata);
        }
        if (metadata == null) {
            Log.d("OmaDrmUtils", "showProtectionInfoDialog, get metadata is null, it's not drm file");
            return;
        }
        Integer asInteger = metadata.getAsInteger("isdrm");
        if (asInteger == null || asInteger.intValue() <= 0) {
            Log.d("OmaDrmUtils", "showProtectionInfoDialog, get metadata is null, it's not drm file");
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        StringBuilder sb = new StringBuilder();
        Resources resources = context.getResources();
        ContentValues constraints = drmManagerClient.getConstraints(str, getActionByMimetype(metadata.getAsString("drm_mime_type")));
        if (DEBUG) {
            Log.d("OmaDrmUtils", "showProtectionInfoDialog : constraints = " + constraints);
        }
        sb.append(MediaFile.getFileTitle(str));
        sb.append("\n");
        sb.append(resources.getString(134545470));
        sb.append(" ");
        if (metadata.getAsInteger("drm_method").intValue() == 4) {
            sb.append(resources.getString(134545471));
            sb.append("\n");
        } else {
            sb.append(resources.getString(134545472));
            sb.append("\n");
        }
        if (constraints == null || constraints.size() == 0) {
            sb.append(resources.getString(134545474));
            final String asString = metadata.getAsString("drm_rights_issuer");
            if (!TextUtils.isEmpty(asString)) {
                builder.setPositiveButton(134545480, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent intent = new Intent("android.intent.action.VIEW", Uri.parse(asString));
                        if (BenesseExtension.getDchaState() == 0) {
                            context.startActivity(intent);
                        }
                    }
                });
            }
        } else {
            Long asLong = constraints.getAsLong("max_repeat_count");
            Long asLong2 = constraints.getAsLong("remaining_repeat_count");
            Long asLong3 = constraints.getAsLong("license_start_time");
            Long asLong4 = constraints.getAsLong("license_expiry_time");
            Long asLong5 = constraints.getAsLong("license_available_time");
            if (asLong == null || asLong3 == null || asLong5 == null) {
                Log.w("OmaDrmUtils", "showConsumerDialog:max count or start time or available time is null");
                return;
            }
            if (asLong2.longValue() > 0) {
                sb.append(resources.getString(134545478));
                sb.append(" ");
                sb.append(asLong2);
                sb.append("\n");
            }
            if (asLong3.longValue() > 0 && asLong4.longValue() > 0) {
                sb.append(resources.getString(134545509));
                sb.append(" ");
                sb.append(toDateTimeString(asLong3));
                sb.append(" - ");
                sb.append(toDateTimeString(asLong4));
            } else if (asLong5.longValue() > 0) {
                String timeString = toTimeString(asLong5);
                sb.append(resources.getString(134545504));
                sb.append(" ");
                sb.append(timeString);
            }
        }
        if (DEBUG) {
            Log.d("OmaDrmUtils", "showProtectionIfoDialog : message = " + ((Object) sb));
        }
        builder.setTitle(134545506);
        builder.setMessage(sb);
        builder.setNeutralButton(R.string.ok, (DialogInterface.OnClickListener) null);
        ProtectionDialogFragment protectionDialogFragmentNewInstance = ProtectionDialogFragment.newInstance(builder);
        if (sProtectionInfoDialog != null) {
            sProtectionInfoDialog.dismissAllowingStateLoss();
        }
        sProtectionInfoDialog = protectionDialogFragmentNewInstance;
        FragmentTransaction fragmentTransactionBeginTransaction = ((Activity) context).getFragmentManager().beginTransaction();
        fragmentTransactionBeginTransaction.add(sProtectionInfoDialog, "protection_info_dialog");
        fragmentTransactionBeginTransaction.commitAllowingStateLoss();
        if (DEBUG) {
            Log.d("OmaDrmUtils", "showProtectionInfoDialog: begin show dialog fragment");
        }
    }

    private static boolean markAsConsumeInAppClient(DrmManagerClient drmManagerClient, String str) {
        int callingPid = Binder.getCallingPid();
        if (DEBUG) {
            Log.d("OmaDrmUtils", "markAsConsumeInAppClient : pid = " + callingPid + ", cid = " + str);
        }
        DrmInfoRequest drmInfoRequest = new DrmInfoRequest(2021, "application/vnd.oma.drm.message");
        drmInfoRequest.put("action", "markAsConsumeInAppClient");
        drmInfoRequest.put("data_1", String.valueOf(callingPid));
        if (str != null) {
            drmInfoRequest.put("data_2", str);
        }
        Log.e("OmaDrmUtils", "client.acquireDrmInfo OmaDrmUtils 3 ");
        return "success".equals(getResultFromDrmInfo(drmManagerClient.acquireDrmInfo(drmInfoRequest)));
    }

    private static String toTimeString(Long l) {
        Long l2 = 60L;
        Long l3 = 10L;
        Long lValueOf = Long.valueOf(l.longValue() / (l2.longValue() * l2.longValue()));
        Long lValueOf2 = Long.valueOf((l.longValue() - ((lValueOf.longValue() * l2.longValue()) * l2.longValue())) / l2.longValue());
        Long lValueOf3 = Long.valueOf((l.longValue() - ((lValueOf.longValue() * l2.longValue()) * l2.longValue())) - (lValueOf2.longValue() * l2.longValue()));
        StringBuilder sb = new StringBuilder();
        if (lValueOf.longValue() < l3.longValue()) {
            sb.append("0");
            sb.append(lValueOf.toString());
        } else {
            sb.append(lValueOf.toString());
        }
        sb.append(":");
        if (lValueOf2.longValue() < l3.longValue()) {
            sb.append("0");
            sb.append(lValueOf2.toString());
        } else {
            sb.append(lValueOf2.toString());
        }
        sb.append(":");
        if (lValueOf3.longValue() < l3.longValue()) {
            sb.append("0");
            sb.append(lValueOf3.toString());
        } else {
            sb.append(lValueOf3.toString());
        }
        return sb.toString();
    }

    private static String toDateTimeString(Long l) {
        SimpleDateFormat simpleDateFormat;
        Date date = new Date(l.longValue() * 1000);
        if (DateFormat.is24HourFormat(ActivityThread.currentApplication())) {
            simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        } else {
            simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        }
        Log.e("OmaDrmUtils", "toDateTimeString ");
        return simpleDateFormat.format(date);
    }

    public static boolean isTokenValid(DrmManagerClient drmManagerClient, String str, String str2) {
        Log.d("OmaDrmUtils", "isTokenValid filePath:" + str);
        DrmInfoRequest drmInfoRequest = new DrmInfoRequest(2022, "application/vnd.mtk.cta5.message");
        drmInfoRequest.put("action", "CTA5Checktoken");
        drmInfoRequest.put("CTA5FilePath", str);
        drmInfoRequest.put("CTA5Token", str2);
        Log.e("OmaDrmUtils", "client.acquireDrmInfo OmaDrmUtils 4 ");
        return "success".equals(getResultFromDrmInfo(drmManagerClient.acquireDrmInfo(drmInfoRequest)));
    }

    public static boolean clearToken(DrmManagerClient drmManagerClient, String str, String str2) {
        Log.d("OmaDrmUtils", "clearToken filePath:" + str);
        DrmInfoRequest drmInfoRequest = new DrmInfoRequest(2022, "application/vnd.mtk.cta5.message");
        drmInfoRequest.put("action", "CTA5Cleartoken");
        drmInfoRequest.put("CTA5FilePath", str);
        drmInfoRequest.put("CTA5Token", str2);
        Log.e("OmaDrmUtils", "client.acquireDrmInfo OmaDrmUtils 5");
        return "success".equals(getResultFromDrmInfo(drmManagerClient.acquireDrmInfo(drmInfoRequest)));
    }

    private static String getResultFromDrmInfo(DrmInfo drmInfo) {
        byte[] data;
        if (drmInfo != null) {
            data = drmInfo.getData();
        } else {
            data = null;
        }
        if (data == null) {
            return "";
        }
        try {
            return new String(data, "US-ASCII");
        } catch (UnsupportedEncodingException e) {
            Log.e("OmaDrmUtils", "Unsupported hongen encoding type of the returned DrmInfo data");
            return "";
        }
    }

    private static String convertUriToPath(Context context, Uri uri) throws Throwable {
        Cursor cursorQuery;
        String path = null;
        path = null;
        path = null;
        path = null;
        path = null;
        path = null;
        Cursor cursor = null;
        path = null;
        path = null;
        if (uri != null) {
            String scheme = uri.getScheme();
            if (scheme == null || scheme.equals("") || scheme.equals("file")) {
                path = uri.getPath();
            } else if (scheme.equals("http")) {
                path = uri.toString();
            } else if (scheme.equals("content")) {
                String[] strArr = {"_data"};
                ContentResolver contentResolver = context.getContentResolver();
                try {
                    try {
                        cursorQuery = contentResolver.query(uri, strArr, null, null, null);
                        if (cursorQuery != null) {
                            try {
                                if (cursorQuery.moveToFirst()) {
                                    int columnIndex = cursorQuery.getColumnIndex("_data");
                                    if (columnIndex != -1) {
                                        path = cursorQuery.getString(columnIndex);
                                    } else {
                                        if (cursorQuery != null) {
                                            cursorQuery.close();
                                            cursorQuery = null;
                                        }
                                        String contentUri = getContentUri(context, uri);
                                        if (contentUri != null) {
                                            Cursor cursorQuery2 = contentResolver.query(FILE_URI, new String[]{"_data"}, "drm_content_uri=?", new String[]{contentUri}, null);
                                            if (cursorQuery2 != null) {
                                                try {
                                                    if (cursorQuery2.moveToFirst()) {
                                                        path = cursorQuery2.getString(0);
                                                    }
                                                } catch (SQLiteException e) {
                                                    cursor = cursorQuery2;
                                                    String path2 = uri.getPath();
                                                    if (cursor != null) {
                                                        cursor.close();
                                                    }
                                                    path = path2;
                                                } catch (Throwable th) {
                                                    cursorQuery = cursorQuery2;
                                                    th = th;
                                                    if (cursorQuery != null) {
                                                        cursorQuery.close();
                                                    }
                                                    throw th;
                                                }
                                            }
                                            cursorQuery = cursorQuery2;
                                        }
                                    }
                                }
                            } catch (SQLiteException e2) {
                                cursor = cursorQuery;
                            } catch (Throwable th2) {
                                th = th2;
                            }
                        }
                        if (cursorQuery != null) {
                            cursorQuery.close();
                        }
                    } catch (Throwable th3) {
                        th = th3;
                        cursorQuery = path;
                    }
                } catch (SQLiteException e3) {
                }
            }
        }
        if (DEBUG) {
            Log.d("OmaDrmUtils", "convertUriToPath: uri = " + uri + " --> path = " + path);
        }
        return path;
    }

    private static String getContentUri(Context context, Uri uri) throws Throwable {
        InputStream inputStreamOpenInputStream;
        IOException e;
        String str;
        InputStream inputStream = null;
        strSubstring = null;
        strSubstring = null;
        strSubstring = null;
        String strSubstring = null;
        inputStream = null;
        try {
            try {
                inputStreamOpenInputStream = context.getContentResolver().openInputStream(uri);
                if (inputStreamOpenInputStream != null) {
                    try {
                        try {
                            byte[] bArr = new byte[3];
                            if (3 == inputStreamOpenInputStream.read(bArr) && bArr[0] == 1) {
                                int i = bArr[1];
                                int i2 = bArr[2];
                                byte[] bArr2 = new byte[i + i2];
                                if (bArr2.length == inputStreamOpenInputStream.read(bArr2)) {
                                    str = new String(bArr2, i, i2);
                                    try {
                                        strSubstring = str.substring(str.indexOf(":") + 1);
                                    } catch (IOException e2) {
                                        e = e2;
                                        inputStream = inputStreamOpenInputStream;
                                        Log.e("OmaDrmUtils", "getContentUri: IOException fail with " + uri, e);
                                        if (inputStream != null) {
                                            try {
                                                inputStream.close();
                                            } catch (Exception e3) {
                                                Log.e("OmaDrmUtils", "getContentUri: close input stream fail with " + e3);
                                            }
                                        }
                                        strSubstring = str;
                                    }
                                }
                            }
                        } catch (Throwable th) {
                            th = th;
                            if (inputStreamOpenInputStream != null) {
                                try {
                                    inputStreamOpenInputStream.close();
                                } catch (Exception e4) {
                                    Log.e("OmaDrmUtils", "getContentUri: close input stream fail with " + e4);
                                }
                            }
                            throw th;
                        }
                    } catch (IOException e5) {
                        e = e5;
                        str = null;
                    }
                }
                if (inputStreamOpenInputStream != null) {
                    try {
                        inputStreamOpenInputStream.close();
                    } catch (Exception e6) {
                        Log.e("OmaDrmUtils", "getContentUri: close input stream fail with " + e6);
                    }
                }
            } catch (IOException e7) {
                e = e7;
                str = null;
            }
            if (DEBUG) {
                Log.d("OmaDrmUtils", "getContentUri: uri = " + uri + ", contentUri = " + strSubstring);
            }
            return strSubstring;
        } catch (Throwable th2) {
            th = th2;
            inputStreamOpenInputStream = inputStream;
        }
    }
}
