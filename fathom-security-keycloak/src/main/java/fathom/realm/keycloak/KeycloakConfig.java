package fathom.realm.keycloak;

import org.keycloak.representations.adapters.config.AdapterConfig;

/**
 * @author James Moger
 */
public class KeycloakConfig extends AdapterConfig {

    public void setRealmPublicKey(String realmKey) {
        super.setRealmKey(realmKey);
    }

    public String getRealmPublicKey() {
        return super.getRealmKey();
    }
}
