package servlets;

import dto.ChatMessageDTO;
import dto.request.ChatRequestDTO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import manager.ChatManager;
import util.GsonProvider;
import util.ServletUtils;

import java.io.IOException;
import java.util.List;

/**
 * A servlet for handling chat requests.
 */
@WebServlet("/chat")
public class ChatServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (ServletUtils.validateUserSession(request) == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        int since = 0;
        try {
            since = Integer.parseInt(request.getParameter("since"));
        } catch (NumberFormatException ignored) { }

        List<ChatMessageDTO> messages =
                ServletUtils.getChatManager(getServletContext()).getSince(since);

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().print(GsonProvider.GSON.toJson(messages));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setCharacterEncoding("UTF-8");

        String userName = ServletUtils.validateUserSession(request);
        if (userName == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().print("User is not logged in.");
            return;
        }

        try {
            ChatRequestDTO body = GsonProvider.GSON.fromJson(request.getReader(), ChatRequestDTO.class);
            if (body == null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().print("Missing message.");
                return;
            }
            ServletUtils.getChatManager(getServletContext()).add(userName, body.text());
        } catch (IllegalArgumentException ex) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().print(ex.getMessage());
        }
    }
}