package net.sf.ha.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * @author Paul Ferraro
 * @version $Revision$
 */
public final class Driver implements java.sql.Driver
{
	private static final String URL_PREFIX = "jdbc:ha-jdbc:";
	private static final int MAJOR_VERSION = 1;
	private static final int MINOR_VERSION = 0;
	private static final boolean JDBC_COMPLIANT = true;
	
	static
	{
		try
		{
			DriverManager.registerDriver(new Driver());
		}
		catch (SQLException e)
		{
			throw new RuntimeException(e);
		}
	}

	// Maps cluster name -> DatabaseCluster
	private Map databaseClusterMap;
	
	public Driver() throws SQLException
	{
		Set clusterSet = DatabaseClusterManagerFactory.getClusterManager().getClusterSet(this.getClass());
		
		this.databaseClusterMap = new HashMap(clusterSet.size());
		
		Iterator clusters = clusterSet.iterator();
		
		while (clusters.hasNext())
		{
			String clusterName = (String) clusters.next();

			Set databaseSet = DatabaseClusterManagerFactory.getClusterManager().getDatabaseSet(clusterName);
			
			Map driverMap = new HashMap(databaseSet.size());
			
			Iterator databases = databaseSet.iterator();
			
			while (databases.hasNext())
			{
				DriverDatabase database = (DriverDatabase) databases.next();
				
				java.sql.Driver driver = DriverManager.getDriver(database.getUrl());
				driverMap.put(database, driver);
			}
			
			this.databaseClusterMap.put(clusterName, driverMap);
		}
	}
	
	/**
	 * @see java.sql.Driver#getMajorVersion()
	 */
	public int getMajorVersion()
	{
		return MAJOR_VERSION;
	}
	
	/**
	 * @see java.sql.Driver#getMinorVersion()
	 */
	public int getMinorVersion()
	{
		return MINOR_VERSION;
	}
	
	/**
	 * @see java.sql.Driver#jdbcCompliant()
	 */
	public boolean jdbcCompliant()
	{
		return JDBC_COMPLIANT;
	}
	
	private DatabaseCluster getDatabaseCluster(String clusterName)
	{
		return (DatabaseCluster) this.databaseClusterMap.get(clusterName);
	}
	
	private String extractClusterName(String url)
	{
		int index = URL_PREFIX.length();
		
		return (index > url.length()) ? url.substring(index) : null; 
	}
	
	private boolean acceptsClusterName(String clusterName)
	{
		return this.databaseClusterMap.keySet().contains(clusterName);
	}
	
	/**
	 * @see java.sql.Driver#acceptsURL(java.lang.String)
	 */
	public boolean acceptsURL(String url)
	{
		String clusterName = this.extractClusterName(url);
		
		return (clusterName != null) && this.acceptsClusterName(clusterName);
	}
	
	/**
	 * @see java.sql.Driver#connect(java.lang.String, java.util.Properties)
	 */
	public Connection connect(String url, Properties properties) throws SQLException
	{
		String clusterName = this.extractClusterName(url);
		
		if ((clusterName == null) || !acceptsClusterName(clusterName))
		{
			return null;
		}
		
		DatabaseCluster databaseCluster = this.getDatabaseCluster(clusterName);
		
		DriverOperation operation = new DriverOperation()
		{
			public Object execute(DriverDatabase database, Driver driver) throws SQLException
			{
				return driver.connect(database.getUrl(), database.getProperties());
			}
		};
		
		return new ConnectionProxy(databaseCluster, databaseCluster.executeWrite(operation));
	}
	
	/**
	 * @see java.sql.Driver#getPropertyInfo(java.lang.String, java.util.Properties)
	 */
	public DriverPropertyInfo[] getPropertyInfo(String url, Properties properties) throws SQLException
	{
		String clusterName = this.extractClusterName(url);
		
		if ((clusterName == null) || !acceptsClusterName(clusterName))
		{
			return null;
		}
		
		DatabaseCluster databaseCluster = this.getDatabaseCluster(clusterName);
		
		DriverOperation operation = new DriverOperation()
		{
			public Object execute(DriverDatabase database, Driver driver) throws SQLException
			{
				return driver.getPropertyInfo(database.getUrl(), database.getProperties());
			}
		};
		
		return (DriverPropertyInfo[]) databaseCluster.executeRead(operation);
	}
}
