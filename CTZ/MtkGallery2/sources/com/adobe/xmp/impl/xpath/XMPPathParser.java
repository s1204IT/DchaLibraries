package com.adobe.xmp.impl.xpath;

import com.adobe.xmp.XMPException;
import com.adobe.xmp.XMPMetaFactory;
import com.adobe.xmp.impl.Utils;
import com.adobe.xmp.properties.XMPAliasInfo;

public final class XMPPathParser {
    public static XMPPath expandXPath(String str, String str2) throws XMPException {
        XMPPathSegment indexSegment;
        if (str == null || str2 == null) {
            throw new XMPException("Parameter must not be null", 4);
        }
        XMPPath xMPPath = new XMPPath();
        PathPosition pathPosition = new PathPosition();
        pathPosition.path = str2;
        parseRootNode(str, pathPosition, xMPPath);
        while (pathPosition.stepEnd < str2.length()) {
            pathPosition.stepBegin = pathPosition.stepEnd;
            skipPathDelimiter(str2, pathPosition);
            pathPosition.stepEnd = pathPosition.stepBegin;
            if (str2.charAt(pathPosition.stepBegin) != '[') {
                indexSegment = parseStructSegment(pathPosition);
            } else {
                indexSegment = parseIndexSegment(pathPosition);
            }
            if (indexSegment.getKind() == 1) {
                if (indexSegment.getName().charAt(0) == '@') {
                    indexSegment.setName("?" + indexSegment.getName().substring(1));
                    if (!"?xml:lang".equals(indexSegment.getName())) {
                        throw new XMPException("Only xml:lang allowed with '@'", 102);
                    }
                }
                if (indexSegment.getName().charAt(0) == '?') {
                    pathPosition.nameStart++;
                    indexSegment.setKind(2);
                }
                verifyQualName(pathPosition.path.substring(pathPosition.nameStart, pathPosition.nameEnd));
            } else if (indexSegment.getKind() != 6) {
                continue;
            } else {
                if (indexSegment.getName().charAt(1) == '@') {
                    indexSegment.setName("[?" + indexSegment.getName().substring(2));
                    if (!indexSegment.getName().startsWith("[?xml:lang=")) {
                        throw new XMPException("Only xml:lang allowed with '@'", 102);
                    }
                }
                if (indexSegment.getName().charAt(1) == '?') {
                    pathPosition.nameStart++;
                    indexSegment.setKind(5);
                    verifyQualName(pathPosition.path.substring(pathPosition.nameStart, pathPosition.nameEnd));
                }
            }
            xMPPath.add(indexSegment);
        }
        return xMPPath;
    }

    private static void skipPathDelimiter(String str, PathPosition pathPosition) throws XMPException {
        if (str.charAt(pathPosition.stepBegin) == '/') {
            pathPosition.stepBegin++;
            if (pathPosition.stepBegin >= str.length()) {
                throw new XMPException("Empty XMPPath segment", 102);
            }
        }
        if (str.charAt(pathPosition.stepBegin) == '*') {
            pathPosition.stepBegin++;
            if (pathPosition.stepBegin >= str.length() || str.charAt(pathPosition.stepBegin) != '[') {
                throw new XMPException("Missing '[' after '*'", 102);
            }
        }
    }

    private static XMPPathSegment parseStructSegment(PathPosition pathPosition) throws XMPException {
        pathPosition.nameStart = pathPosition.stepBegin;
        while (pathPosition.stepEnd < pathPosition.path.length() && "/[*".indexOf(pathPosition.path.charAt(pathPosition.stepEnd)) < 0) {
            pathPosition.stepEnd++;
        }
        pathPosition.nameEnd = pathPosition.stepEnd;
        if (pathPosition.stepEnd == pathPosition.stepBegin) {
            throw new XMPException("Empty XMPPath segment", 102);
        }
        return new XMPPathSegment(pathPosition.path.substring(pathPosition.stepBegin, pathPosition.stepEnd), 1);
    }

    private static XMPPathSegment parseIndexSegment(PathPosition pathPosition) throws XMPException {
        XMPPathSegment xMPPathSegment;
        pathPosition.stepEnd++;
        if ('0' <= pathPosition.path.charAt(pathPosition.stepEnd) && pathPosition.path.charAt(pathPosition.stepEnd) <= '9') {
            while (pathPosition.stepEnd < pathPosition.path.length() && '0' <= pathPosition.path.charAt(pathPosition.stepEnd) && pathPosition.path.charAt(pathPosition.stepEnd) <= '9') {
                pathPosition.stepEnd++;
            }
            xMPPathSegment = new XMPPathSegment(null, 3);
        } else {
            while (pathPosition.stepEnd < pathPosition.path.length() && pathPosition.path.charAt(pathPosition.stepEnd) != ']' && pathPosition.path.charAt(pathPosition.stepEnd) != '=') {
                pathPosition.stepEnd++;
            }
            if (pathPosition.stepEnd >= pathPosition.path.length()) {
                throw new XMPException("Missing ']' or '=' for array index", 102);
            }
            if (pathPosition.path.charAt(pathPosition.stepEnd) == ']') {
                if (!"[last()".equals(pathPosition.path.substring(pathPosition.stepBegin, pathPosition.stepEnd))) {
                    throw new XMPException("Invalid non-numeric array index", 102);
                }
                xMPPathSegment = new XMPPathSegment(null, 4);
            } else {
                pathPosition.nameStart = pathPosition.stepBegin + 1;
                pathPosition.nameEnd = pathPosition.stepEnd;
                pathPosition.stepEnd++;
                char cCharAt = pathPosition.path.charAt(pathPosition.stepEnd);
                if (cCharAt != '\'' && cCharAt != '\"') {
                    throw new XMPException("Invalid quote in array selector", 102);
                }
                pathPosition.stepEnd++;
                while (pathPosition.stepEnd < pathPosition.path.length()) {
                    if (pathPosition.path.charAt(pathPosition.stepEnd) == cCharAt) {
                        if (pathPosition.stepEnd + 1 >= pathPosition.path.length() || pathPosition.path.charAt(pathPosition.stepEnd + 1) != cCharAt) {
                            break;
                        }
                        pathPosition.stepEnd++;
                    }
                    pathPosition.stepEnd++;
                }
                if (pathPosition.stepEnd >= pathPosition.path.length()) {
                    throw new XMPException("No terminating quote for array selector", 102);
                }
                pathPosition.stepEnd++;
                xMPPathSegment = new XMPPathSegment(null, 6);
            }
        }
        if (pathPosition.stepEnd >= pathPosition.path.length() || pathPosition.path.charAt(pathPosition.stepEnd) != ']') {
            throw new XMPException("Missing ']' for array index", 102);
        }
        pathPosition.stepEnd++;
        xMPPathSegment.setName(pathPosition.path.substring(pathPosition.stepBegin, pathPosition.stepEnd));
        return xMPPathSegment;
    }

    private static void parseRootNode(String str, PathPosition pathPosition, XMPPath xMPPath) throws XMPException {
        while (pathPosition.stepEnd < pathPosition.path.length() && "/[*".indexOf(pathPosition.path.charAt(pathPosition.stepEnd)) < 0) {
            pathPosition.stepEnd++;
        }
        if (pathPosition.stepEnd == pathPosition.stepBegin) {
            throw new XMPException("Empty initial XMPPath step", 102);
        }
        String strVerifyXPathRoot = verifyXPathRoot(str, pathPosition.path.substring(pathPosition.stepBegin, pathPosition.stepEnd));
        XMPAliasInfo xMPAliasInfoFindAlias = XMPMetaFactory.getSchemaRegistry().findAlias(strVerifyXPathRoot);
        if (xMPAliasInfoFindAlias == null) {
            xMPPath.add(new XMPPathSegment(str, Integer.MIN_VALUE));
            xMPPath.add(new XMPPathSegment(strVerifyXPathRoot, 1));
            return;
        }
        xMPPath.add(new XMPPathSegment(xMPAliasInfoFindAlias.getNamespace(), Integer.MIN_VALUE));
        XMPPathSegment xMPPathSegment = new XMPPathSegment(verifyXPathRoot(xMPAliasInfoFindAlias.getNamespace(), xMPAliasInfoFindAlias.getPropName()), 1);
        xMPPathSegment.setAlias(true);
        xMPPathSegment.setAliasForm(xMPAliasInfoFindAlias.getAliasForm().getOptions());
        xMPPath.add(xMPPathSegment);
        if (xMPAliasInfoFindAlias.getAliasForm().isArrayAltText()) {
            XMPPathSegment xMPPathSegment2 = new XMPPathSegment("[?xml:lang='x-default']", 5);
            xMPPathSegment2.setAlias(true);
            xMPPathSegment2.setAliasForm(xMPAliasInfoFindAlias.getAliasForm().getOptions());
            xMPPath.add(xMPPathSegment2);
            return;
        }
        if (xMPAliasInfoFindAlias.getAliasForm().isArray()) {
            XMPPathSegment xMPPathSegment3 = new XMPPathSegment("[1]", 3);
            xMPPathSegment3.setAlias(true);
            xMPPathSegment3.setAliasForm(xMPAliasInfoFindAlias.getAliasForm().getOptions());
            xMPPath.add(xMPPathSegment3);
        }
    }

    private static void verifyQualName(String str) throws XMPException {
        int iIndexOf = str.indexOf(58);
        if (iIndexOf > 0) {
            String strSubstring = str.substring(0, iIndexOf);
            if (Utils.isXMLNameNS(strSubstring)) {
                if (XMPMetaFactory.getSchemaRegistry().getNamespaceURI(strSubstring) != null) {
                    return;
                } else {
                    throw new XMPException("Unknown namespace prefix for qualified name", 102);
                }
            }
        }
        throw new XMPException("Ill-formed qualified name", 102);
    }

    private static void verifySimpleXMLName(String str) throws XMPException {
        if (!Utils.isXMLName(str)) {
            throw new XMPException("Bad XML name", 102);
        }
    }

    private static String verifyXPathRoot(String str, String str2) throws XMPException {
        if (str == null || str.length() == 0) {
            throw new XMPException("Schema namespace URI is required", 101);
        }
        if (str2.charAt(0) == '?' || str2.charAt(0) == '@') {
            throw new XMPException("Top level name must not be a qualifier", 102);
        }
        if (str2.indexOf(47) >= 0 || str2.indexOf(91) >= 0) {
            throw new XMPException("Top level name must be simple", 102);
        }
        String namespacePrefix = XMPMetaFactory.getSchemaRegistry().getNamespacePrefix(str);
        if (namespacePrefix == null) {
            throw new XMPException("Unregistered schema namespace URI", 101);
        }
        int iIndexOf = str2.indexOf(58);
        if (iIndexOf < 0) {
            verifySimpleXMLName(str2);
            return namespacePrefix + str2;
        }
        verifySimpleXMLName(str2.substring(0, iIndexOf));
        verifySimpleXMLName(str2.substring(iIndexOf));
        String strSubstring = str2.substring(0, iIndexOf + 1);
        String namespacePrefix2 = XMPMetaFactory.getSchemaRegistry().getNamespacePrefix(str);
        if (namespacePrefix2 == null) {
            throw new XMPException("Unknown schema namespace prefix", 101);
        }
        if (!strSubstring.equals(namespacePrefix2)) {
            throw new XMPException("Schema namespace URI and prefix mismatch", 101);
        }
        return str2;
    }
}
