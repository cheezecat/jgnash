/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2013 Craig Cavanaugh
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
package jgnash.engine.jpa;


import jgnash.engine.dao.RecurringDAO;
import jgnash.engine.recurring.Reminder;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

/**
 * Reminder DAO
 *
 * @author Craig Cavanaugh
 */
class JpaRecurringDAO extends AbstractJpaDAO implements RecurringDAO {

    private static final Logger logger = Logger.getLogger(JpaRecurringDAO.class.getName());

    JpaRecurringDAO(final EntityManager entityManager, final boolean isRemote) {
        super(entityManager, isRemote);
    }

    /*
     * @see jgnash.engine.ReminderDAOInterface#getReminderList()
     */
    @Override
    public List<Reminder> getReminderList() {

        try {
            emLock.lock();

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Reminder> cq = cb.createQuery(Reminder.class);
            Root<Reminder> root = cq.from(Reminder.class);
            cq.select(root);

            TypedQuery<Reminder> q = em.createQuery(cq);

            return stripMarkedForRemoval(new ArrayList<>(q.getResultList()));
        } finally {
            emLock.unlock();
        }
    }

    /*
     * @see jgnash.engine.ReminderDAOInterface#addReminder(jgnash.engine.recurring.Reminder)
     */
    @Override
    public boolean addReminder(final Reminder reminder) {
        try {
            emLock.lock();
            em.getTransaction().begin();

            em.persist(reminder);

            em.getTransaction().commit();

            return true;
        } finally {
            emLock.unlock();
        }
    }

    @Override
    public Reminder getReminderByUuid(String uuid) {
        try {
            emLock.lock();

            Reminder reminder = null;

            try {
                reminder = em.find(Reminder.class, uuid);
            } catch (Exception e) {
                logger.info("Did not find Reminder for uuid: " + uuid);
            }

            return reminder;
        } finally {
            emLock.unlock();
        }
    }


    @Override
    public boolean updateReminder(final Reminder reminder) {
        return addReminder(reminder);   // call add, same code
    }

    @Override
    public void refreshReminder(final Reminder reminder) {
        try {
            emLock.lock();
            em.getTransaction().begin();

            em.merge(reminder);

            em.getTransaction().commit();
        } finally {
            emLock.unlock();
        }
    }
}
