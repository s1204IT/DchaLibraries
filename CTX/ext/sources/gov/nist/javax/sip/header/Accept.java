package gov.nist.javax.sip.header;

import gov.nist.core.Separators;
import javax.sip.InvalidArgumentException;
import javax.sip.header.AcceptHeader;

public final class Accept extends ParametersHeader implements AcceptHeader {
    private static final long serialVersionUID = -7866187924308658151L;
    protected MediaRange mediaRange;

    public Accept() {
        super("Accept");
    }

    @Override
    public boolean allowsAllContentTypes() {
        return this.mediaRange != null && this.mediaRange.type.compareTo(Separators.STAR) == 0;
    }

    @Override
    public boolean allowsAllContentSubTypes() {
        return this.mediaRange != null && this.mediaRange.getSubtype().compareTo(Separators.STAR) == 0;
    }

    @Override
    protected String encodeBody() {
        return encodeBody(new StringBuffer()).toString();
    }

    @Override
    protected StringBuffer encodeBody(StringBuffer stringBuffer) {
        if (this.mediaRange != null) {
            this.mediaRange.encode(stringBuffer);
        }
        if (this.parameters != null && !this.parameters.isEmpty()) {
            stringBuffer.append(';');
            this.parameters.encode(stringBuffer);
        }
        return stringBuffer;
    }

    public MediaRange getMediaRange() {
        return this.mediaRange;
    }

    @Override
    public String getContentType() {
        if (this.mediaRange == null) {
            return null;
        }
        return this.mediaRange.getType();
    }

    @Override
    public String getContentSubType() {
        if (this.mediaRange == null) {
            return null;
        }
        return this.mediaRange.getSubtype();
    }

    @Override
    public float getQValue() {
        return getParameterAsFloat("q");
    }

    @Override
    public boolean hasQValue() {
        return super.hasParameter("q");
    }

    @Override
    public void removeQValue() {
        super.removeParameter("q");
    }

    @Override
    public void setContentSubType(String str) {
        if (this.mediaRange == null) {
            this.mediaRange = new MediaRange();
        }
        this.mediaRange.setSubtype(str);
    }

    @Override
    public void setContentType(String str) {
        if (this.mediaRange == null) {
            this.mediaRange = new MediaRange();
        }
        this.mediaRange.setType(str);
    }

    @Override
    public void setQValue(float f) throws InvalidArgumentException {
        if (f == -1.0f) {
            super.removeParameter("q");
        }
        super.setParameter("q", f);
    }

    public void setMediaRange(MediaRange mediaRange) {
        this.mediaRange = mediaRange;
    }

    @Override
    public Object clone() {
        Accept accept = (Accept) super.clone();
        if (this.mediaRange != null) {
            accept.mediaRange = (MediaRange) this.mediaRange.clone();
        }
        return accept;
    }
}
