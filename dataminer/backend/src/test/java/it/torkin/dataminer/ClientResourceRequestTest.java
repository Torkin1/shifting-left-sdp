package it.torkin.dataminer;

import org.junit.Test;

import it.torkin.dataminer.rest.ClientResourceRequest;
import it.torkin.dataminer.rest.UnableToPostResourceException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class ClientResourceRequestTest {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static class TestResource {
        private String name;
        private int age;
    }
    @Data
    private static final class TestResponse{
        private String message;

    }

    private static final String RESOURCE_URL = "localhost:8080/t/myToilet/";
    
    @Test
    public void testPost() throws UnableToPostResourceException{

        ClientResourceRequest<TestResponse> request = new ClientResourceRequest<>(TestResponse.class, RESOURCE_URL);
        TestResponse testResponse = request.postResource(new TestResource("John", 30));
        // TODO: test if response received is correct

    }
    
}
