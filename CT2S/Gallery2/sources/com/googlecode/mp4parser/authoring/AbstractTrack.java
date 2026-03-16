package com.googlecode.mp4parser.authoring;

public abstract class AbstractTrack implements Track {
    private boolean enabled = true;
    private boolean inMovie = true;
    private boolean inPreview = true;
    private boolean inPoster = true;

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    @Override
    public boolean isInMovie() {
        return this.inMovie;
    }

    @Override
    public boolean isInPreview() {
        return this.inPreview;
    }

    @Override
    public boolean isInPoster() {
        return this.inPoster;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setInMovie(boolean inMovie) {
        this.inMovie = inMovie;
    }

    public void setInPreview(boolean inPreview) {
        this.inPreview = inPreview;
    }

    public void setInPoster(boolean inPoster) {
        this.inPoster = inPoster;
    }
}
