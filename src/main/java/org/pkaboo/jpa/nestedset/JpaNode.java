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
import org.springframework.util.ObjectUtils;

import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;

/**
 * A decorator for a {@link NodeInfo} implementation that enriches it with the full API
 * of a node in a nested set tree.
 *
 * @param <T extends NodeInfo> The wrapped entity type.
 */
@NotThreadSafe
class JpaNode<T extends NodeInfo> implements Node<T> {
    private static final int PREV_SIBLING = 1;
    private static final int FIRST_CHILD = 2;
    private static final int NEXT_SIBLING = 3;
    private static final int LAST_CHILD = 4;

    private final JpaNestedSetManager nsm;
    private final T node;
    private final Class<T> type;

    private CriteriaQuery<T> baseQuery;
    private Root<T> queryRoot;

    @SuppressWarnings("unchecked")
    public JpaNode(T node, JpaNestedSetManager nsm) {
        this.node = node;
        this.nsm = nsm;
        this.type = (Class<T>) node.getClass();
    }

    @Override
    public Long getId() {
        return this.node.getId();
    }

    @Override
    public String getName() {
        return this.node.getName();
    }

    @Override
    public Integer getLft() {
        return this.node.getLft();
    }

    @Override
    public Integer getRgt() {
        return this.node.getRgt();
    }

    @Override
    public Integer getLevel() {
        return this.node.getLevel();
    }

    @Override
    public Long getRoot() {
        return this.node.getRoot();
    }

    @Override
    public void setName(String name) {
        this.node.setName(name);
    }

    @Override
    public void setRoot(Long value) {
        this.node.setRoot(value);
    }

    @Override
    public void setLft(Integer value) {
        this.node.setLft(value);
    }

    @Override
    public void setRgt(Integer value) {
        this.node.setRgt(value);
    }

    @Override
    public void setLevel(Integer level) {
        this.node.setLevel(level);
    }

    @Override
    public String toString() {
        return "JpaNode{" +
                "node=" + node +
                '}';
    }

    @Override
    public boolean hasChildren() {
        return (getRgt() - getLft()) > 1;
    }

    @Override
    public boolean hasParent() {
        return !isRoot();
    }

    @Override
    public boolean isValid() {
        return isValidNode(this);
    }

    private boolean isValidNode(NodeInfo node) {
        return node != null && node.getRgt() > node.getLft();
    }

    private CriteriaQuery<T> getBaseQuery() {
        if (this.baseQuery == null) {
            this.baseQuery = nsm.getEntityManager().getCriteriaBuilder().createQuery(type);
            this.queryRoot = this.baseQuery.from(type);
        }
        return this.baseQuery;
    }

    public int getNumberOfChildren() {
        return getChildren().size();
    }

    public int getNumberOfDescendants() {
        return (this.getRgt() - this.getLft() - 1) / 2;
    }

    @Override
    public boolean isRoot() {
        return getLft() == 1;
    }

    @Override
    public List<Node<T>> getChildren() {
        return getDescendants(1);
    }

    @Override
    public Node<T> getParent() {
        if (isRoot()) {
            return null;
        }

        CriteriaBuilder cb = nsm.getEntityManager().getCriteriaBuilder();
        CriteriaQuery<T> cq = getBaseQuery();
        cq.where(cb.lt(
                queryRoot.<Number>get(nsm.getConfig(this.type).getLeftFieldName()),
                getLft()
                ),
                cb.gt(
                        queryRoot.<Number>get(nsm.getConfig(this.type).getRightFieldName()),
                        getRgt()
                ));
        cq.orderBy(cb.asc(queryRoot.get(nsm.getConfig(this.type).getRightFieldName())));
        nsm.applyRootId(this.type, cq, getRoot());

        List<T> result = nsm.getEntityManager().createQuery(cq).getResultList();

        return nsm.getNode(result.get(0));
    }

    @Override
    public List<Node<T>> getDescendants() {
        return getDescendants(0);
    }

    @Override
    public List<Node<T>> getDescendants(int depth) {
        CriteriaBuilder cb = nsm.getEntityManager().getCriteriaBuilder();
        CriteriaQuery<T> cq = getBaseQuery();
        Predicate wherePredicate = cb.and(
                cb.gt(
                        queryRoot.get(nsm.getConfig(this.type).getLeftFieldName()),
                        getLft()
                ),
                cb.lt(
                        queryRoot.get(nsm.getConfig(this.type).getRightFieldName()),
                        getRgt()
                ));

        if (depth > 0) {
            wherePredicate = cb.and(
                    wherePredicate,
                    cb.le(
                            queryRoot.get(nsm.getConfig(this.type).getLevelFieldName()),
                            getLevel() + depth
                    )
            );
        }
        cq.where(wherePredicate);
        cq.orderBy(cb.asc(queryRoot.get(nsm.getConfig(this.type).getLeftFieldName())));

        nsm.applyRootId(this.type, cq, getRoot());

        List<Node<T>> nodes = new ArrayList<>();
        for (T n : nsm.getEntityManager().createQuery(cq).getResultList()) {
            nodes.add(nsm.getNode(n));
        }

        return nodes;
    }

    @Override
    public Node<T> addChild(T child) {
        if (child == this.node) {
            throw new IllegalArgumentException("Cannot add node as child of itself.");
        }

        int newLeft = getRgt();
        int newRight = getRgt() + 1;
        Long newRoot = getRoot();

        shiftRLValues(newLeft, 0, 2, newRoot);
        child.setLevel(getLevel() + 1);
        child.setLft(newLeft);
        child.setRgt(newRight);
        child.setRoot(newRoot);

        if(ObjectUtils.isEmpty(child.getId())){
            nsm.getEntityManager().persist(child);
        }else{
            nsm.getEntityManager().merge(child);
        }


        return this.nsm.getNode(child);
    }

    private void insertAsPrevSiblingOf(Node<T> dest) {
        if (dest == this.node) {
            throw new IllegalArgumentException("Cannot add node as child of itself.");
        }

        int newLeft = dest.getLft();
        int newRight = dest.getLft() + 1;
        Long newRoot = dest.getRoot();

        shiftRLValues(newLeft, 0, 2, newRoot);
        setLevel(dest.getLevel());
        setLft(newLeft);
        setRgt(newRight);
        setRoot(newRoot);
        nsm.getEntityManager().persist(this.node);
    }

    private void insertAsNextSiblingOf(Node<T> dest) {
        if (dest == this.node) {
            throw new IllegalArgumentException("Cannot add node as child of itself.");
        }

        int newLeft = dest.getRgt() + 1;
        int newRight = dest.getRgt() + 2;
        Long newRoot = dest.getRoot();

        shiftRLValues(newLeft, 0, 2, newRoot);
        setLevel(dest.getLevel());
        setLft(newLeft);
        setRgt(newRight);
        setRoot(newRoot);
        nsm.getEntityManager().persist(this.node);
    }

    private void insertAsLastChildOf(Node<T> dest) {
        if (dest == this.node) {
            throw new IllegalArgumentException("Cannot add node as child of itself.");
        }

        int newLeft = dest.getRgt();
        int newRight = dest.getRgt() + 1;
        Long newRoot = dest.getRoot();

        shiftRLValues(newLeft, 0, 2, newRoot);
        setLevel(dest.getLevel() + 1);
        setLft(newLeft);
        setRgt(newRight);
        setRoot(newRoot);
        nsm.getEntityManager().persist(this.node);
    }

    private void insertAsFirstChildOf(Node<T> dest) {
        if (dest == this.node) {
            throw new IllegalArgumentException("Cannot add node as child of itself.");
        }

        int newLeft = dest.getLft() + 1;
        int newRight = dest.getLft() + 2;
        Long newRoot = dest.getRoot();

        shiftRLValues(newLeft, 0, 2, newRoot);
        setLevel(dest.getLevel());
        setLft(newLeft);
        setRgt(newRight);
        setRoot(newRoot);
        nsm.getEntityManager().persist(this.node);
    }

    @Override
    public void delete() {
        Long oldRoot = getRoot();
        Configuration cfg = nsm.getConfig(this.type);
        String rootIdFieldName = cfg.getRootIdFieldName();
        String leftFieldName = cfg.getLeftFieldName();
        String rightFieldName = cfg.getRightFieldName();
        String entityName = cfg.getEntityName();

        StringBuilder sb = new StringBuilder();
        sb.append("delete from ")
                .append(entityName).append(" n")
                .append(" where n.").append(leftFieldName).append(">= ?1")
                .append(" and n.").append(rightFieldName).append("<= ?2");

        if (rootIdFieldName != null) {
            sb.append(" and n.").append(rootIdFieldName).append("= ?3");
        }

        Query q = nsm.getEntityManager().createQuery(sb.toString());
        q.setParameter(1, getLft());
        q.setParameter(2, getRgt());
        if (rootIdFieldName != null) {
            q.setParameter(3, oldRoot);
        }
        q.executeUpdate();

        // Close gap in tree
        int first = getRgt() + 1;
        int delta = getLft() - getRgt() - 1;
        shiftRLValues(first, 0, delta, oldRoot);

        nsm.removeNodes(getLft(), getRgt(), oldRoot);
    }

    /**
     * Adds 'delta' to all left and right values that are >= 'first' and
     * <= 'last'. 'delta' can also be negative. If 'last' is 0 it is skipped and there is
     * no upper bound.
     *
     * @param first  The first left/right value (inclusive) of the nodes to shift.
     * @param last   The last left/right value (inclusive) of the nodes to shift.
     * @param delta  The offset by which to shift the left/right values (can be negative).
     * @param rootId The root/tree ID of the nodes to shift.
     */
    private void shiftRLValues(int first, int last, int delta, Long rootId) {
        Configuration cfg = nsm.getConfig(this.type);
        String rootIdFieldName = cfg.getRootIdFieldName();
        String leftFieldName = cfg.getLeftFieldName();
        String rightFieldName = cfg.getRightFieldName();
        String entityName = cfg.getEntityName();

        // Shift left values
        StringBuilder sbLeft = new StringBuilder();
        sbLeft.append("update ").append(entityName).append(" n")
                .append(" set n.").append(leftFieldName).append(" = n.").append(leftFieldName).append(" + ?1")
                .append(" where n.").append(leftFieldName).append(" >= ?2");

        if (last > 0) {
            sbLeft.append(" and n.").append(leftFieldName).append(" <= ?3");
        }

        if (rootIdFieldName != null && rootId != null) {
            sbLeft.append(" and n.").append(rootIdFieldName).append(" = ?").append(last > 0 ? 4 : 3);
        }

        Query qLeft = nsm.getEntityManager().createQuery(sbLeft.toString());
        qLeft.setParameter(1, delta);
        qLeft.setParameter(2, first);
        if (last > 0) {
            qLeft.setParameter(3, last);
        }
        if (rootIdFieldName != null && rootId != null) {
            qLeft.setParameter(last > 0 ? 4 : 3, rootId);
        }
        qLeft.executeUpdate();
        this.nsm.updateLeftValues(first, last, delta, rootId);

        // Shift right values
        StringBuilder sbRight = new StringBuilder();
        sbRight.append("update ").append(entityName).append(" n")
                .append(" set n.").append(rightFieldName).append(" = n.").append(rightFieldName).append(" + ?1")
                .append(" where n.").append(rightFieldName).append(" >= ?2");

        if (last > 0) {
            sbRight.append(" and n.").append(rightFieldName).append(" <= ?3");
        }

        if (rootIdFieldName != null && rootId != null) {
            sbRight.append(" and n.").append(rootIdFieldName).append(" = ?").append(last > 0 ? 4 : 3);
        }

        Query qRight = nsm.getEntityManager().createQuery(sbRight.toString());
        qRight.setParameter(1, delta);
        qRight.setParameter(2, first);
        if (last > 0) {
            qRight.setParameter(3, last);
        }
        if (rootIdFieldName != null && rootId != null) {
            qRight.setParameter(last > 0 ? 4 : 3, rootId);
        }
        qRight.executeUpdate();
        this.nsm.updateRightValues(first, last, delta, rootId);
    }

    @Override
    public T unwrap() {
        return this.node;
    }

    public boolean isLeaf() {
        return !hasChildren();
    }

    @Override
    public Node<T> getFirstChild() {
        CriteriaBuilder cb = nsm.getEntityManager().getCriteriaBuilder();
        CriteriaQuery<T> cq = getBaseQuery();
        cq.where(cb.equal(queryRoot.get(nsm.getConfig(this.type).getLeftFieldName()), getLft() + 1));

        nsm.applyRootId(this.type, cq, getRoot());

        return nsm.getNode(nsm.getEntityManager().createQuery(cq).getSingleResult());
    }

    @Override
    public Node<T> getLastChild() {
        CriteriaBuilder cb = nsm.getEntityManager().getCriteriaBuilder();
        CriteriaQuery<T> cq = getBaseQuery();
        cq.where(cb.equal(queryRoot.get(nsm.getConfig(this.type).getRightFieldName()), getRgt() - 1));

        nsm.applyRootId(this.type, cq, getRoot());

        return nsm.getNode(nsm.getEntityManager().createQuery(cq).getSingleResult());
    }

    @Override
    public List<Node<T>> getAncestors() {
        CriteriaBuilder cb = nsm.getEntityManager().getCriteriaBuilder();
        CriteriaQuery<T> cq = getBaseQuery();
        Predicate wherePredicate = cb.and(
                cb.lt(queryRoot.<Number>get(nsm.getConfig(this.type).getLeftFieldName()), getLft()),
                cb.gt(queryRoot.<Number>get(nsm.getConfig(this.type).getRightFieldName()), getRgt())
        );

        cq.where(wherePredicate);
        cq.orderBy(cb.asc(queryRoot.get(nsm.getConfig(this.type).getLeftFieldName())));

        nsm.applyRootId(this.type, cq, getRoot());

        List<Node<T>> nodes = new ArrayList<Node<T>>();

        for (T n : nsm.getEntityManager().createQuery(cq).getResultList()) {
            nodes.add(nsm.getNode(n));
        }

        return nodes;
    }

    @Override
    public boolean isDescendantOf(Node<T> subj) {
        return ((getLft() > subj.getLft()) &&
                (getRgt() < subj.getRgt()) &&
                (getRoot() == subj.getRoot()));
    }

    public String getPath(String seperator) {
        StringBuilder path = new StringBuilder();
        List<Node<T>> ancestors = getAncestors();
        for (Node<T> ancestor : ancestors) {
            path.append(ancestor.toString()).append(seperator);
        }

        return path.toString();
    }

    @Override
    public void moveAsPrevSiblingOf(Node<T> dest) {
        if (dest == this.node) {
            throw new IllegalArgumentException("Cannot move node as previous sibling of itself");
        }

        if (dest.getRoot() != getRoot()) {
            moveBetweenTrees(dest, dest.getLft(), 1);
        } else {
            // Move within the tree
            int oldLevel = getLevel();
            setLevel(dest.getLevel());
            updateNode(dest.getLft(), getLevel() - oldLevel);
        }
    }

    /**
     * move node's and its children to location 'destLeft' and update rest of tree.
     *
     * @param int       destLeft destination left value
     * @param levelDiff
     */
    private void updateNode(int destLeft, int levelDiff) {
        int left = getLft();
        int right = getRgt();
        Long rootId = getRoot();
        int treeSize = right - left + 1;

        // Make room in the new branch
        shiftRLValues(destLeft, 0, treeSize, rootId);

        if (left >= destLeft) { // src was shifted too?
            left += treeSize;
            right += treeSize;
        }

        String levelFieldName = nsm.getConfig(this.type).getLevelFieldName();
        String leftFieldName = nsm.getConfig(this.type).getLeftFieldName();
        String rightFieldName = nsm.getConfig(this.type).getRightFieldName();
        String rootIdFieldName = nsm.getConfig(this.type).getRootIdFieldName();
        String entityName = nsm.getConfig(this.type).getEntityName();

        // update level for descendants
        StringBuilder updateQuery = new StringBuilder();
        updateQuery.append("update ").append(entityName).append(" n")
                .append(" set n.").append(levelFieldName).append(" = n.").append(levelFieldName).append(" + ?1")
                .append(" where n.").append(leftFieldName).append(" > ?2")
                .append(" and n.").append(rightFieldName).append(" < ?3");

        if (rootIdFieldName != null) {
            updateQuery.append(" and n.").append(rootIdFieldName).append(" = ?4");
        }

        Query q = nsm.getEntityManager().createQuery(updateQuery.toString());
        q.setParameter(1, levelDiff);
        q.setParameter(2, left);
        q.setParameter(3, right);
        if (rootIdFieldName != null) {
            q.setParameter(4, rootId);
        }
        q.executeUpdate();
        this.nsm.updateLevels(left, right, levelDiff, rootId);

        // now there's enough room next to target to move the subtree
        shiftRLValues(left, right, destLeft - left, rootId);

        // correct values after source (close gap in old tree)
        shiftRLValues(right + 1, 0, -treeSize, rootId);
    }

    @Override
    public void moveAsNextSiblingOf(Node<T> dest) {
        if (dest == this.node) {
            throw new IllegalArgumentException("Cannot move node as next sibling of itself");
        }
        if (!dest.getRoot().equals(getRoot())) {
            moveBetweenTrees(dest, dest.getRgt() + 1, 3);
        } else {
            // Move within tree
            int oldLevel = getLevel();
            setLevel(dest.getLevel());
            updateNode(dest.getRgt() + 1, getLevel() - oldLevel);
        }
    }

    @Override
    public void moveAsFirstChildOf(Node<T> dest) {
        if (dest == this.node) {
            throw new IllegalArgumentException("Cannot move node as first child of itself");
        }

        if (!dest.getRoot().equals(getRoot())) {
            moveBetweenTrees(dest, dest.getLft() + 1, 2);
        } else {
            // Move within tree
            int oldLevel = getLevel();
            setLevel(dest.getLevel() + 1);
            updateNode(dest.getLft() + 1, getLevel() - oldLevel);
        }
    }

    @Override
    public void moveAsLastChildOf(Node<T> dest) {
        if (dest == this.node) {
            throw new IllegalArgumentException("Cannot move node as first child of itself");
        }

        if (!dest.getRoot().equals(getRoot())) {
            moveBetweenTrees(dest, dest.getLft() + 1, 4);
        } else {
            // Move within tree
            int oldLevel = getLevel();
            setLevel(dest.getLevel() + 1);
            updateNode(dest.getRgt(), getLevel() - oldLevel);
        }
    }

    /**
     * Accomplishes moving of nodes between different trees.
     * Used by the move* methods if the root values of the two nodes are different.
     *
     * @param dest
     * @param newLeftValue
     * @param moveType
     */
    private void moveBetweenTrees(Node<T> dest, int newLeftValue, int moveType) {

        Configuration cfg = nsm.getConfig(this.type);
        String leftFieldName = cfg.getLeftFieldName();
        String rightFieldName = cfg.getRightFieldName();
        String levelFieldName = cfg.getLevelFieldName();
        String rootIdFieldName = cfg.getRootIdFieldName();
        String entityName = cfg.getEntityName();

        // Move between trees: Detach from old tree & insert into new tree
        Long newRoot = dest.getRoot();
        Long oldRoot = getRoot();
        int oldLft = getLft();
        int oldRgt = getRgt();
        int oldLevel = getLevel();

        // Prepare target tree for insertion, make room
        shiftRLValues(newLeftValue, 0, oldRgt - oldLft - 1, newRoot);

        // Set new root id for this node
        setRoot(newRoot);
        //$this ->  _node ->  save();
        // Insert this node as a new node
        setRgt(0);
        setLft(0);

        switch (moveType) {
            case PREV_SIBLING:
                insertAsPrevSiblingOf(dest);
                break;
            case FIRST_CHILD:
                insertAsFirstChildOf(dest);
                break;
            case NEXT_SIBLING:
                insertAsNextSiblingOf(dest);
                break;
            case LAST_CHILD:
                insertAsLastChildOf(dest);
                break;
            default:
                throw new IllegalArgumentException("Unknown move operation: " + moveType);
        }

        setRgt(getLft() + (oldRgt - oldLft));

        int newLevel = getLevel();
        int levelDiff = newLevel - oldLevel;

        // Relocate descendants of the node
        int diff = getLft() - oldLft;

        // Update lft/rgt/root/level for all descendants
        StringBuilder updateQuery = new StringBuilder();
        updateQuery.append("update ").append(entityName).append(" n")
                .append(" set n.").append(leftFieldName).append(" = n.").append(leftFieldName).append(" + ?1")
                .append(", n.").append(rightFieldName).append(" = n.").append(rightFieldName).append(" + ?2")
                .append(", n.").append(levelFieldName).append(" = n.").append(levelFieldName).append(" + ?3")
                .append(", n.").append(rootIdFieldName).append(" = ?4")
                .append(" where n.").append(leftFieldName).append(" > ?5")
                .append(" and n.").append(rightFieldName).append(" < ?6")
                .append(" and n.").append(rootIdFieldName).append(" = ?7");

        Query q = nsm.getEntityManager().createQuery(updateQuery.toString());
        q.setParameter(1, diff);
        q.setParameter(2, diff);
        q.setParameter(3, levelDiff);
        q.setParameter(4, newRoot);
        q.setParameter(5, oldLft);
        q.setParameter(6, oldRgt);
        q.setParameter(7, oldRoot);

        q.executeUpdate();

        // Close gap in old tree
        int first = oldRgt + 1;
        int delta = oldLft - oldRgt - 1;
        shiftRLValues(first, 0, delta, oldRoot);
    }

    public void makeRoot(Long newRootId) {
        if (isRoot()) {
            return;
        }

        Configuration cfg = nsm.getConfig(this.type);
        String leftFieldName = cfg.getLeftFieldName();
        String rightFieldName = cfg.getRightFieldName();
        String levelFieldName = cfg.getLevelFieldName();
        String rootIdFieldName = cfg.getRootIdFieldName();
        String entityName = cfg.getEntityName();

        int oldRgt = getRgt();
        int oldLft = getLft();
        Long oldRoot = getRoot();
        int oldLevel = getLevel();

        // Update descendants lft/rgt/root/level values
        int diff = 1 - oldLft;
        Long newRoot = newRootId;

        StringBuilder updateQuery = new StringBuilder();
        updateQuery.append("update ").append(entityName).append(" n")
                .append(" set n.").append(leftFieldName).append(" = n.").append(leftFieldName).append(" + ?1")
                .append(", n.").append(rightFieldName).append(" = n.").append(rightFieldName).append(" + ?2")
                .append(", n.").append(levelFieldName).append(" = n.").append(levelFieldName).append(" - ?3")
                .append(", n.").append(rootIdFieldName).append(" = ?4")
                .append("where n.").append(leftFieldName).append(" > ?5")
                .append(" and n.").append(rightFieldName).append(" < ?6")
                .append(" and n.").append(rootIdFieldName).append(" = ?7");

        Query q = nsm.getEntityManager().createQuery(updateQuery.toString());
        q.setParameter(1, diff);
        q.setParameter(2, diff);
        q.setParameter(3, oldLevel);
        q.setParameter(4, newRoot);
        q.setParameter(5, oldLft);
        q.setParameter(6, oldRgt);
        q.setParameter(7, oldRoot);

        q.executeUpdate();

        // Detach from old tree (close gap in old tree)
        int first = oldRgt + 1;
        int delta = oldLft - oldRgt - 1;
        shiftRLValues(first, 0, delta, getRoot());

        // Set new lft/rgt/root/level values for root node
        setLft(1);
        setRgt(oldRgt - oldLft + 1);
        setRoot(newRootId);
        setLevel(0);
    }
}
