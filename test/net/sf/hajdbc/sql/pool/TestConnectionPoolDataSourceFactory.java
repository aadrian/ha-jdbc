/*
 * HA-JDBC: High-Availability JDBC
 * Copyright (c) 2004-2008 Paul Ferraro
 * 
 * This library is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Lesser General Public License as published by the 
 * Free Software Foundation; either version 2.1 of the License, or (at your 
 * option) any later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or 
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License 
 * for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation, 
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 * 
 * Contact: ferraro@users.sourceforge.net
 */
package net.sf.hajdbc.sql.pool;

import java.lang.reflect.Proxy;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Hashtable;
import java.util.Properties;
import java.util.prefs.Preferences;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.naming.spi.ObjectFactory;
import javax.sql.ConnectionPoolDataSource;

import net.sf.hajdbc.local.LocalStateManager;
import net.sf.hajdbc.sql.MockInitialContextFactory;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @author Paul Ferraro
 *
 */
public class TestConnectionPoolDataSourceFactory implements ObjectFactory
{
	private ConnectionPoolDataSourceFactory factory = new ConnectionPoolDataSourceFactory();
	private Context context;
	
	@BeforeClass
	protected void setUp() throws Exception
	{
		Preferences.userNodeForPackage(LocalStateManager.class).put("test-pool-datasource-cluster", "datasource1,datasource2");
		
		Properties properties = new Properties();
		
		properties.setProperty(Context.INITIAL_CONTEXT_FACTORY, MockInitialContextFactory.class.getName());
		
		this.context = new InitialContext(properties);
		
		Reference reference = new Reference(ConnectionPoolDataSourceFactory.class.getName(), MockConnectionPoolDataSourceFactory.class.getName(), null);
		
		this.context.bind("datasource1", reference);
		this.context.bind("datasource2", reference);
		
		this.context.bind("datasource", new ConnectionPoolDataSourceReference("test-pool-datasource-cluster"));
	}

	@AfterClass
	protected void tearDown() throws Exception
	{
		this.context.unbind("datasource");
		
		this.context.unbind("datasource1");
		this.context.unbind("datasource2");
		
		Preferences.userNodeForPackage(LocalStateManager.class).remove("test-pool-datasource-cluster");
	}

	@DataProvider(name = "factory")
	Object[][] objectInstanceProvider()
	{
		return new Object[][] {
			new Object[] { null, null, null, null },
			new Object[] { new Object(), null, null, null },
			new Object[] { new Reference(ConnectionPoolDataSource.class.getName(), new StringRefAddr("cluster", "test-pool-datasource-cluster")), null, null, null },
			new Object[] { new Reference(ConnectionPoolDataSource.class.getName(), new StringRefAddr("cluster", null)), null, null, null },
			new Object[] { new Reference(Driver.class.getName(), new StringRefAddr("cluster", "test-pool-datasource-cluster")), null, null, null },
			new Object[] { new Reference(ConnectionPoolDataSource.class.getName()), null, null, null },
			new Object[] { new Reference(ConnectionPoolDataSource.class.getName(), new StringRefAddr("cluster", "invalid-cluster")), null, null, null }
		};
	}
	
	/**
	 * @see javax.naming.spi.ObjectFactory#getObjectInstance(java.lang.Object, javax.naming.Name, javax.naming.Context, java.util.Hashtable)
	 */
	@Test(dataProvider = "factory")
	public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment)
	{
		try
		{
			Object result = this.factory.getObjectInstance(obj, name, nameCtx, environment);
			
			if ((obj == null) || !Reference.class.isInstance(obj))
			{
				assert result == null;
				
				return result;
			}
			
			Reference reference = (Reference) obj;
			
			if (!reference.getClassName().equals(ConnectionPoolDataSource.class.getName()))
			{
				assert result == null;
				
				return result;
			}
			
			RefAddr addr = reference.get("cluster");
			
			if ((addr == null) || (addr.getContent() == null))
			{
				assert result == null;
				
				return result;
			}
			
			String id = (String) addr.getContent();
			
			if ((id == null) || !id.equals("test-pool-datasource-cluster"))
			{
				assert result == null;
			}
			else
			{
				assert result != null;
				assert Proxy.isProxyClass(result.getClass()) : result.getClass().getName();
			}
			
			return result;
		}
		catch (SQLException e)
		{
			assert ((Reference) obj).get("cluster").getContent().equals("invalid-cluster");
			
			return null;
		}
		catch (Exception e)
		{
			assert false : e;
		
			return null;
		}
	}
}