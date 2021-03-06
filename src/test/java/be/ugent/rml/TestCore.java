package be.ugent.rml;

import be.ugent.rml.cli.Main;
import be.ugent.rml.functions.FunctionLoader;
import be.ugent.rml.records.RecordsFactory;
import be.ugent.rml.store.Quad;
import be.ugent.rml.store.QuadStore;
import be.ugent.rml.store.QuadStoreFactory;
import be.ugent.rml.term.NamedNode;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public abstract class TestCore {

    final Logger logger = LoggerFactory.getLogger(this.getClass());

    Executor createExecutor(String mapPath) throws Exception {
        return createExecutor(mapPath, new ArrayList<>());
    }

    /**
     * Create an executor and add extra quads to the mapping file.
     * @param mapPath The path to the mapping file.
     * @param extraQuads A list of extra quads that need to be added to the mapping file.
     * @return An executor.
     * @throws Exception
     */
    Executor createExecutor(String mapPath, List<Quad> extraQuads) throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        // execute mapping file
        URL url = classLoader.getResource(mapPath);

        if (url != null) {
            mapPath = url.getFile();
        }

        File mappingFile = new File(mapPath);
        QuadStore rmlStore = QuadStoreFactory.read(mappingFile);
        rmlStore.addQuads(extraQuads);

        return new Executor(rmlStore,
                new RecordsFactory(mappingFile.getParent()), Utils.getBaseDirectiveTurtle(mappingFile));
    }

    Executor createExecutor(String mapPath, FunctionLoader functionLoader) throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        // execute mapping file
        File mappingFile = new File(classLoader.getResource(mapPath).getFile());
        QuadStore rmlStore = QuadStoreFactory.read(mappingFile);

        return new Executor(rmlStore, new RecordsFactory(mappingFile.getParent()),
                functionLoader, Utils.getBaseDirectiveTurtle(mappingFile));
    }

    /**
     * Test function to compare output with expected files, using CLI interface
     */
    public void doMapping(Path cwd, String mappingFiles, String output, String expected) {
        Main.main(("-m " + mappingFiles + " -o " + output).split(" "), cwd.toString());
        try {
            try {
                compareFiles(
                    expected,
                    output,
                    false
                );
            } catch (Exception e) {
                e.printStackTrace();
                fail();
            }
        } finally {
            // remove output file
            try {
                File outputFile = Utils.getFile(output);
                assertTrue(outputFile.delete());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public Executor doMapping(String mapPath, String outPath) {
        try {
            Executor executor = this.createExecutor(mapPath);
            doMapping(executor, outPath);
            return executor;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            fail();
        }

        return null;
    }

    void doMapping(Executor executor, String outPath) {
        try {
            QuadStore result = executor.execute(null);
            result.removeDuplicates();
            compareStores(result, filePathToStore(outPath));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            fail();
        }
    }

    void doMappingExpectError(String mapPath) {
        ClassLoader classLoader = getClass().getClassLoader();

        // execute mapping file
        File mappingFile = new File(classLoader.getResource(mapPath).getFile());

        QuadStore rmlStore = null;
        try {
            rmlStore = QuadStoreFactory.read(mappingFile);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }

        try {
            Executor executor = new Executor(rmlStore, new RecordsFactory(mappingFile.getParent()), Utils.getBaseDirectiveTurtle(mappingFile));
            QuadStore result = executor.execute(null);
        } catch (Exception e) {
            // I expected you!
            logger.warn(e.getMessage(), e);
            return;
        }
        fail("Expecting error not found");
    }

    private void compareStores(QuadStore store1, QuadStore store2) {
        String string1 = store1.toSortedString();
        String string2 = store2.toSortedString();
        // First arg is expected, second is actual
        assertEquals(string1, string2);
    }

    void compareFiles(String path1, String path2, boolean removeTimestamps) {
        QuadStore store1 = null;
        QuadStore store2 = null;

        try {
            store1 = filePathToStore(path1);
            store2 = filePathToStore(path2);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }

        if (removeTimestamps) {
            store1.removeQuads(null, new NamedNode("http://www.w3.org/ns/prov#generatedAtTime"), null);
            store2.removeQuads(null, new NamedNode("http://www.w3.org/ns/prov#generatedAtTime"), null);
            store1.removeQuads(null, new NamedNode("http://www.w3.org/ns/prov#endedAtTime"), null);
            store2.removeQuads(null, new NamedNode("http://www.w3.org/ns/prov#endedAtTime"), null);
            store1.removeQuads(null, new NamedNode("http://www.w3.org/ns/prov#startedAtTime"), null);
            store2.removeQuads(null, new NamedNode("http://www.w3.org/ns/prov#startedAtTime"), null);
        }

        try {
            assertEquals(store1, store2);
        } catch (AssertionError e) {
            compareStores(store1, store2);
        }
    }

    private QuadStore filePathToStore(String path) throws Exception {
        // load output-turtle file
        File outputFile = null;
        try {
            outputFile = Utils.getFile(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        QuadStore store;

        if (path.endsWith(".nq")) {
            store = QuadStoreFactory.read(outputFile, RDFFormat.NQUADS);
        } else if (path.endsWith(".json")) {
            store = QuadStoreFactory.read(outputFile, RDFFormat.JSONLD);
        } else if (path.endsWith(".trig")) {
            store = QuadStoreFactory.read(outputFile, RDFFormat.TRIG);
        } else {
            store = QuadStoreFactory.read(outputFile);
        }

        return store;
    }
}
