package servlets;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import manager.ActiveUsersManager;
import util.ServerConstants;
import util.ServletUtils;

import java.io.IOException;

@WebServlet("/logout")
public class LogoutServlet extends HttpServlet {

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response)
            throws IOException {

        HttpSession session = request.getSession(false);

        if (session == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().print("No active session found.");
            return;
        }

        String userName =
                (String) session.getAttribute(
                        ServerConstants.USERNAME_SESSION_ATTRIBUTE
                );

        if (userName == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().print(
                    "Session exists, but no username was found."
            );
            return;
        }

        ActiveUsersManager activeUsersManager =
                ServletUtils.getActiveUsersManager(
                        getServletContext()
                );

        boolean wasLoggedIn =
                activeUsersManager.isLoggedIn(userName);

        activeUsersManager.logout(userName);

        session.invalidate();

        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().print(
                "Logged out: " + userName +
                        ", wasActive=" + wasLoggedIn
        );
    }
}
