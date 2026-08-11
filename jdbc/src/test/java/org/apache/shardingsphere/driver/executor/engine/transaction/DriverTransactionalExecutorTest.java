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

package org.apache.shardingsphere.driver.executor.engine.transaction;

import org.apache.shardingsphere.database.connector.core.type.DatabaseType;
import org.apache.shardingsphere.driver.jdbc.core.connection.DriverDatabaseConnectionManager;
import org.apache.shardingsphere.driver.jdbc.core.connection.ShardingSphereConnection;
import org.apache.shardingsphere.infra.metadata.database.ShardingSphereDatabase;
import org.apache.shardingsphere.infra.metadata.database.rule.RuleMetaData;
import org.apache.shardingsphere.infra.spi.type.typed.TypedSPILoader;
import org.apache.shardingsphere.mode.manager.ContextManager;
import org.apache.shardingsphere.sql.parser.statement.core.statement.SQLStatement;
import org.apache.shardingsphere.transaction.ConnectionTransaction;
import org.apache.shardingsphere.transaction.rule.TransactionRule;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Collections;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DriverTransactionalExecutorTest {
    
    @Test
    void assertImplicitTransactionCommit() throws SQLException {
        ShardingSphereConnection connection = mock(ShardingSphereConnection.class);
        DriverDatabaseConnectionManager connectionManager = mock(DriverDatabaseConnectionManager.class);
        when(connection.getDatabaseConnectionManager()).thenReturn(connectionManager);
        ConnectionTransaction connectionTransaction = mock(ConnectionTransaction.class);
        when(connectionManager.getConnectionTransaction()).thenReturn(connectionTransaction);
        when(connection.getAutoCommit()).thenReturn(true);
        TransactionRule transactionRule = mock(TransactionRule.class);
        ContextManager contextManager = mock(ContextManager.class, RETURNS_DEEP_STUBS);
        when(contextManager.getMetaDataContexts().getMetaData().getGlobalRuleMetaData()).thenReturn(new RuleMetaData(Collections.singleton(transactionRule)));
        when(connection.getContextManager()).thenReturn(contextManager);
        SQLStatement sqlStatement = mock(SQLStatement.class);
        when(transactionRule.isImplicitCommitTransaction(sqlStatement, true, connectionTransaction, true)).thenReturn(true);
        DriverTransactionalExecutor executor = new DriverTransactionalExecutor(connection);
        assertThat(executor.execute(mock(ShardingSphereDatabase.class), sqlStatement, true, () -> "result"), is("result"));
        verify(connectionManager).begin();
        verify(connectionManager).commit();
        verify(connection, never()).commit();
    }
    
    @Test
    void assertImplicitTransactionRollback() throws SQLException {
        ShardingSphereConnection connection = mock(ShardingSphereConnection.class);
        DriverDatabaseConnectionManager connectionManager = mock(DriverDatabaseConnectionManager.class);
        when(connection.getDatabaseConnectionManager()).thenReturn(connectionManager);
        ConnectionTransaction connectionTransaction = mock(ConnectionTransaction.class);
        when(connectionManager.getConnectionTransaction()).thenReturn(connectionTransaction);
        when(connection.getAutoCommit()).thenReturn(true);
        TransactionRule transactionRule = mock(TransactionRule.class);
        ContextManager contextManager = mock(ContextManager.class, RETURNS_DEEP_STUBS);
        when(contextManager.getMetaDataContexts().getMetaData().getGlobalRuleMetaData()).thenReturn(new RuleMetaData(Collections.singleton(transactionRule)));
        when(connection.getContextManager()).thenReturn(contextManager);
        SQLStatement sqlStatement = mock(SQLStatement.class);
        when(transactionRule.isImplicitCommitTransaction(sqlStatement, true, connectionTransaction, true)).thenReturn(true);
        ShardingSphereDatabase database = mock(ShardingSphereDatabase.class);
        when(database.getProtocolType()).thenReturn(TypedSPILoader.getService(DatabaseType.class, "SQL92"));
        DriverTransactionalExecutor executor = new DriverTransactionalExecutor(connection);
        assertThrows(SQLException.class, () -> executor.execute(database, sqlStatement, true, () -> {
            throw new SQLException("expected");
        }));
        verify(connectionManager).begin();
        verify(connectionManager).rollback();
        verify(connection, never()).rollback();
    }
}
