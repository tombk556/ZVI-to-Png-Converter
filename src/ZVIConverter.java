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
     * Converts a .zvi file to a PNG image (8-bit grayscale or 8-bit/channel RGB).
     * Performs min–max auto-scaling so the resulting image covers the full
     * 0..255 range.
     *
     * @param zviFilePath    Path to the input .zvi file.
     * @param pngOutputPath  Path where the output PNG file will be saved.
     * @throws FormatException If there is an error in the file format.
     * @throws IOException     If there is an IO error.
     */
    public void convertZVIToPNG(String zviFilePath, String pngOutputPath)
            throws FormatException, IOException {

        ImageReader reader = new ImageReader();
        try {
            // -- Set up OME-XML metadata --
            ServiceFactory factory = new ServiceFactory();
            OMEXMLService service = factory.getInstance(OMEXMLService.class);
            IMetadata meta = service.createOMEXMLMetadata();
            reader.setMetadataStore(meta);

            // -- Initialize the reader with the ZVI file --
            reader.setId(zviFilePath);

            // -- Query image dimensions & properties --
            int width = reader.getSizeX();
            int height = reader.getSizeY();
            int pixelType = reader.getPixelType();
            boolean isLittleEndian = reader.isLittleEndian();
            int channelCount = reader.getSizeC();

            // Debugging info
            System.out.println("Pixel Type: " + FormatTools.getPixelTypeString(pixelType));
            System.out.println("Endianness: " + (isLittleEndian ? "Little Endian" : "Big Endian"));
            System.out.println("Image Dimensions: " + width + " x " + height);
            System.out.println("Number of Channels: " + channelCount);

            // -- Process the image data --
            BufferedImage img = null;
            if (channelCount >= 3) {
                // If at least 3 channels, assume RGB
                img = processRGBImage(reader, width, height, pixelType, isLittleEndian);
            } else {
                // Otherwise, assume a single grayscale channel
                img = processGrayscaleImage(reader, width, height, pixelType, isLittleEndian);
            }

            // -- Write the resulting image to PNG --
            ImageIO.write(img, "png", new File(pngOutputPath));
            System.out.println("Conversion completed successfully: " + pngOutputPath);

        } catch (ServiceException e) {
            throw new RuntimeException(e);
        } catch (DependencyException e) {
            throw new RuntimeException(e);
        } finally {
            // -- Clean up --
            reader.close();
        }
    }

    /**
     * Processes an image with 3 channels (RGB).
     * Reads each channel's plane separately, applies min–max scaling (if >8 bits),
     * then combines into a TYPE_INT_RGB BufferedImage.
     */
    private BufferedImage processRGBImage(ImageReader reader, int width, int height,
                                          int pixelType, boolean isLittleEndian)
            throws FormatException, IOException {

        // We'll only extract the first 3 channels, even if more exist
        byte[][] channelData = new byte[3][];

        for (int c = 0; c < 3; c++) {
            // Calculate the plane index for channel c, z=0, t=0
            int planeIndex = reader.getIndex(0, c, 0);
            byte[] imgBytes = reader.openBytes(planeIndex);

            if (FormatTools.getBytesPerPixel(pixelType) > 1) {
                // Convert from 16-bit (or more) down to 8-bit
                channelData[c] = convertAndScaleData(
                        imgBytes, width, height, pixelType, isLittleEndian
                );
            } else {
                // Already 8-bit, just use as-is
                channelData[c] = imgBytes;
            }
        }

        // Now combine R, G, B into a single BufferedImage (TYPE_INT_RGB)
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

    /**
     * Processes an image with a single grayscale channel.
     * If > 8 bits, it will downscale to 8-bit using min–max scaling.
     */
    private BufferedImage processGrayscaleImage(ImageReader reader, int width, int height,
                                                int pixelType, boolean isLittleEndian)
            throws FormatException, IOException {

        // We assume channelCount == 1, so just open the first plane
        byte[] imgBytes = reader.openBytes(0);

        BufferedImage img;
        if (FormatTools.getBytesPerPixel(pixelType) > 1) {
            // If it's 16-bit or more, convert to 8-bit
            byte[] byteData = convertAndScaleData(
                    imgBytes, width, height, pixelType, isLittleEndian
            );
            img = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
            WritableRaster raster = img.getRaster();
            raster.setDataElements(0, 0, width, height, byteData);
        } else {
            // Already 8-bit, just use as-is
            img = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
            WritableRaster raster = img.getRaster();
            raster.setDataElements(0, 0, width, height, imgBytes);
        }

        return img;
    }

    /**
     * Converts high-bit pixel data (e.g. 12-bit, 16-bit) into 8-bit data.
     * We do a min–max auto-scaling: the smallest pixel becomes 0,
     * the largest pixel becomes 255.
     */
    private byte[] convertAndScaleData(byte[] imgBytes, int width, int height,
                                       int pixelType, boolean isLittleEndian) {

        int bytesPerPixel = FormatTools.getBytesPerPixel(pixelType);
        int dataLength = width * height;

        // We'll store the extracted pixel values (up to 16 or 32 bits) in intData
        int[] intData = new int[dataLength];

        // Pass 1: parse raw bytes into intData[], track min & max
        int minVal = Integer.MAX_VALUE;
        int maxVal = Integer.MIN_VALUE;

        for (int i = 0; i < dataLength; i++) {
            int pixelValue = 0;

            if (isLittleEndian) {
                // For little endian, we read from last byte down to first
                for (int b = bytesPerPixel - 1; b >= 0; b--) {
                    pixelValue = (pixelValue << 8) | (imgBytes[i * bytesPerPixel + b] & 0xFF);
                }
            } else {
                // For big endian, we read from first byte to last
                for (int b = 0; b < bytesPerPixel; b++) {
                    pixelValue = (pixelValue << 8) | (imgBytes[i * bytesPerPixel + b] & 0xFF);
                }
            }

            intData[i] = pixelValue;
            if (pixelValue < minVal) minVal = pixelValue;
            if (pixelValue > maxVal) maxVal = pixelValue;
        }

        System.out.println("Min pixel value: " + minVal);
        System.out.println("Max pixel value: " + maxVal);

        // Pass 2: map each intData[] to 0..255
        byte[] outBytes = new byte[dataLength];
        int range = maxVal - minVal;
        if (range < 1) {
            // If the image is uniform, just fill everything with 0 or 255
            for (int i = 0; i < dataLength; i++) {
                outBytes[i] = 0; // or (byte)255, but 0 if all pixels are the same
            }
            return outBytes;
        }

        for (int i = 0; i < dataLength; i++) {
            int scaled = (intData[i] - minVal);
            scaled = (scaled * 255) / range;  // integer arithmetic for 8-bit
            // clamp to [0..255] just in case
            if (scaled < 0)   scaled = 0;
            if (scaled > 255) scaled = 255;
            outBytes[i] = (byte) scaled;
        }

        return outBytes;
    }

    // OPTIONAL: A quick example main method to demonstrate usage.
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: java ZVIConverter <input.zvi> <output.png>");
            System.exit(1);
        }
        String inputZvi = args[0];
        String outputPng = args[1];

        try {
            ZVIConverter converter = new ZVIConverter();
            converter.convertZVIToPNG(inputZvi, outputPng);
        } catch (FormatException | IOException e) {
            e.printStackTrace();
        }
    }
}
