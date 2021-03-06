/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
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
package org.apache.camel.component.elsql;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.sql.SqlConstants;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.After;
import org.junit.Test;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

public class ElSqlProducerBodySimpleTest extends CamelTestSupport {

    private EmbeddedDatabase db;

    @Override
    protected JndiRegistry createRegistry() throws Exception {
        JndiRegistry jndi = super.createRegistry();

        // this is the database we create with some initial data for our unit test
        db = new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.DERBY).addScript("sql/createAndPopulateDatabase.sql").build();

        jndi.bind("dataSource", db);

        return jndi;
    }

    @Test
    public void testSimpleBody() throws Exception {
        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedMessageCount(1);

        Project pojo = new Project();
        pojo.setLicense("XXX");

        template.sendBody("direct:simple", pojo);

        mock.assertIsSatisfied();

        // the result is a List
        List<?> received = assertIsInstanceOf(List.class, mock.getReceivedExchanges().get(0).getIn().getBody());

        // and each row in the list is a Map
        Map<?, ?> row = assertIsInstanceOf(Map.class, received.get(0));

        // and we should be able the get the project from the map that should be Linux
        assertEquals("Linux", row.get("PROJECT"));
    }

    @Test
    public void testBodyParameter() throws InterruptedException {
        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedMessageCount(1);
        mock.message(0).body().isInstanceOf(List.class);
        mock.message(0).header(SqlConstants.SQL_ROW_COUNT).isEqualTo(1);

        template.sendBody("direct:parameters", Collections.singletonMap("id", 1));

        mock.assertIsSatisfied();

        List<?> received = mock.getReceivedExchanges().get(0).getIn().getBody(List.class);

        Map<?, ?> row = assertIsInstanceOf(Map.class, received.get(0));

        assertEquals("Camel", row.get("PROJECT"));
    }

    @Test
    public void testHeadersParameter() throws InterruptedException {
        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedMessageCount(1);
        mock.message(0).body().isInstanceOf(List.class);
        mock.message(0).header(SqlConstants.SQL_ROW_COUNT).isEqualTo(1);

        template.sendBodyAndHeader("direct:parameters", "", "id", 1);

        mock.assertIsSatisfied();

        List<?> received = mock.getReceivedExchanges().get(0).getIn().getBody(List.class);

        Map<?, ?> row = assertIsInstanceOf(Map.class, received.get(0));

        assertEquals("Camel", row.get("PROJECT"));
    }
    
    @Test
    public void testUpdateHeader() throws InterruptedException {
        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedMessageCount(1);
        mock.message(0).header(SqlConstants.SQL_UPDATE_COUNT).isEqualTo(1);
        
        Map<String, Object> headers = new HashMap<>();       
        headers.put("id", "3");
        headers.put("lic", "GNU");

        template.sendBodyAndHeaders("direct:update", "", headers);

        mock.assertIsSatisfied();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();

        db.shutdown();
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            public void configure() {
                from("direct:simple")
                        .to("elsql:projectsByIdBody:elsql/projects.elsql?dataSource=#dataSource")
                        .to("mock:result");

                from("direct:parameters")
                        .to("elsql:projectById:elsql/projects.elsql?dataSource=#dataSource")
                        .to("mock:result");
                
                from("direct:update")
                        .to("elsql:updateLicense:elsql/projects.elsql?dataSource=#dataSource")
                        .to("mock:result");
            }
        };
    }
}