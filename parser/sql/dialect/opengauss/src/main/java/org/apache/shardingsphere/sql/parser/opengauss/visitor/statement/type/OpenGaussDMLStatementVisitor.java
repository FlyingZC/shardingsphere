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

package org.apache.shardingsphere.sql.parser.opengauss.visitor.statement.type;

import org.apache.shardingsphere.sql.parser.api.ASTNode;
import org.apache.shardingsphere.sql.parser.api.visitor.statement.type.DMLStatementVisitor;
import org.apache.shardingsphere.sql.parser.autogen.OpenGaussStatementParser.CallContext;
import org.apache.shardingsphere.sql.parser.autogen.OpenGaussStatementParser.CopyContext;
import org.apache.shardingsphere.sql.parser.autogen.OpenGaussStatementParser.DoStatementContext;
import org.apache.shardingsphere.sql.parser.autogen.OpenGaussStatementParser.ReturningClauseContext;
import org.apache.shardingsphere.sql.parser.opengauss.visitor.statement.OpenGaussStatementVisitor;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.ReturningSegment;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.item.ProjectionsSegment;
import org.apache.shardingsphere.sql.parser.sql.common.segment.generic.table.SimpleTableSegment;
import org.apache.shardingsphere.sql.parser.sql.dialect.statement.opengauss.dml.OpenGaussCallStatement;
import org.apache.shardingsphere.sql.parser.sql.dialect.statement.opengauss.dml.OpenGaussCopyStatement;
import org.apache.shardingsphere.sql.parser.sql.dialect.statement.opengauss.dml.OpenGaussDoStatement;

/**
 * DML statement visitor for openGauss.
 */
public final class OpenGaussDMLStatementVisitor extends OpenGaussStatementVisitor implements DMLStatementVisitor {
    
    @Override
    public ASTNode visitCall(final CallContext ctx) {
        return new OpenGaussCallStatement();
    }
    
    @Override
    public ASTNode visitDoStatement(final DoStatementContext ctx) {
        return new OpenGaussDoStatement();
    }
    
    @Override
    public ASTNode visitCopy(final CopyContext ctx) {
        OpenGaussCopyStatement result = new OpenGaussCopyStatement();
        if (null != ctx.qualifiedName()) {
            result.setTableSegment((SimpleTableSegment) visit(ctx.qualifiedName()));
        }
        return result;
    }
    
    @Override
    public ASTNode visitReturningClause(final ReturningClauseContext ctx) {
        return new ReturningSegment(ctx.start.getStartIndex(), ctx.stop.getStopIndex(), (ProjectionsSegment) visit(ctx.targetList()));
    }
}
