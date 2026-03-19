package org.apache.xalan.templates;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Vector;
import javax.xml.transform.TransformerException;
import org.apache.xalan.res.XSLTErrorResources;
import org.apache.xalan.templates.StylesheetRoot;
import org.apache.xalan.transformer.CountersTable;
import org.apache.xalan.transformer.DecimalToRoman;
import org.apache.xalan.transformer.TransformerImpl;
import org.apache.xml.dtm.DTM;
import org.apache.xml.utils.FastStringBuffer;
import org.apache.xml.utils.NodeVector;
import org.apache.xml.utils.PrefixResolver;
import org.apache.xml.utils.StringBufferPool;
import org.apache.xml.utils.res.CharArrayWrapper;
import org.apache.xml.utils.res.IntArrayWrapper;
import org.apache.xml.utils.res.LongArrayWrapper;
import org.apache.xml.utils.res.StringArrayWrapper;
import org.apache.xml.utils.res.XResourceBundle;
import org.apache.xpath.NodeSetDTM;
import org.apache.xpath.XPath;
import org.apache.xpath.XPathContext;
import org.apache.xpath.compiler.PsuedoNames;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class ElemNumber extends ElemTemplateElement {
    private static final DecimalToRoman[] m_romanConvertTable = {new DecimalToRoman(1000, "M", 900, "CM"), new DecimalToRoman(500, "D", 400, "CD"), new DecimalToRoman(100, "C", 90, "XC"), new DecimalToRoman(50, "L", 40, "XL"), new DecimalToRoman(10, "X", 9, "IX"), new DecimalToRoman(5, "V", 4, "IV"), new DecimalToRoman(1, "I", 1, "I")};
    static final long serialVersionUID = 8118472298274407610L;
    private CharArrayWrapper m_alphaCountTable = null;
    private XPath m_countMatchPattern = null;
    private XPath m_fromMatchPattern = null;
    private int m_level = 1;
    private XPath m_valueExpr = null;
    private AVT m_format_avt = null;
    private AVT m_lang_avt = null;
    private AVT m_lettervalue_avt = null;
    private AVT m_groupingSeparator_avt = null;
    private AVT m_groupingSize_avt = null;

    private class MyPrefixResolver implements PrefixResolver {
        DTM dtm;
        int handle;
        boolean handleNullPrefix;

        public MyPrefixResolver(Node node, DTM dtm, int i, boolean z) {
            this.dtm = dtm;
            this.handle = i;
            this.handleNullPrefix = z;
        }

        @Override
        public String getNamespaceForPrefix(String str) {
            return this.dtm.getNamespaceURI(this.handle);
        }

        @Override
        public String getNamespaceForPrefix(String str, Node node) {
            return getNamespaceForPrefix(str);
        }

        @Override
        public String getBaseIdentifier() {
            return ElemNumber.this.getBaseIdentifier();
        }

        @Override
        public boolean handlesNullPrefixes() {
            return this.handleNullPrefix;
        }
    }

    public void setCount(XPath xPath) {
        this.m_countMatchPattern = xPath;
    }

    public XPath getCount() {
        return this.m_countMatchPattern;
    }

    public void setFrom(XPath xPath) {
        this.m_fromMatchPattern = xPath;
    }

    public XPath getFrom() {
        return this.m_fromMatchPattern;
    }

    public void setLevel(int i) {
        this.m_level = i;
    }

    public int getLevel() {
        return this.m_level;
    }

    public void setValue(XPath xPath) {
        this.m_valueExpr = xPath;
    }

    public XPath getValue() {
        return this.m_valueExpr;
    }

    public void setFormat(AVT avt) {
        this.m_format_avt = avt;
    }

    public AVT getFormat() {
        return this.m_format_avt;
    }

    public void setLang(AVT avt) {
        this.m_lang_avt = avt;
    }

    public AVT getLang() {
        return this.m_lang_avt;
    }

    public void setLetterValue(AVT avt) {
        this.m_lettervalue_avt = avt;
    }

    public AVT getLetterValue() {
        return this.m_lettervalue_avt;
    }

    public void setGroupingSeparator(AVT avt) {
        this.m_groupingSeparator_avt = avt;
    }

    public AVT getGroupingSeparator() {
        return this.m_groupingSeparator_avt;
    }

    public void setGroupingSize(AVT avt) {
        this.m_groupingSize_avt = avt;
    }

    public AVT getGroupingSize() {
        return this.m_groupingSize_avt;
    }

    @Override
    public void compose(StylesheetRoot stylesheetRoot) throws TransformerException {
        super.compose(stylesheetRoot);
        StylesheetRoot.ComposeState composeState = stylesheetRoot.getComposeState();
        Vector variableNames = composeState.getVariableNames();
        if (this.m_countMatchPattern != null) {
            this.m_countMatchPattern.fixupVariables(variableNames, composeState.getGlobalsSize());
        }
        if (this.m_format_avt != null) {
            this.m_format_avt.fixupVariables(variableNames, composeState.getGlobalsSize());
        }
        if (this.m_fromMatchPattern != null) {
            this.m_fromMatchPattern.fixupVariables(variableNames, composeState.getGlobalsSize());
        }
        if (this.m_groupingSeparator_avt != null) {
            this.m_groupingSeparator_avt.fixupVariables(variableNames, composeState.getGlobalsSize());
        }
        if (this.m_groupingSize_avt != null) {
            this.m_groupingSize_avt.fixupVariables(variableNames, composeState.getGlobalsSize());
        }
        if (this.m_lang_avt != null) {
            this.m_lang_avt.fixupVariables(variableNames, composeState.getGlobalsSize());
        }
        if (this.m_lettervalue_avt != null) {
            this.m_lettervalue_avt.fixupVariables(variableNames, composeState.getGlobalsSize());
        }
        if (this.m_valueExpr != null) {
            this.m_valueExpr.fixupVariables(variableNames, composeState.getGlobalsSize());
        }
    }

    @Override
    public int getXSLToken() {
        return 35;
    }

    @Override
    public String getNodeName() {
        return "number";
    }

    @Override
    public void execute(TransformerImpl transformerImpl) throws TransformerException {
        String countString = getCountString(transformerImpl, transformerImpl.getXPathContext().getCurrentNode());
        try {
            transformerImpl.getResultTreeHandler().characters(countString.toCharArray(), 0, countString.length());
        } catch (SAXException e) {
            throw new TransformerException(e);
        }
    }

    @Override
    public ElemTemplateElement appendChild(ElemTemplateElement elemTemplateElement) {
        error(XSLTErrorResources.ER_CANNOT_ADD, new Object[]{elemTemplateElement.getNodeName(), getNodeName()});
        return null;
    }

    int findAncestor(XPathContext xPathContext, XPath xPath, XPath xPath2, int i, ElemNumber elemNumber) throws TransformerException {
        DTM dtm = xPathContext.getDTM(i);
        while (-1 != i && ((xPath == null || xPath.getMatchScore(xPathContext, i) == Double.NEGATIVE_INFINITY) && (xPath2 == null || xPath2.getMatchScore(xPathContext, i) == Double.NEGATIVE_INFINITY))) {
            i = dtm.getParent(i);
        }
        return i;
    }

    private int findPrecedingOrAncestorOrSelf(XPathContext xPathContext, XPath xPath, XPath xPath2, int i, ElemNumber elemNumber) throws TransformerException {
        DTM dtm = xPathContext.getDTM(i);
        while (-1 != i) {
            if (xPath != null && xPath.getMatchScore(xPathContext, i) != Double.NEGATIVE_INFINITY) {
                return -1;
            }
            if (xPath2 == null || xPath2.getMatchScore(xPathContext, i) == Double.NEGATIVE_INFINITY) {
                int previousSibling = dtm.getPreviousSibling(i);
                if (-1 == previousSibling) {
                    i = dtm.getParent(i);
                } else {
                    i = dtm.getLastChild(previousSibling);
                    if (i == -1) {
                        i = previousSibling;
                    }
                }
            } else {
                return i;
            }
        }
        return i;
    }

    XPath getCountMatchPattern(XPathContext xPathContext, int i) throws TransformerException {
        MyPrefixResolver myPrefixResolver;
        XPath xPath;
        XPath xPath2 = this.m_countMatchPattern;
        DTM dtm = xPathContext.getDTM(i);
        if (xPath2 == null) {
            switch (dtm.getNodeType(i)) {
                case 1:
                    if (dtm.getNamespaceURI(i) == null) {
                        myPrefixResolver = new MyPrefixResolver(dtm.getNode(i), dtm, i, false);
                    } else {
                        myPrefixResolver = new MyPrefixResolver(dtm.getNode(i), dtm, i, true);
                    }
                    xPath = new XPath(dtm.getNodeName(i), this, myPrefixResolver, 1, xPathContext.getErrorListener());
                    break;
                case 2:
                    xPath = new XPath("@" + dtm.getNodeName(i), this, this, 1, xPathContext.getErrorListener());
                    break;
                case 3:
                case 4:
                    return new XPath("text()", this, this, 1, xPathContext.getErrorListener());
                case 5:
                case 6:
                default:
                    xPath2 = null;
                    break;
                case 7:
                    xPath = new XPath("pi(" + dtm.getNodeName(i) + ")", this, this, 1, xPathContext.getErrorListener());
                    break;
                case 8:
                    return new XPath("comment()", this, this, 1, xPathContext.getErrorListener());
                case 9:
                    return new XPath(PsuedoNames.PSEUDONAME_ROOT, this, this, 1, xPathContext.getErrorListener());
            }
            return xPath;
        }
        return xPath2;
    }

    String getCountString(TransformerImpl transformerImpl, int i) throws TransformerException {
        long[] jArr;
        XPathContext xPathContext = transformerImpl.getXPathContext();
        CountersTable countersTable = transformerImpl.getCountersTable();
        if (this.m_valueExpr != null) {
            double dFloor = Math.floor(this.m_valueExpr.execute(xPathContext, i, this).num() + 0.5d);
            if (Double.isNaN(dFloor)) {
                return "NaN";
            }
            if (dFloor < XPath.MATCH_SCORE_QNAME && Double.isInfinite(dFloor)) {
                return "-Infinity";
            }
            if (Double.isInfinite(dFloor)) {
                return Constants.ATTRVAL_INFINITY;
            }
            if (dFloor == XPath.MATCH_SCORE_QNAME) {
                return "0";
            }
            jArr = new long[]{(long) dFloor};
        } else if (3 == this.m_level) {
            jArr = new long[]{countersTable.countNode(xPathContext, this, i)};
        } else {
            int size = getMatchingAncestors(xPathContext, i, 1 == this.m_level).size() - 1;
            if (size >= 0) {
                long[] jArr2 = new long[size + 1];
                for (int i2 = size; i2 >= 0; i2--) {
                    jArr2[size - i2] = countersTable.countNode(xPathContext, this, r2.elementAt(i2));
                }
                jArr = jArr2;
            } else {
                jArr = null;
            }
        }
        return jArr != null ? formatNumberList(transformerImpl, jArr, i) : "";
    }

    public int getPreviousNode(XPathContext xPathContext, int i) throws TransformerException {
        XPath countMatchPattern = getCountMatchPattern(xPathContext, i);
        DTM dtm = xPathContext.getDTM(i);
        if (3 == this.m_level) {
            XPath xPath = this.m_fromMatchPattern;
            while (-1 != i) {
                int previousSibling = dtm.getPreviousSibling(i);
                if (-1 == previousSibling) {
                    i = dtm.getParent(i);
                    if (-1 != i && ((xPath != null && xPath.getMatchScore(xPathContext, i) != Double.NEGATIVE_INFINITY) || dtm.getNodeType(i) == 9)) {
                        return -1;
                    }
                } else {
                    while (true) {
                        i = previousSibling;
                        while (-1 != previousSibling) {
                            previousSibling = dtm.getLastChild(i);
                            if (-1 != previousSibling) {
                                break;
                            }
                        }
                    }
                }
                if (-1 != i && (countMatchPattern == null || countMatchPattern.getMatchScore(xPathContext, i) != Double.NEGATIVE_INFINITY)) {
                    return i;
                }
            }
            return i;
        }
        while (-1 != i) {
            i = dtm.getPreviousSibling(i);
            if (-1 != i && (countMatchPattern == null || countMatchPattern.getMatchScore(xPathContext, i) != Double.NEGATIVE_INFINITY)) {
                return i;
            }
        }
        return i;
    }

    public int getTargetNode(XPathContext xPathContext, int i) throws TransformerException {
        XPath countMatchPattern = getCountMatchPattern(xPathContext, i);
        if (3 == this.m_level) {
            return findPrecedingOrAncestorOrSelf(xPathContext, this.m_fromMatchPattern, countMatchPattern, i, this);
        }
        return findAncestor(xPathContext, this.m_fromMatchPattern, countMatchPattern, i, this);
    }

    NodeVector getMatchingAncestors(XPathContext xPathContext, int i, boolean z) throws TransformerException {
        NodeSetDTM nodeSetDTM = new NodeSetDTM(xPathContext.getDTMManager());
        XPath countMatchPattern = getCountMatchPattern(xPathContext, i);
        DTM dtm = xPathContext.getDTM(i);
        while (-1 != i && (this.m_fromMatchPattern == null || this.m_fromMatchPattern.getMatchScore(xPathContext, i) == Double.NEGATIVE_INFINITY || z)) {
            if (countMatchPattern == null) {
                System.out.println("Programmers error! countMatchPattern should never be null!");
            }
            if (countMatchPattern.getMatchScore(xPathContext, i) != Double.NEGATIVE_INFINITY) {
                nodeSetDTM.addElement(i);
                if (z) {
                    break;
                }
            }
            i = dtm.getParent(i);
        }
        return nodeSetDTM;
    }

    Locale getLocale(TransformerImpl transformerImpl, int i) throws TransformerException {
        if (this.m_lang_avt != null) {
            String strEvaluate = this.m_lang_avt.evaluate(transformerImpl.getXPathContext(), i, this);
            if (strEvaluate != null) {
                return new Locale(strEvaluate.toUpperCase(), "");
            }
            return null;
        }
        return Locale.getDefault();
    }

    private DecimalFormat getNumberFormatter(TransformerImpl transformerImpl, int i) throws TransformerException {
        String strEvaluate;
        String strEvaluate2;
        Locale locale = (Locale) getLocale(transformerImpl, i).clone();
        DecimalFormat decimalFormat = null;
        if (this.m_groupingSeparator_avt != null) {
            strEvaluate = this.m_groupingSeparator_avt.evaluate(transformerImpl.getXPathContext(), i, this);
        } else {
            strEvaluate = null;
        }
        if (strEvaluate != null && !this.m_groupingSeparator_avt.isSimple() && strEvaluate.length() != 1) {
            transformerImpl.getMsgMgr().warn(this, XSLTErrorResources.WG_ILLEGAL_ATTRIBUTE_VALUE, new Object[]{"name", this.m_groupingSeparator_avt.getName()});
        }
        if (this.m_groupingSize_avt != null) {
            strEvaluate2 = this.m_groupingSize_avt.evaluate(transformerImpl.getXPathContext(), i, this);
        } else {
            strEvaluate2 = null;
        }
        if (strEvaluate == null || strEvaluate2 == null || strEvaluate.length() <= 0) {
            return null;
        }
        try {
            DecimalFormat decimalFormat2 = (DecimalFormat) NumberFormat.getNumberInstance(locale);
            try {
                decimalFormat2.setGroupingSize(Integer.valueOf(strEvaluate2).intValue());
                DecimalFormatSymbols decimalFormatSymbols = decimalFormat2.getDecimalFormatSymbols();
                decimalFormatSymbols.setGroupingSeparator(strEvaluate.charAt(0));
                decimalFormat2.setDecimalFormatSymbols(decimalFormatSymbols);
                decimalFormat2.setGroupingUsed(true);
                return decimalFormat2;
            } catch (NumberFormatException e) {
                decimalFormat = decimalFormat2;
                decimalFormat.setGroupingUsed(false);
                return decimalFormat;
            }
        } catch (NumberFormatException e2) {
        }
    }

    String formatNumberList(TransformerImpl transformerImpl, long[] jArr, int i) throws TransformerException {
        int i2;
        String strEvaluate;
        String str;
        String str2;
        int i3;
        String str3;
        char cCharAt;
        FastStringBuffer fastStringBuffer = StringBufferPool.get();
        try {
            int length = jArr.length;
            String str4 = Constants.ATTRVAL_THIS;
            String str5 = null;
            if (this.m_format_avt != null) {
                i2 = i;
                strEvaluate = this.m_format_avt.evaluate(transformerImpl.getXPathContext(), i2, this);
            } else {
                i2 = i;
                strEvaluate = null;
            }
            if (strEvaluate == null) {
                strEvaluate = "1";
            }
            NumberFormatStringTokenizer numberFormatStringTokenizer = new NumberFormatStringTokenizer(strEvaluate);
            char cCharAt2 = '1';
            String str6 = null;
            int length2 = 1;
            boolean z = true;
            int i4 = 0;
            while (i4 < length) {
                if (numberFormatStringTokenizer.hasMoreTokens()) {
                    String strNextToken = numberFormatStringTokenizer.nextToken();
                    if (Character.isLetterOrDigit(strNextToken.charAt(strNextToken.length() - 1))) {
                        length2 = strNextToken.length();
                        cCharAt2 = strNextToken.charAt(length2 - 1);
                        str = str6;
                        str2 = str4;
                        i3 = length2;
                        str3 = str5;
                        cCharAt = cCharAt2;
                    } else {
                        if (numberFormatStringTokenizer.isLetterOrDigitAhead()) {
                            while (numberFormatStringTokenizer.nextIsSep()) {
                                strNextToken = strNextToken + numberFormatStringTokenizer.nextToken();
                            }
                            if (!z) {
                                str4 = strNextToken;
                            }
                            String strNextToken2 = numberFormatStringTokenizer.nextToken();
                            int length3 = strNextToken2.length();
                            cCharAt = strNextToken2.charAt(length3 - 1);
                            i3 = length3;
                            str3 = str5;
                            str = strNextToken;
                        } else {
                            while (numberFormatStringTokenizer.hasMoreTokens()) {
                                strNextToken = strNextToken + numberFormatStringTokenizer.nextToken();
                            }
                            str = str6;
                            i3 = length2;
                            cCharAt = cCharAt2;
                            str3 = strNextToken;
                        }
                        str2 = str4;
                    }
                } else {
                    str = str6;
                    str2 = str4;
                    i3 = length2;
                    str3 = str5;
                    cCharAt = cCharAt2;
                }
                if (str != null && z) {
                    fastStringBuffer.append(str);
                } else if (str2 != null && !z) {
                    fastStringBuffer.append(str2);
                }
                getFormattedNumber(transformerImpl, i2, cCharAt, i3, jArr[i4], fastStringBuffer);
                i4++;
                str6 = str;
                length2 = i3;
                cCharAt2 = cCharAt;
                str5 = str3;
                str4 = str2;
                z = false;
            }
            while (numberFormatStringTokenizer.isLetterOrDigitAhead()) {
                numberFormatStringTokenizer.nextToken();
            }
            if (str5 != null) {
                fastStringBuffer.append(str5);
            }
            while (numberFormatStringTokenizer.hasMoreTokens()) {
                fastStringBuffer.append(numberFormatStringTokenizer.nextToken());
            }
            return fastStringBuffer.toString();
        } finally {
            StringBufferPool.free(fastStringBuffer);
        }
    }

    private void getFormattedNumber(TransformerImpl transformerImpl, int i, char c, int i2, long j, FastStringBuffer fastStringBuffer) throws TransformerException {
        String strEvaluate;
        if (this.m_lettervalue_avt != null) {
            strEvaluate = this.m_lettervalue_avt.evaluate(transformerImpl.getXPathContext(), i, this);
        } else {
            strEvaluate = null;
        }
        switch (c) {
            case 'A':
                if (this.m_alphaCountTable == null) {
                    this.m_alphaCountTable = (CharArrayWrapper) XResourceBundle.loadResourceBundle(XResourceBundle.LANG_BUNDLE_NAME, getLocale(transformerImpl, i)).getObject(XResourceBundle.LANG_ALPHABET);
                }
                int2alphaCount(j, this.m_alphaCountTable, fastStringBuffer);
                return;
            case Constants.ELEMNAME_VARIABLE:
                fastStringBuffer.append(long2roman(j, true));
                return;
            case 'a':
                if (this.m_alphaCountTable == null) {
                    this.m_alphaCountTable = (CharArrayWrapper) XResourceBundle.loadResourceBundle(XResourceBundle.LANG_BUNDLE_NAME, getLocale(transformerImpl, i)).getObject(XResourceBundle.LANG_ALPHABET);
                }
                FastStringBuffer fastStringBuffer2 = StringBufferPool.get();
                try {
                    int2alphaCount(j, this.m_alphaCountTable, fastStringBuffer2);
                    fastStringBuffer.append(fastStringBuffer2.toString().toLowerCase(getLocale(transformerImpl, i)));
                    return;
                } finally {
                    StringBufferPool.free(fastStringBuffer2);
                }
            case 'i':
                fastStringBuffer.append(long2roman(j, true).toLowerCase(getLocale(transformerImpl, i)));
                return;
            case 945:
                XResourceBundle xResourceBundleLoadResourceBundle = XResourceBundle.loadResourceBundle(XResourceBundle.LANG_BUNDLE_NAME, new Locale("el", ""));
                if (strEvaluate != null && strEvaluate.equals(Constants.ATTRVAL_TRADITIONAL)) {
                    fastStringBuffer.append(tradAlphaCount(j, xResourceBundleLoadResourceBundle));
                    return;
                } else {
                    int2alphaCount(j, (CharArrayWrapper) xResourceBundleLoadResourceBundle.getObject(XResourceBundle.LANG_ALPHABET), fastStringBuffer);
                    return;
                }
            case 1072:
                XResourceBundle xResourceBundleLoadResourceBundle2 = XResourceBundle.loadResourceBundle(XResourceBundle.LANG_BUNDLE_NAME, new Locale("cy", ""));
                if (strEvaluate != null && strEvaluate.equals(Constants.ATTRVAL_TRADITIONAL)) {
                    fastStringBuffer.append(tradAlphaCount(j, xResourceBundleLoadResourceBundle2));
                    return;
                } else {
                    int2alphaCount(j, (CharArrayWrapper) xResourceBundleLoadResourceBundle2.getObject(XResourceBundle.LANG_ALPHABET), fastStringBuffer);
                    return;
                }
            case 1488:
                XResourceBundle xResourceBundleLoadResourceBundle3 = XResourceBundle.loadResourceBundle(XResourceBundle.LANG_BUNDLE_NAME, new Locale("he", ""));
                if (strEvaluate != null && strEvaluate.equals(Constants.ATTRVAL_TRADITIONAL)) {
                    fastStringBuffer.append(tradAlphaCount(j, xResourceBundleLoadResourceBundle3));
                    return;
                } else {
                    int2alphaCount(j, (CharArrayWrapper) xResourceBundleLoadResourceBundle3.getObject(XResourceBundle.LANG_ALPHABET), fastStringBuffer);
                    return;
                }
            case 3665:
                XResourceBundle xResourceBundleLoadResourceBundle4 = XResourceBundle.loadResourceBundle(XResourceBundle.LANG_BUNDLE_NAME, new Locale("th", ""));
                if (strEvaluate != null && strEvaluate.equals(Constants.ATTRVAL_TRADITIONAL)) {
                    fastStringBuffer.append(tradAlphaCount(j, xResourceBundleLoadResourceBundle4));
                    return;
                } else {
                    int2alphaCount(j, (CharArrayWrapper) xResourceBundleLoadResourceBundle4.getObject(XResourceBundle.LANG_ALPHABET), fastStringBuffer);
                    return;
                }
            case 4304:
                XResourceBundle xResourceBundleLoadResourceBundle5 = XResourceBundle.loadResourceBundle(XResourceBundle.LANG_BUNDLE_NAME, new Locale("ka", ""));
                if (strEvaluate != null && strEvaluate.equals(Constants.ATTRVAL_TRADITIONAL)) {
                    fastStringBuffer.append(tradAlphaCount(j, xResourceBundleLoadResourceBundle5));
                    return;
                } else {
                    int2alphaCount(j, (CharArrayWrapper) xResourceBundleLoadResourceBundle5.getObject(XResourceBundle.LANG_ALPHABET), fastStringBuffer);
                    return;
                }
            case 12354:
                XResourceBundle xResourceBundleLoadResourceBundle6 = XResourceBundle.loadResourceBundle(XResourceBundle.LANG_BUNDLE_NAME, new Locale("ja", "JP", "HA"));
                if (strEvaluate != null && strEvaluate.equals(Constants.ATTRVAL_TRADITIONAL)) {
                    fastStringBuffer.append(tradAlphaCount(j, xResourceBundleLoadResourceBundle6));
                    return;
                } else {
                    fastStringBuffer.append(int2singlealphaCount(j, (CharArrayWrapper) xResourceBundleLoadResourceBundle6.getObject(XResourceBundle.LANG_ALPHABET)));
                    return;
                }
            case 12356:
                XResourceBundle xResourceBundleLoadResourceBundle7 = XResourceBundle.loadResourceBundle(XResourceBundle.LANG_BUNDLE_NAME, new Locale("ja", "JP", "HI"));
                if (strEvaluate != null && strEvaluate.equals(Constants.ATTRVAL_TRADITIONAL)) {
                    fastStringBuffer.append(tradAlphaCount(j, xResourceBundleLoadResourceBundle7));
                    return;
                } else {
                    fastStringBuffer.append(int2singlealphaCount(j, (CharArrayWrapper) xResourceBundleLoadResourceBundle7.getObject(XResourceBundle.LANG_ALPHABET)));
                    return;
                }
            case 12450:
                XResourceBundle xResourceBundleLoadResourceBundle8 = XResourceBundle.loadResourceBundle(XResourceBundle.LANG_BUNDLE_NAME, new Locale("ja", "JP", "A"));
                if (strEvaluate != null && strEvaluate.equals(Constants.ATTRVAL_TRADITIONAL)) {
                    fastStringBuffer.append(tradAlphaCount(j, xResourceBundleLoadResourceBundle8));
                    return;
                } else {
                    fastStringBuffer.append(int2singlealphaCount(j, (CharArrayWrapper) xResourceBundleLoadResourceBundle8.getObject(XResourceBundle.LANG_ALPHABET)));
                    return;
                }
            case 12452:
                XResourceBundle xResourceBundleLoadResourceBundle9 = XResourceBundle.loadResourceBundle(XResourceBundle.LANG_BUNDLE_NAME, new Locale("ja", "JP", "I"));
                if (strEvaluate != null && strEvaluate.equals(Constants.ATTRVAL_TRADITIONAL)) {
                    fastStringBuffer.append(tradAlphaCount(j, xResourceBundleLoadResourceBundle9));
                    return;
                } else {
                    fastStringBuffer.append(int2singlealphaCount(j, (CharArrayWrapper) xResourceBundleLoadResourceBundle9.getObject(XResourceBundle.LANG_ALPHABET)));
                    return;
                }
            case 19968:
                XResourceBundle xResourceBundleLoadResourceBundle10 = XResourceBundle.loadResourceBundle(XResourceBundle.LANG_BUNDLE_NAME, new Locale("zh", "CN"));
                if (strEvaluate != null && strEvaluate.equals(Constants.ATTRVAL_TRADITIONAL)) {
                    fastStringBuffer.append(tradAlphaCount(j, xResourceBundleLoadResourceBundle10));
                    return;
                } else {
                    int2alphaCount(j, (CharArrayWrapper) xResourceBundleLoadResourceBundle10.getObject(XResourceBundle.LANG_ALPHABET), fastStringBuffer);
                    return;
                }
            case 22777:
                XResourceBundle xResourceBundleLoadResourceBundle11 = XResourceBundle.loadResourceBundle(XResourceBundle.LANG_BUNDLE_NAME, new Locale("zh", "TW"));
                if (strEvaluate != null && strEvaluate.equals(Constants.ATTRVAL_TRADITIONAL)) {
                    fastStringBuffer.append(tradAlphaCount(j, xResourceBundleLoadResourceBundle11));
                    return;
                } else {
                    int2alphaCount(j, (CharArrayWrapper) xResourceBundleLoadResourceBundle11.getObject(XResourceBundle.LANG_ALPHABET), fastStringBuffer);
                    return;
                }
            default:
                DecimalFormat numberFormatter = getNumberFormatter(transformerImpl, i);
                String strValueOf = numberFormatter == null ? String.valueOf(0) : numberFormatter.format(0L);
                String strValueOf2 = numberFormatter == null ? String.valueOf(j) : numberFormatter.format(j);
                int length = i2 - strValueOf2.length();
                for (int i3 = 0; i3 < length; i3++) {
                    fastStringBuffer.append(strValueOf);
                }
                fastStringBuffer.append(strValueOf2);
                return;
        }
    }

    String getZeroString() {
        return "0";
    }

    protected String int2singlealphaCount(long j, CharArrayWrapper charArrayWrapper) {
        if (j > charArrayWrapper.getLength()) {
            return getZeroString();
        }
        return new Character(charArrayWrapper.getChar(((int) j) - 1)).toString();
    }

    protected void int2alphaCount(long j, CharArrayWrapper charArrayWrapper, FastStringBuffer fastStringBuffer) {
        int i;
        int length = charArrayWrapper.getLength();
        char[] cArr = new char[length];
        int i2 = 0;
        while (true) {
            i = length - 1;
            if (i2 >= i) {
                break;
            }
            int i3 = i2 + 1;
            cArr[i3] = charArrayWrapper.getChar(i2);
            i2 = i3;
        }
        cArr[0] = charArrayWrapper.getChar(i2);
        char[] cArr2 = new char[100];
        long j2 = j;
        int length2 = cArr2.length - 1;
        int i4 = 1;
        long j3 = 0;
        while (true) {
            if (i4 == 0 || (j3 != 0 && i4 == i)) {
                j3 = i;
            } else {
                j3 = 0;
            }
            i4 = ((int) (j2 + j3)) % length;
            j2 /= (long) length;
            if (i4 == 0 && j2 == 0) {
                break;
            }
            int i5 = length2 - 1;
            cArr2[length2] = cArr[i4];
            if (j2 <= 0) {
                length2 = i5;
                break;
            }
            length2 = i5;
        }
        fastStringBuffer.append(cArr2, length2 + 1, (cArr2.length - length2) - 1);
    }

    protected String tradAlphaCount(long j, XResourceBundle xResourceBundle) {
        long j2;
        int i;
        long j3;
        int i2;
        if (j > Long.MAX_VALUE) {
            error(XSLTErrorResources.ER_NUMBER_TOO_BIG);
            return "#error";
        }
        char[] cArr = new char[100];
        IntArrayWrapper intArrayWrapper = (IntArrayWrapper) xResourceBundle.getObject(XResourceBundle.LANG_NUMBERGROUPS);
        StringArrayWrapper stringArrayWrapper = (StringArrayWrapper) xResourceBundle.getObject(XResourceBundle.LANG_NUM_TABLES);
        int i3 = 0;
        if (xResourceBundle.getString(XResourceBundle.LANG_NUMBERING).equals(XResourceBundle.LANG_MULT_ADD)) {
            String string = xResourceBundle.getString(XResourceBundle.MULT_ORDER);
            LongArrayWrapper longArrayWrapper = (LongArrayWrapper) xResourceBundle.getObject(XResourceBundle.LANG_MULTIPLIER);
            CharArrayWrapper charArrayWrapper = (CharArrayWrapper) xResourceBundle.getObject("zero");
            int i4 = 0;
            while (i4 < longArrayWrapper.getLength() && j < longArrayWrapper.getLong(i4)) {
                i4++;
            }
            j2 = j;
            i = 0;
            while (i4 < longArrayWrapper.getLength()) {
                if (j2 < longArrayWrapper.getLong(i4)) {
                    if (charArrayWrapper.getLength() == 0) {
                        i4++;
                    } else {
                        if (cArr[i - 1] != charArrayWrapper.getChar(i3)) {
                            i2 = i + 1;
                            cArr[i] = charArrayWrapper.getChar(i3);
                        } else {
                            i2 = i;
                        }
                        i4++;
                        i = i2;
                    }
                } else if (j2 >= longArrayWrapper.getLong(i4)) {
                    long j4 = j2 / longArrayWrapper.getLong(i4);
                    long j5 = j2 % longArrayWrapper.getLong(i4);
                    int i5 = 0;
                    while (true) {
                        if (i5 >= intArrayWrapper.getLength()) {
                            j3 = j5;
                            break;
                        }
                        if (j4 / ((long) intArrayWrapper.getInt(i5)) > 0) {
                            CharArrayWrapper charArrayWrapper2 = (CharArrayWrapper) xResourceBundle.getObject(stringArrayWrapper.getString(i5));
                            char[] cArr2 = new char[charArrayWrapper2.getLength() + 1];
                            j3 = j5;
                            int i6 = 0;
                            while (i6 < charArrayWrapper2.getLength()) {
                                int i7 = i6 + 1;
                                cArr2[i7] = charArrayWrapper2.getChar(i6);
                                i6 = i7;
                            }
                            cArr2[0] = charArrayWrapper2.getChar(i6 - 1);
                            int i8 = ((int) j4) / intArrayWrapper.getInt(i5);
                            if (i8 != 0 || j4 != 0) {
                                char c = ((CharArrayWrapper) xResourceBundle.getObject(XResourceBundle.LANG_MULTIPLIER_CHAR)).getChar(i4);
                                if (i8 < cArr2.length) {
                                    if (string.equals(XResourceBundle.MULT_PRECEDES)) {
                                        int i9 = i + 1;
                                        cArr[i] = c;
                                        i = i9 + 1;
                                        cArr[i9] = cArr2[i8];
                                    } else {
                                        if (i8 != 1 || i4 != longArrayWrapper.getLength() - 1) {
                                            cArr[i] = cArr2[i8];
                                            i++;
                                        }
                                        cArr[i] = c;
                                        i++;
                                    }
                                } else {
                                    return "#error";
                                }
                            }
                        } else {
                            i5++;
                        }
                    }
                    i4++;
                    j2 = j3;
                }
                if (i4 >= longArrayWrapper.getLength()) {
                    break;
                }
                i3 = 0;
            }
        } else {
            j2 = j;
            i = 0;
        }
        long j6 = j2;
        int i10 = 0;
        while (i10 < intArrayWrapper.getLength()) {
            if (j6 / ((long) intArrayWrapper.getInt(i10)) > 0) {
                CharArrayWrapper charArrayWrapper3 = (CharArrayWrapper) xResourceBundle.getObject(stringArrayWrapper.getString(i10));
                char[] cArr3 = new char[charArrayWrapper3.getLength() + 1];
                int i11 = 0;
                while (i11 < charArrayWrapper3.getLength()) {
                    int i12 = i11 + 1;
                    cArr3[i12] = charArrayWrapper3.getChar(i11);
                    i11 = i12;
                }
                cArr3[0] = charArrayWrapper3.getChar(i11 - 1);
                int i13 = ((int) j6) / intArrayWrapper.getInt(i10);
                j6 %= (long) intArrayWrapper.getInt(i10);
                if (i13 == 0 && j6 == 0) {
                    break;
                }
                if (i13 >= cArr3.length) {
                    return "#error";
                }
                cArr[i] = cArr3[i13];
                i10++;
                i++;
            } else {
                i10++;
            }
        }
        return new String(cArr, 0, i);
    }

    protected String long2roman(long j, boolean z) {
        if (j <= 0) {
            return getZeroString();
        }
        String str = "";
        int i = 0;
        if (j > 3999) {
            return "#error";
        }
        while (true) {
            if (j >= m_romanConvertTable[i].m_postValue) {
                str = str + m_romanConvertTable[i].m_postLetter;
                j -= m_romanConvertTable[i].m_postValue;
            } else {
                if (z && j >= m_romanConvertTable[i].m_preValue) {
                    str = str + m_romanConvertTable[i].m_preLetter;
                    j -= m_romanConvertTable[i].m_preValue;
                }
                i++;
                if (j <= 0) {
                    return str;
                }
            }
        }
    }

    @Override
    public void callChildVisitors(XSLTVisitor xSLTVisitor, boolean z) {
        if (z) {
            if (this.m_countMatchPattern != null) {
                this.m_countMatchPattern.getExpression().callVisitors(this.m_countMatchPattern, xSLTVisitor);
            }
            if (this.m_fromMatchPattern != null) {
                this.m_fromMatchPattern.getExpression().callVisitors(this.m_fromMatchPattern, xSLTVisitor);
            }
            if (this.m_valueExpr != null) {
                this.m_valueExpr.getExpression().callVisitors(this.m_valueExpr, xSLTVisitor);
            }
            if (this.m_format_avt != null) {
                this.m_format_avt.callVisitors(xSLTVisitor);
            }
            if (this.m_groupingSeparator_avt != null) {
                this.m_groupingSeparator_avt.callVisitors(xSLTVisitor);
            }
            if (this.m_groupingSize_avt != null) {
                this.m_groupingSize_avt.callVisitors(xSLTVisitor);
            }
            if (this.m_lang_avt != null) {
                this.m_lang_avt.callVisitors(xSLTVisitor);
            }
            if (this.m_lettervalue_avt != null) {
                this.m_lettervalue_avt.callVisitors(xSLTVisitor);
            }
        }
        super.callChildVisitors(xSLTVisitor, z);
    }

    class NumberFormatStringTokenizer {
        private int currentPosition;
        private int maxPosition;
        private String str;

        public NumberFormatStringTokenizer(String str) {
            this.str = str;
            this.maxPosition = str.length();
        }

        public void reset() {
            this.currentPosition = 0;
        }

        public String nextToken() {
            if (this.currentPosition >= this.maxPosition) {
                throw new NoSuchElementException();
            }
            int i = this.currentPosition;
            while (this.currentPosition < this.maxPosition && Character.isLetterOrDigit(this.str.charAt(this.currentPosition))) {
                this.currentPosition++;
            }
            if (i == this.currentPosition && !Character.isLetterOrDigit(this.str.charAt(this.currentPosition))) {
                this.currentPosition++;
            }
            return this.str.substring(i, this.currentPosition);
        }

        public boolean isLetterOrDigitAhead() {
            for (int i = this.currentPosition; i < this.maxPosition; i++) {
                if (Character.isLetterOrDigit(this.str.charAt(i))) {
                    return true;
                }
            }
            return false;
        }

        public boolean nextIsSep() {
            if (Character.isLetterOrDigit(this.str.charAt(this.currentPosition))) {
                return false;
            }
            return true;
        }

        public boolean hasMoreTokens() {
            return this.currentPosition < this.maxPosition;
        }

        public int countTokens() {
            int i = this.currentPosition;
            int i2 = 0;
            while (i < this.maxPosition) {
                int i3 = i;
                while (i3 < this.maxPosition && Character.isLetterOrDigit(this.str.charAt(i3))) {
                    i3++;
                }
                if (i == i3 && !Character.isLetterOrDigit(this.str.charAt(i3))) {
                    i3++;
                }
                i = i3;
                i2++;
            }
            return i2;
        }
    }
}
