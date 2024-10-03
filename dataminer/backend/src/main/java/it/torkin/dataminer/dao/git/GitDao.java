package it.torkin.dataminer.dao.git;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import it.torkin.dataminer.config.GitConfig;
import it.torkin.dataminer.entities.dataset.Commit;
import it.torkin.dataminer.toolbox.regex.NoMatchFoundException;
import it.torkin.dataminer.toolbox.regex.Regex;
import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.ProgressBar;

@Slf4j
public class GitDao implements AutoCloseable{

    private String remoteUrl;
    private File localDir;
    private String issueKeyRegexp;
    private String projectName;

    private Repository repository;
        
    public GitDao(GitConfig config, String projectName) throws UnableToInitRepoException{
                
        this.remoteUrl = forgeRemote(config.getHostname(), projectName);
        this.localDir = new File(forgeLocal(config.getReposDir(), projectName));
        this.issueKeyRegexp = config.getLinkedIssueKeyRegexp();
        this.projectName = projectName;
        initRepo();
    }

    private String forgeRemote(String hostname, String projectName){
        return String.format("https://%s/%s", hostname, projectName);
    }

    private String forgeLocal(String localPath, String projectName){
        return String.format("%s/%s", localPath, projectName);
    }
    
    private void initRepo() throws UnableToInitRepoException{
        
        FileRepositoryBuilder repoBuilder = new FileRepositoryBuilder();
        
        // opens local copy of the repository if found, else
        // clones the remote repository
        try {
            cloneRepo(repoBuilder);
            repository = repoBuilder
             .setWorkTree(localDir)
             .setMustExist(true)
             .build();

        } catch (Exception e) {
            
            throw new UnableToInitRepoException(e);
        }

    }
    
    private boolean gitDirExists(File directory, FileRepositoryBuilder repoBuilder) {

        boolean gitDirExists;
        
        repoBuilder.findGitDir(directory);
        gitDirExists = repoBuilder.getGitDir() != null;

        return gitDirExists;
    }

    private boolean isDirEmpty(File directory){
        try(DirectoryStream<Path> contents = Files.newDirectoryStream(directory.toPath())){
            return !contents.iterator().hasNext();
        } catch (IOException e) {
            log.error("error checking if localdir is empty", e);
            return false;
        }
    }
    
    private void cloneRepo(FileRepositoryBuilder repoBuilder) throws UnableToCloneRepoException {

        boolean localDirEmpty;
        boolean gitDirExists;        
        try{
            
            localDir.mkdirs();
            localDirEmpty = isDirEmpty(localDir);
            gitDirExists = gitDirExists(localDir, repoBuilder);
            
            if(!localDirEmpty && gitDirExists) return;
            if(!localDirEmpty && !gitDirExists)
                throw new CloneInNonEmptyDirException(localDir); 

            Git git = Git.cloneRepository()
             .setURI(remoteUrl)
             .setDirectory(localDir)
             .setProgressMonitor(new ProgressMonitor() {

                private ProgressBar progress;
                private ProgressBar subtaskProgress;
                
                @Override
                public void start(int totalTasks) {
                    progress = new ProgressBar(String.format("cloning repository %s", projectName), totalTasks);
                }

                @Override
                public void beginTask(String title, int totalWork) {
                    if(subtaskProgress != null) subtaskProgress.close();
                    subtaskProgress = new ProgressBar(title, totalWork);
                }

                @Override
                public void update(int completed) {
                    if (subtaskProgress != null)
                    {
                        subtaskProgress.stepBy(completed);
                        if (subtaskProgress.getMax() == subtaskProgress.getCurrent()){
                            subtaskProgress.close();
                        }
                    }
                }

                @Override
                public void endTask() {
                    subtaskProgress.close();
                    progress.step();
                    if(progress.getMax() == progress.getCurrent()){
                        progress.close();
                    }
                }

                @Override
                public boolean isCancelled() {
                    return false;
                }

                @Override
                public void showDuration(boolean enabled) {
                    // not implemented
                }
                
             })
             .call();
            git.close();
        }
        
        catch(Exception e){
            throw new UnableToCloneRepoException(e, remoteUrl, localDir);
        }
        
    }

    /** Gets linked issue key from commit message
     * @throws IssueNotFoundException 
     * @throws UnableToGetLinkedIssueKeyException */
    public List<String> getLinkedIssueKeysByCommit(String hash) throws UnableToGetLinkedIssueKeyException {
        
        List<String> keys;;
        String message;
        RevCommit commit;
        
        try {
            commit = getCommit(hash);
            message = commit.getFullMessage();
            keys = extractIssueKeys(message);
            return keys;

        } catch (NoMatchFoundException | UnableToGetCommitException e) {
            throw new UnableToGetLinkedIssueKeyException(hash, projectName, e);
        }

        
    }

    public void getCommitDetails(Commit commit) throws UnableToGetCommitDetailsException {

        long msCommitTime;
        
        try {
            RevCommit commitDetails = getCommit(commit.getHash());
            // Timestamp wants milliseconds, while git commit time is in seconds since the epoch.
            // We must beware of overflow 
            msCommitTime = commitDetails.getCommitTime();
            msCommitTime *= 1000;
            commit.setTimestamp(new Timestamp(msCommitTime));
        } catch (UnableToGetCommitException e) {
            throw new UnableToGetCommitDetailsException(commit.getHash(), e);
        }
    }


    private List<String> extractIssueKeys(String comment) throws NoMatchFoundException {
        
        List<String> keys = new ArrayList<>();
        
        Regex matches = new Regex(issueKeyRegexp, comment);
        matches.forEach((key) -> {
            key = key.toUpperCase(Locale.ROOT);
            keys.add(key);
        });
        
        if (keys.isEmpty()) throw new NoMatchFoundException(issueKeyRegexp, comment);
        return keys;
        
    }

    private RevCommit getCommit(String hash) throws UnableToGetCommitException {

        RevCommit commit = null;
        try (RevWalk walk = new RevWalk(repository)) {
            
            commit = walk.parseCommit(repository.resolve(hash));
            return commit;

        } catch (RevisionSyntaxException | IOException e) {

            throw new UnableToGetCommitException(hash, e);
        }
    }

    @Override
    public void close() throws Exception {
        repository.close();
    }
    
}
