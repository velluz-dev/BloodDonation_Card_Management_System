package com.bdcms.shared.i18n;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;
import java.util.Locale;

/**
 * Which language the interface is in, and how that choice is remembered.
 *
 * <h2>Why a cookie and not the session</h2>
 *
 * <p>The obvious place to keep the choice is the session. It is also the wrong
 * one: signing in invalidates the session to defeat fixation, so an operator
 * who switches the login page to Burmese and then signs in would land on an
 * English dashboard. The cookie outlives that, and outlives signing out too,
 * which is what a shared counter machine actually wants - set the language
 * once, and every shift that follows gets it.
 *
 * <p>Nothing here is a security decision, so the cookie carries no risk beyond
 * disclosing a language preference.
 */
public final class Locales {

    /** English, the fallback and the language the source strings are written in. */
    public static final Locale ENGLISH = Locale.ENGLISH;

    /** Burmese. ISO 639-1 {@code my}, matching {@code messages_my.properties}. */
    public static final Locale MYANMAR = Locale.of("my");

    /** The languages the switcher offers, in the order it offers them. */
    public static final List<Locale> SUPPORTED = List.of(ENGLISH, MYANMAR);

    static final String COOKIE = "bdcms_lang";

    /** Request parameter that switches language, e.g. {@code ?lang=my}. */
    public static final String PARAM = "lang";

    private static final int ONE_YEAR = 60 * 60 * 24 * 365;

    private Locales() {
    }

    /** English when {@code locale} is null, so callers need not null-check. */
    static Locale orDefault(Locale locale) {
        return locale == null ? ENGLISH : locale;
    }

    /**
     * The locale for {@code tag}, or null if it is not one we support.
     *
     * <p>Unsupported values are rejected rather than passed to
     * {@code Locale.of}: {@code ?lang=` } reaching a bundle lookup is a way of
     * probing the classpath, and there are only ever two right answers.
     */
    public static Locale parse(String tag) {
        if (tag == null) {
            return null;
        }
        String wanted = tag.trim().toLowerCase(Locale.ROOT);
        for (Locale supported : SUPPORTED) {
            if (supported.getLanguage().equals(wanted)) {
                return supported;
            }
        }
        return null;
    }

    /**
     * The language this request should be rendered in.
     *
     * <p>In order: an explicit {@code ?lang=} on this request, the remembered
     * cookie, then the browser's {@code Accept-Language}, then English. The
     * browser header is consulted last but does mean a Burmese-configured
     * machine gets Burmese on its very first visit, before anyone has chosen.
     */
    public static Locale resolve(HttpServletRequest request) {
        Locale chosen = parse(request.getParameter(PARAM));
        if (chosen != null) {
            return chosen;
        }
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (COOKIE.equals(cookie.getName())) {
                    Locale remembered = parse(cookie.getValue());
                    if (remembered != null) {
                        return remembered;
                    }
                }
            }
        }
        for (Locale accepted : java.util.Collections.list(request.getLocales())) {
            Locale supported = parse(accepted.getLanguage());
            if (supported != null) {
                return supported;
            }
        }
        return ENGLISH;
    }

    /**
     * The locale {@link LocaleFilter} settled for this request.
     *
     * <p>Falls back to {@link #resolve} if the filter did not run - which
     * happens in tests, and would otherwise make a servlet throw rather than
     * simply answer in English.
     */
    public static Locale current(HttpServletRequest request) {
        Object attribute = request.getAttribute("locale");
        return attribute instanceof Locale locale ? locale : resolve(request);
    }

    /** Writes the choice back so the next request, and the next shift, keeps it. */
    public static void remember(HttpServletRequest request, HttpServletResponse response,
                                Locale locale) {
        Cookie cookie = new Cookie(COOKIE, locale.getLanguage());
        cookie.setPath(request.getContextPath().isEmpty() ? "/" : request.getContextPath());
        cookie.setMaxAge(ONE_YEAR);
        cookie.setHttpOnly(true);
        response.addCookie(cookie);
    }
}
