package javax.obex;

import android.util.Log;
import java.io.IOException;

public class ObexSession {
    private static final String TAG = "ObexSession";
    private static final boolean V = ObexHelper.VDBG;
    protected Authenticator mAuthenticator;
    protected byte[] mChallengeDigest;

    public boolean handleAuthChall(HeaderSet headerSet) throws IOException {
        String strConvertToUnicode;
        boolean z;
        PasswordAuthentication passwordAuthenticationOnAuthenticationChallenge;
        byte[] password;
        byte[] userName;
        if (this.mAuthenticator == null) {
            return false;
        }
        byte[] tagValue = ObexHelper.getTagValue((byte) 0, headerSet.mAuthChall);
        byte[] tagValue2 = ObexHelper.getTagValue((byte) 1, headerSet.mAuthChall);
        byte[] tagValue3 = ObexHelper.getTagValue((byte) 2, headerSet.mAuthChall);
        if (tagValue3 != null) {
            byte[] bArr = new byte[tagValue3.length - 1];
            System.arraycopy(tagValue3, 1, bArr, 0, bArr.length);
            int i = tagValue3[0] & 255;
            if (i != 255) {
                switch (i) {
                    case ObexHelper.OBEX_AUTH_REALM_CHARSET_ASCII:
                    case 1:
                        try {
                            strConvertToUnicode = new String(bArr, "ISO8859_1");
                        } catch (Exception e) {
                            throw new IOException("Unsupported Encoding Scheme");
                        }
                        break;
                    default:
                        throw new IOException("Unsupported Encoding Scheme");
                }
            } else {
                strConvertToUnicode = ObexHelper.convertToUnicode(bArr, false);
            }
        } else {
            strConvertToUnicode = null;
        }
        try {
            if (tagValue2 != null) {
                z = (tagValue2[0] & 1) != 0;
                boolean z2 = (tagValue2[0] & 2) == 0;
                headerSet.mAuthChall = null;
                passwordAuthenticationOnAuthenticationChallenge = this.mAuthenticator.onAuthenticationChallenge(strConvertToUnicode, z, z2);
                if (passwordAuthenticationOnAuthenticationChallenge != null || (password = passwordAuthenticationOnAuthenticationChallenge.getPassword()) == null) {
                    return false;
                }
                userName = passwordAuthenticationOnAuthenticationChallenge.getUserName();
                if (userName == null) {
                    headerSet.mAuthResp = new byte[userName.length + 38];
                    headerSet.mAuthResp[36] = 1;
                    headerSet.mAuthResp[37] = (byte) userName.length;
                    System.arraycopy(userName, 0, headerSet.mAuthResp, 38, userName.length);
                } else {
                    headerSet.mAuthResp = new byte[36];
                }
                byte[] bArr2 = new byte[tagValue.length + password.length + 1];
                System.arraycopy(tagValue, 0, bArr2, 0, tagValue.length);
                bArr2[tagValue.length] = 58;
                System.arraycopy(password, 0, bArr2, tagValue.length + 1, password.length);
                headerSet.mAuthResp[0] = 0;
                headerSet.mAuthResp[1] = 16;
                System.arraycopy(ObexHelper.computeMd5Hash(bArr2), 0, headerSet.mAuthResp, 2, 16);
                headerSet.mAuthResp[18] = 2;
                headerSet.mAuthResp[19] = 16;
                System.arraycopy(tagValue, 0, headerSet.mAuthResp, 20, 16);
                return true;
            }
            z = false;
            passwordAuthenticationOnAuthenticationChallenge = this.mAuthenticator.onAuthenticationChallenge(strConvertToUnicode, z, z2);
            if (passwordAuthenticationOnAuthenticationChallenge != null) {
                return false;
            }
            userName = passwordAuthenticationOnAuthenticationChallenge.getUserName();
            if (userName == null) {
            }
            byte[] bArr22 = new byte[tagValue.length + password.length + 1];
            System.arraycopy(tagValue, 0, bArr22, 0, tagValue.length);
            bArr22[tagValue.length] = 58;
            System.arraycopy(password, 0, bArr22, tagValue.length + 1, password.length);
            headerSet.mAuthResp[0] = 0;
            headerSet.mAuthResp[1] = 16;
            System.arraycopy(ObexHelper.computeMd5Hash(bArr22), 0, headerSet.mAuthResp, 2, 16);
            headerSet.mAuthResp[18] = 2;
            headerSet.mAuthResp[19] = 16;
            System.arraycopy(tagValue, 0, headerSet.mAuthResp, 20, 16);
            return true;
        } catch (Exception e2) {
            if (V) {
                Log.d(TAG, "Exception occured - returning false", e2);
            }
            return false;
        }
        headerSet.mAuthChall = null;
    }

    public boolean handleAuthResp(byte[] bArr) {
        byte[] bArrOnAuthenticationResponse;
        if (this.mAuthenticator == null || (bArrOnAuthenticationResponse = this.mAuthenticator.onAuthenticationResponse(ObexHelper.getTagValue((byte) 1, bArr))) == null) {
            return false;
        }
        byte[] bArr2 = new byte[bArrOnAuthenticationResponse.length + 16];
        System.arraycopy(this.mChallengeDigest, 0, bArr2, 0, 16);
        System.arraycopy(bArrOnAuthenticationResponse, 0, bArr2, 16, bArrOnAuthenticationResponse.length);
        byte[] bArrComputeMd5Hash = ObexHelper.computeMd5Hash(bArr2);
        byte[] tagValue = ObexHelper.getTagValue((byte) 0, bArr);
        for (int i = 0; i < 16; i++) {
            if (bArrComputeMd5Hash[i] != tagValue[i]) {
                return false;
            }
        }
        return true;
    }
}
