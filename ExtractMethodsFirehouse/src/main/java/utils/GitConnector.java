package utils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;

import java.io.File;
import java.io.IOException;


public class GitConnector extends AbstractConnector{
    private final String url;
    private Git git;
    private Repository repository;

    public Git getGit() {
        return git;
    }

    public Repository getRepository() {
        return repository;
    }

    public GitConnector(String url) {
        this.url = url;
    }

    public boolean connect() {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        try {
            repository = builder.setGitDir(new File(url)).readEnvironment() // scan
                    // environment
                    // GIT_*
                    // variables
                    .findGitDir() // scan up the file system tree
                    .build();

        } catch (IOException e) {
            System.err.println(e.getMessage());
            return false;
        }
        try {
            if (repository.getBranch() == null)
                return false;
        } catch (IOException e) {
            return false;
        }

        git = new Git(repository);
        return true;
    }

    public void close() {
        this.git.close();
        this.repository.close();
    }
}
