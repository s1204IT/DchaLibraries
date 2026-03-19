package com.coremedia.iso;

import com.coremedia.iso.boxes.Box;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PropertyBoxParserImpl extends AbstractBoxParser {
    Properties mapping;
    Pattern p = Pattern.compile("(.*)\\((.*?)\\)");

    public PropertyBoxParserImpl(String... strArr) {
        BufferedInputStream bufferedInputStream = new BufferedInputStream(getClass().getResourceAsStream("/isoparser-default.properties"));
        try {
            this.mapping = new Properties();
            try {
                this.mapping.load(bufferedInputStream);
                Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources("isoparser-custom.properties");
                while (resources.hasMoreElements()) {
                    bufferedInputStream = new BufferedInputStream(resources.nextElement().openStream());
                    try {
                        this.mapping.load(bufferedInputStream);
                        bufferedInputStream.close();
                    } finally {
                        bufferedInputStream.close();
                    }
                }
                for (String str : strArr) {
                    this.mapping.load(new BufferedInputStream(getClass().getResourceAsStream(str)));
                }
                try {
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (IOException e2) {
                throw new RuntimeException(e2);
            }
        } catch (Throwable th) {
            try {
            } catch (IOException e3) {
                e3.printStackTrace();
            }
            throw th;
        }
    }

    @Override
    public Box createBox(String str, byte[] bArr, String str2) {
        Constructor<?> constructor;
        FourCcToBox fourCcToBoxInvoke = new FourCcToBox(str, bArr, str2).invoke();
        String[] param = fourCcToBoxInvoke.getParam();
        String clazzName = fourCcToBoxInvoke.getClazzName();
        try {
            if (param[0].trim().length() == 0) {
                param = new String[0];
            }
            Class<?> cls = Class.forName(clazzName);
            Class<?>[] clsArr = new Class[param.length];
            Object[] objArr = new Object[param.length];
            for (int i = 0; i < param.length; i++) {
                if ("userType".equals(param[i])) {
                    objArr[i] = bArr;
                    clsArr[i] = byte[].class;
                } else if ("type".equals(param[i])) {
                    objArr[i] = str;
                    clsArr[i] = String.class;
                } else if ("parent".equals(param[i])) {
                    objArr[i] = str2;
                    clsArr[i] = String.class;
                } else {
                    throw new InternalError("No such param: " + param[i]);
                }
            }
            try {
                try {
                    if (param.length > 0) {
                        constructor = cls.getConstructor(clsArr);
                    } else {
                        constructor = cls.getConstructor(new Class[0]);
                    }
                    return (Box) constructor.newInstance(objArr);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                } catch (InvocationTargetException e2) {
                    throw new RuntimeException(e2);
                }
            } catch (InstantiationException e3) {
                throw new RuntimeException(e3);
            } catch (NoSuchMethodException e4) {
                throw new RuntimeException(e4);
            }
        } catch (ClassNotFoundException e5) {
            throw new RuntimeException(e5);
        }
    }

    private class FourCcToBox {
        private String clazzName;
        private String[] param;
        private String parent;
        private String type;
        private byte[] userType;

        public FourCcToBox(String str, byte[] bArr, String str2) {
            this.type = str;
            this.parent = str2;
            this.userType = bArr;
        }

        public String getClazzName() {
            return this.clazzName;
        }

        public String[] getParam() {
            return this.param;
        }

        public FourCcToBox invoke() {
            String property;
            if (this.userType != null) {
                if (!"uuid".equals(this.type)) {
                    throw new RuntimeException("we have a userType but no uuid box type. Something's wrong");
                }
                property = PropertyBoxParserImpl.this.mapping.getProperty(this.parent + "-uuid[" + Hex.encodeHex(this.userType).toUpperCase() + "]");
                if (property == null) {
                    property = PropertyBoxParserImpl.this.mapping.getProperty("uuid[" + Hex.encodeHex(this.userType).toUpperCase() + "]");
                }
                if (property == null) {
                    property = PropertyBoxParserImpl.this.mapping.getProperty("uuid");
                }
            } else {
                property = PropertyBoxParserImpl.this.mapping.getProperty(this.parent + "-" + this.type);
                if (property == null) {
                    property = PropertyBoxParserImpl.this.mapping.getProperty(this.type);
                }
            }
            if (property == null) {
                property = PropertyBoxParserImpl.this.mapping.getProperty("default");
            }
            if (property == null) {
                throw new RuntimeException("No box object found for " + this.type);
            }
            Matcher matcher = PropertyBoxParserImpl.this.p.matcher(property);
            if (!matcher.matches()) {
                throw new RuntimeException("Cannot work with that constructor: " + property);
            }
            this.clazzName = matcher.group(1);
            this.param = matcher.group(2).split(",");
            return this;
        }
    }
}
