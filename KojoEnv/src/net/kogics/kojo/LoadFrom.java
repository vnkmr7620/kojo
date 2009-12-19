/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.kogics.kojo;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

public final class LoadFrom implements ActionListener {

    public void actionPerformed(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "Kojo Files", "scala");
        chooser.setFileFilter(filter);
        int returnVal = chooser.showOpenDialog(null);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            CodeEditor ce = (CodeEditor) CodeEditor.instance();
            ce.loadFrom(chooser.getSelectedFile());
        }
    }
}
