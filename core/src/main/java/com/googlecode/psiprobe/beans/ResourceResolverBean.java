/*
 * Licensed under the GPL License. You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * THIS PACKAGE IS PROVIDED "AS IS" AND WITHOUT ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING,
 * WITHOUT LIMITATION, THE IMPLIED WARRANTIES OF MERCHANTIBILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE.
 */
package com.googlecode.psiprobe.beans;

import com.googlecode.psiprobe.AbstractTomcatContainer;
import com.googlecode.psiprobe.model.ApplicationResource;
import com.googlecode.psiprobe.model.DataSourceInfo;

import org.apache.catalina.Context;
import org.apache.catalina.Server;
import org.apache.catalina.core.StandardServer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.modeler.Registry;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 * 
 * @author Vlad Ilyushchenko
 * @author Andy Shapoval
 * @author Mark Lewis
 * @author Henry Caballero
 */
public class ResourceResolverBean implements ResourceResolver {

  private Log logger = LogFactory.getLog(getClass());

  /**
   * The default resource prefix for JNDI objects in the global scope: <code>java:</code>.
   */
  public static final String DEFAULT_GLOBAL_RESOURCE_PREFIX = "";

  /**
   * The default resource prefix for objects in a private application scope:
   * <code>java:comp/env/</code>.
   */
  public static final String DEFAULT_RESOURCE_PREFIX = DEFAULT_GLOBAL_RESOURCE_PREFIX
      + "java:comp/env/";

  private List datasourceMappers;

  public List getApplicationResources() throws NamingException {
    logger.info("Reading GLOBAL resources");
    List resources = new ArrayList();

    MBeanServer server = getMBeanServer();
    if (server != null) {
      try {
        Set dsNames =
            server.queryNames(new ObjectName("Catalina:type=Resource,resourcetype=Global,*"), null);
        for (Iterator it = dsNames.iterator(); it.hasNext();) {
          ObjectName objectName = (ObjectName) it.next();
          ApplicationResource resource = new ApplicationResource();

          logger.info("reading resource: " + objectName);
          resource.setName(getStringAttribute(server, objectName, "name"));
          resource.setType(getStringAttribute(server, objectName, "type"));
          resource.setScope(getStringAttribute(server, objectName, "scope"));
          resource.setAuth(getStringAttribute(server, objectName, "auth"));
          resource.setDescription(getStringAttribute(server, objectName, "description"));

          lookupResource(resource, true, true);

          resources.add(resource);
        }
      } catch (Exception e) {
        logger.error("There was an error querying JMX server:", e);
      }
    }
    return resources;
  }

  public synchronized List getApplicationResources(Context context,
      ContainerWrapperBean containerWrapper) throws NamingException {

    List resourceList = new ArrayList();

    boolean contextAvailable = containerWrapper.getTomcatContainer().getAvailable(context);
    if (contextAvailable) {

      logger.info("Reading CONTEXT " + context.getName());

      boolean contextBound = false;

      try {
        ((AbstractTomcatContainer) containerWrapper.getTomcatContainer()).bindToContext(context);
        contextBound = true;
      } catch (NamingException e) {
        logger.error("Cannot bind to context. useNaming=false ?");
        logger.debug("  Stack trace:", e);
      }

      try {
        containerWrapper.getTomcatContainer().addContextResource(context, resourceList,
            contextBound);

        containerWrapper.getTomcatContainer().addContextResourceLink(context, resourceList,
            contextBound);
        for (int i = 0; i < resourceList.size(); i++) {
          lookupResource((ApplicationResource) resourceList.get(i), contextBound, false);
        }

      } finally {
        if (contextBound) {
          ((AbstractTomcatContainer) containerWrapper.getTomcatContainer())
              .unbindFromContext(context);
        }
      }
    }

    return resourceList;
  }



  public void lookupResource(ApplicationResource resource, boolean contextBound, boolean global) {
    DataSourceInfo dataSourceInfo = null;
    if (contextBound) {
      try {
        javax.naming.Context ctx = !global ? new InitialContext() : getGlobalNamingContext();
        String jndiName = resolveJndiName(resource.getName(), global);
        Object o = ctx.lookup(jndiName);
        resource.setLookedUp(true);
        for (Iterator it = datasourceMappers.iterator(); it.hasNext();) {
          DatasourceAccessor accessor = (DatasourceAccessor) it.next();
          dataSourceInfo = accessor.getInfo(o);
          if (dataSourceInfo != null) {
            break;
          }
        }

      } catch (Throwable e) {
        resource.setLookedUp(false);
        dataSourceInfo = null;
        logger.error("Failed to lookup: " + resource.getName(), e);
        //
        // make sure we always re-throw ThreadDeath
        //
        if (e instanceof ThreadDeath) {
          throw (ThreadDeath) e;
        }
      }
    } else {
      resource.setLookedUp(false);
    }

    /*
     * Tomcat 5.0.x DBCP datasources would have URL set to null if they incorrectly configured so we
     * need to deal with this little feature
     */
    if (dataSourceInfo != null && dataSourceInfo.getJdbcURL() == null) {
      resource.setLookedUp(false);
    }

    if (resource.isLookedUp() && dataSourceInfo != null) {
      resource.setDataSourceInfo(dataSourceInfo);
    }
  }

  public synchronized boolean resetResource(final Context context, String resourceName,
      ContainerWrapperBean containerWrapper) throws NamingException {

    if (context != null) {
      ((AbstractTomcatContainer) containerWrapper.getTomcatContainer()).bindToContext(context);
    }
    try {
      javax.naming.Context ctx =
          (context != null) ? new InitialContext() : getGlobalNamingContext();
      String jndiName = resolveJndiName(resourceName, (context == null));
      Object o = ctx.lookup(jndiName);
      try {
        for (Iterator it = datasourceMappers.iterator(); it.hasNext();) {
          DatasourceAccessor accessor = (DatasourceAccessor) it.next();
          if (accessor.reset(o)) {
            return true;
          }
        }
        return false;
      } catch (Throwable e) {
        //
        // make sure we always re-throw ThreadDeath
        //
        if (e instanceof ThreadDeath) {
          throw (ThreadDeath) e;
        }
        return false;
      }
    } finally {
      if (context != null) {
        ((AbstractTomcatContainer) containerWrapper.getTomcatContainer())
            .unbindFromContext(context);
      }
    }
  }

  public synchronized DataSource lookupDataSource(final Context context, String resourceName,
      ContainerWrapperBean containerWrapper) throws NamingException {

    if (context != null) {
      ((AbstractTomcatContainer) containerWrapper.getTomcatContainer()).bindToContext(context);
    }
    try {
      javax.naming.Context ctx =
          (context != null) ? new InitialContext() : getGlobalNamingContext();
      String jndiName = resolveJndiName(resourceName, (context == null));
      Object o = ctx.lookup(jndiName);

      if (o instanceof DataSource) {
        return (DataSource) o;
      } else {
        return null;
      }
    } finally {
      if (context != null) {
        ((AbstractTomcatContainer) containerWrapper.getTomcatContainer())
            .unbindFromContext(context);
      }
    }
  }

  public List getDatasourceMappers() {
    return datasourceMappers;
  }

  public void setDatasourceMappers(List datasourceMappers) {
    this.datasourceMappers = datasourceMappers;
  }

  public boolean supportsPrivateResources() {
    return true;
  }

  public boolean supportsGlobalResources() {
    return true;
  }

  public boolean supportsDataSourceLookup() {
    return true;
  }

  public MBeanServer getMBeanServer() {
    return new Registry().getMBeanServer();
  }

  /**
   * Resolves a JNDI resource name by prepending the scope-appropriate prefix.
   *
   * @param global whether to use the global prefix
   * @param name the JNDI name of the resource
   * 
   * @return the JNDI resource name with the prefix appended
   *
   * @see #DEFAULT_GLOBAL_RESOURCE_PREFIX
   * @see #DEFAULT_RESOURCE_PREFIX
   */
  protected static String resolveJndiName(String name, boolean global) {
    return (global ? DEFAULT_GLOBAL_RESOURCE_PREFIX : DEFAULT_RESOURCE_PREFIX) + name;
  }

  private String getStringAttribute(MBeanServer server, ObjectName objectName, String attributeName) {
    try {
      return (String) server.getAttribute(objectName, attributeName);
    } catch (Exception e) {
      logger.error("Error getting attribute '" + attributeName + "' from '" + objectName + "'", e);
      return null;
    }
  }

  /**
   * Returns the Server's global naming context
   *
   * @return the global JNDI context
   */
  protected javax.naming.Context getGlobalNamingContext() {

    javax.naming.Context globalContext = null;
    MBeanServer mBeanServer = getMBeanServer();
    if (mBeanServer != null) {
      try {
        ObjectName name = new ObjectName("Catalina:type=Server");
        Server server = (Server) mBeanServer.getAttribute(name, "managedResource");
        // getGlobalNamingContext() was added to Server interface in Tomcat 7.0.11
        if (server instanceof StandardServer) {
          globalContext = ((StandardServer) server).getGlobalNamingContext();
        }
      } catch (Exception e) {
        logger.error("There was an error getting globalContext through JMX server:", e);
      }
    }

    return globalContext;
  }
}
