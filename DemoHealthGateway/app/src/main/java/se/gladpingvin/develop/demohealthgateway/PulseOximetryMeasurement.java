package se.gladpingvin.develop.demohealthgateway;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Random;

/**
 * Representation of an IEEE 11073 Pulse oximetry measurement object
 * Created by SEprjASv on 2015-10-22.
 */
public class PulseOximetryMeasurement {
    private static final int MDC_PULS_OXIM_PULS_RATE = 18458;
    private static final int MDC_PULS_OXIM_PLETH = 19380;
    private static final int MDC_PULS_OXIM_SAT_O2 = 19384;

    /**
     * Getter method for heart rate
     * @return the heart rate of the measurement as a floating point number
     */
    public float getHeartRate() {
        return heartRate;
    }

    /**
     * Getter method for blood oxygen saturation
     * @return the blood oxygen saturation of the measurement as a floating point number
     */
    public float getBloodOxygenSaturation() {
        return bloodOxygenSaturation;
    }

    /**
     * Getter method for heart rate unit
     * @return the unit of the heart rate value as a String
     */
    public String getHeartRateUnit() {
        return heartRateUnit;
    }

    /**
     * Getter method for blood oxygen saturation unit
     * @return the unit of the blood oxygen saturation value as a String
     */
    public String getBloodOxygenSaturationUnit() {
        return bloodOxygenSaturationUnit;
    }

    /**
     * Getter method for timestamp
     * @return the timestamp of the measurement as a GregorianCalendar
     */
    public GregorianCalendar getTimeStamp() {
        return timeStamp;
    }

    /**
     * Getter method for patient
     * @return an identifier of the patient for whom the measurement was taken
     */
    public String getPatient() {
        return patient;
    }

    private float heartRate;
    private float bloodOxygenSaturation;
    private String heartRateUnit;
    private String bloodOxygenSaturationUnit;
    private GregorianCalendar timeStamp;
    private String patient;

    /**
     * Constructor for creating a pseudo-random PulseOximetryMeasurement, this constructor is
     * intended for generating test data instead of having to record lots of measurement data
     * manually.
     * The timestamp will vary between 2005-01-01 00:00:00 to last_year-12-31 23:59:59
     * The heart rate will vary between 40 - 69
     * The unit of the heart rate will always be "bpm"
     * The blood oxygen saturation will vary between 90 - 99
     * The unit of the blood oxygen saturation will always be "%"
     * The patient identifier will be the hexadecimal representation of a decimal number between
     * 0 - 9999 (i.e. a hexadecimal number between 0 - 270F)
     */
    public PulseOximetryMeasurement() {
        Random generator = new Random();
        int startYear = 2005;
        GregorianCalendar now = new GregorianCalendar();

        int year = startYear + generator.nextInt(now.get(Calendar.YEAR)-startYear);
        int month = generator.nextInt(12);
        int day = 1 + generator.nextInt(31);
        int hour = generator.nextInt(24);
        int minute = generator.nextInt(60);
        int second = generator.nextInt(60);

        heartRate = (float)(40 + generator.nextInt(30));
        heartRateUnit = "bpm";
        bloodOxygenSaturation = (float)(90 + generator.nextInt(10));
        bloodOxygenSaturationUnit = "%";
        patient = Integer.toHexString(generator.nextInt(10000));
        timeStamp = new GregorianCalendar(year, month, day, hour, minute, second);
    }

    /**
     * This constructor is for when all data of a measurement are known
     * @param rate the heart rate
     * @param rateUnit the unit of the heart rate
     * @param saturation the blood oxygen saturation
     * @param saturationUnit the unit of the blood oxygen saturation
     * @param measureTime the time when the measurement was taken
     * @param pat the identifier of the patient
     */
    public PulseOximetryMeasurement(float rate, String rateUnit, float saturation,
                                    String saturationUnit, GregorianCalendar measureTime,
                                    String pat) {
        heartRate = rate;
        heartRateUnit = rateUnit;
        bloodOxygenSaturation = saturation;
        bloodOxygenSaturationUnit = saturationUnit;
        timeStamp = measureTime;
        patient = pat;
    }

    /**
     * This constructor is for when only measurement data but the time of measuring are known, the
     * constructor assumes that it's called upon receiving a fresh measurement from a PHD, hence it
     * sets the timestamp to the creation time of the object.
     * @param rate the heart rate
     * @param rateUnit the unit of the heart rate
     * @param saturation the blood oxygen saturation
     * @param saturationUnit the unit of the blood oxygen saturation
     * @param patient the identifier of the patient
     */
    public PulseOximetryMeasurement(float rate, String rateUnit, float saturation,
                                    String saturationUnit, String patient) {
        this(rate, rateUnit, saturation, saturationUnit, new GregorianCalendar(), patient);
    }

    /**
     * This constructor converts a JSON representation of a PulseOximetryMeasurement object into a
     * real PulseOximetryMeasurement object
     * @param json the JSON representation of the PulseOximetryMeasurement
     * @throws JSONException when the conversion from JSON throws a JSONException, i.e. when json
     * isn't a full representation of a PulseOximetryMeasurement
     */
    public PulseOximetryMeasurement(JSONObject json) throws JSONException{
        heartRate = (float)json.getDouble("HeartRate");
        heartRateUnit = json.getString("HeartRateUnit");
        bloodOxygenSaturation = (float)json.getDouble("BloodOxygenSaturation");
        bloodOxygenSaturationUnit = json.getString("BloodOxygenSaturationUnit");
        timeStamp = AntidoteHelper.htmlStringToGregorianCalendar(json.getString("TimeStamp"));
        patient = json.getString("PatientIdentification");
    }

    @Override
    public String toString() {
        return timeStamp.get(GregorianCalendar.DAY_OF_MONTH) + "/" + (timeStamp.get(
                GregorianCalendar.MONTH) + 1) + " - " + timeStamp.get(GregorianCalendar.YEAR) + " "
                + patient + ":\n\t" +  "Heart rate: " + (int)heartRate + " " + heartRateUnit +
                "\n\t" + "SPO2: " + (int)bloodOxygenSaturation + " " + bloodOxygenSaturationUnit;
    }

    /**
     * Method for creating a PulseOximetryMeasurement from an xml Document, it's intended to be used
     * when receiving measurement data from Antidote, as it returns measurements in xml format by
     * default.
     * @param document the xml Document containing the measurement data
     * @param patient the identifier of the patient from whom the measurement was taken
     * @return a PulseOximetryMeasurement with the data from document, identified with patient
     */
    public static PulseOximetryMeasurement fromXml(Document document, String patient) {
        float heartRate = -1;
        float saturation = -1;
        String heartRateUnit = "unknown";
        String saturationUnit = "unknown";

        NodeList elements = document.getElementsByTagName("data-list");

        for (int i = 0; i < elements.getLength(); i++) {
            Node element = elements.item(i);

            NodeList entries = ((Element)element).getElementsByTagName("entry");

            for (int j = 0; j < entries.getLength(); j++) {
                Node entry = entries.item(j);

                NodeList entryChildren = entry.getChildNodes();
                float value = -1;
                int type = -1;
                String unit = "unknown";

                for (int k = 0; k < entryChildren.getLength(); k++) {
                    Node entryChild = entryChildren.item(k);

                    if (entryChild.getNodeName().equals("simple")) {
                        NodeList simple = ((Element)entryChild).getElementsByTagName("value");
                        if (simple.getLength() > 0) {
                            value = Float.parseFloat(AntidoteHelper.getXmlText(simple.item(0)));
                        }
                    } else if (entryChild.getNodeName().equals("meta-data")) {
                        NodeList metas = ((Element)entryChild).getElementsByTagName("meta");

                        for (int l = 0; l < metas.getLength(); l++) {
                            NamedNodeMap attributes = metas.item(l).getAttributes();
                            if (attributes == null) {
                                continue;
                            }

                            Node item = attributes.getNamedItem("name");
                            if (item == null) {
                                continue;
                            }

                            if (item.getNodeValue().equals("unit")) {
                                unit = AntidoteHelper.getXmlText(metas.item(l));
                            } else if (item.getNodeValue().equals("metric-id")) {
                                type = Integer.parseInt(AntidoteHelper.getXmlText(metas.item(l)));
                            }
                        }
                    }
                }

                if (type == MDC_PULS_OXIM_PULS_RATE) {
                    heartRate = value;
                    heartRateUnit = unit;
                } else if (type == MDC_PULS_OXIM_SAT_O2) {
                    saturation = value;
                    saturationUnit = unit;
                }
            }
        }

        elements.item(0);

        return new PulseOximetryMeasurement(heartRate, heartRateUnit, saturation, saturationUnit,
                patient);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof  PulseOximetryMeasurement)) {
            return false;
        }

        PulseOximetryMeasurement measurement = (PulseOximetryMeasurement)o;

        // The reason that the timestamps are only compared down to minute, is that the html type
        // datetime-local, which is used when posting measurements to the backend, only has minute
        // precision (also, two identical measurements withing the same minute doesn't really add
        // any value here, this might change with future development though)
        return bloodOxygenSaturation == measurement.getBloodOxygenSaturation() &&
                heartRate == measurement.getHeartRate() &&
                bloodOxygenSaturationUnit.equals(measurement.getBloodOxygenSaturationUnit()) &&
                heartRateUnit.equals(measurement.getHeartRateUnit()) &&
                patient.equals(measurement.getPatient()) &&
                AntidoteHelper.timeStampAsHtmlString(timeStamp).equals(
                        AntidoteHelper.timeStampAsHtmlString(measurement.getTimeStamp()));
    }
}
