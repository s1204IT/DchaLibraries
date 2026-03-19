package android.icu.impl;

import android.icu.impl.ICUResourceBundle;
import android.icu.impl.ICUResourceBundleReader;
import android.icu.impl.UResource;
import android.icu.util.UResourceBundle;
import android.icu.util.UResourceTypeMismatchException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

class ICUResourceBundleImpl extends ICUResourceBundle {
    protected ICUResourceBundleImpl(ICUResourceBundleImpl container, String key) {
        super(container, key);
    }

    ICUResourceBundleImpl(ICUResourceBundle.WholeBundle wholeBundle) {
        super(wholeBundle);
    }

    protected final ICUResourceBundle createBundleObject(String _key, int _resource, HashMap<String, String> aliasesVisited, UResourceBundle requested) {
        switch (ICUResourceBundleReader.RES_GET_TYPE(_resource)) {
            case 0:
            case 6:
                return new ResourceString(this, _key, _resource);
            case 1:
                return new ResourceBinary(this, _key, _resource);
            case 2:
            case 4:
            case 5:
                return new ResourceTable(this, _key, _resource);
            case 3:
                return getAliasedResource(this, null, 0, _key, _resource, aliasesVisited, requested);
            case 7:
                return new ResourceInt(this, _key, _resource);
            case 8:
            case 9:
                return new ResourceArray(this, _key, _resource);
            case 10:
            case 11:
            case 12:
            case 13:
            default:
                throw new IllegalStateException("The resource type is unknown");
            case 14:
                return new ResourceIntVector(this, _key, _resource);
        }
    }

    private static final class ResourceBinary extends ICUResourceBundleImpl {
        private int resource;

        @Override
        public int getType() {
            return 1;
        }

        @Override
        public ByteBuffer getBinary() {
            return this.wholeBundle.reader.getBinary(this.resource);
        }

        @Override
        public byte[] getBinary(byte[] ba) {
            return this.wholeBundle.reader.getBinary(this.resource, ba);
        }

        ResourceBinary(ICUResourceBundleImpl container, String key, int resource) {
            super(container, key);
            this.resource = resource;
        }
    }

    private static final class ResourceInt extends ICUResourceBundleImpl {
        private int resource;

        @Override
        public int getType() {
            return 7;
        }

        @Override
        public int getInt() {
            return ICUResourceBundleReader.RES_GET_INT(this.resource);
        }

        @Override
        public int getUInt() {
            return ICUResourceBundleReader.RES_GET_UINT(this.resource);
        }

        ResourceInt(ICUResourceBundleImpl container, String key, int resource) {
            super(container, key);
            this.resource = resource;
        }
    }

    private static final class ResourceString extends ICUResourceBundleImpl {
        private int resource;
        private String value;

        @Override
        public int getType() {
            return 0;
        }

        @Override
        public String getString() {
            if (this.value != null) {
                return this.value;
            }
            return this.wholeBundle.reader.getString(this.resource);
        }

        ResourceString(ICUResourceBundleImpl container, String key, int resource) {
            super(container, key);
            this.resource = resource;
            String s = this.wholeBundle.reader.getString(resource);
            if (s.length() >= 12 && !CacheValue.futureInstancesWillBeStrong()) {
                return;
            }
            this.value = s;
        }
    }

    private static final class ResourceIntVector extends ICUResourceBundleImpl {
        private int resource;

        @Override
        public int getType() {
            return 14;
        }

        @Override
        public int[] getIntVector() {
            return this.wholeBundle.reader.getIntVector(this.resource);
        }

        ResourceIntVector(ICUResourceBundleImpl container, String key, int resource) {
            super(container, key);
            this.resource = resource;
        }
    }

    static abstract class ResourceContainer extends ICUResourceBundleImpl {
        protected ICUResourceBundleReader.Container value;

        @Override
        public int getSize() {
            return this.value.getSize();
        }

        @Override
        public String getString(int index) {
            int res = this.value.getContainerResource(this.wholeBundle.reader, index);
            if (res == -1) {
                throw new IndexOutOfBoundsException();
            }
            String s = this.wholeBundle.reader.getString(res);
            if (s != null) {
                return s;
            }
            return super.getString(index);
        }

        protected int getContainerResource(int index) {
            return this.value.getContainerResource(this.wholeBundle.reader, index);
        }

        protected UResourceBundle createBundleObject(int index, String resKey, HashMap<String, String> aliasesVisited, UResourceBundle requested) {
            int item = getContainerResource(index);
            if (item == -1) {
                throw new IndexOutOfBoundsException();
            }
            return createBundleObject(resKey, item, aliasesVisited, requested);
        }

        ResourceContainer(ICUResourceBundleImpl container, String key) {
            super(container, key);
        }

        ResourceContainer(ICUResourceBundle.WholeBundle wholeBundle) {
            super(wholeBundle);
        }
    }

    static class ResourceArray extends ResourceContainer {
        @Override
        public int getType() {
            return 8;
        }

        @Override
        protected String[] handleGetStringArray() {
            ICUResourceBundleReader reader = this.wholeBundle.reader;
            int length = this.value.getSize();
            String[] strings = new String[length];
            for (int i = 0; i < length; i++) {
                String s = reader.getString(this.value.getContainerResource(reader, i));
                if (s == null) {
                    throw new UResourceTypeMismatchException("");
                }
                strings[i] = s;
            }
            return strings;
        }

        @Override
        public String[] getStringArray() {
            return handleGetStringArray();
        }

        @Override
        protected UResourceBundle handleGet(String indexStr, HashMap<String, String> aliasesVisited, UResourceBundle requested) {
            int i = Integer.parseInt(indexStr);
            return createBundleObject(i, indexStr, aliasesVisited, requested);
        }

        @Override
        protected UResourceBundle handleGet(int index, HashMap<String, String> aliasesVisited, UResourceBundle requested) {
            return createBundleObject(index, Integer.toString(index), aliasesVisited, requested);
        }

        void getAllItems(UResource.Key key, ICUResourceBundleReader.ReaderValue readerValue, UResource.ArraySink sink) {
            ICUResourceBundleReader reader = this.wholeBundle.reader;
            readerValue.reader = reader;
            ((ICUResourceBundleReader.Array) this.value).getAllItems(reader, key, readerValue, sink);
        }

        ResourceArray(ICUResourceBundleImpl container, String key, int resource) {
            super(container, key);
            this.value = this.wholeBundle.reader.getArray(resource);
        }
    }

    static class ResourceTable extends ResourceContainer {
        @Override
        public int getType() {
            return 2;
        }

        protected String getKey(int index) {
            return ((ICUResourceBundleReader.Table) this.value).getKey(this.wholeBundle.reader, index);
        }

        @Override
        protected Set<String> handleKeySet() {
            ICUResourceBundleReader reader = this.wholeBundle.reader;
            TreeSet<String> keySet = new TreeSet<>();
            ICUResourceBundleReader.Table table = (ICUResourceBundleReader.Table) this.value;
            for (int i = 0; i < table.getSize(); i++) {
                keySet.add(table.getKey(reader, i));
            }
            return keySet;
        }

        @Override
        protected UResourceBundle handleGet(String resKey, HashMap<String, String> aliasesVisited, UResourceBundle requested) {
            int i = ((ICUResourceBundleReader.Table) this.value).findTableItem(this.wholeBundle.reader, resKey);
            if (i < 0) {
                return null;
            }
            return createBundleObject(resKey, getContainerResource(i), aliasesVisited, requested);
        }

        @Override
        protected UResourceBundle handleGet(int index, HashMap<String, String> aliasesVisited, UResourceBundle requested) {
            String itemKey = ((ICUResourceBundleReader.Table) this.value).getKey(this.wholeBundle.reader, index);
            if (itemKey == null) {
                throw new IndexOutOfBoundsException();
            }
            return createBundleObject(itemKey, getContainerResource(index), aliasesVisited, requested);
        }

        @Override
        protected Object handleGetObject(String key) {
            ICUResourceBundleReader reader = this.wholeBundle.reader;
            int index = ((ICUResourceBundleReader.Table) this.value).findTableItem(reader, key);
            if (index >= 0) {
                int res = this.value.getContainerResource(reader, index);
                String s = reader.getString(res);
                if (s != null) {
                    return s;
                }
                ICUResourceBundleReader.Container array = reader.getArray(res);
                if (array != null) {
                    int length = array.getSize();
                    String[] strings = new String[length];
                    for (int j = 0; j != length; j++) {
                        String s2 = reader.getString(array.getContainerResource(reader, j));
                        if (s2 != null) {
                            strings[j] = s2;
                        }
                    }
                    return strings;
                }
            }
            return super.handleGetObject(key);
        }

        String findString(String key) {
            ICUResourceBundleReader reader = this.wholeBundle.reader;
            int index = ((ICUResourceBundleReader.Table) this.value).findTableItem(reader, key);
            if (index < 0) {
                return null;
            }
            return reader.getString(this.value.getContainerResource(reader, index));
        }

        void getAllItems(UResource.Key key, ICUResourceBundleReader.ReaderValue readerValue, UResource.TableSink sink) {
            ICUResourceBundleReader reader = this.wholeBundle.reader;
            readerValue.reader = reader;
            ((ICUResourceBundleReader.Table) this.value).getAllItems(reader, key, readerValue, sink);
        }

        ResourceTable(ICUResourceBundleImpl container, String key, int resource) {
            super(container, key);
            this.value = this.wholeBundle.reader.getTable(resource);
        }

        ResourceTable(ICUResourceBundle.WholeBundle wholeBundle, int rootRes) {
            super(wholeBundle);
            this.value = wholeBundle.reader.getTable(rootRes);
        }
    }
}
