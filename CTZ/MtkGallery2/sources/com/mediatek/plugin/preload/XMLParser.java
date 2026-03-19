package com.mediatek.plugin.preload;

import com.mediatek.plugin.builder.Builder;
import com.mediatek.plugin.builder.BuilderFactory;
import com.mediatek.plugin.element.Element;
import com.mediatek.plugin.res.IResource;
import com.mediatek.plugin.utils.Log;
import com.mediatek.plugin.utils.TraceHelper;
import java.io.IOException;
import java.io.InputStream;
import java.util.Stack;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

class XMLParser {
    static final boolean $assertionsDisabled = false;
    private static final String TAG = "PluginManager/XMLParser";
    private InputStream mInputStream;
    private IResource mResParser;

    public XMLParser(InputStream inputStream, IResource iResource) {
        this.mResParser = null;
        this.mInputStream = inputStream;
        this.mResParser = iResource;
    }

    public Element parserXML() {
        XmlPullParser xmlPullParserNewPullParser;
        int next;
        Builder builder;
        Element element = null;
        if (this.mInputStream == null) {
            return null;
        }
        TraceHelper.beginSection(">>>>XMLParser-parserXML");
        Element element2 = new Element();
        element2.id = "Root";
        Stack stack = new Stack();
        try {
            TraceHelper.beginSection(">>>>XMLParser-parserXML-new XmlPullParserFactory");
            XmlPullParserFactory xmlPullParserFactoryNewInstance = XmlPullParserFactory.newInstance();
            TraceHelper.endSection();
            TraceHelper.beginSection(">>>>XMLParser-parserXML-new XmlPullParser");
            xmlPullParserNewPullParser = xmlPullParserFactoryNewInstance.newPullParser();
            TraceHelper.endSection();
            TraceHelper.beginSection(">>>>XMLParser-parserXML-setInput");
            xmlPullParserNewPullParser.setInput(this.mInputStream, "UTF-8");
            TraceHelper.endSection();
            next = -1;
            builder = null;
        } catch (XmlPullParserException e) {
            e = e;
        }
        while (next != 1) {
            try {
                TraceHelper.beginSection(">>>>XMLParser-parserXML-while");
                TraceHelper.beginSection(">>>>XMLParser-parserXML-while-getEventType");
                int eventType = xmlPullParserNewPullParser.getEventType();
                TraceHelper.endSection();
                Log.d(TAG, "<parserXML> " + xmlPullParserNewPullParser.getName() + " || eventType = " + eventType + " stack = " + stack.size());
                switch (eventType) {
                    case 0:
                        stack.push(element2);
                        TraceHelper.beginSection(">>>>XMLParser-parserXML-while-next");
                        next = xmlPullParserNewPullParser.next();
                        TraceHelper.endSection();
                        TraceHelper.endSection();
                        break;
                    case 1:
                        stack.clear();
                        TraceHelper.beginSection(">>>>XMLParser-parserXML-while-next");
                        next = xmlPullParserNewPullParser.next();
                        TraceHelper.endSection();
                        TraceHelper.endSection();
                        break;
                    case 2:
                        Builder builder2 = BuilderFactory.getBuilder(xmlPullParserNewPullParser.getName());
                        Log.d(TAG, "<parserXML> builder = " + builder2);
                        if (builder2 != null) {
                            Element rVar = builder2.parser(xmlPullParserNewPullParser, this.mResParser);
                            if (rVar != null) {
                                try {
                                    stack.push(rVar);
                                } catch (IOException e2) {
                                    e = e2;
                                    element = rVar;
                                    Log.d(TAG, "<parserXML>", e);
                                    TraceHelper.endSection();
                                    return element;
                                } catch (XmlPullParserException e3) {
                                    e = e3;
                                    element = rVar;
                                    Log.d(TAG, "<parserXML>", e);
                                    TraceHelper.endSection();
                                    return element;
                                }
                            }
                            element = rVar;
                        }
                        builder = builder2;
                        TraceHelper.beginSection(">>>>XMLParser-parserXML-while-next");
                        next = xmlPullParserNewPullParser.next();
                        TraceHelper.endSection();
                        TraceHelper.endSection();
                        break;
                    case 3:
                        Element element3 = (Element) stack.pop();
                        try {
                            Element element4 = (Element) stack.peek();
                            if (builder != null && element4 != null && element3 != null) {
                                builder.bind(element4, element3);
                            }
                            element = element3;
                            TraceHelper.beginSection(">>>>XMLParser-parserXML-while-next");
                            next = xmlPullParserNewPullParser.next();
                            TraceHelper.endSection();
                            TraceHelper.endSection();
                        } catch (IOException e4) {
                            e = e4;
                            element = element3;
                            Log.d(TAG, "<parserXML>", e);
                            TraceHelper.endSection();
                            return element;
                        } catch (XmlPullParserException e5) {
                            e = e5;
                            element = element3;
                            Log.d(TAG, "<parserXML>", e);
                            TraceHelper.endSection();
                            return element;
                        }
                        break;
                    default:
                        TraceHelper.beginSection(">>>>XMLParser-parserXML-while-next");
                        next = xmlPullParserNewPullParser.next();
                        TraceHelper.endSection();
                        TraceHelper.endSection();
                        break;
                }
            } catch (IOException e6) {
                e = e6;
            }
            TraceHelper.endSection();
            return element;
        }
        TraceHelper.endSection();
        return element;
    }
}
