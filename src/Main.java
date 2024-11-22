import java.io.File;
import java.util.List;

public class Main {

    public static void main(String[] args) {

        String path = "/Users/tom/Documents/AWI Msc./3. Semester/FuE/featurebasedradiomics/Data/d65/SCD50Glios_U87-MG-Tshp53_2_20Gy_d65 26.04.2017 24850";
        File folder = new File(path);
        String extension = ".zvi";

        List<String> fileNames = FileLister.listFilesForFolder(folder, extension);

        for (String fileName : fileNames) {
            String pngFilename = fileName.substring(fileName.lastIndexOf("/") + 1);
            String fileNameWithoutExtension = pngFilename.substring(0, pngFilename.lastIndexOf("."));

            String fullPathNameZVI = path + "/" + fileName;
            String fullPathNamePNG = path + "/" + fileNameWithoutExtension + ".png";

            ZVIConverter converter = new ZVIConverter();

            try {
                converter.convertZVIToPNG(fullPathNameZVI, fullPathNamePNG);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
