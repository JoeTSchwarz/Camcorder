This is a virtual Camcorder that allows you to focus on a specific area of the screen and to record the actions occur within this area.
This package contains 4 sources files written in JavaFX:
1) Camcoder.java: the main class
2) Display.java: the diplay API to display the recorded/created GIF or GZIP files
3) GifIO.java: IO for saving/reading of recorded .gif and .gzip files
4) Tools.java: utilities
   
and 3 icons (snaduhr.gif, save.png, Camcoder.png) + gif.css and manifest.mf
Camcorder relies on Java and JavaFX. If your JDK version is smaller than 10 (best: JDK 8 or 9) you can compile the sources without any problem.
Otherwise you have to download the Open JavaFX according to your JDK version if your JDK is higher than 10.
The manifest.mf file is purposed for the case that you want to build JAR file via jar tool.
 
