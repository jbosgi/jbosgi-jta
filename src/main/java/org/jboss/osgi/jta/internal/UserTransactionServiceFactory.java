/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.osgi.jta.internal;

//$Id$

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.transaction.UserTransaction;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

/**
 * A service factory for the user transaction
 * 
 * @author thomas.diesler@jboss.com
 * @since 28-Oct-2009
 */
public class UserTransactionServiceFactory implements ServiceFactory
{
   public Object getService(Bundle bundle, ServiceRegistration registration)
   {
      ClassLoader classLoader = getClass().getClassLoader();
      ClassLoader ctxLoader = Thread.currentThread().getContextClassLoader();
      try
      {
         Thread.currentThread().setContextClassLoader(classLoader);
         UserTransactionProxy handler = new UserTransactionProxy(classLoader);
         return Proxy.newProxyInstance(classLoader, new Class[] { UserTransaction.class }, handler);
      }
      finally
      {
         Thread.currentThread().setContextClassLoader(ctxLoader);
      }
   }

   public void ungetService(Bundle bundle, ServiceRegistration registration, Object service)
   {
      // nothing to do 
   }

   /**
    * Wraps every invocation on the UserTransaction to make the context class loader available 
    */
   class UserTransactionProxy implements InvocationHandler
   {
      private UserTransaction tx;
      private ClassLoader classLoader;

      public UserTransactionProxy(ClassLoader classLoader)
      {
         this.tx = com.arjuna.ats.jta.UserTransaction.userTransaction();
         this.classLoader = classLoader;
      }

      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
      {
         ClassLoader ctxLoader = Thread.currentThread().getContextClassLoader();
         try
         {
            Thread.currentThread().setContextClassLoader(classLoader);
            return method.invoke(tx, args);
         }
         finally
         {
            Thread.currentThread().setContextClassLoader(ctxLoader);
         }
      }
   }
}