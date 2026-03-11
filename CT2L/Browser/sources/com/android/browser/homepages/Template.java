package com.android.browser.homepages;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
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

public class Template {
    private static HashMap<Integer, Template> sCachedTemplates = new HashMap<>();
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

    public static Template getCachedTemplate(Context context, int id) {
        Template templateCopy;
        synchronized (sCachedTemplates) {
            Template template = sCachedTemplates.get(Integer.valueOf(id));
            if (template == null) {
                template = new Template(context, id);
                sCachedTemplates.put(Integer.valueOf(id), template);
            }
            templateCopy = template.copy();
        }
        return templateCopy;
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
        Template mSubTemplate;

        public ListEntity(Context context, String key, String subTemplate) {
            this.mKey = key;
            this.mSubTemplate = new Template(context, subTemplate);
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

    private Template(Context context, int tid) {
        this(context, readRaw(context, tid));
    }

    private Template(Context context, String template) {
        this.mData = new HashMap<>();
        this.mTemplate = new ArrayList();
        parseTemplate(context, replaceConsts(context, template));
    }

    private Template(Template copy) {
        this.mData = new HashMap<>();
        this.mTemplate = copy.mTemplate;
    }

    Template copy() {
        return new Template(this);
    }

    void parseTemplate(Context context, String template) {
        Pattern pattern = Pattern.compile("<%([=\\{])\\s*(\\w+)\\s*%>");
        Matcher m = pattern.matcher(template);
        int start = 0;
        while (m.find()) {
            String static_part = template.substring(start, m.start());
            if (static_part.length() > 0) {
                this.mTemplate.add(new StringEntity(static_part));
            }
            String type = m.group(1);
            String name = m.group(2);
            if (type.equals("=")) {
                this.mTemplate.add(new SimpleEntity(name));
            } else if (type.equals("{")) {
                Pattern p = Pattern.compile("<%\\}\\s*" + Pattern.quote(name) + "\\s*%>");
                Matcher end_m = p.matcher(template);
                if (end_m.find(m.end())) {
                    int start2 = m.end();
                    m.region(end_m.end(), template.length());
                    String subTemplate = template.substring(start2, end_m.start());
                    this.mTemplate.add(new ListEntity(context, name, subTemplate));
                    start = end_m.end();
                }
            }
            start = m.end();
        }
        String static_part2 = template.substring(start, template.length());
        if (static_part2.length() > 0) {
            this.mTemplate.add(new StringEntity(static_part2));
        }
    }

    public void assign(String name, String value) {
        this.mData.put(name, value.getBytes());
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
            return new String(buf, "utf-8");
        } catch (IOException e) {
            return "<html><body>Error</body></html>";
        }
    }
}
