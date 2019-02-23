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
package jgnash.uifx.report;

import jgnash.report.pdf.Report;
import jgnash.report.table.AbstractReportTableModel;
import jgnash.resource.util.ResourceUtils;
import jgnash.time.DateUtils;
import jgnash.time.Period;
import jgnash.uifx.control.DatePickerEx;
import jgnash.uifx.report.pdf.ReportController;
import jgnash.uifx.util.JavaFXUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;

/**
 * Profit Loss Report Controller
 *
 * @author Craig Cavanaugh
 */
public class ProfitLossReportController2 implements ReportController {

    @FXML
    public ComboBox<Period> resolutionComboBox;

    @FXML
    public ComboBox sortOrderComboBox;

    @FXML
    public CheckBox showLongNamesCheckBox;

    @FXML
    private DatePickerEx startDatePicker;

    @FXML
    private DatePickerEx endDatePicker;

    @FXML
    private CheckBox hideZeroBalanceAccounts;

    private static final String HIDE_ZERO_BALANCE = "hideZeroBalance";

    private static final String PERIOD = "period";

    private static final String MONTHS = "months";

    private static final String SHOW_FULL_ACCOUNT_PATH = "showFullAccountPath";

    private ProfitLossReport report = new ProfitLossReport();

    private Runnable refreshRunnable = null;

    public ProfitLossReportController2() {
        super();
    }

    private Preferences getPreferences() {
        return Preferences.userNodeForPackage(getClass()).node(getClass().getSimpleName());
    }

    @FXML
    private void initialize() {
        final Preferences preferences = getPreferences();

        hideZeroBalanceAccounts.setSelected(preferences.getBoolean(HIDE_ZERO_BALANCE, true));

        startDatePicker.setValue(LocalDate.now().minusMonths(preferences.getInt(MONTHS, 4) - 1));

        startDatePicker.valueProperty().addListener((observable, oldValue, newValue) -> handleReportRefresh());

        endDatePicker.valueProperty().addListener((observable, oldValue, newValue) -> handleReportRefresh());

        hideZeroBalanceAccounts.onActionProperty().setValue(event -> handleReportRefresh());

        showLongNamesCheckBox.setSelected(preferences.getBoolean(SHOW_FULL_ACCOUNT_PATH, false));

        final Period lastPeriod = Period.values()[preferences.getInt(PERIOD, Period.QUARTERLY.ordinal())];

        resolutionComboBox.getItems().add(Period.MONTHLY);
        resolutionComboBox.getItems().add(Period.QUARTERLY);
        resolutionComboBox.getItems().add(Period.YEARLY);

        resolutionComboBox.setValue(lastPeriod);

        resolutionComboBox.valueProperty().addListener((observable, oldValue, newValue) ->
                JavaFXUtils.runLater(ProfitLossReportController2.this::refreshReport));

        showLongNamesCheckBox.selectedProperty().addListener((observable, oldValue, newValue) ->
                JavaFXUtils.runLater(ProfitLossReportController2.this::refreshReport));

        // boot the report generation
        JavaFXUtils.runLater(this::refreshReport);
    }

    @Override
    public void setRefreshRunnable(final Runnable runnable) {
        refreshRunnable = runnable;
    }

    @Override
    public void getReport(final Consumer<Report> reportConsumer) {
        reportConsumer.accept(report);
    }

    @Override
    public void refreshReport() {
        handleReportRefresh();
    }


    private void handleReportRefresh() {

        final Preferences preferences = getPreferences();

        preferences.putBoolean(HIDE_ZERO_BALANCE, hideZeroBalanceAccounts.isSelected());
        preferences.putInt(MONTHS, DateUtils.getLastDayOfTheMonths(startDatePicker.getValue(),
                endDatePicker.getValue()).size());
        preferences.putInt(PERIOD, resolutionComboBox.getValue().ordinal());
        preferences.putBoolean(SHOW_FULL_ACCOUNT_PATH, showLongNamesCheckBox.isSelected());

        addTable();

        // send notification the report has been updated
        if (refreshRunnable != null) {
            refreshRunnable.run();
        }
    }

    private void addTable() {
        AbstractReportTableModel model = createReportModel();

        report.clearReport();


        try {
            report.addTable(model, ResourceUtils.getString("Title.ProfitLoss"));
            report.addFooter();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    private AbstractReportTableModel createReportModel() {
        report.setReportPeriod(resolutionComboBox.getValue());
        report.setShowFullAccountPath(showLongNamesCheckBox.isSelected());

        return report.createReportModel(startDatePicker.getValue(), endDatePicker.getValue(),
                hideZeroBalanceAccounts.isSelected());
    }

    @FXML
    public void handleRefresh() {
        JavaFXUtils.runLater(this::refreshReport);
    }
}
