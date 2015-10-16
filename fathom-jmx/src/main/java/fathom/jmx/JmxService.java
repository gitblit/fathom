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
import java.net.InetAddress;

/**
 * @author James Moger
 */
@Singleton
public class JmxService implements Service {

    private final Logger log = LoggerFactory.getLogger(JmxService.class);

    @Inject
    Settings settings;

    JmxServer server;

    String serverName;

    @Override
    public int getPreferredStartOrder() {
        return 50;
    }

    @Override
    public void start() {
        int jmxPort = settings.getJmxPort();
        if (jmxPort > 0) {
            InetAddress loopback = InetAddress.getLoopbackAddress();
            server = new JmxServer(loopback, jmxPort);
            serverName = "jmx://" + loopback.getHostAddress() + ":" + jmxPort;
            try {
                server.start();

                log.info("{} started", serverName);
            } catch (JMException e) {
                throw new FathomException("Failed to start JMX server", e);
            }
        }
    }

    @Override
    public void stop() {
        if (server != null) {
            log.info("{} is stopping...", serverName);
            server.stop();
            log.info("{} stopped", serverName);
        }
    }
}
