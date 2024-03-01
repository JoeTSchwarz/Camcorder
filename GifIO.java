// 
//  GifIO.java
//  
import org.w3c.dom.*;
import javax.imageio.*;
import javax.imageio.metadata.*;
import javax.imageio.stream.*;
//
import java.awt.RenderingHints;
import java.awt.Graphics2D;
import java.awt.image.*;
import java.awt.Color;
//
import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.nio.file.Files;
// Joe Schwarz
public class GifIO {
  /**
   readGIF from a gif file
   @param fgif String, GIF file name
   @return ArrayList of BufferedImage
   @throws Exception if something is wrong
  */
  public static ArrayList<BufferedImage> readGIF(String fgif) throws Exception {
    ImageReader reader = (ImageReader)ImageIO.getImageReadersByFormatName("gif").next();
    reader.setInput(ImageIO.createImageInputStream(new File(fgif)), false);
    
    ArrayList<BufferedImage> list = new ArrayList<>();
    ByteArrayOutputStream bao = new ByteArrayOutputStream();
    
    BufferedImage master = null;
    for (int i = 0, max = reader.getNumImages(true); i < max; ++i) {
      BufferedImage image = reader.read(i);

      NodeList children = reader.
                          getImageMetadata(i).
                          getAsTree("javax_imageio_gif_image_1.0").
                          getChildNodes();

      for (int j = 0, mx = children.getLength(); j < mx; ++j) {
        Node nodeItem = children.item(j);
        if(nodeItem.getNodeName().equals("ImageDescriptor")) {
          if(i == 0) {
            master = new BufferedImage(Integer.valueOf(nodeItem.
                                                       getAttributes().
                                                       getNamedItem("imageWidth").
                                                       getNodeValue()
                                                      ),
                                       Integer.valueOf(nodeItem.
                                                       getAttributes().
                                                       getNamedItem("imageHeight").
                                                       getNodeValue()
                                                      ),                                                   
                                       BufferedImage.TYPE_INT_ARGB
                                      );              
          }
          master.getGraphics().drawImage(image,
                               Integer.valueOf(nodeItem.
                                               getAttributes().
                                               getNamedItem("imageLeftPosition").
                                               getNodeValue()
                                              ),
                               Integer.valueOf(nodeItem.
                                               getAttributes().
                                               getNamedItem("imageTopPosition").
                                               getNodeValue()
                                              ),
                               null
                              );
          ImageIO.write(master, "PNG", bao);
          list.add(ImageIO.read(new ByteArrayInputStream(bao.toByteArray())));
          bao.reset();
        }
      }
    }
    return list;
  }
  /**
   readGZIP BufferedImage from a gzip file
   @param fgzip String, GIF file name
   @return ArrayList of BufferedImage
   @throws Exception if something is wrong
  */
  public static ArrayList<BufferedImage> readGZIP(String fgzip) throws Exception {
    byte[] buf = java.nio.file.Files.readAllBytes((new File(fgzip)).toPath());
    if (buf.length == 0) throw new Exception(fgzip+" is empty");
    ByteArrayInputStream bis = new ByteArrayInputStream(buf); 
    GZIPInputStream gi = new GZIPInputStream(bis);
    //
    buf = new byte[1048576]; // 1MB
    ArrayList<BufferedImage> bImgLst = new ArrayList<>();
    ByteArrayOutputStream bao = new ByteArrayOutputStream();
    for (int n = gi.read(buf); n > 0; n = gi.read(buf)) bao.write(buf, 0, n);
    bao.flush();
    buf = bao.toByteArray();      
    bao.close();
    gi.close();
    //
    for (int i = 0, l; i < buf.length; i += (4+l)) {
      l = (int)((buf[i]&0xFF)<<24)+(int)((buf[i+1]&0xFF)<<16)+
          (int)((buf[i+2]&0xFF)<<8)+(int)(buf[i+3]&0xFF);
      bImgLst.add(ImageIO.read(new ByteArrayInputStream(buf, i+4, l)));
    }
    return bImgLst;
  }
  /**
  @param zR float, Zooming Ratio between 2.0 .. 0.1 (max. 2.0, min. 0.1)
  @param imgLst ArrayList of BufferedImages
  @param outFile String, the outputfile.gif
  @param loop , 0: loop repeatedly, 1: no loop
  @throws Exception if something is wrong
  */
  public static void writeGIF(float zR, ArrayList<BufferedImage> imgLst,
                               String outFile, int loop) throws Exception {
    BufferedImage img = imgLst.get(0);
    ImageOutputStream output = new FileImageOutputStream(new File(outFile));
    zR = zR < 2.0f? zR < 0.1f? 0.1f:zR : 2.0f;
    GifIO writer = new GifIO(output, loop);
    if (zR > 0.95f && zR < 1.05f) {
      for(BufferedImage bImg : imgLst) writer.write(bImg);
    } else {
      int type = img.getType();      
      int wi = (int)(Math.ceil(zR * img.getWidth()));
      int hi = (int)(Math.ceil(zR * img.getHeight()));
      for(BufferedImage bImg : imgLst) {
        BufferedImage image = new BufferedImage(wi, hi, type);
        Graphics2D graphics2D = image.createGraphics();
        graphics2D.setBackground(Color.WHITE);
        graphics2D.setPaint(Color.WHITE);
        graphics2D.fillRect(0, 0, wi, hi);
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
        graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING,
                                    RenderingHints.VALUE_RENDER_QUALITY);
        graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics2D.drawImage(bImg, 0, 0, wi, hi, null);
        writer.write(image);
      }
    }
    output.flush();
    writer.close();
    output.close();
  }
  /**
  @param zR float, Zooming Ratio between 2.0 .. 0.1 (max. 2.0, min. 0.1)
  @param imgLst ArrayList of BufferedImages
  @param outFile String, the outputfile.gzip
  @throws Exception if something is wrong
  */
  public static void writeGZIP(float zR, ArrayList<BufferedImage> imgLst,
                               String outFile) throws Exception {
    GZIPOutputStream go = new GZIPOutputStream(new FileOutputStream(outFile, false), true);
    ByteArrayOutputStream bos = new ByteArrayOutputStream( );
    ByteArrayOutputStream bao = new ByteArrayOutputStream( );
    zR = zR < 2.0f? zR < 0.1f? 0.1f:zR : 2.0f;
    if (zR < 0.95f || zR > 1.05f) {
      for(BufferedImage bImg : imgLst) write(bImg, bao, bos);
    } else {
      BufferedImage img = imgLst.get(0);      
      int wi = (int)(Math.ceil(zR * img.getWidth()));
      int hi = (int)(Math.ceil(zR * img.getHeight()));
      for(BufferedImage bImg : imgLst) {
        img = new BufferedImage(wi, hi, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics2D = img.createGraphics();
        graphics2D.setBackground(Color.WHITE);
        graphics2D.setPaint(Color.WHITE);
        graphics2D.fillRect(0, 0, wi, hi);
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
        graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING,
                                    RenderingHints.VALUE_RENDER_QUALITY);
        graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics2D.drawImage(bImg, 0, 0, wi, hi, null);
        write(img, bao, bos);
      }
    }
    go.write(bos.toByteArray());
    bos.close();
    go.flush( );
    go.close( );
  }
  /**
   write a BufferedImage in GZIP format
   * @param img BufferedImage
   * @param bao ByteArrayOutputStream, the on-behalf stream
   * @throws Exception if something is wrong
   */
  private static void write(BufferedImage img,
                            ByteArrayOutputStream bao,  
                            ByteArrayOutputStream bos) throws Exception {
    ImageIO.write(img, "JPG", bao);
    int le = bao.size(); // get the Image size
    bos.write(new byte[] {(byte)((le >> 24)&0xFF), (byte)((le >> 16)&0xFF),
                          (byte)((le >> 8)&0xFF), (byte)( le & 0xFF)
                         }
             );       
    bos.write(bao.toByteArray());
    bos.flush();
    bao.reset();
  }    
  /**
   * private Constructor
   * 
   * @param outStream the ImageOutputStream to be written to
   * @param loop int, 0: loop repeatedly, 1: no loop
   * @throws OException if something is wrong
   */
  private GifIO(ImageOutputStream outStream, int loop) throws Exception {
    Iterator<ImageWriter> iter = ImageIO.getImageWritersBySuffix("gif");
    if(!iter.hasNext()) throw new IIOException("No GIF Image Writers Exist");
    imgWriter = iter.next();
    //
    imgWriteParam = imgWriter.getDefaultWriteParam();
    ImageTypeSpecifier imageTypeSpecifier = ImageTypeSpecifier.
          createFromBufferedImageType(BufferedImage.TYPE_INT_ARGB);

    imgMetaData = imgWriter.getDefaultImageMetadata(imageTypeSpecifier, imgWriteParam);

    String metaFormatName = imgMetaData.getNativeMetadataFormatName();

    IIOMetadataNode root = (IIOMetadataNode) imgMetaData.getAsTree(metaFormatName);

    IIOMetadataNode graphicsControlExtensionNode = getNode(root, "GraphicControlExtension");

    graphicsControlExtensionNode.setAttribute("disposalMethod", "none");
    graphicsControlExtensionNode.setAttribute("userInputFlag", "FALSE");
    graphicsControlExtensionNode.setAttribute("transparentColorFlag", "FALSE");
    graphicsControlExtensionNode.setAttribute("delayTime", "10"); // 10 mSecond
    graphicsControlExtensionNode.setAttribute("transparentColorIndex", "0");

    IIOMetadataNode commentsNode = getNode(root, "CommentExtensions");
    commentsNode.setAttribute("CommentExtension", "Created by JAVA");

    IIOMetadataNode appEntensionsNode = getNode(root, "ApplicationExtensions");
    IIOMetadataNode child = new IIOMetadataNode("ApplicationExtension");
    // don't change applicationID "NETSCAPE"
    child.setAttribute("applicationID", "NETSCAPE");
    child.setAttribute("authenticationCode", "2.0");
    // loop: accept only 0 or 1
    child.setUserObject(new byte[]{ 0x01, (byte)(loop & 0x01), 0x00 });
    appEntensionsNode.appendChild(child);
    imgMetaData.setFromTree(metaFormatName, root);
    imgWriter.setOutput(outStream);
    imgWriter.prepareWriteSequence(null);
  }
  /**
   write a BufferedImage in GIF format
   * @param img BufferedImage
   * @throws IOException if something is wrong
   */
  private void write(RenderedImage img) throws IOException {
    imgWriter.writeToSequence(new IIOImage(img, null, imgMetaData),imgWriteParam);
  }
  
  /**
   * Close this GifIO object. This does not close the underlying
   * stream, just finishes off the GIF.
   * @throws OException if something is wrong
   */
  private void close() throws IOException {
    imgWriter.endWriteSequence();    
  }
  /**
   * Returns an existing child node, or creates and returns a new child node (if 
   * the requested node does not exist).
   * 
   * @param rootNode the <tt>IIOMetadataNode</tt> to search for the child node.
   * @param nodeName the name of the child node.
   * 
   * @return the child node, if found or a new node created with the given name.
   */
  private static IIOMetadataNode getNode(IIOMetadataNode rootNode, String nodeName) {
    int nNodes = rootNode.getLength();
    for (int i = 0; i < nNodes; ++i) {
      if (rootNode.item(i).getNodeName().compareToIgnoreCase(nodeName) == 0) {
        return((IIOMetadataNode) rootNode.item(i));
      }
    }
    IIOMetadataNode node = new IIOMetadataNode(nodeName);
    rootNode.appendChild(node);
    return(node);
  }
  //
  private ImageWriter imgWriter;
  private IIOMetadata imgMetaData;
  private ImageWriteParam imgWriteParam;
}