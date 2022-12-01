/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.transaction.xa;

import com.atomikos.jdbc.AtomikosDataSourceBean;
import com.zaxxer.hikari.HikariDataSource;
import lombok.SneakyThrows;
import org.apache.shardingsphere.infra.database.type.DatabaseType;
import org.apache.shardingsphere.infra.database.type.DatabaseTypeFactory;
import org.apache.shardingsphere.transaction.core.ResourceDataSource;
import org.apache.shardingsphere.transaction.api.TransactionType;
import org.apache.shardingsphere.transaction.xa.fixture.DataSourceUtils;
import org.apache.shardingsphere.transaction.xa.jta.datasource.XATransactionDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.transaction.Transaction;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class XAShardingSphereTransactionManagerTest {
    
    private final XAShardingSphereTransactionManager xaTransactionManager = new XAShardingSphereTransactionManager();
    
    @Before
    public void setUp() {
        Map<String, ResourceDataSource> resourceDataSources = createResourceDataSources(DatabaseTypeFactory.getInstance("H2"));
        Map<String, DatabaseType> databaseTypes = createDatabaseTypes(DatabaseTypeFactory.getInstance("H2"));
        xaTransactionManager.init(databaseTypes, resourceDataSources, "Atomikos");
    }
    
    @After
    public void tearDown() throws Exception {
        xaTransactionManager.close();
    }
    
    @Test
    public void assertGetTransactionType() {
        assertThat(xaTransactionManager.getTransactionType(), is(TransactionType.XA));
    }
    
    @Test
    public void assertRegisterXADataSource() {
        Map<String, XATransactionDataSource> cachedXADataSourceMap = getCachedDataSources();
        assertThat(cachedXADataSourceMap.size(), is(3));
    }
    
    @Test
    public void assertIsInTransaction() {
        assertFalse(xaTransactionManager.isInTransaction());
        xaTransactionManager.begin();
        assertTrue(xaTransactionManager.isInTransaction());
        xaTransactionManager.commit(false);
    }
    
    @Test
    public void assertGetConnection() throws SQLException {
        xaTransactionManager.begin();
        Connection actual1 = xaTransactionManager.getConnection("sharding_db", "ds_0");
        Connection actual2 = xaTransactionManager.getConnection("sharding_db", "ds_1");
        Connection actual3 = xaTransactionManager.getConnection("sharding_db", "ds_2");
        assertThat(actual1, instanceOf(Connection.class));
        assertThat(actual2, instanceOf(Connection.class));
        assertThat(actual3, instanceOf(Connection.class));
        xaTransactionManager.commit(false);
    }
    
    @Test
    public void assertGetConnectionOfNestedTransaction() throws SQLException {
        ThreadLocal<Map<Transaction, Connection>> transactions = getEnlistedTransactions(getCachedDataSources().get("sharding_db.ds_1"));
        xaTransactionManager.begin();
        assertTrue(transactions.get().isEmpty());
        xaTransactionManager.getConnection("sharding_db", "ds_1");
        assertThat(transactions.get().size(), is(1));
        executeNestedTransaction(transactions);
        assertThat(transactions.get().size(), is(1));
        xaTransactionManager.commit(false);
        assertTrue(transactions.get().isEmpty());
    }
    
    private void executeNestedTransaction(final ThreadLocal<Map<Transaction, Connection>> transactions) throws SQLException {
        xaTransactionManager.begin();
        xaTransactionManager.getConnection("sharding_db", "ds_1");
        assertThat(transactions.get().size(), is(2));
        xaTransactionManager.commit(false);
        assertThat(transactions.get().size(), is(1));
    }
    
    @Test
    public void assertClose() throws Exception {
        xaTransactionManager.close();
        Map<String, XATransactionDataSource> cachedSingleXADataSourceMap = getCachedDataSources();
        assertTrue(cachedSingleXADataSourceMap.isEmpty());
    }
    
    @Test
    public void assertCommit() {
        xaTransactionManager.begin();
        assertTrue(xaTransactionManager.isInTransaction());
        xaTransactionManager.commit(false);
        assertFalse(xaTransactionManager.isInTransaction());
    }
    
    @Test
    public void assertRollback() {
        xaTransactionManager.begin();
        assertTrue(xaTransactionManager.isInTransaction());
        xaTransactionManager.rollback();
        assertFalse(xaTransactionManager.isInTransaction());
    }
    
    @SneakyThrows(ReflectiveOperationException.class)
    @SuppressWarnings("unchecked")
    private Map<String, XATransactionDataSource> getCachedDataSources() {
        Field field = xaTransactionManager.getClass().getDeclaredField("cachedDataSources");
        field.setAccessible(true);
        return (Map<String, XATransactionDataSource>) field.get(xaTransactionManager);
    }
    
    @SneakyThrows(ReflectiveOperationException.class)
    @SuppressWarnings("unchecked")
    private ThreadLocal<Map<Transaction, Connection>> getEnlistedTransactions(final XATransactionDataSource transactionDataSource) {
        Field field = transactionDataSource.getClass().getDeclaredField("enlistedTransactions");
        field.setAccessible(true);
        return (ThreadLocal<Map<Transaction, Connection>>) field.get(transactionDataSource);
    }
    
    private Map<String, ResourceDataSource> createResourceDataSources(final DatabaseType databaseType) {
        Map<String, ResourceDataSource> result = new LinkedHashMap<>(3, 1);
        result.put("sharding_db.ds_0", new ResourceDataSource("sharding_db.ds_0", DataSourceUtils.build(HikariDataSource.class, databaseType, "demo_ds_0")));
        result.put("sharding_db.ds_1", new ResourceDataSource("sharding_db.ds_1", DataSourceUtils.build(HikariDataSource.class, databaseType, "demo_ds_1")));
        result.put("sharding_db.ds_2", new ResourceDataSource("sharding_db.ds_2", DataSourceUtils.build(AtomikosDataSourceBean.class, databaseType, "demo_ds_2")));
        return result;
    }
    
    private Map<String, DatabaseType> createDatabaseTypes(final DatabaseType databaseType) {
        Map<String, DatabaseType> result = new LinkedHashMap<>(3, 1);
        result.put("sharding_db.ds_0", databaseType);
        result.put("sharding_db.ds_1", databaseType);
        result.put("sharding_db.ds_2", databaseType);
        return result;
    }
}
