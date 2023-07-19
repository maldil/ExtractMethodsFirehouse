package org.com;

import config.Configurations;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.diff.MoveClassRefactoring;
import gr.uom.java.xmi.diff.MoveOperationRefactoring;
import gr.uom.java.xmi.diff.RenameClassRefactoring;
import gr.uom.java.xmi.diff.RenameOperationRefactoring;
import io.vavr.control.Try;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter;
import org.eclipse.jgit.util.io.NullOutputStream;
import org.refactoringminer.api.GitHistoryRefactoringMiner;
import org.refactoringminer.api.GitService;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.util.GitServiceImpl;
import utils.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.stream.Collectors;

import static org.eclipse.jgit.revwalk.RevSort.REVERSE;

public class Main {
    public static void main(String[] args) {
        int i = 0;
        boolean outputFolderCreated = true;
        File file = new File(Configurations.LONG_METHODS);
        if (!file.exists()) {
            outputFolderCreated = file.mkdirs();
        }

        if (!outputFolderCreated) return;

        Scanner sc = new Scanner(Objects.requireNonNull(FileIO.readStringFromFile("selected-repos.csv")));
        while (sc.hasNextLine()) {
            String proName = sc.nextLine();
            String path = null;
            try {
                path = MyGit.cloneOrUpdate(Configurations.PROJECT_REPOSITORY, proName);
            } catch (IOException | GitAPIException e) {
                e.printStackTrace();
                break;
            }

            GitConnector git = new GitConnector(path + "/.git");
            git.connect();
            List<RevCommit> commitsInLastNHours = MyGit.getCommitsInLastNHours(git.getGit(), REVERSE, Configurations.DURATION);
            analyze(new File(path + "/"), proName, commitsInLastNHours);
            i++;
        }
        sc.close();

    }

    private static ArrayList<MyPair<String, MethodDeclaration>> getRowNewFunctions(String oldFile, String oldPath, String newFile, String newPath) {
        MyASTVisitor oldv = new MyASTVisitor();
        CompilationUnit oldcu = Try.of(() -> JavaASTUtil.parseSource(oldFile)).onFailure(System.err::println).get();
        oldcu.accept(oldv);

        MyASTVisitor newv = new MyASTVisitor();
        CompilationUnit newcu = Try.of(() -> JavaASTUtil.parseSource(newFile)).onFailure(System.err::println).get();
        newcu.accept(newv);

        ArrayList<MyPair<String, MethodDeclaration>> newMethods = new ArrayList<>();
        for (MethodDeclaration newvMethod : newv.getMethods()) {
            if (isNewMethods(newvMethod, oldv.getMethods())) {
                if (((CompilationUnit) newvMethod.getRoot()).getLineNumber(newvMethod.getStartPosition() +
                        newvMethod.getLength()) - ((CompilationUnit) newvMethod.getRoot()).getLineNumber(newvMethod.getStartPosition()) > Configurations.METHOD_LENGTH) {
                    newMethods.add(new MyPair(oldPath, newvMethod));
                }
            }
        }
        return newMethods;
    }

    private static boolean isNewMethods(MethodDeclaration newvMethod, ArrayList<MethodDeclaration> methods) {
        if (newvMethod.getParent() instanceof AnonymousClassDeclaration) return false;
        for (MethodDeclaration oldMethod : methods) {
            if (oldMethod.getName().getIdentifier().equals(newvMethod.getName().getIdentifier()) && oldMethod.parameters().size() ==
                    newvMethod.parameters().size() && ((oldMethod.getParent() instanceof TypeDeclaration && newvMethod.getParent() instanceof TypeDeclaration
                    && ((TypeDeclaration) oldMethod.getParent()).getName().getIdentifier().equals(((TypeDeclaration) newvMethod.getParent()).getName().getIdentifier()))
                    || (oldMethod.getParent() instanceof AnonymousClassDeclaration && newvMethod.getParent() instanceof AnonymousClassDeclaration))) {
                return false;
            }
        }
        return true;
    }


    private static List<MyPair<String, MethodDeclaration>> getRowNewFunctions(String newFile, String filePath) {
        MyASTVisitor mv = new MyASTVisitor();
        CompilationUnit unit = Try.of(() -> JavaASTUtil.parseSource(newFile)).onFailure(System.err::println).get();
        unit.accept(mv);
        return mv.getMethods().stream().filter(y -> ((CompilationUnit) y.getRoot()).getLineNumber(y.getStartPosition() +
                y.getLength()) - ((CompilationUnit) y.getRoot()).getLineNumber(y.getStartPosition()) >
                Configurations.METHOD_LENGTH).map(x -> new MyPair<>(filePath, x)).collect(Collectors.toList());
    }


    private static void analyze(File path, String proName, List<RevCommit> commitsInLastNHours) {
        System.out.println(String.format("Analyzing %s/%s", path, proName));
        GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
        Repository repo = Try.of(() -> openRepository(path.getPath())).get();
        ArrayList<Refactoring> refs = new ArrayList<>();

        getRefactorings(commitsInLastNHours, miner, repo, refs);

        ArrayList<MyPair<String, MethodDeclaration>> rowNewFunctions = getNewMethodDeclarations(commitsInLastNHours, repo);

        if (refs.size() > 0) {
            ArrayList<MyPair<String, MethodDeclaration>> finalMethods = refineNewMethods(rowNewFunctions, refs);
            writeToFile(finalMethods, proName);
        } else {
            writeToFile(rowNewFunctions, proName);
        }


        System.out.println("Done");
    }

    private static void writeToFile(ArrayList<MyPair<String, MethodDeclaration>> rowNewFunctions, String projectName) {
        File outputFile = new File(String.format("%s/%s.csv", Configurations.LONG_METHODS, projectName.replace("/", "_")));
        if (outputFile.exists()) {
            outputFile.delete();
        }

        for (MyPair<String, MethodDeclaration> function : rowNewFunctions) {
            String githubLocation = "https://github.com/" + projectName + "/commit/" + function.first.split(",")[0] + "#diff-" +
                    FileIO.getMD5Encoding(function.first.split(",")[1]) + "R" + ((CompilationUnit) function.second.getRoot()).getLineNumber(function.second.getStartPosition());

            FileIO.appendHashSetToFile(outputFile, function.first.split(",")[1] + "::" + function.second.getName().getIdentifier() + "," + githubLocation);
        }
    }

    private static ArrayList<MyPair<String, MethodDeclaration>> refineNewMethods(ArrayList<MyPair<String, MethodDeclaration>> rowNewFunctions, ArrayList<Refactoring> refs) {
        ArrayList<String> toberemoved = new ArrayList<>();
        for (Refactoring ref : refs) {
            if (ref instanceof RenameOperationRefactoring) {
                toberemoved.add(((RenameOperationRefactoring) ref).getRenamedOperation().getName());
            } else if (ref instanceof MoveOperationRefactoring) {
                toberemoved.add(((MoveOperationRefactoring) ref).getMovedOperation().getName());
            } else if (ref instanceof RenameClassRefactoring) {
                toberemoved.addAll(((RenameClassRefactoring) ref).getRenamedClass().getOperations().stream().map(UMLOperation::getName).toList());
            } else if (ref instanceof MoveClassRefactoring) {
                toberemoved.addAll(((MoveClassRefactoring) ref).getMovedClass().getOperations().stream().map(UMLOperation::getName).toList());
            }

        }
        List<MyPair<String, MethodDeclaration>> myPairStream = rowNewFunctions.stream().filter(x -> toberemoved.contains(x.second.getName().getIdentifier())).toList();
        rowNewFunctions.removeAll(myPairStream);

        return rowNewFunctions;
    }

    private static ArrayList<MyPair<String, MethodDeclaration>> getNewMethodDeclarations(List<RevCommit> commitsInLastNHours, Repository repo) {
        ArrayList<MyPair<String, MethodDeclaration>> rowNewFunctions = new ArrayList<>();
        for (RevCommit commits : commitsInLastNHours) {
            if (commits.getParentCount() == 1) {
                RevWalk rw = new RevWalk(repo);
                RevCommit parent = null;
                try {
                    parent = rw.parseCommit(commits.getParent(0).getId());
                } catch (IOException e) {
                    continue;
                }
                if (parent == null) {
                    rw.close();
                    continue;
                }
                DiffFormatter df = new DiffFormatter(NullOutputStream.INSTANCE);
                df.setRepository(repo);
                df.setDiffComparator(RawTextComparator.DEFAULT);
                df.setDetectRenames(true);
                df.setPathFilter(PathSuffixFilter.create(".java"));
                List<DiffEntry> diffs = null;
                try {
                    diffs = df.scan(parent.getTree(), commits.getTree());
                } catch (IOException e) {
                    continue;
                }
                if (diffs == null || diffs.size() > 50) {
                    rw.close();
                    df.close();
                    continue;
                }

                for (DiffEntry diff : diffs) {
                    ObjectLoader ldr = null;

                    if (diff.getChangeType() == DiffEntry.ChangeType.ADD) {
                        String oldContent = null, newContent = null;
                        try {
                            ldr = repo.open(diff.getNewId().toObjectId(), Constants.OBJ_BLOB);
                            newContent = new String(ldr.getCachedBytes());
                        } catch (IOException e) {
                            continue;
                        }
                        if (newContent.isEmpty()) {
                            continue;
                        } else {
                            rowNewFunctions.addAll(getRowNewFunctions(newContent, commits.getName() + "," + diff.getNewPath()));

                        }

                    } else if (diff.getChangeType() == DiffEntry.ChangeType.MODIFY) {
                        String oldContent = null, newContent = null;
                        try {
                            ldr = repo.open(diff.getOldId().toObjectId(), Constants.OBJ_BLOB);
                            oldContent = new String(ldr.getCachedBytes());
                        } catch (IOException e) {
                            continue;
                        }
                        try {
                            ldr = repo.open(diff.getNewId().toObjectId(), Constants.OBJ_BLOB);
                            newContent = new String(ldr.getCachedBytes());
                        } catch (IOException e) {
                            continue;
                        }
                        if (oldContent.isEmpty() || newContent.isEmpty()) {
                            continue;
                        } else {
                            rowNewFunctions.addAll(getRowNewFunctions(oldContent, commits.getName() + "," + diff.getOldPath(), newContent, commits.getName() + "," + diff.getNewPath()));
                        }
                    }


                }

            }
        }
        return rowNewFunctions;
    }

    private static void getRefactorings(List<RevCommit> commitsInLastNHours, GitHistoryRefactoringMiner miner, Repository repo, ArrayList<Refactoring> refs) {
        try {
            for (RevCommit commit : commitsInLastNHours) {
                miner.detectAtCommit(repo, commit.getName(), new RefactoringHandler() {
                    @Override
                    public void handle(String commitId, List<Refactoring> refactorings) {
                        refs.addAll(refactorings);
                    }
                });
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Repository openRepository(String repositoryPath) throws IOException {
        FileRepositoryBuilder repositoryBuilder = new FileRepositoryBuilder();
        repositoryBuilder.setGitDir(new File(repositoryPath, ".git"));

        Repository repository = repositoryBuilder.build();

        return repository;
    }

    static class MyASTVisitor extends ASTVisitor {
        private final ArrayList<MethodDeclaration> methods = new ArrayList<>();

        @Override
        public boolean visit(MethodDeclaration node) {
            methods.add(node);
            return super.visit(node);
        }


        public ArrayList<MethodDeclaration> getMethods() {
            return methods;
        }
    }
}