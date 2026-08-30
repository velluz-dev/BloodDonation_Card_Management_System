package com.bdcms.shared.i18n;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.jsp.jstl.core.Config;

import java.io.IOException;
import java.util.Locale;

/**
 * Decides the language for every request, once, before anything renders.
 *
 * <p>Runs immediately after the encoding filter and before authentication, so
 * the login page and the error pages are translated too - the screens an
 * operator is most likely to be stuck on are the last place to fall back to
 * English.
 *
 * <p>It publishes the choice in three forms because three different consumers
 * need it:
 *
 * <ul>
 *   <li>{@link Config#FMT_LOCALE} on the request, which is what makes a bare
 *       {@code <fmt:message key="..."/>} resolve in a JSP with no per-page
 *       setup. Paired with the {@code fmt.localizationContext} context
 *       parameter in {@code web.xml}, that is the whole JSP-side wiring.</li>
 *   <li>{@code request.setAttribute("locale", …)} and {@code "lang"}, for the
 *       switcher in the top bar and the {@code <html lang>} attribute.</li>
 *   <li>{@link HttpServletResponse#setLocale}, which sets {@code
 *       Content-Language} and, more usefully, the charset defaults.</li>
 * </ul>
 */
public class LocaleFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        Locale locale = Locales.resolve(request);

        // Only write the cookie when the language was actually chosen on this
        // request. Rewriting it on every request would be harmless but noisy,
        // and would hide which request did the switching in a HAR capture.
        if (Locales.parse(request.getParameter(Locales.PARAM)) != null) {
            Locales.remember(request, response, locale);
        }

        Config.set(request, Config.FMT_LOCALE, locale);
        request.setAttribute("locale", locale);
        request.setAttribute("lang", locale.getLanguage());
        request.setAttribute("langSwitchBase", switchBase(request));
        response.setLocale(locale);

        chain.doFilter(request, response);
    }

    /**
     * This page's own URL with any {@code lang} parameter stripped, ready for
     * the switcher to append a new one.
     *
     * <p>Switching language should leave the operator exactly where they were,
     * mid-search and mid-filter. Computing that here rather than in each JSP
     * means the login page, the error pages and the 21 views all get it right
     * without repeating query-string handling in EL - and it avoids redirecting
     * to a {@code Referer}, which would be an open redirect wearing a hat.
     */
    private static String switchBase(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        if (query == null || query.isEmpty()) {
            return uri + "?";
        }
        StringBuilder kept = new StringBuilder();
        for (String pair : query.split("&")) {
            // Drop the existing choice, or switching twice would stack
            // ?lang=my&lang=en and leave the first one winning.
            if (pair.isEmpty() || pair.equals(Locales.PARAM)
                    || pair.startsWith(Locales.PARAM + "=")) {
                continue;
            }
            kept.append(pair).append('&');
        }
        return uri + "?" + kept;
    }
}
