package me.kuku.onemanager;

import act.Act;
import act.inject.DefaultValue;
import act.util.Output;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.mvc.annotation.GetAction;
import osgl.version.Version;
import osgl.version.Versioned;

/**
 * A simple hello world app entry
 *
 * Run this app, try to update some of the code, then
 * press F5 in the browser to watch the immediate change
 * in the browser!
 */
@SuppressWarnings("unused")
@Versioned
public class AppEntry {

    /**
     * Version of this application
     */
    public static final Version VERSION = Version.of(AppEntry.class);

    /**
     * A logger instance that could be used through out the application
     */
    public static final Logger LOGGER = LogManager.get(AppEntry.class);

    /**
     * The home (`/`) endpoint.
     *
     * This will accept a query parameter named `who` and
     * render a template (resources/rythm/__package__/AppEntry/home.html),
     * where `__package__` corresponding to the package name, e.g.
     * if your package is `com.mycomp.myproj`, then `__package__`
     * is `com/mycomp/myproj`.
     *
     * @param who
     *      request query parameter to specify the hello target.
     *      default value is `World`.
     */
    @GetAction
    public void home(@DefaultValue("World") @Output String who) {
    }

    public static void main(String[] args) throws Exception {
        Act.start();
    }

}
