package com.android.documentsui;

import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.DocumentsContract;
import android.util.Log;
import com.android.documentsui.base.DocumentInfo;
import com.android.documentsui.base.RootInfo;
import com.android.documentsui.base.SharedMinimal;
import com.android.documentsui.base.State;
import com.android.internal.logging.MetricsLogger;
import java.util.Iterator;
import java.util.List;

public final class Metrics {
    static final boolean $assertionsDisabled = false;

    public static void logActivityLaunch(Context context, State state, Intent intent) {
        logHistogram(context, "docsui_launch_action", toMetricsAction(state.action));
        Uri data = intent.getData();
        int i = state.action;
        if (i != 1) {
            switch (i) {
                case 3:
                    logHistogram(context, "docsui_open_mime", sanitizeMime(intent.getType()));
                    break;
                case 4:
                    logHistogram(context, "docsui_create_mime", sanitizeMime(intent.getType()));
                    break;
                case 5:
                    logHistogram(context, "docsui_get_content_mime", sanitizeMime(intent.getType()));
                    break;
            }
        }
        logHistogram(context, "docsui_browse_root", sanitizeRoot(data));
    }

    public static void logLaunchAtLocation(Context context, State state, Uri uri) {
        int i = state.action;
        if (i == 1) {
            logHistogram(context, "docsui_browse_at_location", sanitizeRoot(uri));
        }
        switch (i) {
            case 3:
                logHistogram(context, "docsui_open_at_location", sanitizeRoot(uri));
                break;
            case 4:
                logHistogram(context, "docsui_create_at_location", sanitizeRoot(uri));
                break;
            case 5:
                logHistogram(context, "docsui_get_content_at_location", sanitizeRoot(uri));
                break;
        }
    }

    public static void logRootVisited(Context context, int i, RootInfo rootInfo) {
        switch (i) {
            case 1:
                logHistogram(context, "docsui_root_visited_in_manager", sanitizeRoot(rootInfo));
                break;
            case 2:
                logHistogram(context, "docsui_root_visited_in_picker", sanitizeRoot(rootInfo));
                break;
        }
    }

    public static void logAppVisited(Context context, ResolveInfo resolveInfo) {
        logHistogram(context, "docsui_root_visited_in_picker", sanitizeRoot(resolveInfo));
    }

    public static void logFileOperation(Context context, int i, List<DocumentInfo> list, DocumentInfo documentInfo) {
        ProviderCounts providerCounts = new ProviderCounts();
        countProviders(providerCounts, list, documentInfo);
        if (providerCounts.intraProvider > 0) {
            logIntraProviderFileOps(context, documentInfo.authority, i);
        }
        if (providerCounts.systemProvider > 0) {
            logInterProviderFileOps(context, "docsui_fileop_system", documentInfo, i);
        }
        if (providerCounts.externalProvider > 0) {
            logInterProviderFileOps(context, "docsui_fileop_external", documentInfo, i);
        }
    }

    public static void logFileOperated(Context context, int i, int i2) {
        if (i == 1) {
            logHistogram(context, "docsui_file_copied", i2);
        } else if (i == 4) {
            logHistogram(context, "docsui_file_moved", i2);
        }
    }

    public static void logCreateDirOperation(Context context) {
        logHistogram(context, "docsui_fileop_system", 10);
    }

    public static void logRenameFileOperation(Context context) {
        logHistogram(context, "docsui_fileop_system", 9);
    }

    public static void logFileOperationErrors(Context context, int i, List<DocumentInfo> list, List<Uri> list2) {
        int i2;
        ProviderCounts providerCounts = new ProviderCounts();
        countProviders(providerCounts, list, null);
        countProviders(providerCounts, list2);
        switch (i) {
            case 1:
                i2 = 103;
                break;
            case 2:
                i2 = 113;
                break;
            case 3:
                i2 = 112;
                break;
            case 4:
                i2 = 102;
                break;
            case 5:
                i2 = 101;
                break;
            default:
                i2 = 100;
                break;
        }
        if (providerCounts.systemProvider > 0) {
            logHistogram(context, "docsui_fileop_system", i2);
        }
        if (providerCounts.externalProvider > 0) {
            logHistogram(context, "docsui_fileop_external", i2);
        }
    }

    public static void logFileOperationFailure(Context context, int i, Uri uri) {
        byte b;
        String authority = uri.getAuthority();
        int iHashCode = authority.hashCode();
        if (iHashCode != -849996601) {
            if (iHashCode != 320699453) {
                if (iHashCode != 596745902) {
                    b = (iHashCode == 1734583286 && authority.equals("com.android.providers.media.documents")) ? (byte) 0 : (byte) -1;
                } else if (authority.equals("com.android.externalstorage.documents")) {
                    b = 1;
                }
            } else if (authority.equals("com.android.providers.downloads.documents")) {
                b = 2;
            }
        } else if (authority.equals("com.android.mtp.documents")) {
            b = 3;
        }
        switch (b) {
            case 0:
                logHistogram(context, "docsui_media_fileop_failure", i);
                break;
            case 1:
                logStorageFileOperationFailure(context, i, uri);
                break;
            case 2:
                logHistogram(context, "docsui_downloads_fileop_failure", i);
                break;
            case 3:
                logHistogram(context, "docsui_mtp_fileop_failure", i);
                break;
            default:
                logHistogram(context, "docsui_other_fileop_failure", i);
                break;
        }
    }

    public static void logCreateDirError(Context context) {
        logHistogram(context, "docsui_fileop_system", 105);
    }

    public static void logRenameFileError(Context context) {
        logHistogram(context, "docsui_fileop_system", 104);
    }

    public static void logFileOperationCancelled(Context context, int i) {
        logHistogram(context, "docsui_fileop_canceled", toMetricsOpType(i));
    }

    public static void logStartupMs(Context context, int i) {
        logHistogram(context, "docsui_startup_ms", i);
    }

    private static void logInterProviderFileOps(Context context, String str, DocumentInfo documentInfo, int i) {
        if (i == 5) {
            logHistogram(context, str, 8);
        } else {
            logHistogram(context, str, getOpCode(i, isSystemProvider(documentInfo.authority) ? 1 : 2));
        }
    }

    private static void logIntraProviderFileOps(Context context, String str, int i) {
        logHistogram(context, isSystemProvider(str) ? "docsui_fileop_system" : "docsui_fileop_external", getOpCode(i, 0));
    }

    public static void logUserAction(Context context, int i) {
        logHistogram(context, "docsui_menu_action", i);
    }

    private static void logStorageFileOperationFailure(Context context, int i, Uri uri) {
        boolean z;
        String str;
        ContentProviderClient contentProviderClientAcquireUnstableProviderOrThrow;
        Throwable th;
        try {
            contentProviderClientAcquireUnstableProviderOrThrow = DocumentsApplication.acquireUnstableProviderOrThrow(context.getContentResolver(), "com.android.externalstorage.documents");
            th = null;
        } catch (RemoteException | RuntimeException e) {
            Log.e("Metrics", "Failed to obtain its root info. Log the metrics as internal.", e);
            z = true;
        }
        try {
            try {
                z = !DocumentsApplication.getProvidersCache(context).getRootOneshot("com.android.externalstorage.documents", DocumentsContract.findDocumentPath(contentProviderClientAcquireUnstableProviderOrThrow, uri).getRootId()).supportsEject();
                if (contentProviderClientAcquireUnstableProviderOrThrow != null) {
                    contentProviderClientAcquireUnstableProviderOrThrow.close();
                }
                if (z) {
                    str = "docsui_internal_storage_fileop_failure";
                } else {
                    str = "docsui_external_storage_fileop_failure";
                }
                logHistogram(context, str, i);
            } finally {
            }
        } catch (Throwable th2) {
            if (contentProviderClientAcquireUnstableProviderOrThrow != null) {
                if (th != null) {
                    try {
                        contentProviderClientAcquireUnstableProviderOrThrow.close();
                    } catch (Throwable th3) {
                        th.addSuppressed(th3);
                    }
                } else {
                    contentProviderClientAcquireUnstableProviderOrThrow.close();
                }
            }
            throw th2;
        }
    }

    private static void logHistogram(Context context, String str, int i) {
        if (SharedMinimal.DEBUG) {
            Log.d("Metrics", str + ": " + i);
        }
        MetricsLogger.histogram(context, str, i);
    }

    private static int sanitizeRoot(android.net.Uri r8) {
        r0 = 1;
        if (r8 == null || r8.getAuthority() == null || com.android.documentsui.files.LauncherActivity.isLaunchUri(r8)) {
            return 1;
        } else {
            r1 = r8.getAuthority();
            r2 = r1.hashCode();
            if (r2 != -849996601) {
                if (r2 != 320699453) {
                    if (r2 != 596745902) {
                        if (r2 == 1734583286 && r1.equals("com.android.providers.media.documents")) {
                            r1 = 0;
                        } else {
                            r1 = -1;
                        }
                    } else {
                        if (r1.equals("com.android.externalstorage.documents")) {
                            r1 = 1;
                        }
                    }
                } else {
                    if (r1.equals("com.android.providers.downloads.documents")) {
                        r1 = 2;
                    }
                }
            } else {
                if (r1.equals("com.android.mtp.documents")) {
                    r1 = 3;
                }
            }
            switch (r1) {
                case 0:
                    r8 = android.provider.DocumentsContract.getRootId(r8);
                    r1 = r8.hashCode();
                    if (r1 != -1222868407) {
                        if (r1 != 1549308843) {
                            if (r1 == 1939674473 && r8.equals("videos_root")) {
                                r0 = 2;
                            } else {
                                r0 = -1;
                            }
                        } else {
                            if (r8.equals("audio_root")) {
                                r0 = 0;
                            }
                        }
                    } else {
                        if (!r8.equals("images_root")) {
                        }
                    }
                    switch (r0) {
                    }
                case 1:
                    if ("home".equals(android.provider.DocumentsContract.getRootId(r8))) {
                    }
            }
            return 2;
        }
    }

    private static int sanitizeRoot(RootInfo rootInfo) {
        if (rootInfo.isRecents()) {
            return 8;
        }
        return sanitizeRoot(rootInfo.getUri());
    }

    private static int sanitizeRoot(ResolveInfo resolveInfo) {
        return 100;
    }

    private static int sanitizeMime(String str) {
        if (str == null) {
            return 1;
        }
        if ("*/*".equals(str)) {
            return 2;
        }
        switch (str.substring(0, str.indexOf(47))) {
            case "application":
                return 3;
            case "audio":
                return 4;
            case "image":
                return 5;
            case "message":
                return 6;
            case "multipart":
                return 7;
            case "text":
                return 8;
            case "video":
                return 9;
            default:
                return 10;
        }
    }

    private static boolean isSystemProvider(String str) {
        byte b;
        int iHashCode = str.hashCode();
        if (iHashCode != 320699453) {
            if (iHashCode != 596745902) {
                b = (iHashCode == 1734583286 && str.equals("com.android.providers.media.documents")) ? (byte) 0 : (byte) -1;
            } else if (str.equals("com.android.externalstorage.documents")) {
                b = 1;
            }
        } else if (str.equals("com.android.providers.downloads.documents")) {
            b = 2;
        }
        switch (b) {
            case 0:
            case 1:
            case 2:
                return true;
            default:
                return false;
        }
    }

    private static int getOpCode(int r0, int r1) {
        throw new UnsupportedOperationException("Method not decompiled: com.android.documentsui.Metrics.getOpCode(int, int):int");
    }

    private static int toMetricsOpType(int i) {
        if (i == 1) {
            return 2;
        }
        switch (i) {
            case 4:
                return 3;
            case 5:
                return 4;
            default:
                return 1;
        }
    }

    private static int toMetricsAction(int i) {
        switch (i) {
            case 1:
                return 7;
            case 2:
                return 8;
            case 3:
                return 2;
            case 4:
                return 3;
            case 5:
                return 4;
            case 6:
                return 5;
            default:
                return 1;
        }
    }

    private static void countProviders(ProviderCounts providerCounts, List<DocumentInfo> list, DocumentInfo documentInfo) {
        Iterator<DocumentInfo> it = list.iterator();
        while (it.hasNext()) {
            countForAuthority(providerCounts, it.next().authority, documentInfo);
        }
    }

    private static void countProviders(ProviderCounts providerCounts, List<Uri> list) {
        Iterator<Uri> it = list.iterator();
        while (it.hasNext()) {
            countForAuthority(providerCounts, it.next().getAuthority(), null);
        }
    }

    private static void countForAuthority(ProviderCounts providerCounts, String str, DocumentInfo documentInfo) {
        if (documentInfo != null && str.equals(documentInfo.authority)) {
            providerCounts.intraProvider++;
        } else if (isSystemProvider(str)) {
            providerCounts.systemProvider++;
        } else {
            providerCounts.externalProvider++;
        }
    }

    private static class ProviderCounts {
        int externalProvider;
        int intraProvider;
        int systemProvider;

        private ProviderCounts() {
        }
    }
}
