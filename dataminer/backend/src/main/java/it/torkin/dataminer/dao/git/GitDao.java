package it.torkin.dataminer.dao.git;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.AndRevFilter;
import org.eclipse.jgit.revwalk.filter.CommitTimeRevFilter;
import org.eclipse.jgit.revwalk.filter.MessageRevFilter;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import it.torkin.dataminer.config.GitConfig;
import it.torkin.dataminer.entities.dataset.Commit;
import it.torkin.dataminer.toolbox.regex.NoMatchFoundException;
import it.torkin.dataminer.toolbox.regex.Regex;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.ProgressBar;

@Slf4j
public class GitDao implements AutoCloseable{

    @RequiredArgsConstructor
    private static class ProgressBarMonitor implements ProgressMonitor{
        private ProgressBar progress;
        private ProgressBar subtaskProgress;

        private final String taskName;

        @Override
        public void start(int totalTasks) {
            progress = new ProgressBar(taskName, totalTasks);
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
            if (progress != null){
                progress.step();
                if(progress.getMax() == progress.getCurrent()){
                    progress.close();
                }
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
    }

    private String remoteUrl;
    private File localDir;
    private String issueKeyRegexp;
    @Getter
    private final String projectName;

    private Repository repository;
    private String defaultBranch;

    public GitDao(GitConfig config, String projectName) throws UnableToInitRepoException{

        this.remoteUrl = forgeRemote(config.getHostname(), projectName);
        this.issueKeyRegexp = config.getLinkedIssueKeyRegexp();
        this.localDir = new File(forgeLocal(config.getReposDir(), projectName));
        this.projectName = projectName;
        initRepo(config);
    }

    private String findDefaultBranch(List<String> candidates) throws UnableToDetectDefaultBranchException {
        try {
            for (String candidate : candidates){
                if (repository.resolve(candidate) != null){
                    return candidate;
                }
            }
            throw new UnableToDetectDefaultBranchException(String.format("no master candidate branch found in repository %s", projectName));
        } catch (IOException | RevisionSyntaxException e) {
            throw new UnableToDetectDefaultBranchException(e);
        }
    }

    private String forgeRemote(String hostname, String projectName){
        return String.format("https://%s/%s", hostname, projectName);
    }

    private String forgeLocal(String localPath, String projectName){
        return String.format("%s/%s", localPath, projectName);
    }

    private void initRepo(GitConfig config) throws UnableToInitRepoException{

        FileRepositoryBuilder repoBuilder = new FileRepositoryBuilder();

        // opens local copy of the repository if found, else
        // clones the remote repository
        try {
            cloneRepo(repoBuilder);
            repository = repoBuilder
                .setWorkTree(localDir)
                .setMustExist(true)
                .build();

            this.defaultBranch = findDefaultBranch(config.getDefaultBranchCandidates());
            checkout(defaultBranch);

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
                    .setProgressMonitor(new ProgressBarMonitor(String.format("cloning repository %s", projectName)))
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

    public String getCommitMessage(String hash) throws UnableToGetCommitException{
        RevCommit commit = getCommit(hash);
        return commit.getFullMessage();
    }

    @Override
    public void close() throws Exception {
        repository.close();
    }

 /**
     * Checkouts local clone to a specific commit
     * @param commit
     * @throws UnableToCheckoutException
     */
    public void checkout(Commit commit) throws UnableToCheckoutException{
        try (Git git = new Git(this.repository)){
            checkout(commit.getHash());   
        }
    }

    /**checkouts local to the default branch
     * @throws UnableToCheckoutException
     * */
    public void checkout() throws UnableToCheckoutException{
        try (Git git = new Git(this.repository)){
            checkout(defaultBranch);
        }
    }

    /**
     * Checkouts local clone to a specific branch name or commit hash
     * !NOTE: this leaves the repository in a detached HEAD state. This should not be a problem
     * unless you are going to make changes to the code.
     * https://stackoverflow.com/questions/10228760/how-do-i-fix-a-git-detached-head#answer-58142219
     * @param name can be the branch name or the sha-1 hash of the commit
     */
    public void checkout(String name) throws UnableToCheckoutException{
        try (Git git = new Git(this.repository)){

            // don't kow why, but a git reset is needed before calling a checkout in a containerized env running on windows.
            // Only the cli git seems to work. Make sure to have it installed.
            new ProcessBuilder("git", "reset", "--hard").directory(localDir).start().waitFor();
            // Below you can find all prevoius unsuccessful attempts to fix the same problem as above
            // 
            // 1) https://stackoverflow.com/questions/33961511/jgit-checkout-over-the-same-branch
            // git.clean().setCleanDirectories(true).setForce(true).call();
            // git.reset().setMode(ResetType.HARD).setRef("refs/heads/"+defaultBranch).call();
            // 
            // 2) https://stackoverflow.com/questions/28391052/using-the-jgit-checkout-command-i-get-extra-conflicts
            //git.checkout().setAllPaths(true).setForced(true).call();

            git.checkout()
                .setName(name)
                .setProgressMonitor(new ProgressBarMonitor(String.format("checking out %s at %s", projectName, name)))
                .call();

        } catch (GitAPIException | InterruptedException | IOException e) {

            throw new UnableToCheckoutException(e);
        }
    }

    /**
     * Checkouts local clone to the most recent commit not after {@code date}
     */
    public void checkout(Date date) throws UnableToCheckoutException{
        try {
            RevCommit commit = getLatestCommit(date, null);
            if (commit != null){
                checkout(commit.getName());
            } else {
                throw new UnableToCheckoutException(String.format("no commit found before %s in repository %s", date, projectName));
            }
        } catch (UnableToGetCommitsException e) {
            throw new UnableToCheckoutException(e);
        }
    }

    /**
     * gets most recent commit applied not after {@code beforeDate}  containing optional {@code commentContent} string in its comment
     * Returns null if no such commit is found 
     *
     * @throws UnableToGetCommitsException
     */
    private RevCommit getLatestCommit(Date beforeDate, String commentContent) throws UnableToGetCommitsException {

        try (Git git = new Git(repository)) {

            LogCommand logCommand = git.log();
            RevFilter filter;
            ObjectId head = repository.resolve(defaultBranch);

            // set filter on commit time and comment content
            filter = CommitTimeRevFilter.before(beforeDate);
            if (commentContent != null){
                filter = AndRevFilter.create(filter, MessageRevFilter.create(commentContent));
            }
            logCommand.setRevFilter(filter);
            logCommand.setMaxCount(1);
            logCommand.add(head);

            // extracts latest commit which matches filter
            Iterator<RevCommit> commits = logCommand.call().iterator();
            if (commits.hasNext()){
                return commits.next();
            } else {
                return null;
            }

        } catch (GitAPIException | RevisionSyntaxException | IOException e) {

            throw new UnableToGetCommitsException(e);
        }
    }

    /**
     * gets most recent commit hash applied strictly before {@code beforeDate}  containing optional {@code commentContent} string in its comment
     * Returns null if no such commit is found 
     *
     * @throws UnableToGetCommitsException
     */
    public String getLatestCommitHash(Date beforeDate, String commentContent) throws UnableToGetCommitsException{
        RevCommit commit = getLatestCommit(beforeDate, commentContent);
        return commit == null ? null : commit.getName();
    }

}
