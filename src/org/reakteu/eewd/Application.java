/*
 * Copyright (C) 2014-2015 by gempa GmbH - http://gempa.de
 * License: http://www.gnu.org/licenses/gpl.html GPL version 2 or higher
 */
package org.reakteu.eewd;

import com.bbn.openmap.InformationDelegator;
import org.reakteu.eewd.layer.TargetLayer;
import org.reakteu.eewd.layer.StationLayer;
import org.reakteu.eewd.data.POI;
import com.bbn.openmap.LayerHandler;
import com.bbn.openmap.MapBean;
import java.awt.Component;

import javax.swing.JMenuBar;

import com.bbn.openmap.MapHandler;
import com.bbn.openmap.PropertyHandler;
import com.bbn.openmap.gui.BasicMapPanel;
import com.bbn.openmap.gui.DefaultHelpMenu;
import com.bbn.openmap.gui.FileMenu;
import com.bbn.openmap.gui.MapPanel;
import com.bbn.openmap.gui.OpenMapFrame;
import com.bbn.openmap.gui.OverlayMapPanel;
import com.bbn.openmap.gui.ToolPanel;
import com.bbn.openmap.gui.menu.AboutMenuItem;
import com.bbn.openmap.proj.Projection;
import com.bbn.openmap.util.ArgParser;
import com.bbn.openmap.util.Debug;
import java.awt.Desktop;
import java.awt.Point;
import org.reakteu.eewd.data.EventArchive;
import org.reakteu.eewd.data.EventData;
import org.reakteu.eewd.data.QMLListener;
import org.reakteu.eewd.data.EventTimeScheduler;
import org.reakteu.eewd.data.EventFileScheduler;
import org.reakteu.eewd.layer.EventLayer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import javax.swing.JEditorPane;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quakeml.xmlns.bedRt.x12.EventParameters;
import org.reakteu.eewd.data.EventCountdown;
import org.reakteu.eewd.data.Shaking;
import org.reakteu.eewd.data.ShakingCalculator;
import org.reakteu.eewd.layer.LogoLayer;
import org.reakteu.eewd.layer.ShakeMapLayer;

/**
 *
 * @author Stephan Herrnkind herrnkind at gempa dot de
 */
public class Application implements QMLListener, ActionListener {

    private static final Logger LOG = LogManager.getLogger(Application.class);

    public static final String PropertyMapProperties = "mapProperties";
    public static final String PropertyEventArchive = "eventArchive";

    // target
    public static final String PropertyTargetFile = "targetFile";
    public static final String PropertyTargetIcon = "targetIcon";
    public static final String PropertyShowTargetName = "showTargetName";
    public static final String PropertyBlindZoneRadius = "blindZoneRadius";

    // station
    public static final String PropertyStationFile = "stationFile";
    public static final String PropertyShowStations = "showStations";
    public static final String PropertyShowStationName = "showStationName";
    public static final String PropertyShowUsedStations = "showUsedStations";
    public static final String PropertyShowStationShaking = "showStationShaking";
    public static final String PropertyShowStationAlert = "showStationAlert";

    // logo
    public static final String PropertyLogoIcon = "logoIcon";

    // shake map
    public static final String PropertySM = "shakeMap";
    public static final String PropertySMFile = PropertySM + ".file";
    public static final String PropertySMParameter = PropertySM + ".parameter";
    public static final String PropertySMMinValue = PropertySM + ".minValue";
    public static final String PropertySMMaxValue = PropertySM + ".maxValue";
    public static final String PropertySMLogScale = PropertySM + ".logScale";

    // event
    public static final String PropertyVP = "vp";
    public static final String PropertyVS = "vs";
    public static final String PropertyEventIcon = "eventIcon";
    public static final String PropertyTimeoutAfterOriginTime = "timeoutAfterOriginTime";
    public static final String PropertyToFront = "toFront";
    public static final String PropertyAlertSound = "alertSound";
    public static final String PropertyAlertSoundLoop = "alertSoundLoop";
    public static final String PropertyCountdownSound = "countdownSound";
    public static final String PropertyCountdownSeconds = "countdownSeconds";

    // filter
    public static final String PropertyFilter = "filter";
    public static final String PropertyFilterMinMag = PropertyFilter + ".minimumMagnitude";
    public static final String PropertyFilterMinLikelihood = PropertyFilter + ".minimumLikelihood";

    // processing
    public static final String PropertyAmpliProxyName = "ampliProxyName";

    public static final String PropertyGMPE = "gmpe";
    public static final String PropertyGMICE = "gmice";
    public static final String PropertyIPE = "ipe";

    public static final String PropertyControlPeriod = "controlPeriod";

    // spectrum plot
    public static final String PropertySpec = "spectrum";
    public static final String PropertySpecPeriods = PropertySpec + ".periods";
    public static final String PropertySpecLogScale = PropertySpec + ".logScale";
    public static final String PropertySpecParameter = PropertySpec + ".parameter";
    public static final String PropertySpecRef1 = PropertySpec + ".reference1";
    public static final String PropertySpecRef2 = PropertySpec + ".reference2";

    public static final String PropertyUseFrequencies = "useFrequencies";
    public static final String PropertyRIsHypocentral = "rIsHypocentral";

    public static final String PropertyRadiusOfInfluence = "radiusOfInfluence";
    public static final String PropertyStationDisplacementThreshold = "stationDisplacementThreshold";
    public static final String PropertyStationTauCThreshold = "stationTauCThreshold";

    // messaging
    public static final String PropertyConHost = "connection.host";
    public static final String PropertyConPort = "connection.port";
    public static final String PropertyConTopic = "connection.topic";
    public static final String PropertyConUsername = "connection.username";
    public static final String PropertyConPassword = "connection.password";
    public static final String PropertyConKeepaliveInterval = "connection.keepaliveInterval";
    public static final String PropertyConMaxLatency = "connection.maxLatency";

    public static final double EarthAcceleration = 9.807;
    public static final double EarthAcceleration1 = 1 / EarthAcceleration;

    public static final double DefaultVP = 5.5;
    public static final double DefaultVS = 3.3;

    private static final String ActionEventBrowser = "eventBrowser";
    private static final String ActionAbout = "about";

    private static Application instance = null;

    // gui components
    private BasicMapPanel mapPanel;
    private OpenMapFrame openMapFrame = null;
    private EventPanel eventPanel = null;
    private StationLayer stationLayer = null;
    private EventLayer eventLayer = null;
    private ShakeMapLayer shakeMapLayer = null;

    private Properties properties;
    private final EventArchive eventArchive;
    private final EventTimeScheduler eventTimeScheduler;
    private final EventFileScheduler eventFileScheduler;
    private final EventCountdown eventCountdown;
    private final List<POI> targets;
    private final Map<String, POI> stations;

    private final ShakingCalculator shakingCalculator;

    private final Messaging messaging;

    private Double controlPeriod = null;
    private double[] periods = null;
    private boolean useFrequencies = false;
    private Shaking.Type spectrumParameter = Shaking.Type.PSA;
    private Shaking.Type shakeMapParameter = Shaking.Type.PGA;

    private String title = null;

    private Float minMag = null;
    private Float minLikelihood = null;

    public Application(Properties props) {
        instance = this;
        properties = props;

        // set default system look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
            LOG.warn("could net set system default look and feel", ex);
        }
        String mapProps = properties.getProperty(PropertyMapProperties,
                                                 "file:data/openmap.properties");

        PropertyHandler mapPropertyHandler = null;
        try {
            //mapPropertyHandler = new PropertyHandler.Builder().setPropertiesFile((String) null).setPropertyPrefix("main").build();
            PropertyHandler.Builder builder = new PropertyHandler.Builder();
            builder.setPropertiesFile(mapProps);
            //builder.setPropertyPrefix("main");
            mapPropertyHandler = builder.build();
        } catch (MalformedURLException murle) {
            LOG.warn(murle.getMessage(), murle);
        } catch (IOException ioe) {
            LOG.warn(ioe.getMessage(), ioe);
        }
        if (mapPropertyHandler == null) {
            mapPropertyHandler = new PropertyHandler();
        }

        // search event archive directory
        eventArchive = new EventArchive();

        // regular updates of ongoing event
        eventTimeScheduler = new EventTimeScheduler();

        // scenario scheduler (sequence of event updates)
        eventFileScheduler = new EventFileScheduler();
        eventFileScheduler.addQMLListener(this);

        // play countdown prior to S-wave arrival
        eventCountdown = new EventCountdown();

        // messaging
        messaging = new Messaging();

        // read targets and stations // read targets and stations
        stations = new HashMap();
        for (POI station : readPOIs(properties.getProperty(PropertyStationFile,
                                                           "data/stations.csv"))) {
            stations.put(station.name, station);
        }
        targets = readPOIs(properties.getProperty(PropertyTargetFile,
                                                  "data/targets.csv"));

        controlPeriod = getProperty(PropertyControlPeriod, (Double) null);
        periods = getProperty(PropertySpecPeriods, new double[0]);
        useFrequencies = getProperty(PropertyUseFrequencies, false);

        Arrays.sort(periods);

        minMag = getProperty(PropertyFilterMinMag, (Float) null);
        minLikelihood = getProperty(PropertyFilterMinLikelihood, (Float) null);

        // read spectrum parameter
        String param = properties.getProperty(Application.PropertySpecParameter);
        if (param != null) {
            spectrumParameter = Shaking.Type.valueOf(param);
            if (spectrumParameter == null) {
                LOG.warn("invalid " + Application.PropertySpecParameter + " value: " + param);
            }
        }

        // read shake map parameter
        param = properties.getProperty(Application.PropertySMParameter);
        if (param != null) {
            shakeMapParameter = Shaking.Type.valueOf(param);
            if (shakeMapParameter == null) {
                LOG.warn("invalid " + Application.PropertySMParameter + " value: " + param);
            }
        }

        shakeMapLayer = new ShakeMapLayer();
        shakeMapLayer.setName("Shake Map");

        shakingCalculator = new ShakingCalculator(targets, stations, shakeMapLayer);
        title = mapPropertyHandler.getProperties().getProperty("openmap.Title");

        configureMapPanel(mapPropertyHandler);

        // Schedule a job for the event-dispatching thread:
        // creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(
                new Runnable() {
                    @Override
                    public void run() {
                        showInFrame();
                    }
                }
        );

        messaging.listen();
    }

    public Double getControlPeriod() {
        return controlPeriod;
    }

    public double[] getPeriods() {
        return periods;
    }

    public boolean isUseFrequencies() {
        return useFrequencies;
    }

    public Shaking.Type getSpectrumParameter() {
        return spectrumParameter;
    }

    public Shaking.Type getShakeMapParameter() {
        return shakeMapParameter;
    }

    public static final Application getInstance() {
        return instance;
    }

    public Properties getProperties() {
        return properties;
    }

    public String getProperty(String key, String def) {
        return properties.getProperty(key, def);
    }

    public final boolean getProperty(String key, boolean def) {
        String value = properties.getProperty(key);
        if (value != null) {
            try {
                return Boolean.parseBoolean(value);
            } catch (NumberFormatException nfe) {
                LOG.warn(String.format("invalid boolean found in property: %s",
                                       key));
            }
        }
        return def;
    }

    public final int getProperty(String key, int def) {
        String value = properties.getProperty(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException nfe) {
                LOG.warn(String.format("invalid integer found in property: %s",
                                       key));
            }
        }
        return def;
    }

    public final double getProperty(String key, double def) {
        String value = properties.getProperty(key);
        if (value != null) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException nfe) {
                LOG.warn(String.format("invalid double found in property: %s",
                                       key));
            }
        }
        return def;
    }

    public final double[] getProperty(String key, double[] def) {
        String value = properties.getProperty(key);
        if (value != null) {
            String values[] = value.split(",");
            try {
                double[] retn = new double[values.length];
                for (int i = 0; i < values.length; ++i) {
                    retn[i] = Double.parseDouble(values[i]);
                }
                return retn;
            } catch (NumberFormatException nfe) {
                LOG.warn(String.format("invalid double found in property: %s",
                                       key));
            }
        }
        return def;
    }

    public final Double getProperty(String key, Double def) {
        String value = properties.getProperty(key);
        if (value != null) {
            try {
                return Double.valueOf(value);
            } catch (NumberFormatException nfe) {
                LOG.warn(String.format("invalid double found in property: %s",
                                       key));
            }
        }
        return def;
    }

    public final Float getProperty(String key, Float def) {
        String value = properties.getProperty(key);
        if (value != null) {
            try {
                return Float.valueOf(value);
            } catch (NumberFormatException nfe) {
                LOG.warn(String.format("invalid double found in property: %s",
                                       key));
            }
        }
        return def;
    }

    public EventArchive getEventArchive() {
        return eventArchive;
    }

    public EventFileScheduler getEventFileScheduler() {
        return eventFileScheduler;
    }

    /**
     * A method that lets you control what gets added to the application
     * programmatically. These components are required for handling an
     * OMEventHandler, which would be added to the MapHandler. If you wanted to
     * use the standard OpenMap application, you could add these components to
     * the MapHandler, instead.
     *
     * @param propertyHandler no description
     */
    protected void configureMapPanel(PropertyHandler propertyHandler) {
        OverlayMapPanel overlayMapPanel = new OverlayMapPanel(propertyHandler, true);
        overlayMapPanel.create();
        this.mapPanel = overlayMapPanel;

        // initialize map components
        MapHandler mapHandler = mapPanel.getMapHandler();

        MainPanel mainPanel = new MainPanel();
        mapHandler.add(mainPanel);

        eventPanel = new EventPanel(targets);

        ToolPanel toolPanel = (ToolPanel) mapHandler.get(ToolPanel.class);
        if (toolPanel == null) {
            toolPanel = new ToolPanel();
            mapHandler.add(toolPanel);
        }

        mapHandler.add(eventPanel);

        mainPanel.getSlider().setLeftComponent(eventPanel);

        LayerHandler layerHandler = (LayerHandler) mapHandler.get(LayerHandler.class);
        if (layerHandler != null) {
            LogoLayer logoLayer = new LogoLayer();
            logoLayer.setName("Logo");
            layerHandler.addLayer(logoLayer, 0);

            if (!shakeMapLayer.getPoints().isEmpty()) {
                layerHandler.addLayer(shakeMapLayer, 0);
            }

            stationLayer = new StationLayer(stations);
            stationLayer.setName("Stations");
            layerHandler.addLayer(stationLayer, 0);

            TargetLayer targetLayer = new TargetLayer(targets);
            targetLayer.setName("Targets");
            layerHandler.addLayer(targetLayer, 0);

            eventLayer = new EventLayer();
            eventLayer.setName("Event");
            layerHandler.addLayer(eventLayer, 0);
        }

        eventTimeScheduler.addUpdateListener(stationLayer);
        eventTimeScheduler.addUpdateListener(eventLayer);
        eventTimeScheduler.addUpdateListener(eventPanel);
    }

    public void setTarget(POI target) {
        if (eventLayer != null) {
            eventLayer.setTarget(target);
        }
        eventCountdown.setTarget(target);
    }

    protected void showInFrame() {
        openMapFrame = (OpenMapFrame) mapPanel.getMapHandler().get(OpenMapFrame.class);

        if (openMapFrame == null) {
            openMapFrame = new OpenMapFrame() {
                @Override
                public void considerForContent(Object someObj) {
                    if (someObj instanceof MainPanel) {
                        setContent((Component) someObj);
                    }

                    if (someObj instanceof MapPanel) {
                        JMenuBar menuBar = ((MapPanel) someObj).getMapMenuBar();
                        if (menuBar != null) {
                            getRootPane().setJMenuBar(menuBar);
                            addCustomMenuItems(menuBar);
                        }
                    }
                }
            };
            mapPanel.getMapHandler().add(openMapFrame);
            //openMapFrame.setTitle(title);
        }

        openMapFrame.addWindowListener(
                new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        if (messaging != null) {
                            messaging.close();
                        }
                        System.exit(0);
                    }
                }
        );

        setConnectionState("offline");
        openMapFrame.setVisible(true);
        mapPanel.getMapBean().showLayerPalettes();
        Debug.message("basic", "OpenMap: READY");
    }

    private void addCustomMenuItems(JMenuBar menuBar) {
        JMenu fileMenu = null;
        JMenu helpMenu = null;
        for (int i = 0; i < menuBar.getMenuCount(); ++i) {
            if (menuBar.getMenu(i).getClass() == FileMenu.class) {
                fileMenu = menuBar.getMenu(i);
                // remove about menu item
                for (int j = 0; j < fileMenu.getItemCount(); ++j) {
                    if (fileMenu.getItem(j).getClass() == AboutMenuItem.class) {
                        fileMenu.remove(j);
                        break;
                    }
                }
            } else if (menuBar.getMenu(i).getClass() == DefaultHelpMenu.class) {
                helpMenu = menuBar.getMenu(i);
            }
        }
        if (fileMenu == null) {
            fileMenu = new JMenu("File");
            fileMenu.setMnemonic('F');
        }
        if (helpMenu == null) {
            helpMenu = new JMenu("Help");
            helpMenu.setMnemonic('H');
        }

        JMenuItem eventBrowserMI = new JMenuItem("Event Browser");
        eventBrowserMI.setMnemonic('B');
        eventBrowserMI.addActionListener(this);
        eventBrowserMI.setActionCommand(ActionEventBrowser);
        eventBrowserMI.setEnabled(eventArchive != null);
        fileMenu.add(eventBrowserMI, 0);

        JMenuItem aboutMI = new JMenuItem("About");
        aboutMI.setMnemonic('A');
        aboutMI.addActionListener(this);
        aboutMI.setActionCommand(ActionAbout);
        helpMenu.add(aboutMI, 0);
    }

    private List<POI> readPOIs(String fileName) {
        List<POI> pois = new ArrayList();
        BufferedReader br;
        try {
            br = new BufferedReader(new FileReader(fileName));
            String line;
            String[] parts;
            while ((line = br.readLine()) != null) {
                // trim line and remove comments
                line = line.trim();
                int commentIdx = line.indexOf('#');
                if (commentIdx == 0) {
                    continue;
                } else if (commentIdx > 0) {
                    line = line.substring(0, commentIdx);
                }

                parts = line.split(",", 5);
                if (parts.length != 5) {
                    continue;
                }
                pois.add(new POI(
                        Double.parseDouble(parts[2]), // latitude
                        Double.parseDouble(parts[1]), // longitude
                        Double.parseDouble(parts[3]), // altitude
                        Double.parseDouble(parts[4]), // amplification
                        parts[0] // name
                ));

            }
            br.close();
        } catch (IOException ioe) {
            LOG.error(String.format("could not read POI file '%s'", fileName), ioe);
        }
        return pois;
    }

    @Override
    public EventData processQML(EventParameters eventParameters, long offset) {
        LOG.info("received event update");
        if (eventParameters == null) {
            return null;
        }

        EventData event;
        try {
            event = new EventData(eventParameters, offset, stations);
        } catch (EventData.InvalidEventDataException ex) {
            LOG.warn(ex.getMessage());
            return null;
        }

        boolean disable = true;
        if (event.isFakeEvent) {
            LOG.info(String.format("event %s marked as not existing", event.eventID));
        } else if (minMag != null && event.magnitude < minMag) {
            LOG.info(String.format("ignoring event %s, magnitude of %.1f below threshold",
                                   event.eventID, event.magnitude));
        } else if (minLikelihood != null && event.likelihood == null) {
            LOG.info(String.format("ignoring event %s, likelihood not available",
                                   event.eventID));
        } else if (minLikelihood != null && event.likelihood < minLikelihood) {
            LOG.info(String.format("ignoring event %s, likelihood of %.1f below threshold",
                                   event.eventID, event.likelihood));
        } else {
            disable = false;

            // zoom out if epicenter is not visible
            zoomToPoint(event.latitude, event.longitude);

            // process shaking
            shakingCalculator.processEvent(event);
            LOG.trace(event.toString());
        }

        eventTimeScheduler.setEvent(event, disable);
        eventCountdown.setEvent(disable ? null : event);
        return event;
    }

    private void zoomToPoint(double latitude, double longitude) {
        MapBean mapBean = mapPanel.getMapBean();
        Projection proj = mapBean.getProjection();
        Point eventXY = (Point) proj.forward(latitude, longitude, new Point());
        if (mapBean.contains(eventXY)) {
            return;
        }

        int dx = Math.abs(eventXY.x - mapBean.getWidth() / 2);
        int dy = Math.abs(eventXY.y - mapBean.getHeight() / 2);
        float scale = proj.getScale();
        float newScale = Math.max(dx * scale / mapBean.getWidth(),
                                  dy * scale / mapBean.getHeight()) * 2.2f;
        if (newScale > proj.getScale()) {
            mapBean.setScale(newScale);
        }
    }

    public void toFront() {
//        if (openMapFrame != null) {
//            LOG.debug("TOFRONT1");
//            EventQueue.invokeLater(new Runnable() {
//                private final WindowListener l = new WindowAdapter() {
//                    @Override
//                    public void windowDeiconified(WindowEvent e) {
//                        // Window now deiconified so bring it to the front.
//                        bringToFront();
//
//                        // Remove "one-shot" WindowListener to prevent memory leak.
//                        openMapFrame.removeWindowListener(this);
//                    }
//                };
//
//                @Override
//                public void run() {
//                    if (openMapFrame.getExtendedState() == JFrame.ICONIFIED) {
//                        // Add listener and await callback once window has been deiconified.
//                        openMapFrame.addWindowListener(l);
//                        openMapFrame.setExtendedState(JFrame.NORMAL);
//                    } else {
//                        // Bring to front synchronously.
//                        bringToFront();
//                    }
//                }
//
//                private void bringToFront() {
//                    openMapFrame.getGlassPane().setVisible(!openMapFrame.getGlassPane().isVisible());
//                    openMapFrame.toFront();
//                    openMapFrame.repaint();
//                }
//            });
//        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals(ActionEventBrowser)) {
            new EventBrowser(openMapFrame, true).setVisible(true);
        }
        if (e.getActionCommand().equals(ActionAbout)) {
            String message = "<html>"
                             + "The first public release of the Earthquake Early Warning Display (EEWD) for Europe is a product of REAKT,<br>"
                             + "Strategies and Tools for Real-Time Earthquake Risk Reduction,<br>"
                             + "FP7/2007-2013, contract no. 282862; <a href=\"http://www.reaktproject.eu\">www.reaktproject.eu</a>.<br><br>"
                             + "Version %s<br><br>"
                             + "Copyright (C) by<br>"
                             + "<ul>"
                             + "<li>Swiss Seismological Service (SED) at ETH Zurich;</li>"
                             + "<li>RISSC-Lab, the Seismological laboratory of the Department of Physics, University of Naples Federico II.</li>"
                             + "</ul>"
                             + "Developed by <a href=\"http://www.gempa.de\">gempa GmbH</a>.<br><br>"
                             + "License: http://www.gnu.org/licenses/gpl.html GPL version 2 or higher<br><br>"
                             + "Please cite as:<br></br>"
                             + "Cauzzi C, Behr Y, Clinton J, Kaestli P, Elia L, Zollo A and Herrnkind S (2015) Basic specifications for an Earthquake Early Warning Display (EEWD) for Europe.<br>"
                             + "Available at <a href=\"http://www.reaktproject.eu\">www.reaktproject.eu</a> -> WP7 or WP4 deliverables -> Earthquake Early Warning Display (EEWD)"
                             + "</html>";
            String version = null;

            Package p = Application.class.getPackage();
            if (p != null) {
                version = p.getImplementationVersion();
            }

            message = String.format(message, version == null ? "unknown" : version);
            final JEditorPane pane = new JEditorPane("text/html", message);

            pane.setEditable(false);
            pane.addHyperlinkListener(new HyperlinkListener() {

                @Override
                public void hyperlinkUpdate(HyperlinkEvent e) {
                    if (HyperlinkEvent.EventType.ACTIVATED == e.getEventType()) {
                        try {
                            Desktop.getDesktop().browse(e.getURL().toURI());
                        } catch (URISyntaxException | IOException ex) {
                            LOG.error("could not open URL", ex);
                        }
                    }
                }
            });

            JOptionPane.showMessageDialog(mapPanel, pane,
                                          "Earthquake Early Warning Display",
                                          JOptionPane.PLAIN_MESSAGE);
        }
    }

    public void setConnectionState(String state) {
        if (openMapFrame != null) {
            openMapFrame.setTitle(title + " [ " + state + " ]");
        }
    }

    public void updateHeartbeat(String hb) {
        InformationDelegator infoDelegator = (InformationDelegator) mapPanel.getMapHandler().get(InformationDelegator.class);
        infoDelegator.displayInfoLine(hb);
    }

    /**
     * The main OpenMap application.
     *
     * @param args no description
     */
    static public void main(String args[]) {
        System.setProperty("user.timezone", "UTC");
        Locale.setDefault(Locale.ROOT);

        Debug.init();

        // read property location from command line parameter
        ArgParser ap = new ArgParser("EEWD");
        ap.add("properties",
               "A resource, file path or URL to properties file\n Ex: http://myhost.com/xyz.props or file:/myhome/abc.pro\n See Java Documentation for java.net.URL class for more details",
               1);
        ap.parse(args);

        String propURL = "file:eewd.properties";
        String[] arg = ap.getArgValues("properties");
        if (arg != null) {
            propURL = arg[0];
        }

        Properties props = null;
        try {
            URL url = new URL(propURL);
            InputStream in = url.openStream();
            props = new Properties();
            props.load(in);
        } catch (MalformedURLException mue) {
            LOG.fatal("invalid property file location", mue);
        } catch (IOException ioe) {
            LOG.fatal("could not read property file", ioe);
        }
        if (props == null) {
            System.exit(1);
        }

        Application app = new Application(props);
    }

}
