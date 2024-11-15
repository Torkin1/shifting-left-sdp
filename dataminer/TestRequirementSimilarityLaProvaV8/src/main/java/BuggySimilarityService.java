import java.io.File;
import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import it.torkin.dataminer.nlp.BuggyTicketsSimilarityMiningGrpc.BuggyTicketsSimilarityMiningImplBase;
import it.torkin.dataminer.nlp.Request.NlpIssueBean;
import it.torkin.dataminer.nlp.Request.NlpIssueRequest;
import it.torkin.dataminer.nlp.Similarity.NlpIssueSimilarityScores;
import it.torkin.dataminer.nlp.Similarity.NlpIssueSimilarityVariantsRequest;
import it.torkin.dataminer.nlp.Similarity.NlpIssueSimilarityVariantsResponse;
import it.torkin.dataminer.nlp.Similarity.RegisterNlpIssueBeansResponse;

public class BuggySimilarityService extends BuggyTicketsSimilarityMiningImplBase{

    File jsonFile = new File(BuggyRequirementSimilarity.JSON_FILE_PATH);
    private JsonGenerator jsonGenerator;

    Exception error = null;

    @Override
    public StreamObserver<NlpIssueBean> registerIssueBeans(
            StreamObserver<RegisterNlpIssueBeansResponse> responseObserver) {
        
        File jsonFile = new File(BuggyRequirementSimilarity.JSON_FILE_PATH);
        
        // Create a JsonFactory and JsonGenerator
        JsonFactory jsonFactory = new JsonFactory();
        try {
            jsonGenerator = jsonFactory.createGenerator(jsonFile, com.fasterxml.jackson.core.JsonEncoding.UTF8);
                // Start writing JSON array
                jsonGenerator.writeStartArray();
    
        } catch (IOException e) {
            throw GrpcTools.internalServerError("Error writing JSON array to file", e);
        }
        
        return new StreamObserver<NlpIssueBean>() {
            @Override
            public void onNext(NlpIssueBean value) {
                // TODO Auto-generated method stub
                
            }

            @Override
            public void onError(Throwable t) {
                // TODO Auto-generated method stub
                
            }

            @Override
            public void onCompleted() {
                // End JSON array
                try {
                    jsonGenerator.writeEndArray();

                    // Flush to ensure all data is written to the file
                    jsonGenerator.flush();

                    System.out.println("Successfully wrote JSON array to file.");
                } catch (IOException e) {
                    throw GrpcTools.internalServerError("Error finalizing JSON array to file", e);
                } finally{
                    try {
                        jsonGenerator.close();
                    } catch (IOException e) {
                        throw GrpcTools.internalServerError("Error while closing json file", e);
                    }
                    jsonGenerator = null;
                }
                
            }
        };
    }

    private Requirement beanToRequirement(NlpIssueBean bean){
        // TODO:
        throw new UnsupportedOperationException("Not implemented yet");
    }

    private void serializeRequirement(Requirement requirement){

        // TODO:
        throw new UnsupportedOperationException("Not implemented yet");

    }

    @Override
    public void getSimilarityScores(NlpIssueRequest request,
            StreamObserver<NlpIssueSimilarityScores> responseObserver) {

        responseObserver.onNext(processRequest(request));
        responseObserver.onCompleted();

    }

    private NlpIssueSimilarityScores processRequest(NlpIssueRequest request){
        System.out.println("Received request for " + request.getDataset() + " " + request.getProject() + " " + request.getKey());
        System.out.println("Reading records from " + BuggyRequirementSimilarity.getOutputFileName(request.getDataset(), request.getProject()));
        try (Resultset<Map<String, String>> records = new Resultset<>(BuggyRequirementSimilarity.getOutputFileName(request.getDataset(), request.getProject()), Map.class)) {
            while(records.hasNext()){
                // System.out.println("Reading record");
                Map<String, String> record = records.next();

                // TODO: Check measurement date too
                String dataset = record.get("ProjectName").split("_")[0];
                String project = record.get("ProjectName").split("_")[1];
                String key = record.get("RequirementID");

                // System.out.println("Checking " + dataset + " " + project + " " + key);
                if (request.getDataset().equals(dataset) && request.getProject().equals(project) && request.getKey().equals(key)){
                    System.out.println("Found " + dataset + " " + project + " " + key);
                    NlpIssueSimilarityScores.Builder similarityScoresBuilder = NlpIssueSimilarityScores.newBuilder()
                        .setRequest(
                            NlpIssueRequest.newBuilder()
                                .setDataset(dataset)
                                .setProject(project)
                                .setKey(key)
                        );
                        record.forEach((k, v) -> {
                            if (!k.equals("ProjectName") && !k.equals("RequirementID") && !k.equals("Buggy")){
                                similarityScoresBuilder.putScoreByName(k, StringTools.isBlank(v) ? Double.NaN : Double.parseDouble(v));
                            }
                        });
                    NlpIssueSimilarityScores similarityScores = similarityScoresBuilder.build(); 
                    // System.out.println("Sent " + dataset + " " + project + " " + key);
                    return similarityScores;
                }

            }
                
        } catch (UnableToGetResultsetException | IOException e) {
            throw GrpcTools.internalServerError("error while reading csv scores", e);
        }
        throw Status.NOT_FOUND.withDescription("request has no correspondence in registered nlp beans: " + request.getDataset() + " " + request.getProject() + " " + request.getKey() ).asRuntimeException();
    }

    @Override
    public void getSimilarityVariants(NlpIssueSimilarityVariantsRequest request,
            StreamObserver<NlpIssueSimilarityVariantsResponse> responseObserver) {
        
        NlpIssueSimilarityVariantsResponse response = NlpIssueSimilarityVariantsResponse.newBuilder()
            .addAllVariant(BuggyRequirementSimilarity.getVariantNames())
            .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
    

    
}
