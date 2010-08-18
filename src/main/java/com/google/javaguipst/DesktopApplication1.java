/*
 * DesktopApplication1.java
 */
package com.google.javaguipst;

import com.pff.PSTException;
import com.pff.PSTFile;
import com.pff.PSTFolder;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.tree.DefaultMutableTreeNode;
import org.jdesktop.application.Action;
import org.jdesktop.application.Application;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.Task;

/**
 * The main class of the application.
 */
public class DesktopApplication1 extends SingleFrameApplication {

  /**
   * At startup create and show the main frame of the application.
   */
  @Override
  protected void startup() {
    show(new DesktopApplication1View(this));
  }

  /**
   * This method is to initialize the specified window by injecting resources.
   * Windows shown in our application come fully initialized from the GUI
   * builder, so this additional configuration is not needed.
   */
  @Override
  protected void configureWindow(java.awt.Window root) {
  }

  /**
   * A convenient static getter for the application instance.
   * @return the instance of DesktopApplication1
   */
  public static DesktopApplication1 getApplication() {
    return Application.getInstance(DesktopApplication1.class);
  }

  /**
   * Main method launching the application.
   */
  public static void main(String[] args) {
    launch(DesktopApplication1.class, args);
  }


  public static void buildTree(PSTFolderNode top, PSTFolder theFolder) {
    // this is recursive, try and keep up.
    try {
      Vector children = theFolder.getSubFolders();
      Iterator childrenIterator = children.iterator();
      while (childrenIterator.hasNext()) {
        PSTFolder folder = (PSTFolder) childrenIterator.next();

        PSTFolderNode node = new PSTFolderNode(folder);

        if (folder.getSubFolders().size() > 0) {
          buildTree(node, folder);
        } else {
        }
        top.add(node);
      }
    } catch (Exception err) {
      err.printStackTrace();
      System.exit(1);
    }
  }
}
