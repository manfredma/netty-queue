package org.mitallast.queue.rest.action;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.mitallast.queue.client.Service;
import org.mitallast.queue.common.settings.Settings;
import org.mitallast.queue.rest.*;

import java.io.IOException;
import java.io.OutputStream;

public class IndexAction extends BaseRestHandler {

    public IndexAction(Settings settings, Service service, RestController controller) {
        super(settings, service);
        controller.registerHandler(HttpMethod.GET, "/", this);
        controller.registerHandler(HttpMethod.HEAD, "/", this);
    }

    @Override
    public void handleRequest(RestRequest request, RestSession session) {
        JsonRestResponse restResponse = new JsonRestResponse(HttpResponseStatus.OK);
        JsonFactory factory = new JsonFactory();
        try (OutputStream stream = restResponse.getOutputStream()) {
            JsonGenerator generator = factory.createGenerator(stream);
            generator.writeStartObject();
            generator.writeStringField("message", "You now, for queue");
            generator.writeEndObject();
            generator.close();
            session.sendResponse(restResponse);
        } catch (IOException e) {
            session.sendResponse(e);
        }
    }
}
