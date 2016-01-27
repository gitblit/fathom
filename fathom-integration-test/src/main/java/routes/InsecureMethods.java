package routes;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dao.ItemDao;
import fathom.xmlrpc.XmlRpc;

/**
 * @author James Moger
 */
@Singleton
@XmlRpc("insecure")
public class InsecureMethods {

    @Inject
    ItemDao itemDao;

    @XmlRpc
    public int min(int a, int b) {
        return Math.min(a, b);
    }

    @XmlRpc("nameOfItem")
    public String itemName(int id) {
        return itemDao.get(id).getName();
    }

}
