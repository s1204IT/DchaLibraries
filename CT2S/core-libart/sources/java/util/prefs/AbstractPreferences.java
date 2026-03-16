package java.util.prefs;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.EventListener;
import java.util.EventObject;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import libcore.io.Base64;
import libcore.util.EmptyArray;

public abstract class AbstractPreferences extends Preferences {
    private Map<String, AbstractPreferences> cachedNode;
    private boolean isRemoved;
    protected final Object lock;
    protected boolean newNode;
    private List<EventListener> nodeChangeListeners;
    private String nodeName;
    private AbstractPreferences parentPref;
    private List<EventListener> preferenceChangeListeners;
    private AbstractPreferences root;
    boolean userNode;
    private static final List<EventObject> events = new LinkedList();
    private static final EventDispatcher dispatcher = new EventDispatcher("Preference Event Dispatcher");

    protected abstract AbstractPreferences childSpi(String str);

    protected abstract String[] childrenNamesSpi() throws BackingStoreException;

    protected abstract void flushSpi() throws BackingStoreException;

    protected abstract String getSpi(String str);

    protected abstract String[] keysSpi() throws BackingStoreException;

    protected abstract void putSpi(String str, String str2);

    protected abstract void removeNodeSpi() throws BackingStoreException;

    protected abstract void removeSpi(String str);

    protected abstract void syncSpi() throws BackingStoreException;

    static {
        dispatcher.setDaemon(true);
        dispatcher.start();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                Preferences uroot = Preferences.userRoot();
                Preferences sroot = Preferences.systemRoot();
                try {
                    uroot.flush();
                } catch (BackingStoreException e) {
                }
                try {
                    sroot.flush();
                } catch (BackingStoreException e2) {
                }
            }
        });
    }

    protected AbstractPreferences(AbstractPreferences parent, String name) {
        if (((parent == null) ^ (name.length() == 0)) || name.indexOf("/") >= 0) {
            throw new IllegalArgumentException();
        }
        this.root = parent == null ? this : parent.root;
        this.nodeChangeListeners = new LinkedList();
        this.preferenceChangeListeners = new LinkedList();
        this.isRemoved = false;
        this.cachedNode = new HashMap();
        this.nodeName = name;
        this.parentPref = parent;
        this.lock = new Object();
        this.userNode = this.root.userNode;
    }

    protected final AbstractPreferences[] cachedChildren() {
        return (AbstractPreferences[]) this.cachedNode.values().toArray(new AbstractPreferences[this.cachedNode.size()]);
    }

    protected AbstractPreferences getChild(String name) throws BackingStoreException {
        AbstractPreferences result;
        synchronized (this.lock) {
            checkState();
            result = null;
            String[] childrenNames = childrenNames();
            int len$ = childrenNames.length;
            int i$ = 0;
            while (true) {
                if (i$ >= len$) {
                    break;
                }
                String childrenName = childrenNames[i$];
                if (!childrenName.equals(name)) {
                    i$++;
                } else {
                    result = childSpi(name);
                    break;
                }
            }
        }
        return result;
    }

    protected boolean isRemoved() {
        boolean z;
        synchronized (this.lock) {
            z = this.isRemoved;
        }
        return z;
    }

    @Override
    public String absolutePath() {
        if (this.parentPref == null) {
            return "/";
        }
        if (this.parentPref == this.root) {
            return "/" + this.nodeName;
        }
        return this.parentPref.absolutePath() + "/" + this.nodeName;
    }

    @Override
    public String[] childrenNames() throws BackingStoreException {
        String[] strArr;
        synchronized (this.lock) {
            checkState();
            TreeSet<String> result = new TreeSet<>(this.cachedNode.keySet());
            String[] names = childrenNamesSpi();
            for (String str : names) {
                result.add(str);
            }
            strArr = (String[]) result.toArray(new String[result.size()]);
        }
        return strArr;
    }

    @Override
    public void clear() throws BackingStoreException {
        synchronized (this.lock) {
            String[] arr$ = keys();
            for (String key : arr$) {
                remove(key);
            }
        }
    }

    @Override
    public void exportNode(OutputStream ostream) throws IOException, BackingStoreException {
        if (ostream == null) {
            throw new NullPointerException("ostream == null");
        }
        checkState();
        XMLParser.exportPrefs(this, ostream, false);
    }

    @Override
    public void exportSubtree(OutputStream ostream) throws IOException, BackingStoreException {
        if (ostream == null) {
            throw new NullPointerException("ostream == null");
        }
        checkState();
        XMLParser.exportPrefs(this, ostream, true);
    }

    @Override
    public void flush() throws BackingStoreException {
        synchronized (this.lock) {
            flushSpi();
        }
        AbstractPreferences[] cc = cachedChildren();
        for (AbstractPreferences abstractPreferences : cc) {
            abstractPreferences.flush();
        }
    }

    @Override
    public String get(String key, String deflt) {
        if (key == null) {
            throw new NullPointerException("key == null");
        }
        String result = null;
        synchronized (this.lock) {
            checkState();
            try {
                result = getSpi(key);
            } catch (Exception e) {
            }
        }
        if (result == null) {
            return deflt;
        }
        String deflt2 = result;
        return deflt2;
    }

    @Override
    public boolean getBoolean(String key, boolean deflt) {
        String result = get(key, null);
        if (result != null) {
            if ("true".equalsIgnoreCase(result)) {
                return true;
            }
            if ("false".equalsIgnoreCase(result)) {
                return false;
            }
            return deflt;
        }
        return deflt;
    }

    @Override
    public byte[] getByteArray(String key, byte[] deflt) {
        String svalue = get(key, null);
        if (svalue != null) {
            if (svalue.length() == 0) {
                return EmptyArray.BYTE;
            }
            try {
                byte[] bavalue = svalue.getBytes(StandardCharsets.US_ASCII);
                if (bavalue.length % 4 == 0) {
                    return Base64.decode(bavalue);
                }
                return deflt;
            } catch (Exception e) {
                return deflt;
            }
        }
        return deflt;
    }

    @Override
    public double getDouble(String key, double deflt) {
        String result = get(key, null);
        if (result != null) {
            try {
                return Double.parseDouble(result);
            } catch (NumberFormatException e) {
                return deflt;
            }
        }
        return deflt;
    }

    @Override
    public float getFloat(String key, float deflt) {
        String result = get(key, null);
        if (result != null) {
            try {
                return Float.parseFloat(result);
            } catch (NumberFormatException e) {
                return deflt;
            }
        }
        return deflt;
    }

    @Override
    public int getInt(String key, int deflt) {
        String result = get(key, null);
        if (result != null) {
            try {
                return Integer.parseInt(result);
            } catch (NumberFormatException e) {
                return deflt;
            }
        }
        return deflt;
    }

    @Override
    public long getLong(String key, long deflt) {
        String result = get(key, null);
        if (result != null) {
            try {
                return Long.parseLong(result);
            } catch (NumberFormatException e) {
                return deflt;
            }
        }
        return deflt;
    }

    @Override
    public boolean isUserNode() {
        return this.root == Preferences.userRoot();
    }

    @Override
    public String[] keys() throws BackingStoreException {
        String[] strArrKeysSpi;
        synchronized (this.lock) {
            checkState();
            strArrKeysSpi = keysSpi();
        }
        return strArrKeysSpi;
    }

    @Override
    public String name() {
        return this.nodeName;
    }

    @Override
    public Preferences node(String name) {
        AbstractPreferences startNode;
        synchronized (this.lock) {
            checkState();
            validateName(name);
            if (!name.isEmpty()) {
                if ("/".equals(name)) {
                    return this.root;
                }
                if (name.startsWith("/")) {
                    startNode = this.root;
                    name = name.substring(1);
                } else {
                    startNode = this;
                }
                try {
                    return startNode.nodeImpl(name, true);
                } catch (BackingStoreException e) {
                    return null;
                }
            }
            return this;
        }
    }

    private void validateName(String name) {
        if (name.endsWith("/") && name.length() > 1) {
            throw new IllegalArgumentException("Name cannot end with '/'");
        }
        if (name.indexOf("//") >= 0) {
            throw new IllegalArgumentException("Name cannot contain consecutive '/' characters");
        }
    }

    private AbstractPreferences nodeImpl(String path, boolean createNew) throws BackingStoreException {
        AbstractPreferences temp;
        String[] names = path.split("/");
        AbstractPreferences currentNode = this;
        for (String name : names) {
            synchronized (currentNode.lock) {
                temp = currentNode.cachedNode.get(name);
                if (temp == null) {
                    temp = getNodeFromBackend(createNew, currentNode, name);
                }
            }
            currentNode = temp;
            if (currentNode == null) {
                break;
            }
        }
        return currentNode;
    }

    private AbstractPreferences getNodeFromBackend(boolean createNew, AbstractPreferences currentNode, String name) throws BackingStoreException {
        if (name.length() > 80) {
            throw new IllegalArgumentException("Name '" + name + "' too long");
        }
        if (createNew) {
            AbstractPreferences temp = currentNode.childSpi(name);
            currentNode.cachedNode.put(name, temp);
            if (temp.newNode && currentNode.nodeChangeListeners.size() > 0) {
                currentNode.notifyChildAdded(temp);
                return temp;
            }
            return temp;
        }
        return currentNode.getChild(name);
    }

    @Override
    public boolean nodeExists(String name) throws BackingStoreException {
        AbstractPreferences startNode;
        if (name == null) {
            throw new NullPointerException("name == null");
        }
        synchronized (this.lock) {
            if (isRemoved()) {
                if (name.isEmpty()) {
                    return false;
                }
                throw new IllegalStateException("This node has been removed");
            }
            validateName(name);
            if (name.isEmpty() || "/".equals(name)) {
                return true;
            }
            if (name.startsWith("/")) {
                startNode = this.root;
                name = name.substring(1);
            } else {
                startNode = this;
            }
            try {
                Preferences result = startNode.nodeImpl(name, false);
                return result != null;
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
    }

    @Override
    public Preferences parent() {
        checkState();
        return this.parentPref;
    }

    private void checkState() {
        if (isRemoved()) {
            throw new IllegalStateException("This node has been removed");
        }
    }

    @Override
    public void put(String key, String value) {
        if (key == null) {
            throw new NullPointerException("key == null");
        }
        if (value == null) {
            throw new NullPointerException("value == null");
        }
        if (key.length() > 80 || value.length() > 8192) {
            throw new IllegalArgumentException();
        }
        synchronized (this.lock) {
            checkState();
            putSpi(key, value);
        }
        notifyPreferenceChange(key, value);
    }

    @Override
    public void putBoolean(String key, boolean value) {
        put(key, String.valueOf(value));
    }

    @Override
    public void putByteArray(String key, byte[] value) {
        put(key, Base64.encode(value));
    }

    @Override
    public void putDouble(String key, double value) {
        put(key, Double.toString(value));
    }

    @Override
    public void putFloat(String key, float value) {
        put(key, Float.toString(value));
    }

    @Override
    public void putInt(String key, int value) {
        put(key, Integer.toString(value));
    }

    @Override
    public void putLong(String key, long value) {
        put(key, Long.toString(value));
    }

    @Override
    public void remove(String key) {
        synchronized (this.lock) {
            checkState();
            removeSpi(key);
        }
        notifyPreferenceChange(key, null);
    }

    @Override
    public void removeNode() throws BackingStoreException {
        if (this.root == this) {
            throw new UnsupportedOperationException("Cannot remove root node");
        }
        synchronized (this.parentPref.lock) {
            removeNodeImpl();
        }
    }

    private void removeNodeImpl() throws BackingStoreException {
        synchronized (this.lock) {
            checkState();
            String[] childrenNames = childrenNamesSpi();
            for (String childrenName : childrenNames) {
                if (this.cachedNode.get(childrenName) == null) {
                    AbstractPreferences child = childSpi(childrenName);
                    this.cachedNode.put(childrenName, child);
                }
            }
            Collection<AbstractPreferences> values = this.cachedNode.values();
            AbstractPreferences[] children = (AbstractPreferences[]) values.toArray(new AbstractPreferences[values.size()]);
            for (AbstractPreferences child2 : children) {
                child2.removeNodeImpl();
            }
            removeNodeSpi();
            this.isRemoved = true;
            this.parentPref.cachedNode.remove(this.nodeName);
        }
        if (this.parentPref.nodeChangeListeners.size() > 0) {
            this.parentPref.notifyChildRemoved(this);
        }
    }

    @Override
    public void addNodeChangeListener(NodeChangeListener ncl) {
        if (ncl == null) {
            throw new NullPointerException("ncl == null");
        }
        checkState();
        synchronized (this.nodeChangeListeners) {
            this.nodeChangeListeners.add(ncl);
        }
    }

    @Override
    public void addPreferenceChangeListener(PreferenceChangeListener pcl) {
        if (pcl == null) {
            throw new NullPointerException("pcl == null");
        }
        checkState();
        synchronized (this.preferenceChangeListeners) {
            this.preferenceChangeListeners.add(pcl);
        }
    }

    @Override
    public void removeNodeChangeListener(NodeChangeListener ncl) {
        checkState();
        synchronized (this.nodeChangeListeners) {
            int pos = this.nodeChangeListeners.indexOf(ncl);
            if (pos == -1) {
                throw new IllegalArgumentException();
            }
            this.nodeChangeListeners.remove(pos);
        }
    }

    @Override
    public void removePreferenceChangeListener(PreferenceChangeListener pcl) {
        checkState();
        synchronized (this.preferenceChangeListeners) {
            int pos = this.preferenceChangeListeners.indexOf(pcl);
            if (pos == -1) {
                throw new IllegalArgumentException();
            }
            this.preferenceChangeListeners.remove(pos);
        }
    }

    @Override
    public void sync() throws BackingStoreException {
        synchronized (this.lock) {
            checkState();
            syncSpi();
        }
        AbstractPreferences[] arr$ = cachedChildren();
        for (AbstractPreferences child : arr$) {
            child.sync();
        }
    }

    @Override
    public String toString() {
        return (isUserNode() ? "User" : "System") + " Preference Node: " + absolutePath();
    }

    private void notifyChildAdded(Preferences child) {
        NodeChangeEvent nce = new NodeAddEvent(this, child);
        synchronized (events) {
            events.add(nce);
            events.notifyAll();
        }
    }

    private void notifyChildRemoved(Preferences child) {
        NodeChangeEvent nce = new NodeRemoveEvent(this, child);
        synchronized (events) {
            events.add(nce);
            events.notifyAll();
        }
    }

    private void notifyPreferenceChange(String key, String newValue) {
        PreferenceChangeEvent pce = new PreferenceChangeEvent(this, key, newValue);
        synchronized (events) {
            events.add(pce);
            events.notifyAll();
        }
    }

    private static class EventDispatcher extends Thread {
        EventDispatcher(String name) {
            super(name);
        }

        @Override
        public void run() {
            while (true) {
                try {
                    EventObject event = getEventObject();
                    AbstractPreferences pref = (AbstractPreferences) event.getSource();
                    if (event instanceof NodeAddEvent) {
                        dispatchNodeAdd((NodeChangeEvent) event, pref.nodeChangeListeners);
                    } else if (event instanceof NodeRemoveEvent) {
                        dispatchNodeRemove((NodeChangeEvent) event, pref.nodeChangeListeners);
                    } else if (event instanceof PreferenceChangeEvent) {
                        dispatchPrefChange((PreferenceChangeEvent) event, pref.preferenceChangeListeners);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        private EventObject getEventObject() throws InterruptedException {
            EventObject event;
            synchronized (AbstractPreferences.events) {
                if (AbstractPreferences.events.isEmpty()) {
                    AbstractPreferences.events.wait();
                }
                event = (EventObject) AbstractPreferences.events.get(0);
                AbstractPreferences.events.remove(0);
            }
            return event;
        }

        private void dispatchPrefChange(PreferenceChangeEvent event, List<EventListener> preferenceChangeListeners) {
            synchronized (preferenceChangeListeners) {
                for (EventListener preferenceChangeListener : preferenceChangeListeners) {
                    ((PreferenceChangeListener) preferenceChangeListener).preferenceChange(event);
                }
            }
        }

        private void dispatchNodeRemove(NodeChangeEvent event, List<EventListener> nodeChangeListeners) {
            synchronized (nodeChangeListeners) {
                for (EventListener nodeChangeListener : nodeChangeListeners) {
                    ((NodeChangeListener) nodeChangeListener).childRemoved(event);
                }
            }
        }

        private void dispatchNodeAdd(NodeChangeEvent event, List<EventListener> nodeChangeListeners) {
            synchronized (nodeChangeListeners) {
                for (EventListener nodeChangeListener : nodeChangeListeners) {
                    NodeChangeListener ncl = (NodeChangeListener) nodeChangeListener;
                    ncl.childAdded(event);
                }
            }
        }
    }

    private static class NodeAddEvent extends NodeChangeEvent {
        private static final long serialVersionUID = 1;

        public NodeAddEvent(Preferences p, Preferences c) {
            super(p, c);
        }
    }

    private static class NodeRemoveEvent extends NodeChangeEvent {
        private static final long serialVersionUID = 1;

        public NodeRemoveEvent(Preferences p, Preferences c) {
            super(p, c);
        }
    }
}
