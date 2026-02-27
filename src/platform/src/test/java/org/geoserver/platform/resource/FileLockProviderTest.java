/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform.resource;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.io.File;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * The file lock provider is actually not reentrant (not possible with Java) but the keys for meta-tile caching are
 * different, so we don't really need it to handle nested locks on the same key.
 */
public class FileLockProviderTest {

    /** Small timeout for testing failure cases quickly */
    private static final int TEST_TIMEOUT_SECONDS = 1;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private FileLockProvider provider;
    private File root;

    @Before
    public void setUp() throws Exception {
        root = tempFolder.newFolder("lockRoot");
        provider = new FileLockProvider(root);
    }

    @Test
    public void testInterruptionDuringAcquisition() throws Exception {
        String key = "interrupt-key";

        // Mock a situation where the lock is already held
        Resource.Lock firstLock = provider.acquire(key);

        Thread testThread = new Thread(() -> {
            try {
                provider.acquire(key);
                fail("Should have been interrupted");
            } catch (Exception e) {
                // Expected
            }
        });

        testThread.start();
        Thread.sleep(200); // Let it enter the loop
        testThread.interrupt();

        testThread.join(2000);
        assertFalse("Thread should have terminated after interruption", testThread.isAlive());

        firstLock.release();
    }

    private File getLockFile(String key) {
        // This mimics the internal logic of FileLockProvider to verify disk state
        String hash = org.apache.commons.codec.digest.DigestUtils.sha256Hex(key);
        return new File(new File(root, "filelocks"), hash + ".lock");
    }
}
