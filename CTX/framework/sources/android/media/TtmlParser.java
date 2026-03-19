package android.media;

import android.net.wifi.WifiEnterpriseConfig;
import android.text.TextUtils;
import android.util.Log;
import java.io.IOException;
import java.io.StringReader;
import java.util.LinkedList;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

class TtmlParser {
    private static final int DEFAULT_FRAMERATE = 30;
    private static final int DEFAULT_SUBFRAMERATE = 1;
    private static final int DEFAULT_TICKRATE = 1;
    static final String TAG = "TtmlParser";
    private long mCurrentRunId;
    private final TtmlNodeListener mListener;
    private XmlPullParser mParser;

    public TtmlParser(TtmlNodeListener ttmlNodeListener) {
        this.mListener = ttmlNodeListener;
    }

    public void parse(String str, long j) throws XmlPullParserException, IOException {
        this.mParser = null;
        this.mCurrentRunId = j;
        loadParser(str);
        parseTtml();
    }

    private void loadParser(String str) throws XmlPullParserException {
        XmlPullParserFactory xmlPullParserFactoryNewInstance = XmlPullParserFactory.newInstance();
        xmlPullParserFactoryNewInstance.setNamespaceAware(false);
        this.mParser = xmlPullParserFactoryNewInstance.newPullParser();
        this.mParser.setInput(new StringReader(str));
    }

    private void extractAttribute(XmlPullParser xmlPullParser, int i, StringBuilder sb) {
        sb.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
        sb.append(xmlPullParser.getAttributeName(i));
        sb.append("=\"");
        sb.append(xmlPullParser.getAttributeValue(i));
        sb.append("\"");
    }

    private void parseTtml() throws XmlPullParserException, IOException {
        LinkedList linkedList = new LinkedList();
        boolean z = true;
        int i = 0;
        while (!isEndOfDoc()) {
            int eventType = this.mParser.getEventType();
            TtmlNode ttmlNode = (TtmlNode) linkedList.peekLast();
            if (z) {
                if (eventType == 2) {
                    if (!isSupportedTag(this.mParser.getName())) {
                        Log.w(TAG, "Unsupported tag " + this.mParser.getName() + " is ignored.");
                        i++;
                        z = false;
                    } else {
                        TtmlNode node = parseNode(ttmlNode);
                        linkedList.addLast(node);
                        if (ttmlNode != null) {
                            ttmlNode.mChildren.add(node);
                        }
                    }
                } else if (eventType != 4) {
                    if (eventType == 3) {
                        if (this.mParser.getName().equals(TtmlUtils.TAG_P)) {
                            this.mListener.onTtmlNodeParsed((TtmlNode) linkedList.getLast());
                        } else if (this.mParser.getName().equals(TtmlUtils.TAG_TT)) {
                            this.mListener.onRootNodeParsed((TtmlNode) linkedList.getLast());
                        }
                        linkedList.removeLast();
                    }
                } else {
                    String strApplyDefaultSpacePolicy = TtmlUtils.applyDefaultSpacePolicy(this.mParser.getText());
                    if (!TextUtils.isEmpty(strApplyDefaultSpacePolicy)) {
                        ttmlNode.mChildren.add(new TtmlNode(TtmlUtils.PCDATA, "", strApplyDefaultSpacePolicy, 0L, Long.MAX_VALUE, ttmlNode, this.mCurrentRunId));
                    }
                }
            } else if (eventType == 2) {
                i++;
            } else if (eventType == 3 && i - 1 == 0) {
                z = true;
            }
            this.mParser.next();
        }
    }

    private TtmlNode parseNode(TtmlNode ttmlNode) throws XmlPullParserException, IOException {
        if (this.mParser.getEventType() != 2) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        long timeExpression = 0;
        long timeExpression2 = Long.MAX_VALUE;
        long timeExpression3 = 0;
        for (int i = 0; i < this.mParser.getAttributeCount(); i++) {
            String attributeName = this.mParser.getAttributeName(i);
            String attributeValue = this.mParser.getAttributeValue(i);
            String strReplaceFirst = attributeName.replaceFirst("^.*:", "");
            if (strReplaceFirst.equals("begin")) {
                timeExpression = TtmlUtils.parseTimeExpression(attributeValue, 30, 1, 1);
            } else if (strReplaceFirst.equals("end")) {
                timeExpression2 = TtmlUtils.parseTimeExpression(attributeValue, 30, 1, 1);
            } else if (strReplaceFirst.equals(TtmlUtils.ATTR_DURATION)) {
                timeExpression3 = TtmlUtils.parseTimeExpression(attributeValue, 30, 1, 1);
            } else {
                extractAttribute(this.mParser, i, sb);
            }
        }
        if (ttmlNode != null) {
            timeExpression += ttmlNode.mStartTimeMs;
            if (timeExpression2 != Long.MAX_VALUE) {
                timeExpression2 += ttmlNode.mStartTimeMs;
            }
        }
        long j = timeExpression;
        if (timeExpression3 > 0) {
            if (timeExpression2 != Long.MAX_VALUE) {
                Log.e(TAG, "'dur' and 'end' attributes are defined at the same time.'end' value is ignored.");
            }
            timeExpression2 = j + timeExpression3;
        }
        return new TtmlNode(this.mParser.getName(), sb.toString(), null, j, (ttmlNode == null || timeExpression2 != Long.MAX_VALUE || ttmlNode.mEndTimeMs == Long.MAX_VALUE || timeExpression2 <= ttmlNode.mEndTimeMs) ? timeExpression2 : ttmlNode.mEndTimeMs, ttmlNode, this.mCurrentRunId);
    }

    private boolean isEndOfDoc() throws XmlPullParserException {
        return this.mParser.getEventType() == 1;
    }

    private static boolean isSupportedTag(String str) {
        if (str.equals(TtmlUtils.TAG_TT) || str.equals(TtmlUtils.TAG_HEAD) || str.equals("body") || str.equals(TtmlUtils.TAG_DIV) || str.equals(TtmlUtils.TAG_P) || str.equals(TtmlUtils.TAG_SPAN) || str.equals(TtmlUtils.TAG_BR) || str.equals(TtmlUtils.TAG_STYLE) || str.equals(TtmlUtils.TAG_STYLING) || str.equals(TtmlUtils.TAG_LAYOUT) || str.equals(TtmlUtils.TAG_REGION) || str.equals(TtmlUtils.TAG_METADATA) || str.equals(TtmlUtils.TAG_SMPTE_IMAGE) || str.equals(TtmlUtils.TAG_SMPTE_DATA) || str.equals(TtmlUtils.TAG_SMPTE_INFORMATION)) {
            return true;
        }
        return false;
    }
}
