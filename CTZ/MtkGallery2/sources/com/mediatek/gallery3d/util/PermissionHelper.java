package com.mediatek.gallery3d.util;

import android.app.Activity;
import android.content.Context;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;
import com.mediatek.galleryportable.RuntimePermissionUtils;
import java.util.ArrayList;

public class PermissionHelper {
    public static boolean checkAndRequestForGallery(Activity activity) {
        ArrayList<String> arrayList = new ArrayList();
        arrayList.add("android.permission.READ_EXTERNAL_STORAGE");
        arrayList.add("android.permission.WRITE_EXTERNAL_STORAGE");
        ArrayList arrayList2 = new ArrayList();
        for (String str : arrayList) {
            if (ContextCompat.checkSelfPermission(activity, str) != 0) {
                arrayList2.add(str);
            }
        }
        if (arrayList2.size() == 0) {
            Log.d("MtkGallery2/PermissionHelper", "<checkAndRequestForGallery> all permissions are granted");
            return true;
        }
        Log.d("MtkGallery2/PermissionHelper", "<checkAndRequestForGallery> not all permissions are granted, reuqest");
        ActivityCompat.requestPermissions(activity, (String[]) arrayList2.toArray(new String[arrayList2.size()]), 0);
        return false;
    }

    public static boolean checkAndRequestForLocationCluster(Activity activity) {
        if (ContextCompat.checkSelfPermission(activity, "android.permission.ACCESS_FINE_LOCATION") != 0) {
            Log.d("MtkGallery2/PermissionHelper", "<checkAndRequestForLocationCluster> permission not granted, reuqest");
            ActivityCompat.requestPermissions(activity, new String[]{"android.permission.ACCESS_FINE_LOCATION"}, 0);
            return false;
        }
        Log.d("MtkGallery2/PermissionHelper", "<checkAndRequestForLocationCluster> all permissions are granted");
        return true;
    }

    public static boolean checkAndRequestForWidget(Activity activity) {
        if (!checkStoragePermission(activity)) {
            Log.d("MtkGallery2/PermissionHelper", "<checkAndRequestForWidget> permission not granted, reuqest");
            ActivityCompat.requestPermissions(activity, new String[]{"android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE"}, 0);
            return false;
        }
        Log.d("MtkGallery2/PermissionHelper", "<checkAndRequestForWidget> all permissions are granted");
        return true;
    }

    public static boolean checkForFilterShow(Activity activity) {
        if (!checkStoragePermission(activity)) {
            Log.d("MtkGallery2/PermissionHelper", "<checkForFilterShow> permission not granted, finish");
            showDeniedPrompt(activity);
            activity.finish();
            return false;
        }
        Log.d("MtkGallery2/PermissionHelper", "<checkForFilterShow> all permissions are granted");
        return true;
    }

    public static boolean isAllPermissionsGranted(String[] strArr, int[] iArr) {
        for (int i : iArr) {
            if (i != 0) {
                return false;
            }
        }
        return true;
    }

    public static boolean checkStoragePermission(Context context) {
        boolean z = ContextCompat.checkSelfPermission(context, "android.permission.WRITE_EXTERNAL_STORAGE") == 0;
        boolean z2 = ContextCompat.checkSelfPermission(context, "android.permission.READ_EXTERNAL_STORAGE") == 0;
        Log.d("MtkGallery2/PermissionHelper", "<checkStoragePermission> writeGranted = " + z + ", readGranted = " + z2);
        return z && z2;
    }

    public static boolean checkLocationPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, "android.permission.ACCESS_FINE_LOCATION") == 0;
    }

    public static void showDeniedPrompt(Context context) {
        Toast.makeText(context, RuntimePermissionUtils.getDeniedPermissionString(context), 0).show();
    }

    public static boolean showDeniedPromptIfNeeded(Activity activity, String str) {
        if (!ActivityCompat.shouldShowRequestPermissionRationale(activity, str)) {
            showDeniedPrompt(activity.getApplicationContext());
            return true;
        }
        return false;
    }
}
