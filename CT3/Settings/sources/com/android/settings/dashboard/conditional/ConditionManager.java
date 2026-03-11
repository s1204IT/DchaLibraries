package com.android.settings.dashboard.conditional;

import android.content.Context;
import android.os.AsyncTask;
import android.os.PersistableBundle;
import android.util.Log;
import android.util.Xml;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class ConditionManager {
    private static final Comparator<Condition> CONDITION_COMPARATOR = new Comparator<Condition>() {
        @Override
        public int compare(Condition lhs, Condition rhs) {
            return Long.compare(lhs.getLastChange(), rhs.getLastChange());
        }
    };
    private static ConditionManager sInstance;
    private final Context mContext;
    private File mXmlFile;
    private final ArrayList<ConditionListener> mListeners = new ArrayList<>();
    private final ArrayList<Condition> mConditions = new ArrayList<>();

    public interface ConditionListener {
        void onConditionsChanged();
    }

    private ConditionManager(Context context, boolean loadConditionsNow) {
        ConditionLoader conditionLoader = null;
        this.mContext = context;
        if (loadConditionsNow) {
            ConditionLoader loader = new ConditionLoader(this, conditionLoader);
            loader.onPostExecute(loader.doInBackground(new Void[0]));
        } else {
            new ConditionLoader(this, conditionLoader).execute(new Void[0]);
        }
    }

    public void refreshAll() {
        ArrayList<Condition> list = new ArrayList<>(this.mConditions);
        int N = list.size();
        for (int i = 0; i < N; i++) {
            list.get(i).refreshState();
        }
    }

    public void readFromXml(File xmlFile, ArrayList<Condition> conditions) {
        try {
            XmlPullParser parser = Xml.newPullParser();
            FileReader in = new FileReader(xmlFile);
            parser.setInput(in);
            for (int state = parser.getEventType(); state != 1; state = parser.next()) {
                if ("c".equals(parser.getName())) {
                    int depth = parser.getDepth();
                    String clz = parser.getAttributeValue("", "cls");
                    if (!clz.startsWith("com.android.settings.dashboard.conditional.")) {
                        clz = "com.android.settings.dashboard.conditional." + clz;
                    }
                    Condition condition = createCondition(Class.forName(clz));
                    PersistableBundle bundle = PersistableBundle.restoreFromXml(parser);
                    condition.restoreState(bundle);
                    conditions.add(condition);
                    while (parser.getDepth() > depth) {
                        parser.next();
                    }
                }
            }
            in.close();
        } catch (IOException | ClassNotFoundException | XmlPullParserException e) {
            Log.w("ConditionManager", "Problem reading condition_state.xml", e);
        }
    }

    private void saveToXml() {
        try {
            XmlSerializer serializer = Xml.newSerializer();
            FileWriter writer = new FileWriter(this.mXmlFile);
            serializer.setOutput(writer);
            serializer.startDocument("UTF-8", true);
            serializer.startTag("", "cs");
            int N = this.mConditions.size();
            for (int i = 0; i < N; i++) {
                PersistableBundle bundle = new PersistableBundle();
                if (this.mConditions.get(i).saveState(bundle)) {
                    serializer.startTag("", "c");
                    String clz = this.mConditions.get(i).getClass().getSimpleName();
                    serializer.attribute("", "cls", clz);
                    bundle.saveToXml(serializer);
                    serializer.endTag("", "c");
                }
            }
            serializer.endTag("", "cs");
            serializer.flush();
            writer.close();
        } catch (IOException | XmlPullParserException e) {
            Log.w("ConditionManager", "Problem writing condition_state.xml", e);
        }
    }

    public void addMissingConditions(ArrayList<Condition> conditions) {
        addIfMissing(AirplaneModeCondition.class, conditions);
        addIfMissing(HotspotCondition.class, conditions);
        addIfMissing(DndCondition.class, conditions);
        addIfMissing(BatterySaverCondition.class, conditions);
        addIfMissing(CellularDataCondition.class, conditions);
        addIfMissing(BackgroundDataCondition.class, conditions);
        addIfMissing(WorkModeCondition.class, conditions);
        Collections.sort(conditions, CONDITION_COMPARATOR);
    }

    private void addIfMissing(Class<? extends Condition> clz, ArrayList<Condition> conditions) {
        if (getCondition(clz, conditions) != null) {
            return;
        }
        conditions.add(createCondition(clz));
    }

    private Condition createCondition(Class<?> clz) {
        if (AirplaneModeCondition.class == clz) {
            return new AirplaneModeCondition(this);
        }
        if (HotspotCondition.class == clz) {
            return new HotspotCondition(this);
        }
        if (DndCondition.class == clz) {
            return new DndCondition(this);
        }
        if (BatterySaverCondition.class == clz) {
            return new BatterySaverCondition(this);
        }
        if (CellularDataCondition.class == clz) {
            return new CellularDataCondition(this);
        }
        if (BackgroundDataCondition.class == clz) {
            return new BackgroundDataCondition(this);
        }
        if (WorkModeCondition.class == clz) {
            return new WorkModeCondition(this);
        }
        throw new RuntimeException("Unexpected Condition " + clz);
    }

    Context getContext() {
        return this.mContext;
    }

    public <T extends Condition> T getCondition(Class<T> cls) {
        return (T) getCondition(cls, this.mConditions);
    }

    private <T extends Condition> T getCondition(Class<T> clz, List<Condition> conditions) {
        int N = conditions.size();
        for (int i = 0; i < N; i++) {
            if (clz.equals(conditions.get(i).getClass())) {
                return (T) conditions.get(i);
            }
        }
        return null;
    }

    public List<Condition> getConditions() {
        return this.mConditions;
    }

    public void notifyChanged(Condition condition) {
        saveToXml();
        Collections.sort(this.mConditions, CONDITION_COMPARATOR);
        int N = this.mListeners.size();
        for (int i = 0; i < N; i++) {
            this.mListeners.get(i).onConditionsChanged();
        }
    }

    public void addListener(ConditionListener listener) {
        this.mListeners.add(listener);
        listener.onConditionsChanged();
    }

    public void remListener(ConditionListener listener) {
        this.mListeners.remove(listener);
    }

    private class ConditionLoader extends AsyncTask<Void, Void, ArrayList<Condition>> {
        ConditionLoader(ConditionManager this$0, ConditionLoader conditionLoader) {
            this();
        }

        private ConditionLoader() {
        }

        @Override
        public ArrayList<Condition> doInBackground(Void... params) {
            ArrayList<Condition> conditions = new ArrayList<>();
            ConditionManager.this.mXmlFile = new File(ConditionManager.this.mContext.getFilesDir(), "condition_state.xml");
            if (ConditionManager.this.mXmlFile.exists()) {
                ConditionManager.this.readFromXml(ConditionManager.this.mXmlFile, conditions);
            }
            ConditionManager.this.addMissingConditions(conditions);
            return conditions;
        }

        @Override
        public void onPostExecute(ArrayList<Condition> conditions) {
            ConditionManager.this.mConditions.clear();
            ConditionManager.this.mConditions.addAll(conditions);
            int N = ConditionManager.this.mListeners.size();
            for (int i = 0; i < N; i++) {
                ((ConditionListener) ConditionManager.this.mListeners.get(i)).onConditionsChanged();
            }
        }
    }

    public static ConditionManager get(Context context) {
        return get(context, true);
    }

    public static ConditionManager get(Context context, boolean loadConditionsNow) {
        if (sInstance == null) {
            sInstance = new ConditionManager(context.getApplicationContext(), loadConditionsNow);
        }
        return sInstance;
    }
}
