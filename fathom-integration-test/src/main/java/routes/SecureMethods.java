package routes;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dao.ItemDao;
import fathom.rest.security.aop.RequireAuthenticated;
import fathom.rest.security.aop.RequirePermission;
import fathom.xmlrpc.XmlRpc;

/**
 * @author James Moger
 */
@Singleton
@XmlRpc("secure")
public class SecureMethods {

    @Inject
    ItemDao itemDao;

    @RequireAuthenticated
    @XmlRpc
    public int min(int a, int b) {
        return Math.min(a, b);
    }

    @RequirePermission("secure:view")
    @XmlRpc("nameOfItem")
    public String itemName(int id) {
        return itemDao.get(id).getName();
    }

}
