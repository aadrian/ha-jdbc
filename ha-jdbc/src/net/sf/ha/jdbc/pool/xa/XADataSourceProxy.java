package net.sf.ha.jdbc.pool.xa;

import java.sql.SQLException;

import javax.sql.XAConnection;
import javax.sql.XADataSource;

import net.sf.ha.jdbc.DataSourceDatabase;
import net.sf.ha.jdbc.DatabaseCluster;
import net.sf.ha.jdbc.pool.ConnectionPoolDataSourceProxy;

/**
 * @author Paul Ferraro
 * @version $Revision$
 */
public class XADataSourceProxy extends ConnectionPoolDataSourceProxy implements XADataSource
{
	private DatabaseCluster databaseCluster;

	public DatabaseCluster getDatabaseCluster()
	{
		return this.databaseCluster;
	}
	
	public void setDatabaseManager(DatabaseCluster databaseCluster)
	{
		this.databaseCluster = databaseCluster;
	}

	/**
	 * @see javax.sql.XADataSource#getXAConnection()
	 */
	public XAConnection getXAConnection() throws SQLException
	{
		XADataSourceOperation operation = new XADataSourceOperation()
		{
			public Object execute(DataSourceDatabase database, XADataSource dataSource) throws SQLException
			{
				return dataSource.getXAConnection();
			}
		};
		
		return new XAConnectionProxy(this.databaseCluster, this.databaseCluster.executeWrite(operation));
	}

	/**
	 * @see javax.sql.XADataSource#getXAConnection(java.lang.String, java.lang.String)
	 */
	public XAConnection getXAConnection(String user, String password) throws SQLException
	{
		XADataSourceOperation operation = new XADataSourceOperation()
		{
			public Object execute(DataSourceDatabase database, XADataSource dataSource) throws SQLException
			{
				return dataSource.getXAConnection(database.getUser(), database.getPassword());
			}
		};
		
		return new XAConnectionProxy(this.databaseCluster, this.databaseCluster.executeWrite(operation));
	}
	
	/**
	 * @see net.sf.ha.jdbc.AbstractDataSourceProxy#getObjectFactoryClass()
	 */
	protected Class getObjectFactoryClass()
	{
		return XADataSourceProxy.class;
	}
}
