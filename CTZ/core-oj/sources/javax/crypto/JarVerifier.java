package javax.crypto;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.cert.Certificate;
import java.util.jar.JarEntry;
import java.util.jar.JarException;
import java.util.jar.JarFile;

final class JarVerifier {
    private CryptoPermissions appPerms = null;
    private URL jarURL;
    private boolean savePerms;

    JarVerifier(URL url, boolean z) {
        this.jarURL = url;
        this.savePerms = z;
    }

    void verify() throws IOException {
        final URL url;
        JarFile jarFile;
        if (!this.savePerms) {
            return;
        }
        if (this.jarURL.getProtocol().equalsIgnoreCase("jar")) {
            url = this.jarURL;
        } else {
            url = new URL("jar:" + this.jarURL.toString() + "!/");
        }
        try {
            try {
                jarFile = (JarFile) AccessController.doPrivileged(new PrivilegedExceptionAction<JarFile>() {
                    @Override
                    public JarFile run() throws Exception {
                        JarURLConnection jarURLConnection = (JarURLConnection) url.openConnection();
                        jarURLConnection.setUseCaches(false);
                        return jarURLConnection.getJarFile();
                    }
                });
                if (jarFile != null) {
                    try {
                        JarEntry jarEntry = jarFile.getJarEntry("cryptoPerms");
                        if (jarEntry == null) {
                            throw new JarException("Can not find cryptoPerms");
                        }
                        try {
                            this.appPerms = new CryptoPermissions();
                            this.appPerms.load(jarFile.getInputStream(jarEntry));
                        } catch (Exception e) {
                            JarException jarException = new JarException("Cannot load/parse" + this.jarURL.toString());
                            jarException.initCause(e);
                            throw jarException;
                        }
                    } catch (Throwable th) {
                        th = th;
                        if (jarFile != null) {
                            jarFile.close();
                        }
                        throw th;
                    }
                }
                if (jarFile != null) {
                    jarFile.close();
                }
            } catch (Throwable th2) {
                th = th2;
                jarFile = null;
            }
        } catch (PrivilegedActionException e2) {
            throw new SecurityException("Cannot load " + url.toString(), e2);
        }
    }

    static void verifyPolicySigned(Certificate[] certificateArr) throws Exception {
    }

    CryptoPermissions getPermissions() {
        return this.appPerms;
    }
}
