package it.torkin.dataminer.rest;

import java.io.IOException;

import org.restlet.ext.gson.GsonRepresentation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;

import it.torkin.dataminer.rest.parsing.AnnotationExclusionStrategy;

/**
 * Models a REST remote resource request from a client.
 * It expects the response to be in JSON format and it uses Gson to parse it
 * to an object of the specified type.
 */
public class ClientResourceRequest<T> {

    private final Class<T> resourceType;
    private final String resourceUrl;

    public ClientResourceRequest(Class<T> resourceType, String resourceUrl){
        this.resourceType = resourceType;
        this.resourceUrl = resourceUrl;
    }
    
    public T getResource() throws UnableToGetResourceException {
        try {
            GsonRepresentation<T> gsonRepresentation =  new ClientResource(resourceUrl).get(GsonRepresentation.class); 
            gsonRepresentation.setObjectClass(resourceType);
            gsonRepresentation.getBuilder().setExclusionStrategies(new AnnotationExclusionStrategy());
            return gsonRepresentation.getObject();
        } catch (ResourceException | IOException e) {
            
            throw new UnableToGetResourceException(e, resourceUrl, resourceType.getName());
        }

    }
    
}
