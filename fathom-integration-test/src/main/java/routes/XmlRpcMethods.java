package routes;

import com.google.inject.Inject;
import fathom.xmlrpc.XmlRpcMethodRegistrar;
import fathom.xmlrpc.XmlRpcRouteHandler;

/**
 * @author James Moger
 */

public class XmlRpcMethods extends XmlRpcRouteHandler {

    @Inject
    public XmlRpcMethods(XmlRpcMethodRegistrar xmlRpcMethodRegistrar) {
        super(xmlRpcMethodRegistrar);

        xmlRpcMethodRegistrar.addMethodGroup(InsecureMethods.class);
        xmlRpcMethodRegistrar.addMethodGroup(SecureMethods.class);
    }
}
