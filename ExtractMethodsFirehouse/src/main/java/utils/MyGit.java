package utils;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import io.vavr.control.Try;

import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class MyGit {
    public static List<RevCommit> getCommitsInLastNHours(Git git, RevSort order, int hours) {
        Instant currentTime = Instant.now();
        Instant timeRangeAgo = currentTime.minus(hours, ChronoUnit.HOURS);
        List<RevCommit> commits = new ArrayList<>(Try.of(() -> {
                    RevWalk walk = new RevWalk(git.getRepository());
                    walk.markStart(walk.parseCommit(git.getRepository().resolve(Constants.HEAD)));
                    walk.sort(order);
                    walk.setRevFilter(RevFilter.NO_MERGES);
                    return walk;
                })
                .map(walk -> {
                    Iterator<RevCommit> iter = walk.iterator();
                    List<RevCommit> l = new ArrayList<>();
                    while (iter.hasNext()) {
                        l.add(iter.next());
                    }
                    walk.dispose();
                    return l;
                })
                .onSuccess(l -> System.out.println(l.size() + " number of commits found for " + git.getRepository().getDirectory().getParentFile().getName()))
                .onFailure(Throwable::printStackTrace)
                .getOrElse(new ArrayList<>())).stream().filter(c->{
                    Instant commitTime = Instant.ofEpochSecond(c.getCommitTime());
                    return commitTime.isAfter(timeRangeAgo);

        }).collect(Collectors.toList());
        Collections.reverse(commits);
        return commits;
    }

    public static String cloneDeleteIfExits(String name, String path) {

        if (Files.exists(Path.of(path + "/" + name))) FileIO.deleteDFile(path+name);
        Try.of(() -> {
                    Git.cloneRepository().setURI("https://github.com/"+name+".git").setDirectory(new File(path + "/" + name)).call();
                    return null;
                }).onFailure(Throwable::printStackTrace);
        return path + "/" + name;
    }
}
