import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
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

public class ZVIConverter {

    /**
     * Converts a .zvi file to a PNG image.
     *
     * @param zviFilePath    Path to the input .zvi file.
     * @param pngOutputPath  Path where the output PNG file will be saved.
     * @throws FormatException If there is an error in the file format.
     * @throws IOException     If there is an IO error.
     */
    public void convertZVIToPNG(String zviFilePath, String pngOutputPath) throws FormatException, IOException {
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
                img = processRGBImage(reader, width, height, pixelType, isLittleEndian);
            } else {
                img = processGrayscaleImage(reader, width, height, pixelType, isLittleEndian);
            }

            ImageIO.write(img, "png", new File(pngOutputPath));

            System.out.println("Conversion completed successfully.");

        } catch (ServiceException e) {
            throw new RuntimeException(e);
        } catch (DependencyException e) {
            throw new RuntimeException(e);
        } finally {
            reader.close();
        }
    }

    private BufferedImage processRGBImage(ImageReader reader, int width, int height, int pixelType, boolean isLittleEndian) throws FormatException, IOException {
        byte[][] channelData = new byte[3][];
        for (int c = 0; c < 3; c++) {
            int planeIndex = reader.getIndex(0, c, 0);
            byte[] imgBytes = reader.openBytes(planeIndex);

            if (FormatTools.getBytesPerPixel(pixelType) > 1) {
                channelData[c] = convertAndScaleData(imgBytes, width, height, pixelType, isLittleEndian);
            } else {
                channelData[c] = imgBytes;
            }
        }

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int[] rgbData = new int[width * height];
        for (int i = 0; i < rgbData.length; i++) {
            int r = channelData[0][i] & 0xFF;
            int g = channelData[1][i] & 0xFF;
            int b = channelData[2][i] & 0xFF;
            rgbData[i] = (r << 16) | (g << 8) | b;
        }
        img.setRGB(0, 0, width, height, rgbData, 0, width);

        return img;
    }

    private BufferedImage processGrayscaleImage(ImageReader reader, int width, int height, int pixelType, boolean isLittleEndian) throws FormatException, IOException {
        byte[] imgBytes = reader.openBytes(0);
        BufferedImage img;

        if (FormatTools.getBytesPerPixel(pixelType) > 1) {
            byte[] byteData = convertAndScaleData(imgBytes, width, height, pixelType, isLittleEndian);
            img = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
            WritableRaster raster = img.getRaster();
            raster.setDataElements(0, 0, width, height, byteData);
        } else {
            img = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
            WritableRaster raster = img.getRaster();
            raster.setDataElements(0, 0, width, height, imgBytes);
        }

        return img;
    }

    private byte[] convertAndScaleData(byte[] imgBytes, int width, int height, int pixelType, boolean isLittleEndian) {
        int bytesPerPixel = FormatTools.getBytesPerPixel(pixelType);
        int dataLength = width * height;
        short[] shortData = new short[dataLength];
        byte[] byteData = new byte[dataLength];
        int maxPixelValue = 0;

        for (int i = 0; i < dataLength; i++) {
            int pixelValue = 0;
            if (isLittleEndian) {
                for (int b = bytesPerPixel - 1; b >= 0; b--) {
                    pixelValue = (pixelValue << 8) | (imgBytes[i * bytesPerPixel + b] & 0xFF);
                }
            } else {
                for (int b = 0; b < bytesPerPixel; b++) {
                    pixelValue = (pixelValue << 8) | (imgBytes[i * bytesPerPixel + b] & 0xFF);
                }
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

        return byteData;
    }
}
