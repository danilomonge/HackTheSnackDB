package de.pecus.api.services.nutrition;

/** A USDA or DeepL request failed. The message never contains URLs or keys. */
public class UpstreamException extends Exception {

    private static final long serialVersionUID = 1L;

    private final int status;

    public UpstreamException(String service, int status) {
        super(status > 0 ? service + " request failed with HTTP " + status : service + " request failed");
        this.status = status;
    }

    /** Upstream HTTP status, or 0 when no response arrived. */
    public int getStatus() {
        return status;
    }
}
