package fathom.rest.security;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import fathom.conf.Settings;
import fathom.security.SecurityManager;

/**
 * @author James Moger
 */
@Singleton
public class PassiveBasicTokenAuthenticationHandler extends BasicTokenAuthenticationHandler {


    @Inject
    public PassiveBasicTokenAuthenticationHandler(SecurityManager securityManager, Settings settings) {
        this(securityManager, false, settings.getApplicationName());
    }

    public PassiveBasicTokenAuthenticationHandler(SecurityManager securityManager, boolean createSessions, String realmName) {
        super(securityManager, createSessions, true, realmName);
    }
}
