package fathom.rest.route;

import fathom.rest.Context;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Extension of SerializedObjectHandler which gzips the outgoing serialized POJO.
 *
 * @author James Moger
 */
public abstract class GzipSerializedObjectHandler<X, Y> extends SerializedObjectHandler<X, Y> {

    public final static String CONTENT_TYPE = "application/x-gzip-java-serialized-object";

    @Override
    protected void writeObject(Context context, Object result) {
        try {
            context.getResponse()
                .contentType(CONTENT_TYPE)
                .header(CLASS_NAME, result == null ? NULL : result.getClass().getName());

            if (result != null) {
                GZIPOutputStream zipStream = new GZIPOutputStream(new BufferedOutputStream(context.getResponse().getOutputStream()));
                ObjectOutputStream replyStream = new ObjectOutputStream(zipStream);
                replyStream.writeObject(result);
                zipStream.finish();
                replyStream.flush();
            }
        } catch (IOException e) {
        }
    }
}
