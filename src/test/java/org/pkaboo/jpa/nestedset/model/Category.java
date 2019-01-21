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
    private int id;
    private String name;

    @Column(updatable=false)
    @LeftColumn
    private int lft;
    @RightColumn
    @Column(updatable=false)
    private int rgt;
    @LevelColumn
    @Column(updatable=false)
    private int level;
    @RootColumn
    private int rootId;

    @Override public int getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int getLeftValue() {
        return this.lft;
    }

    @Override
    public int getRightValue() {
        return this.rgt;
    }

    @Override
    public int getLevel() {
        return this.level;
    }

    @Override
    public void setLeftValue(int value) {
        this.lft = value;
    }

    @Override
    public void setRightValue(int value) {
        this.rgt = value;
    }

    @Override
    public void setLevel(int level) {
        this.level = level;
    }

    @Override
    public int getRootValue() {
        return this.rootId;
    }

    @Override
    public void setRootValue(int value) {
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
