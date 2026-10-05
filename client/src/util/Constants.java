package util;

/**
 * Every address and shared parameter name the client needs, in one place.
 *
 * The exercise lets us assume the server is at localhost:8080 and that the WAR
 * keeps the name we submit it under, so CONTEXT_PATH must match that file name
 * exactly: a WAR deployed as guess-market.war answers under /guess-market.
 * Change it here only, never at a call site.
 */
public final class Constants {

    private Constants() {
    }

    private static final String PROTOCOL = "http";
    private static final String DOMAIN   = "localhost:8080";

    /** Must equal the submitted WAR's file name, without the .war suffix. */
    private static final String CONTEXT_PATH = "/guess-market";

    public static final String BASE_URL = PROTOCOL + "://" + DOMAIN + CONTEXT_PATH;

    // ---- endpoints ----
    public static final String LOGIN  = BASE_URL + "/login";
    public static final String LOGOUT = BASE_URL + "/logout";
    public static final String USERS  = BASE_URL + "/users";
    public static final String EVENTS = BASE_URL + "/events";
    public static final String EVENT_STATUS = BASE_URL + "/event/status";
    public static final String EVENT_STATS  = BASE_URL + "/event/stats";
    public static final String EVENT_PARTICIPANTS = BASE_URL + "/event/participants";
    public static final String CURRENT_USER = BASE_URL + "/user";
    public static final String UPLOAD = BASE_URL + "/upload";

    // ---- parameter names, shared with the servlets ----
    public static final String USERNAME_PARAM = "username";

    /** How often the client re-polls the server for changed data (ex3 pull). */
    public static final int REFRESH_RATE_MILLIS = 500;
    public static final String VERSION = BASE_URL + "/version";

}
