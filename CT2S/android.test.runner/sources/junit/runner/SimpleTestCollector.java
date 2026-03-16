package junit.runner;

public class SimpleTestCollector extends ClassPathTestCollector {
    @Override
    protected boolean isTestClass(String classFileName) {
        return classFileName.endsWith(".class") && classFileName.indexOf(36) < 0 && classFileName.indexOf("Test") > 0;
    }
}
