package com.android.server.firewall;

import android.app.AppGlobals;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.os.Environment;
import android.os.FileObserver;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.ArrayMap;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.XmlUtils;
import com.android.server.EventLogTags;
import com.android.server.IntentResolver;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class IntentFirewall {
    private static final int LOG_PACKAGES_MAX_LENGTH = 150;
    private static final int LOG_PACKAGES_SUFFICIENT_LENGTH = 125;
    private static final File RULES_DIR = new File(Environment.getDataSystemDirectory(), "ifw");
    static final String TAG = "IntentFirewall";
    private static final String TAG_ACTIVITY = "activity";
    private static final String TAG_BROADCAST = "broadcast";
    private static final String TAG_RULES = "rules";
    private static final String TAG_SERVICE = "service";
    private static final int TYPE_ACTIVITY = 0;
    private static final int TYPE_BROADCAST = 1;
    private static final int TYPE_SERVICE = 2;
    private static final HashMap<String, FilterFactory> factoryMap;
    private FirewallIntentResolver mActivityResolver;
    private final AMSInterface mAms;
    private FirewallIntentResolver mBroadcastResolver;
    final FirewallHandler mHandler;
    private final RuleObserver mObserver;
    private FirewallIntentResolver mServiceResolver;

    public interface AMSInterface {
        int checkComponentPermission(String str, int i, int i2, int i3, boolean z);

        Object getAMSLock();
    }

    static {
        FilterFactory[] filterFactoryArr = {AndFilter.FACTORY, OrFilter.FACTORY, NotFilter.FACTORY, StringFilter.ACTION, StringFilter.COMPONENT, StringFilter.COMPONENT_NAME, StringFilter.COMPONENT_PACKAGE, StringFilter.DATA, StringFilter.HOST, StringFilter.MIME_TYPE, StringFilter.SCHEME, StringFilter.PATH, StringFilter.SSP, CategoryFilter.FACTORY, SenderFilter.FACTORY, SenderPackageFilter.FACTORY, SenderPermissionFilter.FACTORY, PortFilter.FACTORY};
        factoryMap = new HashMap<>((filterFactoryArr.length * 4) / 3);
        for (FilterFactory filterFactory : filterFactoryArr) {
            factoryMap.put(filterFactory.getTagName(), filterFactory);
        }
    }

    public IntentFirewall(AMSInterface aMSInterface, Handler handler) {
        this.mActivityResolver = new FirewallIntentResolver();
        this.mBroadcastResolver = new FirewallIntentResolver();
        this.mServiceResolver = new FirewallIntentResolver();
        this.mAms = aMSInterface;
        this.mHandler = new FirewallHandler(handler.getLooper());
        File rulesDir = getRulesDir();
        rulesDir.mkdirs();
        readRulesDir(rulesDir);
        this.mObserver = new RuleObserver(rulesDir);
        this.mObserver.startWatching();
    }

    public boolean checkStartActivity(Intent intent, int i, int i2, String str, ApplicationInfo applicationInfo) {
        return checkIntent(this.mActivityResolver, intent.getComponent(), 0, intent, i, i2, str, applicationInfo.uid);
    }

    public boolean checkService(ComponentName componentName, Intent intent, int i, int i2, String str, ApplicationInfo applicationInfo) {
        return checkIntent(this.mServiceResolver, componentName, 2, intent, i, i2, str, applicationInfo.uid);
    }

    public boolean checkBroadcast(Intent intent, int i, int i2, String str, int i3) {
        return checkIntent(this.mBroadcastResolver, intent.getComponent(), 1, intent, i, i2, str, i3);
    }

    public boolean checkIntent(FirewallIntentResolver firewallIntentResolver, ComponentName componentName, int i, Intent intent, int i2, int i3, String str, int i4) {
        List<Rule> listQueryIntent = firewallIntentResolver.queryIntent(intent, str, false, 0);
        if (listQueryIntent == null) {
            listQueryIntent = new ArrayList<>();
        }
        List<Rule> list = listQueryIntent;
        firewallIntentResolver.queryByComponent(componentName, list);
        boolean block = false;
        boolean log = false;
        for (int i5 = 0; i5 < list.size(); i5++) {
            Rule rule = list.get(i5);
            if (rule.matches(this, componentName, intent, i2, i3, str, i4)) {
                block |= rule.getBlock();
                log |= rule.getLog();
                if (block && log) {
                    break;
                }
            }
        }
        if (log) {
            logIntent(i, intent, i2, str);
        }
        return !block;
    }

    private static void logIntent(int i, Intent intent, int i2, String str) {
        String strFlattenToShortString;
        ComponentName component = intent.getComponent();
        String strJoinPackages = null;
        if (component == null) {
            strFlattenToShortString = null;
        } else {
            strFlattenToShortString = component.flattenToShortString();
        }
        int i3 = 0;
        IPackageManager packageManager = AppGlobals.getPackageManager();
        if (packageManager != null) {
            try {
                String[] packagesForUid = packageManager.getPackagesForUid(i2);
                if (packagesForUid != null) {
                    int length = packagesForUid.length;
                    try {
                        strJoinPackages = joinPackages(packagesForUid);
                        i3 = length;
                    } catch (RemoteException e) {
                        e = e;
                        i3 = length;
                        Slog.e(TAG, "Remote exception while retrieving packages", e);
                    }
                }
            } catch (RemoteException e2) {
                e = e2;
            }
        }
        EventLogTags.writeIfwIntentMatched(i, strFlattenToShortString, i2, i3, strJoinPackages, intent.getAction(), str, intent.getDataString(), intent.getFlags());
    }

    private static String joinPackages(String[] strArr) {
        StringBuilder sb = new StringBuilder();
        boolean z = true;
        for (String str : strArr) {
            if (sb.length() + str.length() + 1 < 150) {
                if (!z) {
                    sb.append(',');
                } else {
                    z = false;
                }
                sb.append(str);
            } else if (sb.length() >= LOG_PACKAGES_SUFFICIENT_LENGTH) {
                return sb.toString();
            }
        }
        if (sb.length() == 0 && strArr.length > 0) {
            String str2 = strArr[0];
            return str2.substring((str2.length() - 150) + 1) + '-';
        }
        return null;
    }

    public static File getRulesDir() {
        return RULES_DIR;
    }

    private void readRulesDir(File file) {
        FirewallIntentResolver[] firewallIntentResolverArr = new FirewallIntentResolver[3];
        for (int i = 0; i < firewallIntentResolverArr.length; i++) {
            firewallIntentResolverArr[i] = new FirewallIntentResolver();
        }
        File[] fileArrListFiles = file.listFiles();
        if (fileArrListFiles != null) {
            for (File file2 : fileArrListFiles) {
                if (file2.getName().endsWith(".xml")) {
                    readRules(file2, firewallIntentResolverArr);
                }
            }
        }
        Slog.i(TAG, "Read new rules (A:" + firewallIntentResolverArr[0].filterSet().size() + " B:" + firewallIntentResolverArr[1].filterSet().size() + " S:" + firewallIntentResolverArr[2].filterSet().size() + ")");
        synchronized (this.mAms.getAMSLock()) {
            this.mActivityResolver = firewallIntentResolverArr[0];
            this.mBroadcastResolver = firewallIntentResolverArr[1];
            this.mServiceResolver = firewallIntentResolverArr[2];
        }
    }

    private void readRules(File file, FirewallIntentResolver[] firewallIntentResolverArr) {
        ?? fileInputStream = 3;
        ArrayList arrayList = new ArrayList(3);
        for (int i = 0; i < 3; i++) {
            arrayList.add(new ArrayList());
        }
        try {
            try {
                fileInputStream = new FileInputStream(file);
                try {
                    try {
                        ?? NewPullParser = Xml.newPullParser();
                        NewPullParser.setInput(fileInputStream, null);
                        XmlUtils.beginDocument((XmlPullParser) NewPullParser, TAG_RULES);
                        int depth = NewPullParser.getDepth();
                        while (XmlUtils.nextElementWithin((XmlPullParser) NewPullParser, depth)) {
                            String name = NewPullParser.getName();
                            int i2 = name.equals(TAG_ACTIVITY) ? 0 : name.equals(TAG_BROADCAST) ? 1 : name.equals(TAG_SERVICE) ? 2 : -1;
                            if (i2 != -1) {
                                ?? rule = new Rule();
                                List list = (List) arrayList.get(i2);
                                try {
                                    rule.readFromXml(NewPullParser);
                                    list.add(rule);
                                } catch (XmlPullParserException e) {
                                    Slog.e(TAG, "Error reading an intent firewall rule from " + file, e);
                                }
                            }
                        }
                        try {
                            fileInputStream.close();
                        } catch (IOException e2) {
                            Slog.e(TAG, "Error while closing " + file, e2);
                        }
                        for (int i3 = 0; i3 < arrayList.size(); i3++) {
                            List list2 = (List) arrayList.get(i3);
                            FirewallIntentResolver firewallIntentResolver = firewallIntentResolverArr[i3];
                            for (int i4 = 0; i4 < list2.size(); i4++) {
                                Rule rule2 = (Rule) list2.get(i4);
                                for (int i5 = 0; i5 < rule2.getIntentFilterCount(); i5++) {
                                    firewallIntentResolver.addFilter(rule2.getIntentFilter(i5));
                                }
                                for (int i6 = 0; i6 < rule2.getComponentFilterCount(); i6++) {
                                    firewallIntentResolver.addComponentFilter(rule2.getComponentFilter(i6), rule2);
                                }
                            }
                        }
                    } catch (XmlPullParserException e3) {
                        Slog.e(TAG, "Error reading intent firewall rules from " + file, e3);
                        try {
                            fileInputStream.close();
                        } catch (IOException e4) {
                            Slog.e(TAG, "Error while closing " + file, e4);
                        }
                    }
                } catch (IOException e5) {
                    Slog.e(TAG, "Error reading intent firewall rules from " + file, e5);
                    try {
                        fileInputStream.close();
                    } catch (IOException e6) {
                        Slog.e(TAG, "Error while closing " + file, e6);
                    }
                }
            } catch (FileNotFoundException e7) {
            }
        } catch (Throwable th) {
            try {
                fileInputStream.close();
            } catch (IOException e8) {
                Slog.e(TAG, "Error while closing " + file, e8);
            }
            throw th;
        }
    }

    static Filter parseFilter(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        String name = xmlPullParser.getName();
        FilterFactory filterFactory = factoryMap.get(name);
        if (filterFactory == null) {
            throw new XmlPullParserException("Unknown element in filter list: " + name);
        }
        return filterFactory.newFilter(xmlPullParser);
    }

    private static class Rule extends AndFilter {
        private static final String ATTR_BLOCK = "block";
        private static final String ATTR_LOG = "log";
        private static final String ATTR_NAME = "name";
        private static final String TAG_COMPONENT_FILTER = "component-filter";
        private static final String TAG_INTENT_FILTER = "intent-filter";
        private boolean block;
        private boolean log;
        private final ArrayList<ComponentName> mComponentFilters;
        private final ArrayList<FirewallIntentFilter> mIntentFilters;

        private Rule() {
            this.mIntentFilters = new ArrayList<>(1);
            this.mComponentFilters = new ArrayList<>(0);
        }

        @Override
        public Rule readFromXml(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
            this.block = Boolean.parseBoolean(xmlPullParser.getAttributeValue(null, ATTR_BLOCK));
            this.log = Boolean.parseBoolean(xmlPullParser.getAttributeValue(null, ATTR_LOG));
            super.readFromXml(xmlPullParser);
            return this;
        }

        @Override
        protected void readChild(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
            String name = xmlPullParser.getName();
            if (name.equals(TAG_INTENT_FILTER)) {
                FirewallIntentFilter firewallIntentFilter = new FirewallIntentFilter(this);
                firewallIntentFilter.readFromXml(xmlPullParser);
                this.mIntentFilters.add(firewallIntentFilter);
            } else {
                if (name.equals(TAG_COMPONENT_FILTER)) {
                    String attributeValue = xmlPullParser.getAttributeValue(null, "name");
                    if (attributeValue == null) {
                        throw new XmlPullParserException("Component name must be specified.", xmlPullParser, null);
                    }
                    ComponentName componentNameUnflattenFromString = ComponentName.unflattenFromString(attributeValue);
                    if (componentNameUnflattenFromString == null) {
                        throw new XmlPullParserException("Invalid component name: " + attributeValue);
                    }
                    this.mComponentFilters.add(componentNameUnflattenFromString);
                    return;
                }
                super.readChild(xmlPullParser);
            }
        }

        public int getIntentFilterCount() {
            return this.mIntentFilters.size();
        }

        public FirewallIntentFilter getIntentFilter(int i) {
            return this.mIntentFilters.get(i);
        }

        public int getComponentFilterCount() {
            return this.mComponentFilters.size();
        }

        public ComponentName getComponentFilter(int i) {
            return this.mComponentFilters.get(i);
        }

        public boolean getBlock() {
            return this.block;
        }

        public boolean getLog() {
            return this.log;
        }
    }

    private static class FirewallIntentFilter extends IntentFilter {
        private final Rule rule;

        public FirewallIntentFilter(Rule rule) {
            this.rule = rule;
        }
    }

    private static class FirewallIntentResolver extends IntentResolver<FirewallIntentFilter, Rule> {
        private final ArrayMap<ComponentName, Rule[]> mRulesByComponent;

        private FirewallIntentResolver() {
            this.mRulesByComponent = new ArrayMap<>(0);
        }

        @Override
        protected boolean allowFilterResult(FirewallIntentFilter firewallIntentFilter, List<Rule> list) {
            return !list.contains(firewallIntentFilter.rule);
        }

        @Override
        protected boolean isPackageForFilter(String str, FirewallIntentFilter firewallIntentFilter) {
            return true;
        }

        @Override
        protected FirewallIntentFilter[] newArray(int i) {
            return new FirewallIntentFilter[i];
        }

        @Override
        protected Rule newResult(FirewallIntentFilter firewallIntentFilter, int i, int i2) {
            return firewallIntentFilter.rule;
        }

        @Override
        protected void sortResults(List<Rule> list) {
        }

        public void queryByComponent(ComponentName componentName, List<Rule> list) {
            Rule[] ruleArr = this.mRulesByComponent.get(componentName);
            if (ruleArr != null) {
                list.addAll(Arrays.asList(ruleArr));
            }
        }

        public void addComponentFilter(ComponentName componentName, Rule rule) {
            this.mRulesByComponent.put(componentName, (Rule[]) ArrayUtils.appendElement(Rule.class, this.mRulesByComponent.get(componentName), rule));
        }
    }

    private final class FirewallHandler extends Handler {
        public FirewallHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override
        public void handleMessage(Message message) {
            IntentFirewall.this.readRulesDir(IntentFirewall.getRulesDir());
        }
    }

    private class RuleObserver extends FileObserver {
        private static final int MONITORED_EVENTS = 968;

        public RuleObserver(File file) {
            super(file.getAbsolutePath(), MONITORED_EVENTS);
        }

        @Override
        public void onEvent(int i, String str) {
            if (str.endsWith(".xml")) {
                IntentFirewall.this.mHandler.removeMessages(0);
                IntentFirewall.this.mHandler.sendEmptyMessageDelayed(0, 250L);
            }
        }
    }

    boolean checkComponentPermission(String str, int i, int i2, int i3, boolean z) {
        return this.mAms.checkComponentPermission(str, i, i2, i3, z) == 0;
    }

    boolean signaturesMatch(int i, int i2) {
        try {
            return AppGlobals.getPackageManager().checkUidSignatures(i, i2) == 0;
        } catch (RemoteException e) {
            Slog.e(TAG, "Remote exception while checking signatures", e);
            return false;
        }
    }
}
