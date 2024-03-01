//
import java.io.*;
import java.util.*;
import java.util.zip.*;
import javax.imageio.*;
import javax.imageio.ImageIO;
import java.awt.image.*;
import org.w3c.dom.*;
import javax.imageio.metadata.*;
import javax.imageio.stream.*;

import javafx.application.Platform;
import javafx.stage.DirectoryChooser;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.stage.Stage;
import javafx.scene.image.*;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.geometry.Orientation;
import javafx.scene.input.KeyCode;
import javafx.scene.control.Alert.AlertType;
import javafx.embed.swing.SwingFXUtils;
// Joe Schwarz  (C)
public class Tools {
  /**
  iClone
  @param bImg BufferedImage
  @return BufferedImage the cloned BufferedImage
  */
  public static BufferedImage iClone(BufferedImage bImg) {
    int w = bImg.getWidth(), h = bImg.getHeight();
    BufferedImage img = new BufferedImage(w, h, bImg.getType());
    img.setRGB(0, 0, w, h, bImg.getRGB(0, 0, w, h, null, 0, w), 0, w);
    return img;
  }
  /**
  getDir
  @param name String, path of directory
  @param create boolean, true: create if not existed
  @return boolean name. false if create = false or failed to create 
  */
  public static boolean getDir(String name, boolean create) {
    try {
      File fdir = new File(name);
      boolean ok = fdir.exists();
      if (!create) return ok;
      if (!ok) ok = fdir.mkdir();
      if (ok) return true;
      dialog(null, false, "Unable to create "+name);
    } catch (Exception ex) { }
    return false;
 }
  /** 
   toFiles all extracted BufferedImages from a gif file
   @param gifFile String, GIF file name
   @param prefix String, the prefix name of extracted files: prefix_n.png where n is the sequence.
   @param type boolean. True: gif file, false: gzip file.
   @return int the number of Files
   @throws Exception of JAVA
  */
  public static int toFiles(String gifFile, String prefix, boolean type) throws Exception {
    int mx = 0;
    ArrayList<BufferedImage> bImgLst = type? 
                             GifIO.readGIF(gifFile):
                             GifIO.readGZIP(gifFile);
    if (bImgLst != null) {
      mx = bImgLst.size(); 
      for (int i = 0; i < mx; ++i) 
        ImageIO.write(bImgLst.get(i), "PNG", new File(prefix+"_"+i + ".png"));
    }
    return mx;
  }
   /**
  getFile
  @param panel Stage
  @param dir String, Directory
  @return String abs. path of selected File
  */
  public static String getFile(Stage panel, String dir) {
    FileChooser fChooser = new FileChooser();
    fChooser.setInitialDirectory(new File(getDir(dir, false)? dir:System.getProperty("user.dir")));
    fChooser.setTitle("JPG/JPEG/PNG/GIF/GZIP");
    fChooser.getExtensionFilters().
             add(new FileChooser.ExtensionFilter("Image", "*jpg", "jpeg", "*.gif",  "*.png", "*.gzip"));
    File file = fChooser.showOpenDialog(panel);
    if (file == null) return null;
    return file.getAbsolutePath();
  }
  /**
  getFile
  @param panel Stage
  @param fName String, abs. path
  @param txt String, Header text
  @return String abs. path of selected File
  */
  public static String getFile(Stage panel, final String fName, String txt) {
    Dialog<ButtonType> dia = new Dialog<>();
    dia.setTitle("CreateGIF");
    dia.setHeaderText(txt);
    DialogPane dp = dia.getDialogPane();
    dp.getStylesheets().add("gif.css");
    dp.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
    //
    TextField tf = new TextField(fName);
    tf.setOnAction(e -> {
      if (tf.getText().length() == 0) {
        String fN = getFile(panel, fName.substring(0, fName.lastIndexOf(File.separator)));
        if (fN != null) tf.setText(fN);
      }
    });
    VBox vbox = new VBox(5);
    vbox.setPadding(new Insets(5, 5, 5, 5));
    vbox.getChildren().addAll(new Label("File Name"), tf);
    vbox.setAlignment(Pos.CENTER_LEFT);

    dp.setContent(vbox);
    dp.setPrefSize(350, 140);
    Platform.runLater(() -> {
      tf.requestFocus();
    });
    setPosition(panel, dia);
    Optional<ButtonType> result = dia.showAndWait();
    ButtonType but = result.get();
    //
    String fN = tf.getText().trim();
    if (but == ButtonType.CANCEL || fN.length() == 0) return null;
    return fN;
  }
  /**
  Dialog positioning on top together with Stage
  @param panel Stage
  @param dia Dialog
  */
  public static void setPosition(Stage panel, Dialog<?> dia) {
    dia.initOwner(panel); // always on top of owner
    dia.setX(150);
    dia.setY(0);
  }
  /**
  getSlider
  @param w int width
  @param h int height
  @return Slider
  */
  public static Slider getSlider(int w, int h) {
    Slider slider = new Slider(0.1, 2, 1);
    slider.setShowTickMarks(true);
    slider.setShowTickLabels(true);
    slider.setMajorTickUnit(0.1);
    slider.setBlockIncrement(0.2);
    slider.setSnapToTicks(true);
    slider.setPrefSize(w, h);
    slider.setValue(1.0d);
    return slider;
  }
  /**
   @parem panel Stage, owner 
   @param T boolean, true: information, false: error
   @param txt String, information/error text
  */
  public static void dialog(Stage panel, boolean T, String txt) {
    Dialog<ButtonType> dia = new Dialog<>();
    dia.setTitle("CreateGIF");
    dia.setHeaderText(T?"INFORMATION":"ERROR");
    DialogPane dp = dia.getDialogPane();
    dp.getStylesheets().add("gif.css");
    dp.getButtonTypes().addAll(ButtonType.OK);
    dp.lookupButton(ButtonType.OK).setStyle("-fx-pref-width: 80; -fx-pref-height: 80;");
    Label label = new Label(txt);
    label.setStyle("-fx-text-fill: blue;-fx-font-weight: bold;");
    VBox box = new VBox();
    box.getChildren().add(label);
    dp.setContent(box);
    dp.setPrefSize(220+3*label.getText().length(), 90);
    setPosition(panel, dia);
    dia.showAndWait();
  }
  /**
   @param icon JFX Image or String
   @param txt String as tooltip or button name (if icon = null)
   @return Button
  */
  public static Button getButton(Object icon, String txt) {
    Button button = null;
    if (icon == null || icon instanceof String) {
      button = new Button(icon == null? txt:(String)icon);
      button.setStyle("-fx-font-size:11; -fx-font-weight: bold;");
    } else try {
      button = new Button( );
      button.setGraphic(new ImageView((Image) icon));
    } catch (Exception ex) { 
      button = new Button(txt);
      button.setStyle("-fx-font-weight: bold;");
    }
    Tooltip tt = new Tooltip();
    tt.setText(txt);
    tt.setStyle("-fx-base: #AE3522; -fx-text-fill: orange; -fx-font-cb: bold;");
    button.setTooltip(tt);
    return button;
  }
  /**
   @param v int, initial value
   @return TextField
  */
  public static TextField getTextField(final int v) {
    TextField txt = new TextField(""+v);
    //txt.setPromptText(""+v);
    txt.setPrefWidth(40);
    txt.setOnKeyReleased(e -> {
      KeyCode kc = e.getCode();
      if (kc.isDigitKey() || kc.isArrowKey()) return;
      StringBuilder tx = new StringBuilder(txt.getText());
      int le = tx.length();
      for (int i = 0; i < le;) {
        char c = tx.charAt(i);
        if (c < '0' || c > '9') {
          tx.deleteCharAt(i);
          --le;
        } else ++i;
      }
      if (le == 0) tx.append(""+v); 
      txt.setText(tx.toString());
      txt.positionCaret(tx.length());
    });
    return txt;
  }
}