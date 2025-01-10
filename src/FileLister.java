import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileLister {

    public static List<String> listFilesWithExtension(final File rootFolder, final String extension) {
        List<String> filePaths = new ArrayList<>();
        if (rootFolder.exists() && rootFolder.isDirectory()) {
            listFilesRecursively(rootFolder, extension, filePaths);
        } else {
            throw new IllegalArgumentException("The provided root folder is invalid or does not exist: " + rootFolder);
        }
        return filePaths;
    }

    private static void listFilesRecursively(final File folder, final String extension, List<String> filePaths) {
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                listFilesRecursively(fileEntry, extension, filePaths);
            } else {
                if (fileEntry.getName().toLowerCase().endsWith(extension.toLowerCase())) {
                    filePaths.add(fileEntry.getAbsolutePath());
                }
            }
        }
    }

}
