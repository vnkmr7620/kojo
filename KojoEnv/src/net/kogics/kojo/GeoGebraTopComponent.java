/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.kogics.kojo;

import java.awt.BorderLayout;
import java.util.logging.Logger;
import javax.swing.JPanel;
import net.kogics.kojo.geogebra.GeoCanvas;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;
//import org.openide.util.ImageUtilities;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.util.ImageUtilities;

/**
 * Top component which displays something.
 */
@ConvertAsProperties(dtd = "-//net.kogics.kojo//GeoGebra//EN",
autostore = false)
public final class GeoGebraTopComponent extends TopComponent {

    private static GeoGebraTopComponent instance;
    /** path to the icon used by the component and its open action */
    static final String ICON_PATH = "images/geogebra16.png";
    private static final String PREFERRED_ID = "GeoGebraTopComponent";

    private JPanel geoGebraPanel() {
        return (JPanel)GeoCanvas.instance();
    }

    public GeoGebraTopComponent() {
        initComponents();
        add(geoGebraPanel(), BorderLayout.CENTER);
        setName(NbBundle.getMessage(GeoGebraTopComponent.class, "CTL_GeoGebraTopComponent"));
        setToolTipText(NbBundle.getMessage(GeoGebraTopComponent.class, "HINT_GeoGebraTopComponent"));
        setIcon(ImageUtilities.loadImage(ICON_PATH, true));

    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setLayout(new java.awt.BorderLayout());
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
    /**
     * Gets default instance. Do not use directly: reserved for *.settings files only,
     * i.e. deserialization routines; otherwise you could get a non-deserialized instance.
     * To obtain the singleton instance, use {@link #findInstance}.
     */
    public static synchronized GeoGebraTopComponent getDefault() {
        if (instance == null) {
            instance = new GeoGebraTopComponent();
        }
        return instance;
    }

    /**
     * Obtain the GeoGebraTopComponent instance. Never call {@link #getDefault} directly!
     */
    public static synchronized GeoGebraTopComponent findInstance() {
        TopComponent win = WindowManager.getDefault().findTopComponent(PREFERRED_ID);
        if (win == null) {
            Logger.getLogger(GeoGebraTopComponent.class.getName()).warning(
                    "Cannot find " + PREFERRED_ID + " component. It will not be located properly in the window system.");
            return getDefault();
        }
        if (win instanceof GeoGebraTopComponent) {
            return (GeoGebraTopComponent) win;
        }
        Logger.getLogger(GeoGebraTopComponent.class.getName()).warning(
                "There seem to be multiple components with the '" + PREFERRED_ID
                + "' ID. That is a potential source of errors and unexpected behavior.");
        return getDefault();
    }

    @Override
    public int getPersistenceType() {
        return TopComponent.PERSISTENCE_NEVER;
    }

    @Override
    public void componentOpened() {
        // TODO add custom code on component opening
    }

    @Override
    public void componentClosed() {
        // TODO add custom code on component closing
    }

    void writeProperties(java.util.Properties p) {
        // better to version settings since initial version as advocated at
        // http://wiki.apidesign.org/wiki/PropertyFiles
        p.setProperty("version", "1.0");
        // TODO store your settings
    }

    Object readProperties(java.util.Properties p) {
        if (instance == null) {
            instance = this;
        }
        instance.readPropertiesImpl(p);
        return instance;
    }

    private void readPropertiesImpl(java.util.Properties p) {
        String version = p.getProperty("version");
        // TODO read your settings according to their version
    }

    @Override
    protected String preferredID() {
        return PREFERRED_ID;
    }
}
