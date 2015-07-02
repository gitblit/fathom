package ${package};

import com.google.common.base.Throwables;
import fathom.Boot;

/**
 * Launch entry point.
 * <p>
 * Run this class as an Application.
 * </p>
 */
public class Launcher {

    public static void main(String... args) {
        try {
            Boot boot = new Boot(args);
            boot.addShutdownHook().start();
        } catch (Exception e) {
            Exception root = (Exception) Throwables.getRootCause(e);
            root.printStackTrace();
            System.exit(1);
        }
    }
}
