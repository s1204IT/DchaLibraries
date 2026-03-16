package com.coremedia.iso;

import com.coremedia.iso.boxes.Box;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
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

    public PropertyBoxParserImpl(String... customProperties) {
        InputStream customIS = new BufferedInputStream(getClass().getResourceAsStream("/isoparser-default.properties"));
        try {
            this.mapping = new Properties();
            try {
                this.mapping.load(customIS);
                Enumeration<URL> enumeration = Thread.currentThread().getContextClassLoader().getResources("isoparser-custom.properties");
                while (enumeration.hasMoreElements()) {
                    URL url = enumeration.nextElement();
                    customIS = new BufferedInputStream(url.openStream());
                    try {
                        this.mapping.load(customIS);
                    } finally {
                        customIS.close();
                    }
                }
                for (String customProperty : customProperties) {
                    this.mapping.load(new BufferedInputStream(getClass().getResourceAsStream(customProperty)));
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
    public Box createBox(String type, byte[] userType, String parent) {
        Constructor<?> constructor;
        FourCcToBox fourCcToBox = new FourCcToBox(type, userType, parent).invoke();
        String[] param = fourCcToBox.getParam();
        String clazzName = fourCcToBox.getClazzName();
        try {
            if (param[0].trim().length() == 0) {
                param = new String[0];
            }
            Class<?> cls = Class.forName(clazzName);
            Class<?>[] clsArr = new Class[param.length];
            Object[] constructorArgs = new Object[param.length];
            for (int i = 0; i < param.length; i++) {
                if ("userType".equals(param[i])) {
                    constructorArgs[i] = userType;
                    clsArr[i] = byte[].class;
                } else if ("type".equals(param[i])) {
                    constructorArgs[i] = type;
                    clsArr[i] = String.class;
                } else if ("parent".equals(param[i])) {
                    constructorArgs[i] = parent;
                    clsArr[i] = String.class;
                } else {
                    throw new InternalError("No such param: " + param[i]);
                }
            }
            try {
                try {
                    try {
                        if (param.length > 0) {
                            constructor = cls.getConstructor(clsArr);
                        } else {
                            constructor = cls.getConstructor(new Class[0]);
                        }
                        return (Box) constructor.newInstance(constructorArgs);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                } catch (InstantiationException e2) {
                    throw new RuntimeException(e2);
                }
            } catch (NoSuchMethodException e3) {
                throw new RuntimeException(e3);
            } catch (InvocationTargetException e4) {
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

        public FourCcToBox(String type, byte[] userType, String parent) {
            this.type = type;
            this.parent = parent;
            this.userType = userType;
        }

        public String getClazzName() {
            return this.clazzName;
        }

        public String[] getParam() {
            return this.param;
        }

        public FourCcToBox invoke() {
            String constructor;
            if (this.userType != null) {
                if (!"uuid".equals(this.type)) {
                    throw new RuntimeException("we have a userType but no uuid box type. Something's wrong");
                }
                constructor = PropertyBoxParserImpl.this.mapping.getProperty(this.parent + "-uuid[" + Hex.encodeHex(this.userType).toUpperCase() + "]");
                if (constructor == null) {
                    constructor = PropertyBoxParserImpl.this.mapping.getProperty("uuid[" + Hex.encodeHex(this.userType).toUpperCase() + "]");
                }
                if (constructor == null) {
                    constructor = PropertyBoxParserImpl.this.mapping.getProperty("uuid");
                }
            } else {
                constructor = PropertyBoxParserImpl.this.mapping.getProperty(this.parent + "-" + this.type);
                if (constructor == null) {
                    constructor = PropertyBoxParserImpl.this.mapping.getProperty(this.type);
                }
            }
            if (constructor == null) {
                constructor = PropertyBoxParserImpl.this.mapping.getProperty("default");
            }
            if (constructor == null) {
                throw new RuntimeException("No box object found for " + this.type);
            }
            Matcher m = PropertyBoxParserImpl.this.p.matcher(constructor);
            boolean matches = m.matches();
            if (!matches) {
                throw new RuntimeException("Cannot work with that constructor: " + constructor);
            }
            this.clazzName = m.group(1);
            this.param = m.group(2).split(",");
            return this;
        }
    }
}
