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

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

        String userName = request.getParameter(ServerConstants.USERNAME_PARAMETER);

        if (userName == null || userName.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().print("User name cannot be empty.");
            return;
        }

        userName = userName.trim();

        ActiveUsersManager activeUserManager = ServletUtils.getActiveUsersManager(getServletContext());

        boolean loginSucceeded = activeUserManager.login(userName);

        if (!loginSucceeded) {
            response.setStatus(HttpServletResponse.SC_CONFLICT);
            response.getWriter().print("User is already logged in.");
            return;
        }

        try
        {
            ServletUtils.getController(getServletContext()).getOrCreateUser(userName);

            HttpSession session = request.getSession(true);

            session.setAttribute(ServerConstants.USERNAME_SESSION_ATTRIBUTE, userName);

            response.setStatus(HttpServletResponse.SC_OK);

        } catch (RuntimeException e) {
            activeUserManager.logout(userName);
            throw e;
        }
    }
}
