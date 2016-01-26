package fathom.rest.security;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import fathom.authc.TokenCredentials;
import fathom.conf.Settings;
import fathom.realm.Account;
import fathom.rest.Context;
import fathom.security.SecurityManager;
import ro.pippo.core.route.RouteHandler;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * @author James Moger
 */
@Singleton
public class BasicTokenAuthenticationHandler extends StandardCredentialsHandler implements RouteHandler<Context> {

    private final boolean createSessions;
    private final boolean isPassive;
    private final String challenge;

    @Inject
    public BasicTokenAuthenticationHandler(SecurityManager securityManager, Settings settings) {
        this(securityManager, false, false, settings.getApplicationName());
    }

    public BasicTokenAuthenticationHandler(SecurityManager securityManager, boolean createSessions, boolean isPassive, String realmName) {
        super(securityManager);

        this.createSessions = createSessions;
        this.isPassive = isPassive;
        this.challenge = "Basic realm=\"" + realmName + "\"";
    }

    @Override
    protected boolean isCreateSessions() {
        return createSessions;
    }

    @Override
    public void handle(Context context) {
        if (isAuthenticated(context)) {
            // already authenticated
            if (isCreateSessions()) {
                // touch the session to prolong it's life
                context.touchSession();
            }

            // continue chain
            context.next();

            return;
        }

        String authorization = context.getRequest().getHeader("Authorization");
        if (Strings.isNullOrEmpty(authorization)) {

            if (isPassive) {
                // Authentication is not required for the Request, but the Response may be limited
                context.next();
            } else {
                // ISSUE CHALLENGE
                context.setHeader("WWW-Authenticate", challenge);
                context.getResponse().unauthorized();
            }

        } else if (authorization.toLowerCase().startsWith("token")) {

            // TOKEN AUTH
            String packet = authorization.substring("token".length()).trim();
            TokenCredentials tokenCredentials = new TokenCredentials(packet);

            Account account = securityManager.authenticate(tokenCredentials);
            if (setupContext(context, account)) {
                // continue the chain
                context.next();
            } else {
                context.getResponse().unauthorized();
            }

        } else if (authorization.toLowerCase().startsWith("basic")) {

            // BASIC AUTH
            String packet = authorization.substring("basic".length()).trim();
            String credentials = new String(Base64.getDecoder().decode(packet), StandardCharsets.UTF_8);
            String[] values = credentials.split(":", 2);
            String username = values[0];
            String password = values[1];

            Account account = this.authenticate(username, password);
            if (setupContext(context, account)) {
                context.next();
            } else {
                context.setHeader("WWW-Authenticate", challenge);
                context.getResponse().unauthorized();
            }
        }
    }
}
