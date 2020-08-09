/**
 * LICENSE
 * <p>
 * This source file is subject to the MIT license that is bundled
 * with this package in the file MIT.txt.
 * It is also available through the world-wide-web at this URL:
 * http://www.opensource.org/licenses/mit-license.html
 */

package org.pkaboo.jpa.nestedset;

import net.jcip.annotations.NotThreadSafe;
import org.pkaboo.jpa.nestedset.annotations.LeftColumn;
import org.pkaboo.jpa.nestedset.annotations.LevelColumn;
import org.pkaboo.jpa.nestedset.annotations.RightColumn;
import org.pkaboo.jpa.nestedset.annotations.RootColumn;

import javax.inject.Inject;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.lang.reflect.Field;
import java.util.*;

/** The default implementation of a JPA {@link NestedSetManager}. */
@NotThreadSafe
public class JpaNestedSetManager implements NestedSetManager {
    private final EntityManager em;
    private final Map<Key, Node<?>> nodes;
    private final Map<Class<?>, Configuration> configs;
    private final Long defaultRootId = 0L;

    @Inject
    public JpaNestedSetManager(EntityManager em) {
        this.em = em;
        this.nodes = new HashMap<Key, Node<?>>();
        this.configs = new HashMap<Class<?>, Configuration>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EntityManager getEntityManager() {
        return this.em;
    }

    /**
     * {@inheritDoc}
     */
    // @Override
    public void clear() {
        this.nodes.clear();
    }

    /**
     * {@inheritDoc}
     */
    // @Override
    public Collection<Node<?>> getManagedNodes() {
        return Collections.unmodifiableCollection(this.nodes.values());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends NodeInfo> List<Node<T>> listNodes(Class<T> clazz) {
        return listNodes(clazz, defaultRootId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends NodeInfo> List<Node<T>> listNodes(Class<T> clazz, Long rootId) {
        Configuration config = getConfig(clazz);
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<T> cq = cb.createQuery(clazz);
        Root<T> queryRoot = cq.from(clazz);
        cq.where(cb.ge(queryRoot.<Number>get(config.getLeftFieldName()), 1));
        cq.orderBy(cb.asc(queryRoot.get(config.getLeftFieldName())));
        applyRootId(clazz, cq, rootId);

        List<Node<T>> nodes = new ArrayList<Node<T>>();
        for (T n : em.createQuery(cq).getResultList()) {
            nodes.add(getNode(n));
        }

        return nodes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends NodeInfo> Node<T> createRoot(T root) {
        if (root.getLft() != null && root.getRgt() != null && root.getLft() < root.getRgt()) {
            throw new IllegalArgumentException("The node already has a position in a tree.");
        }

        Configuration config = getConfig(root.getClass());

        int maximumRight;
        if (config.hasManyRoots()) {
            maximumRight = 0;
        } else {
            maximumRight = getMaximumRight(root.getClass());
        }
        root.setLft(maximumRight + 1);
        root.setRgt(maximumRight + 2);
        root.setLevel(0);

        if (root.getRoot() == null) {
            root.setRoot(defaultRootId);
        }

        em.persist(root);

        return getNode(root);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends NodeInfo> Node<T> getNode(T nodeInfo) {
        Key key = new Key(nodeInfo.getClass(), nodeInfo.getId());
        if (this.nodes.containsKey(key)) {
            @SuppressWarnings("unchecked")
            Node<T> n = (Node<T>) this.nodes.get(key);
            return n;
        }
        Node<T> node = new JpaNode<T>(nodeInfo, this);
        if (!node.isValid()) {
            throw new IllegalArgumentException("The given NodeInfo instance has no position " +
                    "in a tree and is thus not yet a node.");
        }
        this.nodes.put(key, node);

        return node;
    }

    Configuration getConfig(Class<?> clazz) {
        if (!this.configs.containsKey(clazz)) {
            Configuration config = new Configuration();

            Entity entity = clazz.getAnnotation(Entity.class);
            String name = entity.name();
            config.setEntityName(name.length() > 0 ? name : clazz.getSimpleName());

            for (Field field : clazz.getDeclaredFields()) {
                if (field.getAnnotation(LeftColumn.class) != null) {
                    config.setLeftFieldName(field.getName());
                } else if (field.getAnnotation(RightColumn.class) != null) {
                    config.setRightFieldName(field.getName());
                } else if (field.getAnnotation(LevelColumn.class) != null) {
                    config.setLevelFieldName(field.getName());
                } else if (field.getAnnotation(RootColumn.class) != null) {
                    config.setRootIdFieldName(field.getName());
                }
            }

            this.configs.put(clazz, config);
        }

        return this.configs.get(clazz);
    }

    private int getMaximumRight(Class<? extends NodeInfo> clazz) {
        Configuration config = getConfig(clazz);
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<? extends NodeInfo> cq = cb.createQuery(clazz);
        Root<? extends NodeInfo> queryRoot = cq.from(clazz);
        cq.orderBy(cb.desc(queryRoot.get(config.getRightFieldName())));
        List<? extends NodeInfo> highestRows = em.createQuery(cq).setMaxResults(1).getResultList();
        if (highestRows.isEmpty()) {
            return 0;
        } else {
            return highestRows.get(0).getRgt();
        }
    }

    void applyRootId(Class<?> clazz, CriteriaQuery<?> cq, Long rootId) {
        Configuration config = getConfig(clazz);
        if (config.getRootIdFieldName() != null) {
            Root<?> root = cq.getRoots().iterator().next();
            CriteriaBuilder cb = em.getCriteriaBuilder();
            Predicate p = cq.getRestriction();
            cq.where(cb.and(p, cb.equal(root.get(config.getRootIdFieldName()), rootId)));
        }
    }

    void updateLeftValues(int minLeft, int maxLeft, int delta, Long rootId) {
        for (Node<?> node : this.nodes.values()) {
            if (node.getRoot().equals(rootId)) {
                if (node.getLft() >= minLeft && (maxLeft == 0 || node.getLft() <= maxLeft)) {
                    node.setLft(node.getLft() + delta);
                }
            }
        }
    }

    void updateRightValues(int minRight, int maxRight, int delta, Long rootId) {
        for (Node<?> node : this.nodes.values()) {
            if (node.getRoot().equals(rootId)) {
                if (node.getRgt() >= minRight && (maxRight == 0 || node.getRgt() <= maxRight)) {
                    node.setRgt(node.getRgt() + delta);
                }
            }
        }
    }

    void updateLevels(int left, int right, int delta, Long rootId) {
        for (Node<?> node : this.nodes.values()) {
            if (node.getRoot().equals(rootId)) {
                if (node.getLft() > left && node.getRgt() < right) {
                    node.setLevel(node.getLevel() + delta);
                }
            }
        }
    }

    void removeNodes(int left, int right, Long rootId) {
        Set<Key> removed = new HashSet<Key>();
        for (Node<?> node : this.nodes.values()) {
            if (node.getRoot().equals(rootId)) {
                if (node.getLft() >= left && node.getRgt() <= right) {
                    removed.add(new Key(node.unwrap().getClass(), node.getId()));
                }
            }
        }
        for (Key k : removed) {
            Node<?> n = this.nodes.remove(k);
            n.setLft(0);
            n.setRgt(0);
            n.setLevel(0);
            n.setRoot(0L);
            this.em.detach(n.unwrap());
        }
    }
}
