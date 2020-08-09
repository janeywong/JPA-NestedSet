package org.pkaboo.jpa.nestedset;

import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * @author jj
 */
public class TreeNode implements TreeEntity<TreeNode> {
    private Long id;

    private Long parentId;

    private String name;

    private List<TreeNode> children;

    public TreeNode(){}

    public TreeNode(Long id, String name, Long parentId) {
        this.id = id;
        this.parentId = parentId;
        this.name = name;
    }

    public TreeNode(Long id, String name, TreeNode parent) {
        this(id, name, parent.getId());
    }


    @Override
    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Boolean isParent() {
        return CollectionUtils.isEmpty(children);
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<TreeNode> getChildren() {
        return children;
    }

    @Override
    public void setChildren(List<TreeNode> children) {
        this.children = children;
    }

    @Override
    public String toString() {
        return "TreeNode{" +
            "id=" + id +
            ", parentId=" + parentId +
            ", isParent=" + isParent() +
            ", name='" + name + '\'' +
            ", children=" + children +
            '}';
    }
}
