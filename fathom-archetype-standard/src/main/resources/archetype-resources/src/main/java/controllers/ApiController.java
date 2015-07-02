package ${package}.controllers;

import fathom.rest.controller.Controller;
import fathom.rest.controller.Path;

/**
 * To be discoverable, a controller must be annotated with {@code @Path}.
 */
@Path("/api")
public abstract class ApiController extends Controller {

}
