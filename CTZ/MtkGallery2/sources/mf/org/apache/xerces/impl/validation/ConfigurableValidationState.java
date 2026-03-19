package mf.org.apache.xerces.impl.validation;

public final class ConfigurableValidationState extends ValidationState {
    private boolean fIdIdrefChecking = true;
    private boolean fUnparsedEntityChecking = true;

    public void setIdIdrefChecking(boolean setting) {
        this.fIdIdrefChecking = setting;
    }

    public void setUnparsedEntityChecking(boolean setting) {
        this.fUnparsedEntityChecking = setting;
    }

    @Override
    public String checkIDRefID() {
        if (this.fIdIdrefChecking) {
            return super.checkIDRefID();
        }
        return null;
    }

    @Override
    public boolean isIdDeclared(String name) {
        if (this.fIdIdrefChecking) {
            return super.isIdDeclared(name);
        }
        return false;
    }

    @Override
    public boolean isEntityDeclared(String name) {
        if (this.fUnparsedEntityChecking) {
            return super.isEntityDeclared(name);
        }
        return true;
    }

    @Override
    public boolean isEntityUnparsed(String name) {
        if (this.fUnparsedEntityChecking) {
            return super.isEntityUnparsed(name);
        }
        return true;
    }

    @Override
    public void addId(String name) {
        if (this.fIdIdrefChecking) {
            super.addId(name);
        }
    }

    @Override
    public void addIdRef(String name) {
        if (this.fIdIdrefChecking) {
            super.addIdRef(name);
        }
    }
}
