package net.robinx.lib.http.config;

/**
 * For the default request body definition request method
 *
 * @author Robin
 * @since 2015-05-13 19:31:46
 */
public class HttpMethod {
    private HttpMethod() {
    }

    public static final int GET = 0,
            POST = 1,
            PUT = 2,
            DELETE = 3,
            HEAD = 4,
            OPTIONS = 5,
            TRACE = 6,
            PATCH = 7;
}