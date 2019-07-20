/**
 * LICENSE
 *
 * This source file is subject to the MIT license that is bundled
 * with this package in the file MIT.txt.
 * It is also available through the world-wide-web at this URL:
 * http://www.opensource.org/licenses/mit-license.html
 */

package org.pkaboo.jpa.nestedset;

/**
 * A NodeInfo implementor carries information about the identity and position
 * of a node in a nested set tree.
 */
public interface NodeInfo {
    Long getId();
    String getName();
    Integer getLeftValue();
    Integer getRightValue();
    Integer getLevel();
    Long getRootValue();
    void setName(String name);
    void setLeftValue(Integer value);
    void setRightValue(Integer value);
    void setLevel(Integer level);
    void setRootValue(Long value);
}
