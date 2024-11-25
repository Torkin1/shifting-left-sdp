package it.torkin.codesmells;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import org.jline.utils.Log;

import io.grpc.stub.StreamObserver;
import it.torkin.codesmells.git.GitConfig;
import it.torkin.codesmells.git.GitDao;
import it.torkin.dataminer.CodeSmellsMiningGrpc.CodeSmellsMiningImplBase;
import it.torkin.dataminer.Smells.CodeSmellsCountRequest;
import it.torkin.dataminer.Smells.CodeSmellsCountResponse;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.PmdAnalysis;
import net.sourceforge.pmd.reporting.GlobalAnalysisListener;

@Slf4j
public class CodeSmellsService extends CodeSmellsMiningImplBase{

    @Override
    public void countSmells(CodeSmellsCountRequest request, StreamObserver<CodeSmellsCountResponse> responseObserver) {
        responseObserver.onNext(processRequest(request));
        responseObserver.onCompleted();
    }
        
    private CodeSmellsCountResponse processRequest(CodeSmellsCountRequest request) {
        
        Integer smellsCount;
        String dataDirName = System.getProperty("user.dir")+"/data";
        
        GitConfig gitConfig = new GitConfig();
        gitConfig.setDefaultBranchCandidates(List.of("master", "main"));
        gitConfig.setHostname("github.com");
        File reposDir = new File(dataDirName);
        if (!reposDir.exists()) {
            reposDir.mkdirs();
        }
        gitConfig.setReposDir(dataDirName);

        
        try (GitDao gitDao = new GitDao(gitConfig, request.getRepoCoordinates().getName())){

            // checkout corresponding repo at measurement date
            Date measurementDate = Date.from(Instant.ofEpochSecond(
                request.getMeasurementDate().getSeconds(),
                request.getMeasurementDate().getNanos()));
            System.out.println("requested to measure code quality of repo "+request.getRepoCoordinates().getName()+ " at " +measurementDate);
            gitDao.checkout(measurementDate);

            //pmd check -d . -R rulesets/java/quickstart.xml -f csv -r /violations.csv
            File repository = new File(gitConfig.getReposDir() + "/" + request.getRepoCoordinates().getName());
            File violationsFile = new File(dataDirName+"/violations.csv");
            (new ProcessBuilder("/pmd/bin/pmd", "check", "-d", ".", "-R", "rulesets/java/quickstart.xml", "-f", "csv", "-r", violationsFile.getAbsolutePath()))
                    .directory(repository)
                    .inheritIO()
                    .start();

            // run code quality analysis using PMD
            /*
            PMDConfiguration pmdConfig = new PMDConfiguration();
            File repository = new File(gitConfig.getReposDir() + "/" + request.getRepoCoordinates().getName());
            pmdConfig.addInputPath(Path.of(repository.getAbsolutePath()));
            pmdConfig.setRuleSets(List.of(
                "rulesets/internal/all-java.xml",
                "rulesets/internal/all-ecmascript.xml",
                "rulesets/java/quickstart.xml"
            ));
            GlobalAnalysisListener.ViolationCounterListener smellsListener;
            try (PmdAnalysis pmdAnalysis = PmdAnalysis.create(pmdConfig)) {
                smellsListener = new GlobalAnalysisListener.ViolationCounterListener();
                pmdAnalysis.addListener(smellsListener);
                pmdAnalysis.performAnalysis();
            }
            smellsCount = smellsListener.getResult();
             */

            // As usual, I don't know why, but java api seems to not work in container on wsl
            // smell count is always 0
            // System.out.println("about to start reading violations file");
            try (BufferedReader reader = new BufferedReader(new FileReader(violationsFile))) {
                // System.out.println("violations file opened");
                smellsCount = 0;
                reader.skip(1);
                // System.out.println("about to read lines");
                while (reader.readLine() != null) smellsCount ++;
            }


        } catch (Exception e) {
            Log.error("unable to mine smells for repo {} ", request.getRepoCoordinates().getName(), e);
            smellsCount = -1;
        }

        System.out.println("smellscount: "+smellsCount);
        return CodeSmellsCountResponse.newBuilder()
            .setSmellsCount(smellsCount)
            .build();
    }

}
