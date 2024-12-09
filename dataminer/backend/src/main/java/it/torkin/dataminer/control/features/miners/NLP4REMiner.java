package it.torkin.dataminer.control.features.miners;

import java.util.Set;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;

import it.torkin.dataminer.entities.dataset.features.DoubleFeature;
import it.torkin.dataminer.toolbox.csv.Resultset;
import it.torkin.dataminer.toolbox.csv.UnableToGetResultsetException;
import it.torkin.dataminer.control.features.FeatureMiner;
import it.torkin.dataminer.control.features.FeatureMinerBean;
import it.torkin.dataminer.control.features.IssueFeature;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Component
public class NLP4REMiner extends FeatureMiner{

    @RequiredArgsConstructor
    @Getter
    private enum NLP4REFeatures {
        DESCRIPTION_ATTRIBUTE_ACTION("DA_ACT"),
        DESCRIPTION_ATTRIBUTE_CONDITIONALS("DA_CND"),
        DESCRIPTION_ATTRIBUTE_CONTINUANCES("DA_CNT"),
        DESCRIPTION_ATTRIBUTE_IMPERATIVES("DA_IMP"),
        DESCRIPTION_ATTRIBUTE_INCOMPLETES("DA_INC"),
        DESCRIPTION_ATTRIBUTE_OPTIONS("DA_OPT"),
        DESCRIPTION_ATTRIBUTE_RISK_LEVEL("DA_RKL"),
        DESCRIPTION_ATTRIBUTE_SOURCES("DA_SRC"),
        DESCRIPTION_ATTRIBUTE_WEAK_PHRASES("DA_WKP"),
        NUMBER_OF_SUBJECTS("EX_SBJ"),
        NUMBER_OF_WORDS("EX_CNS"),
        NUMBER_OF_VERBS("EX_VRB"),
        NUMBER_OF_AMBIGUITES("EX_AMB"),
        NUMBER_OF_DIRECTIVES("EX_DIR"),
        READABILITY_SCORE("EX_RDS"),
        SENTENCE_COMPLETENESS("EX_ICP"),
        ACTION_DENSITY("EX_ACD"),
        NUMBER_OF_ENTITIES("EX_ENT"),
        SENTIMENT_POLARITY("IT_POL"),
        SENTIMENT_SUBJECTIVITY("IT_SUB"),
        HAS_CODE("EX_COD"),
        ;

        private final String name;
    }
    
    @Override
    public void mine(FeatureMinerBean bean) {
        
        // read result file with features and find the corresponding issue.
        // TODO: result file should be generated on demand by communicating with the
        // NLP4RE service. For now, we use a pre-generated file.
        try (
        Resultset<Map<String, String>> rows = new Resultset<>("./src/main/resources/nlp4re/result.csv", Map.class)) {
            Map<String, String> row = null;
            while (rows.hasNext()) {
                row = rows.next();
                if (bean.getIssue().getKey().equals(row.get("Requirement ID"))
                // FIXME: project name is not unique. We should use project key instead, but current result file uses project names.
                && bean.getIssue().getDetails().getFields().getProject().getName().equals(row.get("Project name"))
                && bean.getMeasurementDate().getName().equals(row.get("measurementDateName"))){
                    for (NLP4REFeatures feature : NLP4REFeatures.values()) {
                        String fName = buildFullFeatureName(feature);
                        String fValueString = row.get(fName);
                        Double fValue = fValueString == null ? Double.NaN : Double.parseDouble(fValueString);
                        bean.getMeasurement().getFeatures().add(new DoubleFeature(fName, fValue));
                    }
                    return;
                }
            }

            // if issue is not found, put all features as NaNs.
            for (NLP4REFeatures feature : NLP4REFeatures.values()) {
                String fName = buildFullFeatureName(feature);
                bean.getMeasurement().getFeatures().add(new DoubleFeature(fName, Double.NaN));
            }

        } catch (UnableToGetResultsetException | IOException e) {
            throw new RuntimeException(e);
        }
        
    }

    private String buildFullFeatureName(NLP4REFeatures feature) {
        // as of now, there are only sentiment or description features.
        Set<NLP4REFeatures> sentimentFeatures = Set.of(NLP4REFeatures.SENTIMENT_POLARITY, NLP4REFeatures.SENTIMENT_SUBJECTIVITY);
        if (sentimentFeatures.contains(feature)){
            return IssueFeature.NLP_SENTIMENT.getFullName() + ": " + feature.getName();
        } else {
            return IssueFeature.NLP_DESCRIPTION.getFullName() + ": " + feature.getName();
        }
    }
    
    @Override
    protected Set<String> getFeatureNames() {
        Set<String> featureNames = new HashSet<>();
        for (NLP4REFeatures feature : NLP4REFeatures.values()) {
            featureNames.add(buildFullFeatureName(feature));
        }
        return featureNames;
    }
    
}
