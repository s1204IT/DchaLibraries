package junit.runner;

public class StandardTestSuiteLoader implements TestSuiteLoader {
    @Override
    public Class load(String suiteClassName) throws ClassNotFoundException {
        return Class.forName(suiteClassName);
    }

    @Override
    public Class reload(Class aClass) throws ClassNotFoundException {
        return aClass;
    }
}
