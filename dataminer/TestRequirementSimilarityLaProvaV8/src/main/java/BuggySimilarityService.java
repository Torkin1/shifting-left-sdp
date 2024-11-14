import java.io.File;
import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

import io.grpc.stub.StreamObserver;
import it.torkin.dataminer.nlp.BuggyTicketsSimilarityMiningGrpc.BuggyTicketsSimilarityMiningImplBase;
import it.torkin.dataminer.nlp.Request.NlpIssueBean;
import it.torkin.dataminer.nlp.Request.NlpIssueRequest;
import it.torkin.dataminer.nlp.Similarity.NlpIssueSimilarityScores;
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
    public StreamObserver<NlpIssueRequest> getSimilarityScores(
            StreamObserver<NlpIssueSimilarityScores> responseObserver) {

        return new StreamObserver<NlpIssueRequest>() {

            @Override
            public void onNext(NlpIssueRequest bean) {
                System.out.println("Received request for " + bean.getDataset() + " " + bean.getProject() + " " + bean.getKey());
                System.out.println("Reading records from " + BuggyRequirementSimilarity.getOutputFileName(bean.getDataset(), bean.getProject()));
                try (Resultset<Map<String, String>> records = new Resultset<>(BuggyRequirementSimilarity.getOutputFileName(bean.getDataset(), bean.getProject()), Map.class)) {
                    while(records.hasNext()){
                        // System.out.println("Reading record");
                        Map<String, String> record = records.next();

                        // TODO: Check measurement date too
                        String dataset = record.get("ProjectName").split("_")[0];
                        String project = record.get("ProjectName").split("_")[1];
                        String key = record.get("RequirementID");

                        // System.out.println("Checking " + dataset + " " + project + " " + key);
                        if (bean.getDataset().equals(dataset) && bean.getProject().equals(project) && bean.getKey().equals(key)){
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
                            responseObserver.onNext(similarityScores);
                            // System.out.println("Sent " + dataset + " " + project + " " + key);
                            break;
                        }

                    }
                        
                } catch (UnableToGetResultsetException | IOException e) {
                    GrpcTools.internalServerError("error while reading csv scores", e);
                }
            }

            @Override
            public void onError(Throwable t) {
                System.out.println("Error while processing similarity scores request");
                GrpcTools.internalServerError("Fatal", error);
            }

            @Override
            public void onCompleted() {
                System.out.println("similarity scores request Completed");
                responseObserver.onCompleted();
            }
        };
}

    
}
