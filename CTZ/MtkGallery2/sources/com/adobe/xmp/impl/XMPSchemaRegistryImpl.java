package com.adobe.xmp.impl;

import com.adobe.xmp.XMPException;
import com.adobe.xmp.XMPSchemaRegistry;
import com.adobe.xmp.options.AliasOptions;
import com.adobe.xmp.properties.XMPAliasInfo;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import mf.org.apache.xerces.impl.xs.SchemaSymbols;

public final class XMPSchemaRegistryImpl implements XMPSchemaRegistry {
    private Map namespaceToPrefixMap = new HashMap();
    private Map prefixToNamespaceMap = new HashMap();
    private Map aliasMap = new HashMap();
    private Pattern p = Pattern.compile("[/*?\\[\\]]");

    public XMPSchemaRegistryImpl() {
        try {
            registerStandardNamespaces();
            registerStandardAliases();
        } catch (XMPException e) {
            throw new RuntimeException("The XMPSchemaRegistry cannot be initialized!");
        }
    }

    @Override
    public synchronized String registerNamespace(String str, String str2) throws XMPException {
        ParameterAsserts.assertSchemaNS(str);
        ParameterAsserts.assertPrefix(str2);
        if (str2.charAt(str2.length() - 1) != ':') {
            str2 = str2 + ':';
        }
        if (!Utils.isXMLNameNS(str2.substring(0, str2.length() - 1))) {
            throw new XMPException("The prefix is a bad XML name", 201);
        }
        String str3 = (String) this.namespaceToPrefixMap.get(str);
        String str4 = (String) this.prefixToNamespaceMap.get(str2);
        if (str3 != null) {
            return str3;
        }
        if (str4 != null) {
            String str5 = str2;
            int i = 1;
            while (this.prefixToNamespaceMap.containsKey(str5)) {
                str5 = str2.substring(0, str2.length() - 1) + "_" + i + "_:";
                i++;
            }
            str2 = str5;
        }
        this.prefixToNamespaceMap.put(str2, str);
        this.namespaceToPrefixMap.put(str, str2);
        return str2;
    }

    @Override
    public synchronized String getNamespacePrefix(String str) {
        return (String) this.namespaceToPrefixMap.get(str);
    }

    @Override
    public synchronized String getNamespaceURI(String str) {
        if (str != null) {
            try {
                if (!str.endsWith(":")) {
                    str = str + ":";
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        return (String) this.prefixToNamespaceMap.get(str);
    }

    private void registerStandardNamespaces() throws XMPException {
        registerNamespace("http://www.w3.org/XML/1998/namespace", "xml");
        registerNamespace("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "rdf");
        registerNamespace("http://purl.org/dc/elements/1.1/", "dc");
        registerNamespace("http://iptc.org/std/Iptc4xmpCore/1.0/xmlns/", "Iptc4xmpCore");
        registerNamespace("adobe:ns:meta/", "x");
        registerNamespace("http://ns.adobe.com/iX/1.0/", "iX");
        registerNamespace("http://ns.adobe.com/xap/1.0/", "xmp");
        registerNamespace("http://ns.adobe.com/xap/1.0/rights/", "xmpRights");
        registerNamespace("http://ns.adobe.com/xap/1.0/mm/", "xmpMM");
        registerNamespace("http://ns.adobe.com/xap/1.0/bj/", "xmpBJ");
        registerNamespace("http://ns.adobe.com/xmp/note/", "xmpNote");
        registerNamespace("http://ns.adobe.com/pdf/1.3/", "pdf");
        registerNamespace("http://ns.adobe.com/pdfx/1.3/", "pdfx");
        registerNamespace("http://www.npes.org/pdfx/ns/id/", "pdfxid");
        registerNamespace("http://www.aiim.org/pdfa/ns/schema#", "pdfaSchema");
        registerNamespace("http://www.aiim.org/pdfa/ns/property#", "pdfaProperty");
        registerNamespace("http://www.aiim.org/pdfa/ns/type#", "pdfaType");
        registerNamespace("http://www.aiim.org/pdfa/ns/field#", "pdfaField");
        registerNamespace("http://www.aiim.org/pdfa/ns/id/", "pdfaid");
        registerNamespace("http://www.aiim.org/pdfa/ns/extension/", "pdfaExtension");
        registerNamespace("http://ns.adobe.com/photoshop/1.0/", "photoshop");
        registerNamespace("http://ns.adobe.com/album/1.0/", "album");
        registerNamespace("http://ns.adobe.com/exif/1.0/", "exif");
        registerNamespace("http://ns.adobe.com/exif/1.0/aux/", "aux");
        registerNamespace("http://ns.adobe.com/tiff/1.0/", "tiff");
        registerNamespace("http://ns.adobe.com/png/1.0/", "png");
        registerNamespace("http://ns.adobe.com/jpeg/1.0/", "jpeg");
        registerNamespace("http://ns.adobe.com/jp2k/1.0/", "jp2k");
        registerNamespace("http://ns.adobe.com/camera-raw-settings/1.0/", "crs");
        registerNamespace("http://ns.adobe.com/StockPhoto/1.0/", "bmsp");
        registerNamespace("http://ns.adobe.com/creatorAtom/1.0/", "creatorAtom");
        registerNamespace("http://ns.adobe.com/asf/1.0/", "asf");
        registerNamespace("http://ns.adobe.com/xmp/wav/1.0/", "wav");
        registerNamespace("http://ns.adobe.com/xmp/1.0/DynamicMedia/", "xmpDM");
        registerNamespace("http://ns.adobe.com/xmp/transient/1.0/", "xmpx");
        registerNamespace("http://ns.adobe.com/xap/1.0/t/", "xmpT");
        registerNamespace("http://ns.adobe.com/xap/1.0/t/pg/", "xmpTPg");
        registerNamespace("http://ns.adobe.com/xap/1.0/g/", "xmpG");
        registerNamespace("http://ns.adobe.com/xap/1.0/g/img/", "xmpGImg");
        registerNamespace("http://ns.adobe.com/xap/1.0/sType/Font#", "stFNT");
        registerNamespace("http://ns.adobe.com/xap/1.0/sType/Dimensions#", "stDim");
        registerNamespace("http://ns.adobe.com/xap/1.0/sType/ResourceEvent#", "stEvt");
        registerNamespace("http://ns.adobe.com/xap/1.0/sType/ResourceRef#", "stRef");
        registerNamespace("http://ns.adobe.com/xap/1.0/sType/Version#", "stVer");
        registerNamespace("http://ns.adobe.com/xap/1.0/sType/Job#", "stJob");
        registerNamespace("http://ns.adobe.com/xap/1.0/sType/ManifestItem#", "stMfs");
        registerNamespace("http://ns.adobe.com/xmp/Identifier/qual/1.0/", "xmpidq");
    }

    @Override
    public synchronized XMPAliasInfo findAlias(String str) {
        return (XMPAliasInfo) this.aliasMap.get(str);
    }

    synchronized void registerAlias(String str, String str2, final String str3, final String str4, AliasOptions aliasOptions) throws XMPException {
        AliasOptions aliasOptions2;
        ParameterAsserts.assertSchemaNS(str);
        ParameterAsserts.assertPropName(str2);
        ParameterAsserts.assertSchemaNS(str3);
        ParameterAsserts.assertPropName(str4);
        if (aliasOptions != null) {
            aliasOptions2 = new AliasOptions(XMPNodeUtils.verifySetOptions(aliasOptions.toPropertyOptions(), null).getOptions());
        } else {
            aliasOptions2 = new AliasOptions();
        }
        final AliasOptions aliasOptions3 = aliasOptions2;
        if (this.p.matcher(str2).find() || this.p.matcher(str4).find()) {
            throw new XMPException("Alias and actual property names must be simple", 102);
        }
        String namespacePrefix = getNamespacePrefix(str);
        final String namespacePrefix2 = getNamespacePrefix(str3);
        if (namespacePrefix == null) {
            throw new XMPException("Alias namespace is not registered", 101);
        }
        if (namespacePrefix2 == null) {
            throw new XMPException("Actual namespace is not registered", 101);
        }
        String str5 = namespacePrefix + str2;
        if (this.aliasMap.containsKey(str5)) {
            throw new XMPException("Alias is already existing", 4);
        }
        if (this.aliasMap.containsKey(namespacePrefix2 + str4)) {
            throw new XMPException("Actual property is already an alias, use the base property", 4);
        }
        this.aliasMap.put(str5, new XMPAliasInfo() {
            @Override
            public String getNamespace() {
                return str3;
            }

            @Override
            public String getPropName() {
                return str4;
            }

            @Override
            public AliasOptions getAliasForm() {
                return aliasOptions3;
            }

            public String toString() {
                return namespacePrefix2 + str4 + " NS(" + str3 + "), FORM (" + getAliasForm() + ")";
            }
        });
    }

    private void registerStandardAliases() throws XMPException {
        AliasOptions arrayOrdered = new AliasOptions().setArrayOrdered(true);
        AliasOptions arrayAltText = new AliasOptions().setArrayAltText(true);
        registerAlias("http://ns.adobe.com/xap/1.0/", "Author", "http://purl.org/dc/elements/1.1/", "creator", arrayOrdered);
        registerAlias("http://ns.adobe.com/xap/1.0/", "Authors", "http://purl.org/dc/elements/1.1/", "creator", null);
        registerAlias("http://ns.adobe.com/xap/1.0/", "Description", "http://purl.org/dc/elements/1.1/", "description", null);
        registerAlias("http://ns.adobe.com/xap/1.0/", "Format", "http://purl.org/dc/elements/1.1/", "format", null);
        registerAlias("http://ns.adobe.com/xap/1.0/", "Keywords", "http://purl.org/dc/elements/1.1/", "subject", null);
        registerAlias("http://ns.adobe.com/xap/1.0/", "Locale", "http://purl.org/dc/elements/1.1/", SchemaSymbols.ATTVAL_LANGUAGE, null);
        registerAlias("http://ns.adobe.com/xap/1.0/", "Title", "http://purl.org/dc/elements/1.1/", "title", null);
        registerAlias("http://ns.adobe.com/xap/1.0/rights/", "Copyright", "http://purl.org/dc/elements/1.1/", "rights", null);
        registerAlias("http://ns.adobe.com/pdf/1.3/", "Author", "http://purl.org/dc/elements/1.1/", "creator", arrayOrdered);
        registerAlias("http://ns.adobe.com/pdf/1.3/", "BaseURL", "http://ns.adobe.com/xap/1.0/", "BaseURL", null);
        registerAlias("http://ns.adobe.com/pdf/1.3/", "CreationDate", "http://ns.adobe.com/xap/1.0/", "CreateDate", null);
        registerAlias("http://ns.adobe.com/pdf/1.3/", "Creator", "http://ns.adobe.com/xap/1.0/", "CreatorTool", null);
        registerAlias("http://ns.adobe.com/pdf/1.3/", "ModDate", "http://ns.adobe.com/xap/1.0/", "ModifyDate", null);
        registerAlias("http://ns.adobe.com/pdf/1.3/", "Subject", "http://purl.org/dc/elements/1.1/", "description", arrayAltText);
        registerAlias("http://ns.adobe.com/pdf/1.3/", "Title", "http://purl.org/dc/elements/1.1/", "title", arrayAltText);
        registerAlias("http://ns.adobe.com/photoshop/1.0/", "Author", "http://purl.org/dc/elements/1.1/", "creator", arrayOrdered);
        registerAlias("http://ns.adobe.com/photoshop/1.0/", "Caption", "http://purl.org/dc/elements/1.1/", "description", arrayAltText);
        registerAlias("http://ns.adobe.com/photoshop/1.0/", "Copyright", "http://purl.org/dc/elements/1.1/", "rights", arrayAltText);
        registerAlias("http://ns.adobe.com/photoshop/1.0/", "Keywords", "http://purl.org/dc/elements/1.1/", "subject", null);
        registerAlias("http://ns.adobe.com/photoshop/1.0/", "Marked", "http://ns.adobe.com/xap/1.0/rights/", "Marked", null);
        registerAlias("http://ns.adobe.com/photoshop/1.0/", "Title", "http://purl.org/dc/elements/1.1/", "title", arrayAltText);
        registerAlias("http://ns.adobe.com/photoshop/1.0/", "WebStatement", "http://ns.adobe.com/xap/1.0/rights/", "WebStatement", null);
        registerAlias("http://ns.adobe.com/tiff/1.0/", "Artist", "http://purl.org/dc/elements/1.1/", "creator", arrayOrdered);
        registerAlias("http://ns.adobe.com/tiff/1.0/", "Copyright", "http://purl.org/dc/elements/1.1/", "rights", null);
        registerAlias("http://ns.adobe.com/tiff/1.0/", "DateTime", "http://ns.adobe.com/xap/1.0/", "ModifyDate", null);
        registerAlias("http://ns.adobe.com/tiff/1.0/", "ImageDescription", "http://purl.org/dc/elements/1.1/", "description", null);
        registerAlias("http://ns.adobe.com/tiff/1.0/", "Software", "http://ns.adobe.com/xap/1.0/", "CreatorTool", null);
        registerAlias("http://ns.adobe.com/png/1.0/", "Author", "http://purl.org/dc/elements/1.1/", "creator", arrayOrdered);
        registerAlias("http://ns.adobe.com/png/1.0/", "Copyright", "http://purl.org/dc/elements/1.1/", "rights", arrayAltText);
        registerAlias("http://ns.adobe.com/png/1.0/", "CreationTime", "http://ns.adobe.com/xap/1.0/", "CreateDate", null);
        registerAlias("http://ns.adobe.com/png/1.0/", "Description", "http://purl.org/dc/elements/1.1/", "description", arrayAltText);
        registerAlias("http://ns.adobe.com/png/1.0/", "ModificationTime", "http://ns.adobe.com/xap/1.0/", "ModifyDate", null);
        registerAlias("http://ns.adobe.com/png/1.0/", "Software", "http://ns.adobe.com/xap/1.0/", "CreatorTool", null);
        registerAlias("http://ns.adobe.com/png/1.0/", "Title", "http://purl.org/dc/elements/1.1/", "title", arrayAltText);
    }
}
