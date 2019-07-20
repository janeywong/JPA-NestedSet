/**
 * LICENSE
 *
 * This source file is subject to the MIT license that is bundled
 * with this package in the file MIT.txt.
 * It is also available through the world-wide-web at this URL:
 * http://www.opensource.org/licenses/mit-license.html
 */

package org.pkaboo.jpa.nestedset.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import org.pkaboo.jpa.nestedset.NodeInfo;
import org.pkaboo.jpa.nestedset.annotations.LeftColumn;
import org.pkaboo.jpa.nestedset.annotations.LevelColumn;
import org.pkaboo.jpa.nestedset.annotations.RightColumn;
import org.pkaboo.jpa.nestedset.annotations.RootColumn;

@Entity
public class Category implements NodeInfo {
    @Id @GeneratedValue
    private Long id;
    private String name;

    @Column(updatable=false)
    @LeftColumn
    private Integer lft;
    @RightColumn
    @Column(updatable=false)
    private Integer rgt;
    @LevelColumn
    @Column(updatable=false)
    private Integer level;
    @RootColumn
    private Long rootId;

    @Override public Long getId() {
        return this.id;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Integer getLeftValue() {
        return this.lft;
    }

    @Override
    public Integer getRightValue() {
        return this.rgt;
    }

    @Override
    public Integer getLevel() {
        return this.level;
    }

    @Override
    public void setLeftValue(Integer value) {
        this.lft = value;
    }

    @Override
    public void setRightValue(Integer value) {
        this.rgt = value;
    }

    @Override
    public void setLevel(Integer level) {
        this.level = level;
    }

    @Override
    public Long getRootValue() {
        return this.rootId;
    }

    @Override
    public void setRootValue(Long value) {
        this.rootId = value;
    }

    @Override
    public String toString() {
        return "Category{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", lft=" + lft +
                ", rgt=" + rgt +
                ", level=" + level +
                ", rootId=" + rootId +
                '}';
    }
}
