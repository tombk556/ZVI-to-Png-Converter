import loci.formats.FormatException;
import loci.formats.FormatTools;
import loci.formats.ImageReader;
import loci.common.services.ServiceFactory;
import loci.formats.services.OMEXMLService;
import loci.formats.meta.IMetadata;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class ZVIToPNGConverter {

    public static void main(String[] args) {
        String zviFilePath = "/Users/tom/Documents/AWI Msc./3. Semester/FuE/featurebasedradiomics/Data/d65/SCD50Glios_U87-MG-Tshp53_2_17.5Gy_d65 26.04.2017 24847/SCD50Glios_U87-MG-Tshp53_2_17.5Gy_d65 26.04.2017 24847_10C_01.zvi";
        String pngOutputPath = "file2.png";

        ImageReader reader = new ImageReader();

        try {
            ServiceFactory factory = new ServiceFactory();
            OMEXMLService service = factory.getInstance(OMEXMLService.class);
            IMetadata meta = service.createOMEXMLMetadata();
            reader.setMetadataStore(meta);

            reader.setId(zviFilePath);

            int width = reader.getSizeX();
            int height = reader.getSizeY();
            int pixelType = reader.getPixelType();
            boolean isLittleEndian = reader.isLittleEndian();
            int channelCount = reader.getSizeC();

            System.out.println("Pixel Type: " + FormatTools.getPixelTypeString(pixelType));
            System.out.println("Endianness: " + (isLittleEndian ? "Little Endian" : "Big Endian"));
            System.out.println("Image Dimensions: " + width + " x " + height);
            System.out.println("Number of Channels: " + channelCount);

            BufferedImage img = null;

            if (channelCount >= 3) {
                byte[][] channelData = new byte[3][];
                for (int c = 0; c < 3; c++) {
                    int planeIndex = reader.getIndex(0, c, 0);
                    byte[] imgBytes = reader.openBytes(planeIndex);

                    if (pixelType == FormatTools.UINT16 || pixelType == FormatTools.INT16) {
                        short[] shortData = new short[width * height];
                        byte[] byteData = new byte[width * height];
                        int maxPixelValue = 0;

                        for (int i = 0; i < shortData.length; i++) {
                            int b1 = imgBytes[2 * i] & 0xFF;
                            int b2 = imgBytes[2 * i + 1] & 0xFF;
                            int pixelValue;
                            if (isLittleEndian) {
                                pixelValue = (b2 << 8) | b1;
                            } else {
                                pixelValue = (b1 << 8) | b2;
                            }
                            shortData[i] = (short) pixelValue;
                            if (pixelValue > maxPixelValue) {
                                maxPixelValue = pixelValue;
                            }
                        }

                        for (int i = 0; i < byteData.length; i++) {
                            int pixelValue = shortData[i] & 0xFFFF;
                            byteData[i] = (byte) ((pixelValue * 255) / maxPixelValue);
                        }

                        channelData[c] = byteData;
                    } else {
                        channelData[c] = imgBytes;
                    }
                }

                img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                int[] rgbData = new int[width * height];
                for (int i = 0; i < rgbData.length; i++) {
                    int r = channelData[0][i] & 0xFF;
                    int g = channelData[1][i] & 0xFF;
                    int b = channelData[2][i] & 0xFF;
                    rgbData[i] = (r << 16) | (g << 8) | b;
                }
                img.setRGB(0, 0, width, height, rgbData, 0, width);

            } else {
                byte[] imgBytes = reader.openBytes(0);

                if (pixelType == FormatTools.UINT16 || pixelType == FormatTools.INT16) {
                    short[] shortData = new short[width * height];
                    byte[] byteData = new byte[width * height];
                    int maxPixelValue = 0;

                    for (int i = 0; i < shortData.length; i++) {
                        int b1 = imgBytes[2 * i] & 0xFF;
                        int b2 = imgBytes[2 * i + 1] & 0xFF;
                        int pixelValue;
                        if (isLittleEndian) {
                            pixelValue = (b2 << 8) | b1;
                        } else {
                            pixelValue = (b1 << 8) | b2;
                        }
                        shortData[i] = (short) pixelValue;
                        if (pixelValue > maxPixelValue) {
                            maxPixelValue = pixelValue;
                        }
                    }

                    System.out.println("Max pixel value: " + maxPixelValue);

                    for (int i = 0; i < byteData.length; i++) {
                        int pixelValue = shortData[i] & 0xFFFF;
                        byteData[i] = (byte) ((pixelValue * 255) / maxPixelValue);
                    }

                    img = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
                    WritableRaster raster = img.getRaster();
                    raster.setDataElements(0, 0, width, height, byteData);

                } else {
                    img = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
                    WritableRaster raster = img.getRaster();
                    raster.setDataElements(0, 0, width, height, imgBytes);
                }
            }

            ImageIO.write(img, "png", new File(pngOutputPath));
            reader.close();

            System.out.println("Conversion completed successfully.");

        } catch (FormatException | IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
