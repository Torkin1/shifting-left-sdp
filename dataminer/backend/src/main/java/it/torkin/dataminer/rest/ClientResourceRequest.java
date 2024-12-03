package it.torkin.dataminer.rest;

import java.io.IOException;

import org.restlet.ext.gson.GsonRepresentation;
import org.restlet.resource.ResourceException;

/**
 * Models a REST remote resource request from a client.
 * It expects the response to be in JSON format and it uses Gson to parse it
 * to an object of the specified type.
 */
public class ClientResourceRequest<R> {

    private final Class<R> resourceType;
    private final String resourceUrl;
    private final GsonRepresentationFactory factory = GsonRepresentationFactory.getInstance();

    public ClientResourceRequest(Class<R> resourceType, String resourceUrl){
        this.resourceType = resourceType;
        this.resourceUrl = resourceUrl;
    }
    
    /**
     * Sends a GET request to the specified resource URL.
     */
    public R getResource() throws UnableToGetResourceException {
        try {
            GsonRepresentation<R> gsonRepresentation = factory.get(resourceType, resourceUrl);
            if (gsonRepresentation == null) {
                throw new UnableToGetResourceException(resourceUrl, resourceType.getName());
            }
            return gsonRepresentation.getObject();
        } catch (ResourceException | IOException e) {
            
            throw new UnableToGetResourceException(e, resourceUrl, resourceType.getName());
        }

    }
    /**
     * Sends a POST request to the specified resource URL with the specified payload.
     * Can return null if the response is empty.
     * @param <P>
     * @param payload
     * @return
     * @throws UnableToPostResourceException
     */
    public <P> R postResource(P payload) throws UnableToPostResourceException {
        try {
            GsonRepresentation<R> gsonRepresentation = factory.post(payload, resourceType, resourceUrl);
            if (gsonRepresentation == null) return null;
            return gsonRepresentation.getObject();
        } catch (ResourceException | IOException e) {
            throw new UnableToPostResourceException(e, resourceUrl);
        }
    }
    
}
