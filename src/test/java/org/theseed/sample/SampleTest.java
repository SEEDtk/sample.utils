/**
 *
 */
package org.theseed.sample;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.theseed.genome.Genome;
import org.theseed.sample.AnnotatedSample.Exception;

/**
 * @author Bruce Parrello
 *
 */
public class SampleTest {

    @Test
    public void test() throws IOException, Exception {
        File sampleDir = new File("data", "RTest");
        AnnotatedSample imported = AnnotatedSample.convert(sampleDir);
        assertThat(imported.getName(), equalTo("RTest"));
        assertThat(imported.getSource(), equalTo("NCBI"));
        assertThat(imported.getSite(), equalTo("hot_springs"));
        Genome g1 = imported.getGenome("1773.22803");
        assertThat(g1.getName(), equalTo("Mycobacterium tuberculosis clonal population"));
        List<Genome> genomes = imported.getAll();
        assertThat(genomes.size(), equalTo(2));
        File dir = new File("data");
        File sampleFile = new File(dir, AnnotatedSample.defaultName("RTest"));
        assertThat(imported.getFileName(dir), equalTo(sampleFile));
        assertThat(AnnotatedSample.defaultName("SRS1234"), equalTo("SRS1234.sample.gz"));
        File testOutput = new File("data", "sample.ser");
        imported.save(testOutput);
        AnnotatedSample loaded = new AnnotatedSample(testOutput);
        assertThat(loaded.getName(), equalTo("RTest"));
        assertThat(loaded.getSource(), equalTo("NCBI"));
        assertThat(loaded.getSite(), equalTo("hot_springs"));
        g1 = loaded.getGenome("1773.22803");
        assertThat(g1.getName(), equalTo("Mycobacterium tuberculosis clonal population"));
        genomes = loaded.getAll();
        assertThat(genomes.size(), equalTo(2));

    }

}
