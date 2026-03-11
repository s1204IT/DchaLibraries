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

    TemplateSiteNavigation(Context context, String template, TemplateSiteNavigation templateSiteNavigation) {
        this(context, template);
    }

    public static TemplateSiteNavigation getCachedTemplate(Context context, int id) {
        TemplateSiteNavigation templateSiteNavigationCopy;
        String changeToCountry = context.getResources().getConfiguration().locale.getDisplayCountry();
        Log.d("@M_browser/TemplateSiteNavigation", "TemplateSiteNavigation.getCachedTemplate() display country :" + changeToCountry + ", before country :" + sCurrentCountry);
        if (changeToCountry != null && !changeToCountry.equals(sCurrentCountry)) {
            sCountryChanged = true;
            sCurrentCountry = changeToCountry;
        }
        synchronized (sCachedTemplates) {
            TemplateSiteNavigation template = sCachedTemplates.get(Integer.valueOf(id));
            if (template == null || sCountryChanged) {
                sCountryChanged = false;
                template = new TemplateSiteNavigation(context, id);
                sCachedTemplates.put(Integer.valueOf(id), template);
            }
            templateSiteNavigationCopy = template.copy();
        }
        return templateSiteNavigationCopy;
    }

    static class StringEntity implements Entity {
        byte[] mValue;

        public StringEntity(String value) {
            this.mValue = value.getBytes();
        }

        @Override
        public void write(OutputStream stream, EntityData params) throws IOException {
            stream.write(this.mValue);
        }
    }

    static class SimpleEntity implements Entity {
        String mKey;

        public SimpleEntity(String key) {
            this.mKey = key;
        }

        @Override
        public void write(OutputStream stream, EntityData params) throws IOException {
            params.writeValue(stream, this.mKey);
        }
    }

    static class ListEntity implements Entity {
        String mKey;
        TemplateSiteNavigation mSubTemplate;

        public ListEntity(Context context, String key, String subTemplate) {
            this.mKey = key;
            this.mSubTemplate = new TemplateSiteNavigation(context, subTemplate, null);
        }

        @Override
        public void write(OutputStream stream, EntityData params) throws IOException {
            ListEntityIterator iter = params.getListIterator(this.mKey);
            iter.reset();
            while (iter.moveToNext()) {
                this.mSubTemplate.write(stream, iter);
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
        public ListEntityIterator getListIterator(String key) {
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
        public ListEntityIterator getListIterator(String key) {
            return (ListEntityIterator) this.mData.get(key);
        }

        @Override
        public void writeValue(OutputStream stream, String key) throws IOException {
            stream.write((byte[]) this.mData.get(key));
        }
    }

    private TemplateSiteNavigation(Context context, int tid) {
        this(context, readRaw(context, tid));
    }

    private TemplateSiteNavigation(Context context, String template) {
        this.mData = new HashMap<>();
        this.mTemplate = new ArrayList();
        parseTemplate(context, replaceConsts(context, template));
    }

    private TemplateSiteNavigation(TemplateSiteNavigation copy) {
        this.mData = new HashMap<>();
        this.mTemplate = copy.mTemplate;
    }

    TemplateSiteNavigation copy() {
        return new TemplateSiteNavigation(this);
    }

    void parseTemplate(Context context, String template) {
        Pattern pattern = Pattern.compile("<%([=\\{])\\s*(\\w+)\\s*%>");
        Matcher m = pattern.matcher(template);
        int start = 0;
        while (m.find()) {
            String staticPart = template.substring(start, m.start());
            if (staticPart.length() > 0) {
                this.mTemplate.add(new StringEntity(staticPart));
            }
            String type = m.group(1);
            String name = m.group(2);
            if (type.equals("=")) {
                this.mTemplate.add(new SimpleEntity(name));
            } else if (type.equals("{")) {
                Pattern p = Pattern.compile("<%\\}\\s*" + Pattern.quote(name) + "\\s*%>");
                Matcher end = p.matcher(template);
                if (end.find(m.end())) {
                    int start2 = m.end();
                    m.region(end.end(), template.length());
                    String subTemplate = template.substring(start2, end.start());
                    this.mTemplate.add(new ListEntity(context, name, subTemplate));
                    start = end.end();
                }
            }
            start = m.end();
        }
        String staticPart2 = template.substring(start, template.length());
        if (staticPart2.length() <= 0) {
            return;
        }
        this.mTemplate.add(new StringEntity(staticPart2));
    }

    public void assignLoop(String name, ListEntityIterator iter) {
        this.mData.put(name, iter);
    }

    public void write(OutputStream stream) throws IOException {
        write(stream, new HashMapEntityData(this.mData));
    }

    public void write(OutputStream stream, EntityData data) throws IOException {
        for (Entity ent : this.mTemplate) {
            ent.write(stream, data);
        }
    }

    private static String replaceConsts(Context context, String template) {
        String replacement;
        Pattern pattern = Pattern.compile("<%@\\s*(\\w+/\\w+)\\s*%>");
        Resources res = context.getResources();
        String packageName = R.class.getPackage().getName();
        Matcher m = pattern.matcher(template);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String name = m.group(1);
            if (name.startsWith("drawable/")) {
                m.appendReplacement(sb, "res/" + name);
            } else {
                int id = res.getIdentifier(name, null, packageName);
                if (id != 0) {
                    TypedValue value = new TypedValue();
                    res.getValue(id, value, true);
                    if (value.type == 5) {
                        float dimen = res.getDimension(id);
                        int dimeni = (int) dimen;
                        if (dimeni == dimen) {
                            replacement = Integer.toString(dimeni);
                        } else {
                            replacement = Float.toString(dimen);
                        }
                    } else {
                        replacement = value.coerceToString().toString();
                    }
                    m.appendReplacement(sb, replacement);
                }
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String readRaw(Context context, int id) {
        InputStream ins = context.getResources().openRawResource(id);
        try {
            byte[] buf = new byte[ins.available()];
            ins.read(buf);
            ins.close();
            return new String(buf, "utf-8");
        } catch (IOException e) {
            return "<html><body>Error</body></html>";
        }
    }
}
