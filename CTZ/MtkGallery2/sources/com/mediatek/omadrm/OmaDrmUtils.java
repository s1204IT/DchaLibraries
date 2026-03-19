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
import android.drm.DrmRights;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
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
import com.mediatek.dcfdecoder.DcfDecoder;
import com.mediatek.gallery3d.video.BookmarkEnhance;
import com.mediatek.omadrm.OmaDrmInfoRequest;
import com.mediatek.omadrm.OmaDrmStore;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import mf.org.apache.xerces.impl.xs.SchemaSymbols;

public class OmaDrmUtils {
    private static final String CONSUME_DIALOG_TAG = "consume_rights_dialog";
    private static final boolean DEBUG;
    private static final Uri FILE_URI;
    private static final int HEADER_BUFFER_SIZE = 128;
    private static final String LINE_FEED = "\n";
    private static final String PROTECTION_INFO_DIALOG_TAG = "protection_info_dialog";
    private static final String SPACES = " ";
    public static final String TAG = "OmaDrmUtils";
    private static DialogFragment sConsumeDialog;
    private static boolean sIsOmaDrmEnabled;
    private static DialogFragment sProtectionInfoDialog;

    static {
        DEBUG = Log.isLoggable(TAG, 3) || "eng".equals(Build.TYPE);
        FILE_URI = MediaStore.Files.getContentUri("external");
        sConsumeDialog = null;
        sProtectionInfoDialog = null;
        sIsOmaDrmEnabled = SystemProperties.getBoolean("ro.vendor.mtk_oma_drm_support", false);
    }

    public static boolean isOmaDrmEnabled() {
        return sIsOmaDrmEnabled;
    }

    public static boolean isDrm(DrmManagerClient drmManagerClient, String str) {
        Integer asInteger;
        ContentValues metadata = drmManagerClient.getMetadata(str);
        if (metadata == null || (asInteger = metadata.getAsInteger(OmaDrmStore.MetadatasColumns.IS_DRM)) == null || asInteger.intValue() <= 0) {
            return false;
        }
        return true;
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
            return (metadata == null || (asInteger = metadata.getAsInteger(OmaDrmStore.MetadatasColumns.IS_DRM)) == null || asInteger.intValue() <= 0) ? false : true;
        } catch (IllegalArgumentException e3) {
            Log.e(TAG, "isDrm: getMetadata fail with " + uri, e3);
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
                                Log.e(TAG, "isDrm: IOException fail with " + uri, e);
                                if (inputStreamOpenInputStream != null) {
                                    try {
                                        inputStreamOpenInputStream.close();
                                    } catch (Exception e5) {
                                        e2 = e5;
                                        str = TAG;
                                        sb = new StringBuilder();
                                        sb.append("isDrm: close input stream fail with ");
                                        sb.append(e2);
                                        Log.e(str, sb.toString());
                                        Log.d(TAG, "isDrm: check from file with result = " + zIsDrm);
                                        return zIsDrm;
                                    }
                                }
                                Log.d(TAG, "isDrm: check from file with result = " + zIsDrm);
                                return zIsDrm;
                            }
                        }
                        if (inputStreamOpenInputStream != null) {
                            try {
                                inputStreamOpenInputStream.close();
                            } catch (Exception e6) {
                                e2 = e6;
                                str = TAG;
                                sb = new StringBuilder();
                                sb.append("isDrm: close input stream fail with ");
                                sb.append(e2);
                                Log.e(str, sb.toString());
                                Log.d(TAG, "isDrm: check from file with result = " + zIsDrm);
                                return zIsDrm;
                            }
                        }
                    } catch (Throwable th) {
                        th = th;
                        if (CurrentApplication != 0) {
                            try {
                                CurrentApplication.close();
                            } catch (Exception e7) {
                                Log.e(TAG, "isDrm: close input stream fail with " + e7);
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
            Log.d(TAG, "isDrm: check from file with result = " + zIsDrm);
            return zIsDrm;
        }
    }

    public static boolean isDrm(String str) throws Throwable {
        RandomAccessFile randomAccessFile;
        Log.d(TAG, "isDrm: check file " + str);
        RandomAccessFile randomAccessFile2 = null;
        try {
            try {
                randomAccessFile = new RandomAccessFile(new File(str), "r");
            } catch (IOException e) {
                e = e;
            }
        } catch (Throwable th) {
            th = th;
        }
        try {
            randomAccessFile.seek(0L);
            byte[] bArr = new byte[128];
            boolean zIsDrm = 128 == randomAccessFile.read(bArr) ? isDrm(bArr) : false;
            try {
                randomAccessFile.close();
            } catch (IOException e2) {
                Log.e(TAG, "isOmaDrmFile: close randomAccessFile with IOException ", e2);
            }
            Log.d(TAG, "isDrm: " + zIsDrm);
            return zIsDrm;
        } catch (IOException e3) {
            e = e3;
            randomAccessFile2 = randomAccessFile;
            Log.e(TAG, "isOmaDrmFile: read file with IOException ", e);
            if (randomAccessFile2 != null) {
                try {
                    randomAccessFile2.close();
                } catch (IOException e4) {
                    Log.e(TAG, "isOmaDrmFile: close randomAccessFile with IOException ", e4);
                }
            }
            return false;
        } catch (Throwable th2) {
            th = th2;
            randomAccessFile2 = randomAccessFile;
            if (randomAccessFile2 != null) {
                try {
                    randomAccessFile2.close();
                } catch (IOException e5) {
                    Log.e(TAG, "isOmaDrmFile: close randomAccessFile with IOException ", e5);
                }
            }
            throw th;
        }
    }

    private static boolean isDrm(byte[] bArr) {
        if (bArr == null || bArr.length < 128) {
            return false;
        }
        if (bArr[0] != 1) {
            Log.d(TAG, "isDrmFile: version is not dcf version 1, no oma drm file");
            return false;
        }
        byte b = bArr[1];
        byte b2 = bArr[2];
        if (b <= 0 || b + 3 > 128 || b2 <= 0 || b2 > 128) {
            Log.d(TAG, "isDrmFile: content type or uri len invalid, not oma drm file, contentType[" + ((int) b) + "] contentUri[" + ((int) b2) + "]");
            return false;
        }
        String str = new String(bArr, 3, (int) b);
        if (!str.contains("/")) {
            Log.d(TAG, "isDrmFile: content type not right, not oma drm file");
            return false;
        }
        Log.d(TAG, "this is a oma drm file: " + str);
        return true;
    }

    public static boolean canBeForwarded(DrmManagerClient drmManagerClient, String str) {
        return drmManagerClient.checkRightsStatus(str, 3) == 0;
    }

    public static boolean canBeForwarded(DrmManagerClient drmManagerClient, Uri uri) {
        return drmManagerClient.checkRightsStatus(uri, 3) == 0;
    }

    public static boolean installDrmToDevice(DrmManagerClient drmManagerClient, String str) throws Throwable {
        FileOutputStream fileOutputStream;
        boolean zEquals;
        int iSaveRights;
        if (DEBUG) {
            Log.d(TAG, "installDrmToDevice: path = " + str);
        }
        if (TextUtils.isEmpty(str)) {
            Log.e(TAG, "installDrmToDevice : Given path is not valid");
            return false;
        }
        File file = new File(str);
        if (!file.exists()) {
            Log.e(TAG, "installDrmToDevice : Given file is not exist");
            return false;
        }
        FileOutputStream fileOutputStream2 = null;
        DrmRights drmRights = str.endsWith(OmaDrmStore.DrmFileExtension.EXTENSION_RIGHTS_XML) ? new DrmRights(file, OmaDrmStore.DrmObjectMimeType.MIME_TYPE_RIGHTS_XML) : str.endsWith(OmaDrmStore.DrmFileExtension.EXTENSION_RIGHTS_WBXML) ? new DrmRights(file, OmaDrmStore.DrmObjectMimeType.MIME_TYPE_RIGHTS_WBXML) : null;
        if (drmRights != null) {
            try {
                iSaveRights = drmManagerClient.saveRights(drmRights, null, null);
            } catch (IOException e) {
                Log.e(TAG, "installDrmToDevice : save rights with", e);
                iSaveRights = -2000;
            }
            Log.d(TAG, "installDrmToDevice : save rights with result " + iSaveRights);
            return iSaveRights == 0;
        }
        String strGenerateDcfFilePath = generateDcfFilePath(str);
        File file2 = new File(strGenerateDcfFilePath);
        try {
            try {
                if (!file2.exists()) {
                    file2.createNewFile();
                }
                fileOutputStream = new FileOutputStream(strGenerateDcfFilePath);
            } catch (Throwable th) {
                th = th;
                fileOutputStream = null;
            }
        } catch (IOException e2) {
            e = e2;
        }
        try {
            DrmInfoRequest drmInfoRequest = new DrmInfoRequest(2021, OmaDrmStore.DrmObjectMimeType.MIME_TYPE_DRM_MESSAGE);
            drmInfoRequest.put(OmaDrmInfoRequest.KEY_ACTION, OmaDrmInfoRequest.ACTION_INSTALL_DRM_TO_DEVICE);
            drmInfoRequest.put(OmaDrmInfoRequest.KEY_DATA, str);
            drmInfoRequest.put(OmaDrmInfoRequest.KEY_FILEDESCRIPTOR, formatFdToString(fileOutputStream.getFD()));
            Log.e(TAG, "client.acquireDrmInfo OmaDrmUtils 1 ");
            zEquals = OmaDrmInfoRequest.DrmRequestResult.RESULT_SUCCESS.equals(getResultFromDrmInfo(drmManagerClient.acquireDrmInfo(drmInfoRequest)));
            try {
                fileOutputStream.close();
            } catch (IOException e3) {
                Log.e(TAG, "close dcfStream with ", e3);
            }
        } catch (IOException e4) {
            e = e4;
            fileOutputStream2 = fileOutputStream;
            Log.e(TAG, "installDrmTodevice with ", e);
            if (fileOutputStream2 != null) {
                try {
                    fileOutputStream2.close();
                } catch (IOException e5) {
                    Log.e(TAG, "close dcfStream with ", e5);
                }
            }
            zEquals = false;
        } catch (Throwable th2) {
            th = th2;
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e6) {
                    Log.e(TAG, "close dcfStream with ", e6);
                }
            }
            throw th;
        }
        if (zEquals) {
            file.delete();
            String strSubstring = strGenerateDcfFilePath.substring(0, strGenerateDcfFilePath.lastIndexOf("."));
            if (!file2.renameTo(new File(strSubstring))) {
                Log.e(TAG, "installDrmTodevice failed due to rename [" + strGenerateDcfFilePath + "] -> [" + strSubstring + "] failed");
                return false;
            }
            strGenerateDcfFilePath = file2.getPath();
        } else {
            file2.delete();
        }
        if (DEBUG) {
            Log.d(TAG, "installDrmTodevice: [" + str + "] -> [" + strGenerateDcfFilePath + "] " + zEquals);
        }
        return zEquals;
    }

    public static int getActionByMimetype(String str) {
        int i = 0;
        if (!TextUtils.isEmpty(str)) {
            if (str.startsWith(OmaDrmStore.MimePrefix.IMAGE)) {
                i = 7;
            } else if (str.startsWith(OmaDrmStore.MimePrefix.VIDEO) || str.startsWith(OmaDrmStore.MimePrefix.AUDIO)) {
                i = 1;
            }
        }
        if (DEBUG) {
            Log.d(TAG, "getActionByMimetype: mimetype=" + str + ", action=" + i);
        }
        return i;
    }

    public static Bitmap overlapLockIcon(DrmManagerClient drmManagerClient, Resources resources, Bitmap bitmap, String str) {
        if (DEBUG) {
            Log.d(TAG, "overlapLockIcon(res): path = " + str);
        }
        int iCheckRightsStatus = 1;
        if (str != null) {
            iCheckRightsStatus = drmManagerClient.checkRightsStatus(str);
        }
        return overlapLockIcon(drmManagerClient, resources, bitmap, iCheckRightsStatus);
    }

    public static int checkRightsStatusByFd(DrmManagerClient drmManagerClient, FileDescriptor fileDescriptor) {
        int i;
        DrmInfoRequest drmInfoRequest = new DrmInfoRequest(2021, OmaDrmStore.DrmObjectMimeType.MIME_TYPE_DRM_CONTENT);
        drmInfoRequest.put(OmaDrmInfoRequest.KEY_ACTION, OmaDrmInfoRequest.ACTION_CHECK_RIGHTS_STATUS_BY_FD);
        drmInfoRequest.put(OmaDrmInfoRequest.KEY_FILEDESCRIPTOR, formatFdToString(fileDescriptor));
        Log.e(TAG, "client.acquireDrmInfo OmaDrmUtils 2 ");
        String resultFromDrmInfo = getResultFromDrmInfo(drmManagerClient.acquireDrmInfo(drmInfoRequest));
        try {
            i = Integer.parseInt(resultFromDrmInfo);
        } catch (NumberFormatException e) {
            Log.e(TAG, "checkRightsStatusByFd with " + e);
            i = 1;
        }
        if (DEBUG) {
            Log.d(TAG, "checkRightsStatusByFd: rightsStatus = " + resultFromDrmInfo);
        }
        return i;
    }

    public static Bitmap getOriginalLockIcon(DrmManagerClient drmManagerClient, Resources resources, String str) {
        int iCheckRightsStatus;
        int i;
        Integer asInteger;
        if (str != null) {
            ContentValues metadata = drmManagerClient.getMetadata(str);
            if (DEBUG) {
                Log.d(TAG, "getOriginalLockIcon: metadata = " + metadata);
            }
            if (metadata == null || (asInteger = metadata.getAsInteger(OmaDrmStore.MetadatasColumns.IS_DRM)) == null || asInteger.intValue() <= 0) {
                return null;
            }
            String asString = metadata.getAsString(OmaDrmStore.MetadatasColumns.DRM_MIME_TYPE);
            if (!TextUtils.isEmpty(asString)) {
                iCheckRightsStatus = drmManagerClient.checkRightsStatus(str, getActionByMimetype(asString));
            } else {
                iCheckRightsStatus = 1;
            }
        }
        if (iCheckRightsStatus == 0) {
            i = 134348871;
        } else {
            i = 134348872;
        }
        return BitmapFactory.decodeResource(resources, i);
    }

    public static Bitmap overlapLockIcon(DrmManagerClient drmManagerClient, Resources resources, int i, String str) {
        return overlapLockIcon(drmManagerClient, resources, BitmapFactory.decodeResource(resources, i), str);
    }

    public static Bitmap overlapLockIcon(DrmManagerClient drmManagerClient, Context context, int i, String str) {
        if (DEBUG) {
            Log.d(TAG, "overlapLockIcon: path " + str);
        }
        Resources resources = context.getResources();
        Bitmap bitmapDecodeResource = BitmapFactory.decodeResource(resources, i);
        int iCheckRightsStatus = 1;
        if (str != null) {
            iCheckRightsStatus = drmManagerClient.checkRightsStatus(str);
        }
        return overlapLockIcon(drmManagerClient, resources, bitmapDecodeResource, iCheckRightsStatus);
    }

    public static Bitmap overlapLockIcon(DrmManagerClient drmManagerClient, Context context, int i, Uri uri) {
        if (DEBUG) {
            Log.d(TAG, "overlapLockIcon: uri " + uri);
        }
        Resources resources = context.getResources();
        Bitmap bitmapDecodeResource = BitmapFactory.decodeResource(resources, i);
        int iCheckRightsStatus = 1;
        if (uri != null) {
            iCheckRightsStatus = drmManagerClient.checkRightsStatus(uri);
        }
        return overlapLockIcon(drmManagerClient, resources, bitmapDecodeResource, iCheckRightsStatus);
    }

    public static Bitmap overlapLockIcon(DrmManagerClient drmManagerClient, Context context, int i, FileDescriptor fileDescriptor) {
        if (DEBUG) {
            Log.d(TAG, "overlapLockIcon: fd " + fileDescriptor);
        }
        Resources resources = context.getResources();
        return overlapLockIcon(drmManagerClient, resources, BitmapFactory.decodeResource(resources, i), checkRightsStatusByFd(drmManagerClient, fileDescriptor));
    }

    public static Bitmap overlapLockIcon(DrmManagerClient drmManagerClient, Context context, Bitmap bitmap, String str) {
        if (DEBUG) {
            Log.d(TAG, "overlapLockIcon: path " + str);
        }
        Resources resources = context.getResources();
        int iCheckRightsStatus = 1;
        if (str != null) {
            iCheckRightsStatus = drmManagerClient.checkRightsStatus(str);
        }
        return overlapLockIcon(drmManagerClient, resources, bitmap, iCheckRightsStatus);
    }

    public static Bitmap overlapLockIcon(DrmManagerClient drmManagerClient, Context context, Bitmap bitmap, Uri uri) {
        if (DEBUG) {
            Log.d(TAG, "overlapLockIcon: uri " + uri);
        }
        Resources resources = context.getResources();
        int iCheckRightsStatus = 1;
        if (uri != null) {
            iCheckRightsStatus = drmManagerClient.checkRightsStatus(uri);
        }
        return overlapLockIcon(drmManagerClient, resources, bitmap, iCheckRightsStatus);
    }

    public static Bitmap overlapLockIcon(DrmManagerClient drmManagerClient, Context context, Bitmap bitmap, FileDescriptor fileDescriptor) {
        if (DEBUG) {
            Log.d(TAG, "overlapLockIcon: fd " + fileDescriptor);
        }
        return overlapLockIcon(drmManagerClient, context.getResources(), bitmap, checkRightsStatusByFd(drmManagerClient, fileDescriptor));
    }

    private static Bitmap overlapLockIcon(DrmManagerClient drmManagerClient, Resources resources, Bitmap bitmap, int i) {
        int i2;
        if (i == 0) {
            i2 = 134348871;
        } else {
            i2 = 134348872;
        }
        Drawable drawable = resources.getDrawable(i2);
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
        Canvas canvas = new Canvas(bitmapCreateBitmap);
        if (!bitmap.isRecycled()) {
            canvas.drawBitmap(bitmap, 0.0f, 0.0f, (Paint) null);
        }
        int intrinsicWidth = drawable.getIntrinsicWidth();
        int intrinsicHeight = drawable.getIntrinsicHeight();
        int width = bitmap.getWidth() - intrinsicWidth;
        int height = bitmap.getHeight() - intrinsicHeight;
        drawable.setBounds(new Rect(width, height, intrinsicWidth + width, intrinsicHeight + height));
        drawable.draw(canvas);
        return bitmapCreateBitmap;
    }

    public static void showConsumerDialog(Context context, DrmManagerClient drmManagerClient, Uri uri, DialogInterface.OnClickListener onClickListener) {
        showConsumerDialog(context, drmManagerClient, convertUriToPath(context, uri), onClickListener);
    }

    public static void showConsumerDialog(Context context, final DrmManagerClient drmManagerClient, String str, final DialogInterface.OnClickListener onClickListener) {
        if (DEBUG) {
            Log.d(TAG, "showConsumerDialog: path = " + str);
        }
        if (onClickListener == null) {
            Log.e(TAG, "showConsumerDialog, onClickListener is null.");
            return;
        }
        if (!sIsOmaDrmEnabled) {
            Log.d(TAG, "showConsumerDialog, oma drm disable.");
            onClickListener.onClick(null, -1);
            return;
        }
        if (TextUtils.isEmpty(str)) {
            Log.e(TAG, "showConsumerDialog: Given path is invalid");
            return;
        }
        if (!(context instanceof Activity)) {
            Log.e(TAG, "showConsumerDialog : not an acitivty context");
            onClickListener.onClick(null, -2);
            return;
        }
        ContentValues metadata = drmManagerClient.getMetadata(str);
        if (DEBUG) {
            Log.d(TAG, "showConsumerDialog: metadata = " + metadata);
        }
        if (metadata == null) {
            Log.d(TAG, "showConsumerDialog, get metadata is null, it's not drm file");
            onClickListener.onClick(null, -1);
            return;
        }
        Integer asInteger = metadata.getAsInteger(OmaDrmStore.MetadatasColumns.IS_DRM);
        if (asInteger == null || asInteger.intValue() <= 0) {
            Log.d(TAG, "showConsumerDialog, get metadata is null, it's not drm file");
            onClickListener.onClick(null, -1);
            return;
        }
        final String asString = metadata.getAsString(OmaDrmStore.MetadatasColumns.DRM_MIME_TYPE);
        int actionByMimetype = getActionByMimetype(asString);
        if (drmManagerClient.checkRightsStatus(str, actionByMimetype) != 0) {
            if (DEBUG) {
                Log.d(TAG, "showConsumerDialog: rights is invalid, play directly");
            }
            onClickListener.onClick(null, -1);
            return;
        }
        final String asString2 = metadata.getAsString(OmaDrmStore.MetadatasColumns.DRM_CONTENT_URI);
        DialogInterface.OnClickListener onClickListener2 = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (i == -1 && (asString.startsWith(OmaDrmStore.MimePrefix.IMAGE) || asString.startsWith(OmaDrmStore.MimePrefix.VIDEO))) {
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
            Log.d(TAG, "showConsumerDialog : constraints = " + constraints);
        }
        if (constraints == null || constraints.size() == 0) {
            Log.e(TAG, "showConsumerDialog : constraints is null, no rights");
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
            Log.w(TAG, "showConsumerDialog: max count or start time or available time is null");
            onClickListener2.onClick(null, -1);
            return;
        }
        if (asLong.longValue() > 0) {
            if (asLong2 == asLong) {
                sb.append(resources.getString(134545501));
                sb.append(SPACES);
                sb.append(resources.getString(134545502));
                sb.append(LINE_FEED);
                sb.append(resources.getString(134545478));
                sb.append(SPACES);
                sb.append(asLong2);
            } else if (asLong2.longValue() <= 2) {
                sb.append(resources.getString(134545478));
                sb.append(SPACES);
                sb.append(asLong2);
                sb.append(LINE_FEED);
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
            sb.append(SPACES);
            sb.append(resources.getString(134545502));
        } else {
            onClickListener2.onClick(null, -1);
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "showConsumerDialog with message: " + ((Object) sb));
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
        FragmentTransaction fragmentTransactionBeginTransaction = context.getFragmentManager().beginTransaction();
        fragmentTransactionBeginTransaction.add(sConsumeDialog, CONSUME_DIALOG_TAG);
        fragmentTransactionBeginTransaction.commitAllowingStateLoss();
        if (DEBUG) {
            Log.d(TAG, "showConsumerDialog: begin show dialog fragment");
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
            Log.d(TAG, "showProtectionInfoDialog: path=" + str + ", context=" + context);
        }
        if (!sIsOmaDrmEnabled) {
            Log.d(TAG, "showProtectionInfoDialog, oma drm is disable");
            return;
        }
        if (TextUtils.isEmpty(str)) {
            Log.e(TAG, "showProtectionInfoDialog: Given path is invalid");
            return;
        }
        if (!(context instanceof Activity)) {
            Log.e(TAG, "showConsumerDialog : not an Acitivty context");
            return;
        }
        ContentValues metadata = drmManagerClient.getMetadata(str);
        if (DEBUG) {
            Log.d(TAG, "showProtectionInfoDialog: metadata = " + metadata);
        }
        if (metadata == null) {
            Log.d(TAG, "showProtectionInfoDialog, get metadata is null, it's not drm file");
            return;
        }
        Integer asInteger = metadata.getAsInteger(OmaDrmStore.MetadatasColumns.IS_DRM);
        if (asInteger == null || asInteger.intValue() <= 0) {
            Log.d(TAG, "showProtectionInfoDialog, get metadata is null, it's not drm file");
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        StringBuilder sb = new StringBuilder();
        Resources resources = context.getResources();
        ContentValues constraints = drmManagerClient.getConstraints(str, getActionByMimetype(metadata.getAsString(OmaDrmStore.MetadatasColumns.DRM_MIME_TYPE)));
        if (DEBUG) {
            Log.d(TAG, "showProtectionInfoDialog : constraints = " + constraints);
        }
        sb.append(MediaFile.getFileTitle(str));
        sb.append(LINE_FEED);
        sb.append(resources.getString(134545470));
        sb.append(SPACES);
        if (metadata.getAsInteger(OmaDrmStore.MetadatasColumns.DRM_METHOD).intValue() == 4) {
            sb.append(resources.getString(134545471));
            sb.append(LINE_FEED);
        } else {
            sb.append(resources.getString(134545472));
            sb.append(LINE_FEED);
        }
        if (constraints == null || constraints.size() == 0) {
            sb.append(resources.getString(134545474));
            final String asString = metadata.getAsString(OmaDrmStore.MetadatasColumns.DRM_RIGHTS_ISSUER);
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
                Log.w(TAG, "showConsumerDialog:max count or start time or available time is null");
                return;
            }
            if (asLong2.longValue() > 0) {
                sb.append(resources.getString(134545478));
                sb.append(SPACES);
                sb.append(asLong2);
                sb.append(LINE_FEED);
            }
            if (asLong3.longValue() > 0 && asLong4.longValue() > 0) {
                sb.append(resources.getString(134545509));
                sb.append(SPACES);
                sb.append(toDateTimeString(asLong3));
                sb.append(" - ");
                sb.append(toDateTimeString(asLong4));
            } else if (asLong5.longValue() > 0) {
                String timeString = toTimeString(asLong5);
                sb.append(resources.getString(134545504));
                sb.append(SPACES);
                sb.append(timeString);
            }
        }
        if (DEBUG) {
            Log.d(TAG, "showProtectionIfoDialog : message = " + ((Object) sb));
        }
        builder.setTitle(134545506);
        builder.setMessage(sb);
        builder.setNeutralButton(R.string.ok, (DialogInterface.OnClickListener) null);
        ProtectionDialogFragment protectionDialogFragmentNewInstance = ProtectionDialogFragment.newInstance(builder);
        if (sProtectionInfoDialog != null) {
            sProtectionInfoDialog.dismissAllowingStateLoss();
        }
        sProtectionInfoDialog = protectionDialogFragmentNewInstance;
        FragmentTransaction fragmentTransactionBeginTransaction = context.getFragmentManager().beginTransaction();
        fragmentTransactionBeginTransaction.add(sProtectionInfoDialog, PROTECTION_INFO_DIALOG_TAG);
        fragmentTransactionBeginTransaction.commitAllowingStateLoss();
        if (DEBUG) {
            Log.d(TAG, "showProtectionInfoDialog: begin show dialog fragment");
        }
    }

    private static boolean markAsConsumeInAppClient(DrmManagerClient drmManagerClient, String str) {
        int callingPid = Binder.getCallingPid();
        if (DEBUG) {
            Log.d(TAG, "markAsConsumeInAppClient : pid = " + callingPid + ", cid = " + str);
        }
        DrmInfoRequest drmInfoRequest = new DrmInfoRequest(2021, OmaDrmStore.DrmObjectMimeType.MIME_TYPE_DRM_MESSAGE);
        drmInfoRequest.put(OmaDrmInfoRequest.KEY_ACTION, OmaDrmInfoRequest.ACTION_MARK_AS_CONSUME_IN_APP_CLIENT);
        drmInfoRequest.put(OmaDrmInfoRequest.KEY_DATA_1, String.valueOf(callingPid));
        if (str != null) {
            drmInfoRequest.put(OmaDrmInfoRequest.KEY_DATA_2, str);
        }
        Log.e(TAG, "client.acquireDrmInfo OmaDrmUtils 3 ");
        return OmaDrmInfoRequest.DrmRequestResult.RESULT_SUCCESS.equals(getResultFromDrmInfo(drmManagerClient.acquireDrmInfo(drmInfoRequest)));
    }

    private static String generateDcfFilePath(String str) {
        Log.v(TAG, "generateDcfFilePath : " + str);
        int iLastIndexOf = str.lastIndexOf(".");
        if (-1 != iLastIndexOf) {
            return str.substring(0, iLastIndexOf) + ".dcf.tmp";
        }
        return null;
    }

    private static String toTimeString(Long l) {
        Long l2 = 60L;
        Long l3 = 10L;
        Long lValueOf = Long.valueOf(l.longValue() / (l2.longValue() * l2.longValue()));
        Long lValueOf2 = Long.valueOf((l.longValue() - ((lValueOf.longValue() * l2.longValue()) * l2.longValue())) / l2.longValue());
        Long lValueOf3 = Long.valueOf((l.longValue() - ((lValueOf.longValue() * l2.longValue()) * l2.longValue())) - (lValueOf2.longValue() * l2.longValue()));
        StringBuilder sb = new StringBuilder();
        if (lValueOf.longValue() < l3.longValue()) {
            sb.append(SchemaSymbols.ATTVAL_FALSE_0);
            sb.append(lValueOf.toString());
        } else {
            sb.append(lValueOf.toString());
        }
        sb.append(":");
        if (lValueOf2.longValue() < l3.longValue()) {
            sb.append(SchemaSymbols.ATTVAL_FALSE_0);
            sb.append(lValueOf2.toString());
        } else {
            sb.append(lValueOf2.toString());
        }
        sb.append(":");
        if (lValueOf3.longValue() < l3.longValue()) {
            sb.append(SchemaSymbols.ATTVAL_FALSE_0);
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
        Log.e(TAG, "toDateTimeString ");
        return simpleDateFormat.format(date);
    }

    public static boolean isTokenValid(DrmManagerClient drmManagerClient, String str, String str2) {
        Log.d(TAG, "isTokenValid filePath:" + str);
        DrmInfoRequest drmInfoRequest = new DrmInfoRequest(2022, OmaDrmStore.DrmObjectMimeType.MIME_TYPE_CTA5_MESSAGE);
        drmInfoRequest.put(OmaDrmInfoRequest.KEY_ACTION, OmaDrmInfoRequest.ACTION_CTA5_CHECKTOKEN);
        drmInfoRequest.put(OmaDrmInfoRequest.KEY_CTA5_FILEPATH, str);
        drmInfoRequest.put(OmaDrmInfoRequest.KEY_CTA5_TOKEN, str2);
        Log.e(TAG, "client.acquireDrmInfo OmaDrmUtils 4 ");
        return OmaDrmInfoRequest.DrmRequestResult.RESULT_SUCCESS.equals(getResultFromDrmInfo(drmManagerClient.acquireDrmInfo(drmInfoRequest)));
    }

    public static boolean clearToken(DrmManagerClient drmManagerClient, String str, String str2) {
        Log.d(TAG, "clearToken filePath:" + str);
        DrmInfoRequest drmInfoRequest = new DrmInfoRequest(2022, OmaDrmStore.DrmObjectMimeType.MIME_TYPE_CTA5_MESSAGE);
        drmInfoRequest.put(OmaDrmInfoRequest.KEY_ACTION, OmaDrmInfoRequest.ACTION_CTA5_CLEARTOKEN);
        drmInfoRequest.put(OmaDrmInfoRequest.KEY_CTA5_FILEPATH, str);
        drmInfoRequest.put(OmaDrmInfoRequest.KEY_CTA5_TOKEN, str2);
        Log.e(TAG, "client.acquireDrmInfo OmaDrmUtils 5");
        return OmaDrmInfoRequest.DrmRequestResult.RESULT_SUCCESS.equals(getResultFromDrmInfo(drmManagerClient.acquireDrmInfo(drmInfoRequest)));
    }

    public static byte[] forceDecryptFile(String str, boolean z) {
        return new DcfDecoder().forceDecryptFile(str, z);
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
            Log.e(TAG, "Unsupported hongen encoding type of the returned DrmInfo data");
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
                String[] strArr = {BookmarkEnhance.COLUMN_DATA};
                ContentResolver contentResolver = context.getContentResolver();
                try {
                    try {
                        cursorQuery = contentResolver.query(uri, strArr, null, null, null);
                        if (cursorQuery != null) {
                            try {
                                if (cursorQuery.moveToFirst()) {
                                    int columnIndex = cursorQuery.getColumnIndex(BookmarkEnhance.COLUMN_DATA);
                                    if (columnIndex != -1) {
                                        path = cursorQuery.getString(columnIndex);
                                    } else {
                                        if (cursorQuery != null) {
                                            cursorQuery.close();
                                            cursorQuery = null;
                                        }
                                        String contentUri = getContentUri(context, uri);
                                        if (contentUri != null) {
                                            Cursor cursorQuery2 = contentResolver.query(FILE_URI, new String[]{BookmarkEnhance.COLUMN_DATA}, "drm_content_uri=?", new String[]{contentUri}, null);
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
            Log.d(TAG, "convertUriToPath: uri = " + uri + " --> path = " + path);
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
                                        Log.e(TAG, "getContentUri: IOException fail with " + uri, e);
                                        if (inputStream != null) {
                                            try {
                                                inputStream.close();
                                            } catch (Exception e3) {
                                                Log.e(TAG, "getContentUri: close input stream fail with " + e3);
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
                                    Log.e(TAG, "getContentUri: close input stream fail with " + e4);
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
                        Log.e(TAG, "getContentUri: close input stream fail with " + e6);
                    }
                }
            } catch (IOException e7) {
                e = e7;
                str = null;
            }
            if (DEBUG) {
                Log.d(TAG, "getContentUri: uri = " + uri + ", contentUri = " + strSubstring);
            }
            return strSubstring;
        } catch (Throwable th2) {
            th = th2;
            inputStreamOpenInputStream = inputStream;
        }
    }

    private static String formatFdToString(FileDescriptor fileDescriptor) {
        return "FileDescriptor[" + fileDescriptor.getInt$() + "]";
    }
}
