package com.android.se.security.arf.pkcs15;

import android.util.Log;
import com.android.se.Channel;
import com.android.se.security.arf.ASN1;
import com.android.se.security.arf.SecureElement;
import com.android.se.security.arf.SecureElementException;
import java.io.IOException;
import java.security.AccessControlException;
import java.security.cert.CertificateException;
import java.util.MissingResourceException;
import java.util.NoSuchElementException;

public class PKCS15Handler {
    private SecureElement mSEHandle;
    public static final byte[] GPAC_ARF_AID = {ASN1.TAG_PrivateKey, 0, 0, 0, 24, 71, ASN1.TAG_ApplLabel, 65, 67, 45, 49, 53};
    public static final byte[] PKCS15_AID = {ASN1.TAG_PrivateKey, 0, 0, 0, 99, ASN1.TAG_ApplLabel, 75, 67, 83, 45, 49, 53};
    public static final byte[][] CONTAINER_AIDS = {PKCS15_AID, GPAC_ARF_AID, null};
    public final String mTag = "SecureElement-PKCS15Handler";
    private String mSELabel = null;
    private Channel mArfChannel = null;
    private EFACMain mACMainObject = null;
    private EFACRules mACRulesObject = null;
    private byte[] mPkcs15Path = null;
    private byte[] mACMainPath = null;
    private boolean mACMFfound = true;

    public PKCS15Handler(SecureElement secureElement) {
        this.mSEHandle = secureElement;
    }

    private boolean updateACRules() throws Exception {
        if (!this.mACMFfound) {
            this.mSEHandle.resetAccessRules();
            this.mACMainPath = null;
            if (this.mArfChannel != null) {
                this.mSEHandle.closeArfChannel();
            }
            initACEntryPoint();
        }
        try {
            byte[] bArrAnalyseFile = this.mACMainObject.analyseFile();
            this.mACMFfound = true;
            if (bArrAnalyseFile != null) {
                Log.i("SecureElement-PKCS15Handler", "Access Rules needs to be updated...");
                if (this.mACRulesObject == null) {
                    this.mACRulesObject = new EFACRules(this.mSEHandle);
                }
                this.mSEHandle.clearAccessRuleCache();
                this.mACMainPath = null;
                if (this.mArfChannel != null) {
                    this.mSEHandle.closeArfChannel();
                }
                initACEntryPoint();
                try {
                    this.mACRulesObject.analyseFile(bArrAnalyseFile);
                    return true;
                } catch (IOException e) {
                    throw e;
                } catch (Exception e2) {
                    Log.i("SecureElement-PKCS15Handler", "Exception: clear access rule cache and refresh tag");
                    this.mSEHandle.resetAccessRules();
                    throw e2;
                }
            }
            Log.i("SecureElement-PKCS15Handler", "Refresh Tag has not been changed...");
            return false;
        } catch (IOException e3) {
            throw e3;
        } catch (Exception e4) {
            Log.i("SecureElement-PKCS15Handler", "ACMF Not found !");
            this.mACMainObject = null;
            this.mSEHandle.resetAccessRules();
            this.mACMFfound = false;
            throw e4;
        }
    }

    private void initACEntryPoint() throws SecureElementException, MissingResourceException, IOException, CertificateException, NoSuchElementException, PKCS15Exception {
        byte[] bArrAnalyseFile;
        boolean z = false;
        boolean z2 = true;
        int i = 0;
        while (true) {
            if (i < CONTAINER_AIDS.length) {
                try {
                    if (!selectACRulesContainer(CONTAINER_AIDS[i])) {
                        z2 = false;
                    } else {
                        try {
                            if (this.mACMainPath == null) {
                                bArrAnalyseFile = new EFDODF(this.mSEHandle).analyseFile(new EFODF(this.mSEHandle).analyseFile(this.mPkcs15Path));
                                this.mACMainPath = bArrAnalyseFile;
                            } else if (this.mPkcs15Path != null) {
                                bArrAnalyseFile = new byte[this.mPkcs15Path.length + this.mACMainPath.length];
                                System.arraycopy(this.mPkcs15Path, 0, bArrAnalyseFile, 0, this.mPkcs15Path.length);
                                System.arraycopy(this.mACMainPath, 0, bArrAnalyseFile, this.mPkcs15Path.length, this.mACMainPath.length);
                            } else {
                                bArrAnalyseFile = this.mACMainPath;
                            }
                            this.mACMainObject = new EFACMain(this.mSEHandle, bArrAnalyseFile);
                            break;
                        } catch (NoSuchElementException e) {
                            z2 = false;
                        }
                    }
                } catch (NoSuchElementException e2) {
                }
                i++;
            } else {
                z = z2;
                break;
            }
        }
        if (z) {
            throw new NoSuchElementException("No ARF exists");
        }
    }

    private boolean selectACRulesContainer(byte[] bArr) throws SecureElementException, MissingResourceException, IOException, NoSuchElementException, PKCS15Exception {
        if (bArr == null) {
            this.mArfChannel = this.mSEHandle.openLogicalArfChannel(new byte[0]);
            if (this.mArfChannel == null) {
                return false;
            }
            Log.i("SecureElement-PKCS15Handler", "Logical channels are used to access to PKC15");
            if (this.mPkcs15Path == null) {
                this.mACMainPath = null;
                this.mPkcs15Path = new EFDIR(this.mSEHandle).lookupAID(PKCS15_AID);
                if (this.mPkcs15Path == null) {
                    Log.i("SecureElement-PKCS15Handler", "Cannot use ARF: cannot select PKCS#15 directory via EF Dir");
                    throw new NoSuchElementException("Cannot select PKCS#15 directory via EF Dir");
                }
                return true;
            }
            return true;
        }
        this.mArfChannel = this.mSEHandle.openLogicalArfChannel(bArr);
        if (this.mArfChannel == null) {
            Log.w("SecureElement-PKCS15Handler", "GPAC/PKCS#15 ADF not found!!");
            return false;
        }
        if (this.mPkcs15Path != null) {
            this.mACMainPath = null;
        }
        this.mPkcs15Path = null;
        return true;
    }

    public synchronized boolean loadAccessControlRules(String str) throws MissingResourceException, IOException, NoSuchElementException {
        this.mSELabel = str;
        Log.i("SecureElement-PKCS15Handler", "- Loading " + this.mSELabel + " rules...");
        try {
            try {
                try {
                    initACEntryPoint();
                } catch (IOException | MissingResourceException | NoSuchElementException e) {
                    throw e;
                }
            } catch (Exception e2) {
                Log.e("SecureElement-PKCS15Handler", this.mSELabel + " rules not correctly initialized! " + e2.getLocalizedMessage());
                throw new AccessControlException(e2.getLocalizedMessage());
            }
        } finally {
            if (this.mArfChannel != null) {
                this.mSEHandle.closeArfChannel();
            }
        }
        return updateACRules();
    }
}
