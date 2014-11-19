package ws.finson.wifix.app;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nu.xom.Attribute;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;
import nu.xom.Node;
import nu.xom.Nodes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.tuxi.lib.cfg.ApplicationComponent;
import ws.tuxi.lib.cfg.ConfigurationException;
import ws.tuxi.lib.pipeline.PipelineOperation;
import ws.tuxi.lib.pipeline.PipelineOperationException;

/**
 * This BuildSensorBranch reads a DAP scan sequence branch and redistributes the data into a sensor
 * sequence branch. Capture scan data is structured scan by scan, whereas sensor data is structured
 * sensor by sensor.
 * 
 * @author Doug Johnson, Nov 14, 2014
 * 
 */
public class BuildSensorBranch implements PipelineOperation<Document, Document> {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private String primaryKeyName = null;
    private final List<String> sensorNames = new ArrayList<>();

    /**
     * @param ac
     *            the containing ApplicationComponent
     * @param cE
     *            the Element from the config file that defines this object
     * @throws IOException
     * @throws ConfigurationException
     */
    public BuildSensorBranch(ApplicationComponent ac, Element cE) throws IOException,
            ConfigurationException {

        // Process each of the configuration sections

        Elements sectionElements = cE.getChildElements();
        for (int idx = 0; idx < sectionElements.size(); idx++) {
            Element sectionElement = sectionElements.get(idx);
            logger.debug("Begin section element <{}>", sectionElement.getLocalName());
            if ("primary-key".equals(sectionElement.getLocalName())) {
                if (primaryKeyName != null) {
                    logger.warn("Ignoring extra <{}> definition, only one is allowed.",
                            sectionElement.getLocalName());
                } else {
                    primaryKeyName = sectionElement.getValue();
                }
            } else if ("sensor".equals(sectionElement.getLocalName())) {
                sensorNames.add(sectionElement.getValue());
            } else {
                logger.warn("Skipping <{}> element. Element not recognized.",
                        sectionElement.getLocalName());
            }
        }
        if (primaryKeyName == null) {
            throw new ConfigurationException("Name of the primary (unique) key must be specified.");
        }
        if (sensorNames.size() == 0) {
            throw new ConfigurationException("Name of at least one sensor must be specified.");
        }
    }

    /**
     * @see ws.tuxi.lib.pipeline.PipelineOperation#doStep(java.lang.Object)
     */
    @Override
    public Document doStep(Document in) throws PipelineOperationException {

        // 1. What are the dimensions of the data tables?

        // collect all the primary key value nodes in the scan data

        Element scanSequenceElement = in.getRootElement().getFirstChildElement("scan-sequence");
        Nodes result = scanSequenceElement.query("scan/scan-values[@field='" + primaryKeyName
                + "']/value");
        logger.debug("Total primary key ({}) occurences: {}", primaryKeyName, result.size());

        // and eliminate the duplicates

        Set<String> keySet = new HashSet<>(result.size());
        for (int idx = 0; idx < result.size(); idx++) {
            String id = result.get(idx).getValue();
            keySet.add(id);
        }
        logger.debug("Unique primary key values: {}", keySet.size());

        // How many scans?

        Nodes scanNodes = scanSequenceElement.query("scan");
        logger.debug("scan count: {}", scanNodes.size());

        // For the timetag data, create a String value array and fill it in

        String[] timeValues = new String[scanNodes.size()];
        for (int idx = 0; idx < scanNodes.size(); idx++) {
            Nodes timeValueNodes = scanNodes.get(idx).query("scan-values[@field='timetag']/value");
            if ((timeValueNodes.size() != 1)) {
                String ordinal = ((Element)(scanNodes.get(idx))).getAttributeValue("ordinal");
                if (ordinal == null) {
                    ordinal = "??";
                }
                throw new PipelineOperationException("Unexpected XML structure in scan "+ordinal+".  One and only one timetag value element allowed.");
            } else {
                timeValues[idx] = timeValueNodes.get(0).getValue();
            }
        }
        
        // write the timetag-values element to the sensor-sequence branch

        Element f = new Element("timetag-values");
        for (int scanIndex = 0; scanIndex < timeValues.length; scanIndex++) {
            Element ve = new Element("value");
            ve.appendChild(timeValues[scanIndex]);
            f.appendChild(ve);
        }
        
        Element sensorSequenceElement = new Element("sensor-sequence");
        sensorSequenceElement.appendChild(f);

        // Repeat the following steps for each requested sensor

        // 2. Prepare the empty data structures to receive the collated data

        for (String sensorName : sensorNames) {

            // Prepare the (scan_count) x (key_count) Map matrix

            Map<String, String[]> matrix = new HashMap<>(keySet.size());

            // For each primary key value, put a 0-filled sensor value array in the Map

            Iterator<String> iter = keySet.iterator();
            while (iter.hasNext()) {
                String[] vals = new String[scanNodes.size()];
                Arrays.fill(vals, "0");
                matrix.put(iter.next(), vals);
            }

            // 3. Re-organize the values from the scan element groups into the sensor arrays in the
            // map

            for (int scanIndex = 0; scanIndex < scanNodes.size(); scanIndex++) {

                // Store data values from the scan into the sensor arrays in the Map

                Nodes keyValueNodes = scanNodes.get(scanIndex).query(
                        "scan-values[@field='" + primaryKeyName + "']/value");
                Nodes sensorValueNodes = scanNodes.get(scanIndex).query(
                        "scan-values[@field='" + sensorName + "']/value");

                logger.trace("primary key count: {}", keyValueNodes.size());
                logger.trace("sensor '{}' value count: {}", sensorName,
                        sensorValueNodes.size());

                assert (keyValueNodes.size() == sensorValueNodes.size()) : "key count and value count must be equal.";

                for (int sensorIndex = 0; sensorIndex < sensorValueNodes.size(); sensorIndex++) {
                    logger.trace("{} {} {}", sensorIndex,
                            keyValueNodes.get(sensorIndex).getValue(),
                            sensorValueNodes.get(sensorIndex).getValue());
                    String[] valueArray = matrix.get(keyValueNodes.get(sensorIndex).getValue());
                    valueArray[scanIndex] = sensorValueNodes.get(sensorIndex).getValue();
                }
            }

            // 4. Model the new data structures in an XML tree branch.

            // write the sensor arrays from the map to the sensor branch

            Element sensorElement = new Element("sensor");
            Attribute m = new Attribute("label", sensorName);
            sensorElement.addAttribute(m);

            for (String key : matrix.keySet()) {
                f = new Element("sensor-values");
                m = new Attribute("label", key.replace(':', '-'));
                f.addAttribute(m);
                String[] sensorValues = matrix.get(key);
                for (int scanIndex = 0; scanIndex < sensorValues.length; scanIndex++) {
                    Element ve = new Element("value");
                    ve.appendChild(sensorValues[scanIndex]);
                    f.appendChild(ve);
                }
                sensorElement.appendChild(f);
            }

            // attach the new sensor twig to the sensor-sequence branch

            sensorSequenceElement.appendChild(sensorElement);
        }

        // 5. Add the new sensor-sequence branch to the existing XML tree

        in.getRootElement().appendChild(sensorSequenceElement);
        return in;
    }

}
