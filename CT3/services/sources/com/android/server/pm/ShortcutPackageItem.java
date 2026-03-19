package com.android.server.pm;

import android.content.pm.PackageInfo;
import com.android.internal.util.Preconditions;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

abstract class ShortcutPackageItem {
    private static final String TAG = "ShortcutService";
    private final ShortcutPackageInfo mPackageInfo;
    private final String mPackageName;
    private final int mPackageUserId;
    protected final ShortcutUser mShortcutUser;

    public abstract int getOwnerUserId();

    protected abstract void onRestoreBlocked(ShortcutService shortcutService);

    protected abstract void onRestored(ShortcutService shortcutService);

    public abstract void saveToXml(XmlSerializer xmlSerializer, boolean z) throws XmlPullParserException, IOException;

    protected ShortcutPackageItem(ShortcutUser shortcutUser, int packageUserId, String packageName, ShortcutPackageInfo packageInfo) {
        this.mShortcutUser = shortcutUser;
        this.mPackageUserId = packageUserId;
        this.mPackageName = (String) Preconditions.checkStringNotEmpty(packageName);
        this.mPackageInfo = (ShortcutPackageInfo) Preconditions.checkNotNull(packageInfo);
    }

    public int getPackageUserId() {
        return this.mPackageUserId;
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    public ShortcutPackageInfo getPackageInfo() {
        return this.mPackageInfo;
    }

    public void refreshPackageInfoAndSave(ShortcutService s) {
        if (this.mPackageInfo.isShadow()) {
            return;
        }
        this.mPackageInfo.refresh(s, this);
        s.scheduleSaveUser(getOwnerUserId());
    }

    public void attemptToRestoreIfNeededAndSave(ShortcutService s) {
        if (!this.mPackageInfo.isShadow() || !s.isPackageInstalled(this.mPackageName, this.mPackageUserId)) {
            return;
        }
        if (!this.mPackageInfo.hasSignatures()) {
            s.wtf("Attempted to restore package " + this.mPackageName + ", user=" + this.mPackageUserId + " but signatures not found in the restore data.");
            onRestoreBlocked(s);
            return;
        }
        PackageInfo pi = s.getPackageInfoWithSignatures(this.mPackageName, this.mPackageUserId);
        if (!this.mPackageInfo.canRestoreTo(s, pi)) {
            onRestoreBlocked(s);
            return;
        }
        onRestored(s);
        this.mPackageInfo.setShadow(false);
        s.scheduleSaveUser(this.mPackageUserId);
    }
}
