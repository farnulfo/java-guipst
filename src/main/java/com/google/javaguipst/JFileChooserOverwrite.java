/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.google.javaguipst;

import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

public class JFileChooserOverwrite extends JFileChooser {

    @Override
    public void approveSelection() {
        File file = getSelectedFile();
        if (file.exists()) {
            int answer = JOptionPane.showConfirmDialog(
                    this, file + " exists. Overwrite?", "Overwrite?",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (answer != JOptionPane.OK_OPTION) {
                return;
            }
        }
        super.approveSelection();
    }
}
