package org.pkaboo.jpa.nestedset;

import java.util.List;

/**
 * @author jj
 */
public interface TreeEntity<E extends TreeNode> {
    /**
     * 节点编号
     * @return 节点编号
     */
    Long getId();

    /**
     * 节点名称
     * @return 节点名称
     */
    String getName();

    /**
     * 是否为叶子节点
     * @return
     */
    Boolean isParent();

    /**
     * 父级编号
     * @return 父级编号
     */
    Long getParentId();

    /**
     * 设置子节点
     * @param children list
     */
    void setChildren(List<E> children);
}
