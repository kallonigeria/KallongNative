package com.ng.kallo;

public class Config {

    // copy your api url from php admin dashboard & paste below
    public static final String API_SERVER_URL = "https://www.kallo.ng/rest-api";

    //copy your api key from php admin dashboard & paste below
    public static final String API_KEY = "oczkk67p7pyykt9xlk1eytde";

    //copy your terms url from php admin dashboard & paste below
    public static final String TERMS_URL = "https://www.kallo.ng/page/privacy-policy/";

//    public static final String PAYMENT_URL = "";
    // download option for non subscribed user
    public static final boolean ENABLE_DOWNLOAD_TO_ALL = false;

    //enable RTL
    public static boolean ENABLE_RTL = true;

    //youtube video auto play
    public static boolean YOUTUBE_VIDEO_AUTO_PLAY = true;

    //enable external player
    public static final boolean ENABLE_EXTERNAL_PLAYER = true;

    //default theme
    public static boolean DEFAULT_DARK_THEME_ENABLE = true;

    // First, you have to configure firebase to enable facebook, phone and google login
    // facebook authentication
    public static final boolean ENABLE_FACEBOOK_LOGIN = false;

    //Phone authentication
    public static final boolean ENABLE_PHONE_LOGIN = true;

    //Google authentication
    public static final boolean ENABLE_GOOGLE_LOGIN = true;


}
