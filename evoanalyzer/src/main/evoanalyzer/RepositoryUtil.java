package evoanalyzer;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

/**
 * 
 * @author linyun
 *
 */
public class RepositoryUtil {
	public static Repository openRepository(String filePath) throws IOException {
		
		File repoDir = new File(filePath, ".git");
		
		if(repoDir.exists()){
			System.currentTimeMillis();
		}
		
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        Repository repository = builder.setGitDir(repoDir)
                .readEnvironment() // scan environment GIT_* variables
                .findGitDir() // scan up the file system tree
                .build();
        
        return repository;
    }
}
