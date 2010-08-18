/*
 * DesktopApplication1View.java
 */
package com.google.javaguipst;

import com.pff.PSTActivity;
import com.pff.PSTAttachment;
import com.pff.PSTContact;
import com.pff.PSTException;
import com.pff.PSTFile;
import com.pff.PSTFolder;
import com.pff.PSTMessage;
import com.pff.PSTRss;
import com.pff.PSTTask;
import java.awt.Font;
import org.jdesktop.application.Action;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.TaskMonitor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Timer;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import org.jdesktop.application.Task;

/**
 * The application's main frame.
 */
public class DesktopApplication1View extends FrameView {

  public DesktopApplication1View(SingleFrameApplication app) {
    super(app);

    initComponents();
    folderTree.setModel(null);
    DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
    renderer.setLeafIcon(renderer.getDefaultClosedIcon());
    folderTree.setCellRenderer(renderer);
    // event handler for changing...


    emailTable.setFillsViewportHeight(true);
    emailTable.setModel(new DefaultTableModel());

    ListSelectionModel selectionModel = emailTable.getSelectionModel();
    selectionModel.addListSelectionListener(new ListSelectionListenerImpl(emailTable, attachmentsPane));

    emailContentPane.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

    // Always select the message tab
    viewTabbedPane.setSelectedIndex(0);

    // status bar initialization - message timeout, idle icon and busy animation, etc
    ResourceMap resourceMap = getResourceMap();
    int messageTimeout = resourceMap.getInteger("StatusBar.messageTimeout");
    messageTimer = new Timer(messageTimeout, new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        statusMessageLabel.setText("");
      }
    });

    messageTimer.setRepeats(false);
    int busyAnimationRate = resourceMap.getInteger("StatusBar.busyAnimationRate");
    for (int i = 0;
            i < busyIcons.length;
            i++) {
      busyIcons[i] = resourceMap.getIcon("StatusBar.busyIcons[" + i + "]");
    }
    busyIconTimer = new Timer(busyAnimationRate, new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        busyIconIndex = (busyIconIndex + 1) % busyIcons.length;
        statusAnimationLabel.setIcon(busyIcons[busyIconIndex]);
      }
    });
    idleIcon = resourceMap.getIcon("StatusBar.idleIcon");

    statusAnimationLabel.setIcon(idleIcon);

    progressBar.setVisible(false);
    // connecting action tasks to status bar via TaskMonitor
    TaskMonitor taskMonitor = new TaskMonitor(getApplication().getContext());

    taskMonitor.addPropertyChangeListener(new java.beans.PropertyChangeListener() {

      public void propertyChange(java.beans.PropertyChangeEvent evt) {
        String propertyName = evt.getPropertyName();
        if ("started".equals(propertyName)) {
          if (!busyIconTimer.isRunning()) {
            statusAnimationLabel.setIcon(busyIcons[0]);
            busyIconIndex = 0;
            busyIconTimer.start();
          }
          progressBar.setVisible(true);
          progressBar.setIndeterminate(true);
        } else if ("done".equals(propertyName)) {
          busyIconTimer.stop();
          statusAnimationLabel.setIcon(idleIcon);
          progressBar.setVisible(false);
          progressBar.setValue(0);
        } else if ("message".equals(propertyName)) {
          String text = (String) (evt.getNewValue());
          statusMessageLabel.setText((text == null) ? "" : text);
          messageTimer.restart();
        } else if ("progress".equals(propertyName)) {
          int value = (Integer) (evt.getNewValue());
          progressBar.setVisible(true);
          progressBar.setIndeterminate(false);
          progressBar.setValue(value);
        }
      }
    });

  }

  @Action
  public void showAboutBox() {
    if (aboutBox == null) {
      JFrame mainFrame = DesktopApplication1.getApplication().getMainFrame();
      aboutBox = new DesktopApplication1AboutBox(mainFrame);
      aboutBox.setLocationRelativeTo(mainFrame);


    }
    DesktopApplication1.getApplication().show(aboutBox);


  }

  @Action(block = Task.BlockingScope.APPLICATION)
  public Task loadPSTFile() {


    return new LoadPSTFileTask(org.jdesktop.application.Application.getInstance(com.google.javaguipst.DesktopApplication1.class));
  }

  private class LoadPSTFileTask extends org.jdesktop.application.Task<Object, Void> {

    File file = null;

    LoadPSTFileTask(org.jdesktop.application.Application app) {
      // Runs on the EDT.  Copy GUI state that
      // doInBackground() depends on from parameters
      // to LoadPSTFileTask fields, here.
      super(app);
      JFileChooser jFileChooser = new JFileChooser();
      int returnVal = jFileChooser.showOpenDialog(null);
      if (returnVal == JFileChooser.APPROVE_OPTION) {
        file = jFileChooser.getSelectedFile();
      }
    }

    @Override
    protected Object doInBackground() {
      if (file == null) {
        // No selected file
        return null;
      }
      Hashtable result = null;
      PSTFile pstFile = null;
      PSTFolderNode top = null;
      try {
        // Your Task's code here.  This method runs
        // on a background thread, so don't reference
        // the Swing GUI from here.
        pstFile = new PSTFile(file.getCanonicalPath());
        top = new PSTFolderNode(pstFile.getMessageStore());
        DesktopApplication1.buildTree(top, pstFile.getRootFolder());
        result = new Hashtable();
        result.put("pstFile", pstFile);
        result.put("top", top);


      } catch (FileNotFoundException ex) {
        Logger.getLogger(DesktopApplication1.class.getName()).log(Level.SEVERE, null, ex);
      } catch (PSTException ex) {
        Logger.getLogger(DesktopApplication1.class.getName()).log(Level.SEVERE, null, ex);
      } catch (IOException ex) {
        Logger.getLogger(DesktopApplication1.class.getName()).log(Level.SEVERE, null, ex);
      }
      return result;  // return your result
    }

    @Override
    protected void succeeded(Object result) {
      // Runs on the EDT.  Update the GUI based on
      // the result computed by doInBackground().
      if (result != null) {
        try {
          Hashtable context = (Hashtable) result;
          DefaultMutableTreeNode top = (DefaultMutableTreeNode) context.get("top");
          folderTree.setModel(new DefaultTreeModel(top));
          PSTFile pstFile = (PSTFile) context.get("pstFile");
          EmailTableModel emailTableModel = new EmailTableModel(pstFile.getRootFolder(), pstFile);
          emailTable.setModel(emailTableModel);
          TreeSelectionListener[] listeners = folderTree.getListeners(TreeSelectionListener.class);
          for (int i = 0; i < listeners.length; i++) {
            TreeSelectionListener listener = listeners[i];
            folderTree.removeTreeSelectionListener(listener);
          }
          folderTree.addTreeSelectionListener(new TreeSelectionListenerImpl(emailContentPane, emailTableModel, attachmentsPane));
        } catch (PSTException ex) {
          Logger.getLogger(DesktopApplication1View.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
          Logger.getLogger(DesktopApplication1View.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
    }
  }

  /** This method is called from within the constructor to
   * initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is
   * always regenerated by the Form Editor.
   */
  @SuppressWarnings("unchecked")
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {

    mainPanel = new javax.swing.JPanel();
    jSplitPane1 = new javax.swing.JSplitPane();
    jScrollPane1 = new javax.swing.JScrollPane();
    folderTree = new javax.swing.JTree();
    jSplitPane2 = new javax.swing.JSplitPane();
    jScrollPane2 = new javax.swing.JScrollPane();
    emailTable = new javax.swing.JTable();
    viewTabbedPane = new javax.swing.JTabbedPane();
    jScrollPane3 = new javax.swing.JScrollPane();
    emailContentPane = new javax.swing.JTextPane();
    attachmentsPane = new javax.swing.JPanel();
    menuBar = new javax.swing.JMenuBar();
    javax.swing.JMenu fileMenu = new javax.swing.JMenu();
    jMenuItemLoadPSTFile = new javax.swing.JMenuItem();
    javax.swing.JMenuItem exitMenuItem = new javax.swing.JMenuItem();
    javax.swing.JMenu helpMenu = new javax.swing.JMenu();
    javax.swing.JMenuItem aboutMenuItem = new javax.swing.JMenuItem();
    statusPanel = new javax.swing.JPanel();
    javax.swing.JSeparator statusPanelSeparator = new javax.swing.JSeparator();
    statusMessageLabel = new javax.swing.JLabel();
    statusAnimationLabel = new javax.swing.JLabel();
    progressBar = new javax.swing.JProgressBar();

    mainPanel.setName("mainPanel"); // NOI18N

    jSplitPane1.setName("jSplitPane1"); // NOI18N

    jScrollPane1.setName("jScrollPane1"); // NOI18N

    folderTree.setName("folderTree"); // NOI18N
    jScrollPane1.setViewportView(folderTree);

    jSplitPane1.setLeftComponent(jScrollPane1);

    jSplitPane2.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
    jSplitPane2.setName("jSplitPane2"); // NOI18N

    jScrollPane2.setName("jScrollPane2"); // NOI18N

    emailTable.setModel(new javax.swing.table.DefaultTableModel(
      new Object [][] {
        {null, null, null, null},
        {null, null, null, null},
        {null, null, null, null},
        {null, null, null, null}
      },
      new String [] {
        "Title 1", "Title 2", "Title 3", "Title 4"
      }
    ));
    emailTable.setName("emailTable"); // NOI18N
    jScrollPane2.setViewportView(emailTable);

    jSplitPane2.setTopComponent(jScrollPane2);

    viewTabbedPane.setName("viewTabbedPane"); // NOI18N

    jScrollPane3.setName("jScrollPane3"); // NOI18N

    emailContentPane.setName("emailContentPane"); // NOI18N
    jScrollPane3.setViewportView(emailContentPane);

    org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(com.google.javaguipst.DesktopApplication1.class).getContext().getResourceMap(DesktopApplication1View.class);
    viewTabbedPane.addTab(resourceMap.getString("jScrollPane3.TabConstraints.tabTitle"), jScrollPane3); // NOI18N

    attachmentsPane.setName("attachmentsPane"); // NOI18N
    attachmentsPane.setLayout(new javax.swing.BoxLayout(attachmentsPane, javax.swing.BoxLayout.LINE_AXIS));
    viewTabbedPane.addTab(resourceMap.getString("attachmentsPane.TabConstraints.tabTitle"), attachmentsPane); // NOI18N

    jSplitPane2.setRightComponent(viewTabbedPane);

    jSplitPane1.setRightComponent(jSplitPane2);

    org.jdesktop.layout.GroupLayout mainPanelLayout = new org.jdesktop.layout.GroupLayout(mainPanel);
    mainPanel.setLayout(mainPanelLayout);
    mainPanelLayout.setHorizontalGroup(
      mainPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
      .add(org.jdesktop.layout.GroupLayout.TRAILING, jSplitPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 491, Short.MAX_VALUE)
    );
    mainPanelLayout.setVerticalGroup(
      mainPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
      .add(jSplitPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 281, Short.MAX_VALUE)
    );

    menuBar.setName("menuBar"); // NOI18N

    fileMenu.setText(resourceMap.getString("fileMenu.text")); // NOI18N
    fileMenu.setName("fileMenu"); // NOI18N

    javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(com.google.javaguipst.DesktopApplication1.class).getContext().getActionMap(DesktopApplication1View.class, this);
    jMenuItemLoadPSTFile.setAction(actionMap.get("loadPSTFile")); // NOI18N
    jMenuItemLoadPSTFile.setText(resourceMap.getString("jMenuItemLoadPSTFile.text")); // NOI18N
    jMenuItemLoadPSTFile.setName("jMenuItemLoadPSTFile"); // NOI18N
    fileMenu.add(jMenuItemLoadPSTFile);

    exitMenuItem.setAction(actionMap.get("quit")); // NOI18N
    exitMenuItem.setName("exitMenuItem"); // NOI18N
    fileMenu.add(exitMenuItem);

    menuBar.add(fileMenu);

    helpMenu.setText(resourceMap.getString("helpMenu.text")); // NOI18N
    helpMenu.setName("helpMenu"); // NOI18N

    aboutMenuItem.setAction(actionMap.get("showAboutBox")); // NOI18N
    aboutMenuItem.setName("aboutMenuItem"); // NOI18N
    helpMenu.add(aboutMenuItem);

    menuBar.add(helpMenu);

    statusPanel.setName("statusPanel"); // NOI18N

    statusPanelSeparator.setName("statusPanelSeparator"); // NOI18N

    statusMessageLabel.setName("statusMessageLabel"); // NOI18N

    statusAnimationLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
    statusAnimationLabel.setName("statusAnimationLabel"); // NOI18N

    progressBar.setName("progressBar"); // NOI18N

    org.jdesktop.layout.GroupLayout statusPanelLayout = new org.jdesktop.layout.GroupLayout(statusPanel);
    statusPanel.setLayout(statusPanelLayout);
    statusPanelLayout.setHorizontalGroup(
      statusPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
      .add(statusPanelSeparator, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 491, Short.MAX_VALUE)
      .add(statusPanelLayout.createSequentialGroup()
        .addContainerGap()
        .add(statusMessageLabel)
        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 295, Short.MAX_VALUE)
        .add(progressBar, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
        .add(statusAnimationLabel)
        .addContainerGap())
    );
    statusPanelLayout.setVerticalGroup(
      statusPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
      .add(statusPanelLayout.createSequentialGroup()
        .add(statusPanelSeparator, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        .add(statusPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
          .add(statusMessageLabel)
          .add(statusAnimationLabel)
          .add(progressBar, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
        .add(3, 3, 3))
    );

    setComponent(mainPanel);
    setMenuBar(menuBar);
    setStatusBar(statusPanel);
  }// </editor-fold>//GEN-END:initComponents
  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JPanel attachmentsPane;
  private javax.swing.JTextPane emailContentPane;
  private javax.swing.JTable emailTable;
  private javax.swing.JTree folderTree;
  private javax.swing.JMenuItem jMenuItemLoadPSTFile;
  private javax.swing.JScrollPane jScrollPane1;
  private javax.swing.JScrollPane jScrollPane2;
  private javax.swing.JScrollPane jScrollPane3;
  private javax.swing.JSplitPane jSplitPane1;
  private javax.swing.JSplitPane jSplitPane2;
  private javax.swing.JPanel mainPanel;
  private javax.swing.JMenuBar menuBar;
  private javax.swing.JProgressBar progressBar;
  private javax.swing.JLabel statusAnimationLabel;
  private javax.swing.JLabel statusMessageLabel;
  private javax.swing.JPanel statusPanel;
  private javax.swing.JTabbedPane viewTabbedPane;
  // End of variables declaration//GEN-END:variables
  private final Timer messageTimer;
  private final Timer busyIconTimer;
  private final Icon idleIcon;
  private final Icon[] busyIcons = new Icon[15];
  private int busyIconIndex = 0;
  private JDialog aboutBox;

  private class TreeSelectionListenerImpl implements TreeSelectionListener {

    private JTextPane emailContent;
    private EmailTableModel emailTableModel;
    private JPanel attachmentsPanel;

    public TreeSelectionListenerImpl(JTextPane emailContent, EmailTableModel emailTableModel, JPanel attachmentsPanel) {
      this.emailContent = emailContent;
      this.emailTableModel = emailTableModel;
      this.attachmentsPanel = attachmentsPanel;
    }

    public void valueChanged(TreeSelectionEvent e) {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode) folderTree.getLastSelectedPathComponent();
      if (node == null) {
        return;
      }
      if (node.getUserObject() instanceof PSTFolder) {
        PSTFolder folderValue = (PSTFolder) node.getUserObject();
        emailTableModel.setFolder(folderValue);
        emailContent.setText(null);
        attachmentsPanel.removeAll();
        attachmentsPanel.getParent().repaint();
      }
    }
  }

  private class ListSelectionListenerImpl implements ListSelectionListener {

    private JTable emailTable;
    private JPanel attachmentsPane;

    public ListSelectionListenerImpl(JTable emailTable, JPanel attachmentsPane) {
      this.emailTable = emailTable;
      this.attachmentsPane = attachmentsPane;
    }

    public void valueChanged(ListSelectionEvent e) {
      if (e.getValueIsAdjusting()) {
        return;
      }
      JTable source = emailTable;
      EmailTableModel emailTableModel = (EmailTableModel) emailTable.getModel();
      PSTMessage selectedMessage = emailTableModel.getMessageAtRow(source.getSelectedRow());
      if (selectedMessage instanceof PSTContact) {
        PSTContact contact = (PSTContact) selectedMessage;
        emailContentPane.setText(contact.toString());
      } else if (selectedMessage instanceof PSTTask) {
        PSTTask task = (PSTTask) selectedMessage;
        emailContentPane.setText(task.toString());
      } else if (selectedMessage instanceof PSTActivity) {
        PSTActivity journalEntry = (PSTActivity) selectedMessage;
        emailContentPane.setText(journalEntry.toString());
      } else if (selectedMessage instanceof PSTRss) {
        PSTRss rss = (PSTRss) selectedMessage;
        emailContentPane.setText(rss.toString());
      } else if (selectedMessage != null) {
        emailContentPane.setText(selectedMessage.getBodyHTML());
        updateAttachmentsPanel(selectedMessage);
      }
      //					treePane.getViewport().setViewPosition(new Point(0,0));
      emailContentPane.setCaretPosition(0);
    }

    private void updateAttachmentsPanel(PSTMessage selectedMessage) {
      attachmentsPane.removeAll();
      for (int i = 0; i < selectedMessage.getNumberOfAttachments(); i++) {
        try {
          PSTAttachment attachment = selectedMessage.getAttachment(i);
          JButton button = new JButton(attachment.getLongFilename());
          button.addActionListener(new ActionListenerImpl(attachment));
          attachmentsPane.add(button);

        } catch (PSTException ex) {
          Logger.getLogger(DesktopApplication1View.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
          Logger.getLogger(DesktopApplication1View.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
      //attachmentsPane.repaint();
      attachmentsPane.getParent().repaint();

    }

    private class ActionListenerImpl implements ActionListener {

      private PSTAttachment attachment;

      public ActionListenerImpl(PSTAttachment attachment) {
        this.attachment = attachment;
      }

      public void actionPerformed(ActionEvent e) {
        final JFileChooserOverwrite chooser = new JFileChooserOverwrite();
        File selectedFile = new File(attachment.getLongFilename());
        chooser.setSelectedFile(selectedFile);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        // Show the dialog; wait until dialog is closed
        int result = chooser.showSaveDialog(getComponent());


        boolean save = false;
        // Determine which button was clicked to close the dialog
        switch (result) {
          case JFileChooser.APPROVE_OPTION:
            // Approve (Open or Save) was clicked
            selectedFile = chooser.getSelectedFile();
            save = true;
            break;
          case JFileChooser.CANCEL_OPTION:
            // Cancel or the close-dialog icon was clicked
            break;
          case JFileChooser.ERROR_OPTION:
            // The selection process did not complete successfully
            break;
        }

        if (save) {
          InputStream in = null;
          try {
            in = attachment.getFileInputStream();
            OutputStream out = new FileOutputStream(selectedFile);
            // Transfer bytes from in to out
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
              out.write(buf, 0, len);
            }
            in.close();
            out.close();
          } catch (IOException ex) {
            Logger.getLogger(DesktopApplication1View.class.getName()).log(Level.SEVERE, null, ex);
          } catch (PSTException ex) {
            Logger.getLogger(DesktopApplication1View.class.getName()).log(Level.SEVERE, null, ex);
          } finally {
            try {
              in.close();
            } catch (IOException ex) {
              Logger.getLogger(DesktopApplication1View.class.getName()).log(Level.SEVERE, null, ex);
            }
          }
        }
      }
    }
  }
}
