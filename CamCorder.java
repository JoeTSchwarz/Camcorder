// Java
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.util.*;
import java.io.*;
//
import java.util.concurrent.*;
// JFX
import javafx.application.*;
import javafx.stage.*;
import javafx.scene.*;
import javafx.geometry.*;
import javafx.scene.layout.*;
import javafx.scene.control.*;
import javafx.scene.canvas.*;
import javafx.scene.text.*;
import javafx.scene.image.*;
import javafx.scene.input.*;
import javafx.scene.paint.Color;
import javafx.embed.swing.SwingFXUtils;
//
// Joe Schwarz (C)
//
public class CamCorder extends Application {
  //
  public void start(Stage panel) {
    this.panel = panel;
    Stage stage = new Stage();
    panel.setOnCloseRequest(ev -> {
      ev.consume();
    });
    try {
      BufferedImage screen = (new java.awt.Robot()).
        createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));
      wMx = screen.getWidth();
      hMx = screen.getHeight();
    } catch (Exception ex) {
      wMx = 1366; hMx = 768;
    }
    Canvas canvas = new Canvas(wMx, hMx);
    saveList = new ArrayList<>();
    bImgList = new ArrayList<>();
    // Focusing window
    Pane pane = new Pane();
    pane.getChildren().addAll(canvas);
    // hMx+13: adjust to the FrameBar with Iconify/Enlarge/Close button
    Scene window = new Scene(pane, wMx, hMx);
    window.setFill(Color.TRANSPARENT);
    window.getStylesheets().add("gif.css");
    stage.setScene(window);
    stage.setOpacity(0.5);
    //
    //
    flag = false;
    move = false;
    focused = false;
    gc = canvas.getGraphicsContext2D( );
    gc.setFontSmoothingType(FontSmoothingType.GRAY);
    canvas.addEventFilter(MouseEvent.ANY, (e) -> canvas.requestFocus());
    canvas.setOnMouseMoved(e -> {
      if (!move || !flag) return;
      int xx = (int)e.getX();
      int yy = (int)e.getY();
      //drawing rectangle....
      gc.clearRect(0, 0, wMx, hMx);
      //
      gc.setLineWidth(2);
      gc.setStroke(Color.RED);
      gc.strokeLine(x0, y0, xx, y0);
      gc.strokeLine(x0, yy, xx, yy);
      gc.strokeLine(x0, y0, x0, yy);
      gc.strokeLine(xx, y0, xx, yy);
    });
    canvas.setOnMouseReleased(e -> {
      if (!flag) return;
      int xx = (int)e.getX(); 
      int yy = (int)e.getY();
      if (x0 < 0) {
        x0 = xx;
        y0 = yy;
        move = true;
        return;
      }
      move = false;
      flag = false;
      // setting focusing area: x0, y0, width and height
      if (x0 > xx) {
        int x = x0;
        x0 = xx;
        xx = x;
      }
      if (y0 > yy) {
        int y = y0;
        y0 = yy;
        yy = y;
      }
      width = xx-x0; 
      height = yy-y0;
      y0 += lx? 30:13;
      //
      waiting(200);
      stage.hide();
      disable(false);
      focused = true;
    });
    //
    focus = Tools.getButton("FOCUS", "Focus Screen Image");
    focus.setOnAction(ev -> {
      gc.clearRect(0, 0, wMx, hMx);
      disable(true);
      stage.show();
      x0 = y0 = -1;
      flag = true;
    });
    start = Tools.getButton("START", "Start Recording Screen");
    start.setOnAction(ev -> {
      if (!focused) {
        Tools.dialog(panel, true, "Nowhere on SCREEN is focused.");
        return;
      }
      if (idle) {
        disable(true);
        // except START/STOP
        start.setDisable(false);
        start.setText("STOP");
        bImgList.clear();
        saveList.clear();
        idle = false;
        done = false;
      } else {
        start.setText("START");
        disable(false);
        idle = true;
        done = true;
      }
    });
    //
    load = Tools.getButton("LOAD", "Load from GIF/GZIP image file");
    load.setOnAction(ev -> {
      disable(true);
      fName = Tools.getFile(panel, dir);
      if (fName != null) try {
        bImgList.clear();
        if (fName.toLowerCase().lastIndexOf(".gif") > 0) {
          bImgList = GifIO.readGIF(fName);
        } else {
          bImgList = GifIO.readGZIP(fName);
        }
        focused = false;
      } catch (Exception ex) {
        Tools.dialog(panel, false, ex.toString());
      }
      disable(false);
    });
    edit = Tools.getButton("EDIT", "Edit BufferedImages");
    edit.setOnAction(ev -> {
      editing();
    });
    undo = Tools.getButton("UNDO", "Undo Editing");
    undo.setOnAction(ev -> {
      if (!saveList.isEmpty()) {
        disable(true);
        bImgList.clear(); // remove the content
        for (BufferedImage img:saveList) bImgList.add(Tools.iClone(img));
        disable(false);
      }
      Tools.dialog(panel, true, "Original BufferedImage List is restored");
    });
    show = Tools.getButton("SHOW", "Show animated BufferedImages");
    show.setOnAction(ev -> {
      int mx = bImgList.size();
      if (mx == 0) return;
      //
      Dialog<ButtonType> dia = new Dialog<>();
      dia.setTitle("CamCorder");
      dia.setHeaderText("SHOW GIF/GZIP Images");
      DialogPane dp = dia.getDialogPane();
      dp.getStylesheets().add("gif.css");
      dp.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
      //
      TextField tff = Tools.getTextField(0);
      TextField tfe = Tools.getTextField(mx);
      CheckBox cb = new CheckBox("Reverse");
      HBox hbox = new HBox(7);
      hbox.setAlignment(Pos.CENTER);
      hbox.getChildren().addAll(new Label("From Image"), tff,
                                new Label("To Image"), tfe, cb);
      dp.setContent(hbox);
      dp.setPrefSize(400, 100);
      Tools.setPosition(panel, dia);
      if (dia.showAndWait().get() == ButtonType.OK) {
        int b = Integer.parseInt(tff.getText());
        int e = Integer.parseInt(tfe.getText());
        if (b >= e) {
          Tools.dialog(panel, false, "Begin: "+b+" >= end: "+e);
          return;
        }
        ArrayList<BufferedImage> al = new ArrayList<>();
        if (cb.isSelected())
             for (int i = e-1; i >= b; --i) al.add(bImgList.get(i));
        else for (int i = b; i < e; ++i) al.add(bImgList.get(i));
        new Display(al, 40);
      }
    });
    save = Tools.getButton(new Image(CamCorder.class.getResourceAsStream("save.png")),
                                 "Save to GIF/GZIP file");
    save.setOnAction(ev -> {
      saving();
    });
    reset = Tools.getButton("RESET", "RESET");
    reset.setOnAction(ev -> {
      idle = true;
      done = true;
      flag = false;
      move = false;
      x0 = y0 = -1;
      focused = false;
      saveList.clear();
      bImgList.clear();
    });
    Button quit = Tools.getButton("QUIT", "Quit & Exit CamCorder");
    quit.setOnAction(ev -> {
      done = true;
      idle = false;
      running = false;
      //
      waiting(10);
      stage.close();
      panel.close();
      Platform.exit();
      System.exit(0);
    });
    VBox bBox = new VBox(5);
    bBox.setAlignment(Pos.CENTER);
    // Insets(top, right, bottom, left)
    bBox.setPadding(new Insets(10, 0, 15, 0));
    bBox.getChildren().addAll(focus, start, load, edit, undo, show, save, reset, quit);
    Scene scene = new Scene(bBox, 145, 350);
    scene.getStylesheets().add("gif.css");
    panel.getIcons().add(new Image(CamCorder.class.getResourceAsStream("Camcorder.png")));
    panel.setAlwaysOnTop(true);
    panel.setResizable(false);
    panel.setScene(scene);
    panel.setY(0);    
    panel.setX(0);
    panel.show();
    // snapshot from screen
    ForkJoinPool.commonPool().execute(() -> {
      BufferedImage bImg, image;
      while (running) {
        while (idle) 
          waiting(100);
        while (!done) {
          try {
            bImg = (new java.awt.Robot()).
                   createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));
            image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            image.setRGB(0, 0, width, height, 
                         bImg.getRGB(x0, y0, width, height, null, 0, width),
                         0, width);
            bImgList.add(image);
            TimeUnit.MILLISECONDS.sleep(20);
          } catch (Exception ex) { }    
        }
      }
    });
  }
  //
  private void editing() {
    int mx = bImgList.size();
    disable(true);
    if (mx > 0) try {
      BufferedImage img = bImgList.get(0);
      int x = img.getWidth();
      int y = img.getHeight();
      if (x < 100 || y < 100) {
        Tools.dialog(panel, false, "Images are too small for Editing. Min. 100x100.");
        disable(false);
        return;
      }
      if (saveList.isEmpty()) // clone the original
        for (BufferedImage bimg:bImgList) saveList.add(Tools.iClone(bimg));
      //
      Dialog<ButtonType> dia = new Dialog<>();
      dia.setTitle("CamCorder");
      dia.setHeaderText("Edit GIF/GZIP Images");
      DialogPane dp = dia.getDialogPane();
      dp.getStylesheets().add("gif.css");
      dp.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
      TextField tfa = new TextField("Any Text");
      tfa.setPrefWidth(245);
      HBox h1 = new HBox(5);
      h1.setAlignment(Pos.CENTER_LEFT);
      h1.getChildren().addAll(new Label("Your Text    "), tfa);
      //
      TextField tff = Tools.getTextField(0);
      TextField tfe = Tools.getTextField(mx);
      TextField tfo = Tools.getTextField(15);
      HBox h2 = new HBox(5);
      h2.setAlignment(Pos.CENTER_LEFT);
      h2.getChildren().addAll(new Label("From Image"), tff,
                              new Label("To Image"), tfe,
                              new Label("Font Size"), tfo);
      ComboBox<String> cbc = new ComboBox<>();
      cbc.getItems().addAll("YELLOW!WHITE!BLACK!GREEN!BLUE!RED".split("!"));
      java.awt.Color colors[] = { java.awt.Color.YELLOW,
                                  java.awt.Color.WHITE,
                                  java.awt.Color.BLACK,
                                  java.awt.Color.GREEN,
                                  java.awt.Color.BLUE,
                                  java.awt.Color.RED
                                };
      idx = 0;                          
      cbc.setValue("YELLOW");
      cbc.setOnAction(a -> {
        idx = cbc.getSelectionModel().getSelectedIndex();
      });
      ComboBox<String> cbf = new ComboBox<>();
      cbf.getItems().addAll("Arial!Courier!Georgia!Lucida!Times!Tahoma!Verdana".split("!"));
      cbf.setValue("Georgia");
      font = "Georgia";     
      cbf.setOnAction(a -> {
        font = cbf.getSelectionModel().getSelectedItem();
        if ("Times".equals(font)) font = "Times New Roman";
      });
      type = 0;
      int types[] = { java.awt.Font.BOLD,
                      java.awt.Font.ITALIC, 
                      java.awt.Font.PLAIN
                    };
      ComboBox<String> cbt = new ComboBox<>();
      cbt.getItems().addAll("BOLD!ITALIC!PLAIN".split("!"));
      cbt.setValue("BOLD");
      cbt.setOnAction(a -> {
        type = cbt.getSelectionModel().getSelectedIndex();        
      });
      HBox h3 = new HBox(18);
      h3.setAlignment(Pos.CENTER_LEFT);
      h3.getChildren().addAll(cbc, cbf, cbt);
      //      
      TextField tfx = Tools.getTextField(10);
      TextField tfy = Tools.getTextField(10);
      HBox h4 = new HBox(10);
      h4.setAlignment(Pos.CENTER_LEFT);
      h4.getChildren().addAll(new Label("X position (0.."+x+")"), tfx,
                              new Label("Y position (0.."+y+")"), tfy);
      VBox box = new VBox(5);
      box.getChildren().addAll(h1, h2, h4, h3);
      dp.setContent(box);
      dp.setPrefSize(400, 200);
      Tools.setPosition(panel, dia);
      if (dia.showAndWait().get() == ButtonType.CANCEL) {
        disable(false);
        return;
      }
      x = Integer.parseInt(tfx.getText().trim());
      y = Integer.parseInt(tfy.getText().trim());
      int b = Integer.parseInt(tff.getText().trim());
      int e = Integer.parseInt(tfe.getText().trim());
      if (e > mx) e = mx;
      if (b >= e) {
        Tools.dialog(panel, false, "Begin at:"+b+". End at:"+e+"?");
        return;
      }
      String text = tfa.getText().trim();
      int fsize = Integer.parseInt(tfo.getText().trim());
      if (fsize < 10) fsize = 10;
      if (fsize > 40) fsize = 40;
      if (y < fsize) y = fsize+5;
      for (int i = b; i < e; ++i) {
        java.awt.Graphics2D g =(java.awt.Graphics2D)(bImgList.get(i)).getGraphics();
        g.setFont(new java.awt.Font(font, types[type], fsize));
        g.setPaint(colors[idx]);
        g.drawString(text, x, y);
        g.dispose();
      }
    } catch (Exception ex) {
      Tools.dialog(panel, false, ex.toString());
    }
    disable(false);
  }
  // start, load, edit, save,...;
  private void disable(boolean boo) {
    focus.setDisable(boo);
    start.setDisable(boo);
    reset.setDisable(boo);
    show.setDisable(boo);
    load.setDisable(boo);
    edit.setDisable(boo);
    save.setDisable(boo);
    undo.setDisable(boo);
  }
  //
  private void saving() {
    int mx = bImgList.size();
    if (mx == 0) return;
    //
    disable(true);
    if (fName == null) fName = dir+File.separator+"image.gif";
    fName = Tools.getFile(panel, fName, "SAVE file as GIF/GZIP");
    if (fName == null) {
      disable(false);
      return;
    }
    Dialog<ButtonType> dia = new Dialog<>();
    dia.setTitle("CamCorder");
    dia.setHeaderText("SAVE GIF/GZIP Images");
    DialogPane dp = dia.getDialogPane();
    dp.getStylesheets().add("gif.css");
    dp.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
    //
    TextField tff = Tools.getTextField(0);
    TextField tfe = Tools.getTextField(mx);
    CheckBox cb = new CheckBox("Reverse");
    HBox hbox = new HBox(7);
    hbox.setAlignment(Pos.CENTER);
    hbox.getChildren().addAll(new Label("From Image"), tff,
                              new Label("To Image"), tfe, cb);
    //
    ratio = 1.0f;
    Slider slider = Tools.getSlider(420, 40);
    slider.valueProperty().addListener((observable, oValue, nValue) -> {
      ratio =  (float)nValue.doubleValue();
    });
    VBox vbox = new VBox(5);
    vbox.getChildren().addAll(hbox, slider);
    dp.setContent(vbox);
    dp.setPrefSize(440, 150);
    Tools.setPosition(panel, dia);
    if (dia.showAndWait().get() == ButtonType.CANCEL) {
      disable(false);
      return;
    }
    int b = Integer.parseInt(tff.getText());
    int e = Integer.parseInt(tfe.getText());
    if (b >= e) {
      disable(false);
      Tools.dialog(panel, false, "Begin: "+b+" >= end: "+e);
      return;
    }
    ArrayList<BufferedImage> al = new ArrayList<>();
    if (cb.isSelected())
         for (int i = e-1; i >= b; --i) al.add(bImgList.get(i));
    else for (int i = b; i < e; ++i) al.add(bImgList.get(i));
    
    try {
      if (fName.toLowerCase().indexOf(".gif") < 0)
           GifIO.writeGZIP(ratio, al, fName);
      else GifIO.writeGIF(ratio, al, fName, 0);
      //
      int n = fName.lastIndexOf(File.separator);
      if (n > 0) dir = fName.substring(0, n);
    } catch (Exception ex) {
      Tools.dialog(panel, false, ex.toString());
    }
    disable(false);
  }
  //
  private void waiting(int time) {
    try {
      TimeUnit.MILLISECONDS.sleep(time);
    } catch (Exception ex) { }
  }
  //
  private float ratio;
  private Stage panel;
  private boolean focused;
  private String font, fName;
  private GraphicsContext gc;
  private volatile ArrayList<BufferedImage> bImgList, saveList;
  private Button start, load, edit, save, focus, show, reset, undo;
  private boolean lx = System.getProperty("os.name").equals("Linux");
  private volatile int width, height, x0, y0, wMx, hMx, idx, fSize, type;
  private String dir = System.getProperty("user.dir")+File.separator+"GIFimages";
  private volatile boolean flag, move, idle = true, running = true, done = false;
}
