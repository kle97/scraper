package io.playground.scraper.core.side;

import io.playground.scraper.core.UCDriver;
import io.playground.scraper.model.response.cookie.CookieParams;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.logging.Logs;

import java.sql.Date;
import java.util.HashSet;
import java.util.Set;

public class UCOptions implements WebDriver.Options {

    private final UCDriver driver;

    public UCOptions(UCDriver driver) {
        this.driver = driver;
    }
    
    @Override
    public void addCookie(Cookie cookie) {
        CookieParams params = new CookieParams(cookie.getName(), cookie.getValue(), null, cookie.getDomain(), 
                                               cookie.getPath(), cookie.isSecure(), cookie.isHttpOnly(), 
                                               cookie.getSameSite(), cookie.getExpiry().toString(),
                                               null, null, null, null, null);
        driver.getClient().setCookie(params);
    }

    @Override
    public void deleteCookieNamed(String name) {
        CookieParams params = new CookieParams(name, null, null, null, null, null, null, 
                                               null, null, null, null, null, null, null);
        driver.getClient().deleteCookie(params);
    }

    @Override
    public void deleteCookie(Cookie cookie) {
        CookieParams params = new CookieParams(cookie.getName(), null, null, cookie.getDomain(), cookie.getPath(), 
                                               null, null, null, null, null, null, null, null, null);
        driver.getClient().deleteCookie(params);
    }

    @Override
    public void deleteAllCookies() {
        driver.getClient().deleteAllCookies();
    }

    @Override
    public Set<Cookie> getCookies() {
        Set<Cookie> cookies = new HashSet<>();
        CookieParams cookieParams = driver.getClient().getCookies();
        if (cookieParams != null) {
            cookies.add(new Cookie(cookieParams.name(), cookieParams.value(), cookieParams.domain(), cookieParams.path(),
                                   Date.valueOf(cookieParams.expires()), cookieParams.secure(), 
                                   cookieParams.httpOnly(), cookieParams.sameSite()));
        }
        return cookies;
    }

    @Override
    public Cookie getCookieNamed(String name) {
        for (Cookie cookie : getCookies()) {
            if (cookie.getName().equals(name)) {
                return cookie;
            }
        }
        return null;
    }

    @Override
    public WebDriver.Timeouts timeouts() {
        return new UCTimeouts(driver);
    }

    @Override
    public WebDriver.Window window() {
        return new UCWindow(driver);
    }

    @Override
    public Logs logs() {
        throw new UnsupportedOperationException();
    }
}
