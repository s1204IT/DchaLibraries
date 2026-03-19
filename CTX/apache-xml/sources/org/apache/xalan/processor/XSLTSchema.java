package org.apache.xalan.processor;

import java.util.HashMap;
import org.apache.xalan.templates.Constants;
import org.apache.xalan.templates.ElemApplyImport;
import org.apache.xalan.templates.ElemApplyTemplates;
import org.apache.xalan.templates.ElemAttribute;
import org.apache.xalan.templates.ElemCallTemplate;
import org.apache.xalan.templates.ElemChoose;
import org.apache.xalan.templates.ElemComment;
import org.apache.xalan.templates.ElemCopy;
import org.apache.xalan.templates.ElemCopyOf;
import org.apache.xalan.templates.ElemElement;
import org.apache.xalan.templates.ElemExsltFuncResult;
import org.apache.xalan.templates.ElemExsltFunction;
import org.apache.xalan.templates.ElemExtensionDecl;
import org.apache.xalan.templates.ElemExtensionScript;
import org.apache.xalan.templates.ElemFallback;
import org.apache.xalan.templates.ElemForEach;
import org.apache.xalan.templates.ElemIf;
import org.apache.xalan.templates.ElemLiteralResult;
import org.apache.xalan.templates.ElemMessage;
import org.apache.xalan.templates.ElemNumber;
import org.apache.xalan.templates.ElemOtherwise;
import org.apache.xalan.templates.ElemPI;
import org.apache.xalan.templates.ElemParam;
import org.apache.xalan.templates.ElemSort;
import org.apache.xalan.templates.ElemTemplate;
import org.apache.xalan.templates.ElemText;
import org.apache.xalan.templates.ElemTextLiteral;
import org.apache.xalan.templates.ElemUnknown;
import org.apache.xalan.templates.ElemValueOf;
import org.apache.xalan.templates.ElemVariable;
import org.apache.xalan.templates.ElemWhen;
import org.apache.xalan.templates.ElemWithParam;
import org.apache.xml.utils.QName;

public class XSLTSchema extends XSLTElementDef {
    private HashMap m_availElems = new HashMap();

    XSLTSchema() {
        build();
    }

    void build() {
        XSLTAttributeDef xSLTAttributeDef = new XSLTAttributeDef((String) null, Constants.ATTRNAME_HREF, 2, true, false, 1);
        XSLTAttributeDef xSLTAttributeDef2 = new XSLTAttributeDef((String) null, Constants.ATTRNAME_ELEMENTS, 12, true, false, 1);
        XSLTAttributeDef xSLTAttributeDef3 = new XSLTAttributeDef((String) null, Constants.ATTRNAME_OUTPUT_METHOD, 9, false, false, 1);
        XSLTAttributeDef xSLTAttributeDef4 = new XSLTAttributeDef((String) null, "version", 13, false, false, 1);
        XSLTAttributeDef xSLTAttributeDef5 = new XSLTAttributeDef((String) null, "encoding", 1, false, false, 1);
        XSLTAttributeDef xSLTAttributeDef6 = new XSLTAttributeDef((String) null, "omit-xml-declaration", 8, false, false, 1);
        XSLTAttributeDef xSLTAttributeDef7 = new XSLTAttributeDef((String) null, Constants.ATTRNAME_OUTPUT_STANDALONE, 8, false, false, 1);
        XSLTAttributeDef xSLTAttributeDef8 = new XSLTAttributeDef((String) null, Constants.ATTRNAME_OUTPUT_DOCTYPE_PUBLIC, 1, false, false, 1);
        XSLTAttributeDef xSLTAttributeDef9 = new XSLTAttributeDef((String) null, Constants.ATTRNAME_OUTPUT_DOCTYPE_SYSTEM, 1, false, false, 1);
        XSLTAttributeDef xSLTAttributeDef10 = new XSLTAttributeDef((String) null, Constants.ATTRNAME_OUTPUT_CDATA_SECTION_ELEMENTS, 19, false, false, 1);
        XSLTAttributeDef xSLTAttributeDef11 = new XSLTAttributeDef((String) null, "indent", 8, false, false, 1);
        XSLTAttributeDef xSLTAttributeDef12 = new XSLTAttributeDef((String) null, Constants.ATTRNAME_OUTPUT_MEDIATYPE, 1, false, false, 1);
        XSLTAttributeDef xSLTAttributeDef13 = new XSLTAttributeDef((String) null, "name", 9, true, false, 1);
        XSLTAttributeDef xSLTAttributeDef14 = new XSLTAttributeDef((String) null, "name", 18, true, true, 2);
        XSLTAttributeDef xSLTAttributeDef15 = new XSLTAttributeDef((String) null, "name", 17, true, true, 2);
        XSLTAttributeDef xSLTAttributeDef16 = new XSLTAttributeDef((String) null, "name", 9, false, false, 1);
        XSLTAttributeDef xSLTAttributeDef17 = new XSLTAttributeDef((String) null, "use", 5, true, false, 1);
        XSLTAttributeDef xSLTAttributeDef18 = new XSLTAttributeDef((String) null, Constants.ATTRNAME_NAMESPACE, 2, false, true, 2);
        XSLTAttributeDef xSLTAttributeDef19 = new XSLTAttributeDef((String) null, Constants.ATTRNAME_DECIMALSEPARATOR, 6, false, 1, Constants.ATTRVAL_THIS);
        XSLTAttributeDef xSLTAttributeDef20 = new XSLTAttributeDef((String) null, Constants.ATTRNAME_INFINITY, 1, false, 1, Constants.ATTRVAL_INFINITY);
        XSLTAttributeDef xSLTAttributeDef21 = new XSLTAttributeDef((String) null, Constants.ATTRNAME_MINUSSIGN, 6, false, 1, "-");
        XSLTAttributeDef xSLTAttributeDef22 = new XSLTAttributeDef((String) null, "NaN", 1, false, 1, "NaN");
        XSLTAttributeDef xSLTAttributeDef23 = new XSLTAttributeDef((String) null, Constants.ATTRNAME_PERCENT, 6, false, 1, "%");
        XSLTAttributeDef xSLTAttributeDef24 = new XSLTAttributeDef((String) null, Constants.ATTRNAME_PERMILLE, 6, false, false, 1);
        XSLTAttributeDef xSLTAttributeDef25 = new XSLTAttributeDef((String) null, Constants.ATTRNAME_ZERODIGIT, 6, false, 1, "0");
        XSLTAttributeDef xSLTAttributeDef26 = new XSLTAttributeDef((String) null, Constants.ATTRNAME_DIGIT, 6, false, 1, "#");
        XSLTAttributeDef xSLTAttributeDef27 = new XSLTAttributeDef((String) null, Constants.ATTRNAME_PATTERNSEPARATOR, 6, false, 1, ";");
        XSLTAttributeDef xSLTAttributeDef28 = new XSLTAttributeDef((String) null, Constants.ATTRNAME_GROUPINGSEPARATOR, 6, false, 1, ",");
        XSLTAttributeDef xSLTAttributeDef29 = new XSLTAttributeDef((String) null, Constants.ATTRNAME_USEATTRIBUTESETS, 10, false, false, 1);
        XSLTAttributeDef xSLTAttributeDef30 = new XSLTAttributeDef((String) null, Constants.ATTRNAME_TEST, 5, true, false, 1);
        XSLTAttributeDef xSLTAttributeDef31 = new XSLTAttributeDef((String) null, Constants.ATTRNAME_SELECT, 5, true, false, 1);
        XSLTAttributeDef xSLTAttributeDef32 = new XSLTAttributeDef((String) null, Constants.ATTRNAME_SELECT, 5, false, false, 1);
        XSLTAttributeDef xSLTAttributeDef33 = new XSLTAttributeDef((String) null, Constants.ATTRNAME_SELECT, 5, false, 1, "node()");
        XSLTAttributeDef xSLTAttributeDef34 = new XSLTAttributeDef((String) null, Constants.ATTRNAME_SELECT, 5, false, 1, Constants.ATTRVAL_THIS);
        XSLTAttributeDef xSLTAttributeDef35 = new XSLTAttributeDef((String) null, Constants.ATTRNAME_MATCH, 4, true, false, 1);
        XSLTAttributeDef xSLTAttributeDef36 = new XSLTAttributeDef((String) null, Constants.ATTRNAME_MATCH, 4, false, false, 1);
        XSLTAttributeDef xSLTAttributeDef37 = new XSLTAttributeDef((String) null, Constants.ATTRNAME_PRIORITY, 7, false, false, 1);
        XSLTAttributeDef xSLTAttributeDef38 = new XSLTAttributeDef((String) null, Constants.ATTRNAME_MODE, 9, false, false, 1);
        XSLTAttributeDef xSLTAttributeDef39 = new XSLTAttributeDef("http://www.w3.org/XML/1998/namespace", "space", false, false, false, 2, Constants.ATTRNAME_DEFAULT, 2, "preserve", 1);
        XSLTAttributeDef xSLTAttributeDef40 = new XSLTAttributeDef("http://www.w3.org/XML/1998/namespace", "space", 2, false, true, 1);
        XSLTAttributeDef xSLTAttributeDef41 = new XSLTAttributeDef((String) null, Constants.ATTRNAME_STYLESHEET_PREFIX, 1, true, false, 1);
        XSLTAttributeDef xSLTAttributeDef42 = new XSLTAttributeDef((String) null, Constants.ATTRNAME_RESULT_PREFIX, 1, true, false, 1);
        XSLTAttributeDef xSLTAttributeDef43 = new XSLTAttributeDef((String) null, Constants.ATTRNAME_DISABLE_OUTPUT_ESCAPING, 8, false, false, 1);
        XSLTAttributeDef xSLTAttributeDef44 = new XSLTAttributeDef(null, Constants.ATTRNAME_LEVEL, false, false, false, 1, Constants.ATTRVAL_SINGLE, 1, Constants.ATTRVAL_MULTI, 2, "any", 3);
        xSLTAttributeDef44.setDefault(Constants.ATTRVAL_SINGLE);
        XSLTAttributeDef xSLTAttributeDef45 = new XSLTAttributeDef((String) null, "count", 4, false, false, 1);
        XSLTAttributeDef xSLTAttributeDef46 = new XSLTAttributeDef((String) null, Constants.ATTRNAME_FROM, 4, false, false, 1);
        XSLTAttributeDef xSLTAttributeDef47 = new XSLTAttributeDef((String) null, Constants.ATTRNAME_VALUE, 5, false, false, 1);
        XSLTAttributeDef xSLTAttributeDef48 = new XSLTAttributeDef((String) null, Constants.ATTRNAME_FORMAT, 1, false, true, 1);
        xSLTAttributeDef48.setDefault("1");
        XSLTAttributeDef xSLTAttributeDef49 = new XSLTAttributeDef((String) null, "lang", 13, false, true, 1);
        XSLTAttributeDef xSLTAttributeDef50 = new XSLTAttributeDef(null, Constants.ATTRNAME_LETTERVALUE, false, true, false, 1, Constants.ATTRVAL_ALPHABETIC, 1, Constants.ATTRVAL_TRADITIONAL, 2);
        XSLTAttributeDef xSLTAttributeDef51 = new XSLTAttributeDef((String) null, Constants.ATTRNAME_GROUPINGSEPARATOR, 6, false, true, 1);
        XSLTAttributeDef xSLTAttributeDef52 = new XSLTAttributeDef((String) null, Constants.ATTRNAME_GROUPINGSIZE, 7, false, true, 1);
        XSLTAttributeDef xSLTAttributeDef53 = new XSLTAttributeDef(null, Constants.ATTRNAME_DATATYPE, false, true, true, 1, "text", 1, "number", 1);
        xSLTAttributeDef53.setDefault("text");
        XSLTAttributeDef xSLTAttributeDef54 = new XSLTAttributeDef(null, Constants.ATTRNAME_ORDER, false, true, false, 1, Constants.ATTRVAL_ORDER_ASCENDING, 1, Constants.ATTRVAL_ORDER_DESCENDING, 2);
        xSLTAttributeDef54.setDefault(Constants.ATTRVAL_ORDER_ASCENDING);
        XSLTAttributeDef xSLTAttributeDef55 = new XSLTAttributeDef(null, Constants.ATTRNAME_CASEORDER, false, true, false, 1, Constants.ATTRVAL_CASEORDER_UPPER, 1, Constants.ATTRVAL_CASEORDER_LOWER, 2);
        XSLTAttributeDef xSLTAttributeDef56 = new XSLTAttributeDef((String) null, Constants.ATTRNAME_TERMINATE, 8, false, false, 1);
        xSLTAttributeDef56.setDefault("no");
        XSLTAttributeDef xSLTAttributeDef57 = new XSLTAttributeDef(org.apache.xml.utils.Constants.S_XSLNAMESPACEURL, Constants.ATTRNAME_EXCLUDE_RESULT_PREFIXES, 20, false, false, 1);
        XSLTAttributeDef xSLTAttributeDef58 = new XSLTAttributeDef(org.apache.xml.utils.Constants.S_XSLNAMESPACEURL, Constants.ATTRNAME_EXTENSIONELEMENTPREFIXES, 15, false, false, 1);
        XSLTAttributeDef xSLTAttributeDef59 = new XSLTAttributeDef(org.apache.xml.utils.Constants.S_XSLNAMESPACEURL, Constants.ATTRNAME_USEATTRIBUTESETS, 10, false, false, 1);
        XSLTAttributeDef xSLTAttributeDef60 = new XSLTAttributeDef(org.apache.xml.utils.Constants.S_XSLNAMESPACEURL, "version", 13, false, false, 1);
        XSLTElementDef xSLTElementDef = new XSLTElementDef(this, null, "text()", null, null, null, new ProcessorCharacters(), ElemTextLiteral.class);
        xSLTElementDef.setType(2);
        XSLTElementDef xSLTElementDef2 = new XSLTElementDef(this, null, "text()", null, null, null, null, ElemTextLiteral.class);
        xSLTElementDef.setType(2);
        XSLTAttributeDef xSLTAttributeDef61 = new XSLTAttributeDef((String) null, "*", 3, false, true, 2);
        XSLTAttributeDef xSLTAttributeDef62 = new XSLTAttributeDef(org.apache.xml.utils.Constants.S_XSLNAMESPACEURL, "*", 1, false, false, 2);
        XSLTElementDef[] xSLTElementDefArr = new XSLTElementDef[24];
        XSLTElementDef[] xSLTElementDefArr2 = new XSLTElementDef[24];
        XSLTElementDef[] xSLTElementDefArr3 = new XSLTElementDef[24];
        XSLTElementDef xSLTElementDef3 = new XSLTElementDef(this, (String) null, "*", (String) null, xSLTElementDefArr, new XSLTAttributeDef[]{xSLTAttributeDef40, xSLTAttributeDef57, xSLTAttributeDef58, xSLTAttributeDef59, xSLTAttributeDef60, xSLTAttributeDef62, xSLTAttributeDef61}, (XSLTElementProcessor) new ProcessorLRE(), ElemLiteralResult.class, 20, true);
        XSLTElementDef xSLTElementDef4 = new XSLTElementDef(this, "*", "unknown", (String) null, xSLTElementDefArr, new XSLTAttributeDef[]{xSLTAttributeDef57, xSLTAttributeDef58, xSLTAttributeDef59, xSLTAttributeDef60, xSLTAttributeDef62, xSLTAttributeDef61}, (XSLTElementProcessor) new ProcessorUnknown(), ElemUnknown.class, 20, true);
        XSLTElementDef xSLTElementDef5 = new XSLTElementDef(this, org.apache.xml.utils.Constants.S_XSLNAMESPACEURL, Constants.ELEMNAME_VALUEOF_STRING, (String) null, (XSLTElementDef[]) null, new XSLTAttributeDef[]{xSLTAttributeDef31, xSLTAttributeDef43}, (XSLTElementProcessor) new ProcessorTemplateElem(), ElemValueOf.class, 20, true);
        XSLTElementDef xSLTElementDef6 = new XSLTElementDef(this, org.apache.xml.utils.Constants.S_XSLNAMESPACEURL, Constants.ELEMNAME_COPY_OF_STRING, (String) null, (XSLTElementDef[]) null, new XSLTAttributeDef[]{xSLTAttributeDef31}, (XSLTElementProcessor) new ProcessorTemplateElem(), ElemCopyOf.class, 20, true);
        XSLTElementDef xSLTElementDef7 = new XSLTElementDef(this, org.apache.xml.utils.Constants.S_XSLNAMESPACEURL, "number", (String) null, (XSLTElementDef[]) null, new XSLTAttributeDef[]{xSLTAttributeDef44, xSLTAttributeDef45, xSLTAttributeDef46, xSLTAttributeDef47, xSLTAttributeDef48, xSLTAttributeDef49, xSLTAttributeDef50, xSLTAttributeDef51, xSLTAttributeDef52}, (XSLTElementProcessor) new ProcessorTemplateElem(), ElemNumber.class, 20, true);
        XSLTElementDef xSLTElementDef8 = new XSLTElementDef(this, org.apache.xml.utils.Constants.S_XSLNAMESPACEURL, Constants.ELEMNAME_SORT_STRING, (String) null, (XSLTElementDef[]) null, new XSLTAttributeDef[]{xSLTAttributeDef34, xSLTAttributeDef49, xSLTAttributeDef53, xSLTAttributeDef54, xSLTAttributeDef55}, (XSLTElementProcessor) new ProcessorTemplateElem(), ElemSort.class, 19, true);
        XSLTElementDef xSLTElementDef9 = new XSLTElementDef(this, org.apache.xml.utils.Constants.S_XSLNAMESPACEURL, Constants.ELEMNAME_WITHPARAM_STRING, (String) null, xSLTElementDefArr, new XSLTAttributeDef[]{xSLTAttributeDef13, xSLTAttributeDef32}, (XSLTElementProcessor) new ProcessorTemplateElem(), ElemWithParam.class, 19, true);
        XSLTElementDef xSLTElementDef10 = new XSLTElementDef(this, org.apache.xml.utils.Constants.S_XSLNAMESPACEURL, Constants.ELEMNAME_APPLY_TEMPLATES_STRING, (String) null, new XSLTElementDef[]{xSLTElementDef8, xSLTElementDef9}, new XSLTAttributeDef[]{xSLTAttributeDef33, xSLTAttributeDef38}, (XSLTElementProcessor) new ProcessorTemplateElem(), ElemApplyTemplates.class, 20, true);
        XSLTElementDef xSLTElementDef11 = new XSLTElementDef(this, org.apache.xml.utils.Constants.S_XSLNAMESPACEURL, Constants.ELEMNAME_APPLY_IMPORTS_STRING, null, null, new XSLTAttributeDef[0], new ProcessorTemplateElem(), ElemApplyImport.class);
        XSLTElementDef xSLTElementDef12 = new XSLTElementDef(this, org.apache.xml.utils.Constants.S_XSLNAMESPACEURL, Constants.ELEMNAME_FOREACH_STRING, null, xSLTElementDefArr2, new XSLTAttributeDef[]{xSLTAttributeDef31, xSLTAttributeDef39}, new ProcessorTemplateElem(), ElemForEach.class, true, false, true, 20, true);
        XSLTElementDef xSLTElementDef13 = new XSLTElementDef(this, org.apache.xml.utils.Constants.S_XSLNAMESPACEURL, Constants.ELEMNAME_IF_STRING, (String) null, xSLTElementDefArr, new XSLTAttributeDef[]{xSLTAttributeDef30, xSLTAttributeDef39}, (XSLTElementProcessor) new ProcessorTemplateElem(), ElemIf.class, 20, true);
        XSLTElementDef xSLTElementDef14 = new XSLTElementDef(this, org.apache.xml.utils.Constants.S_XSLNAMESPACEURL, Constants.ELEMNAME_CHOOSE_STRING, null, new XSLTElementDef[]{new XSLTElementDef(this, org.apache.xml.utils.Constants.S_XSLNAMESPACEURL, Constants.ELEMNAME_WHEN_STRING, null, xSLTElementDefArr, new XSLTAttributeDef[]{xSLTAttributeDef30, xSLTAttributeDef39}, new ProcessorTemplateElem(), ElemWhen.class, false, true, 1, true), new XSLTElementDef(this, org.apache.xml.utils.Constants.S_XSLNAMESPACEURL, Constants.ELEMNAME_OTHERWISE_STRING, null, xSLTElementDefArr, new XSLTAttributeDef[]{xSLTAttributeDef39}, new ProcessorTemplateElem(), ElemOtherwise.class, false, false, 2, false)}, new XSLTAttributeDef[]{xSLTAttributeDef39}, new ProcessorTemplateElem(), ElemChoose.class, true, false, true, 20, true);
        XSLTElementDef[] xSLTElementDefArr4 = {xSLTElementDef, xSLTElementDef10, xSLTElementDef, xSLTElementDef11, xSLTElementDef12, xSLTElementDef5, xSLTElementDef6, xSLTElementDef7, xSLTElementDef14, xSLTElementDef13, xSLTElementDef, xSLTElementDef, xSLTElementDef, xSLTElementDef, xSLTElementDef};
        XSLTElementDef xSLTElementDef15 = new XSLTElementDef(this, org.apache.xml.utils.Constants.S_XSLNAMESPACEURL, "attribute", (String) null, xSLTElementDefArr4, new XSLTAttributeDef[]{xSLTAttributeDef14, xSLTAttributeDef18, xSLTAttributeDef39}, (XSLTElementProcessor) new ProcessorTemplateElem(), ElemAttribute.class, 20, true);
        XSLTElementDef xSLTElementDef16 = new XSLTElementDef(this, org.apache.xml.utils.Constants.S_XSLNAMESPACEURL, Constants.ELEMNAME_CALLTEMPLATE_STRING, (String) null, new XSLTElementDef[]{xSLTElementDef9}, new XSLTAttributeDef[]{xSLTAttributeDef13}, (XSLTElementProcessor) new ProcessorTemplateElem(), ElemCallTemplate.class, 20, true);
        XSLTElementDef xSLTElementDef17 = new XSLTElementDef(this, org.apache.xml.utils.Constants.S_XSLNAMESPACEURL, Constants.ELEMNAME_VARIABLE_STRING, (String) null, xSLTElementDefArr, new XSLTAttributeDef[]{xSLTAttributeDef13, xSLTAttributeDef32}, (XSLTElementProcessor) new ProcessorTemplateElem(), ElemVariable.class, 20, true);
        XSLTElementDef xSLTElementDef18 = new XSLTElementDef(this, org.apache.xml.utils.Constants.S_XSLNAMESPACEURL, Constants.ELEMNAME_PARAMVARIABLE_STRING, (String) null, xSLTElementDefArr, new XSLTAttributeDef[]{xSLTAttributeDef13, xSLTAttributeDef32}, (XSLTElementProcessor) new ProcessorTemplateElem(), ElemParam.class, 19, true);
        XSLTElementDef xSLTElementDef19 = new XSLTElementDef(this, org.apache.xml.utils.Constants.S_XSLNAMESPACEURL, "text", (String) null, new XSLTElementDef[]{xSLTElementDef}, new XSLTAttributeDef[]{xSLTAttributeDef43}, (XSLTElementProcessor) new ProcessorText(), ElemText.class, 20, true);
        XSLTElementDef xSLTElementDef20 = new XSLTElementDef(this, org.apache.xml.utils.Constants.S_XSLNAMESPACEURL, Constants.ELEMNAME_PI_STRING, (String) null, xSLTElementDefArr4, new XSLTAttributeDef[]{xSLTAttributeDef15, xSLTAttributeDef39}, (XSLTElementProcessor) new ProcessorTemplateElem(), ElemPI.class, 20, true);
        XSLTElementDef xSLTElementDef21 = new XSLTElementDef(this, org.apache.xml.utils.Constants.S_XSLNAMESPACEURL, "element", (String) null, xSLTElementDefArr, new XSLTAttributeDef[]{xSLTAttributeDef14, xSLTAttributeDef18, xSLTAttributeDef29, xSLTAttributeDef39}, (XSLTElementProcessor) new ProcessorTemplateElem(), ElemElement.class, 20, true);
        XSLTElementDef xSLTElementDef22 = new XSLTElementDef(this, org.apache.xml.utils.Constants.S_XSLNAMESPACEURL, Constants.ELEMNAME_COMMENT_STRING, (String) null, xSLTElementDefArr4, new XSLTAttributeDef[]{xSLTAttributeDef39}, (XSLTElementProcessor) new ProcessorTemplateElem(), ElemComment.class, 20, true);
        XSLTElementDef xSLTElementDef23 = new XSLTElementDef(this, org.apache.xml.utils.Constants.S_XSLNAMESPACEURL, Constants.ELEMNAME_COPY_STRING, (String) null, xSLTElementDefArr, new XSLTAttributeDef[]{xSLTAttributeDef39, xSLTAttributeDef29}, (XSLTElementProcessor) new ProcessorTemplateElem(), ElemCopy.class, 20, true);
        XSLTElementDef xSLTElementDef24 = new XSLTElementDef(this, org.apache.xml.utils.Constants.S_XSLNAMESPACEURL, Constants.ELEMNAME_MESSAGE_STRING, (String) null, xSLTElementDefArr, new XSLTAttributeDef[]{xSLTAttributeDef56}, (XSLTElementProcessor) new ProcessorTemplateElem(), ElemMessage.class, 20, true);
        XSLTElementDef xSLTElementDef25 = new XSLTElementDef(this, org.apache.xml.utils.Constants.S_XSLNAMESPACEURL, Constants.ELEMNAME_FALLBACK_STRING, (String) null, xSLTElementDefArr, new XSLTAttributeDef[]{xSLTAttributeDef39}, (XSLTElementProcessor) new ProcessorTemplateElem(), ElemFallback.class, 20, true);
        XSLTElementDef xSLTElementDef26 = new XSLTElementDef(this, org.apache.xml.utils.Constants.S_EXSLT_FUNCTIONS_URL, Constants.EXSLT_ELEMNAME_FUNCTION_STRING, null, xSLTElementDefArr3, new XSLTAttributeDef[]{xSLTAttributeDef13}, new ProcessorExsltFunction(), ElemExsltFunction.class);
        XSLTElementDef[] xSLTElementDefArr5 = {xSLTElementDef, xSLTElementDef10, xSLTElementDef16, xSLTElementDef11, xSLTElementDef12, xSLTElementDef5, xSLTElementDef6, xSLTElementDef7, xSLTElementDef14, xSLTElementDef13, xSLTElementDef19, xSLTElementDef23, xSLTElementDef17, xSLTElementDef24, xSLTElementDef25, xSLTElementDef20, xSLTElementDef22, xSLTElementDef21, xSLTElementDef15, xSLTElementDef3, xSLTElementDef4, xSLTElementDef26, new XSLTElementDef(this, org.apache.xml.utils.Constants.S_EXSLT_FUNCTIONS_URL, Constants.EXSLT_ELEMNAME_FUNCRESULT_STRING, null, xSLTElementDefArr5, new XSLTAttributeDef[]{xSLTAttributeDef32}, new ProcessorExsltFuncResult(), ElemExsltFuncResult.class)};
        System.arraycopy(xSLTElementDefArr5, 0, xSLTElementDefArr, 0, 23);
        System.arraycopy(xSLTElementDefArr5, 0, xSLTElementDefArr2, 0, 23);
        System.arraycopy(xSLTElementDefArr5, 0, xSLTElementDefArr3, 0, 23);
        xSLTElementDefArr[23] = xSLTElementDef18;
        xSLTElementDefArr2[23] = xSLTElementDef8;
        xSLTElementDefArr3[23] = xSLTElementDef18;
        XSLTElementDef xSLTElementDef27 = new XSLTElementDef(this, org.apache.xml.utils.Constants.S_XSLNAMESPACEURL, Constants.ELEMNAME_IMPORT_STRING, (String) null, (XSLTElementDef[]) null, new XSLTAttributeDef[]{xSLTAttributeDef}, (XSLTElementProcessor) new ProcessorImport(), (Class) null, 1, true);
        XSLTElementDef xSLTElementDef28 = new XSLTElementDef(this, org.apache.xml.utils.Constants.S_XSLNAMESPACEURL, Constants.ELEMNAME_INCLUDE_STRING, (String) null, (XSLTElementDef[]) null, new XSLTAttributeDef[]{xSLTAttributeDef}, (XSLTElementProcessor) new ProcessorInclude(), (Class) null, 20, true);
        XSLTAttributeDef[] xSLTAttributeDefArr = {new XSLTAttributeDef((String) null, "lang", 13, true, false, 2), new XSLTAttributeDef((String) null, "src", 2, false, false, 2)};
        XSLTAttributeDef[] xSLTAttributeDefArr2 = {new XSLTAttributeDef((String) null, "prefix", 13, true, false, 2), new XSLTAttributeDef((String) null, Constants.ATTRNAME_ELEMENTS, 14, false, false, 2), new XSLTAttributeDef((String) null, Constants.ELEMNAME_EXTENSION_STRING, 14, false, false, 2)};
        XSLTElementDef xSLTElementDef29 = new XSLTElementDef(this, org.apache.xml.utils.Constants.S_XSLNAMESPACEURL, Constants.ELEMNAME_STYLESHEET_STRING, Constants.ELEMNAME_TRANSFORM_STRING, new XSLTElementDef[]{xSLTElementDef28, xSLTElementDef27, xSLTElementDef2, xSLTElementDef4, new XSLTElementDef(this, org.apache.xml.utils.Constants.S_XSLNAMESPACEURL, Constants.ELEMNAME_STRIPSPACE_STRING, (String) null, (XSLTElementDef[]) null, new XSLTAttributeDef[]{xSLTAttributeDef2}, (XSLTElementProcessor) new ProcessorStripSpace(), (Class) null, 20, true), new XSLTElementDef(this, org.apache.xml.utils.Constants.S_XSLNAMESPACEURL, Constants.ELEMNAME_PRESERVESPACE_STRING, (String) null, (XSLTElementDef[]) null, new XSLTAttributeDef[]{xSLTAttributeDef2}, (XSLTElementProcessor) new ProcessorPreserveSpace(), (Class) null, 20, true), new XSLTElementDef(this, org.apache.xml.utils.Constants.S_XSLNAMESPACEURL, Constants.ELEMNAME_OUTPUT_STRING, (String) null, (XSLTElementDef[]) null, new XSLTAttributeDef[]{xSLTAttributeDef3, xSLTAttributeDef4, xSLTAttributeDef5, xSLTAttributeDef6, xSLTAttributeDef7, xSLTAttributeDef8, xSLTAttributeDef9, xSLTAttributeDef10, xSLTAttributeDef11, xSLTAttributeDef12, XSLTAttributeDef.m_foreignAttr}, (XSLTElementProcessor) new ProcessorOutputElem(), (Class) null, 20, true), new XSLTElementDef(this, org.apache.xml.utils.Constants.S_XSLNAMESPACEURL, "key", (String) null, (XSLTElementDef[]) null, new XSLTAttributeDef[]{xSLTAttributeDef13, xSLTAttributeDef35, xSLTAttributeDef17}, (XSLTElementProcessor) new ProcessorKey(), (Class) null, 20, true), new XSLTElementDef(this, org.apache.xml.utils.Constants.S_XSLNAMESPACEURL, Constants.ELEMNAME_DECIMALFORMAT_STRING, (String) null, (XSLTElementDef[]) null, new XSLTAttributeDef[]{xSLTAttributeDef16, xSLTAttributeDef19, xSLTAttributeDef28, xSLTAttributeDef20, xSLTAttributeDef21, xSLTAttributeDef22, xSLTAttributeDef23, xSLTAttributeDef24, xSLTAttributeDef25, xSLTAttributeDef26, xSLTAttributeDef27}, (XSLTElementProcessor) new ProcessorDecimalFormat(), (Class) null, 20, true), new XSLTElementDef(this, org.apache.xml.utils.Constants.S_XSLNAMESPACEURL, "attribute-set", (String) null, new XSLTElementDef[]{xSLTElementDef15}, new XSLTAttributeDef[]{xSLTAttributeDef13, xSLTAttributeDef29}, (XSLTElementProcessor) new ProcessorAttributeSet(), (Class) null, 20, true), new XSLTElementDef(this, org.apache.xml.utils.Constants.S_XSLNAMESPACEURL, Constants.ELEMNAME_VARIABLE_STRING, (String) null, xSLTElementDefArr5, new XSLTAttributeDef[]{xSLTAttributeDef13, xSLTAttributeDef32}, (XSLTElementProcessor) new ProcessorGlobalVariableDecl(), ElemVariable.class, 20, true), new XSLTElementDef(this, org.apache.xml.utils.Constants.S_XSLNAMESPACEURL, Constants.ELEMNAME_PARAMVARIABLE_STRING, (String) null, xSLTElementDefArr5, new XSLTAttributeDef[]{xSLTAttributeDef13, xSLTAttributeDef32}, (XSLTElementProcessor) new ProcessorGlobalParamDecl(), ElemParam.class, 20, true), new XSLTElementDef(this, org.apache.xml.utils.Constants.S_XSLNAMESPACEURL, Constants.ELEMNAME_TEMPLATE_STRING, null, xSLTElementDefArr, new XSLTAttributeDef[]{xSLTAttributeDef36, xSLTAttributeDef16, xSLTAttributeDef37, xSLTAttributeDef38, xSLTAttributeDef39}, new ProcessorTemplate(), ElemTemplate.class, true, 20, true), new XSLTElementDef(this, org.apache.xml.utils.Constants.S_XSLNAMESPACEURL, Constants.ELEMNAME_NSALIAS_STRING, (String) null, (XSLTElementDef[]) null, new XSLTAttributeDef[]{xSLTAttributeDef41, xSLTAttributeDef42}, (XSLTElementProcessor) new ProcessorNamespaceAlias(), (Class) null, 20, true), new XSLTElementDef(this, "http://xml.apache.org/xalan", Constants.ELEMNAME_COMPONENT_STRING, null, new XSLTElementDef[]{new XSLTElementDef(this, "http://xml.apache.org/xalan", Constants.ELEMNAME_SCRIPT_STRING, (String) null, new XSLTElementDef[]{xSLTElementDef}, xSLTAttributeDefArr, (XSLTElementProcessor) new ProcessorLRE(), ElemExtensionScript.class, 20, true)}, xSLTAttributeDefArr2, new ProcessorLRE(), ElemExtensionDecl.class), new XSLTElementDef(this, org.apache.xml.utils.Constants.S_BUILTIN_OLD_EXTENSIONS_URL, Constants.ELEMNAME_COMPONENT_STRING, null, new XSLTElementDef[]{new XSLTElementDef(this, org.apache.xml.utils.Constants.S_BUILTIN_OLD_EXTENSIONS_URL, Constants.ELEMNAME_SCRIPT_STRING, (String) null, new XSLTElementDef[]{xSLTElementDef}, xSLTAttributeDefArr, (XSLTElementProcessor) new ProcessorLRE(), ElemExtensionScript.class, 20, true)}, xSLTAttributeDefArr2, new ProcessorLRE(), ElemExtensionDecl.class), xSLTElementDef26}, new XSLTAttributeDef[]{new XSLTAttributeDef((String) null, Constants.ATTRNAME_EXTENSIONELEMENTPREFIXES, 15, false, false, 2), new XSLTAttributeDef((String) null, Constants.ATTRNAME_EXCLUDE_RESULT_PREFIXES, 20, false, false, 2), new XSLTAttributeDef((String) null, "id", 1, false, false, 2), new XSLTAttributeDef((String) null, "version", 13, true, false, 2), xSLTAttributeDef39}, new ProcessorStylesheetElement(), null, true, -1, false);
        xSLTElementDef27.setElements(new XSLTElementDef[]{xSLTElementDef29, xSLTElementDef3, xSLTElementDef4});
        xSLTElementDef28.setElements(new XSLTElementDef[]{xSLTElementDef29, xSLTElementDef3, xSLTElementDef4});
        build(null, null, null, new XSLTElementDef[]{xSLTElementDef29, xSLTElementDef2, xSLTElementDef3, xSLTElementDef4}, null, new ProcessorStylesheetDoc(), null);
    }

    public HashMap getElemsAvailable() {
        return this.m_availElems;
    }

    void addAvailableElement(QName qName) {
        this.m_availElems.put(qName, qName);
    }

    public boolean elementAvailable(QName qName) {
        return this.m_availElems.containsKey(qName);
    }
}
