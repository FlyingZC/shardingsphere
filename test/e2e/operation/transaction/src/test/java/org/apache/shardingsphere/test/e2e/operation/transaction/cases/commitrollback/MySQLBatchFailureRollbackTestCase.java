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

package org.apache.shardingsphere.test.e2e.operation.transaction.cases.commitrollback;

import org.apache.shardingsphere.test.e2e.operation.transaction.cases.base.BaseTransactionTestCase;
import org.apache.shardingsphere.test.e2e.operation.transaction.engine.base.TransactionContainerComposer;
import org.apache.shardingsphere.test.e2e.operation.transaction.engine.base.TransactionTestCase;
import org.apache.shardingsphere.test.e2e.operation.transaction.engine.constants.TransactionTestConstants;
import org.apache.shardingsphere.transaction.api.TransactionType;

import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MySQL batch failure rollback transaction integration test.
 */
@TransactionTestCase(dbTypes = TransactionTestConstants.MYSQL, adapters = TransactionTestConstants.JDBC, transactionTypes = TransactionType.LOCAL)
public final class MySQLBatchFailureRollbackTestCase extends BaseTransactionTestCase {
    
    private static final String INSERT_SQL = "INSERT INTO account(id, balance, transaction_id) VALUES(?, ?, ?)";
    
    public MySQLBatchFailureRollbackTestCase(final TransactionTestCaseParameter testCaseParam) {
        super(testCaseParam);
    }
    
    @Override
    protected void executeTest(final TransactionContainerComposer containerComposer) throws SQLException {
        assertFailureRollbackWithPreparedStatement();
        assertFailureRollbackWithStatement();
    }
    
    private void assertFailureRollbackWithPreparedStatement() throws SQLException {
        try (
                Connection connection = getDataSource().getConnection();
                PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
            connection.setAutoCommit(false);
            addBatch(statement, 1, 1);
            addBatch(statement, 1, 11);
            addBatch(statement, 2, 2);
            BatchUpdateException actualException = assertThrows(BatchUpdateException.class, statement::executeBatch);
            assertThat(actualException.getSQLState(), is("23000"));
            assertThat(actualException.getErrorCode(), is(1062));
            assertTrue(Arrays.stream(actualException.getUpdateCounts()).anyMatch(each -> Statement.EXECUTE_FAILED == each));
            connection.rollback();
        }
        try (Connection connection = getDataSource().getConnection()) {
            assertAccountBalances(connection);
        }
    }
    
    private void assertFailureRollbackWithStatement() throws SQLException {
        try (Connection connection = getDataSource().getConnection(); Statement statement = connection.createStatement()) {
            connection.setAutoCommit(false);
            statement.addBatch("INSERT INTO account(id, balance, transaction_id) VALUES(1, 1, 1)");
            statement.addBatch("INSERT INTO account(id, balance, transaction_id) VALUES(1, 11, 11)");
            statement.addBatch("INSERT INTO account(id, balance, transaction_id) VALUES(2, 2, 2)");
            BatchUpdateException actualException = assertThrows(BatchUpdateException.class, statement::executeBatch);
            assertThat(actualException.getSQLState(), is("23000"));
            assertThat(actualException.getErrorCode(), is(1062));
            assertThat(actualException.getUpdateCounts(), is(new int[]{1}));
            connection.rollback();
        }
        try (Connection connection = getDataSource().getConnection()) {
            assertAccountBalances(connection);
        }
    }
    
    private void addBatch(final PreparedStatement statement, final int id, final int balance) throws SQLException {
        statement.setInt(1, id);
        statement.setInt(2, balance);
        statement.setInt(3, id);
        statement.addBatch();
    }
}
