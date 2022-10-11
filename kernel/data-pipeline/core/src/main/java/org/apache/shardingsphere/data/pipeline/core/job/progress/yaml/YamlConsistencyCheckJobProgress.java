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

package org.apache.shardingsphere.data.pipeline.core.job.progress.yaml;

import lombok.Data;
import org.apache.shardingsphere.infra.util.yaml.YamlConfiguration;

/**
 * Yaml data consistency check job progress.
 */
@Data
public final class YamlConsistencyCheckJobProgress implements YamlConfiguration {
    
    private String status;
    
    private String tableNames;
    
    private Long checkedRecordsCount;
    
    private Long recordsCount;
    
    private Long checkBeginTimeMillis;
    
    private Long checkEndTimeMillis;
}
