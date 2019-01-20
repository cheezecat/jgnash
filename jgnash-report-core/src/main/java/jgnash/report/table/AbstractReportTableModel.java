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
package jgnash.report.table;

import jgnash.engine.CurrencyNode;
import jgnash.resource.util.ResourceUtils;
import jgnash.text.CommodityFormat;
import jgnash.time.DateUtils;
import jgnash.util.LogUtil;
import jgnash.util.NotNull;

import javax.swing.table.AbstractTableModel;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

/**
 * Base Report model class
 *
 * The report must contain a minimum of one group defined by {@code ColumnStyle.GROUP_NO_HEADER} or
 * {@code ColumnStyle.GROUP}.  If a is not defined/assigned, all rows will be grouped together.
 *
 * TODO, convert to extend a report specific interface
 *
 * @author Craig Cavanaugh
 */
public abstract class AbstractReportTableModel extends AbstractTableModel {

    /**
     * Default group for unassigned rows.
     */
    public static final String DEFAULT_GROUP = "_default_";

    public abstract CurrencyNode getCurrency();

    public abstract ColumnStyle getColumnStyle(int columnIndex);

    public abstract ColumnHeaderStyle getColumnHeaderStyle(int columnIndex);

    /**
     * Returns the legend for the grand total
     *
     * @return report name
     */
    public String getGrandTotalLegend() {
        return ResourceUtils.getString("Word.Total");
    }

    /**
     * Returns the general label for the group footer
     *
     * @return footer label
     */
    public String getGroupFooterLabel() {
        return ResourceUtils.getString("Word.Subtotal");
    }

    /** Return true if the column should be fixed width
     *
     * @param columnIndex column to verify
     * @return true if fixed width
     */
    public boolean isColumnFixedWidth(final int columnIndex) {
        return false;
    }

    private int getGroupColumn() {
        int column = -1;

        for (int i = 0; i < getColumnCount(); i++) {
            if (getColumnStyle(i) == ColumnStyle.GROUP) {
                column = i;
                break;
            }
        }
        return column;
    }

    public String getGroup(final int row) {
        String group = DEFAULT_GROUP;   // default group if row is not assigned

        for (int i = 0; i < getColumnCount(); i++) {
            if (getColumnStyle(i) == ColumnStyle.GROUP || getColumnStyle(i) == ColumnStyle.GROUP_NO_HEADER) {
                group = getValueAt(row, i).toString();
            }
        }

        return group;
    }

    @NotNull
    public int[] getColumnsToHide() {
        return new int[0];  // return an empty array by default
    }

    /**
     * Determines if a column is visible.
     *
     * @param column column to check
     * @return true if the column data is visible in the report
     */
    public boolean isColumnVisible(final int column) {
        boolean result = true;

        final ColumnStyle columnStyle = getColumnStyle(column);

        if (columnStyle == ColumnStyle.GROUP_NO_HEADER || columnStyle == ColumnStyle.GROUP) {
            result = false;
        } else {
            int[] columnsToHide = getColumnsToHide();

            for (int i : columnsToHide) {
                if (column == i) {
                    result = false;
                    break;
                }
            }
        }

        return result;
    }

    /**
     * Determines if a column should be summed
     *
     * @param columnIndex column index to check
     * @return true if the column values should be summed
     */
    public boolean isColumnSummed(final int columnIndex) {
        return getColumnStyle(columnIndex) == ColumnStyle.BALANCE_WITH_SUM ||
                getColumnStyle(columnIndex) == ColumnStyle.BALANCE_WITH_SUM_AND_GLOBAL
                || getColumnStyle(columnIndex) == ColumnStyle.AMOUNT_SUM;
    }

    /**
     * Returns the longest value for the specified column
     *
     * @param columnIndex column to check
     * @return String representing the longest value
     */
    public String getColumnPrototypeValueAt(final int columnIndex) {

        final int groupColumn = getGroupColumn();

        String longest = "";

        if (getColumnClass(columnIndex).isAssignableFrom(BigDecimal.class)) {

            // does the column need to be summed
            boolean sum = isColumnSummed(columnIndex);

            // mapping to sum groups
            final HashMap<String, BigDecimal> groupMap = new HashMap<>();

            // number format to use
            NumberFormat nf;

            switch (getColumnStyle(columnIndex)) {
                case QUANTITY:
                    nf = Formats.getQuantityFormat();
                    break;
                case PERCENTAGE:
                    nf = Formats.getPercentageFormat();
                    break;
                default:
                    nf = CommodityFormat.getFullNumberFormat(getCurrency());
                    break;
            }

            BigDecimal total = BigDecimal.ZERO; // end total

            for (int i = 0; i < getRowCount(); i++) {
                final BigDecimal value = (BigDecimal) getValueAt(i, columnIndex);

                if (value != null) {

                    final String prototype = nf.format(value);

                    // look and individual values
                    if (prototype.length() > longest.length()) {
                        longest = prototype;
                    }

                    if (sum) {
                        total = total.add(value); // global value
                    }

                    if (groupColumn >= 0) {
                        final String group = (String) getValueAt(i, groupColumn);
                        final BigDecimal o = groupMap.get(group);

                        groupMap.put(group, o != null ? o.add(value) : value);
                    }
                }
            }

            if (sum) {
                if (nf.format(total).length() > longest.length()) { // look at column total
                    longest = nf.format(total);
                }

                for (final BigDecimal value : groupMap.values()) {
                    if (nf.format(value).length() > longest.length()) { // look at group totals
                        longest = nf.format(value);
                    }
                }
            }

        } else if (getColumnStyle(columnIndex) == ColumnStyle.STRING) {
            for (int i = 0; i < getRowCount(); i++) {
                final String val = (String) getValueAt(i, columnIndex);

                if (val != null && val.length() > longest.length()) {
                    longest = val;
                }
            }
        } else if (getColumnStyle(columnIndex) == ColumnStyle.SHORT_DATE) {
            final DateTimeFormatter dateTimeFormatter = DateUtils.getShortDateFormatter();

            for (int i = 0; i < getRowCount(); i++) {
                try {
                    final LocalDate date = (LocalDate) getValueAt(i, columnIndex);

                    if (date != null) {
                        final String val = dateTimeFormatter.format(date);
                        if (val.length() > longest.length()) {
                            longest = val;
                        }
                    }
                } catch (final IllegalArgumentException e) {
                    LogUtil.logSevere(AbstractReportTableModel.class, e);
                }
            }
        } else if (getColumnStyle(columnIndex) == ColumnStyle.TIMESTAMP) {
            final DateTimeFormatter dateTimeFormatter = DateUtils.getShortDateTimeFormatter();

            for (int i = 0; i < getRowCount(); i++) {
                try {
                    final LocalDateTime localDateTime = (LocalDateTime) getValueAt(i, columnIndex);

                    if (localDateTime != null) {
                        final String val = dateTimeFormatter.format(localDateTime);
                        if (val.length() > longest.length()) {
                            longest = val;
                        }
                    }
                } catch (final IllegalArgumentException e) {
                    LogUtil.logSevere(AbstractReportTableModel.class, e);
                }
            }
        }

        return longest;
    }
}
