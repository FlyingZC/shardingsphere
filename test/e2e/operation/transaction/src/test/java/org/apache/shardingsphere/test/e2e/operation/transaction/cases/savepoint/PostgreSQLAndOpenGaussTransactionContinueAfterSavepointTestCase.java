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

package org.apache.shardingsphere.test.e2e.operation.transaction.cases.savepoint;

import org.apache.shardingsphere.test.e2e.operation.transaction.cases.base.BaseTransactionTestCase;
import org.apache.shardingsphere.test.e2e.operation.transaction.engine.base.TransactionContainerComposer;
import org.apache.shardingsphere.test.e2e.operation.transaction.engine.base.TransactionTestCase;
import org.apache.shardingsphere.test.e2e.operation.transaction.engine.constants.TransactionTestConstants;
import org.apache.shardingsphere.transaction.api.TransactionType;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * PostgreSQL and openGauss transaction continuation after savepoint rollback integration test.
 */
@TransactionTestCase(dbTypes = {TransactionTestConstants.POSTGRESQL, TransactionTestConstants.OPENGAUSS}, adapters = TransactionTestConstants.JDBC,
        transactionTypes = TransactionType.LOCAL)
public final class PostgreSQLAndOpenGaussTransactionContinueAfterSavepointTestCase extends BaseTransactionTestCase {
    
    public PostgreSQLAndOpenGaussTransactionContinueAfterSavepointTestCase(final TransactionTestCaseParameter testCaseParam) {
        super(testCaseParam);
    }
    
    @Override
    protected void executeTest(final TransactionContainerComposer containerComposer) throws SQLException {
        try (Connection connection = getDataSource().getConnection()) {
            connection.setAutoCommit(false);
            executeUpdateWithLog(connection, "INSERT INTO account (id, balance, transaction_id) VALUES (1, 1, 1)");
            Savepoint savepoint = connection.setSavepoint("point_before_failure");
            executeUpdateWithLog(connection, "UPDATE account SET balance = 100 WHERE id = 1");
            SQLException actualException = assertThrows(SQLException.class,
                    () -> executeUpdateWithLog(connection, "INSERT INTO account (id, balance, transaction_id) VALUES (1, 11, 11)"));
            assertThat(actualException.getSQLState(), is("23505"));
            connection.rollback(savepoint);
            executeUpdateWithLog(connection, "INSERT INTO account (id, balance, transaction_id) VALUES (2, 2, 2)");
            connection.commit();
        }
        try (Connection connection = getDataSource().getConnection()) {
            assertAccountBalances(connection, 1, 2);
        }
    }
}
