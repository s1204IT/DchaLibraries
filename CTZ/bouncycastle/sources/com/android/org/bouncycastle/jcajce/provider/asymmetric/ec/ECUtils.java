package com.android.org.bouncycastle.jcajce.provider.asymmetric.ec;

import com.android.org.bouncycastle.asn1.ASN1Null;
import com.android.org.bouncycastle.asn1.ASN1ObjectIdentifier;
import com.android.org.bouncycastle.asn1.DERNull;
import com.android.org.bouncycastle.asn1.x9.X962Parameters;
import com.android.org.bouncycastle.asn1.x9.X9ECParameters;
import com.android.org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import com.android.org.bouncycastle.jcajce.provider.asymmetric.util.EC5Util;
import com.android.org.bouncycastle.jcajce.provider.asymmetric.util.ECUtil;
import com.android.org.bouncycastle.jce.spec.ECNamedCurveSpec;
import com.android.org.bouncycastle.math.ec.ECCurve;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.PublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;

class ECUtils {
    ECUtils() {
    }

    static AsymmetricKeyParameter generatePublicKeyParameter(PublicKey publicKey) throws InvalidKeyException {
        return publicKey instanceof BCECPublicKey ? ((BCECPublicKey) publicKey).engineGetKeyParameters() : ECUtil.generatePublicKeyParameter(publicKey);
    }

    static X9ECParameters getDomainParametersFromGenSpec(ECGenParameterSpec eCGenParameterSpec) {
        return getDomainParametersFromName(eCGenParameterSpec.getName());
    }

    static X9ECParameters getDomainParametersFromName(String str) {
        X9ECParameters namedCurveByName;
        try {
            if (str.charAt(0) >= '0' && str.charAt(0) <= '2') {
                namedCurveByName = ECUtil.getNamedCurveByOid(new ASN1ObjectIdentifier(str));
                str = str;
            } else if (str.indexOf(32) > 0) {
                String strSubstring = str.substring(str.indexOf(32) + 1);
                try {
                    X9ECParameters namedCurveByName2 = ECUtil.getNamedCurveByName(strSubstring);
                    namedCurveByName = namedCurveByName2;
                    str = namedCurveByName2;
                } catch (IllegalArgumentException e) {
                    str = strSubstring;
                    return ECUtil.getNamedCurveByName(str);
                }
            } else {
                namedCurveByName = ECUtil.getNamedCurveByName(str);
                str = str;
            }
            return namedCurveByName;
        } catch (IllegalArgumentException e2) {
        }
    }

    static X962Parameters getDomainParametersFromName(ECParameterSpec eCParameterSpec, boolean z) {
        if (eCParameterSpec instanceof ECNamedCurveSpec) {
            ECNamedCurveSpec eCNamedCurveSpec = (ECNamedCurveSpec) eCParameterSpec;
            ASN1ObjectIdentifier namedCurveOid = ECUtil.getNamedCurveOid(eCNamedCurveSpec.getName());
            if (namedCurveOid == null) {
                namedCurveOid = new ASN1ObjectIdentifier(eCNamedCurveSpec.getName());
            }
            return new X962Parameters(namedCurveOid);
        }
        if (eCParameterSpec == null) {
            return new X962Parameters((ASN1Null) DERNull.INSTANCE);
        }
        ECCurve eCCurveConvertCurve = EC5Util.convertCurve(eCParameterSpec.getCurve());
        return new X962Parameters(new X9ECParameters(eCCurveConvertCurve, EC5Util.convertPoint(eCCurveConvertCurve, eCParameterSpec.getGenerator(), z), eCParameterSpec.getOrder(), BigInteger.valueOf(eCParameterSpec.getCofactor()), eCParameterSpec.getCurve().getSeed()));
    }
}
