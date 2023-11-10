/**
 *
 */
package org.theseed.sample.utils;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.basic.BaseProcessor;
import org.theseed.basic.ParseFailureException;
import org.theseed.sample.AnnotatedSample;

/**
 * This command imports samples from a binning output directory complex.  The positional parameter are
 * the name of the input directory (containing binning output, one directory per sample) and the name of
 * the output directory.  The output directory will contain GZIP files, each having the name
 * "XXXXXXXX.sample.gz", where "XXXXXXXX" is the sample ID.  When uncompressed, the GZIP file contains
 * JSON strings, one per line.  The first line will be a metadata hash (currently empty).  The remaining
 * lines will be genome type objects.
 *
 * The command-line options are as follows.
 *
 * -h	display command-line usage
 * -v	display more frequent log messages
 *
 * --missing	only copy samples not already in the output directory
 * --clear		erase the output directory before starting
 *
 * @author Bruce Parrello
 *
 */
public class SampleImportProcessor extends BaseProcessor {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(SampleImportProcessor.class);

    // COMMAND-LINE OPTIONS

    /** TRUE if only new samples should be copied */
    @Option(name = "--missing", usage = "if specified, only new samples will be copied")
    private boolean missingFlag;

    /** TRUE if the output directory should be erased before starting */
    @Option(name = "--clear", usage = "erase the output directory before copying")
    private boolean clearFlag;

    /** input directory */
    @Argument(index = 0, metaVar = "inDir", usage = "input directory containing binned samples in subdirectories")
    private File inDir;

    /** output directory */
    @Argument(index = 1, metaVar = "outDir", usage = "output directory to contain the annotated samples")
    private File outDir;

    @Override
    protected void setDefaults() {
        this.missingFlag = false;
        this.clearFlag = false;
    }

    @Override
    protected boolean validateParms() throws IOException, ParseFailureException {
        if (this.missingFlag && this.clearFlag)
            throw new ParseFailureException("--missing and --clear are mutually exclusive.");
        // Verify the input directory.
        if (! this.inDir.isDirectory())
            throw new FileNotFoundException("Input directory " + this.inDir + " is not found or invalid.");
        log.info("Copying samples from {}.", this.inDir);
        // Set up the output directory.
        if (! this.outDir.isDirectory()) {
            log.info("Creating output directory {}.", this.outDir);
            FileUtils.forceMkdir(this.outDir);
        } else if (this.clearFlag) {
            log.info("Erasing output directory {}.", this.outDir);
            FileUtils.cleanDirectory(this.outDir);
        } else
            log.info("Samples will be copied into {}.", this.outDir);
        return true;
    }

    /**
     * This is the file filter for finding completed sample directories.  The directory is complete if it
     * has an index.tbl file in the Eval subdirectory.
     */
    private static class SampleDirFilter implements FileFilter {

        @Override
        public boolean accept(File pathname) {
            boolean retVal = pathname.isDirectory();
            if (retVal) {
                File evalFile = new File(pathname, "Eval/index.tbl");
                retVal = evalFile.exists();
            }
            return retVal;
        }

    }

    @Override
    protected void runCommand() throws Exception {
        // Get all the incoming binned samples.
        File[] samples = this.inDir.listFiles(new SampleDirFilter());
        log.info("{} binned samples found in {}.", samples.length, this.inDir);
        // Loop through them.
        int count = 0;
        for (File sample : samples) {
            // Compute the target file name.
            String sampleName = sample.getName();
            File targetFile = new File(this.outDir, AnnotatedSample.defaultName(sampleName));
            count++;
            // Check to see if we already have the sample.
            if (this.missingFlag && targetFile.exists())
                log.info("Skipping sample {}: sample-- already exists.", sampleName);
            else {
                log.info("Loading sample {} ({} of {}).", sample, count, samples.length);
                try {
                    AnnotatedSample imported = AnnotatedSample.convert(sample);
                    log.info("Saving sample to {}.", targetFile);
                    imported.save(targetFile);
                } catch (AnnotatedSample.Exception e) {
                    log.error("Sample {} needs to be rerun: {}", sampleName, e.toString());
                }
            }
        }
        log.info("All done. {} samples checked.", count);
    }

}
