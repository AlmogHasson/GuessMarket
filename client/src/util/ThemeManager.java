package util;

import javafx.scene.Scene;

import java.net.URL;
import java.util.List;

/**
 * One owner for the active theme.
 *
 * Both the login page and the top bar let the user change theme, and both have
 * to resolve the same stylesheet the same way. Keeping that here avoids the two
 * failure modes this project has already hit once each: adding a stylesheet
 * instead of replacing it (two themes stay live and the older one keeps
 * winning), and putting a stylesheet on a node instead of the scene (node
 * stylesheets sit above scene ones in the cascade, so the swap does nothing).
 *
 * The path is absolute - "/themes/x.css" from the classpath root, which is
 * client/src/themes. A relative lookup would resolve against this class's own
 * package and break the moment the class moves.
 */
public final class ThemeManager {

    private ThemeManager() {
    }

    public static final List<String> THEMES = List.of("Light", "Dark", "Japanese");

    private static final String DEFAULT_THEME = "Dark";

    private static String current = DEFAULT_THEME;

    public static String getCurrent() {
        return current;
    }

    /**
     * Replaces whatever stylesheet the scene is using with the named theme.
     * setAll, not add: the scene must end up with exactly one theme on it.
     */
    public static void apply(Scene scene, String themeName) {
        if (scene == null || themeName == null) {
            return;
        }

        String path = "/themes/" + themeName.toLowerCase() + ".css";
        URL url = ThemeManager.class.getResource(path);

        if (url == null) {
            System.err.println("Theme not found on the classpath: " + path);
            return;
        }

        current = themeName;
        scene.getStylesheets().setAll(url.toExternalForm());
    }

    /** Applies whatever theme is already current - used when a scene is built. */
    public static void apply(Scene scene) {
        apply(scene, current);
    }
}
