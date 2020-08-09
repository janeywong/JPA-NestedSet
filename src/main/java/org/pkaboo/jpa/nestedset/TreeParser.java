package org.pkaboo.jpa.nestedset;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author jj
 * @link https://blog.csdn.net/qq_39547747/article/details/79655622
 */
public class TreeParser {

    /**
     * 根据id、parentId转adjacency list
     *
     * @param topId
     * @param entityList
     * @param <E>
     * @return
     */
    public static <E extends TreeNode> List<TreeNode> getTreeList(Long topId, List<E> entityList) {
        return entityList.stream()
                .filter(node -> node.getParentId() == null || node.getParentId().equals(topId))
                .peek(node -> node.setChildren(getSubList1(node.getId(), entityList)))
                .collect(Collectors.toList());
    }

    private static <E extends TreeNode> List<TreeNode> getSubList1(Long id, List<E> entityList) {
        List<TreeNode> subList = entityList.stream()
                .filter(node -> id.equals(node.getParentId()))
                .peek(node -> node.setChildren(getSubList1(node.getId(), entityList)))
                .collect(Collectors.toList());

        return subList.size() > 0 ? subList : null;
    }

    public static <T extends NodeInfo> List<TreeNode> getTreeList(List<T> bedList) {
        return getTreeList(0, bedList);
    }

    /**
     * 根据nested set model 转 adjacency list
     *
     * @param level   从哪一级开始构建
     * @param bedList
     * @return
     */
    public static <T extends NodeInfo> List<TreeNode> getTreeList(Integer level, List<T> bedList) {
        return bedList.stream()
                .filter(bed -> bed.getLevel().equals(level))
                .map(bed -> {
                    TreeNode treeNode = new TreeNode();
                    treeNode.setId(bed.getId());
                    treeNode.setName(bed.getName());
                    treeNode.setChildren(getSubList(bed, bedList));
                    return treeNode;
                }).collect(Collectors.toList());
    }

    private static <T extends NodeInfo> List<TreeNode> getSubList(T parent, List<T> bedList) {
        List<TreeNode> subList = bedList.stream()
                .filter(bed -> bed.getLevel().equals(parent.getLevel() + 1) && bed.getLft() > parent.getLft() && bed.getRgt() < parent.getRgt())
                .map(bed -> {
                    TreeNode treeNode = new TreeNode();
                    treeNode.setId(bed.getId());
                    treeNode.setName(bed.getName());
                    treeNode.setParentId(parent.getId());
                    treeNode.setChildren(getSubList(bed, bedList));
                    return treeNode;
                }).collect(Collectors.toList());
        return subList.size() > 0 ? subList : null;
    }

    public static <T extends NodeInfo> List<TreeNode> build(List<T> nodes) {
        return build(0, nodes);
    }

    public static <T extends NodeInfo> List<TreeNode> build(Integer level, List<T> nodes) {
        return nodes.stream()
                .filter(t -> t.getLevel().equals(level))
                .map(t -> {
                    TreeNode treeNode = new TreeNode();
                    treeNode.setId(t.getId());
                    treeNode.setName(t.getName());
                    treeNode.setChildren(getSub(t, nodes));
                    return treeNode;
                }).collect(Collectors.toList());
    }

    private static <T extends NodeInfo> List<TreeNode> getSub(T parent, List<T> nodes) {
        List<TreeNode> subList = nodes.stream()
                .filter(t -> t.getLevel().equals(parent.getLevel() + 1) && t.getLft() > parent.getLft() && t.getRgt() < parent.getRgt())
                .map(t -> {
                    TreeNode treeNode = new TreeNode();
                    treeNode.setId(t.getId());
                    treeNode.setName(t.getName());
                    treeNode.setParentId(parent.getId());
                    treeNode.setChildren(getSub(t, nodes));
                    return treeNode;
                }).collect(Collectors.toList());
        return subList.size() > 0 ? subList : null;
    }
}
