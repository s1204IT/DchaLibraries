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
        Map<String, Method> h = null;
        BufferedReader br = null;
        try {
            try {
                ClassLoader loader = ClassLoaderUtil.getClassLoader(URLHandler.class);
                InputStream is = loader.getResourceAsStream(PROPNAME);
                if (is != null) {
                    Class<?>[] params = {URL.class};
                    BufferedReader br2 = new BufferedReader(new InputStreamReader(is));
                    try {
                        String line = br2.readLine();
                        Map<String, Method> h2 = null;
                        while (true) {
                            if (line == null) {
                                break;
                            }
                            try {
                                String line2 = line.trim();
                                if (line2.length() == 0 || line2.charAt(0) == '#') {
                                    h = h2;
                                } else {
                                    int ix = line2.indexOf(61);
                                    if (ix == -1) {
                                        if (DEBUG) {
                                            System.err.println("bad urlhandler line: '" + line2 + "'");
                                        }
                                    } else {
                                        String key = line2.substring(0, ix).trim();
                                        String value = line2.substring(ix + 1).trim();
                                        try {
                                            Class<?> cl = Class.forName(value);
                                            Method m = cl.getDeclaredMethod("get", params);
                                            h = h2 == null ? new HashMap<>() : h2;
                                            try {
                                                h.put(key, m);
                                            } catch (ClassNotFoundException e) {
                                                e = e;
                                                if (DEBUG) {
                                                    System.err.println(e);
                                                }
                                            } catch (NoSuchMethodException e2) {
                                                e = e2;
                                                if (DEBUG) {
                                                    System.err.println(e);
                                                }
                                            } catch (SecurityException e3) {
                                                e = e3;
                                                if (DEBUG) {
                                                    System.err.println(e);
                                                }
                                            }
                                        } catch (ClassNotFoundException e4) {
                                            e = e4;
                                            h = h2;
                                        } catch (NoSuchMethodException e5) {
                                            e = e5;
                                            h = h2;
                                        } catch (SecurityException e6) {
                                            e = e6;
                                            h = h2;
                                        }
                                    }
                                }
                                line = br2.readLine();
                                h2 = h;
                            } catch (Throwable th) {
                                t = th;
                                br = br2;
                                h = h2;
                                if (DEBUG) {
                                    System.err.println(t);
                                }
                                if (br != null) {
                                    try {
                                        br.close();
                                    } catch (IOException e7) {
                                    }
                                }
                            }
                        }
                        br2.close();
                        br = br2;
                        h = h2;
                    } catch (Throwable th2) {
                        th = th2;
                        br = br2;
                    }
                }
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException e8) {
                    }
                }
            } catch (Throwable th3) {
                th = th3;
            }
        } catch (Throwable th4) {
            t = th4;
        }
        handlers = h;
    }

    public static URLHandler get(URL url) {
        Method m;
        if (url == null) {
            return null;
        }
        String protocol = url.getProtocol();
        if (handlers != null && (m = handlers.get(protocol)) != null) {
            try {
                URLHandler handler = (URLHandler) m.invoke(null, url);
                if (handler != null) {
                    return handler;
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
        URLHandler handler = null;
        String protocol = url.getProtocol();
        try {
            if (protocol.equals("file")) {
                handler = new FileURLHandler(url);
            } else if (protocol.equals("jar") || protocol.equals("wsjar")) {
                handler = new JarURLHandler(url);
            }
        } catch (Exception e) {
        }
        return handler;
    }

    private static class FileURLHandler extends URLHandler {
        File file;

        FileURLHandler(URL url) {
            try {
                this.file = new File(url.toURI());
            } catch (URISyntaxException e) {
            }
            if (this.file != null && this.file.exists()) {
                return;
            }
            if (URLHandler.DEBUG) {
                System.err.println("file does not exist - " + url.toString());
            }
            throw new IllegalArgumentException();
        }

        @Override
        public void guide(URLVisitor v, boolean recurse, boolean strip) {
            if (this.file.isDirectory()) {
                process(v, recurse, strip, "/", this.file.listFiles());
            } else {
                v.visit(this.file.getName());
            }
        }

        private void process(URLVisitor v, boolean recurse, boolean strip, String path, File[] files) {
            for (File f : files) {
                if (f.isDirectory()) {
                    if (recurse) {
                        process(v, recurse, strip, path + f.getName() + '/', f.listFiles());
                    }
                } else {
                    v.visit(strip ? f.getName() : path + f.getName());
                }
            }
        }
    }

    private static class JarURLHandler extends URLHandler {
        JarFile jarFile;
        String prefix;

        JarURLHandler(URL url) {
            String urlStr;
            int idx;
            try {
                this.prefix = url.getPath();
                int ix = this.prefix.lastIndexOf("!/");
                if (ix >= 0) {
                    this.prefix = this.prefix.substring(ix + 2);
                }
                String protocol = url.getProtocol();
                if (!protocol.equals("jar") && (idx = (urlStr = url.toString()).indexOf(":")) != -1) {
                    url = new URL("jar" + urlStr.substring(idx));
                }
                JarURLConnection conn = (JarURLConnection) url.openConnection();
                this.jarFile = conn.getJarFile();
            } catch (Exception e) {
                if (URLHandler.DEBUG) {
                    System.err.println("icurb jar error: " + e);
                }
                throw new IllegalArgumentException("jar error: " + e.getMessage());
            }
        }

        @Override
        public void guide(URLVisitor v, boolean recurse, boolean strip) {
            String name;
            int ix;
            try {
                Enumeration<JarEntry> entries = this.jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (!entry.isDirectory()) {
                        String name2 = entry.getName();
                        if (name2.startsWith(this.prefix) && ((ix = (name = name2.substring(this.prefix.length())).lastIndexOf(47)) <= 0 || recurse)) {
                            if (strip && ix != -1) {
                                name = name.substring(ix + 1);
                            }
                            v.visit(name);
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

    public void guide(URLVisitor visitor, boolean recurse) {
        guide(visitor, recurse, true);
    }
}
