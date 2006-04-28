/*
 * Copyright 2006 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.userinterface.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JPanel;

import net.sf.mzmine.util.format.ValueFormat;

/**
 * 
 */
public class XAxis extends JPanel {

    private static final int MIN_HEIGHT = 25;
    private static final int DEFAULT_STEP_PIXELS = 60;

    private int leftMargin, rightMargin;
    private double min, max;

    private ValueFormat format;

    public XAxis(double min, double max, int leftMargin, int rightMargin,
            ValueFormat format) {

        this.min = min;
        this.max = max;
        this.leftMargin = leftMargin;
        this.rightMargin = rightMargin;
        this.format = format;

        setMinimumSize(new Dimension(0, MIN_HEIGHT));
        setPreferredSize(new Dimension(0, MIN_HEIGHT));

        setBackground(Color.white);

    }

    public void setRange(double min, double max) {
        this.min = min;
        this.max = max;
        repaint();
    }

    /**
     * This method paints the x-axis
     */
    public void paint(Graphics g) {

        super.paint(g);

        // Calc some dimensions with depend on the panel width (in pixels)
        // and plot area (in scans)
        int width = getWidth();

        int lowestMark = Math.max(leftMargin, 20);
        int highestMark = width - Math.max(rightMargin, 20);
        int numberOfMarks = (highestMark - lowestMark) / DEFAULT_STEP_PIXELS;
        // X axis value increment per one pixel
        double xAxisStep = (max - min) / (width - leftMargin - rightMargin - 1);
        int stepIncrement = (highestMark - lowestMark) / numberOfMarks;

        // Draw axis
        g.drawLine(leftMargin - 1, 0, width - rightMargin, 0);

        // Draw tics and numbers
        g.setFont(g.getFont().deriveFont(11.0f));
        String tmps;
        double value;

        for (int xpos = lowestMark; xpos <= highestMark; xpos += stepIncrement) {

            g.drawLine(xpos, 0, xpos, 8);
            value = min + ((xpos - leftMargin) * xAxisStep);
            tmps = format.format(value);
            g.drawBytes(tmps.getBytes(), 0, tmps.length(), xpos
                    - (tmps.length() * 3), 20);

        }
    }

}