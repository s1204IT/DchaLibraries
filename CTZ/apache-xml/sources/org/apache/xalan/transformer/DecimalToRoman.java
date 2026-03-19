package org.apache.xalan.transformer;

public class DecimalToRoman {
    public String m_postLetter;
    public long m_postValue;
    public String m_preLetter;
    public long m_preValue;

    public DecimalToRoman(long j, String str, long j2, String str2) {
        this.m_postValue = j;
        this.m_postLetter = str;
        this.m_preValue = j2;
        this.m_preLetter = str2;
    }
}
