/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2019 Craig Cavanaugh
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package jgnash.ui.components;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

/**
 * Renderer to show a checkbox in ListCellRenderer
 * <p>
 * Usage: <code>jList.setCellRenderer(new CheckListCellRenderer(jList.getCellRenderer())); </code>
 *
 * @author Craig Cavanaugh
 */
public class CheckListCellRenderer<E> extends JPanel implements ListCellRenderer<E> {
    private final ListCellRenderer<E> renderer;

    private final JCheckBox checkBox = new JCheckBox();

    public CheckListCellRenderer(final ListCellRenderer<E> renderer) {
        this.renderer = renderer;
        setLayout(new BorderLayout());
        setOpaque(false);
        checkBox.setOpaque(false);
    }

    @Override
    public Component getListCellRendererComponent(final JList<? extends E> list, final E value, final int index, final boolean isSelected, final boolean cellHasFocus) {
        final Component component = renderer.getListCellRendererComponent(list, value, index, false, cellHasFocus);

        checkBox.setSelected(isSelected);
        removeAll();
        add(checkBox, BorderLayout.WEST);
        add(component, BorderLayout.CENTER);
        return this;
    }
}
