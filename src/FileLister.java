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

    public static void main(String[] args) {
        // Example usage
        File rootDirectory = new File("/Users/tom/Documents/AWI Msc./3. Semester/FuE/featurebasedradiomics/Data/test"); // Change this to your root folder path
        String extension = ".zvi"; // Change this to the desired file extension

        try {
            List<String> files = listFilesWithExtension(rootDirectory, extension);
            System.out.println("Files with extension '" + extension + "':");
            for (String filePath : files) {
                System.out.println(filePath);
            }
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
        }
    }
}
