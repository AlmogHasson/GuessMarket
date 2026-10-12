package listener;

import api.GMController;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import manager.ActiveUsersManager;
import manager.ChatManager;
import util.ServerConstants;

@WebListener
public class AppContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent event) {
        ServletContext context = event.getServletContext();

        GMController controller = new GMController();
        context.setAttribute(ServerConstants.DATA_VERSION_ATTRIBUTE, new java.util.concurrent.atomic.AtomicInteger());

        ActiveUsersManager activeUsersManager = new ActiveUsersManager();

        context.setAttribute(ServerConstants.ENGINE_ATTRIBUTE, controller);

        context.setAttribute(ServerConstants.ACTIVE_USERS_ATTRIBUTE, activeUsersManager);

        context.setAttribute(ServerConstants.CHAT_ATTRIBUTE, new ChatManager());
    }
}