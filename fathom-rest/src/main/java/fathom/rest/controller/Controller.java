package fathom.rest.controller;

import fathom.rest.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.pippo.core.Request;
import ro.pippo.core.Response;

/**
 * Base class for a Controller.
 *
 * @author James Moger
 */
public class Controller {

    private Context context;

    public final void setContext(Context context) {
        this.context = context;
    }

    public final Context getContext() {
        return context;
    }

    public final Request getRequest() {
        return context.getRequest();
    }

    public final Response getResponse() {
        return context.getResponse();
    }

    public final void redirectTo(String path) {
        getContext().redirect(path);
    }
}
