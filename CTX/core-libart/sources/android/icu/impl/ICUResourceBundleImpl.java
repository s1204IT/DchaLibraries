package android.icu.impl;

import android.icu.impl.ICUResourceBundle;
import android.icu.impl.ICUResourceBundleReader;
import android.icu.util.UResourceBundle;
import android.icu.util.UResourceTypeMismatchException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

class ICUResourceBundleImpl extends ICUResourceBundle {
    protected int resource;

    protected ICUResourceBundleImpl(ICUResourceBundleImpl iCUResourceBundleImpl, String str, int i) {
        super(iCUResourceBundleImpl, str);
        this.resource = i;
    }

    ICUResourceBundleImpl(ICUResourceBundle.WholeBundle wholeBundle) {
        super(wholeBundle);
        this.resource = wholeBundle.reader.getRootResource();
    }

    public int getResource() {
        return this.resource;
    }

    protected final ICUResourceBundle createBundleObject(String str, int i, HashMap<String, String> map, UResourceBundle uResourceBundle) {
        int iRES_GET_TYPE = ICUResourceBundleReader.RES_GET_TYPE(i);
        if (iRES_GET_TYPE != 14) {
            switch (iRES_GET_TYPE) {
                case 0:
                case 6:
                    return new ResourceString(this, str, i);
                case 1:
                    return new ResourceBinary(this, str, i);
                case 2:
                case 4:
                case 5:
                    return new ResourceTable(this, str, i);
                case 3:
                    return getAliasedResource(this, null, 0, str, i, map, uResourceBundle);
                case 7:
                    return new ResourceInt(this, str, i);
                case 8:
                case 9:
                    return new ResourceArray(this, str, i);
                default:
                    throw new IllegalStateException("The resource type is unknown");
            }
        }
        return new ResourceIntVector(this, str, i);
    }

    private static final class ResourceBinary extends ICUResourceBundleImpl {
        @Override
        public int getType() {
            return 1;
        }

        @Override
        public ByteBuffer getBinary() {
            return this.wholeBundle.reader.getBinary(this.resource);
        }

        @Override
        public byte[] getBinary(byte[] bArr) {
            return this.wholeBundle.reader.getBinary(this.resource, bArr);
        }

        ResourceBinary(ICUResourceBundleImpl iCUResourceBundleImpl, String str, int i) {
            super(iCUResourceBundleImpl, str, i);
        }
    }

    private static final class ResourceInt extends ICUResourceBundleImpl {
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

        ResourceInt(ICUResourceBundleImpl iCUResourceBundleImpl, String str, int i) {
            super(iCUResourceBundleImpl, str, i);
        }
    }

    private static final class ResourceString extends ICUResourceBundleImpl {
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

        ResourceString(ICUResourceBundleImpl iCUResourceBundleImpl, String str, int i) {
            super(iCUResourceBundleImpl, str, i);
            String string = this.wholeBundle.reader.getString(i);
            if (string.length() < 12 || CacheValue.futureInstancesWillBeStrong()) {
                this.value = string;
            }
        }
    }

    private static final class ResourceIntVector extends ICUResourceBundleImpl {
        @Override
        public int getType() {
            return 14;
        }

        @Override
        public int[] getIntVector() {
            return this.wholeBundle.reader.getIntVector(this.resource);
        }

        ResourceIntVector(ICUResourceBundleImpl iCUResourceBundleImpl, String str, int i) {
            super(iCUResourceBundleImpl, str, i);
        }
    }

    static abstract class ResourceContainer extends ICUResourceBundleImpl {
        protected ICUResourceBundleReader.Container value;

        @Override
        public int getSize() {
            return this.value.getSize();
        }

        @Override
        public String getString(int i) {
            int containerResource = this.value.getContainerResource(this.wholeBundle.reader, i);
            if (containerResource == -1) {
                throw new IndexOutOfBoundsException();
            }
            String string = this.wholeBundle.reader.getString(containerResource);
            if (string != null) {
                return string;
            }
            return super.getString(i);
        }

        protected int getContainerResource(int i) {
            return this.value.getContainerResource(this.wholeBundle.reader, i);
        }

        protected UResourceBundle createBundleObject(int i, String str, HashMap<String, String> map, UResourceBundle uResourceBundle) {
            int containerResource = getContainerResource(i);
            if (containerResource == -1) {
                throw new IndexOutOfBoundsException();
            }
            return createBundleObject(str, containerResource, map, uResourceBundle);
        }

        ResourceContainer(ICUResourceBundleImpl iCUResourceBundleImpl, String str, int i) {
            super(iCUResourceBundleImpl, str, i);
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
            ICUResourceBundleReader iCUResourceBundleReader = this.wholeBundle.reader;
            int size = this.value.getSize();
            String[] strArr = new String[size];
            for (int i = 0; i < size; i++) {
                String string = iCUResourceBundleReader.getString(this.value.getContainerResource(iCUResourceBundleReader, i));
                if (string == null) {
                    throw new UResourceTypeMismatchException("");
                }
                strArr[i] = string;
            }
            return strArr;
        }

        @Override
        public String[] getStringArray() {
            return handleGetStringArray();
        }

        @Override
        protected UResourceBundle handleGet(String str, HashMap<String, String> map, UResourceBundle uResourceBundle) {
            return createBundleObject(Integer.parseInt(str), str, map, uResourceBundle);
        }

        @Override
        protected UResourceBundle handleGet(int i, HashMap<String, String> map, UResourceBundle uResourceBundle) {
            return createBundleObject(i, Integer.toString(i), map, uResourceBundle);
        }

        ResourceArray(ICUResourceBundleImpl iCUResourceBundleImpl, String str, int i) {
            super(iCUResourceBundleImpl, str, i);
            this.value = this.wholeBundle.reader.getArray(i);
        }
    }

    static class ResourceTable extends ResourceContainer {
        @Override
        public int getType() {
            return 2;
        }

        protected String getKey(int i) {
            return ((ICUResourceBundleReader.Table) this.value).getKey(this.wholeBundle.reader, i);
        }

        @Override
        protected Set<String> handleKeySet() {
            ICUResourceBundleReader iCUResourceBundleReader = this.wholeBundle.reader;
            TreeSet treeSet = new TreeSet();
            ICUResourceBundleReader.Table table = (ICUResourceBundleReader.Table) this.value;
            for (int i = 0; i < table.getSize(); i++) {
                treeSet.add(table.getKey(iCUResourceBundleReader, i));
            }
            return treeSet;
        }

        @Override
        protected UResourceBundle handleGet(String str, HashMap<String, String> map, UResourceBundle uResourceBundle) {
            int iFindTableItem = ((ICUResourceBundleReader.Table) this.value).findTableItem(this.wholeBundle.reader, str);
            if (iFindTableItem < 0) {
                return null;
            }
            return createBundleObject(str, getContainerResource(iFindTableItem), map, uResourceBundle);
        }

        @Override
        protected UResourceBundle handleGet(int i, HashMap<String, String> map, UResourceBundle uResourceBundle) {
            String key = ((ICUResourceBundleReader.Table) this.value).getKey(this.wholeBundle.reader, i);
            if (key == null) {
                throw new IndexOutOfBoundsException();
            }
            return createBundleObject(key, getContainerResource(i), map, uResourceBundle);
        }

        @Override
        protected Object handleGetObject(String str) {
            ICUResourceBundleReader iCUResourceBundleReader = this.wholeBundle.reader;
            int iFindTableItem = ((ICUResourceBundleReader.Table) this.value).findTableItem(iCUResourceBundleReader, str);
            if (iFindTableItem >= 0) {
                int containerResource = this.value.getContainerResource(iCUResourceBundleReader, iFindTableItem);
                String string = iCUResourceBundleReader.getString(containerResource);
                if (string != null) {
                    return string;
                }
                ICUResourceBundleReader.Array array = iCUResourceBundleReader.getArray(containerResource);
                if (array != null) {
                    int size = array.getSize();
                    String[] strArr = new String[size];
                    for (int i = 0; i != size; i++) {
                        String string2 = iCUResourceBundleReader.getString(array.getContainerResource(iCUResourceBundleReader, i));
                        if (string2 != null) {
                            strArr[i] = string2;
                        }
                    }
                    return strArr;
                }
            }
            return super.handleGetObject(str);
        }

        String findString(String str) {
            ICUResourceBundleReader iCUResourceBundleReader = this.wholeBundle.reader;
            int iFindTableItem = ((ICUResourceBundleReader.Table) this.value).findTableItem(iCUResourceBundleReader, str);
            if (iFindTableItem < 0) {
                return null;
            }
            return iCUResourceBundleReader.getString(this.value.getContainerResource(iCUResourceBundleReader, iFindTableItem));
        }

        ResourceTable(ICUResourceBundleImpl iCUResourceBundleImpl, String str, int i) {
            super(iCUResourceBundleImpl, str, i);
            this.value = this.wholeBundle.reader.getTable(i);
        }

        ResourceTable(ICUResourceBundle.WholeBundle wholeBundle, int i) {
            super(wholeBundle);
            this.value = wholeBundle.reader.getTable(i);
        }
    }
}
