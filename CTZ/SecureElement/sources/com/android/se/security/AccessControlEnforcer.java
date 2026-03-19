package com.android.se.security;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.os.SystemProperties;
import android.util.Log;
import com.android.se.Channel;
import com.android.se.SecureElementService;
import com.android.se.Terminal;
import com.android.se.security.ChannelAccess;
import com.android.se.security.ara.AraController;
import com.android.se.security.arf.ArfController;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.AccessControlException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.MissingResourceException;
import java.util.NoSuchElementException;

public class AccessControlEnforcer {
    private AccessRuleCache mAccessRuleCache;
    private Terminal mTerminal;
    private final String mTag = "SecureElement-AccessControlEnforcer";
    private PackageManager mPackageManager = null;
    private AraController mAraController = null;
    private boolean mUseAra = true;
    private ArfController mArfController = null;
    private boolean mUseArf = false;
    private boolean mRulesRead = false;
    private ChannelAccess mInitialChannelAccess = new ChannelAccess();
    private boolean mFullAccess = false;

    public AccessControlEnforcer(Terminal terminal) {
        this.mAccessRuleCache = null;
        this.mTerminal = null;
        this.mTerminal = terminal;
        this.mAccessRuleCache = new AccessRuleCache();
    }

    public static byte[] getDefaultAccessControlAid() {
        return AraController.getAraMAid();
    }

    private static Certificate decodeCertificate(byte[] bArr) throws CertificateException {
        return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(bArr));
    }

    public static byte[] getAppCertHash(Certificate certificate) throws CertificateEncodingException {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA");
            if (messageDigest == null) {
                throw new AccessControlException("Hash can not be computed");
            }
            return messageDigest.digest(certificate.getEncoded());
        } catch (NoSuchAlgorithmException e) {
            throw new AccessControlException("Exception getting SHA for the signature");
        }
    }

    public PackageManager getPackageManager() {
        return this.mPackageManager;
    }

    public void setPackageManager(PackageManager packageManager) {
        this.mPackageManager = packageManager;
    }

    public Terminal getTerminal() {
        return this.mTerminal;
    }

    public AccessRuleCache getAccessRuleCache() {
        return this.mAccessRuleCache;
    }

    public synchronized void reset() {
        Log.i("SecureElement-AccessControlEnforcer", "Reset the ACE for terminal:" + this.mTerminal.getName());
        this.mAccessRuleCache.reset();
        this.mAraController = null;
        this.mArfController = null;
    }

    public synchronized void initialize() throws MissingResourceException, IOException {
        String localizedMessage;
        this.mInitialChannelAccess.setApduAccess(ChannelAccess.ACCESS.ALLOWED);
        this.mInitialChannelAccess.setNFCEventAccess(ChannelAccess.ACCESS.ALLOWED);
        this.mInitialChannelAccess.setAccess(ChannelAccess.ACCESS.ALLOWED, "");
        readSecurityProfile();
        boolean z = true;
        if (!this.mTerminal.getName().startsWith(SecureElementService.UICC_TERMINAL)) {
            this.mFullAccess = true;
        }
        if (this.mUseAra && this.mAraController == null) {
            this.mAraController = new AraController(this.mAccessRuleCache, this.mTerminal);
        }
        if (!this.mUseAra || this.mAraController == null) {
            localizedMessage = "";
            if (this.mUseArf && !this.mTerminal.getName().startsWith(SecureElementService.UICC_TERMINAL)) {
                Log.i("SecureElement-AccessControlEnforcer", "Disable ARF for terminal: " + this.mTerminal.getName() + " (ARF is only available for UICC)");
                this.mUseArf = false;
            }
            if (this.mUseArf && this.mArfController == null) {
                this.mArfController = new ArfController(this.mAccessRuleCache, this.mTerminal);
            }
            if (this.mUseArf && this.mArfController != null) {
                try {
                    this.mArfController.initialize();
                    Log.i("SecureElement-AccessControlEnforcer", "ARF rules are used for:" + this.mTerminal.getName());
                    this.mFullAccess = false;
                } catch (IOException | MissingResourceException e) {
                    throw e;
                } catch (Exception e2) {
                    this.mUseArf = false;
                    localizedMessage = e2.getLocalizedMessage();
                    Log.e("SecureElement-AccessControlEnforcer", e2.getMessage());
                    if (this.mFullAccess) {
                        if (!(e2 instanceof NoSuchElementException)) {
                            this.mFullAccess = false;
                            z = false;
                        }
                    } else {
                        z = false;
                    }
                }
            }
            if (!this.mUseArf && !this.mUseAra && !this.mFullAccess) {
                this.mInitialChannelAccess.setApduAccess(ChannelAccess.ACCESS.DENIED);
                this.mInitialChannelAccess.setNFCEventAccess(ChannelAccess.ACCESS.DENIED);
                this.mInitialChannelAccess.setAccess(ChannelAccess.ACCESS.DENIED, localizedMessage);
                Log.i("SecureElement-AccessControlEnforcer", "Deny any access to:" + this.mTerminal.getName());
            }
            this.mRulesRead = z;
        } else {
            try {
                this.mAraController.initialize();
                Log.i("SecureElement-AccessControlEnforcer", "ARA applet is used for:" + this.mTerminal.getName());
                this.mUseArf = false;
                this.mFullAccess = false;
                localizedMessage = "";
            } catch (IOException | MissingResourceException e3) {
                throw e3;
            } catch (Exception e4) {
                this.mUseAra = false;
                localizedMessage = e4.getLocalizedMessage();
                if (e4 instanceof NoSuchElementException) {
                    Log.i("SecureElement-AccessControlEnforcer", "No ARA applet found in: " + this.mTerminal.getName());
                } else {
                    if (this.mTerminal.getName().startsWith(SecureElementService.UICC_TERMINAL)) {
                        if (!this.mUseArf) {
                            this.mFullAccess = false;
                        }
                    } else {
                        this.mUseArf = false;
                        this.mFullAccess = false;
                        Log.i("SecureElement-AccessControlEnforcer", "Problem accessing ARA, Access DENIED " + e4.getLocalizedMessage());
                    }
                    z = false;
                }
            }
            if (this.mUseArf) {
                Log.i("SecureElement-AccessControlEnforcer", "Disable ARF for terminal: " + this.mTerminal.getName() + " (ARF is only available for UICC)");
                this.mUseArf = false;
            }
            if (this.mUseArf) {
                this.mArfController = new ArfController(this.mAccessRuleCache, this.mTerminal);
            }
            if (this.mUseArf) {
                this.mArfController.initialize();
                Log.i("SecureElement-AccessControlEnforcer", "ARF rules are used for:" + this.mTerminal.getName());
                this.mFullAccess = false;
            }
            if (!this.mUseArf) {
                this.mInitialChannelAccess.setApduAccess(ChannelAccess.ACCESS.DENIED);
                this.mInitialChannelAccess.setNFCEventAccess(ChannelAccess.ACCESS.DENIED);
                this.mInitialChannelAccess.setAccess(ChannelAccess.ACCESS.DENIED, localizedMessage);
                Log.i("SecureElement-AccessControlEnforcer", "Deny any access to:" + this.mTerminal.getName());
            }
            this.mRulesRead = z;
        }
    }

    public synchronized void checkCommand(Channel channel, byte[] bArr) {
        ChannelAccess channelAccess = channel.getChannelAccess();
        if (channelAccess == null) {
            throw new AccessControlException("SecureElement-AccessControlEnforcerChannel access not set");
        }
        String reason = channelAccess.getReason();
        if (reason.length() == 0) {
            reason = "Command not allowed!";
        }
        if (channelAccess.getAccess() != ChannelAccess.ACCESS.ALLOWED) {
            throw new AccessControlException("SecureElement-AccessControlEnforcer" + reason);
        }
        if (channelAccess.isUseApduFilter()) {
            ApduFilter[] apduFilter = channelAccess.getApduFilter();
            if (apduFilter == null || apduFilter.length == 0) {
                throw new AccessControlException("SecureElement-AccessControlEnforcerAccess Rule not available:" + reason);
            }
            for (ApduFilter apduFilter2 : apduFilter) {
                if (CommandApdu.compareHeaders(bArr, apduFilter2.getMask(), apduFilter2.getApdu())) {
                    return;
                }
            }
            throw new AccessControlException("SecureElement-AccessControlEnforcerAccess Rule does not match: " + reason);
        }
        if (channelAccess.getApduAccess() != ChannelAccess.ACCESS.ALLOWED) {
            throw new AccessControlException("SecureElement-AccessControlEnforcerAPDU access NOT allowed");
        }
    }

    public ChannelAccess setUpChannelAccess(byte[] bArr, String str, boolean z) throws MissingResourceException, IOException {
        ChannelAccess channelAccessInternal_setUpChannelAccess;
        if (this.mInitialChannelAccess.getAccess() == ChannelAccess.ACCESS.DENIED) {
            throw new AccessControlException("SecureElement-AccessControlEnforceraccess denied: " + this.mInitialChannelAccess.getReason());
        }
        if (this.mUseAra || this.mUseArf) {
            channelAccessInternal_setUpChannelAccess = internal_setUpChannelAccess(bArr, str, z);
        } else {
            channelAccessInternal_setUpChannelAccess = null;
        }
        if (channelAccessInternal_setUpChannelAccess == null || (channelAccessInternal_setUpChannelAccess.getApduAccess() != ChannelAccess.ACCESS.ALLOWED && !channelAccessInternal_setUpChannelAccess.isUseApduFilter())) {
            if (this.mFullAccess) {
                channelAccessInternal_setUpChannelAccess = this.mInitialChannelAccess;
            } else {
                throw new AccessControlException("SecureElement-AccessControlEnforcerno APDU access allowed!");
            }
        }
        channelAccessInternal_setUpChannelAccess.setPackageName(str);
        return channelAccessInternal_setUpChannelAccess.m1clone();
    }

    private synchronized ChannelAccess internal_setUpChannelAccess(byte[] bArr, String str, boolean z) throws MissingResourceException, IOException {
        Certificate[] aPPCerts;
        if (str != null) {
            try {
                if (!str.isEmpty()) {
                    try {
                        try {
                            aPPCerts = getAPPCerts(str);
                            if (aPPCerts == null || aPPCerts.length == 0) {
                                throw new AccessControlException("Application certificates are invalid or do not exist.");
                            }
                            if (z) {
                                updateAccessRuleIfNeed();
                            }
                        } catch (IOException | MissingResourceException e) {
                            throw e;
                        }
                    } catch (Throwable th) {
                        throw new AccessControlException(th.getMessage());
                    }
                }
            } catch (Throwable th2) {
                throw th2;
            }
        }
        throw new AccessControlException("package names must be specified");
        return getAccessRule(bArr, aPPCerts);
    }

    public ChannelAccess getAccessRule(byte[] bArr, Certificate[] certificateArr) throws AccessControlException, CertificateEncodingException {
        ChannelAccess channelAccessFindAccessRule;
        if (this.mRulesRead) {
            channelAccessFindAccessRule = this.mAccessRuleCache.findAccessRule(bArr, certificateArr);
        } else {
            channelAccessFindAccessRule = null;
        }
        if (channelAccessFindAccessRule == null) {
            ChannelAccess channelAccess = new ChannelAccess();
            channelAccess.setAccess(ChannelAccess.ACCESS.DENIED, "no access rule found!");
            channelAccess.setApduAccess(ChannelAccess.ACCESS.DENIED);
            channelAccess.setNFCEventAccess(ChannelAccess.ACCESS.DENIED);
            return channelAccess;
        }
        return channelAccessFindAccessRule;
    }

    private Certificate[] getAPPCerts(String str) throws NoSuchAlgorithmException, AccessControlException, CertificateException {
        if (str == null || str.length() == 0) {
            throw new AccessControlException("Package Name not defined");
        }
        try {
            PackageInfo packageInfo = this.mPackageManager.getPackageInfo(str, 64);
            if (packageInfo == null) {
                throw new AccessControlException("Package does not exist");
            }
            ArrayList arrayList = new ArrayList();
            for (Signature signature : packageInfo.signatures) {
                arrayList.add(decodeCertificate(signature.toByteArray()));
            }
            return (Certificate[]) arrayList.toArray(new Certificate[arrayList.size()]);
        } catch (PackageManager.NameNotFoundException e) {
            throw new AccessControlException("Package does not exist");
        }
    }

    public synchronized boolean[] isNfcEventAllowed(byte[] bArr, String[] strArr, boolean z) {
        if (!this.mUseAra && !this.mUseArf) {
            boolean[] zArr = new boolean[strArr.length];
            for (int i = 0; i < zArr.length; i++) {
                zArr[i] = this.mFullAccess;
            }
            return zArr;
        }
        return internal_isNfcEventAllowed(bArr, strArr, z);
    }

    private synchronized boolean[] internal_isNfcEventAllowed(byte[] bArr, String[] strArr, boolean z) {
        boolean[] zArr;
        if (z) {
            try {
                updateAccessRuleIfNeed();
                zArr = new boolean[strArr.length];
                int i = 0;
                for (String str : strArr) {
                    try {
                        Certificate[] aPPCerts = getAPPCerts(str);
                        if (aPPCerts == null || aPPCerts.length == 0) {
                            zArr[i] = false;
                        } else {
                            zArr[i] = getAccessRule(bArr, aPPCerts).getNFCEventAccess() == ChannelAccess.ACCESS.ALLOWED;
                        }
                    } catch (Exception e) {
                        Log.w("SecureElement-AccessControlEnforcer", " Access Rules for NFC: " + e.getLocalizedMessage());
                        zArr[i] = false;
                    }
                    i++;
                }
            } catch (IOException | MissingResourceException e2) {
                throw new AccessControlException("Access-Control not found in " + this.mTerminal.getName());
            }
        } else {
            zArr = new boolean[strArr.length];
            int i2 = 0;
            while (i < r0) {
            }
        }
        return zArr;
    }

    private void updateAccessRuleIfNeed() throws IOException {
        if (this.mUseAra && this.mAraController != null) {
            try {
                this.mAraController.initialize();
                this.mUseArf = false;
                this.mFullAccess = false;
                return;
            } catch (IOException | MissingResourceException e) {
                throw e;
            } catch (Exception e2) {
                throw new AccessControlException("No ARA applet found in " + this.mTerminal.getName());
            }
        }
        if (this.mUseArf && this.mArfController != null) {
            try {
                this.mArfController.initialize();
            } catch (IOException | MissingResourceException e3) {
                throw e3;
            } catch (Exception e4) {
                Log.e("SecureElement-AccessControlEnforcer", e4.getMessage());
            }
        }
    }

    public void dump(PrintWriter printWriter) {
        printWriter.println("SecureElement-AccessControlEnforcer:");
        printWriter.println("mUseArf: " + this.mUseArf);
        printWriter.println("mUseAra: " + this.mUseAra);
        printWriter.println("mInitialChannelAccess:");
        printWriter.println(this.mInitialChannelAccess.toString());
        printWriter.println();
        if (this.mAccessRuleCache != null) {
            this.mAccessRuleCache.dump(printWriter);
        }
    }

    private void readSecurityProfile() {
        if (!Build.IS_DEBUGGABLE) {
            this.mUseArf = true;
            this.mUseAra = true;
            this.mFullAccess = false;
        } else {
            String str = SystemProperties.get("persist.service.seek", SystemProperties.get("service.seek", "useara usearf"));
            if (str.contains("usearf")) {
                this.mUseArf = true;
            } else {
                this.mUseArf = false;
            }
            if (str.contains("useara")) {
                this.mUseAra = true;
            } else {
                this.mUseAra = false;
            }
            if (str.contains("fullaccess")) {
                this.mFullAccess = true;
            } else {
                this.mFullAccess = false;
            }
        }
        Log.i("SecureElement-AccessControlEnforcer", "Allowed ACE mode: ara=" + this.mUseAra + " arf=" + this.mUseArf + " fullaccess=" + this.mFullAccess);
    }
}
