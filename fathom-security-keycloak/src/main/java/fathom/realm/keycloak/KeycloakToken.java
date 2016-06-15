package fathom.realm.keycloak;

import fathom.authc.AuthenticationToken;
import fathom.authc.Credentials;
import org.keycloak.representations.AccessToken;

/**
 * @author James Moger
 */
public class KeycloakToken implements AuthenticationToken, Credentials {

    private final AccessToken accessToken;

    public KeycloakToken(AccessToken accessToken) {
        this.accessToken = accessToken;
    }

    public AccessToken getToken() {
        return accessToken;
    }

    @Override
    public String getUsername() {
        return accessToken.getPreferredUsername();
    }

    @Override
    public Credentials sanitize() {
        return this;
    }
}
