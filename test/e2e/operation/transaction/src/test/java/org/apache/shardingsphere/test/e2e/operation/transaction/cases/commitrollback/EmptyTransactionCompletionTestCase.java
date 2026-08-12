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
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Empty transaction completion integration test.
 */
@TransactionTestCase(adapters = TransactionTestConstants.JDBC, transactionTypes = {TransactionType.LOCAL, TransactionType.XA})
public final class EmptyTransactionCompletionTestCase extends BaseTransactionTestCase {
    
    public EmptyTransactionCompletionTestCase(final TransactionTestCaseParameter testCaseParam) {
        super(testCaseParam);
    }
    
    @Override
    protected void executeTest(final TransactionContainerComposer containerComposer) throws SQLException {
        try (Connection connection = getDataSource().getConnection(); Connection queryConnection = getDataSource().getConnection()) {
            connection.setAutoCommit(false);
            connection.commit();
            assertFalse(connection.getAutoCommit());
            assertAccountBalances(queryConnection);
            executeWithLog(connection, "INSERT INTO account (id, balance, transaction_id) VALUES (1, 1, 1)");
            connection.commit();
            assertAccountBalances(queryConnection, 1);
            executeWithLog(queryConnection, "DELETE FROM account");
            connection.rollback();
            assertFalse(connection.getAutoCommit());
            executeWithLog(connection, "INSERT INTO account (id, balance, transaction_id) VALUES (2, 2, 2)");
            connection.commit();
            assertAccountBalances(queryConnection, 2);
        }
    }
}
