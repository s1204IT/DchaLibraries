package com.android.launcher3;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import com.android.launcher3.allapps.AllAppsSearchBarController;
import com.android.launcher3.util.ComponentKey;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public interface LauncherCallbacks {
    void bindAllApplications(ArrayList<AppInfo> arrayList);

    void dump(String str, FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr);

    void finishBindingItems(boolean z);

    Bundle getAdditionalSearchWidgetOptions();

    AllAppsSearchBarController getAllAppsSearchBarController();

    Intent getFirstRunActivity();

    View getIntroScreen();

    List<ComponentKey> getPredictedApps();

    View getQsbBar();

    int getSearchBarHeight();

    boolean handleBackPressed();

    boolean hasCustomContentToLeft();

    boolean hasDismissableIntroScreen();

    boolean hasFirstRunActivity();

    boolean hasSettings();

    boolean isLauncherPreinstalled();

    void onActivityResult(int i, int i2, Intent intent);

    void onAttachedToWindow();

    void onClickAddWidgetButton(View view);

    void onClickAllAppsButton(View view);

    void onClickAppShortcut(View view);

    void onClickFolderIcon(View view);

    void onClickSettingsButton(View view);

    void onClickWallpaperPicker(View view);

    void onCreate(Bundle bundle);

    void onDestroy();

    void onDetachedFromWindow();

    void onDragStarted(View view);

    void onHomeIntent();

    void onInteractionBegin();

    void onInteractionEnd();

    void onLauncherProviderChange();

    void onNewIntent(Intent intent);

    void onPageSwitch(View view, int i);

    void onPause();

    void onPostCreate(Bundle bundle);

    boolean onPrepareOptionsMenu(Menu menu);

    void onRequestPermissionsResult(int i, String[] strArr, int[] iArr);

    void onResume();

    void onSaveInstanceState(Bundle bundle);

    void onStart();

    void onStop();

    void onTrimMemory(int i);

    void onWindowFocusChanged(boolean z);

    void onWorkspaceLockedChanged();

    boolean overrideWallpaperDimensions();

    void populateCustomContentContainer();

    void preOnCreate();

    void preOnResume();

    boolean providesSearch();

    boolean shouldMoveToDefaultScreenOnHomeIntent();

    boolean startSearch(String str, boolean z, Bundle bundle, Rect rect);
}
