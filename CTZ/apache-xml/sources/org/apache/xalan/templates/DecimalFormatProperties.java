package org.apache.xalan.templates;

import java.text.DecimalFormatSymbols;
import org.apache.xml.utils.QName;

public class DecimalFormatProperties extends ElemTemplateElement {
    static final long serialVersionUID = -6559409339256269446L;
    private QName m_qname = null;
    DecimalFormatSymbols m_dfs = new DecimalFormatSymbols();

    public DecimalFormatProperties(int i) {
        this.m_dfs.setInfinity(Constants.ATTRVAL_INFINITY);
        this.m_dfs.setNaN("NaN");
        this.m_docOrderNumber = i;
    }

    public DecimalFormatSymbols getDecimalFormatSymbols() {
        return this.m_dfs;
    }

    public void setName(QName qName) {
        this.m_qname = qName;
    }

    public QName getName() {
        if (this.m_qname == null) {
            return new QName("");
        }
        return this.m_qname;
    }

    public void setDecimalSeparator(char c) {
        this.m_dfs.setDecimalSeparator(c);
    }

    public char getDecimalSeparator() {
        return this.m_dfs.getDecimalSeparator();
    }

    public void setGroupingSeparator(char c) {
        this.m_dfs.setGroupingSeparator(c);
    }

    public char getGroupingSeparator() {
        return this.m_dfs.getGroupingSeparator();
    }

    public void setInfinity(String str) {
        this.m_dfs.setInfinity(str);
    }

    public String getInfinity() {
        return this.m_dfs.getInfinity();
    }

    public void setMinusSign(char c) {
        this.m_dfs.setMinusSign(c);
    }

    public char getMinusSign() {
        return this.m_dfs.getMinusSign();
    }

    public void setNaN(String str) {
        this.m_dfs.setNaN(str);
    }

    public String getNaN() {
        return this.m_dfs.getNaN();
    }

    @Override
    public String getNodeName() {
        return Constants.ELEMNAME_DECIMALFORMAT_STRING;
    }

    public void setPercent(char c) {
        this.m_dfs.setPercent(c);
    }

    public char getPercent() {
        return this.m_dfs.getPercent();
    }

    public void setPerMille(char c) {
        this.m_dfs.setPerMill(c);
    }

    public char getPerMille() {
        return this.m_dfs.getPerMill();
    }

    @Override
    public int getXSLToken() {
        return 83;
    }

    public void setZeroDigit(char c) {
        this.m_dfs.setZeroDigit(c);
    }

    public char getZeroDigit() {
        return this.m_dfs.getZeroDigit();
    }

    public void setDigit(char c) {
        this.m_dfs.setDigit(c);
    }

    public char getDigit() {
        return this.m_dfs.getDigit();
    }

    public void setPatternSeparator(char c) {
        this.m_dfs.setPatternSeparator(c);
    }

    public char getPatternSeparator() {
        return this.m_dfs.getPatternSeparator();
    }

    @Override
    public void recompose(StylesheetRoot stylesheetRoot) {
        stylesheetRoot.recomposeDecimalFormats(this);
    }
}
