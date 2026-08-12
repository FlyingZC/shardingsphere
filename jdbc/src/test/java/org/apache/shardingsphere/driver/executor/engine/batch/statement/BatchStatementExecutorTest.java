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

package org.apache.shardingsphere.driver.executor.engine.batch.statement;

import org.junit.jupiter.api.Test;

import java.sql.BatchUpdateException;
import java.sql.SQLException;
import java.sql.Statement;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class BatchStatementExecutorTest {
    
    @Test
    void assertExecuteBatchAndClear() throws SQLException {
        Statement statement = mock(Statement.class);
        when(statement.executeUpdate(anyString())).thenReturn(1, 2);
        BatchStatementExecutor executor = new BatchStatementExecutor(statement);
        executor.addBatch("UPDATE t SET col=1 WHERE id=1");
        executor.addBatch("UPDATE t SET col=10 WHERE id=2 OR id=3");
        int[] actual = executor.executeBatch();
        assertThat(actual, is(new int[]{1, 2}));
        executor.clear();
        assertThat(executor.executeBatch(), is(new int[0]));
    }
    
    @Test
    void assertExecuteBatchFailure() throws SQLException {
        Statement statement = mock(Statement.class);
        SQLException cause = new SQLException("failure", "23000", 1062);
        when(statement.executeUpdate(anyString())).thenReturn(1).thenThrow(cause);
        BatchStatementExecutor executor = new BatchStatementExecutor(statement);
        executor.addBatch("UPDATE t SET col=1 WHERE id=1");
        executor.addBatch("UPDATE t SET col=10 WHERE id=2");
        BatchUpdateException actual = assertThrows(BatchUpdateException.class, executor::executeBatch);
        assertThat(actual.getMessage(), is("failure"));
        assertThat(actual.getSQLState(), is("23000"));
        assertThat(actual.getErrorCode(), is(1062));
        assertThat(actual.getUpdateCounts(), is(new int[]{1}));
        assertThat(actual.getCause(), sameInstance(cause));
    }
}
