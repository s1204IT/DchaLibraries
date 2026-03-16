package android.test.mock;

import android.app.PackageInstallObserver;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ContainerEncryptionParams;
import android.content.pm.FeatureInfo;
import android.content.pm.IPackageDataObserver;
import android.content.pm.IPackageDeleteObserver;
import android.content.pm.IPackageInstallObserver;
import android.content.pm.IPackageMoveObserver;
import android.content.pm.IPackageStatsObserver;
import android.content.pm.InstrumentationInfo;
import android.content.pm.KeySet;
import android.content.pm.ManifestDigest;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.VerificationParams;
import android.content.pm.VerifierDeviceIdentity;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.UserHandle;
import java.util.List;

public class MockPackageManager extends PackageManager {
    @Override
    public PackageInfo getPackageInfo(String packageName, int flags) throws PackageManager.NameNotFoundException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String[] currentToCanonicalPackageNames(String[] names) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String[] canonicalToCurrentPackageNames(String[] names) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Intent getLaunchIntentForPackage(String packageName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Intent getLeanbackLaunchIntentForPackage(String packageName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int[] getPackageGids(String packageName) throws PackageManager.NameNotFoundException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getPackageUid(String packageName, int userHandle) throws PackageManager.NameNotFoundException {
        throw new UnsupportedOperationException();
    }

    @Override
    public PermissionInfo getPermissionInfo(String name, int flags) throws PackageManager.NameNotFoundException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<PermissionInfo> queryPermissionsByGroup(String group, int flags) throws PackageManager.NameNotFoundException {
        throw new UnsupportedOperationException();
    }

    @Override
    public PermissionGroupInfo getPermissionGroupInfo(String name, int flags) throws PackageManager.NameNotFoundException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<PermissionGroupInfo> getAllPermissionGroups(int flags) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ApplicationInfo getApplicationInfo(String packageName, int flags) throws PackageManager.NameNotFoundException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ActivityInfo getActivityInfo(ComponentName className, int flags) throws PackageManager.NameNotFoundException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ActivityInfo getReceiverInfo(ComponentName className, int flags) throws PackageManager.NameNotFoundException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ServiceInfo getServiceInfo(ComponentName className, int flags) throws PackageManager.NameNotFoundException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProviderInfo getProviderInfo(ComponentName className, int flags) throws PackageManager.NameNotFoundException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<PackageInfo> getInstalledPackages(int flags) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<PackageInfo> getPackagesHoldingPermissions(String[] permissions, int flags) {
        throw new UnsupportedOperationException();
    }

    public List<PackageInfo> getInstalledPackages(int flags, int userId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int checkPermission(String permName, String pkgName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addPermission(PermissionInfo info) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addPermissionAsync(PermissionInfo info) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removePermission(String name) {
        throw new UnsupportedOperationException();
    }

    public void grantPermission(String packageName, String permissionName) {
        throw new UnsupportedOperationException();
    }

    public void revokePermission(String packageName, String permissionName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int checkSignatures(String pkg1, String pkg2) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int checkSignatures(int uid1, int uid2) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String[] getPackagesForUid(int uid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getNameForUid(int uid) {
        throw new UnsupportedOperationException();
    }

    public int getUidForSharedUser(String sharedUserName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ApplicationInfo> getInstalledApplications(int flags) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ResolveInfo resolveActivity(Intent intent, int flags) {
        throw new UnsupportedOperationException();
    }

    public ResolveInfo resolveActivityAsUser(Intent intent, int flags, int userId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ResolveInfo> queryIntentActivities(Intent intent, int flags) {
        throw new UnsupportedOperationException();
    }

    public List<ResolveInfo> queryIntentActivitiesAsUser(Intent intent, int flags, int userId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ResolveInfo> queryIntentActivityOptions(ComponentName caller, Intent[] specifics, Intent intent, int flags) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ResolveInfo> queryBroadcastReceivers(Intent intent, int flags) {
        throw new UnsupportedOperationException();
    }

    public List<ResolveInfo> queryBroadcastReceivers(Intent intent, int flags, int userId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ResolveInfo resolveService(Intent intent, int flags) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ResolveInfo> queryIntentServices(Intent intent, int flags) {
        throw new UnsupportedOperationException();
    }

    public List<ResolveInfo> queryIntentServicesAsUser(Intent intent, int flags, int userId) {
        throw new UnsupportedOperationException();
    }

    public List<ResolveInfo> queryIntentContentProvidersAsUser(Intent intent, int flags, int userId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ResolveInfo> queryIntentContentProviders(Intent intent, int flags) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProviderInfo resolveContentProvider(String name, int flags) {
        throw new UnsupportedOperationException();
    }

    public ProviderInfo resolveContentProviderAsUser(String name, int flags, int userId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ProviderInfo> queryContentProviders(String processName, int uid, int flags) {
        throw new UnsupportedOperationException();
    }

    @Override
    public InstrumentationInfo getInstrumentationInfo(ComponentName className, int flags) throws PackageManager.NameNotFoundException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<InstrumentationInfo> queryInstrumentation(String targetPackage, int flags) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Drawable getDrawable(String packageName, int resid, ApplicationInfo appInfo) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Drawable getActivityIcon(ComponentName activityName) throws PackageManager.NameNotFoundException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Drawable getActivityIcon(Intent intent) throws PackageManager.NameNotFoundException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Drawable getDefaultActivityIcon() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Drawable getActivityBanner(ComponentName activityName) throws PackageManager.NameNotFoundException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Drawable getActivityBanner(Intent intent) throws PackageManager.NameNotFoundException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Drawable getApplicationBanner(ApplicationInfo info) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Drawable getApplicationBanner(String packageName) throws PackageManager.NameNotFoundException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Drawable getApplicationIcon(ApplicationInfo info) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Drawable getApplicationIcon(String packageName) throws PackageManager.NameNotFoundException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Drawable getActivityLogo(ComponentName activityName) throws PackageManager.NameNotFoundException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Drawable getActivityLogo(Intent intent) throws PackageManager.NameNotFoundException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Drawable getApplicationLogo(ApplicationInfo info) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Drawable getApplicationLogo(String packageName) throws PackageManager.NameNotFoundException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Drawable getUserBadgedIcon(Drawable icon, UserHandle user) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Drawable getUserBadgedDrawableForDensity(Drawable drawable, UserHandle user, Rect badgeLocation, int badgeDensity) {
        throw new UnsupportedOperationException();
    }

    public Drawable getUserBadgeForDensity(UserHandle user, int density) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CharSequence getUserBadgedLabel(CharSequence label, UserHandle user) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CharSequence getText(String packageName, int resid, ApplicationInfo appInfo) {
        throw new UnsupportedOperationException();
    }

    @Override
    public XmlResourceParser getXml(String packageName, int resid, ApplicationInfo appInfo) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CharSequence getApplicationLabel(ApplicationInfo info) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Resources getResourcesForActivity(ComponentName activityName) throws PackageManager.NameNotFoundException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Resources getResourcesForApplication(ApplicationInfo app) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Resources getResourcesForApplication(String appPackageName) throws PackageManager.NameNotFoundException {
        throw new UnsupportedOperationException();
    }

    public Resources getResourcesForApplicationAsUser(String appPackageName, int userId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public PackageInfo getPackageArchiveInfo(String archiveFilePath, int flags) {
        throw new UnsupportedOperationException();
    }

    public void installPackage(Uri packageURI, IPackageInstallObserver observer, int flags, String installerPackageName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setInstallerPackageName(String targetPackage, String installerPackageName) {
        throw new UnsupportedOperationException();
    }

    public void movePackage(String packageName, IPackageMoveObserver observer, int flags) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getInstallerPackageName(String packageName) {
        throw new UnsupportedOperationException();
    }

    public void clearApplicationUserData(String packageName, IPackageDataObserver observer) {
        throw new UnsupportedOperationException();
    }

    public void deleteApplicationCacheFiles(String packageName, IPackageDataObserver observer) {
        throw new UnsupportedOperationException();
    }

    public void freeStorageAndNotify(long idealStorageSize, IPackageDataObserver observer) {
        throw new UnsupportedOperationException();
    }

    public void freeStorage(long idealStorageSize, IntentSender pi) {
        throw new UnsupportedOperationException();
    }

    public void deletePackage(String packageName, IPackageDeleteObserver observer, int flags) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addPackageToPreferred(String packageName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removePackageFromPreferred(String packageName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<PackageInfo> getPreferredPackages(int flags) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setComponentEnabledSetting(ComponentName componentName, int newState, int flags) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getComponentEnabledSetting(ComponentName componentName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setApplicationEnabledSetting(String packageName, int newState, int flags) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getApplicationEnabledSetting(String packageName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addPreferredActivity(IntentFilter filter, int match, ComponentName[] set, ComponentName activity) {
        throw new UnsupportedOperationException();
    }

    public void replacePreferredActivity(IntentFilter filter, int match, ComponentName[] set, ComponentName activity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clearPackagePreferredActivities(String packageName) {
        throw new UnsupportedOperationException();
    }

    public void getPackageSizeInfo(String packageName, int userHandle, IPackageStatsObserver observer) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getPreferredActivities(List<IntentFilter> outFilters, List<ComponentName> outActivities, String packageName) {
        throw new UnsupportedOperationException();
    }

    public ComponentName getHomeActivities(List<ResolveInfo> outActivities) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String[] getSystemSharedLibraryNames() {
        throw new UnsupportedOperationException();
    }

    @Override
    public FeatureInfo[] getSystemAvailableFeatures() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasSystemFeature(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSafeMode() {
        throw new UnsupportedOperationException();
    }

    public KeySet getKeySetByAlias(String packageName, String alias) {
        throw new UnsupportedOperationException();
    }

    public KeySet getSigningKeySet(String packageName) {
        throw new UnsupportedOperationException();
    }

    public boolean isSignedBy(String packageName, KeySet ks) {
        throw new UnsupportedOperationException();
    }

    public boolean isSignedByExactly(String packageName, KeySet ks) {
        throw new UnsupportedOperationException();
    }

    public void installPackageWithVerification(Uri packageURI, IPackageInstallObserver observer, int flags, String installerPackageName, Uri verificationURI, ManifestDigest manifestDigest, ContainerEncryptionParams encryptionParams) {
        throw new UnsupportedOperationException();
    }

    public void installPackageWithVerificationAndEncryption(Uri packageURI, IPackageInstallObserver observer, int flags, String installerPackageName, VerificationParams verificationParams, ContainerEncryptionParams encryptionParams) {
        throw new UnsupportedOperationException();
    }

    public boolean setApplicationHiddenSettingAsUser(String packageName, boolean hidden, UserHandle user) {
        return false;
    }

    public boolean getApplicationHiddenSettingAsUser(String packageName, UserHandle user) {
        return false;
    }

    public int installExistingPackage(String packageName) throws PackageManager.NameNotFoundException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void verifyPendingInstall(int id, int verificationCode) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void extendVerificationTimeout(int id, int verificationCodeAtTimeout, long millisecondsToDelay) {
        throw new UnsupportedOperationException();
    }

    public VerifierDeviceIdentity getVerifierDeviceIdentity() {
        throw new UnsupportedOperationException();
    }

    public boolean isUpgrade() {
        throw new UnsupportedOperationException();
    }

    public void installPackage(Uri packageURI, PackageInstallObserver observer, int flags, String installerPackageName) {
        throw new UnsupportedOperationException();
    }

    public void installPackageWithVerification(Uri packageURI, PackageInstallObserver observer, int flags, String installerPackageName, Uri verificationURI, ManifestDigest manifestDigest, ContainerEncryptionParams encryptionParams) {
        throw new UnsupportedOperationException();
    }

    public void installPackageWithVerificationAndEncryption(Uri packageURI, PackageInstallObserver observer, int flags, String installerPackageName, VerificationParams verificationParams, ContainerEncryptionParams encryptionParams) {
        throw new UnsupportedOperationException();
    }

    public void addCrossProfileIntentFilter(IntentFilter filter, int sourceUserId, int targetUserId, int flags) {
        throw new UnsupportedOperationException();
    }

    public void clearCrossProfileIntentFilters(int sourceUserId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public PackageInstaller getPackageInstaller() {
        throw new UnsupportedOperationException();
    }

    public boolean isPackageAvailable(String packageName) {
        throw new UnsupportedOperationException();
    }

    public Drawable loadItemIcon(PackageItemInfo itemInfo, ApplicationInfo appInfo) {
        throw new UnsupportedOperationException();
    }

    public Drawable loadUnbadgedItemIcon(PackageItemInfo itemInfo, ApplicationInfo appInfo) {
        throw new UnsupportedOperationException();
    }
}
