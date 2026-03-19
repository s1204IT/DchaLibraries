package android.icu.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public abstract class URLHandler {
    private static final boolean DEBUG = ICUDebug.enabled("URLHandler");
    public static final String PROPNAME = "urlhandler.props";
    private static final Map<String, Method> handlers;

    public interface URLVisitor {
        void visit(String str);
    }

    public abstract void guide(URLVisitor uRLVisitor, boolean z, boolean z2);

    static {
        BufferedReader bufferedReader;
        Throwable th;
        HashMap map;
        HashMap map2;
        BufferedReader bufferedReader2 = null;
        HashMap map3 = null;
        BufferedReader bufferedReader3 = null;
        bufferedReader2 = null;
        try {
            try {
                InputStream resourceAsStream = ClassLoaderUtil.getClassLoader(URLHandler.class).getResourceAsStream(PROPNAME);
                if (resourceAsStream != null) {
                    Class<?>[] clsArr = {URL.class};
                    bufferedReader = new BufferedReader(new InputStreamReader(resourceAsStream));
                    while (true) {
                        try {
                            String line = bufferedReader.readLine();
                            if (line == null) {
                                break;
                            }
                            String strTrim = line.trim();
                            if (strTrim.length() != 0 && strTrim.charAt(0) != '#') {
                                int iIndexOf = strTrim.indexOf(61);
                                if (iIndexOf != -1) {
                                    String strTrim2 = strTrim.substring(0, iIndexOf).trim();
                                    try {
                                        Method declaredMethod = Class.forName(strTrim.substring(iIndexOf + 1).trim()).getDeclaredMethod("get", clsArr);
                                        if (map3 == null) {
                                            map3 = new HashMap();
                                        }
                                        map3.put(strTrim2, declaredMethod);
                                    } catch (ClassNotFoundException e) {
                                        if (DEBUG) {
                                            System.err.println(e);
                                        }
                                    } catch (NoSuchMethodException e2) {
                                        if (DEBUG) {
                                            System.err.println(e2);
                                        }
                                    } catch (SecurityException e3) {
                                        if (DEBUG) {
                                            System.err.println(e3);
                                        }
                                    }
                                } else if (DEBUG) {
                                    System.err.println("bad urlhandler line: '" + strTrim + "'");
                                }
                            }
                        } catch (Throwable th2) {
                            th = th2;
                            if (bufferedReader != null) {
                                try {
                                    bufferedReader.close();
                                } catch (IOException e4) {
                                }
                            }
                            throw th;
                        }
                    }
                    bufferedReader.close();
                    map2 = map3;
                    bufferedReader3 = bufferedReader;
                } else {
                    map2 = null;
                }
                if (bufferedReader3 != null) {
                    try {
                        bufferedReader3.close();
                    } catch (IOException e5) {
                    }
                }
            } catch (Throwable th3) {
                th = th3;
                map = null;
            }
            handlers = map2;
        } catch (Throwable th4) {
            bufferedReader = bufferedReader2;
            th = th4;
        }
    }

    public static URLHandler get(URL url) {
        Method method;
        if (url == null) {
            return null;
        }
        String protocol = url.getProtocol();
        if (handlers != null && (method = handlers.get(protocol)) != null) {
            try {
                URLHandler uRLHandler = (URLHandler) method.invoke(null, url);
                if (uRLHandler != null) {
                    return uRLHandler;
                }
            } catch (IllegalAccessException e) {
                if (DEBUG) {
                    System.err.println(e);
                }
            } catch (IllegalArgumentException e2) {
                if (DEBUG) {
                    System.err.println(e2);
                }
            } catch (InvocationTargetException e3) {
                if (DEBUG) {
                    System.err.println(e3);
                }
            }
        }
        return getDefault(url);
    }

    protected static URLHandler getDefault(URL url) {
        URLHandler jarURLHandler;
        String protocol = url.getProtocol();
        try {
            if (protocol.equals("file")) {
                jarURLHandler = new FileURLHandler(url);
            } else {
                if (!protocol.equals("jar") && !protocol.equals("wsjar")) {
                    return null;
                }
                jarURLHandler = new JarURLHandler(url);
            }
            return jarURLHandler;
        } catch (Exception e) {
            return null;
        }
    }

    private static class FileURLHandler extends URLHandler {
        File file;

        FileURLHandler(URL url) {
            try {
                this.file = new File(url.toURI());
            } catch (URISyntaxException e) {
            }
            if (this.file == null || !this.file.exists()) {
                if (URLHandler.DEBUG) {
                    System.err.println("file does not exist - " + url.toString());
                }
                throw new IllegalArgumentException();
            }
        }

        @Override
        public void guide(URLVisitor uRLVisitor, boolean z, boolean z2) {
            if (this.file.isDirectory()) {
                process(uRLVisitor, z, z2, "/", this.file.listFiles());
            } else {
                uRLVisitor.visit(this.file.getName());
            }
        }

        private void process(URLVisitor uRLVisitor, boolean z, boolean z2, String str, File[] fileArr) {
            if (fileArr != null) {
                for (File file : fileArr) {
                    if (file.isDirectory()) {
                        if (z) {
                            process(uRLVisitor, z, z2, str + file.getName() + '/', file.listFiles());
                        }
                    } else {
                        uRLVisitor.visit(z2 ? file.getName() : str + file.getName());
                    }
                }
            }
        }
    }

    private static class JarURLHandler extends URLHandler {
        JarFile jarFile;
        String prefix;

        JarURLHandler(URL url) {
            String string;
            int iIndexOf;
            try {
                this.prefix = url.getPath();
                int iLastIndexOf = this.prefix.lastIndexOf("!/");
                if (iLastIndexOf >= 0) {
                    this.prefix = this.prefix.substring(iLastIndexOf + 2);
                }
                if (!url.getProtocol().equals("jar") && (iIndexOf = (string = url.toString()).indexOf(":")) != -1) {
                    url = new URL("jar" + string.substring(iIndexOf));
                }
                this.jarFile = ((JarURLConnection) url.openConnection()).getJarFile();
            } catch (Exception e) {
                if (URLHandler.DEBUG) {
                    System.err.println("icurb jar error: " + e);
                }
                throw new IllegalArgumentException("jar error: " + e.getMessage());
            }
        }

        @Override
        public void guide(URLVisitor uRLVisitor, boolean z, boolean z2) {
            String strSubstring;
            int iLastIndexOf;
            try {
                Enumeration<JarEntry> enumerationEntries = this.jarFile.entries();
                while (enumerationEntries.hasMoreElements()) {
                    JarEntry jarEntryNextElement = enumerationEntries.nextElement();
                    if (!jarEntryNextElement.isDirectory()) {
                        String name = jarEntryNextElement.getName();
                        if (name.startsWith(this.prefix) && ((iLastIndexOf = (strSubstring = name.substring(this.prefix.length())).lastIndexOf(47)) <= 0 || z)) {
                            if (z2 && iLastIndexOf != -1) {
                                strSubstring = strSubstring.substring(iLastIndexOf + 1);
                            }
                            uRLVisitor.visit(strSubstring);
                        }
                    }
                }
            } catch (Exception e) {
                if (URLHandler.DEBUG) {
                    System.err.println("icurb jar error: " + e);
                }
            }
        }
    }

    public void guide(URLVisitor uRLVisitor, boolean z) {
        guide(uRLVisitor, z, true);
    }
}
