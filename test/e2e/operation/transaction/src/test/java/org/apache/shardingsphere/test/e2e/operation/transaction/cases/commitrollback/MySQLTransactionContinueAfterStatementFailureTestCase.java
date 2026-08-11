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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * MySQL transaction continuation after statement failure test case.
 */
@TransactionTestCase(dbTypes = TransactionTestConstants.MYSQL, adapters = TransactionTestConstants.JDBC, transactionTypes = TransactionType.LOCAL)
public final class MySQLTransactionContinueAfterStatementFailureTestCase extends BaseTransactionTestCase {
    
    public MySQLTransactionContinueAfterStatementFailureTestCase(final TransactionTestCaseParameter testCaseParam) {
        super(testCaseParam);
    }
    
    @Override
    protected void executeTest(final TransactionContainerComposer containerComposer) throws SQLException {
        assertContinueWithStatement();
        assertContinueWithPreparedStatement();
    }
    
    private void assertContinueWithStatement() throws SQLException {
        prepare();
        try (Connection connection = getDataSource().getConnection()) {
            connection.setAutoCommit(false);
            executeUpdateWithLog(connection, "UPDATE account SET balance = 100 WHERE id = 1");
            SQLException actualException = assertThrows(SQLException.class,
                    () -> executeUpdateWithLog(connection, "INSERT INTO account (id, balance, transaction_id) VALUES (1, 11, 11)"));
            assertDuplicateKey(actualException);
            executeUpdateWithLog(connection, "INSERT INTO account (id, balance, transaction_id) VALUES (2, 2, 2)");
            connection.commit();
        }
        assertCommittedRows();
    }
    
    private void assertContinueWithPreparedStatement() throws SQLException {
        prepare();
        try (
                Connection connection = getDataSource().getConnection();
                PreparedStatement statement = connection.prepareStatement("INSERT INTO account (id, balance, transaction_id) VALUES (?, ?, ?)")) {
            connection.setAutoCommit(false);
            executeUpdateWithLog(connection, "UPDATE account SET balance = 100 WHERE id = 1");
            setParameters(statement, 1, 11);
            SQLException actualException = assertThrows(SQLException.class, statement::executeUpdate);
            assertDuplicateKey(actualException);
            setParameters(statement, 2, 2);
            assertThat(statement.executeUpdate(), is(1));
            connection.commit();
        }
        assertCommittedRows();
    }
    
    private void prepare() throws SQLException {
        try (Connection connection = getDataSource().getConnection()) {
            executeWithLog(connection, "DELETE FROM account");
            executeWithLog(connection, "INSERT INTO account (id, balance, transaction_id) VALUES (1, 1, 1)");
        }
    }
    
    private void assertCommittedRows() throws SQLException {
        try (Connection connection = getDataSource().getConnection()) {
            assertAccountBalances(connection, 100, 2);
        }
    }
    
    private void setParameters(final PreparedStatement statement, final int id, final int balance) throws SQLException {
        statement.setInt(1, id);
        statement.setInt(2, balance);
        statement.setInt(3, id);
    }
    
    private void assertDuplicateKey(final SQLException actualException) {
        assertThat(actualException.getSQLState(), is("23000"));
        assertThat(actualException.getErrorCode(), is(1062));
    }
}
