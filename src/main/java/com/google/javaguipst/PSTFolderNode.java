/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.google.javaguipst;

import com.pff.PSTFolder;
import com.pff.PSTMessageStore;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 *
 * @author franck
 */
public class PSTFolderNode extends DefaultMutableTreeNode {

  PSTFolderNode(PSTMessageStore messageStore) {
    super(messageStore);
  }

  PSTFolderNode(PSTFolder folder) {
    super(folder);
  }

  @Override
  public String toString() {
    Object o = this.getUserObject();
    if (o != null) {
      if (o instanceof PSTFolder) {
        PSTFolder folderValue = (PSTFolder) o;
        return folderValue.getDescriptorNodeId() + " - " + folderValue.getDisplayName() + " " + folderValue.getAssociateContentCount() + "";
      } else if (o instanceof PSTMessageStore) {
        PSTMessageStore folderValue = (PSTMessageStore) o;
        return folderValue.getDisplayName();
      } else {
        return o.toString();
      }
    } else {
      return null;
    }
  }
}
