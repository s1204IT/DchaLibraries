package com.android.vcard;

import android.content.ContentValues;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import com.android.contacts.compat.CompatUtils;
import com.android.contacts.model.account.BaseAccountType;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class VCardBuilder {
    private static final Set<String> sAllowedAndroidPropertySet = Collections.unmodifiableSet(new HashSet(Arrays.asList("vnd.android.cursor.item/nickname", "vnd.android.cursor.item/contact_event", "vnd.android.cursor.item/relation")));
    private static final Map<Integer, Integer> sPostalTypePriorityMap = new HashMap();
    private final boolean mAppendTypeParamName;
    private StringBuilder mBuilder;
    private final String mCharset;
    private boolean mEndAppended;
    private final boolean mIsDoCoMo;
    private final boolean mIsJapaneseMobilePhone;
    private final boolean mIsV30OrV40;
    private final boolean mNeedsToConvertPhoneticString;
    private final boolean mOnlyOneNoteFieldIsAvailable;
    private final boolean mRefrainsQPToNameProperties;
    private final boolean mShouldAppendCharsetParam;
    private final boolean mShouldUseQuotedPrintable;
    private final boolean mUsesAndroidProperty;
    private final boolean mUsesDefactProperty;
    private final String mVCardCharsetParameter;
    private final int mVCardType;

    static {
        sPostalTypePriorityMap.put(1, 0);
        sPostalTypePriorityMap.put(2, 1);
        sPostalTypePriorityMap.put(3, 2);
        sPostalTypePriorityMap.put(0, 3);
    }

    public VCardBuilder(int i, String str) {
        this.mVCardType = i;
        if (VCardConfig.isVersion40(i)) {
            Log.w("MTK_vCard", "Should not use vCard 4.0 when building vCard. It is not officially published yet.");
        }
        boolean z = true;
        this.mIsV30OrV40 = VCardConfig.isVersion30(i) || VCardConfig.isVersion40(i);
        this.mShouldUseQuotedPrintable = VCardConfig.shouldUseQuotedPrintable(i);
        this.mIsDoCoMo = VCardConfig.isDoCoMo(i);
        this.mIsJapaneseMobilePhone = VCardConfig.needsToConvertPhoneticString(i);
        this.mOnlyOneNoteFieldIsAvailable = VCardConfig.onlyOneNoteFieldIsAvailable(i);
        this.mUsesAndroidProperty = VCardConfig.usesAndroidSpecificProperty(i);
        this.mUsesDefactProperty = VCardConfig.usesDefactProperty(i);
        this.mRefrainsQPToNameProperties = VCardConfig.shouldRefrainQPToNameProperties(i);
        this.mAppendTypeParamName = VCardConfig.appendTypeParamName(i);
        this.mNeedsToConvertPhoneticString = VCardConfig.needsToConvertPhoneticString(i);
        if (VCardConfig.isVersion30(i) && "UTF-8".equalsIgnoreCase(str)) {
            z = false;
        }
        this.mShouldAppendCharsetParam = z;
        if (VCardConfig.isDoCoMo(i)) {
            if (!"SHIFT_JIS".equalsIgnoreCase(str) && TextUtils.isEmpty(str)) {
                this.mCharset = "SHIFT_JIS";
            } else {
                this.mCharset = str;
            }
            this.mVCardCharsetParameter = "CHARSET=SHIFT_JIS";
        } else if (TextUtils.isEmpty(str)) {
            Log.i("MTK_vCard", "Use the charset \"UTF-8\" for export.");
            this.mCharset = "UTF-8";
            this.mVCardCharsetParameter = "CHARSET=UTF-8";
        } else {
            this.mCharset = str;
            this.mVCardCharsetParameter = "CHARSET=" + str;
        }
        clear();
    }

    public void clear() {
        this.mBuilder = new StringBuilder();
        this.mEndAppended = false;
        appendLine("BEGIN", "VCARD");
        if (VCardConfig.isVersion40(this.mVCardType)) {
            appendLine("VERSION", "4.0");
        } else {
            if (VCardConfig.isVersion30(this.mVCardType)) {
                appendLine("VERSION", "3.0");
                return;
            }
            if (!VCardConfig.isVersion21(this.mVCardType)) {
                Log.w("MTK_vCard", "Unknown vCard version detected.");
            }
            appendLine("VERSION", "2.1");
        }
    }

    private boolean containsNonEmptyName(ContentValues contentValues) {
        return (TextUtils.isEmpty(contentValues.getAsString("data3")) && TextUtils.isEmpty(contentValues.getAsString("data5")) && TextUtils.isEmpty(contentValues.getAsString("data2")) && TextUtils.isEmpty(contentValues.getAsString("data4")) && TextUtils.isEmpty(contentValues.getAsString("data6")) && TextUtils.isEmpty(contentValues.getAsString("data9")) && TextUtils.isEmpty(contentValues.getAsString("data8")) && TextUtils.isEmpty(contentValues.getAsString("data7")) && TextUtils.isEmpty(contentValues.getAsString("data1"))) ? false : true;
    }

    private ContentValues getPrimaryContentValueWithStructuredName(List<ContentValues> list) {
        Iterator<ContentValues> it = list.iterator();
        ContentValues contentValues = null;
        ContentValues contentValues2 = null;
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            ContentValues next = it.next();
            if (next != null) {
                Integer asInteger = next.getAsInteger("is_super_primary");
                if (asInteger == null || asInteger.intValue() <= 0) {
                    if (contentValues == null) {
                        Integer asInteger2 = next.getAsInteger("is_primary");
                        if (asInteger2 == null || asInteger2.intValue() <= 0 || !containsNonEmptyName(next)) {
                            if (contentValues2 == null && containsNonEmptyName(next)) {
                                contentValues2 = next;
                            }
                        } else {
                            contentValues = next;
                        }
                    }
                } else {
                    contentValues = next;
                    break;
                }
            }
        }
        if (contentValues == null) {
            return contentValues2 != null ? contentValues2 : new ContentValues();
        }
        return contentValues;
    }

    private VCardBuilder appendNamePropertiesV40(List<ContentValues> list) {
        String str;
        if (this.mIsDoCoMo || this.mNeedsToConvertPhoneticString) {
            Log.w("MTK_vCard", "Invalid flag is used in vCard 4.0 construction. Ignored.");
        }
        if (list == null || list.isEmpty()) {
            appendLine("FN", "");
            return this;
        }
        ContentValues primaryContentValueWithStructuredName = getPrimaryContentValueWithStructuredName(list);
        String asString = primaryContentValueWithStructuredName.getAsString("data3");
        String asString2 = primaryContentValueWithStructuredName.getAsString("data5");
        String asString3 = primaryContentValueWithStructuredName.getAsString("data2");
        String asString4 = primaryContentValueWithStructuredName.getAsString("data4");
        String asString5 = primaryContentValueWithStructuredName.getAsString("data6");
        String asString6 = primaryContentValueWithStructuredName.getAsString("data1");
        if (!TextUtils.isEmpty(asString) || !TextUtils.isEmpty(asString3) || !TextUtils.isEmpty(asString2) || !TextUtils.isEmpty(asString4) || !TextUtils.isEmpty(asString5)) {
            str = asString;
        } else {
            if (TextUtils.isEmpty(asString6)) {
                appendLine("FN", "");
                return this;
            }
            str = asString6;
        }
        String asString7 = primaryContentValueWithStructuredName.getAsString("data9");
        String asString8 = primaryContentValueWithStructuredName.getAsString("data8");
        String asString9 = primaryContentValueWithStructuredName.getAsString("data7");
        String strEscapeCharacters = escapeCharacters(str);
        String strEscapeCharacters2 = escapeCharacters(asString3);
        String strEscapeCharacters3 = escapeCharacters(asString2);
        String strEscapeCharacters4 = escapeCharacters(asString4);
        String strEscapeCharacters5 = escapeCharacters(asString5);
        this.mBuilder.append("N");
        if (!TextUtils.isEmpty(asString7) || !TextUtils.isEmpty(asString8) || !TextUtils.isEmpty(asString9)) {
            this.mBuilder.append(";");
            String str2 = escapeCharacters(asString7) + ';' + escapeCharacters(asString9) + ';' + escapeCharacters(asString8);
            StringBuilder sb = this.mBuilder;
            sb.append("SORT-AS=");
            sb.append(VCardUtils.toStringAsV40ParamValue(str2));
        }
        this.mBuilder.append(":");
        this.mBuilder.append(strEscapeCharacters);
        this.mBuilder.append(";");
        this.mBuilder.append(strEscapeCharacters2);
        this.mBuilder.append(";");
        this.mBuilder.append(strEscapeCharacters3);
        this.mBuilder.append(";");
        this.mBuilder.append(strEscapeCharacters4);
        this.mBuilder.append(";");
        this.mBuilder.append(strEscapeCharacters5);
        this.mBuilder.append("\r\n");
        if (TextUtils.isEmpty(asString6)) {
            Log.w("MTK_vCard", "DISPLAY_NAME is empty.");
            appendLine("FN", escapeCharacters(VCardUtils.constructNameFromElements(VCardConfig.getNameOrderType(this.mVCardType), str, asString2, asString3, asString4, asString5)));
        } else {
            String strEscapeCharacters6 = escapeCharacters(asString6);
            this.mBuilder.append("FN");
            this.mBuilder.append(":");
            this.mBuilder.append(strEscapeCharacters6);
            this.mBuilder.append("\r\n");
        }
        appendPhoneticNameFields(primaryContentValueWithStructuredName);
        return this;
    }

    public VCardBuilder appendNameProperties(List<ContentValues> list) {
        String strEscapeCharacters;
        String strEscapeCharacters2;
        String strEscapeCharacters3;
        String strEscapeCharacters4;
        String strEscapeCharacters5;
        if (VCardConfig.isVersion40(this.mVCardType)) {
            return appendNamePropertiesV40(list);
        }
        if (list == null || list.isEmpty()) {
            if (VCardConfig.isVersion30(this.mVCardType)) {
                appendLine("N", "");
                appendLine("FN", "");
            } else if (this.mIsDoCoMo) {
                appendLine("N", "");
            }
            return this;
        }
        ContentValues primaryContentValueWithStructuredName = getPrimaryContentValueWithStructuredName(list);
        String asString = primaryContentValueWithStructuredName.getAsString("data3");
        String asString2 = primaryContentValueWithStructuredName.getAsString("data5");
        String asString3 = primaryContentValueWithStructuredName.getAsString("data2");
        String asString4 = primaryContentValueWithStructuredName.getAsString("data4");
        String asString5 = primaryContentValueWithStructuredName.getAsString("data6");
        String asString6 = primaryContentValueWithStructuredName.getAsString("data1");
        if (!TextUtils.isEmpty(asString) || !TextUtils.isEmpty(asString3)) {
            boolean z = false;
            boolean zShouldAppendCharsetParam = shouldAppendCharsetParam(asString, asString3, asString2, asString4, asString5);
            boolean z2 = (this.mRefrainsQPToNameProperties || (VCardUtils.containsOnlyNonCrLfPrintableAscii(asString) && VCardUtils.containsOnlyNonCrLfPrintableAscii(asString3) && VCardUtils.containsOnlyNonCrLfPrintableAscii(asString2) && VCardUtils.containsOnlyNonCrLfPrintableAscii(asString4) && VCardUtils.containsOnlyNonCrLfPrintableAscii(asString5))) ? false : true;
            if (TextUtils.isEmpty(asString6)) {
                asString6 = VCardUtils.constructNameFromElements(VCardConfig.getNameOrderType(this.mVCardType), asString, asString2, asString3, asString4, asString5);
            }
            boolean zShouldAppendCharsetParam2 = shouldAppendCharsetParam(asString6);
            if (!this.mRefrainsQPToNameProperties && !VCardUtils.containsOnlyNonCrLfPrintableAscii(asString6)) {
                z = true;
            }
            if (z2) {
                strEscapeCharacters = encodeQuotedPrintable(asString);
                strEscapeCharacters2 = encodeQuotedPrintable(asString3);
                strEscapeCharacters3 = encodeQuotedPrintable(asString2);
                strEscapeCharacters4 = encodeQuotedPrintable(asString4);
                strEscapeCharacters5 = encodeQuotedPrintable(asString5);
            } else {
                strEscapeCharacters = escapeCharacters(asString);
                strEscapeCharacters2 = escapeCharacters(asString3);
                strEscapeCharacters3 = escapeCharacters(asString2);
                strEscapeCharacters4 = escapeCharacters(asString4);
                strEscapeCharacters5 = escapeCharacters(asString5);
            }
            String strEncodeQuotedPrintable = z ? encodeQuotedPrintable(asString6) : escapeCharacters(asString6);
            this.mBuilder.append("N");
            if (this.mIsDoCoMo) {
                if (zShouldAppendCharsetParam) {
                    this.mBuilder.append(";");
                    this.mBuilder.append(this.mVCardCharsetParameter);
                }
                if (z2) {
                    this.mBuilder.append(";");
                    this.mBuilder.append("ENCODING=QUOTED-PRINTABLE");
                }
                this.mBuilder.append(":");
                this.mBuilder.append(asString6);
                this.mBuilder.append(";");
                this.mBuilder.append(";");
                this.mBuilder.append(";");
                this.mBuilder.append(";");
            } else {
                if (zShouldAppendCharsetParam) {
                    this.mBuilder.append(";");
                    this.mBuilder.append(this.mVCardCharsetParameter);
                }
                if (z2) {
                    this.mBuilder.append(";");
                    this.mBuilder.append("ENCODING=QUOTED-PRINTABLE");
                }
                this.mBuilder.append(":");
                this.mBuilder.append(strEscapeCharacters);
                this.mBuilder.append(";");
                this.mBuilder.append(strEscapeCharacters2);
                this.mBuilder.append(";");
                this.mBuilder.append(strEscapeCharacters3);
                this.mBuilder.append(";");
                this.mBuilder.append(strEscapeCharacters4);
                this.mBuilder.append(";");
                this.mBuilder.append(strEscapeCharacters5);
            }
            this.mBuilder.append("\r\n");
            this.mBuilder.append("FN");
            if (zShouldAppendCharsetParam2) {
                this.mBuilder.append(";");
                this.mBuilder.append(this.mVCardCharsetParameter);
            }
            if (z) {
                this.mBuilder.append(";");
                this.mBuilder.append("ENCODING=QUOTED-PRINTABLE");
            }
            this.mBuilder.append(":");
            this.mBuilder.append(strEncodeQuotedPrintable);
            this.mBuilder.append("\r\n");
        } else if (!TextUtils.isEmpty(asString6)) {
            buildSinglePartNameField("N", asString6);
            this.mBuilder.append(";");
            this.mBuilder.append(";");
            this.mBuilder.append(";");
            this.mBuilder.append(";");
            this.mBuilder.append("\r\n");
            buildSinglePartNameField("FN", asString6);
            this.mBuilder.append("\r\n");
        } else if (VCardConfig.isVersion30(this.mVCardType)) {
            appendLine("N", "");
            appendLine("FN", "");
        } else if (this.mIsDoCoMo) {
            appendLine("N", "");
        }
        appendPhoneticNameFields(primaryContentValueWithStructuredName);
        return this;
    }

    private void buildSinglePartNameField(String str, String str2) {
        String strEscapeCharacters;
        boolean z = (this.mRefrainsQPToNameProperties || VCardUtils.containsOnlyNonCrLfPrintableAscii(str2)) ? false : true;
        if (z) {
            strEscapeCharacters = encodeQuotedPrintable(str2);
        } else {
            strEscapeCharacters = escapeCharacters(str2);
        }
        this.mBuilder.append(str);
        if (shouldAppendCharsetParam(str2)) {
            this.mBuilder.append(";");
            this.mBuilder.append(this.mVCardCharsetParameter);
        }
        if (z) {
            this.mBuilder.append(";");
            this.mBuilder.append("ENCODING=QUOTED-PRINTABLE");
        }
        this.mBuilder.append(":");
        this.mBuilder.append(strEscapeCharacters);
    }

    private void appendPhoneticNameFields(ContentValues contentValues) {
        String strEscapeCharacters;
        String strEscapeCharacters2;
        String strEscapeCharacters3;
        String strEscapeCharacters4;
        String strEscapeCharacters5;
        String strEscapeCharacters6;
        boolean z;
        String asString = contentValues.getAsString("data9");
        String asString2 = contentValues.getAsString("data8");
        String asString3 = contentValues.getAsString("data7");
        if (this.mNeedsToConvertPhoneticString) {
            asString = VCardUtils.toHalfWidthString(asString);
            asString2 = VCardUtils.toHalfWidthString(asString2);
            asString3 = VCardUtils.toHalfWidthString(asString3);
        }
        if (TextUtils.isEmpty(asString) && TextUtils.isEmpty(asString2) && TextUtils.isEmpty(asString3)) {
            if (this.mIsDoCoMo) {
                this.mBuilder.append("SOUND");
                this.mBuilder.append(";");
                this.mBuilder.append("X-IRMC-N");
                this.mBuilder.append(":");
                this.mBuilder.append(";");
                this.mBuilder.append(";");
                this.mBuilder.append(";");
                this.mBuilder.append(";");
                this.mBuilder.append("\r\n");
                return;
            }
            return;
        }
        if (!VCardConfig.isVersion40(this.mVCardType)) {
            if (VCardConfig.isVersion30(this.mVCardType)) {
                String strConstructNameFromElements = VCardUtils.constructNameFromElements(this.mVCardType, asString, asString2, asString3);
                this.mBuilder.append("SORT-STRING");
                if (VCardConfig.isVersion30(this.mVCardType) && shouldAppendCharsetParam(strConstructNameFromElements)) {
                    this.mBuilder.append(";");
                    this.mBuilder.append(this.mVCardCharsetParameter);
                }
                this.mBuilder.append(":");
                this.mBuilder.append(escapeCharacters(strConstructNameFromElements));
                this.mBuilder.append("\r\n");
            } else if (this.mIsJapaneseMobilePhone) {
                this.mBuilder.append("SOUND");
                this.mBuilder.append(";");
                this.mBuilder.append("X-IRMC-N");
                if ((this.mRefrainsQPToNameProperties || (VCardUtils.containsOnlyNonCrLfPrintableAscii(asString) && VCardUtils.containsOnlyNonCrLfPrintableAscii(asString2) && VCardUtils.containsOnlyNonCrLfPrintableAscii(asString3))) ? false : true) {
                    strEscapeCharacters4 = encodeQuotedPrintable(asString);
                    strEscapeCharacters5 = encodeQuotedPrintable(asString2);
                    strEscapeCharacters6 = encodeQuotedPrintable(asString3);
                } else {
                    strEscapeCharacters4 = escapeCharacters(asString);
                    strEscapeCharacters5 = escapeCharacters(asString2);
                    strEscapeCharacters6 = escapeCharacters(asString3);
                }
                if (shouldAppendCharsetParam(strEscapeCharacters4, strEscapeCharacters5, strEscapeCharacters6)) {
                    this.mBuilder.append(";");
                    this.mBuilder.append(this.mVCardCharsetParameter);
                }
                this.mBuilder.append(":");
                if (TextUtils.isEmpty(strEscapeCharacters4)) {
                    z = true;
                } else {
                    this.mBuilder.append(strEscapeCharacters4);
                    z = false;
                }
                if (!TextUtils.isEmpty(strEscapeCharacters5)) {
                    if (!z) {
                        this.mBuilder.append(' ');
                    } else {
                        z = false;
                    }
                    this.mBuilder.append(strEscapeCharacters5);
                }
                if (!TextUtils.isEmpty(strEscapeCharacters6)) {
                    if (!z) {
                        this.mBuilder.append(' ');
                    }
                    this.mBuilder.append(strEscapeCharacters6);
                }
                this.mBuilder.append(";");
                this.mBuilder.append(";");
                this.mBuilder.append(";");
                this.mBuilder.append(";");
                this.mBuilder.append("\r\n");
            }
        }
        if (this.mUsesDefactProperty) {
            if (!TextUtils.isEmpty(asString3)) {
                boolean z2 = this.mShouldUseQuotedPrintable && !VCardUtils.containsOnlyNonCrLfPrintableAscii(asString3);
                if (z2) {
                    strEscapeCharacters3 = encodeQuotedPrintable(asString3);
                } else {
                    strEscapeCharacters3 = escapeCharacters(asString3);
                }
                this.mBuilder.append("X-PHONETIC-FIRST-NAME");
                if (shouldAppendCharsetParam(asString3)) {
                    this.mBuilder.append(";");
                    this.mBuilder.append(this.mVCardCharsetParameter);
                }
                if (z2) {
                    this.mBuilder.append(";");
                    this.mBuilder.append("ENCODING=QUOTED-PRINTABLE");
                }
                this.mBuilder.append(":");
                this.mBuilder.append(strEscapeCharacters3);
                this.mBuilder.append("\r\n");
            }
            if (!TextUtils.isEmpty(asString2)) {
                boolean z3 = this.mShouldUseQuotedPrintable && !VCardUtils.containsOnlyNonCrLfPrintableAscii(asString2);
                if (z3) {
                    strEscapeCharacters2 = encodeQuotedPrintable(asString2);
                } else {
                    strEscapeCharacters2 = escapeCharacters(asString2);
                }
                this.mBuilder.append("X-PHONETIC-MIDDLE-NAME");
                if (shouldAppendCharsetParam(asString2)) {
                    this.mBuilder.append(";");
                    this.mBuilder.append(this.mVCardCharsetParameter);
                }
                if (z3) {
                    this.mBuilder.append(";");
                    this.mBuilder.append("ENCODING=QUOTED-PRINTABLE");
                }
                this.mBuilder.append(":");
                this.mBuilder.append(strEscapeCharacters2);
                this.mBuilder.append("\r\n");
            }
            if (!TextUtils.isEmpty(asString)) {
                boolean z4 = this.mShouldUseQuotedPrintable && !VCardUtils.containsOnlyNonCrLfPrintableAscii(asString);
                if (z4) {
                    strEscapeCharacters = encodeQuotedPrintable(asString);
                } else {
                    strEscapeCharacters = escapeCharacters(asString);
                }
                this.mBuilder.append("X-PHONETIC-LAST-NAME");
                if (shouldAppendCharsetParam(asString)) {
                    this.mBuilder.append(";");
                    this.mBuilder.append(this.mVCardCharsetParameter);
                }
                if (z4) {
                    this.mBuilder.append(";");
                    this.mBuilder.append("ENCODING=QUOTED-PRINTABLE");
                }
                this.mBuilder.append(":");
                this.mBuilder.append(strEscapeCharacters);
                this.mBuilder.append("\r\n");
            }
        }
    }

    public VCardBuilder appendNickNames(List<ContentValues> list) {
        boolean z;
        if (this.mIsV30OrV40) {
            z = false;
        } else if (this.mUsesAndroidProperty) {
            z = true;
        } else {
            return this;
        }
        if (list != null) {
            for (ContentValues contentValues : list) {
                String asString = contentValues.getAsString("data1");
                if (!TextUtils.isEmpty(asString)) {
                    if (z) {
                        appendAndroidSpecificProperty("vnd.android.cursor.item/nickname", contentValues);
                    } else {
                        appendLineWithCharsetAndQPDetection("NICKNAME", asString);
                    }
                }
            }
        }
        return this;
    }

    public VCardBuilder appendPhones(List<ContentValues> list, VCardPhoneNumberTranslationCallback vCardPhoneNumberTranslationCallback) {
        boolean z;
        String string;
        if (list != null) {
            HashSet hashSet = new HashSet();
            z = false;
            for (ContentValues contentValues : list) {
                Integer asInteger = contentValues.getAsInteger("data2");
                String asString = contentValues.getAsString("data3");
                Integer asInteger2 = contentValues.getAsInteger("is_primary");
                boolean z2 = asInteger2 != null && asInteger2.intValue() > 0;
                String asString2 = contentValues.getAsString("data1");
                if (asString2 != null) {
                    asString2 = asString2.trim();
                }
                if (!TextUtils.isEmpty(asString2)) {
                    int iIntValue = asInteger != null ? asInteger.intValue() : 1;
                    if (vCardPhoneNumberTranslationCallback != null) {
                        String strOnValueReceived = vCardPhoneNumberTranslationCallback.onValueReceived(asString2, iIntValue, asString, z2);
                        if (!hashSet.contains(strOnValueReceived)) {
                            hashSet.add(strOnValueReceived);
                            appendTelLine(Integer.valueOf(iIntValue), asString, strOnValueReceived, z2);
                        }
                    } else if (iIntValue == 6 || VCardConfig.refrainPhoneNumberFormatting(this.mVCardType)) {
                        if (!hashSet.contains(asString2)) {
                            hashSet.add(asString2);
                            appendTelLine(Integer.valueOf(iIntValue), asString, asString2, z2);
                        }
                        z = true;
                    } else {
                        List<String> listSplitPhoneNumbers = splitPhoneNumbers(asString2);
                        if (!listSplitPhoneNumbers.isEmpty()) {
                            for (String str : listSplitPhoneNumbers) {
                                if (!hashSet.contains(str)) {
                                    if (TextUtils.equals(str, str)) {
                                        StringBuilder sb = new StringBuilder();
                                        int length = str.length();
                                        for (int i = 0; i < length; i++) {
                                            char cCharAt = str.charAt(i);
                                            if (Character.isDigit(cCharAt) || cCharAt == '+' || cCharAt == 'p' || cCharAt == 'w' || cCharAt == 'P' || cCharAt == 'W' || cCharAt == ' ' || cCharAt == ',' || cCharAt == ';' || cCharAt == '-' || cCharAt == '/' || cCharAt == '*' || cCharAt == '#' || cCharAt == '.') {
                                                sb.append(cCharAt);
                                            }
                                        }
                                        VCardUtils.getPhoneNumberFormat(this.mVCardType);
                                        string = sb.toString();
                                    } else {
                                        string = str;
                                    }
                                    if (VCardConfig.isVersion40(this.mVCardType) && !TextUtils.isEmpty(string) && !string.startsWith("tel:")) {
                                        string = "tel:" + string;
                                    }
                                    hashSet.add(str);
                                    appendTelLine(Integer.valueOf(iIntValue), asString, string, z2);
                                }
                            }
                            z = true;
                        }
                    }
                }
            }
        } else {
            z = false;
        }
        if (!z && this.mIsDoCoMo) {
            appendTelLine(1, "", "", false);
        }
        return this;
    }

    private List<String> splitPhoneNumbers(String str) {
        ArrayList arrayList = new ArrayList();
        StringBuilder sb = new StringBuilder();
        int length = str.length();
        for (int i = 0; i < length; i++) {
            char cCharAt = str.charAt(i);
            if (cCharAt == '\n' && sb.length() > 0) {
                arrayList.add(sb.toString());
                sb = new StringBuilder();
            } else {
                sb.append(cCharAt);
            }
        }
        if (sb.length() > 0) {
            arrayList.add(sb.toString());
        }
        return arrayList;
    }

    public VCardBuilder appendEmails(List<ContentValues> list) {
        boolean z;
        if (list != null) {
            HashSet hashSet = new HashSet();
            z = false;
            for (ContentValues contentValues : list) {
                String asString = contentValues.getAsString("data1");
                if (asString != null) {
                    asString = asString.trim();
                }
                if (!TextUtils.isEmpty(asString)) {
                    Integer asInteger = contentValues.getAsInteger("data2");
                    int iIntValue = asInteger != null ? asInteger.intValue() : 3;
                    String asString2 = contentValues.getAsString("data3");
                    Integer asInteger2 = contentValues.getAsInteger("is_primary");
                    boolean z2 = asInteger2 != null && asInteger2.intValue() > 0;
                    if (!hashSet.contains(asString)) {
                        hashSet.add(asString);
                        appendEmailLine(iIntValue, asString2, asString, z2);
                    }
                    z = true;
                }
            }
        } else {
            z = false;
        }
        if (!z && this.mIsDoCoMo) {
            appendEmailLine(1, "", "", false);
        }
        return this;
    }

    public VCardBuilder appendPostals(List<ContentValues> list) {
        if (list == null || list.isEmpty()) {
            if (this.mIsDoCoMo) {
                this.mBuilder.append("ADR");
                this.mBuilder.append(";");
                this.mBuilder.append("HOME");
                this.mBuilder.append(":");
                this.mBuilder.append("\r\n");
            }
        } else if (this.mIsDoCoMo) {
            appendPostalsForDoCoMo(list);
        } else {
            appendPostalsForGeneric(list);
        }
        return this;
    }

    private void appendPostalsForDoCoMo(List<ContentValues> list) {
        ContentValues contentValues;
        int i;
        Iterator<ContentValues> it = list.iterator();
        int iIntValue = Integer.MAX_VALUE;
        ContentValues contentValues2 = null;
        int i2 = Integer.MAX_VALUE;
        while (true) {
            if (it.hasNext()) {
                ContentValues next = it.next();
                if (next != null) {
                    Integer asInteger = next.getAsInteger("data2");
                    Integer num = sPostalTypePriorityMap.get(asInteger);
                    int iIntValue2 = num != null ? num.intValue() : Integer.MAX_VALUE;
                    if (iIntValue2 < i2) {
                        iIntValue = asInteger.intValue();
                        if (iIntValue2 != 0) {
                            contentValues2 = next;
                            i2 = iIntValue2;
                        } else {
                            i = iIntValue;
                            contentValues = next;
                            break;
                        }
                    } else {
                        continue;
                    }
                }
            } else {
                contentValues = contentValues2;
                i = iIntValue;
                break;
            }
        }
        if (contentValues == null) {
            Log.w("MTK_vCard", "Should not come here. Must have at least one postal data.");
        } else {
            appendPostalLine(i, contentValues.getAsString("data3"), contentValues, false, true);
        }
    }

    private void appendPostalsForGeneric(List<ContentValues> list) {
        int iIntValue;
        for (ContentValues contentValues : list) {
            if (contentValues != null) {
                Integer asInteger = contentValues.getAsInteger("data2");
                if (asInteger != null) {
                    iIntValue = asInteger.intValue();
                } else {
                    iIntValue = 1;
                }
                String asString = contentValues.getAsString("data3");
                Integer asInteger2 = contentValues.getAsInteger("is_primary");
                boolean z = false;
                if (asInteger2 != null && asInteger2.intValue() > 0) {
                    z = true;
                }
                appendPostalLine(iIntValue, asString, contentValues, z, false);
            }
        }
    }

    private static class PostalStruct {
        final String addressData;
        final boolean appendCharset;
        final boolean reallyUseQuotedPrintable;

        public PostalStruct(boolean z, boolean z2, String str) {
            this.reallyUseQuotedPrintable = z;
            this.appendCharset = z2;
            this.addressData = str;
        }
    }

    private PostalStruct tryConstructPostalStruct(ContentValues contentValues) {
        String strEscapeCharacters;
        String strEscapeCharacters2;
        String strEscapeCharacters3;
        String strEscapeCharacters4;
        String strEscapeCharacters5;
        String strEscapeCharacters6;
        String strEscapeCharacters7;
        String strEscapeCharacters8;
        String asString = contentValues.getAsString("data5");
        String asString2 = contentValues.getAsString("data6");
        String asString3 = contentValues.getAsString("data4");
        String asString4 = contentValues.getAsString("data7");
        String asString5 = contentValues.getAsString("data8");
        String asString6 = contentValues.getAsString("data9");
        String asString7 = contentValues.getAsString("data10");
        boolean z = false;
        String[] strArr = {asString, asString2, asString3, asString4, asString5, asString6, asString7};
        if (!VCardUtils.areAllEmpty(strArr)) {
            if (this.mShouldUseQuotedPrintable && !VCardUtils.containsOnlyNonCrLfPrintableAscii(strArr)) {
                z = true;
            }
            boolean z2 = !VCardUtils.containsOnlyPrintableAscii(strArr);
            if (z) {
                strEscapeCharacters2 = encodeQuotedPrintable(asString);
                strEscapeCharacters3 = encodeQuotedPrintable(asString3);
                strEscapeCharacters4 = encodeQuotedPrintable(asString4);
                strEscapeCharacters5 = encodeQuotedPrintable(asString5);
                strEscapeCharacters6 = encodeQuotedPrintable(asString6);
                strEscapeCharacters7 = encodeQuotedPrintable(asString7);
                strEscapeCharacters8 = encodeQuotedPrintable(asString2);
            } else {
                strEscapeCharacters2 = escapeCharacters(asString);
                strEscapeCharacters3 = escapeCharacters(asString3);
                strEscapeCharacters4 = escapeCharacters(asString4);
                strEscapeCharacters5 = escapeCharacters(asString5);
                strEscapeCharacters6 = escapeCharacters(asString6);
                strEscapeCharacters7 = escapeCharacters(asString7);
                strEscapeCharacters8 = escapeCharacters(asString2);
            }
            StringBuilder sb = new StringBuilder();
            sb.append(strEscapeCharacters2);
            sb.append(";");
            if (!TextUtils.isEmpty(strEscapeCharacters8)) {
                sb.append(strEscapeCharacters8);
            }
            sb.append(";");
            sb.append(strEscapeCharacters3);
            sb.append(";");
            sb.append(strEscapeCharacters4);
            sb.append(";");
            sb.append(strEscapeCharacters5);
            sb.append(";");
            sb.append(strEscapeCharacters6);
            sb.append(";");
            sb.append(strEscapeCharacters7);
            return new PostalStruct(z, z2, sb.toString());
        }
        String asString8 = contentValues.getAsString("data1");
        if (TextUtils.isEmpty(asString8)) {
            return null;
        }
        boolean z3 = this.mShouldUseQuotedPrintable && !VCardUtils.containsOnlyNonCrLfPrintableAscii(asString8);
        boolean z4 = !VCardUtils.containsOnlyPrintableAscii(asString8);
        if (z3) {
            strEscapeCharacters = encodeQuotedPrintable(asString8);
        } else {
            strEscapeCharacters = escapeCharacters(asString8);
        }
        return new PostalStruct(z3, z4, ";" + strEscapeCharacters + ";;;;;");
    }

    public VCardBuilder appendIms(List<ContentValues> list) {
        if (list != null) {
            for (ContentValues contentValues : list) {
                Integer asInteger = contentValues.getAsInteger("data5");
                if (asInteger != null) {
                    if (-1 == asInteger.intValue()) {
                        String asString = contentValues.getAsString("data6");
                        ArrayList arrayList = new ArrayList();
                        if (asString != null && !asString.isEmpty()) {
                            arrayList.add(asString);
                        }
                        appendLineWithCharsetAndQPDetection("X-CUSTOM-IM", arrayList, contentValues.getAsString("data1"));
                    } else {
                        String propertyNameForIm = VCardUtils.getPropertyNameForIm(asInteger.intValue());
                        if (propertyNameForIm != null) {
                            String asString2 = contentValues.getAsString("data1");
                            if (asString2 != null) {
                                asString2 = asString2.trim();
                            }
                            if (!TextUtils.isEmpty(asString2)) {
                                Integer asInteger2 = contentValues.getAsInteger("data2");
                                String str = null;
                                switch (asInteger2 != null ? asInteger2.intValue() : 3) {
                                    case 0:
                                        String asString3 = contentValues.getAsString("data3");
                                        if (asString3 != null) {
                                            str = "X-" + asString3;
                                        }
                                        break;
                                    case 1:
                                        str = "HOME";
                                        break;
                                    case 2:
                                        str = "WORK";
                                        break;
                                }
                                ArrayList arrayList2 = new ArrayList();
                                if (!TextUtils.isEmpty(str)) {
                                    arrayList2.add(str);
                                }
                                Integer asInteger3 = contentValues.getAsInteger("is_primary");
                                boolean z = false;
                                if (asInteger3 != null && asInteger3.intValue() > 0) {
                                    z = true;
                                }
                                if (z) {
                                    arrayList2.add("PREF");
                                }
                                appendLineWithCharsetAndQPDetection(propertyNameForIm, arrayList2, asString2);
                            }
                        }
                    }
                }
            }
        }
        return this;
    }

    public VCardBuilder appendWebsites(List<ContentValues> list) {
        if (list != null) {
            Iterator<ContentValues> it = list.iterator();
            while (it.hasNext()) {
                String asString = it.next().getAsString("data1");
                if (asString != null) {
                    asString = asString.trim();
                }
                if (!TextUtils.isEmpty(asString)) {
                    appendLineWithCharsetAndQPDetection("URL", asString);
                }
            }
        }
        return this;
    }

    public VCardBuilder appendOrganizations(List<ContentValues> list) {
        if (list != null) {
            for (ContentValues contentValues : list) {
                String asString = contentValues.getAsString("data1");
                if (asString != null) {
                    asString = asString.trim();
                }
                String asString2 = contentValues.getAsString("data5");
                if (asString2 != null) {
                    asString2 = asString2.trim();
                }
                String asString3 = contentValues.getAsString("data4");
                if (asString3 != null) {
                    asString3 = asString3.trim();
                }
                StringBuilder sb = new StringBuilder();
                if (!TextUtils.isEmpty(asString)) {
                    sb.append(asString);
                }
                if (!TextUtils.isEmpty(asString2)) {
                    if (sb.length() > 0) {
                        sb.append(';');
                    }
                    sb.append(asString2);
                }
                String string = sb.toString();
                appendLine("ORG", string, !VCardUtils.containsOnlyPrintableAscii(string), this.mShouldUseQuotedPrintable && !VCardUtils.containsOnlyNonCrLfPrintableAscii(string));
                if (!TextUtils.isEmpty(asString3)) {
                    appendLine("TITLE", asString3, !VCardUtils.containsOnlyPrintableAscii(asString3), this.mShouldUseQuotedPrintable && !VCardUtils.containsOnlyNonCrLfPrintableAscii(asString3));
                }
            }
        }
        return this;
    }

    public VCardBuilder appendPhotos(List<ContentValues> list) {
        byte[] asByteArray;
        if (list != null) {
            for (ContentValues contentValues : list) {
                if (contentValues != null && (asByteArray = contentValues.getAsByteArray("data15")) != null) {
                    String strGuessImageType = VCardUtils.guessImageType(asByteArray);
                    if (strGuessImageType == null) {
                        Log.d("MTK_vCard", "Unknown photo type. Ignored.");
                    } else {
                        String str = new String(Base64.encode(asByteArray, 2));
                        if (!TextUtils.isEmpty(str)) {
                            appendPhotoLine(str, strGuessImageType);
                        }
                    }
                }
            }
        }
        return this;
    }

    public VCardBuilder appendNotes(List<ContentValues> list) {
        if (list != null) {
            boolean z = false;
            if (this.mOnlyOneNoteFieldIsAvailable) {
                StringBuilder sb = new StringBuilder();
                Iterator<ContentValues> it = list.iterator();
                boolean z2 = true;
                while (it.hasNext()) {
                    String asString = it.next().getAsString("data1");
                    if (asString == null) {
                        asString = "";
                    }
                    if (asString.length() > 0) {
                        if (!z2) {
                            sb.append('\n');
                        } else {
                            z2 = false;
                        }
                        sb.append(asString);
                    }
                }
                String string = sb.toString();
                boolean z3 = !VCardUtils.containsOnlyPrintableAscii(string);
                if (this.mShouldUseQuotedPrintable && !VCardUtils.containsOnlyNonCrLfPrintableAscii(string)) {
                    z = true;
                }
                appendLine("NOTE", string, z3, z);
            } else {
                Iterator<ContentValues> it2 = list.iterator();
                while (it2.hasNext()) {
                    String asString2 = it2.next().getAsString("data1");
                    if (!TextUtils.isEmpty(asString2)) {
                        appendLine("NOTE", asString2, !VCardUtils.containsOnlyPrintableAscii(asString2), this.mShouldUseQuotedPrintable && !VCardUtils.containsOnlyNonCrLfPrintableAscii(asString2));
                    }
                }
            }
        }
        return this;
    }

    public VCardBuilder appendEvents(List<ContentValues> list) {
        int iIntValue;
        if (list != null) {
            Iterator<ContentValues> it = list.iterator();
            String str = null;
            String str2 = null;
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                ContentValues next = it.next();
                if (next != null) {
                    Integer asInteger = next.getAsInteger("data2");
                    if (asInteger != null) {
                        iIntValue = asInteger.intValue();
                    } else {
                        iIntValue = 2;
                    }
                    if (iIntValue == 3) {
                        String asString = next.getAsString("data1");
                        if (asString == null) {
                            continue;
                        } else {
                            Integer asInteger2 = next.getAsInteger("is_super_primary");
                            boolean z = false;
                            if (!(asInteger2 != null && asInteger2.intValue() > 0)) {
                                Integer asInteger3 = next.getAsInteger("is_primary");
                                if (asInteger3 != null && asInteger3.intValue() > 0) {
                                    z = true;
                                }
                                if (!z) {
                                    if (str2 == null) {
                                        str2 = asString;
                                    }
                                } else {
                                    str = asString;
                                }
                            } else {
                                str = asString;
                                break;
                            }
                        }
                    } else if (this.mUsesAndroidProperty) {
                        appendAndroidSpecificProperty("vnd.android.cursor.item/contact_event", next);
                    }
                }
            }
            if (str != null) {
                appendLineWithCharsetAndQPDetection("BDAY", str.trim());
            } else if (str2 != null) {
                appendLineWithCharsetAndQPDetection("BDAY", str2.trim());
            }
        }
        return this;
    }

    public VCardBuilder appendRelation(List<ContentValues> list) {
        if (this.mUsesAndroidProperty && list != null) {
            for (ContentValues contentValues : list) {
                if (contentValues != null) {
                    appendAndroidSpecificProperty("vnd.android.cursor.item/relation", contentValues);
                }
            }
        }
        return this;
    }

    public void appendPostalLine(int i, String str, ContentValues contentValues, boolean z, boolean z2) {
        boolean z3;
        boolean z4;
        String str2;
        PostalStruct postalStructTryConstructPostalStruct = tryConstructPostalStruct(contentValues);
        if (postalStructTryConstructPostalStruct == null) {
            if (z2) {
                str2 = "";
                z3 = false;
                z4 = false;
            } else {
                return;
            }
        } else {
            z3 = postalStructTryConstructPostalStruct.reallyUseQuotedPrintable;
            z4 = postalStructTryConstructPostalStruct.appendCharset;
            str2 = postalStructTryConstructPostalStruct.addressData;
        }
        ArrayList arrayList = new ArrayList();
        if (z) {
            arrayList.add("PREF");
        }
        switch (i) {
            case 0:
                if (!TextUtils.isEmpty(str) && VCardUtils.containsOnlyAlphaDigitHyphen(str)) {
                    arrayList.add("X-" + str);
                }
                break;
            case 1:
                arrayList.add("HOME");
                break;
            case 2:
                arrayList.add("WORK");
                break;
            case 3:
                arrayList.add("OTHER");
                break;
            default:
                Log.e("MTK_vCard", "Unknown StructuredPostal type: " + i);
                break;
        }
        this.mBuilder.append("ADR");
        if (!arrayList.isEmpty()) {
            this.mBuilder.append(";");
            appendTypeParameters(arrayList);
        }
        if (z4) {
            this.mBuilder.append(";");
            this.mBuilder.append(this.mVCardCharsetParameter);
        }
        if (z3) {
            this.mBuilder.append(";");
            this.mBuilder.append("ENCODING=QUOTED-PRINTABLE");
        }
        this.mBuilder.append(":");
        this.mBuilder.append(str2);
        this.mBuilder.append("\r\n");
    }

    public void appendEmailLine(int i, String str, String str2, boolean z) {
        switch (i) {
            case 0:
                if (VCardUtils.isMobilePhoneLabel(str)) {
                    str = "CELL";
                } else if (TextUtils.isEmpty(str) || !this.mIsV30OrV40) {
                    str = (!TextUtils.isEmpty(str) && VCardUtils.containsOnlyAlphaDigitHyphen(str)) ? "X-" + str : null;
                }
                break;
            case 1:
                str = "HOME";
                break;
            case 2:
                str = "WORK";
                break;
            case 3:
                break;
            case CompatUtils.TYPE_ASSERT:
                str = "CELL";
                break;
            default:
                Log.e("MTK_vCard", "Unknown Email type: " + i);
                break;
        }
        ArrayList arrayList = new ArrayList();
        if (z) {
            arrayList.add("PREF");
        }
        if (!TextUtils.isEmpty(str)) {
            arrayList.add(str);
        }
        appendLineWithCharsetAndQPDetection("EMAIL", arrayList, str2);
    }

    public void appendTelLine(Integer num, String str, String str2, boolean z) {
        int iIntValue;
        this.mBuilder.append("TEL");
        this.mBuilder.append(";");
        if (num == null) {
            iIntValue = 7;
        } else {
            iIntValue = num.intValue();
        }
        ArrayList arrayList = new ArrayList();
        boolean z2 = true;
        switch (iIntValue) {
            case 0:
                if (TextUtils.isEmpty(str)) {
                    arrayList.add("VOICE");
                } else if (VCardUtils.isMobilePhoneLabel(str)) {
                    arrayList.add("CELL");
                } else if (this.mIsV30OrV40) {
                    arrayList.add(str);
                } else {
                    String upperCase = str.toUpperCase();
                    if (VCardUtils.isValidInV21ButUnknownToContactsPhoteType(upperCase)) {
                        arrayList.add(upperCase);
                    } else if (VCardUtils.containsOnlyAlphaDigitHyphen(str)) {
                        arrayList.add("X-" + str);
                    }
                }
                z2 = z;
                break;
            case 1:
                arrayList.addAll(Arrays.asList("HOME"));
                z2 = z;
                break;
            case 2:
                arrayList.add("CELL");
                z2 = z;
                break;
            case 3:
                arrayList.addAll(Arrays.asList("WORK"));
                z2 = z;
                break;
            case CompatUtils.TYPE_ASSERT:
                arrayList.addAll(Arrays.asList("WORK", "FAX"));
                z2 = z;
                break;
            case 5:
                arrayList.addAll(Arrays.asList("HOME", "FAX"));
                z2 = z;
                break;
            case 6:
                if (this.mIsDoCoMo) {
                    arrayList.add("VOICE");
                } else {
                    arrayList.add("PAGER");
                }
                z2 = z;
                break;
            case 7:
                arrayList.add("OTHER");
                z2 = z;
                break;
            case 8:
                arrayList.add("CALLBACK");
                z2 = z;
                break;
            case 9:
                arrayList.add("CAR");
                z2 = z;
                break;
            case BaseAccountType.Weight.PHONE:
                arrayList.add("COMPANY-MAIN");
                break;
            case 11:
                arrayList.add("ISDN");
                z2 = z;
                break;
            case 12:
                break;
            case 13:
                arrayList.add("OTHER-FAX");
                z2 = z;
                break;
            case 14:
                arrayList.add("RADIO");
                z2 = z;
                break;
            case BaseAccountType.Weight.EMAIL:
                arrayList.add("TLX");
                z2 = z;
                break;
            case 16:
                arrayList.add("TTY-TDD");
                z2 = z;
                break;
            case 17:
                arrayList.addAll(Arrays.asList("WORK", "CELL"));
                z2 = z;
                break;
            case 18:
                arrayList.add("WORK");
                if (this.mIsDoCoMo) {
                    arrayList.add("VOICE");
                } else {
                    arrayList.add("PAGER");
                }
                z2 = z;
                break;
            case 19:
                arrayList.add("ASSISTANT");
                z2 = z;
                break;
            case 20:
                arrayList.add("MSG");
                z2 = z;
                break;
            default:
                z2 = z;
                break;
        }
        if (z2) {
            arrayList.add("PREF");
        }
        if (arrayList.isEmpty()) {
            appendUncommonPhoneType(this.mBuilder, Integer.valueOf(iIntValue));
        } else {
            appendTypeParameters(arrayList);
        }
        this.mBuilder.append(":");
        this.mBuilder.append(str2);
        this.mBuilder.append("\r\n");
    }

    private void appendUncommonPhoneType(StringBuilder sb, Integer num) {
        if (this.mIsDoCoMo) {
            sb.append("VOICE");
            return;
        }
        String phoneTypeString = VCardUtils.getPhoneTypeString(num);
        if (phoneTypeString != null) {
            appendTypeParameter(phoneTypeString);
            return;
        }
        Log.e("MTK_vCard", "Unknown or unsupported (by vCard) Phone type: " + num);
    }

    public void appendPhotoLine(String str, String str2) {
        StringBuilder sb = new StringBuilder();
        sb.append("PHOTO");
        sb.append(";");
        if (this.mIsV30OrV40) {
            sb.append("ENCODING=B");
        } else {
            sb.append("ENCODING=BASE64");
        }
        sb.append(";");
        appendTypeParameter(sb, str2);
        sb.append(":");
        sb.append(str);
        String string = sb.toString();
        StringBuilder sb2 = new StringBuilder();
        int length = string.length();
        int length2 = 75 - "\r\n".length();
        int length3 = length2 - " ".length();
        int i = length2;
        int i2 = 0;
        for (int i3 = 0; i3 < length; i3++) {
            sb2.append(string.charAt(i3));
            i2++;
            if (i2 > i) {
                sb2.append("\r\n");
                sb2.append(" ");
                i = length3;
                i2 = 0;
            }
        }
        this.mBuilder.append(sb2.toString());
        this.mBuilder.append("\r\n");
        this.mBuilder.append("\r\n");
    }

    public VCardBuilder appendSipAddresses(List<ContentValues> list) {
        boolean z;
        String str;
        if (this.mIsV30OrV40) {
            z = false;
        } else if (this.mUsesDefactProperty) {
            z = true;
        } else {
            return this;
        }
        if (list != null) {
            Iterator<ContentValues> it = list.iterator();
            while (it.hasNext()) {
                String asString = it.next().getAsString("data1");
                if (!TextUtils.isEmpty(asString)) {
                    if (z) {
                        if (asString.startsWith("sip:")) {
                            if (asString.length() != 4) {
                                asString = asString.substring(4);
                            }
                        }
                        appendLineWithCharsetAndQPDetection("X-SIP", asString);
                    } else {
                        if (!asString.startsWith("sip:")) {
                            asString = "sip:" + asString;
                        }
                        if (VCardConfig.isVersion40(this.mVCardType)) {
                            str = "TEL";
                        } else {
                            str = "IMPP";
                        }
                        appendLineWithCharsetAndQPDetection(str, asString);
                    }
                }
            }
        }
        return this;
    }

    public void appendAndroidSpecificProperty(String str, ContentValues contentValues) {
        String strEscapeCharacters;
        if (!sAllowedAndroidPropertySet.contains(str)) {
            return;
        }
        ArrayList<String> arrayList = new ArrayList();
        boolean z = true;
        for (int i = 1; i <= 15; i++) {
            String asString = contentValues.getAsString("data" + i);
            if (asString == null) {
                asString = "";
            }
            arrayList.add(asString);
        }
        boolean z2 = this.mShouldAppendCharsetParam && !VCardUtils.containsOnlyNonCrLfPrintableAscii(arrayList);
        if (!this.mShouldUseQuotedPrintable || VCardUtils.containsOnlyNonCrLfPrintableAscii(arrayList)) {
            z = false;
        }
        this.mBuilder.append("X-ANDROID-CUSTOM");
        if (z2) {
            this.mBuilder.append(";");
            this.mBuilder.append(this.mVCardCharsetParameter);
        }
        if (z) {
            this.mBuilder.append(";");
            this.mBuilder.append("ENCODING=QUOTED-PRINTABLE");
        }
        this.mBuilder.append(":");
        this.mBuilder.append(str);
        for (String str2 : arrayList) {
            if (z) {
                strEscapeCharacters = encodeQuotedPrintable(str2);
            } else {
                strEscapeCharacters = escapeCharacters(str2);
            }
            this.mBuilder.append(";");
            this.mBuilder.append(strEscapeCharacters);
        }
        this.mBuilder.append("\r\n");
    }

    public void appendLineWithCharsetAndQPDetection(String str, String str2) {
        appendLineWithCharsetAndQPDetection(str, null, str2);
    }

    public void appendLineWithCharsetAndQPDetection(String str, List<String> list, String str2) {
        appendLine(str, list, str2, !VCardUtils.containsOnlyPrintableAscii(str2), this.mShouldUseQuotedPrintable && !VCardUtils.containsOnlyNonCrLfPrintableAscii(str2));
    }

    public void appendLine(String str, String str2) {
        appendLine(str, str2, false, false);
    }

    public void appendLine(String str, String str2, boolean z, boolean z2) {
        appendLine(str, null, str2, z, z2);
    }

    public void appendLine(String str, List<String> list, String str2, boolean z, boolean z2) {
        String strEscapeCharacters;
        this.mBuilder.append(str);
        if (list != null && list.size() > 0) {
            this.mBuilder.append(";");
            appendTypeParameters(list);
        }
        if (z) {
            this.mBuilder.append(";");
            this.mBuilder.append(this.mVCardCharsetParameter);
        }
        if (z2) {
            this.mBuilder.append(";");
            this.mBuilder.append("ENCODING=QUOTED-PRINTABLE");
            strEscapeCharacters = encodeQuotedPrintable(str2);
        } else {
            strEscapeCharacters = escapeCharacters(str2);
        }
        this.mBuilder.append(":");
        this.mBuilder.append(strEscapeCharacters);
        this.mBuilder.append("\r\n");
    }

    private void appendTypeParameters(List<String> list) {
        String stringAsV30ParamValue;
        boolean z = true;
        for (String str : list) {
            if (VCardConfig.isVersion30(this.mVCardType) || VCardConfig.isVersion40(this.mVCardType)) {
                if (VCardConfig.isVersion40(this.mVCardType)) {
                    stringAsV30ParamValue = VCardUtils.toStringAsV40ParamValue(str);
                } else {
                    stringAsV30ParamValue = VCardUtils.toStringAsV30ParamValue(str);
                }
                if (!TextUtils.isEmpty(stringAsV30ParamValue)) {
                    if (!z) {
                        this.mBuilder.append(";");
                    } else {
                        z = false;
                    }
                    appendTypeParameter(stringAsV30ParamValue);
                }
            } else if (VCardUtils.isV21Word(str)) {
                if (!z) {
                    this.mBuilder.append(";");
                } else {
                    z = false;
                }
                appendTypeParameter(str);
            }
        }
    }

    private void appendTypeParameter(String str) {
        appendTypeParameter(this.mBuilder, str);
    }

    private void appendTypeParameter(StringBuilder sb, String str) {
        if (VCardConfig.isVersion40(this.mVCardType) || ((VCardConfig.isVersion30(this.mVCardType) || this.mAppendTypeParamName) && !this.mIsDoCoMo)) {
            sb.append("TYPE");
            sb.append("=");
        }
        sb.append(str);
    }

    private boolean shouldAppendCharsetParam(String... strArr) {
        if (!this.mShouldAppendCharsetParam) {
            return false;
        }
        for (String str : strArr) {
            if (!VCardUtils.containsOnlyPrintableAscii(str)) {
                return true;
            }
        }
        return false;
    }

    private String encodeQuotedPrintable(String str) {
        byte[] bytes;
        if (TextUtils.isEmpty(str)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        try {
            bytes = str.getBytes(this.mCharset);
        } catch (UnsupportedEncodingException e) {
            Log.e("MTK_vCard", "Charset " + this.mCharset + " cannot be used. Try default charset");
            bytes = str.getBytes();
        }
        int i = 0;
        int i2 = 0;
        while (i < bytes.length) {
            sb.append(String.format("=%02X", Byte.valueOf(bytes[i])));
            i++;
            i2 += 3;
            if (i2 >= 67) {
                sb.append("=\r\n");
                i2 = 0;
            }
        }
        return sb.toString();
    }

    private String escapeCharacters(String str) {
        if (TextUtils.isEmpty(str)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int length = str.length();
        for (int i = 0; i < length; i++) {
            char cCharAt = str.charAt(i);
            if (cCharAt == '\n') {
                sb.append("\\n");
            } else if (cCharAt != '\r') {
                if (cCharAt != ',') {
                    if (cCharAt == '>') {
                        if (this.mIsDoCoMo) {
                            sb.append('\\');
                            sb.append(cCharAt);
                        } else {
                            sb.append(cCharAt);
                        }
                    } else if (cCharAt != '\\') {
                        switch (cCharAt) {
                            case ';':
                                sb.append('\\');
                                sb.append(';');
                                break;
                            case '<':
                                break;
                            default:
                                sb.append(cCharAt);
                                break;
                        }
                    } else if (this.mIsV30OrV40) {
                        sb.append("\\\\");
                    }
                } else if (this.mIsV30OrV40) {
                    sb.append("\\,");
                } else {
                    sb.append(cCharAt);
                }
            } else if (i + 1 >= length || str.charAt(i) != '\n') {
            }
        }
        return sb.toString();
    }

    public String toString() {
        if (!this.mEndAppended) {
            if (this.mIsDoCoMo) {
                appendLine("X-CLASS", "PUBLIC");
                appendLine("X-REDUCTION", "");
                appendLine("X-NO", "");
                appendLine("X-DCM-HMN-MODE", "");
            }
            appendLine("END", "VCARD");
            this.mEndAppended = true;
        }
        return this.mBuilder.toString();
    }
}
