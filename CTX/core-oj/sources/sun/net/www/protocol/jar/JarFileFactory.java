package sun.net.www.protocol.jar;

import java.io.FileNotFoundException;
import java.io.FilePermission;
import java.io.IOException;
import java.net.SocketPermission;
import java.net.URL;
import java.net.URLConnection;
import java.security.Permission;
import java.util.HashMap;
import java.util.jar.JarFile;
import sun.net.util.URLUtil;
import sun.net.www.protocol.jar.URLJarFile;
import sun.security.util.SecurityConstants;

class JarFileFactory implements URLJarFile.URLJarFileCloseController {
    static final boolean $assertionsDisabled = false;
    private static final HashMap<String, JarFile> fileCache = new HashMap<>();
    private static final HashMap<JarFile, URL> urlCache = new HashMap<>();
    private static final JarFileFactory instance = new JarFileFactory();

    private JarFileFactory() {
    }

    public static JarFileFactory getInstance() {
        return instance;
    }

    URLConnection getConnection(JarFile jarFile) throws IOException {
        URL url;
        synchronized (instance) {
            url = urlCache.get(jarFile);
        }
        if (url != null) {
            return url.openConnection();
        }
        return null;
    }

    public JarFile get(URL url) throws IOException {
        return get(url, true);
    }

    JarFile get(URL url, boolean z) throws IOException {
        JarFile jarFile;
        if (z) {
            synchronized (instance) {
                jarFile = getCachedJarFile(url);
            }
            if (jarFile == null) {
                JarFile jarFile2 = URLJarFile.getJarFile(url, this);
                synchronized (instance) {
                    jarFile = getCachedJarFile(url);
                    if (jarFile == null) {
                        fileCache.put(URLUtil.urlNoFragString(url), jarFile2);
                        urlCache.put(jarFile2, url);
                        jarFile = jarFile2;
                    } else if (jarFile2 != null) {
                        jarFile2.close();
                    }
                }
            }
        } else {
            jarFile = URLJarFile.getJarFile(url, this);
        }
        if (jarFile == null) {
            throw new FileNotFoundException(url.toString());
        }
        return jarFile;
    }

    @Override
    public void close(JarFile jarFile) {
        synchronized (instance) {
            URL urlRemove = urlCache.remove(jarFile);
            if (urlRemove != null) {
                fileCache.remove(URLUtil.urlNoFragString(urlRemove));
            }
        }
    }

    private JarFile getCachedJarFile(URL url) {
        Permission permission;
        SecurityManager securityManager;
        JarFile jarFile = fileCache.get(URLUtil.urlNoFragString(url));
        if (jarFile != null && (permission = getPermission(jarFile)) != null && (securityManager = System.getSecurityManager()) != null) {
            try {
                securityManager.checkPermission(permission);
            } catch (SecurityException e) {
                if ((permission instanceof FilePermission) && permission.getActions().indexOf("read") != -1) {
                    securityManager.checkRead(permission.getName());
                } else if ((permission instanceof SocketPermission) && permission.getActions().indexOf(SecurityConstants.SOCKET_CONNECT_ACTION) != -1) {
                    securityManager.checkConnect(url.getHost(), url.getPort());
                } else {
                    throw e;
                }
            }
        }
        return jarFile;
    }

    private Permission getPermission(JarFile jarFile) {
        try {
            URLConnection connection = getConnection(jarFile);
            if (connection != null) {
                return connection.getPermission();
            }
            return null;
        } catch (IOException e) {
            return null;
        }
    }
}
