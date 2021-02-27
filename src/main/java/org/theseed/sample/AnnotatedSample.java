/**
 *
 */
package org.theseed.sample;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.genome.Genome;
import org.theseed.io.LineReader;

import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.JsonKey;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;

/**
 * This object represents an annotated sample.  The sample consists of a metadata map and a set of genomes.
 *
 * @author Bruce Parrello
 *
 */
public class AnnotatedSample {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(AnnotatedSample.class);
    /** metadata hash */
    private JsonObject metaData;
    /** map of genomes */
    private Map<String, Genome> gMap;

    /**
     * This is an exception for an invalid GTO.
     */
    public static class Exception extends java.lang.Exception {

        public Exception(String message) {
            super(message);
        }

        private static final long serialVersionUID = 3162966234847218684L;

    }

    /** This is the enum for the metadata keys */
    public static enum MetaKeys implements JsonKey {
        NAME("");

        private final Object m_value;

        MetaKeys(final Object value) {
            this.m_value = value;
        }

        /** This is the string used as a key in the incoming JsonObject map.
         */
        @Override
        public String getKey() {
            return this.name().toLowerCase();
        }

        /** This is the default value used when the key is not found.
         */
        @Override
        public Object getValue() {
            return this.m_value;
        }

    }


    /**
     * This is the file filter for finding binning GTO files in a binning output directory.
     */
    private static class BinFileFilter implements FilenameFilter {

        private static Pattern BIN_FILE_PATTERN = Pattern.compile("bin\\d+\\.gto");

        @Override
        public boolean accept(File dir, String name) {
            Matcher m = BIN_FILE_PATTERN.matcher(name);
            return m.matches();
        }

    }

    /**
     * Create a blank, empty annotated sample.
     */
    private AnnotatedSample() {
        this.metaData = new JsonObject();
        this.gMap = new HashMap<String, Genome>();
    }

    /**
     * Read an annotated sample from a file.
     *
     * @param file	sample file name
     * @throws IOException
     */
    public AnnotatedSample(File sampleFile) throws IOException {
        // Get a line reader on the file.
        try (FileInputStream inStream = new FileInputStream(sampleFile);
                GZIPInputStream zipStream = new GZIPInputStream(inStream);
                LineReader reader = new LineReader(zipStream)) {
            // The first line is the metadata.
            String metaString = reader.next();
            this.metaData = (JsonObject) Jsoner.deserialize(metaString);
            // Now read the genomes.
            this.gMap = new HashMap<String, Genome>();
            while (reader.hasNext()) {
                Genome genome = Genome.fromJson(reader.next());
                this.gMap.put(genome.getId(), genome);
            }
        } catch (JsonException e) {
            // Convert this to an IO exception.
            throw new IOException("Error in JSON string: " + e.getMessage());
        }
    }


    /**
     * Create an annotated sample from a binning output directory.
     *
     * @param sampleDir		binning output directory containing the sample
     *
     * @throws IOException					I/O error in files
     * @throws AnnotatedSample.Exception	invalid format in GTO
     */
    public static AnnotatedSample convert(File sampleDir) throws IOException, Exception {
        AnnotatedSample retVal = new AnnotatedSample();
        retVal.setName(sampleDir.getName());
        // Loop through the bins in the input directory.
        File[] binFiles = sampleDir.listFiles(new BinFileFilter());
        for (File binFile : binFiles) {
            try {
                Genome genome = new Genome(binFile);
                if (! genome.hasQuality())
                    throw new Exception("File " + binFile + " has no quality information.");
                retVal.gMap.put(genome.getId(), genome);
            } catch (ClassCastException e) {
                throw new Exception("File " + binFile + " is an invalid GTO.");
            }
        }
        return retVal;
    }

    /**
     * Specify the name of this sample.
     *
     * @param name	sample name
     */
    private void setName(String name) {
        this.metaData.put(MetaKeys.NAME.getKey(), name);
    }

    /**
     * Write an annotated sample to the specified output file.
     *
     * @param outFile	output file name
     *
     * @throws IOException
     */
    public void save(File outFile) throws IOException {
        // Set up to write the output file in GZip format.
        try (FileOutputStream outStream = new FileOutputStream(outFile);
                GZIPOutputStream zipStream = new GZIPOutputStream(outStream);
                PrintWriter writer = new PrintWriter(zipStream)) {
            // Write the metadata.
            writer.println(Jsoner.serialize(this.metaData));
            // Write the genomes.
            for (Genome genome : this.gMap.values())
                writer.println(genome.toJsonString());
        }
    }

    /**
     * @return the name of this sample
     */
    public String getName() {
        return this.metaData.getString(MetaKeys.NAME);
    }

    /**
     * @return the genome with the specified ID, or NULL if it does not exist
     *
     * @param id	ID of the desired genome
     */
    public Genome getGenome(String id) {
        return this.gMap.get(id);
    }

    /**
     * @return the default file name for this sample in the given target directory
     *
     * @param dir	target directory
     */
    public File getFileName(File dir) {
        return new File(dir, AnnotatedSample.defaultName(this.getName()));
    }

    /**
     * @return the default base file name for the sample with a given name
     *
     * @param name	sample name
     */
    public static String defaultName(String name) {
        return name + ".sample.gz";
    }

    /**
     * @return a list of all the genomes in the sample
     */
    public List<Genome> getAll() {
        List<Genome> retVal = new ArrayList<Genome>(this.gMap.values());
        return retVal;
    }

}
