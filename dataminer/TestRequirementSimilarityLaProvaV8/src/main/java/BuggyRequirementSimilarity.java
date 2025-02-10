import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class BuggyRequirementSimilarity {

    public static final String JSON_FILE_PATH = "./data/requirements.json";
    
    public static final Set<String> METHODS = Set.of("TF-IDF_Cosine", "Jaccard", "EuclideanDistance", "BagOfWords_Cosine", "ExactMatch", "DiceCoefficient", "Hamming", "OverlappingCoefficient", "Levenshtein", "CommonTokenOverlap");
    public static final Set<String> FIELDS = Set.of("Title", "Text");
    public static final Set<String> AGGREGATION = Set.of("MaxSimilarity", "AvgSimilarity");

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final Set<String> CUSTOM_STOP_WORDS = new HashSet<>(Arrays.asList(
            "a", "an", "and", "are", "as", "at", "be", "by", "for", "from", "has", "he", "in", "is", "it", "its", "of", "on", "that", "the", "to", "was", "were", "will", "with",
            "about", "above", "across", "after", "again", "against", "all", "also", "am", "among", "amongst", "amount", "another", "any", "anyhow", "anyone", "anything", "anyway", "anywhere",
            "back", "because", "before", "behind", "below", "beside", "besides", "between", "beyond", "both", "but", "can", "cannot", "could", "did", "do", "does", "done", "down", "due", "during",
            "each", "either", "enough", "even", "ever", "every", "everyone", "everything", "everywhere", "except", "few", "first", "former", "formerly", "get", "give", "go", "had", "hence", "here",
            "hereafter", "hereby", "herein", "hereupon", "how", "however", "if", "into", "isn't", "itself", "just", "last", "latter", "latterly", "least", "less", "made", "many", "may", "meanwhile",
            "might", "more", "moreover", "most", "mostly", "much", "must", "myself", "name", "namely", "neither", "never", "nevertheless", "next", "nobody", "none", "noone", "nothing", "now",
            "nowhere", "off", "often", "once", "one", "only", "onto", "other", "others", "otherwise", "our", "ours", "ourselves", "out", "over", "own", "part", "per", "perhaps", "please",
            "put", "rather", "same", "see", "seem", "seemed", "seeming", "seems", "several", "she", "should", "since", "some", "somehow", "someone", "something", "sometime", "sometimes",
            "somewhere", "still", "such", "take", "than", "that", "their", "theirs", "them", "themselves", "then", "thence", "there", "thereafter", "thereby", "therefore", "therein", "thereupon",
            "these", "they", "this", "those", "though", "through", "throughout", "thru", "thus", "together", "too", "toward", "towards", "under", "until", "upon", "us", "very", "via", "wasn't",
            "we", "well", "were", "what", "whatever", "when", "whence", "whenever", "where", "whereafter", "whereas", "whereby", "wherein", "whereupon", "wherever", "whether", "which", "while",
            "whither", "who", "whoever", "whole", "whom", "whose", "why", "will", "with", "within", "without", "would", "yet", "you", "your", "yours", "yourself", "yourselves"
    ));

    private static String buildVariantName(String method, String field, String aggregation) {
        return aggregation + "_" + method + "_" + field;
    }

    public static List<String> getVariantNames(){
        List<String> variantNames = new ArrayList<>(FIELDS.size() * METHODS.size() * AGGREGATION.size());
        for (String field : FIELDS) {
            for (String method : METHODS) {
                for(String aggregation : AGGREGATION){
                    variantNames.add(buildVariantName(method, field, aggregation));
                }
            }
        }
        return variantNames;
    }

    public static void main(String[] args) {
        
        new File("./data").mkdir();
        try {
            List<Requirement> requirements = loadRequirementsFromJSON(JSON_FILE_PATH);
            Set<String> projectDatasetCombinations = requirements.stream()
                    .map(req -> req.getDataset() + "_" + req.getProjectName())
                    .collect(Collectors.toSet());

            ExecutorService executorService = Executors.newFixedThreadPool(projectDatasetCombinations.size());

            for (String combination : projectDatasetCombinations) {
                executorService.execute(() -> {
                    String[] parts = combination.split("_");
                    processProject(requirements, parts[1], parts[0]);
                });
            }

            executorService.shutdown();
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getOutputFileName(String dataset, String project){
        return "./data/"+dataset + "_" + project + "_similarity_results.csv";
    }
    
    private static void processProject(List<Requirement> requirements, String projectName, String dataset) {
        try {
            // Filter requirements by dataset and project
            List<Requirement> projectRequirements = requirements.stream()
                    .filter(req -> req.getProjectName().equals(projectName) && req.getDataset().equals(dataset))
                    .collect(Collectors.toList());

            // Sort requirements by date
            projectRequirements.sort(Comparator.comparing(Requirement::getDate));

            // Initialize CSV for results
            String csvOutputFile = getOutputFileName(dataset, projectName);
            FileWriter csvWriter = new FileWriter(csvOutputFile, false);
            csvWriter.append("RequirementID,ProjectName,Buggy");

            for (String field : FIELDS) {
                for (String method : METHODS) {
                    for(String aggregation : AGGREGATION){
                        csvWriter.append(",").append(buildVariantName(method, field, aggregation));
                    }
                }
            }
            csvWriter.append("\n");

            for (int i = 0; i < projectRequirements.size(); i++) {
                Requirement currentReq = projectRequirements.get(i);

                // Analyze all previous requirements of the same project
                List<Requirement> buggyRequirements = projectRequirements.subList(0, i).stream()
                        .filter(req -> req.isBuggy() && !req.getId().equals(currentReq.getId())) // Exclude current requirement
                        .collect(Collectors.toList());

                csvWriter.append(currentReq.getId()).append(",").append(currentReq.getProjectName()).append(",").append(currentReq.isBuggy() ? "1.0" : "0.0");

                if (buggyRequirements.isEmpty()) {
                    for (int j = 0; j < METHODS.size() * 2 * 2; j++) csvWriter.append(", ");
                    csvWriter.append("\n");
                    continue;
                }

                for (String field : FIELDS) {
                    final String text1 = removeStopWords(getFieldText(currentReq, field));

                    if (text1.isEmpty()) {
                        for (int j = 0; j < METHODS.size() * 2; j++) csvWriter.append(", ");
                        continue;
                    }

                    for (String method : METHODS) {
                        List<Double> similarities = buggyRequirements.stream()
                                .map(prevReq -> calculateSimilarity(text1, removeStopWords(getFieldText(prevReq, field)), method))
                                .collect(Collectors.toList());

                        double maxSimilarity = similarities.stream().mapToDouble(Double::doubleValue).max().orElse(Double.NaN);
                        double avgSimilarity = similarities.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);

                        csvWriter.append(",").append(Double.isNaN(maxSimilarity) ? " " : String.format("%.4f", maxSimilarity));
                        csvWriter.append(",").append(Double.isNaN(avgSimilarity) ? " " : String.format("%.4f", avgSimilarity));
                    }
                }
                csvWriter.append("\n");
            }

            csvWriter.flush();
            csvWriter.close();
            System.out.println("Similarity results for project " + projectName + " in dataset " + dataset + " have been written to " + csvOutputFile);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static List<Requirement> loadRequirementsFromJSON(String filePath) throws IOException, ParseException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(new File(filePath));
        List<Requirement> requirements = new ArrayList<>();

        for (JsonNode node : root) {
            if (node.has("measurementDateName") && "FirstCommitDate".equals(node.get("measurementDateName").asText())) {
                String id = node.get("Requirement ID").asText();
                String projectName = node.get("Project name").asText();
                String dataset = node.get("dataset").asText();
                String text = node.get("Requirement text").asText();
                String title = node.get("Requirement title").asText();
                boolean buggy = node.get("buggy").asBoolean();
                Date date = DATE_FORMAT.parse(node.get("date").asText());
                requirements.add(new Requirement(id, dataset, projectName, title, text, buggy, date));
            }
        }
        return requirements;
    }

    private static String getFieldText(Requirement req, String field) {
        switch (field) {
            case "Title":
                return req.getTitle();
            case "Text":
                return req.getText();
            default:
                return "";
        }
    }

    private static String removeStopWords(String text) {
        return Arrays.stream(text.split(" "))
                .filter(word -> !CUSTOM_STOP_WORDS.contains(word.toLowerCase()))
                .collect(Collectors.joining(" "));
    }

    private static double calculateSimilarity(String text1, String text2, String method) {
        switch (method) {
            case "TF-IDF_Cosine":
                return calculateTFIDFCosineSimilarity(text1, text2);
            case "Jaccard":
                return calculateJaccardSimilarity(text1, text2);
            case "EuclideanDistance":
                return calculateEuclideanDistance(text1, text2);
            default:
                return 0.0;
        }
    }

    private static double calculateTFIDFCosineSimilarity(String text1, String text2) {
        Set<String> uniqueWords = new HashSet<>();
        uniqueWords.addAll(Arrays.asList(text1.split(" ")));
        uniqueWords.addAll(Arrays.asList(text2.split(" ")));

        List<String> uniqueWordList = new ArrayList<>(uniqueWords);
        double[] vector1 = new double[uniqueWordList.size()];
        double[] vector2 = new double[uniqueWordList.size()];

        for (int i = 0; i < uniqueWordList.size(); i++) {
            vector1[i] = tf(text1, uniqueWordList.get(i)) * idf(text1, text2, uniqueWordList.get(i));
            vector2[i] = tf(text2, uniqueWordList.get(i)) * idf(text1, text2, uniqueWordList.get(i));
        }

        return cosineSimilarity(vector1, vector2);
    }

    private static double calculateJaccardSimilarity(String text1, String text2) {
        Set<String> set1 = new HashSet<>(Arrays.asList(text1.split(" ")));
        Set<String> set2 = new HashSet<>(Arrays.asList(text2.split(" ")));

        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    private static double calculateEuclideanDistance(String text1, String text2) {
        int length = Math.min(text1.length(), text2.length());
        double sum = 0.0;

        for (int i = 0; i < length; i++) {
            int diff = text1.charAt(i) - text2.charAt(i);
            sum += diff * diff;
        }

        return 1 / (1 + Math.sqrt(sum));
    }

    private static double cosineSimilarity(double[] vector1, double[] vector2) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vector1.length; i++) {
            dotProduct += vector1[i] * vector2[i];
            normA += vector1[i] * vector1[i];
            normB += vector2[i] * vector2[i];
        }

        return (normA == 0 || normB == 0) ? 0.0 : dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private static int countOccurrences(String text, String word) {
        return (int) Arrays.stream(text.split(" ")).filter(w -> w.equals(word)).count();
    }

    private static double tf(String text, String term) {
        long count = countOccurrences(text, term);
        long totalTerms = text.split(" ").length;
        return (double) count / totalTerms;
    }

    private static double idf(String text1, String text2, String term) {
        double docCount = 0.0;
        if (text1.contains(term)) docCount++;
        if (text2.contains(term)) docCount++;
        return Math.log(2 / (1 + docCount));
    }

}
