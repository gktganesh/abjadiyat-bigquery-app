package com.abjadiyat.util;


import com.abjadiyat.entity.PurchaseRecords;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;

public class HibernateUtil {
    private static SessionFactory sessionFactory;

    static {
        try {
            Configuration configObj = new Configuration();
            configObj.configure();
            configObj.addAnnotatedClass(PurchaseRecords.class);

            ServiceRegistry serviceRegistryObj = new StandardServiceRegistryBuilder().applySettings(
                    configObj.getProperties()).build();

            sessionFactory = configObj.buildSessionFactory(serviceRegistryObj);
        } catch (HibernateException ex) {
            System.err.println("Initial SessionFactory creation failed." + ex);
            throw new ExceptionInInitializerError(ex);
        }

    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }
}