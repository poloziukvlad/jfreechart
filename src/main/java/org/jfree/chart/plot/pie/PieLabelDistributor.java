/* ======================================================
 * JFreeChart : a chart library for the Java(tm) platform
 * ======================================================
 *
 * (C) Copyright 2000-present, by David Gilbert and Contributors.
 *
 * Project Info:  https://www.jfree.org/jfreechart/index.html
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 *
 * [Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.]
 *
 * ------------------------
 * PieLabelDistributor.java
 * ------------------------
 * (C) Copyright 2004-present, by David Gilbert and Contributors.
 *
 * Original Author:  David Gilbert;
 * Contributor(s):   -;
 *
 */

package org.jfree.chart.plot.pie;

import java.util.Collections;

/**
 * This class distributes the section labels for one side of a pie chart so
 * that they do not overlap.
 */
public class PieLabelDistributor extends AbstractPieLabelDistributor {

    /** The default minimum gap between two adjacent labels (in Java2D units). */
    private static final double DEFAULT_MINIMUM_GAP = 4.0;

    /** The minimum gap. */
    private double minGap = DEFAULT_MINIMUM_GAP;

    /**
     * Creates a new distributor.
     *
     * @param labelCount  the number of labels (ignored).
     */
    public PieLabelDistributor(int labelCount) {
        super();
    }

    /**
     * Distributes the labels.
     *
     * @param minY  the minimum y-coordinate in Java2D-space.
     * @param height  the available height (in Java2D units).
     */
    @Override
    public void distributeLabels(double minY, double height) {
        sort();  // sorts the label records into ascending order by baseY

        if (isOverlap()) {
            adjustDownwards(minY, height);
        }

        // if still overlapping, do something else...
        if (isOverlap()) {
            adjustUpwards(minY, height);
        }

        if (isOverlap()) {
            spreadEvenly(minY, height);
        }
    }

    /**
     * Returns {@code true} if there are overlapping labels in the list,
     * and {@code false} otherwise.
     *
     * @return A boolean.
     */
    private boolean isOverlap() {
        double previousUpperY = 0.0;
        for (int i = 0; i < this.labels.size(); i++) {
            PieLabelRecord current = getPieLabelRecord(i);
            if (previousUpperY > current.getLowerY()) {
                return true;
            }
            previousUpperY = current.getUpperY();
        }
        return false;
    }

    /**
     * Adjusts the y-coordinate for the labels in towards the center in an
     * attempt to fix overlapping.
     */
    protected void adjustInwards() {
        int lower = 0;
        int upper = this.labels.size() - 1;
        while (upper > lower) {
            if (lower < upper - 1) {
                PieLabelRecord labelAbove = getPieLabelRecord(lower);
                PieLabelRecord labelBelow = getPieLabelRecord(lower + 1);
                if (labelBelow.getLowerY() < labelAbove.getUpperY()) {
                    double adjust = labelAbove.getUpperY() - labelBelow.getLowerY()
                                    + this.minGap;
                    labelBelow.setAllocatedY(labelBelow.getAllocatedY() + adjust);
                }
            }
            PieLabelRecord secondLast = getPieLabelRecord(upper - 1);
            PieLabelRecord last = getPieLabelRecord(upper);
            if (secondLast.getUpperY() > last.getLowerY()) {
                double adjust = (secondLast.getUpperY() - last.getLowerY()) + this.minGap;
                last.setAllocatedY(last.getAllocatedY() + adjust);
            }
            lower++;
            upper--;
        }
    }

    /**
     * Any labels that are overlapping are moved down in an attempt to
     * eliminate the overlaps.
     *
     * @param minY  the minimum y value (in Java2D coordinate space).
     * @param height  the height available for all labels.
     */
    protected void adjustDownwards(double minY, double height) {
        for (int i = 0; i < this.labels.size() - 1; i++) {
            PieLabelRecord current = getPieLabelRecord(i);
            PieLabelRecord next = getPieLabelRecord(i + 1);
            if (next.getLowerY() < current.getUpperY()) {
                double lowestAllowedY = minY + height - halfHeight(next);
                double stackedBelowCurrent = current.getUpperY() + this.minGap
                        + halfHeight(next);
                next.setAllocatedY(Math.min(lowestAllowedY, stackedBelowCurrent));
            }
        }
    }

    /**
     * Any labels that are overlapping are moved up in an attempt to eliminate
     * the overlaps.
     *
     * @param minY  the minimum y value (in Java2D coordinate space).
     * @param height  the height available for all labels.
     */
    protected void adjustUpwards(double minY, double height) {
        for (int i = this.labels.size() - 1; i > 0; i--) {
            PieLabelRecord current = getPieLabelRecord(i);
            PieLabelRecord previous = getPieLabelRecord(i - 1);
            if (previous.getUpperY() > current.getLowerY()) {
                double highestAllowedY = minY + halfHeight(previous);
                double stackedAboveCurrent = current.getLowerY() - this.minGap
                        - halfHeight(previous);
                previous.setAllocatedY(Math.max(highestAllowedY, stackedAboveCurrent));
            }
        }
    }

    /**
     * Labels are spaced evenly in the available space in an attempt to
     * eliminate the overlaps.
     *
     * @param minY  the minimum y value (in Java2D coordinate space).
     * @param height  the height available for all labels.
     */
    protected void spreadEvenly(double minY, double height) {
        double cursorY = minY;
        double sumOfLabelHeights = 0.0;
        for (int i = 0; i < this.labels.size(); i++) {
            sumOfLabelHeights += getPieLabelRecord(i).getLabelHeight();
        }
        double gap = height - sumOfLabelHeights;
        if (this.labels.size() > 1) {
            gap = gap / (this.labels.size() - 1);
        }
        for (int i = 0; i < this.labels.size(); i++) {
            PieLabelRecord record = getPieLabelRecord(i);
            cursorY = cursorY + halfHeight(record);
            record.setAllocatedY(cursorY);
            cursorY = cursorY + halfHeight(record) + gap;
        }
    }

    /**
     * Returns half of the height of the given label record. Extracted to
     * remove the repeated {@code getLabelHeight() / 2.0} expression.
     *
     * @param record  the label record.
     *
     * @return Half of the label height (in Java2D units).
     */
    private static double halfHeight(PieLabelRecord record) {
        return record.getLabelHeight() / 2.0;
    }

    /**
     * Sorts the label records into ascending order by y-value.
     */
    public void sort() {
        Collections.sort(this.labels);
    }

    /**
     * Returns a string containing a description of the object for
     * debugging purposes.
     *
     * @return A string.
     */
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < this.labels.size(); i++) {
            result.append(getPieLabelRecord(i).toString()).append("\n");
        }
        return result.toString();
    }

}
