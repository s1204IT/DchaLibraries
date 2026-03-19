package java.net;

import java.io.IOException;
import java.util.Objects;
import sun.net.util.IPAddressUtil;

public abstract class URLStreamHandler {
    protected abstract URLConnection openConnection(URL url) throws IOException;

    protected URLConnection openConnection(URL url, Proxy proxy) throws IOException {
        throw new UnsupportedOperationException("Method not implemented.");
    }

    protected void parseURL(URL url, String str, int i, int i2) {
        boolean z;
        String str2;
        String str3;
        int i3;
        int i4;
        int i5;
        int iLastIndexOf;
        int iLastIndexOf2;
        String strSubstring;
        String strSubstring2;
        String strSubstring3;
        int iIndexOf;
        String strSubstring4 = str;
        int i6 = i2;
        String protocol = url.getProtocol();
        String authority = url.getAuthority();
        String userInfo = url.getUserInfo();
        String host = url.getHost();
        int port = url.getPort();
        String path = url.getPath();
        String query = url.getQuery();
        String ref = url.getRef();
        if (i >= i6 || (iIndexOf = strSubstring4.indexOf(63)) == -1 || iIndexOf >= i6) {
            z = false;
        } else {
            query = strSubstring4.substring(iIndexOf + 1, i6);
            if (i6 > iIndexOf) {
                i6 = iIndexOf;
            }
            strSubstring4 = strSubstring4.substring(0, iIndexOf);
            z = true;
        }
        String str4 = null;
        if (i <= i6 - 2 && strSubstring4.charAt(i) == '/' && strSubstring4.charAt(i + 1) == '/') {
            int i7 = i + 2;
            i3 = i7;
            while (i3 < i6) {
                char cCharAt = strSubstring4.charAt(i3);
                if (cCharAt == '#' || cCharAt == '/' || cCharAt == '?' || cCharAt == '\\') {
                    break;
                } else {
                    i3++;
                }
            }
            String strSubstring5 = strSubstring4.substring(i7, i3);
            int iIndexOf2 = strSubstring5.indexOf(64);
            if (iIndexOf2 != -1) {
                if (iIndexOf2 == strSubstring5.lastIndexOf(64)) {
                    strSubstring2 = strSubstring5.substring(0, iIndexOf2);
                    strSubstring = strSubstring5.substring(iIndexOf2 + 1);
                } else {
                    strSubstring = null;
                    strSubstring2 = null;
                }
            } else {
                strSubstring = strSubstring5;
                strSubstring2 = null;
            }
            if (strSubstring != null) {
                if (strSubstring.length() > 0 && strSubstring.charAt(0) == '[') {
                    int iIndexOf3 = strSubstring.indexOf(93);
                    if (iIndexOf3 > 2) {
                        int i8 = iIndexOf3 + 1;
                        String strSubstring6 = strSubstring.substring(0, i8);
                        if (!IPAddressUtil.isIPv6LiteralAddress(strSubstring6.substring(1, iIndexOf3))) {
                            throw new IllegalArgumentException("Invalid host: " + strSubstring6);
                        }
                        if (strSubstring.length() > i8) {
                            if (strSubstring.charAt(i8) == ':') {
                                int i9 = i8 + 1;
                                if (strSubstring.length() > i9) {
                                    port = Integer.parseInt(strSubstring.substring(i9));
                                }
                                host = strSubstring6;
                            } else {
                                throw new IllegalArgumentException("Invalid authority field: " + strSubstring5);
                            }
                        } else {
                            port = -1;
                            host = strSubstring6;
                        }
                    } else {
                        throw new IllegalArgumentException("Invalid authority field: " + strSubstring5);
                    }
                } else {
                    int iIndexOf4 = strSubstring.indexOf(58);
                    if (iIndexOf4 >= 0) {
                        int i10 = iIndexOf4 + 1;
                        if (strSubstring.length() > i10) {
                            char cCharAt2 = strSubstring.charAt(i10);
                            if (cCharAt2 >= '0' && cCharAt2 <= '9') {
                                port = Integer.parseInt(strSubstring.substring(i10));
                            } else {
                                throw new IllegalArgumentException("invalid port: " + strSubstring.substring(i10));
                            }
                        } else {
                            port = -1;
                        }
                        strSubstring3 = strSubstring.substring(0, iIndexOf4);
                    } else {
                        host = strSubstring;
                        port = -1;
                    }
                }
                if (port >= -1) {
                    throw new IllegalArgumentException("Invalid port number :" + port);
                }
                if (z) {
                    str3 = strSubstring5;
                    String str5 = query;
                    str2 = strSubstring2;
                    path = null;
                    str4 = str5;
                } else {
                    str3 = strSubstring5;
                    str2 = strSubstring2;
                    path = null;
                }
            } else {
                strSubstring3 = "";
            }
            host = strSubstring3;
            if (port >= -1) {
            }
        } else {
            str4 = query;
            str2 = userInfo;
            str3 = authority;
            i3 = i;
        }
        if (host == null) {
            host = "";
        }
        if (i3 < i6) {
            if (strSubstring4.charAt(i3) == '/' || strSubstring4.charAt(i3) == '\\') {
                path = strSubstring4.substring(i3, i6);
            } else if (path != null && path.length() > 0) {
                int iLastIndexOf3 = path.lastIndexOf(47);
                String str6 = "";
                if (iLastIndexOf3 == -1 && str3 != null) {
                    str6 = "/";
                }
                path = path.substring(0, iLastIndexOf3 + 1) + str6 + strSubstring4.substring(i3, i6);
            } else {
                path = (str3 != null ? "/" : "") + strSubstring4.substring(i3, i6);
            }
        }
        if (path == null) {
            path = "";
        }
        while (true) {
            int iIndexOf5 = path.indexOf("/./");
            if (iIndexOf5 < 0) {
                break;
            }
            path = path.substring(0, iIndexOf5) + path.substring(iIndexOf5 + 2);
        }
        while (true) {
            int i11 = 0;
            while (true) {
                int iIndexOf6 = path.indexOf("/../", i11);
                if (iIndexOf6 < 0) {
                    break;
                }
                if (iIndexOf6 == 0) {
                    path = path.substring(iIndexOf6 + 3);
                    break;
                }
                if (iIndexOf6 > 0 && (iLastIndexOf2 = path.lastIndexOf(47, iIndexOf6 - 1)) >= 0 && path.indexOf("/../", iLastIndexOf2) != 0) {
                    path = path.substring(0, iLastIndexOf2) + path.substring(iIndexOf6 + 3);
                    break;
                }
                i11 = iIndexOf6 + 3;
            }
        }
        while (path.endsWith("/..") && (iLastIndexOf = path.lastIndexOf(47, path.indexOf("/..") - 1)) >= 0) {
            path = path.substring(0, iLastIndexOf + 1);
        }
        if (path.startsWith("./") && path.length() > 2) {
            path = path.substring(2);
        }
        if (path.endsWith("/.")) {
            i4 = 1;
            i5 = 0;
            path = path.substring(0, path.length() - 1);
        } else {
            i4 = 1;
            i5 = 0;
        }
        if (path.endsWith("?")) {
            path = path.substring(i5, path.length() - i4);
        }
        setURL(url, protocol, host, port, str3, str2, path, str4, ref);
    }

    protected int getDefaultPort() {
        return -1;
    }

    protected boolean equals(URL url, URL url2) {
        return Objects.equals(url.getRef(), url2.getRef()) && Objects.equals(url.getQuery(), url2.getQuery()) && sameFile(url, url2);
    }

    protected int hashCode(URL url) {
        return Objects.hash(url.getRef(), url.getQuery(), url.getProtocol(), url.getFile(), url.getHost(), Integer.valueOf(url.getPort()));
    }

    protected boolean sameFile(URL url, URL url2) {
        if (url.getProtocol() != url2.getProtocol() && (url.getProtocol() == null || !url.getProtocol().equalsIgnoreCase(url2.getProtocol()))) {
            return false;
        }
        if (url.getFile() == url2.getFile() || (url.getFile() != null && url.getFile().equals(url2.getFile()))) {
            return (url.getPort() != -1 ? url.getPort() : url.handler.getDefaultPort()) == (url2.getPort() != -1 ? url2.getPort() : url2.handler.getDefaultPort()) && hostsEqual(url, url2);
        }
        return false;
    }

    protected synchronized InetAddress getHostAddress(URL url) {
        if (url.hostAddress != null) {
            return url.hostAddress;
        }
        String host = url.getHost();
        if (host == null || host.equals("")) {
            return null;
        }
        try {
            url.hostAddress = InetAddress.getByName(host);
            return url.hostAddress;
        } catch (SecurityException e) {
            return null;
        } catch (UnknownHostException e2) {
            return null;
        }
    }

    protected boolean hostsEqual(URL url, URL url2) {
        if (url.getHost() == null || url2.getHost() == null) {
            return url.getHost() == null && url2.getHost() == null;
        }
        return url.getHost().equalsIgnoreCase(url2.getHost());
    }

    protected String toExternalForm(URL url) {
        int length = url.getProtocol().length() + 1;
        if (url.getAuthority() != null && url.getAuthority().length() > 0) {
            length += 2 + url.getAuthority().length();
        }
        if (url.getPath() != null) {
            length += url.getPath().length();
        }
        if (url.getQuery() != null) {
            length += url.getQuery().length() + 1;
        }
        if (url.getRef() != null) {
            length += 1 + url.getRef().length();
        }
        StringBuilder sb = new StringBuilder(length);
        sb.append(url.getProtocol());
        sb.append(":");
        if (url.getAuthority() != null) {
            sb.append("//");
            sb.append(url.getAuthority());
        }
        String file = url.getFile();
        if (file != null) {
            sb.append(file);
        }
        if (url.getRef() != null) {
            sb.append("#");
            sb.append(url.getRef());
        }
        return sb.toString();
    }

    protected void setURL(URL url, String str, String str2, int i, String str3, String str4, String str5, String str6, String str7) {
        if (this == url.handler) {
            url.set(url.getProtocol(), str2, i, str3, str4, str5, str6, str7);
            return;
        }
        throw new SecurityException("handler for url different from this handler");
    }

    @Deprecated
    protected void setURL(URL url, String str, String str2, int i, String str3, String str4) {
        String str5;
        String str6;
        String strSubstring;
        String strSubstring2;
        String str7;
        String str8;
        String strSubstring3 = str2;
        if (strSubstring3 == null || strSubstring3.length() == 0) {
            str5 = strSubstring3;
            str6 = null;
            strSubstring = null;
        } else {
            if (i != -1) {
                str8 = strSubstring3 + ":" + i;
            } else {
                str8 = strSubstring3;
            }
            int iLastIndexOf = strSubstring3.lastIndexOf(64);
            if (iLastIndexOf != -1) {
                strSubstring = strSubstring3.substring(0, iLastIndexOf);
                strSubstring3 = strSubstring3.substring(iLastIndexOf + 1);
            } else {
                strSubstring = null;
            }
            str6 = str8;
            str5 = strSubstring3;
        }
        if (str3 != null) {
            int iLastIndexOf2 = str3.lastIndexOf(63);
            if (iLastIndexOf2 != -1) {
                String strSubstring4 = str3.substring(iLastIndexOf2 + 1);
                strSubstring2 = str3.substring(0, iLastIndexOf2);
                str7 = strSubstring4;
            } else {
                strSubstring2 = str3;
                str7 = null;
            }
        } else {
            strSubstring2 = null;
            str7 = null;
        }
        setURL(url, str, str5, i, str6, strSubstring, strSubstring2, str7, str4);
    }
}
