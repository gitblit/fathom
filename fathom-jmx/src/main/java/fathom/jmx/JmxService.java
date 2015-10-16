package fathom.jmx;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.j256.simplejmx.server.JmxServer;
import fathom.Service;
import fathom.conf.Settings;
import fathom.exception.FathomException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.JMException;

/**
 * @author James Moger
 */
@Singleton
public class JmxService implements Service {

    private final Logger log = LoggerFactory.getLogger(JmxService.class);

    @Inject
    Settings settings;

    JmxServer server;

    @Override
    public int getPreferredStartOrder() {
        return 50;
    }

    @Override
    public void start() {
        int jmxPort = settings.getJmxPort();
        if (jmxPort > 0) {
            server = new JmxServer(jmxPort);
            try {
                server.start();
                log.info("jmx://localhost:{} started", jmxPort);
            } catch (JMException e) {
                throw new FathomException("Failed to start JMX server", e);
            }
        }
    }

    @Override
    public void stop() {
        if (server != null) {
            log.info("jmx://localhost:{} is stopping...", server.getRegistryPort());
            server.stop();
            log.info("jmx://localhost:{} stopped", server.getRegistryPort());
        }
    }
}
