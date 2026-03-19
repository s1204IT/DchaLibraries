package com.android.browser.sitenavigation;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.util.Log;
import android.util.TypedValue;
import com.android.browser.R;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TemplateSiteNavigation {
    private static HashMap<Integer, TemplateSiteNavigation> sCachedTemplates = new HashMap<>();
    private static boolean sCountryChanged = false;
    private static String sCurrentCountry = "US";
    private HashMap<String, Object> mData;
    private List<Entity> mTemplate;

    interface Entity {
        void write(OutputStream outputStream, EntityData entityData) throws IOException;
    }

    interface EntityData {
        ListEntityIterator getListIterator(String str);

        void writeValue(OutputStream outputStream, String str) throws IOException;
    }

    interface ListEntityIterator extends EntityData {
        boolean moveToNext();

        void reset();
    }

    public static TemplateSiteNavigation getCachedTemplate(Context context, int i) {
        TemplateSiteNavigation templateSiteNavigationCopy;
        String displayCountry = context.getResources().getConfiguration().locale.getDisplayCountry();
        Log.d("@M_browser/TemplateSiteNavigation", "TemplateSiteNavigation.getCachedTemplate() display country :" + displayCountry + ", before country :" + sCurrentCountry);
        if (displayCountry != null && !displayCountry.equals(sCurrentCountry)) {
            sCountryChanged = true;
            sCurrentCountry = displayCountry;
        }
        synchronized (sCachedTemplates) {
            TemplateSiteNavigation templateSiteNavigation = sCachedTemplates.get(Integer.valueOf(i));
            if (templateSiteNavigation == null || sCountryChanged) {
                sCountryChanged = false;
                templateSiteNavigation = new TemplateSiteNavigation(context, i);
                sCachedTemplates.put(Integer.valueOf(i), templateSiteNavigation);
            }
            templateSiteNavigationCopy = templateSiteNavigation.copy();
        }
        return templateSiteNavigationCopy;
    }

    static class StringEntity implements Entity {
        byte[] mValue;

        public StringEntity(String str) {
            this.mValue = str.getBytes();
        }

        @Override
        public void write(OutputStream outputStream, EntityData entityData) throws IOException {
            outputStream.write(this.mValue);
        }
    }

    static class SimpleEntity implements Entity {
        String mKey;

        public SimpleEntity(String str) {
            this.mKey = str;
        }

        @Override
        public void write(OutputStream outputStream, EntityData entityData) throws IOException {
            entityData.writeValue(outputStream, this.mKey);
        }
    }

    static class ListEntity implements Entity {
        String mKey;
        TemplateSiteNavigation mSubTemplate;

        public ListEntity(Context context, String str, String str2) {
            this.mKey = str;
            this.mSubTemplate = new TemplateSiteNavigation(context, str2);
        }

        @Override
        public void write(OutputStream outputStream, EntityData entityData) throws IOException {
            ListEntityIterator listIterator = entityData.getListIterator(this.mKey);
            listIterator.reset();
            while (listIterator.moveToNext()) {
                this.mSubTemplate.write(outputStream, listIterator);
            }
        }
    }

    public static abstract class CursorListEntityWrapper implements ListEntityIterator {
        private Cursor mCursor;

        public CursorListEntityWrapper(Cursor cursor) {
            this.mCursor = cursor;
        }

        @Override
        public boolean moveToNext() {
            return this.mCursor.moveToNext();
        }

        @Override
        public void reset() {
            this.mCursor.moveToPosition(-1);
        }

        @Override
        public ListEntityIterator getListIterator(String str) {
            return null;
        }

        public Cursor getCursor() {
            return this.mCursor;
        }
    }

    static class HashMapEntityData implements EntityData {
        HashMap<String, Object> mData;

        public HashMapEntityData(HashMap<String, Object> map) {
            this.mData = map;
        }

        @Override
        public ListEntityIterator getListIterator(String str) {
            return (ListEntityIterator) this.mData.get(str);
        }

        @Override
        public void writeValue(OutputStream outputStream, String str) throws IOException {
            outputStream.write((byte[]) this.mData.get(str));
        }
    }

    private TemplateSiteNavigation(Context context, int i) {
        this(context, readRaw(context, i));
    }

    private TemplateSiteNavigation(Context context, String str) {
        this.mData = new HashMap<>();
        this.mTemplate = new ArrayList();
        parseTemplate(context, replaceConsts(context, str));
    }

    private TemplateSiteNavigation(TemplateSiteNavigation templateSiteNavigation) {
        this.mData = new HashMap<>();
        this.mTemplate = templateSiteNavigation.mTemplate;
    }

    TemplateSiteNavigation copy() {
        return new TemplateSiteNavigation(this);
    }

    void parseTemplate(Context context, String str) {
        Matcher matcher = Pattern.compile("<%([=\\{])\\s*(\\w+)\\s*%>").matcher(str);
        int iEnd = 0;
        while (matcher.find()) {
            String strSubstring = str.substring(iEnd, matcher.start());
            if (strSubstring.length() > 0) {
                this.mTemplate.add(new StringEntity(strSubstring));
            }
            String strGroup = matcher.group(1);
            String strGroup2 = matcher.group(2);
            if (strGroup.equals("=")) {
                this.mTemplate.add(new SimpleEntity(strGroup2));
            } else if (strGroup.equals("{")) {
                Matcher matcher2 = Pattern.compile("<%\\}\\s*" + Pattern.quote(strGroup2) + "\\s*%>").matcher(str);
                if (matcher2.find(matcher.end())) {
                    int iEnd2 = matcher.end();
                    matcher.region(matcher2.end(), str.length());
                    this.mTemplate.add(new ListEntity(context, strGroup2, str.substring(iEnd2, matcher2.start())));
                    iEnd = matcher2.end();
                }
            }
            iEnd = matcher.end();
        }
        String strSubstring2 = str.substring(iEnd, str.length());
        if (strSubstring2.length() > 0) {
            this.mTemplate.add(new StringEntity(strSubstring2));
        }
    }

    public void assignLoop(String str, ListEntityIterator listEntityIterator) {
        this.mData.put(str, listEntityIterator);
    }

    public void write(OutputStream outputStream) throws IOException {
        write(outputStream, new HashMapEntityData(this.mData));
    }

    public void write(OutputStream outputStream, EntityData entityData) throws IOException {
        Iterator<Entity> it = this.mTemplate.iterator();
        while (it.hasNext()) {
            it.next().write(outputStream, entityData);
        }
    }

    private static String replaceConsts(Context context, String str) {
        String string;
        Pattern patternCompile = Pattern.compile("<%@\\s*(\\w+/\\w+)\\s*%>");
        Resources resources = context.getResources();
        String name = R.class.getPackage().getName();
        Matcher matcher = patternCompile.matcher(str);
        StringBuffer stringBuffer = new StringBuffer();
        while (matcher.find()) {
            String strGroup = matcher.group(1);
            if (strGroup.startsWith("drawable/")) {
                matcher.appendReplacement(stringBuffer, "res/" + strGroup);
            } else {
                int identifier = resources.getIdentifier(strGroup, null, name);
                if (identifier != 0) {
                    TypedValue typedValue = new TypedValue();
                    resources.getValue(identifier, typedValue, true);
                    if (typedValue.type == 5) {
                        float dimension = resources.getDimension(identifier);
                        int i = (int) dimension;
                        if (i == dimension) {
                            string = Integer.toString(i);
                        } else {
                            string = Float.toString(dimension);
                        }
                    } else {
                        string = typedValue.coerceToString().toString();
                    }
                    matcher.appendReplacement(stringBuffer, string);
                }
            }
        }
        matcher.appendTail(stringBuffer);
        return stringBuffer.toString();
    }

    private static String readRaw(Context context, int i) {
        InputStream inputStreamOpenRawResource = context.getResources().openRawResource(i);
        try {
            byte[] bArr = new byte[inputStreamOpenRawResource.available()];
            inputStreamOpenRawResource.read(bArr);
            inputStreamOpenRawResource.close();
            return new String(bArr, "utf-8");
        } catch (IOException e) {
            return "<html><body>Error</body></html>";
        }
    }
}
