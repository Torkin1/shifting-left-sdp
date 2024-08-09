package it.torkin.dataminer.dao.git;

import java.io.File;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.TextProgressMonitor;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import it.torkin.dataminer.config.GitConfig;

public class GitDao implements AutoCloseable{

    private String remoteUrl;
    private File localDir;

    private Repository repository;
        
    public GitDao(GitConfig config, String projectName) throws UnableToInitRepoException{
                
        this.remoteUrl = forgeRemote(config.getHostname(), projectName);
        this.localDir = new File(forgeLocal(config.getReposDir(), projectName));

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

    /** TODO: stub 
     * @throws IssueNotFoundException */
    public String getLinkedIssueKeyByCommit(String hash) throws IssueNotFoundException {
        
        String key = "PROJ-123";

        if (key == null) throw new IssueNotFoundException(hash);

        return key;

    }

    @Override
    public void close() throws Exception {
        repository.close();
    }
    
}
