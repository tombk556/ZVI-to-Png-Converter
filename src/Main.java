public class Main {

    public static void main(String[] args) {

        String zviFilePath = "/Users/tom/Documents/AWI Msc./3. Semester/FuE/featurebasedradiomics/Data/d65/SCD50Glios_U87-MG-Tshp53_2_17.5Gy_d65 26.04.2017 24847/SCD50Glios_U87-MG-Tshp53_2_17.5Gy_d65 26.04.2017 24847_10C_01.zvi";
        String pngOutputPath = "file2.png";

        ZVIConverter converter = new ZVIConverter();

        try {
            converter.convertZVIToPNG(zviFilePath, pngOutputPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
