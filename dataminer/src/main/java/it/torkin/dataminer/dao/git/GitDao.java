package it.torkin.dataminer.dao.git;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.TextProgressMonitor;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import it.torkin.dataminer.config.GitConfig;
import it.torkin.dataminer.toolbox.NoMatchFoundException;
import it.torkin.dataminer.toolbox.Regex;

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
            if(!localDir.exists()) cloneRepo(repoBuilder);
            repository = repoBuilder
             .setWorkTree(localDir)
             .setMustExist(true)
             .build();

        } catch (Exception e) {
            
            throw new UnableToInitRepoException(e);
        }

    }
    
    private void cloneRepo(FileRepositoryBuilder repoBuilder) throws UnableToCloneRepoException {

        localDir.mkdirs();

        try (Git git = Git.cloneRepository()
         .setURI(remoteUrl)
         .setDirectory(localDir)
         .setProgressMonitor(new TextProgressMonitor())
         .call()) { }
        catch(Exception e){
            throw new UnableToCloneRepoException(e, remoteUrl, localDir);
        }
        
    }

    /** Gets linked issue key from commit message
     * @throws IssueNotFoundException 
     * @throws UnableToGetLinkedIssueKeyException */
    public String getLinkedIssueKeyByCommit(String hash) throws UnableToGetLinkedIssueKeyException {
        
        String key;
        String message;
        RevCommit commit;
        
        try {
            commit = getCommit(hash);
            message = commit.getFullMessage();
            key = extractIssueKey(message);
            return key;

        } catch (UnableToGetCommitException | NoMatchFoundException e) {
            throw new UnableToGetLinkedIssueKeyException(hash, projectName, e);
        }

        
    }

    private String extractIssueKey(String comment) throws NoMatchFoundException {
        
        return Regex.findFirst(issueKeyRegexp, comment);
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
