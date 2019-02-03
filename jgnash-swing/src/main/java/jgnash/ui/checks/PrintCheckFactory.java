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
package jgnash.ui.checks;

import java.awt.EventQueue;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.Transaction;
import jgnash.engine.checks.CheckLayout;

/**
 * Class that contains the check printing logic.
 *
 * @author Craig Cavanaugh
 */
public class PrintCheckFactory {

    private PrintCheckFactory() {
    }

    /**
     * Print checks
     */
    public static void showDialog() {

        EventQueue.invokeLater(() -> {
            boolean checkNum; // Automatically increment the check number

            TransactionListDialog tld = new TransactionListDialog();

            tld.setVisible(true);

            if (!tld.getReturnStatus() || tld.getPrintableTransactions().isEmpty()) {
                return;
            }

            PrintCheckDialog pcd = new PrintCheckDialog();
            pcd.setVisible(true);

            if (!pcd.getReturnStatus()) {
                return;
            }

            CheckLayout checkLayout = pcd.getCheckLayout();
            int numChecks = checkLayout.getNumberOfChecks();
            int startPosition = pcd.getStartPosition();

            List<Transaction> trans = tld.getPrintableTransactions();

            checkNum = pcd.incrementCheckNumbers();

            int i = 0;
            while (i < trans.size()) {
                Transaction[] pTrans = new Transaction[numChecks];
                for (int j = startPosition; j < numChecks && i < trans.size(); j++) {
                    if (checkNum) {
                        trans.set(i, changeTransNum(trans.get(i)));
                    }
                    pTrans[j] = trans.get(i);
                    i++;
                }
                startPosition = 0; // first iteration is a special case

                PrintableCheckLayout layout = new PrintableCheckLayout(checkLayout);
                layout.print(pTrans); // print the first sheet
            }

        });
    }

    /**
     * Returns the next available check number for a transaction
     *
     * @param t transaction to assign a check number
     * @return next check number to assign
     */
    private static String getNextTransactionNum(Transaction t) {
        String number;

        if (t.getTransactionEntries().size() > 1) {
            number = t.getCommonAccount().getNextTransactionNumber();
        } else {
            number = t.getTransactionEntries().get(0).getDebitAccount().getNextTransactionNumber();
        }

        return number;
    }

    /**
     * Changes and transaction number to the next available in the account and returns the resultant
     *
     * @param transaction transaction to change the check number
     * @return the resultant transaction
     */
    private static Transaction changeTransNum(final Transaction transaction) {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        try {
            Transaction clone = (Transaction) transaction.clone();

            clone.setNumber(getNextTransactionNum(transaction));

            if (engine.removeTransaction(transaction)) {
                engine.addTransaction(clone);
                return clone;
            }
        } catch (CloneNotSupportedException e) {
            Logger.getLogger(PrintCheckFactory.class.getName()).log(Level.SEVERE, null, e);
        }

        return transaction;
    }
}
