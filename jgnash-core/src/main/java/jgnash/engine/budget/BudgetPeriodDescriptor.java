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
package jgnash.engine.budget;

import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import jgnash.time.Period;
import jgnash.time.DateUtils;
import jgnash.util.NotNull;
import jgnash.resource.util.ResourceUtils;

/**
 * Immutable descriptor for a budget period.
 *
 * @author Craig Cavanaugh
 */
public class BudgetPeriodDescriptor implements Comparable<BudgetPeriodDescriptor> {

    private int hash = 0;

    private static final int ONE_WEEK_INCREMENT = 6;

    private static final int TWO_WEEK_INCREMENT = 13;

    /**
     * The starting period (Day of the year).
     */
    private int startPeriod;

    private int endPeriod;

    private LocalDate startDate;

    private LocalDate endDate;

    final private String periodDescription;

    final private Period budgetPeriod;

    final private int budgetYear;

    BudgetPeriodDescriptor(final LocalDate budgetDate, final int budgetYear, final Period budgetPeriod) {
        Objects.requireNonNull(budgetPeriod);
        Objects.requireNonNull(budgetDate);

        this.budgetYear = budgetYear;
        this.budgetPeriod = budgetPeriod;
        this.startPeriod = budgetDate.getDayOfYear() - 1;   // zero based index vs. 1 based day of year

        /* Calendar day 1 is 1.  Need to add 1 to get correct dates */
        startDate = LocalDate.ofYearDay(budgetYear, startPeriod + 1);

        switch (budgetPeriod) {
            case DAILY:
                endDate = startDate;
                endPeriod = startPeriod;

                periodDescription = ResourceUtils.getString("Pattern.NumericDate", DateUtils.asDate(startDate));
                break;
            case WEEKLY:
                // Check to see if we are working with the start of a 53 week year.  The first week will be truncated
                if (DateUtils.getNumberOfWeeksInYear(budgetYear) == DateUtils.LEAP_WEEK && budgetDate.getYear() < budgetYear) {
                    startPeriod = 0;
                    startDate = budgetDate;

                    endPeriod = (int) ChronoUnit.DAYS.between(startDate, LocalDate.of(budgetYear, Month.JANUARY, 1));
                    endDate = startDate.plusDays(ONE_WEEK_INCREMENT);
                } else {
                    endDate = startDate.plusDays(ONE_WEEK_INCREMENT);
                    endPeriod = Math.min(startPeriod + ONE_WEEK_INCREMENT, BudgetGoal.PERIODS - 1); // need to cap for 53 week years
                }

                periodDescription = ResourceUtils.getString("Pattern.WeekOfYear",
                        DateUtils.getWeekOfTheYear(startDate), budgetYear);
                break;
            case BI_WEEKLY:
                if (DateUtils.getWeekOfTheYear(startDate) != DateUtils.LEAP_WEEK) {
                    endDate = startDate.plusDays(TWO_WEEK_INCREMENT);
                    endPeriod = startPeriod + TWO_WEEK_INCREMENT;
                } else {
                    endDate = startDate.plusDays(ONE_WEEK_INCREMENT);
                    endPeriod = startPeriod + ONE_WEEK_INCREMENT;
                }
                periodDescription = ResourceUtils.getString("Pattern.DateRangeShort", DateUtils.asDate(startDate),
                        DateUtils.asDate(endDate));
                break;
            case MONTHLY:
                final int days = startDate.lengthOfMonth();
                endDate = DateUtils.getLastDayOfTheMonth(startDate);
                endPeriod = startPeriod + days - 1;

                periodDescription = ResourceUtils.getString("Pattern.MonthOfYear", DateUtils.asDate(startDate));
                break;
            case QUARTERLY:
                endDate = DateUtils.getLastDayOfTheQuarter(startDate);
                endPeriod = startPeriod + (int) ChronoUnit.DAYS.between(startDate, endDate);

                periodDescription = ResourceUtils.getString("Pattern.QuarterOfYear",
                        DateUtils.getQuarterNumber(startDate), budgetYear);
                break;
            case YEARLY:
                endDate = DateUtils.getLastDayOfTheYear(startDate);
                endPeriod = startPeriod + (int) ChronoUnit.DAYS.between(startDate, endDate);

                periodDescription = Integer.toString(budgetYear);
                break;
            default:
                endPeriod = startPeriod;
                endDate = LocalDate.now();
                periodDescription = "";
        }

        // Periods especially bi-weekly can get weird, correct ending period if needed.
        if (endPeriod >= BudgetGoal.PERIODS) {
            endPeriod = BudgetGoal.PERIODS - 1;
            endDate = DateUtils.getLastDayOfTheYear(startDate);
        }
    }

    public int getStartPeriod() {
        return startPeriod;
    }

    public int getEndPeriod() {
        return endPeriod;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public String getPeriodDescription() {
        return periodDescription;
    }

    public Period getBudgetPeriod() {
        return budgetPeriod;
    }

    /**
     * Determines if the specified date lies within or inclusive of this descriptor period.
     *
     * @param date date to check
     * @return true if between or inclusive
     */
    public boolean isBetween(final LocalDate date) {
        boolean result = false;

        if (DateUtils.after(date, startDate) && DateUtils.before(date, endDate)) {
            result = true;
        }

        return result;
    }

    @Override
    public String toString() {
        return String.format("BudgetPeriodDescriptor [startDate=%tD, endDate=%tD, periodDescription=%s, budgetPeriod=%s]",
                DateUtils.asDate(startDate), DateUtils.asDate(endDate), periodDescription, budgetPeriod);
    }

    @Override
    public int hashCode() {
        int h = hash;
        if (h == 0) {
            final int prime = 31;

            h = 1;
            h = prime * h + budgetPeriod.hashCode();
            h = prime * h + budgetYear;
            h = prime * h + startPeriod;
            hash = h;
        }
        return h;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (!(obj instanceof BudgetPeriodDescriptor)) {
            return false;
        }

        BudgetPeriodDescriptor other = (BudgetPeriodDescriptor) obj;

        return budgetPeriod == other.budgetPeriod && budgetYear == other.budgetYear && startPeriod == other.startPeriod;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Compares by the start date
     */
    @Override
    public int compareTo(@NotNull final BudgetPeriodDescriptor that) {
        return this.startDate.compareTo(that.getStartDate());
    }
}
