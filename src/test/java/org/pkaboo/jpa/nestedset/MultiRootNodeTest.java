/**
 * LICENSE
 *
 * This source file is subject to the MIT license that is bundled
 * with this package in the file MIT.txt.
 * It is also available through the world-wide-web at this URL:
 * http://www.opensource.org/licenses/mit-license.html
 */

package org.pkaboo.jpa.nestedset;

import org.junit.Test;
import org.pkaboo.jpa.nestedset.model.Category;

public class MultiRootNodeTest extends FunctionalNestedSetTest {

    @Test
    public void testCreateTrees() {
        Category javaCat = new Category();
        javaCat.setName("Java");
        javaCat.setRoot(1L);

        Category netCat = new Category();
        netCat.setName(".NET");
        netCat.setRoot(2L);

        Category phpCat = new Category();
        phpCat.setName("PHP");
        phpCat.setRoot(3L);

        em.getTransaction().begin();
        nsm.createRoot(javaCat);
        nsm.createRoot(netCat);
        nsm.createRoot(phpCat);
        em.getTransaction().commit();

        assert 1 == javaCat.getLft();
        assert 2 == javaCat.getRgt();
        assert 1 == netCat.getLft();
        assert 2 == netCat.getRgt();
        assert 1 == phpCat.getLft();
        assert 2 == phpCat.getRgt();

        em.getTransaction().begin();
        Node<Category> javaNode = nsm.getNode(javaCat);
        Category ejbCat = new Category();
        ejbCat.setName("EJB");
        Node<Category> ejbNode = javaNode.addChild(ejbCat);
        em.getTransaction().commit();

        assert 1 == javaCat.getLft();
        assert 2 == ejbCat.getLft();
        assert 3 == ejbCat.getRgt();
        assert 1 == ejbCat.getLevel();
        assert 1 == ejbCat.getRoot();
        assert 4 == javaCat.getRgt();
        assert 1 == netCat.getLft();
        assert 2 == netCat.getRgt();
        assert 1 == phpCat.getLft();
        assert 2 == phpCat.getRgt();

        // move between trees

        em.getTransaction().begin();
        Node<Category> netNode = nsm.getNode(netCat);
        ejbNode.moveAsLastChildOf(netNode);
        // Refresh to make sure that we check the database state, not just the in-memory state.
        em.refresh(javaCat);
        em.refresh(netCat);
        em.refresh(phpCat);
        em.getTransaction().commit();

        assert 1 == javaCat.getLft();
        assert 2 == javaCat.getRgt();
        assert 1 == netNode.getLft();
        assert 2 == ejbNode.getLft();
        assert 3 == ejbNode.getRgt();
        assert 2 == ejbNode.getRoot();
        assert 4 == netNode.getRgt();
        assert 1 == phpCat.getLft();
        assert 2 == phpCat.getRgt();

    }


}
