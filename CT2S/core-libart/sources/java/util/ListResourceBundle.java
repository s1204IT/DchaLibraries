package java.util;

public abstract class ListResourceBundle extends ResourceBundle {
    HashMap<String, Object> table;

    protected abstract Object[][] getContents();

    @Override
    public Enumeration<String> getKeys() {
        initializeTable();
        return this.parent != null ? new Enumeration<String>() {
            Iterator<String> local;
            String nextElement;
            Enumeration<String> pEnum;

            {
                this.local = ListResourceBundle.this.table.keySet().iterator();
                this.pEnum = ListResourceBundle.this.parent.getKeys();
            }

            private boolean findNext() {
                if (this.nextElement != null) {
                    return true;
                }
                while (this.pEnum.hasMoreElements()) {
                    String next = this.pEnum.nextElement();
                    if (!ListResourceBundle.this.table.containsKey(next)) {
                        this.nextElement = next;
                        return true;
                    }
                }
                return false;
            }

            @Override
            public boolean hasMoreElements() {
                if (this.local.hasNext()) {
                    return true;
                }
                return findNext();
            }

            @Override
            public String nextElement() {
                if (this.local.hasNext()) {
                    return this.local.next();
                }
                if (findNext()) {
                    String result = this.nextElement;
                    this.nextElement = null;
                    return result;
                }
                return this.pEnum.nextElement();
            }
        } : new Enumeration<String>() {
            Iterator<String> it;

            {
                this.it = ListResourceBundle.this.table.keySet().iterator();
            }

            @Override
            public boolean hasMoreElements() {
                return this.it.hasNext();
            }

            @Override
            public String nextElement() {
                return this.it.next();
            }
        };
    }

    @Override
    public final Object handleGetObject(String key) {
        initializeTable();
        if (key == null) {
            throw new NullPointerException("key == null");
        }
        return this.table.get(key);
    }

    private synchronized void initializeTable() {
        if (this.table == null) {
            Object[][] contents = getContents();
            this.table = new HashMap<>(((contents.length / 3) * 4) + 3);
            for (Object[] content : contents) {
                if (content[0] == null || content[1] == null) {
                    throw new NullPointerException("null entry");
                }
                this.table.put((String) content[0], content[1]);
            }
        }
    }

    @Override
    protected Set<String> handleKeySet() {
        initializeTable();
        return this.table.keySet();
    }
}
