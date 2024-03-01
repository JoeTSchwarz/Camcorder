 //
import javafx.application.*;
import javafx.stage.*;
import javafx.scene.*;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.*;
import javafx.scene.canvas.*;
import javafx.scene.text.FontSmoothingType;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.*;
import javafx.geometry.Pos;

import java.io.*;
import java.util.*;

import java.util.concurrent.*;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
// Joe Schwarz  (C)
public class Display {
  /**
  Display a BufferedImage array
  @param images ArrayList of buffered images
  @param tFrame int, the time frame between images
  */
  public Display(List<BufferedImage> images, int tFrame) {
    if (images.size() == 0) {
      Tools.dialog(null, false, "List of BufferedImages is empty.");
      return;
    }
    this.tFrame = tFrame;
    this.images = images;
    //    
    popup = new Stage();
    popup.setOnCloseRequest(ev -> {
      closed = true;
      popup.close();
    });
    popup.setTitle("Playing Images");
    BufferedImage img = images.get(0);
    height = img.getHeight();
    width = img.getWidth();
    //
    Canvas canvas = new Canvas(width, height);
    gc = canvas.getGraphicsContext2D( );
    gc.setFontSmoothingType(FontSmoothingType.GRAY);
    //
    Button next = Tools.getButton("NEXT", "Next Image");
    next.setOnAction(a -> {
      toggle = true;
      nxt = true;
      step = 1;
    });
    next.setDisable(true);
    //
    Button prev = Tools.getButton("PREV", "Previous Image");
    prev.setOnAction(a -> {
      toggle = true;
      nxt = true;
      step = -1;
    });
    prev.setDisable(true);
    //
    Button act = Tools.getButton("STOP", "Stop/Start playing");
    act.setOnAction(a -> {
      stop = !stop;
        step = 0;
      if (stop) {
        next.setDisable(false);
        prev.setDisable(false);
        act.setText("START");
        toggle = false;
      } else {
        next.setDisable(true);
        prev.setDisable(true);
        act.setText("STOP");
        toggle = true;
        nxt = true;
      }
    });
    HBox hbox = new HBox(5);
    hbox.setAlignment(Pos.CENTER);
    hbox.setPadding(new Insets(5, 5, 5, 5));
    hbox.getChildren().addAll(prev, act, next);
    //
    VBox vbox = new VBox(5);
    vbox.setAlignment(Pos.CENTER);
    vbox.setPadding(new Insets(5, 5, 5, 5));
    vbox.getChildren().addAll(canvas, hbox);
      
    Scene scene = new Scene(vbox, width+10, height+60);
    scene.getStylesheets().add("gif.css");
    popup.setScene(scene);
    popup.show();
    // start playing the images in ForkJoinPool
    ForkJoinPool.commonPool().execute(() -> {
      // repeat until closed is set
      for (int i = 0, mx = images.size()-1; !closed; ) {
        if (toggle) {
          gc.drawImage(SwingFXUtils.toFXImage(images.get(i), null), 0, 0);
          if (step == 0) try {
            ++i; // next image
            TimeUnit.MILLISECONDS.sleep(tFrame);
          } catch (Exception ex) { }
          else { // Stepping
            i += step;
            nxt = false;
            while (!nxt) ; // do nothing
          }
        } else {
          gc.drawImage(SwingFXUtils.toFXImage(images.get(i), null), 0, 0);
          while (!toggle) ; // do nothing
        }
        if (i < 0) i = mx;
        else if (i > mx) i = 0;
      }
    });
  }
  //
  private Stage popup;
  private GraphicsContext gc;
  private volatile int step = 0;
  private int tFrame, height, width;
  private List<BufferedImage> images;
  private volatile boolean stop = false, on = false, closed = false, toggle = true, nxt = true;
}

