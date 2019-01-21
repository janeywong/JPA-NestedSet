/**
 * LICENSE
 * <p>
 * This source file is subject to the MIT license that is bundled
 * with this package in the file MIT.txt.
 * It is also available through the world-wide-web at this URL:
 * http://www.opensource.org/licenses/mit-license.html
 */

package org.pkaboo.jpa.nestedset;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class FunctionalNestedSetTest {
    private static final Logger log = LoggerFactory.getLogger(FunctionalNestedSetTest.class);
    private static EntityManagerFactory emFactory;
    protected EntityManager em;
    protected JpaNestedSetManager nsm;

    @BeforeClass
    public static void createEntityManagerFactory() {
        try {
            emFactory = Persistence.createEntityManagerFactory("TestPU");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Before
    public void createEntityManager() {
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(lc);
        lc.reset();
        try {
            String filename = System.getProperty("user.dir") + "/target/test-classes/logback.xml";
            configurator.doConfigure(filename);
        } catch (JoranException e) {
            e.printStackTrace();
        }
        StatusPrinter.printInCaseOfErrorsOrWarnings(lc);

        em = emFactory.createEntityManager();
        this.nsm = new JpaNestedSetManager(this.em);
    }

    @After
    public void closeEntityManager() {
        if (em != null) {
            em.getTransaction().begin();
            em.createQuery("delete from Category").executeUpdate();
            em.getTransaction().commit();
            em.close();
            em = null;
        }
        this.nsm = null;
    }

    @AfterClass
    public static void closeEntityManagerFactory() {
        if (emFactory != null) {
            emFactory.close();
        }
    }

    void printTree(Node<?> node) {
        printNode(node);
        if (node.hasChildren()) {
            node.getChildren().stream()
                    .filter(node1 -> node1.getLevel() == node.getLevel() + 1)
                    .forEach(this::printTree);
        }
    }

    private void printNode(Node<?> node) {
        log.debug("{}{}", StringUtils.repeat("--", node.getLevel()), node.toString());
    }
}
