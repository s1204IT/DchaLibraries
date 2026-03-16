package java.util.jar;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import libcore.io.Base64;
import org.apache.harmony.security.utils.JarUtils;

class JarVerifier {
    private static final String[] DIGEST_ALGORITHMS = {"SHA-512", "SHA-384", "SHA-256", "SHA1"};
    private final String jarName;
    private final int mainAttributesEnd;
    private final Manifest manifest;
    private final HashMap<String, byte[]> metaEntries;
    private final Hashtable<String, HashMap<String, Attributes>> signatures = new Hashtable<>(5);
    private final Hashtable<String, Certificate[]> certificates = new Hashtable<>(5);
    private final Hashtable<String, Certificate[][]> verifiedEntries = new Hashtable<>();

    static class VerifierEntry extends OutputStream {
        private final Certificate[][] certChains;
        private final MessageDigest digest;
        private final byte[] hash;
        private final String name;
        private final Hashtable<String, Certificate[][]> verifiedEntries;

        VerifierEntry(String name, MessageDigest digest, byte[] hash, Certificate[][] certChains, Hashtable<String, Certificate[][]> verifedEntries) {
            this.name = name;
            this.digest = digest;
            this.hash = hash;
            this.certChains = certChains;
            this.verifiedEntries = verifedEntries;
        }

        @Override
        public void write(int value) {
            this.digest.update((byte) value);
        }

        @Override
        public void write(byte[] buf, int off, int nbytes) {
            this.digest.update(buf, off, nbytes);
        }

        void verify() {
            byte[] d = this.digest.digest();
            if (!MessageDigest.isEqual(d, Base64.decode(this.hash))) {
                throw JarVerifier.invalidDigest(JarFile.MANIFEST_NAME, this.name, this.name);
            }
            this.verifiedEntries.put(this.name, this.certChains);
        }
    }

    private static SecurityException invalidDigest(String signatureFile, String name, String jarName) {
        throw new SecurityException(signatureFile + " has invalid digest for " + name + " in " + jarName);
    }

    private static SecurityException failedVerification(String jarName, String signatureFile) {
        throw new SecurityException(jarName + " failed verification of " + signatureFile);
    }

    JarVerifier(String name, Manifest manifest, HashMap<String, byte[]> metaEntries) {
        this.jarName = name;
        this.manifest = manifest;
        this.metaEntries = metaEntries;
        this.mainAttributesEnd = manifest.getMainAttributesEnd();
    }

    VerifierEntry initEntry(String name) {
        Attributes attributes;
        if (this.manifest == null || this.signatures.isEmpty() || (attributes = this.manifest.getAttributes(name)) == null) {
            return null;
        }
        ArrayList<Certificate[]> certChains = new ArrayList<>();
        for (Map.Entry<String, HashMap<String, Attributes>> entry : this.signatures.entrySet()) {
            HashMap<String, Attributes> hm = entry.getValue();
            if (hm.get(name) != null) {
                String signatureFile = entry.getKey();
                Certificate[] certChain = this.certificates.get(signatureFile);
                if (certChain != null) {
                    certChains.add(certChain);
                }
            }
        }
        if (certChains.isEmpty()) {
            return null;
        }
        Certificate[][] certChainsArray = (Certificate[][]) certChains.toArray(new Certificate[certChains.size()][]);
        for (int i = 0; i < DIGEST_ALGORITHMS.length; i++) {
            String algorithm = DIGEST_ALGORITHMS[i];
            String hash = attributes.getValue(algorithm + "-Digest");
            if (hash != null) {
                byte[] hashBytes = hash.getBytes(StandardCharsets.ISO_8859_1);
                try {
                    return new VerifierEntry(name, MessageDigest.getInstance(algorithm), hashBytes, certChainsArray, this.verifiedEntries);
                } catch (NoSuchAlgorithmException e) {
                }
            }
        }
        return null;
    }

    void addMetaEntry(String name, byte[] buf) {
        this.metaEntries.put(name.toUpperCase(Locale.US), buf);
    }

    synchronized boolean readCertificates() {
        boolean z;
        if (this.metaEntries.isEmpty()) {
            z = false;
        } else {
            Iterator<String> it = this.metaEntries.keySet().iterator();
            while (it.hasNext()) {
                String key = it.next();
                if (key.endsWith(".DSA") || key.endsWith(".RSA") || key.endsWith(".EC")) {
                    verifyCertificate(key);
                    it.remove();
                }
            }
            z = true;
        }
        return z;
    }

    private void verifyCertificate(String certFile) {
        byte[] manifestBytes;
        String signatureFile = certFile.substring(0, certFile.lastIndexOf(46)) + ".SF";
        byte[] sfBytes = this.metaEntries.get(signatureFile);
        if (sfBytes != null && (manifestBytes = this.metaEntries.get(JarFile.MANIFEST_NAME)) != null) {
            byte[] sBlockBytes = this.metaEntries.get(certFile);
            try {
                Certificate[] signerCertChain = JarUtils.verifySignature(new ByteArrayInputStream(sfBytes), new ByteArrayInputStream(sBlockBytes));
                if (signerCertChain != null) {
                    this.certificates.put(signatureFile, signerCertChain);
                }
                Attributes attributes = new Attributes();
                HashMap<String, Attributes> entries = new HashMap<>();
                try {
                    ManifestReader im = new ManifestReader(sfBytes, attributes);
                    im.readEntries(entries, null);
                    if (attributes.get(Attributes.Name.SIGNATURE_VERSION) != null) {
                        boolean createdBySigntool = false;
                        String createdBy = attributes.getValue("Created-By");
                        if (createdBy != null) {
                            createdBySigntool = createdBy.indexOf("signtool") != -1;
                        }
                        if (this.mainAttributesEnd > 0 && !createdBySigntool && !verify(attributes, "-Digest-Manifest-Main-Attributes", manifestBytes, 0, this.mainAttributesEnd, false, true)) {
                            throw failedVerification(this.jarName, signatureFile);
                        }
                        String digestAttribute = createdBySigntool ? "-Digest" : "-Digest-Manifest";
                        if (!verify(attributes, digestAttribute, manifestBytes, 0, manifestBytes.length, false, false)) {
                            for (Map.Entry<String, Attributes> entry : entries.entrySet()) {
                                Manifest.Chunk chunk = this.manifest.getChunk(entry.getKey());
                                if (chunk != null) {
                                    if (!verify(entry.getValue(), "-Digest", manifestBytes, chunk.start, chunk.end, createdBySigntool, false)) {
                                        throw invalidDigest(signatureFile, entry.getKey(), this.jarName);
                                    }
                                } else {
                                    return;
                                }
                            }
                        }
                        this.metaEntries.put(signatureFile, null);
                        this.signatures.put(signatureFile, entries);
                    }
                } catch (IOException e) {
                }
            } catch (IOException e2) {
            } catch (GeneralSecurityException e3) {
                throw failedVerification(this.jarName, signatureFile);
            }
        }
    }

    boolean isSignedJar() {
        return this.certificates.size() > 0;
    }

    private boolean verify(Attributes attributes, String entry, byte[] data, int start, int end, boolean ignoreSecondEndline, boolean ignorable) {
        for (int i = 0; i < DIGEST_ALGORITHMS.length; i++) {
            String algorithm = DIGEST_ALGORITHMS[i];
            String hash = attributes.getValue(algorithm + entry);
            if (hash != null) {
                try {
                    MessageDigest md = MessageDigest.getInstance(algorithm);
                    if (ignoreSecondEndline && data[end - 1] == 10 && data[end - 2] == 10) {
                        md.update(data, start, (end - 1) - start);
                    } else {
                        md.update(data, start, end - start);
                    }
                    byte[] b = md.digest();
                    byte[] hashBytes = hash.getBytes(StandardCharsets.ISO_8859_1);
                    boolean ignorable2 = MessageDigest.isEqual(b, Base64.decode(hashBytes));
                    return ignorable2;
                } catch (NoSuchAlgorithmException e) {
                }
            }
        }
        return ignorable;
    }

    Certificate[][] getCertificateChains(String name) {
        return this.verifiedEntries.get(name);
    }

    void removeMetaEntries() {
        this.metaEntries.clear();
    }
}
