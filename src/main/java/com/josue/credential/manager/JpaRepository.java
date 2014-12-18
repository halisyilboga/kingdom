/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.josue.credential.manager;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

/**
 *
 * @author Josue
 *
 */
//TODO change to REQUIRED
@Transactional(Transactional.TxType.REQUIRES_NEW)
public class JpaRepository {

    @PersistenceContext
    private EntityManager em;

    public <T> void create(T entity) {
        em.persist(entity);
    }

    public <T> T edit(T entity) {
        return em.merge(entity);
    }

    public <T> void remove(T entity) {
        em.remove(em.merge(entity));
    }

    public <T> T find(Class<T> clazz, Object id) {
        return em.find(clazz, id);
    }

    public <T> List<T> findAll(Class<T> clazz) {
        javax.persistence.criteria.CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
        cq.select(cq.from(clazz));
        return em.createQuery(cq).getResultList();
    }

    public <T> List<T> findRange(Class<T> clazz, int[] range) {
        javax.persistence.criteria.CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
        cq.select(cq.from(clazz));
        javax.persistence.Query q = em.createQuery(cq);
        q.setMaxResults(range[1] - range[0] + 1);
        q.setFirstResult(range[0]);
        return q.getResultList();
    }

    public <T> int count(Class<T> clazz) {
        javax.persistence.criteria.CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
        javax.persistence.criteria.Root<T> rt = cq.from(clazz);
        cq.select(em.getCriteriaBuilder().count(rt));
        javax.persistence.Query q = em.createQuery(cq);
        return ((Long) q.getSingleResult()).intValue();
    }

}