package com.android.server.accounts;

import android.accounts.Account;
import android.accounts.AccountManagerInternal;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import android.util.PackageUtils;
import android.util.Pair;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.content.PackageMonitor;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.XmlUtils;
import com.android.server.accounts.AccountManagerService;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public final class AccountManagerBackupHelper {
    private static final String ATTR_ACCOUNT_SHA_256 = "account-sha-256";
    private static final String ATTR_DIGEST = "digest";
    private static final String ATTR_PACKAGE = "package";
    private static final long PENDING_RESTORE_TIMEOUT_MILLIS = 3600000;
    private static final String TAG = "AccountManagerBackupHelper";
    private static final String TAG_PERMISSION = "permission";
    private static final String TAG_PERMISSIONS = "permissions";
    private final AccountManagerInternal mAccountManagerInternal;
    private final AccountManagerService mAccountManagerService;
    private final Object mLock = new Object();

    @GuardedBy("mLock")
    private Runnable mRestoreCancelCommand;

    @GuardedBy("mLock")
    private RestorePackageMonitor mRestorePackageMonitor;

    @GuardedBy("mLock")
    private List<PendingAppPermission> mRestorePendingAppPermissions;

    public AccountManagerBackupHelper(AccountManagerService accountManagerService, AccountManagerInternal accountManagerInternal) {
        this.mAccountManagerService = accountManagerService;
        this.mAccountManagerInternal = accountManagerInternal;
    }

    private final class PendingAppPermission {
        private final String accountDigest;
        private final String certDigest;
        private final String packageName;
        private final int userId;

        public PendingAppPermission(String str, String str2, String str3, int i) {
            this.accountDigest = str;
            this.packageName = str2;
            this.certDigest = str3;
            this.userId = i;
        }

        public boolean apply(PackageManager packageManager) {
            Account account;
            AccountManagerService.UserAccounts userAccounts = AccountManagerBackupHelper.this.mAccountManagerService.getUserAccounts(this.userId);
            synchronized (userAccounts.dbLock) {
                synchronized (userAccounts.cacheLock) {
                    account = null;
                    for (Account[] accountArr : userAccounts.accountCache.values()) {
                        int length = accountArr.length;
                        int i = 0;
                        while (true) {
                            if (i >= length) {
                                break;
                            }
                            Account account2 = accountArr[i];
                            if (!this.accountDigest.equals(PackageUtils.computeSha256Digest(account2.name.getBytes()))) {
                                i++;
                            } else {
                                account = account2;
                                break;
                            }
                        }
                        if (account != null) {
                            break;
                        }
                    }
                }
            }
            if (account == null) {
                return false;
            }
            try {
                PackageInfo packageInfoAsUser = packageManager.getPackageInfoAsUser(this.packageName, 64, this.userId);
                String[] strArrComputeSignaturesSha256Digests = PackageUtils.computeSignaturesSha256Digests(packageInfoAsUser.signatures);
                if (!this.certDigest.equals(PackageUtils.computeSignaturesSha256Digest(strArrComputeSignaturesSha256Digests)) && (packageInfoAsUser.signatures.length <= 1 || !this.certDigest.equals(strArrComputeSignaturesSha256Digests[0]))) {
                    return false;
                }
                int i2 = packageInfoAsUser.applicationInfo.uid;
                if (!AccountManagerBackupHelper.this.mAccountManagerInternal.hasAccountAccess(account, i2)) {
                    AccountManagerBackupHelper.this.mAccountManagerService.grantAppPermission(account, "com.android.AccountManager.ACCOUNT_ACCESS_TOKEN_TYPE", i2);
                }
                return true;
            } catch (PackageManager.NameNotFoundException e) {
                return false;
            }
        }
    }

    public byte[] backupAccountAccessPermissions(int i) {
        AccountManagerService.UserAccounts userAccounts = this.mAccountManagerService.getUserAccounts(i);
        synchronized (userAccounts.dbLock) {
            synchronized (userAccounts.cacheLock) {
                List<Pair<String, Integer>> listFindAllAccountGrants = userAccounts.accountsDb.findAllAccountGrants();
                if (listFindAllAccountGrants.isEmpty()) {
                    return null;
                }
                try {
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
                    fastXmlSerializer.setOutput(byteArrayOutputStream, StandardCharsets.UTF_8.name());
                    fastXmlSerializer.startDocument(null, true);
                    fastXmlSerializer.startTag(null, TAG_PERMISSIONS);
                    PackageManager packageManager = this.mAccountManagerService.mContext.getPackageManager();
                    for (Pair<String, Integer> pair : listFindAllAccountGrants) {
                        String str = (String) pair.first;
                        String[] packagesForUid = packageManager.getPackagesForUid(((Integer) pair.second).intValue());
                        if (packagesForUid != null) {
                            for (String str2 : packagesForUid) {
                                try {
                                    String strComputeSignaturesSha256Digest = PackageUtils.computeSignaturesSha256Digest(packageManager.getPackageInfoAsUser(str2, 64, i).signatures);
                                    if (strComputeSignaturesSha256Digest != null) {
                                        fastXmlSerializer.startTag(null, TAG_PERMISSION);
                                        fastXmlSerializer.attribute(null, ATTR_ACCOUNT_SHA_256, PackageUtils.computeSha256Digest(str.getBytes()));
                                        fastXmlSerializer.attribute(null, "package", str2);
                                        fastXmlSerializer.attribute(null, ATTR_DIGEST, strComputeSignaturesSha256Digest);
                                        fastXmlSerializer.endTag(null, TAG_PERMISSION);
                                    }
                                } catch (PackageManager.NameNotFoundException e) {
                                    Slog.i(TAG, "Skipping backup of account access grant for non-existing package: " + str2);
                                }
                            }
                        }
                    }
                    fastXmlSerializer.endTag(null, TAG_PERMISSIONS);
                    fastXmlSerializer.endDocument();
                    fastXmlSerializer.flush();
                    return byteArrayOutputStream.toByteArray();
                } catch (IOException e2) {
                    Log.e(TAG, "Error backing up account access grants", e2);
                    return null;
                }
            }
        }
    }

    public void restoreAccountAccessPermissions(byte[] bArr, int i) {
        try {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bArr);
            XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
            xmlPullParserNewPullParser.setInput(byteArrayInputStream, StandardCharsets.UTF_8.name());
            PackageManager packageManager = this.mAccountManagerService.mContext.getPackageManager();
            int depth = xmlPullParserNewPullParser.getDepth();
            while (true) {
                if (XmlUtils.nextElementWithin(xmlPullParserNewPullParser, depth)) {
                    if (TAG_PERMISSIONS.equals(xmlPullParserNewPullParser.getName())) {
                        int depth2 = xmlPullParserNewPullParser.getDepth();
                        while (XmlUtils.nextElementWithin(xmlPullParserNewPullParser, depth2)) {
                            if (TAG_PERMISSION.equals(xmlPullParserNewPullParser.getName())) {
                                String attributeValue = xmlPullParserNewPullParser.getAttributeValue(null, ATTR_ACCOUNT_SHA_256);
                                if (TextUtils.isEmpty(attributeValue)) {
                                    XmlUtils.skipCurrentTag(xmlPullParserNewPullParser);
                                }
                                String attributeValue2 = xmlPullParserNewPullParser.getAttributeValue(null, "package");
                                if (TextUtils.isEmpty(attributeValue2)) {
                                    XmlUtils.skipCurrentTag(xmlPullParserNewPullParser);
                                }
                                String attributeValue3 = xmlPullParserNewPullParser.getAttributeValue(null, ATTR_DIGEST);
                                if (TextUtils.isEmpty(attributeValue3)) {
                                    XmlUtils.skipCurrentTag(xmlPullParserNewPullParser);
                                }
                                PendingAppPermission pendingAppPermission = new PendingAppPermission(attributeValue, attributeValue2, attributeValue3, i);
                                if (!pendingAppPermission.apply(packageManager)) {
                                    synchronized (this.mLock) {
                                        if (this.mRestorePackageMonitor == null) {
                                            this.mRestorePackageMonitor = new RestorePackageMonitor();
                                            this.mRestorePackageMonitor.register(this.mAccountManagerService.mContext, this.mAccountManagerService.mHandler.getLooper(), true);
                                        }
                                        if (this.mRestorePendingAppPermissions == null) {
                                            this.mRestorePendingAppPermissions = new ArrayList();
                                        }
                                        this.mRestorePendingAppPermissions.add(pendingAppPermission);
                                    }
                                }
                            }
                        }
                    }
                } else {
                    this.mRestoreCancelCommand = new CancelRestoreCommand();
                    this.mAccountManagerService.mHandler.postDelayed(this.mRestoreCancelCommand, 3600000L);
                    return;
                }
            }
        } catch (IOException | XmlPullParserException e) {
            Log.e(TAG, "Error restoring app permissions", e);
        }
    }

    private final class RestorePackageMonitor extends PackageMonitor {
        private RestorePackageMonitor() {
        }

        public void onPackageAdded(String str, int i) {
            synchronized (AccountManagerBackupHelper.this.mLock) {
                if (AccountManagerBackupHelper.this.mRestorePendingAppPermissions == null) {
                    return;
                }
                if (UserHandle.getUserId(i) != 0) {
                    return;
                }
                for (int size = AccountManagerBackupHelper.this.mRestorePendingAppPermissions.size() - 1; size >= 0; size--) {
                    PendingAppPermission pendingAppPermission = (PendingAppPermission) AccountManagerBackupHelper.this.mRestorePendingAppPermissions.get(size);
                    if (pendingAppPermission.packageName.equals(str) && pendingAppPermission.apply(AccountManagerBackupHelper.this.mAccountManagerService.mContext.getPackageManager())) {
                        AccountManagerBackupHelper.this.mRestorePendingAppPermissions.remove(size);
                    }
                }
                if (AccountManagerBackupHelper.this.mRestorePendingAppPermissions.isEmpty() && AccountManagerBackupHelper.this.mRestoreCancelCommand != null) {
                    AccountManagerBackupHelper.this.mAccountManagerService.mHandler.removeCallbacks(AccountManagerBackupHelper.this.mRestoreCancelCommand);
                    AccountManagerBackupHelper.this.mRestoreCancelCommand.run();
                    AccountManagerBackupHelper.this.mRestoreCancelCommand = null;
                }
            }
        }
    }

    private final class CancelRestoreCommand implements Runnable {
        private CancelRestoreCommand() {
        }

        @Override
        public void run() {
            synchronized (AccountManagerBackupHelper.this.mLock) {
                AccountManagerBackupHelper.this.mRestorePendingAppPermissions = null;
                if (AccountManagerBackupHelper.this.mRestorePackageMonitor != null) {
                    AccountManagerBackupHelper.this.mRestorePackageMonitor.unregister();
                    AccountManagerBackupHelper.this.mRestorePackageMonitor = null;
                }
            }
        }
    }
}
