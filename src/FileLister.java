import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileLister {

    public static List<String> listFilesForFolder(final File folder, final String extension) {
        List<String> fileNames = new ArrayList<>();
        listFilesForFolder(folder, extension, fileNames);
        return fileNames;
    }

    private static void listFilesForFolder(final File folder, final String extension, List<String> fileNames) {
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                listFilesForFolder(fileEntry, extension, fileNames);
            } else {
                if (fileEntry.getName().toLowerCase().endsWith(extension.toLowerCase())) {
                    fileNames.add(fileEntry.getName());
                }
            }
        }
    }

    public static void main(String[] args) {
        String path = "/Users/tom/Documents/AWI Msc./3. Semester/FuE/featurebasedradiomics/Data/d65/SCD50Glios_U87-MG-Tshp53_2_17.5Gy_d65 26.04.2017 24847";
        File folder = new File(path);
        String extension = ".zvi";

        List<String> fileNames = listFilesForFolder(folder, extension);

        for (String fileName : fileNames) {
            String fullPathName = path + fileName;
            System.out.println(fullPathName);
            break;
        }
    }
}
