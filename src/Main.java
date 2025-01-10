import java.io.File;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        String path = "/Users/tom/Documents/AWI Msc./3. Semester/FuE/featurebasedradiomics/Data/d65/";
        File folder = new File(path);
        String extension = ".zvi";

        List<String> filePaths = FileLister.listFilesWithExtension(folder, extension);

        for (String filePath : filePaths) {
            File originalFile = new File(filePath);
            String parentDirectory = originalFile.getParent(); // Get the directory of the original file
            String fileNameWithoutExtension = originalFile.getName().substring(0, originalFile.getName().lastIndexOf("."));

            String fullPathNamePNG = parentDirectory + File.separator + fileNameWithoutExtension + ".png";

            ZVIConverter converter = new ZVIConverter();

            try {
                converter.convertZVIToPNG(filePath, fullPathNamePNG);
                System.out.println("Converted: " + filePath + " -> " + fullPathNamePNG);
            } catch (Exception e) {
                System.err.println("Failed to convert: " + filePath);
                e.printStackTrace();
            }
        }
    }
}
