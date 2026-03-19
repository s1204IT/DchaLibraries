package sun.security.util;

import java.io.IOException;
import java.security.CodeSigner;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarException;
import java.util.jar.Manifest;
import sun.security.jca.Providers;

public class ManifestEntryVerifier {
    private static final Debug debug = Debug.getInstance("jar");
    private static final char[] hexc = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    private JarEntry entry;
    private Manifest man;
    private String name = null;
    private boolean skip = true;
    private CodeSigner[] signers = null;
    HashMap<String, MessageDigest> createdDigests = new HashMap<>(11);
    ArrayList<MessageDigest> digests = new ArrayList<>();
    ArrayList<byte[]> manifestHashes = new ArrayList<>();

    private static class SunProviderHolder {
        private static final Provider instance = Providers.getSunProvider();

        private SunProviderHolder() {
        }
    }

    public ManifestEntryVerifier(Manifest manifest) {
        this.man = manifest;
    }

    public void setEntry(String str, JarEntry jarEntry) throws IOException {
        this.digests.clear();
        this.manifestHashes.clear();
        this.name = str;
        this.entry = jarEntry;
        this.skip = true;
        this.signers = null;
        if (this.man == null || str == null) {
            return;
        }
        Attributes attributes = this.man.getAttributes(str);
        if (attributes == null) {
            attributes = this.man.getAttributes("./" + str);
            if (attributes == null) {
                attributes = this.man.getAttributes("/" + str);
                if (attributes == null) {
                    return;
                }
            }
        }
        for (Map.Entry<Object, Object> entry : attributes.entrySet()) {
            String string = entry.getKey().toString();
            if (string.toUpperCase(Locale.ENGLISH).endsWith("-DIGEST")) {
                String strSubstring = string.substring(0, string.length() - 7);
                MessageDigest messageDigest = this.createdDigests.get(strSubstring);
                if (messageDigest == null) {
                    try {
                        MessageDigest messageDigest2 = MessageDigest.getInstance(strSubstring, SunProviderHolder.instance);
                        try {
                            this.createdDigests.put(strSubstring, messageDigest2);
                            messageDigest = messageDigest2;
                        } catch (NoSuchAlgorithmException e) {
                            messageDigest = messageDigest2;
                        }
                    } catch (NoSuchAlgorithmException e2) {
                    }
                }
                if (messageDigest != null) {
                    this.skip = false;
                    messageDigest.reset();
                    this.digests.add(messageDigest);
                    this.manifestHashes.add(Base64.getMimeDecoder().decode((String) entry.getValue()));
                }
            }
        }
    }

    public void update(byte b) {
        if (this.skip) {
            return;
        }
        for (int i = 0; i < this.digests.size(); i++) {
            this.digests.get(i).update(b);
        }
    }

    public void update(byte[] bArr, int i, int i2) {
        if (this.skip) {
            return;
        }
        for (int i3 = 0; i3 < this.digests.size(); i3++) {
            this.digests.get(i3).update(bArr, i, i2);
        }
    }

    public JarEntry getEntry() {
        return this.entry;
    }

    public CodeSigner[] verify(Hashtable<String, CodeSigner[]> hashtable, Hashtable<String, CodeSigner[]> hashtable2) throws JarException {
        if (this.skip) {
            return null;
        }
        if (this.signers != null) {
            return this.signers;
        }
        for (int i = 0; i < this.digests.size(); i++) {
            MessageDigest messageDigest = this.digests.get(i);
            byte[] bArr = this.manifestHashes.get(i);
            byte[] bArrDigest = messageDigest.digest();
            if (debug != null) {
                debug.println("Manifest Entry: " + this.name + " digest=" + messageDigest.getAlgorithm());
                Debug debug2 = debug;
                StringBuilder sb = new StringBuilder();
                sb.append("  manifest ");
                sb.append(toHex(bArr));
                debug2.println(sb.toString());
                debug.println("  computed " + toHex(bArrDigest));
                debug.println();
            }
            if (!MessageDigest.isEqual(bArrDigest, bArr)) {
                throw new SecurityException(messageDigest.getAlgorithm() + " digest error for " + this.name);
            }
        }
        this.signers = hashtable2.remove(this.name);
        if (this.signers != null) {
            hashtable.put(this.name, this.signers);
        }
        return this.signers;
    }

    static String toHex(byte[] bArr) {
        StringBuffer stringBuffer = new StringBuffer(bArr.length * 2);
        for (int i = 0; i < bArr.length; i++) {
            stringBuffer.append(hexc[(bArr[i] >> 4) & 15]);
            stringBuffer.append(hexc[bArr[i] & 15]);
        }
        return stringBuffer.toString();
    }
}
