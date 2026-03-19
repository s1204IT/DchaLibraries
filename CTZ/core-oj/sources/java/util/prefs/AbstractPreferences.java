package java.util.prefs;

import java.io.IOException;
import java.io.OutputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.TreeSet;

public abstract class AbstractPreferences extends Preferences {
    private final String absolutePath;
    private final String name;
    final AbstractPreferences parent;
    private final AbstractPreferences root;
    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    private static final AbstractPreferences[] EMPTY_ABSTRACT_PREFS_ARRAY = new AbstractPreferences[0];
    private static final List<EventObject> eventQueue = new LinkedList();
    private static Thread eventDispatchThread = null;
    protected boolean newNode = false;
    private Map<String, AbstractPreferences> kidCache = new HashMap();
    private boolean removed = false;
    private final ArrayList<PreferenceChangeListener> prefListeners = new ArrayList<>();
    private final ArrayList<NodeChangeListener> nodeListeners = new ArrayList<>();
    protected final Object lock = new Object();

    protected abstract AbstractPreferences childSpi(String str);

    protected abstract String[] childrenNamesSpi() throws BackingStoreException;

    protected abstract void flushSpi() throws BackingStoreException;

    protected abstract String getSpi(String str);

    protected abstract String[] keysSpi() throws BackingStoreException;

    protected abstract void putSpi(String str, String str2);

    protected abstract void removeNodeSpi() throws BackingStoreException;

    protected abstract void removeSpi(String str);

    protected abstract void syncSpi() throws BackingStoreException;

    protected AbstractPreferences(AbstractPreferences abstractPreferences, String str) {
        String str2;
        if (abstractPreferences == null) {
            if (!str.equals("")) {
                throw new IllegalArgumentException("Root name '" + str + "' must be \"\"");
            }
            this.absolutePath = "/";
            this.root = this;
        } else {
            if (str.indexOf(47) != -1) {
                throw new IllegalArgumentException("Name '" + str + "' contains '/'");
            }
            if (str.equals("")) {
                throw new IllegalArgumentException("Illegal name: empty string");
            }
            this.root = abstractPreferences.root;
            if (abstractPreferences == this.root) {
                str2 = "/" + str;
            } else {
                str2 = abstractPreferences.absolutePath() + "/" + str;
            }
            this.absolutePath = str2;
        }
        this.name = str;
        this.parent = abstractPreferences;
    }

    @Override
    public void put(String str, String str2) {
        if (str == null || str2 == null) {
            throw new NullPointerException();
        }
        if (str.length() > 80) {
            throw new IllegalArgumentException("Key too long: " + str);
        }
        if (str2.length() > 8192) {
            throw new IllegalArgumentException("Value too long: " + str2);
        }
        synchronized (this.lock) {
            if (this.removed) {
                throw new IllegalStateException("Node has been removed.");
            }
            putSpi(str, str2);
            enqueuePreferenceChangeEvent(str, str2);
        }
    }

    @Override
    public String get(String str, String str2) {
        String spi;
        if (str == null) {
            throw new NullPointerException("Null key");
        }
        synchronized (this.lock) {
            if (this.removed) {
                throw new IllegalStateException("Node has been removed.");
            }
            try {
                spi = getSpi(str);
            } catch (Exception e) {
                spi = null;
            }
            if (spi == null) {
                spi = str2;
            }
        }
        return spi;
    }

    @Override
    public void remove(String str) {
        Objects.requireNonNull(str, "Specified key cannot be null");
        synchronized (this.lock) {
            if (this.removed) {
                throw new IllegalStateException("Node has been removed.");
            }
            removeSpi(str);
            enqueuePreferenceChangeEvent(str, null);
        }
    }

    @Override
    public void clear() throws BackingStoreException {
        synchronized (this.lock) {
            for (String str : keys()) {
                remove(str);
            }
        }
    }

    @Override
    public void putInt(String str, int i) {
        put(str, Integer.toString(i));
    }

    @Override
    public int getInt(String str, int i) {
        try {
            String str2 = get(str, null);
            if (str2 != null) {
                return Integer.parseInt(str2);
            }
            return i;
        } catch (NumberFormatException e) {
            return i;
        }
    }

    @Override
    public void putLong(String str, long j) {
        put(str, Long.toString(j));
    }

    @Override
    public long getLong(String str, long j) {
        try {
            String str2 = get(str, null);
            if (str2 != null) {
                return Long.parseLong(str2);
            }
            return j;
        } catch (NumberFormatException e) {
            return j;
        }
    }

    @Override
    public void putBoolean(String str, boolean z) {
        put(str, String.valueOf(z));
    }

    @Override
    public boolean getBoolean(String str, boolean z) {
        String str2 = get(str, null);
        if (str2 != null) {
            if (str2.equalsIgnoreCase("true")) {
                return true;
            }
            if (str2.equalsIgnoreCase("false")) {
                return false;
            }
            return z;
        }
        return z;
    }

    @Override
    public void putFloat(String str, float f) {
        put(str, Float.toString(f));
    }

    @Override
    public float getFloat(String str, float f) {
        try {
            String str2 = get(str, null);
            if (str2 != null) {
                return Float.parseFloat(str2);
            }
            return f;
        } catch (NumberFormatException e) {
            return f;
        }
    }

    @Override
    public void putDouble(String str, double d) {
        put(str, Double.toString(d));
    }

    @Override
    public double getDouble(String str, double d) {
        try {
            String str2 = get(str, null);
            if (str2 != null) {
                return Double.parseDouble(str2);
            }
            return d;
        } catch (NumberFormatException e) {
            return d;
        }
    }

    @Override
    public void putByteArray(String str, byte[] bArr) {
        put(str, Base64.byteArrayToBase64(bArr));
    }

    @Override
    public byte[] getByteArray(String str, byte[] bArr) {
        String str2 = get(str, null);
        if (str2 != null) {
            try {
                return Base64.base64ToByteArray(str2);
            } catch (RuntimeException e) {
                return bArr;
            }
        }
        return bArr;
    }

    @Override
    public String[] keys() throws BackingStoreException {
        String[] strArrKeysSpi;
        synchronized (this.lock) {
            if (this.removed) {
                throw new IllegalStateException("Node has been removed.");
            }
            strArrKeysSpi = keysSpi();
        }
        return strArrKeysSpi;
    }

    @Override
    public String[] childrenNames() throws BackingStoreException {
        String[] strArr;
        synchronized (this.lock) {
            if (this.removed) {
                throw new IllegalStateException("Node has been removed.");
            }
            TreeSet treeSet = new TreeSet(this.kidCache.keySet());
            for (String str : childrenNamesSpi()) {
                treeSet.add(str);
            }
            strArr = (String[]) treeSet.toArray(EMPTY_STRING_ARRAY);
        }
        return strArr;
    }

    protected final AbstractPreferences[] cachedChildren() {
        return (AbstractPreferences[]) this.kidCache.values().toArray(EMPTY_ABSTRACT_PREFS_ARRAY);
    }

    @Override
    public Preferences parent() {
        AbstractPreferences abstractPreferences;
        synchronized (this.lock) {
            if (this.removed) {
                throw new IllegalStateException("Node has been removed.");
            }
            abstractPreferences = this.parent;
        }
        return abstractPreferences;
    }

    @Override
    public Preferences node(String str) {
        synchronized (this.lock) {
            if (this.removed) {
                throw new IllegalStateException("Node has been removed.");
            }
            if (str.equals("")) {
                return this;
            }
            if (str.equals("/")) {
                return this.root;
            }
            if (str.charAt(0) != '/') {
                return node(new StringTokenizer(str, "/", true));
            }
            return this.root.node(new StringTokenizer(str.substring(1), "/", true));
        }
    }

    private Preferences node(StringTokenizer stringTokenizer) {
        String strNextToken = stringTokenizer.nextToken();
        if (strNextToken.equals("/")) {
            throw new IllegalArgumentException("Consecutive slashes in path");
        }
        synchronized (this.lock) {
            AbstractPreferences abstractPreferencesChildSpi = this.kidCache.get(strNextToken);
            if (abstractPreferencesChildSpi == null) {
                if (strNextToken.length() > 80) {
                    throw new IllegalArgumentException("Node name " + strNextToken + " too long");
                }
                abstractPreferencesChildSpi = childSpi(strNextToken);
                if (abstractPreferencesChildSpi.newNode) {
                    enqueueNodeAddedEvent(abstractPreferencesChildSpi);
                }
                this.kidCache.put(strNextToken, abstractPreferencesChildSpi);
            }
            if (!stringTokenizer.hasMoreTokens()) {
                return abstractPreferencesChildSpi;
            }
            stringTokenizer.nextToken();
            if (!stringTokenizer.hasMoreTokens()) {
                throw new IllegalArgumentException("Path ends with slash");
            }
            return abstractPreferencesChildSpi.node(stringTokenizer);
        }
    }

    @Override
    public boolean nodeExists(String str) throws BackingStoreException {
        synchronized (this.lock) {
            if (str.equals("")) {
                return !this.removed;
            }
            if (this.removed) {
                throw new IllegalStateException("Node has been removed.");
            }
            if (str.equals("/")) {
                return true;
            }
            if (str.charAt(0) != '/') {
                return nodeExists(new StringTokenizer(str, "/", true));
            }
            return this.root.nodeExists(new StringTokenizer(str.substring(1), "/", true));
        }
    }

    private boolean nodeExists(StringTokenizer stringTokenizer) throws BackingStoreException {
        String strNextToken = stringTokenizer.nextToken();
        if (strNextToken.equals("/")) {
            throw new IllegalArgumentException("Consecutive slashes in path");
        }
        synchronized (this.lock) {
            AbstractPreferences child = this.kidCache.get(strNextToken);
            if (child == null) {
                child = getChild(strNextToken);
            }
            if (child == null) {
                return false;
            }
            if (!stringTokenizer.hasMoreTokens()) {
                return true;
            }
            stringTokenizer.nextToken();
            if (!stringTokenizer.hasMoreTokens()) {
                throw new IllegalArgumentException("Path ends with slash");
            }
            return child.nodeExists(stringTokenizer);
        }
    }

    @Override
    public void removeNode() throws BackingStoreException {
        if (this == this.root) {
            throw new UnsupportedOperationException("Can't remove the root!");
        }
        synchronized (this.parent.lock) {
            removeNode2();
            this.parent.kidCache.remove(this.name);
        }
    }

    private void removeNode2() throws BackingStoreException {
        synchronized (this.lock) {
            if (this.removed) {
                throw new IllegalStateException("Node already removed.");
            }
            String[] strArrChildrenNamesSpi = childrenNamesSpi();
            for (int i = 0; i < strArrChildrenNamesSpi.length; i++) {
                if (!this.kidCache.containsKey(strArrChildrenNamesSpi[i])) {
                    this.kidCache.put(strArrChildrenNamesSpi[i], childSpi(strArrChildrenNamesSpi[i]));
                }
            }
            Iterator<AbstractPreferences> it = this.kidCache.values().iterator();
            while (it.hasNext()) {
                try {
                    it.next().removeNode2();
                    it.remove();
                } catch (BackingStoreException e) {
                }
            }
            removeNodeSpi();
            this.removed = true;
            this.parent.enqueueNodeRemovedEvent(this);
        }
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public String absolutePath() {
        return this.absolutePath;
    }

    @Override
    public boolean isUserNode() {
        return ((Boolean) AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                return Boolean.valueOf(AbstractPreferences.this.root == Preferences.userRoot());
            }
        })).booleanValue();
    }

    @Override
    public void addPreferenceChangeListener(PreferenceChangeListener preferenceChangeListener) {
        if (preferenceChangeListener == null) {
            throw new NullPointerException("Change listener is null.");
        }
        synchronized (this.lock) {
            if (this.removed) {
                throw new IllegalStateException("Node has been removed.");
            }
            this.prefListeners.add(preferenceChangeListener);
        }
        startEventDispatchThreadIfNecessary();
    }

    @Override
    public void removePreferenceChangeListener(PreferenceChangeListener preferenceChangeListener) {
        synchronized (this.lock) {
            if (this.removed) {
                throw new IllegalStateException("Node has been removed.");
            }
            if (!this.prefListeners.contains(preferenceChangeListener)) {
                throw new IllegalArgumentException("Listener not registered.");
            }
            this.prefListeners.remove(preferenceChangeListener);
        }
    }

    @Override
    public void addNodeChangeListener(NodeChangeListener nodeChangeListener) {
        if (nodeChangeListener == null) {
            throw new NullPointerException("Change listener is null.");
        }
        synchronized (this.lock) {
            if (this.removed) {
                throw new IllegalStateException("Node has been removed.");
            }
            this.nodeListeners.add(nodeChangeListener);
        }
        startEventDispatchThreadIfNecessary();
    }

    @Override
    public void removeNodeChangeListener(NodeChangeListener nodeChangeListener) {
        synchronized (this.lock) {
            if (this.removed) {
                throw new IllegalStateException("Node has been removed.");
            }
            if (!this.nodeListeners.contains(nodeChangeListener)) {
                throw new IllegalArgumentException("Listener not registered.");
            }
            this.nodeListeners.remove(nodeChangeListener);
        }
    }

    protected AbstractPreferences getChild(String str) throws BackingStoreException {
        synchronized (this.lock) {
            String[] strArrChildrenNames = childrenNames();
            for (int i = 0; i < strArrChildrenNames.length; i++) {
                if (strArrChildrenNames[i].equals(str)) {
                    return childSpi(strArrChildrenNames[i]);
                }
            }
            return null;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(isUserNode() ? "User" : "System");
        sb.append(" Preference Node: ");
        sb.append(absolutePath());
        return sb.toString();
    }

    @Override
    public void sync() throws BackingStoreException {
        sync2();
    }

    private void sync2() throws BackingStoreException {
        AbstractPreferences[] abstractPreferencesArrCachedChildren;
        synchronized (this.lock) {
            if (this.removed) {
                throw new IllegalStateException("Node has been removed");
            }
            syncSpi();
            abstractPreferencesArrCachedChildren = cachedChildren();
        }
        for (AbstractPreferences abstractPreferences : abstractPreferencesArrCachedChildren) {
            abstractPreferences.sync2();
        }
    }

    @Override
    public void flush() throws BackingStoreException {
        flush2();
    }

    private void flush2() throws BackingStoreException {
        synchronized (this.lock) {
            flushSpi();
            if (this.removed) {
                return;
            }
            for (AbstractPreferences abstractPreferences : cachedChildren()) {
                abstractPreferences.flush2();
            }
        }
    }

    protected boolean isRemoved() {
        boolean z;
        synchronized (this.lock) {
            z = this.removed;
        }
        return z;
    }

    private class NodeAddedEvent extends NodeChangeEvent {
        private static final long serialVersionUID = -6743557530157328528L;

        NodeAddedEvent(Preferences preferences, Preferences preferences2) {
            super(preferences, preferences2);
        }
    }

    private class NodeRemovedEvent extends NodeChangeEvent {
        private static final long serialVersionUID = 8735497392918824837L;

        NodeRemovedEvent(Preferences preferences, Preferences preferences2) {
            super(preferences, preferences2);
        }
    }

    private static class EventDispatchThread extends Thread {
        private EventDispatchThread() {
        }

        @Override
        public void run() {
            int i;
            EventObject eventObject;
            while (true) {
                synchronized (AbstractPreferences.eventQueue) {
                    while (AbstractPreferences.eventQueue.isEmpty()) {
                        try {
                            AbstractPreferences.eventQueue.wait();
                        } catch (InterruptedException e) {
                            return;
                        }
                    }
                    i = 0;
                    eventObject = (EventObject) AbstractPreferences.eventQueue.remove(0);
                }
                AbstractPreferences abstractPreferences = (AbstractPreferences) eventObject.getSource();
                if (eventObject instanceof PreferenceChangeEvent) {
                    PreferenceChangeEvent preferenceChangeEvent = (PreferenceChangeEvent) eventObject;
                    PreferenceChangeListener[] preferenceChangeListenerArrPrefListeners = abstractPreferences.prefListeners();
                    while (i < preferenceChangeListenerArrPrefListeners.length) {
                        preferenceChangeListenerArrPrefListeners[i].preferenceChange(preferenceChangeEvent);
                        i++;
                    }
                } else {
                    NodeChangeEvent nodeChangeEvent = (NodeChangeEvent) eventObject;
                    NodeChangeListener[] nodeChangeListenerArrNodeListeners = abstractPreferences.nodeListeners();
                    if (nodeChangeEvent instanceof NodeAddedEvent) {
                        while (i < nodeChangeListenerArrNodeListeners.length) {
                            nodeChangeListenerArrNodeListeners[i].childAdded(nodeChangeEvent);
                            i++;
                        }
                    } else {
                        while (i < nodeChangeListenerArrNodeListeners.length) {
                            nodeChangeListenerArrNodeListeners[i].childRemoved(nodeChangeEvent);
                            i++;
                        }
                    }
                }
            }
        }
    }

    private static synchronized void startEventDispatchThreadIfNecessary() {
        if (eventDispatchThread == null) {
            eventDispatchThread = new EventDispatchThread();
            eventDispatchThread.setDaemon(true);
            eventDispatchThread.start();
        }
    }

    PreferenceChangeListener[] prefListeners() {
        PreferenceChangeListener[] preferenceChangeListenerArr;
        synchronized (this.lock) {
            preferenceChangeListenerArr = (PreferenceChangeListener[]) this.prefListeners.toArray(new PreferenceChangeListener[this.prefListeners.size()]);
        }
        return preferenceChangeListenerArr;
    }

    NodeChangeListener[] nodeListeners() {
        NodeChangeListener[] nodeChangeListenerArr;
        synchronized (this.lock) {
            nodeChangeListenerArr = (NodeChangeListener[]) this.nodeListeners.toArray(new NodeChangeListener[this.nodeListeners.size()]);
        }
        return nodeChangeListenerArr;
    }

    private void enqueuePreferenceChangeEvent(String str, String str2) {
        if (!this.prefListeners.isEmpty()) {
            synchronized (eventQueue) {
                eventQueue.add(new PreferenceChangeEvent(this, str, str2));
                eventQueue.notify();
            }
        }
    }

    private void enqueueNodeAddedEvent(Preferences preferences) {
        if (!this.nodeListeners.isEmpty()) {
            synchronized (eventQueue) {
                eventQueue.add(new NodeAddedEvent(this, preferences));
                eventQueue.notify();
            }
        }
    }

    private void enqueueNodeRemovedEvent(Preferences preferences) {
        if (!this.nodeListeners.isEmpty()) {
            synchronized (eventQueue) {
                eventQueue.add(new NodeRemovedEvent(this, preferences));
                eventQueue.notify();
            }
        }
    }

    @Override
    public void exportNode(OutputStream outputStream) throws BackingStoreException, IOException {
        XmlSupport.export(outputStream, this, false);
    }

    @Override
    public void exportSubtree(OutputStream outputStream) throws BackingStoreException, IOException {
        XmlSupport.export(outputStream, this, true);
    }
}
