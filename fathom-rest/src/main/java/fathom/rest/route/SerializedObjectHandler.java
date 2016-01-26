package fathom.rest.route;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import fathom.conf.Settings;
import fathom.rest.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.pippo.core.route.RouteHandler;

import javax.servlet.http.HttpServletRequest;
import java.io.*;

/**
 * Base class which allows POJOs to be exchanged between two Java processes using Java serialization.
 *
 * @author James Moger
 */
public abstract class SerializedObjectHandler<X, Y> implements RouteHandler<Context> {

    public final static String CONTENT_TYPE = "application/x-java-serialized-object";

    public final static String CLASS_NAME = "class-name";

    public final static String NULL = "NULL";

    private final Logger log = LoggerFactory.getLogger(SerializedObjectHandler.class);

    @Inject
    private Settings settings;

    @Override
    public void handle(Context context) {
        try {
            final String className = context.getHeader(CLASS_NAME);
            if (Strings.isNullOrEmpty(className)) {
                log.debug("Handling serialized object request {}", context.getRequestUri());
            } else {
                log.debug("Handling serialized object request {} receiving {}", context.getRequestUri(), className);
            }
            HttpServletRequest request = context.getRequest().getHttpServletRequest();
            X arg = null;
            try (InputStream is = request.getInputStream()) {
                ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(is));
                arg = (X) in.readObject();
            } catch (EOFException e) {
                // caller sent nothing, can be ok
            } catch (Exception e) {
                sendError(context, e);
                return;
            }

            Y result = execute(context, arg);
            writeObject(context, result);

        } catch (Exception e) {
            sendError(context, e);
        }
    }

    protected abstract Y execute(Context context, X arg) throws Exception;

    private void sendError(Context context, Exception e) {
        log.error("Failed handling serialized object request {}", context.getRequestUri(), e);
        String content = e.getMessage();
        if (settings.isDev()) {
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            e.printStackTrace(printWriter);
            content = stringWriter.toString();
        }
        writeObject(context, content);
    }

    protected void writeObject(Context context, Object result) {
        try {
            context.getResponse()
                .contentType(CONTENT_TYPE)
                .header(CLASS_NAME, result == null ? NULL : result.getClass().getName());

            if (result != null) {
                ObjectOutputStream replyStream = new ObjectOutputStream(context.getResponse().getOutputStream());
                replyStream.writeObject(result);
                replyStream.flush();
            }
        } catch (IOException e) {
        }
    }
}
