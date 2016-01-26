package fathom.rest.route;

import fathom.exception.FathomException;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;

public class SerializedObjectCaller {

    public final static String CONTENT_TYPE = "application/x-java-serialized-object";

    public final static String CLASS_NAME = "class-name";

    public final static String NULL = "NULL";

    public static <X> X call(URL endpointUrl, Object... args) throws ClassNotFoundException, IOException {
        URLConnection connection = endpointUrl.openConnection();
        connection.setUseCaches(false);
        connection.setDoOutput(true);

        if (args != null && args.length > 0) {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            ObjectOutputStream serializedObject = new ObjectOutputStream(byteStream);
            Object object;
            if (args.length == 1) {
                object = args[0];
            } else {
                object = new ArrayList<>(Arrays.asList(args));
            }

            connection.setRequestProperty(CLASS_NAME, object.getClass().getName());
            serializedObject.writeObject(object);

            connection.setRequestProperty("Content-Type", CONTENT_TYPE);
            connection.setRequestProperty("Content-Length", String.valueOf(byteStream.size()));
            try (OutputStream os = connection.getOutputStream()) {
                os.write(byteStream.toByteArray());
                os.flush();
            }
        }

        Object reply = null;
        try (InputStream is = connection.getInputStream()) {
            final String contentType = connection.getContentType();
            final String className = connection.getHeaderField(CLASS_NAME);
            if (!NULL.equals(className)) {
                if (contentType.contains("gzip")) {
                    ObjectInputStream in = new ObjectInputStream(new GZIPInputStream(new BufferedInputStream(is)));
                    reply = in.readObject();
                } else {
                    ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(is));
                    reply = in.readObject();
                }
            }
        }

        if (reply instanceof Exception) {
            throw new FathomException("Remote failure for {}", endpointUrl.toString(), reply);
        }

        return (X) reply;
    }

}
