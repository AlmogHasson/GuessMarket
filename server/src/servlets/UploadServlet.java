package servlets;

import api.GMController;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import util.ServletUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@WebServlet("/upload")
@MultipartConfig
public class UploadServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String userName = ServletUtils.getLoggedInUserName(request);

        if (userName == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().print("User is not logged in.");
            return;
        }

        Part filePart = request.getPart("file");

        if (filePart == null || filePart.getSize() == 0) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().print("No XML file was provided.");
            return;
        }

        Path tempFile = Files.createTempFile(
                "guess-market-",
                ".xml"
        );

        try {
            filePart.write(tempFile.toString());

            GMController controller = ServletUtils.getController(getServletContext());
            controller.loadFile(tempFile.toString(), userName);

            response.setStatus(HttpServletResponse.SC_OK);

        } catch (Exception e) {

            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);

            response.getWriter().print(
                    e.getMessage() == null
                            ? "Could not load file."
                            : e.getMessage()
            );

        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
}