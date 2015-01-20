/*
 * This software is licensed under the Apache License, Version 2.0
 * (the "License") agreement; you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.moneta.dao;

import java.sql.Connection;
import java.util.concurrent.Callable;

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.moneta.config.MonetaEnvironment;
import org.moneta.types.search.SearchResult;
import org.moneta.types.topic.Topic;

class SqlSelectExecutor implements Callable<SearchResult> {
	
	private Topic topic;
	private String sqlText;
	
	public SqlSelectExecutor(String topicName, String sqlText) {
		topic = MonetaEnvironment.getConfiguration().getTopic(topicName);
		Validate.notNull(topic, "topic not found.    topic=" + topicName);
		Validate.notEmpty(sqlText, "Null or blank SqlText not allowed.");
		this.sqlText = sqlText;
	}

	public SearchResult call() {
		SearchResult result = new SearchResult();
		Connection topicConnection = null;
		QueryRunner runner = new QueryRunner();
		
		try {
			topicConnection = MonetaEnvironment.getConfiguration()
					.getConnection(topic.getDataSourceName());
			
			result.setResultData(runner.query(topicConnection, sqlText, new RecordResultSetHandler()));
			result.setNbrRows(Long.valueOf(result.getResultData().length));
			
			if (topicConnection.getAutoCommit()) {
				DbUtils.commitAndCloseQuietly(topicConnection);
			}
		}
		catch (Exception e) {			
			result.setErrorCode(500);
			result.setErrorMessage(ExceptionUtils.getStackTrace(e));
			DbUtils.rollbackAndCloseQuietly(topicConnection);
		}
		
		return result;

	}

}