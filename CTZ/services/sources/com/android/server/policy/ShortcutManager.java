package com.android.server.policy;

import android.R;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.XmlResourceParser;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyCharacterMap;
import com.android.internal.util.XmlUtils;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.slice.SliceClientPermissions;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParserException;

class ShortcutManager {
    private static final String ATTRIBUTE_CATEGORY = "category";
    private static final String ATTRIBUTE_CLASS = "class";
    private static final String ATTRIBUTE_PACKAGE = "package";
    private static final String ATTRIBUTE_SHIFT = "shift";
    private static final String ATTRIBUTE_SHORTCUT = "shortcut";
    private static final String TAG = "ShortcutManager";
    private static final String TAG_BOOKMARK = "bookmark";
    private static final String TAG_BOOKMARKS = "bookmarks";
    private final Context mContext;
    private final SparseArray<ShortcutInfo> mShortcuts = new SparseArray<>();
    private final SparseArray<ShortcutInfo> mShiftShortcuts = new SparseArray<>();

    public ShortcutManager(Context context) throws PackageManager.NameNotFoundException {
        this.mContext = context;
        loadShortcuts();
    }

    public Intent getIntent(KeyCharacterMap keyCharacterMap, int i, int i2) {
        ShortcutInfo shortcutInfo;
        char lowerCase;
        SparseArray<ShortcutInfo> sparseArray = (i2 & 1) == 1 ? this.mShiftShortcuts : this.mShortcuts;
        int i3 = keyCharacterMap.get(i, i2);
        if (i3 != 0) {
            shortcutInfo = sparseArray.get(i3);
        } else {
            shortcutInfo = null;
        }
        if (shortcutInfo == null && (lowerCase = Character.toLowerCase(keyCharacterMap.getDisplayLabel(i))) != 0) {
            shortcutInfo = sparseArray.get(lowerCase);
        }
        if (shortcutInfo != null) {
            return shortcutInfo.intent;
        }
        return null;
    }

    private void loadShortcuts() throws PackageManager.NameNotFoundException {
        String string;
        Intent intentMakeMainSelectorActivity;
        ActivityInfo activityInfo;
        PackageManager packageManager = this.mContext.getPackageManager();
        try {
            XmlResourceParser xml = this.mContext.getResources().getXml(R.xml.bookmarks);
            XmlUtils.beginDocument(xml, TAG_BOOKMARKS);
            while (true) {
                XmlUtils.nextElement(xml);
                if (xml.getEventType() != 1 && TAG_BOOKMARK.equals(xml.getName())) {
                    String attributeValue = xml.getAttributeValue(null, "package");
                    String attributeValue2 = xml.getAttributeValue(null, "class");
                    String attributeValue3 = xml.getAttributeValue(null, ATTRIBUTE_SHORTCUT);
                    String attributeValue4 = xml.getAttributeValue(null, ATTRIBUTE_CATEGORY);
                    String attributeValue5 = xml.getAttributeValue(null, ATTRIBUTE_SHIFT);
                    if (TextUtils.isEmpty(attributeValue3)) {
                        Log.w(TAG, "Unable to get shortcut for: " + attributeValue + SliceClientPermissions.SliceAuthority.DELIMITER + attributeValue2);
                    } else {
                        char cCharAt = attributeValue3.charAt(0);
                        boolean z = attributeValue5 != null && attributeValue5.equals("true");
                        if (attributeValue != null && attributeValue2 != null) {
                            ComponentName componentName = new ComponentName(attributeValue, attributeValue2);
                            try {
                                activityInfo = packageManager.getActivityInfo(componentName, 794624);
                            } catch (PackageManager.NameNotFoundException e) {
                                ComponentName componentName2 = new ComponentName(packageManager.canonicalToCurrentPackageNames(new String[]{attributeValue})[0], attributeValue2);
                                try {
                                    activityInfo = packageManager.getActivityInfo(componentName2, 794624);
                                    componentName = componentName2;
                                } catch (PackageManager.NameNotFoundException e2) {
                                    Log.w(TAG, "Unable to add bookmark: " + attributeValue + SliceClientPermissions.SliceAuthority.DELIMITER + attributeValue2, e);
                                }
                            }
                            intentMakeMainSelectorActivity = new Intent("android.intent.action.MAIN");
                            intentMakeMainSelectorActivity.addCategory("android.intent.category.LAUNCHER");
                            intentMakeMainSelectorActivity.setComponent(componentName);
                            string = activityInfo.loadLabel(packageManager).toString();
                            ShortcutInfo shortcutInfo = new ShortcutInfo(string, intentMakeMainSelectorActivity);
                            if (!z) {
                            }
                        } else if (attributeValue4 != null) {
                            intentMakeMainSelectorActivity = Intent.makeMainSelectorActivity("android.intent.action.MAIN", attributeValue4);
                            string = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
                            ShortcutInfo shortcutInfo2 = new ShortcutInfo(string, intentMakeMainSelectorActivity);
                            if (!z) {
                                this.mShiftShortcuts.put(cCharAt, shortcutInfo2);
                            } else {
                                this.mShortcuts.put(cCharAt, shortcutInfo2);
                            }
                        } else {
                            Log.w(TAG, "Unable to add bookmark for shortcut " + attributeValue3 + ": missing package/class or category attributes");
                        }
                    }
                }
                return;
            }
        } catch (IOException e3) {
            Log.w(TAG, "Got exception parsing bookmarks.", e3);
        } catch (XmlPullParserException e4) {
            Log.w(TAG, "Got exception parsing bookmarks.", e4);
        }
    }

    private static final class ShortcutInfo {
        public final Intent intent;
        public final String title;

        public ShortcutInfo(String str, Intent intent) {
            this.title = str;
            this.intent = intent;
        }
    }
}
