package android.security.keystore;

import android.annotation.SystemApi;
import android.content.Context;
import android.os.Build;
import android.security.KeyStore;
import android.security.keymaster.KeymasterArguments;
import android.security.keymaster.KeymasterCertificateChain;
import android.security.keymaster.KeymasterDefs;
import android.telephony.TelephonyManager;
import android.util.ArraySet;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.List;

@SystemApi
public abstract class AttestationUtils {
    public static final int ID_TYPE_IMEI = 2;
    public static final int ID_TYPE_MEID = 3;
    public static final int ID_TYPE_SERIAL = 1;

    private AttestationUtils() {
    }

    public static X509Certificate[] parseCertificateChain(KeymasterCertificateChain keymasterCertificateChain) throws KeyAttestationException {
        List<byte[]> certificates = keymasterCertificateChain.getCertificates();
        if (certificates.size() < 2) {
            throw new KeyAttestationException("Attestation certificate chain contained " + certificates.size() + " entries. At least two are required.");
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            Iterator<byte[]> it = certificates.iterator();
            while (it.hasNext()) {
                byteArrayOutputStream.write(it.next());
            }
            return (X509Certificate[]) CertificateFactory.getInstance("X.509").generateCertificates(new ByteArrayInputStream(byteArrayOutputStream.toByteArray())).toArray(new X509Certificate[0]);
        } catch (Exception e) {
            throw new KeyAttestationException("Unable to construct certificate chain", e);
        }
    }

    private static KeymasterArguments prepareAttestationArgumentsForDeviceId(Context context, int[] iArr, byte[] bArr) throws DeviceIdAttestationException {
        if (iArr == null) {
            throw new NullPointerException("Missing id types");
        }
        return prepareAttestationArguments(context, iArr, bArr);
    }

    public static KeymasterArguments prepareAttestationArguments(Context context, int[] iArr, byte[] bArr) throws DeviceIdAttestationException {
        if (bArr == null) {
            throw new NullPointerException("Missing attestation challenge");
        }
        KeymasterArguments keymasterArguments = new KeymasterArguments();
        keymasterArguments.addBytes(KeymasterDefs.KM_TAG_ATTESTATION_CHALLENGE, bArr);
        if (iArr == null) {
            return keymasterArguments;
        }
        ArraySet<Integer> arraySet = new ArraySet(iArr.length);
        for (int i : iArr) {
            arraySet.add(Integer.valueOf(i));
        }
        TelephonyManager telephonyManager = null;
        if ((arraySet.contains(2) || arraySet.contains(3)) && (telephonyManager = (TelephonyManager) context.getSystemService("phone")) == null) {
            throw new DeviceIdAttestationException("Unable to access telephony service");
        }
        for (Integer num : arraySet) {
            switch (num.intValue()) {
                case 1:
                    keymasterArguments.addBytes(KeymasterDefs.KM_TAG_ATTESTATION_ID_SERIAL, Build.getSerial().getBytes(StandardCharsets.UTF_8));
                    break;
                case 2:
                    String imei = telephonyManager.getImei(0);
                    if (imei == null) {
                        throw new DeviceIdAttestationException("Unable to retrieve IMEI");
                    }
                    keymasterArguments.addBytes(KeymasterDefs.KM_TAG_ATTESTATION_ID_IMEI, imei.getBytes(StandardCharsets.UTF_8));
                    break;
                    break;
                case 3:
                    String meid = telephonyManager.getMeid(0);
                    if (meid == null) {
                        throw new DeviceIdAttestationException("Unable to retrieve MEID");
                    }
                    keymasterArguments.addBytes(KeymasterDefs.KM_TAG_ATTESTATION_ID_MEID, meid.getBytes(StandardCharsets.UTF_8));
                    break;
                    break;
                default:
                    throw new IllegalArgumentException("Unknown device ID type " + num);
            }
        }
        keymasterArguments.addBytes(KeymasterDefs.KM_TAG_ATTESTATION_ID_BRAND, Build.BRAND.getBytes(StandardCharsets.UTF_8));
        keymasterArguments.addBytes(KeymasterDefs.KM_TAG_ATTESTATION_ID_DEVICE, Build.DEVICE.getBytes(StandardCharsets.UTF_8));
        keymasterArguments.addBytes(KeymasterDefs.KM_TAG_ATTESTATION_ID_PRODUCT, Build.PRODUCT.getBytes(StandardCharsets.UTF_8));
        keymasterArguments.addBytes(KeymasterDefs.KM_TAG_ATTESTATION_ID_MANUFACTURER, Build.MANUFACTURER.getBytes(StandardCharsets.UTF_8));
        keymasterArguments.addBytes(KeymasterDefs.KM_TAG_ATTESTATION_ID_MODEL, Build.MODEL.getBytes(StandardCharsets.UTF_8));
        return keymasterArguments;
    }

    public static X509Certificate[] attestDeviceIds(Context context, int[] iArr, byte[] bArr) throws DeviceIdAttestationException {
        KeymasterArguments keymasterArgumentsPrepareAttestationArgumentsForDeviceId = prepareAttestationArgumentsForDeviceId(context, iArr, bArr);
        KeymasterCertificateChain keymasterCertificateChain = new KeymasterCertificateChain();
        int iAttestDeviceIds = KeyStore.getInstance().attestDeviceIds(keymasterArgumentsPrepareAttestationArgumentsForDeviceId, keymasterCertificateChain);
        if (iAttestDeviceIds != 1) {
            throw new DeviceIdAttestationException("Unable to perform attestation", KeyStore.getKeyStoreException(iAttestDeviceIds));
        }
        try {
            return parseCertificateChain(keymasterCertificateChain);
        } catch (KeyAttestationException e) {
            throw new DeviceIdAttestationException(e.getMessage(), e);
        }
    }

    public static boolean isChainValid(KeymasterCertificateChain keymasterCertificateChain) {
        return keymasterCertificateChain != null && keymasterCertificateChain.getCertificates().size() >= 2;
    }
}
