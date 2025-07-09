package dev.vlab.tweetsms.helper;

public class UrlContainer {
    private static final String DOMAIN = "https://localtext.businesslocal.com.au/";
    private static final String BASE_URL = DOMAIN + "/api/v1/";
    private static final String LOGIN = BASE_URL + "login";
    private static final String FORGOT = BASE_URL + "password/email";

    // Getters if needed
    public static String getBaseUrl() {
        return BASE_URL;
    }

    public static String getLogin() {
        return LOGIN;
    }
}
