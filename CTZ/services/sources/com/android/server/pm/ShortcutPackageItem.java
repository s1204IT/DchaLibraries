package com.android.server.pm;

import android.content.pm.PackageInfo;
import com.android.internal.util.Preconditions;
import com.android.server.hdmi.HdmiCecKeycode;
import java.io.IOException;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

abstract class ShortcutPackageItem {
    private static final String KEY_NAME = "name";
    private static final String TAG = "ShortcutService";
    private final ShortcutPackageInfo mPackageInfo;
    private final String mPackageName;
    private final int mPackageUserId;
    protected ShortcutUser mShortcutUser;

    protected abstract boolean canRestoreAnyVersion();

    public abstract int getOwnerUserId();

    protected abstract void onRestored(int i);

    public abstract void saveToXml(XmlSerializer xmlSerializer, boolean z) throws XmlPullParserException, IOException;

    protected ShortcutPackageItem(ShortcutUser shortcutUser, int i, String str, ShortcutPackageInfo shortcutPackageInfo) {
        this.mShortcutUser = shortcutUser;
        this.mPackageUserId = i;
        this.mPackageName = (String) Preconditions.checkStringNotEmpty(str);
        this.mPackageInfo = (ShortcutPackageInfo) Preconditions.checkNotNull(shortcutPackageInfo);
    }

    public void replaceUser(ShortcutUser shortcutUser) {
        this.mShortcutUser = shortcutUser;
    }

    public ShortcutUser getUser() {
        return this.mShortcutUser;
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

    public void refreshPackageSignatureAndSave() {
        if (this.mPackageInfo.isShadow()) {
            return;
        }
        ShortcutService shortcutService = this.mShortcutUser.mService;
        this.mPackageInfo.refreshSignature(shortcutService, this);
        shortcutService.scheduleSaveUser(getOwnerUserId());
    }

    public void attemptToRestoreIfNeededAndSave() {
        int iCanRestoreTo;
        if (!this.mPackageInfo.isShadow()) {
            return;
        }
        ShortcutService shortcutService = this.mShortcutUser.mService;
        if (!shortcutService.isPackageInstalled(this.mPackageName, this.mPackageUserId)) {
            return;
        }
        if (!this.mPackageInfo.hasSignatures()) {
            shortcutService.wtf("Attempted to restore package " + this.mPackageName + "/u" + this.mPackageUserId + " but signatures not found in the restore data.");
            iCanRestoreTo = HdmiCecKeycode.CEC_KEYCODE_RESTORE_VOLUME_FUNCTION;
        } else {
            PackageInfo packageInfoWithSignatures = shortcutService.getPackageInfoWithSignatures(this.mPackageName, this.mPackageUserId);
            packageInfoWithSignatures.getLongVersionCode();
            iCanRestoreTo = this.mPackageInfo.canRestoreTo(shortcutService, packageInfoWithSignatures, canRestoreAnyVersion());
        }
        onRestored(iCanRestoreTo);
        this.mPackageInfo.setShadow(false);
        shortcutService.scheduleSaveUser(this.mPackageUserId);
    }

    public JSONObject dumpCheckin(boolean z) throws JSONException {
        JSONObject jSONObject = new JSONObject();
        jSONObject.put("name", this.mPackageName);
        return jSONObject;
    }

    public void verifyStates() {
    }
}
