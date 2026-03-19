package com.android.org.bouncycastle.jcajce.provider.asymmetric.ec;

import com.android.org.bouncycastle.asn1.ASN1ObjectIdentifier;
import com.android.org.bouncycastle.asn1.x9.X9ECParameters;
import com.android.org.bouncycastle.jcajce.provider.asymmetric.util.ECUtil;
import java.security.spec.ECGenParameterSpec;

class ECUtils {
    ECUtils() {
    }

    static X9ECParameters getDomainParametersFromGenSpec(ECGenParameterSpec genSpec) {
        return getDomainParametersFromName(genSpec.getName());
    }

    static X9ECParameters getDomainParametersFromName(String curveName) {
        X9ECParameters domainParameters;
        try {
            if (curveName.charAt(0) >= '0' && curveName.charAt(0) <= '2') {
                ASN1ObjectIdentifier oidID = new ASN1ObjectIdentifier(curveName);
                domainParameters = ECUtil.getNamedCurveByOid(oidID);
            } else if (curveName.indexOf(32) > 0) {
                curveName = curveName.substring(curveName.indexOf(32) + 1);
                domainParameters = ECUtil.getNamedCurveByName(curveName);
            } else {
                domainParameters = ECUtil.getNamedCurveByName(curveName);
            }
            return domainParameters;
        } catch (IllegalArgumentException e) {
            X9ECParameters domainParameters2 = ECUtil.getNamedCurveByName(curveName);
            return domainParameters2;
        }
    }
}
