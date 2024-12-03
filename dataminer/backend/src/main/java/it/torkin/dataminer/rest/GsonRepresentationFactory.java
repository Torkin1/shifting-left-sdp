package it.torkin.dataminer.rest;

import org.restlet.ext.gson.GsonRepresentation;
import org.restlet.resource.ClientResource;

import it.torkin.dataminer.rest.parsing.AnnotationExclusionStrategy;

public class GsonRepresentationFactory {
    
    private static GsonRepresentationFactory instance;

    private GsonRepresentationFactory() {
        // private constructor to prevent instantiation
    }

    public static synchronized GsonRepresentationFactory getInstance() {
        if (instance == null) {
            instance = new GsonRepresentationFactory();
        }
        return instance;
    }
    
    public <T> GsonRepresentation<T> create(T resource, Class<T> resourceType) {
        GsonRepresentation<T> gsonRepresentation = new GsonRepresentation<>(resource);
        init(gsonRepresentation, resourceType);
        return gsonRepresentation;
    }

    private <T> GsonRepresentation<T> init(GsonRepresentation<T> gsonRepresentation, Class<T> resourceClass){
        if (gsonRepresentation == null) return null;
        gsonRepresentation.setObjectClass(resourceClass);
        gsonRepresentation.getBuilder().setExclusionStrategies(new AnnotationExclusionStrategy());
        return gsonRepresentation;
    }

    public <T> GsonRepresentation<T> get(Class<T> resourceClass, String resourceUrl){
        GsonRepresentation<T> gsonRepresentation = new ClientResource(resourceUrl).get(GsonRepresentation.class);
        init(gsonRepresentation, resourceClass);
        return gsonRepresentation; 
    }

    // https://stackoverflow.com/questions/35359796/post-to-server-using-java-restlet
    public <R, P> GsonRepresentation<R> post(P payload, Class<R> resourceClass, String resourceUrl){
        GsonRepresentation<P> payloadRepresentation = create(payload, (Class<P>)payload.getClass());
        GsonRepresentation<R> gsonRepresentation = new ClientResource(resourceUrl).post(payloadRepresentation, GsonRepresentation.class);
        init(gsonRepresentation, resourceClass);
        return gsonRepresentation;
    }
}
