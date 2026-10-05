package util;

import api.GMController;
import jakarta.servlet.ServletContext;
import manager.ActiveUsersManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.util.concurrent.atomic.AtomicInteger;

public final class ServletUtils {

    private ServletUtils() {}

    public static GMController getController(ServletContext context) {
        return (GMController) context.getAttribute(ServerConstants.ENGINE_ATTRIBUTE);
    }

    public static ActiveUsersManager getActiveUsersManager(ServletContext context) {
        return (ActiveUsersManager) context.getAttribute(ServerConstants.ACTIVE_USERS_ATTRIBUTE);
    }

    public static String getLoggedInUserName(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session == null) {
            return null;
        }

        return (String) session.getAttribute(ServerConstants.USERNAME_SESSION_ATTRIBUTE);
    }


    public static AtomicInteger getDataVersion(ServletContext context) {
        return (AtomicInteger) context.getAttribute(ServerConstants.DATA_VERSION_ATTRIBUTE);
    }
}