package example;

import javax.sql.DataSource;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class UseWireMockToMockJdbcResultSetsTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(0);

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    JdbcTemplate jdbcTemplate;

    @Before
    public void before() {
        MyP6MockFactory myP6MockFactory = new MyP6MockFactory();
        myP6MockFactory.setTargetUrl("http://localhost:" + wireMockRule.port() + "/sqlstub");

        DataSource dataSource = myP6MockFactory.createMockDataSource();

        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Test
    public void intercepts_matching_query_and_responds_with_mockresultset() {
        final String NAME_ERICH_EICHINGER = "Erich Eichinger";

        // setup mock resultsets
        WireMock.stubFor(WireMock
                .post(WireMock.urlPathEqualTo("/sqlstub"))
                    // SQL Statement is posted in the body, use any available matchers to match
                .withRequestBody(WireMock.equalTo("SELECT birthday FROM PEOPLE WHERE name = ?"))
                    // Parameters are sent with index has headername and value as headervalue
                .withHeader("1", WireMock.equalTo(NAME_ERICH_EICHINGER))
                    // return a recordset
                .willReturn(WireMock
                        .aResponse()
                        .withBody(""
                                + "<resultset>"
                                + "     <cols><col>birthday</col></cols>"
                                + "     <row><val>1980-01-01</val></row>"
                                + "</resultset>"
                        )
                )
        )
        ;

        String dateTime = jdbcTemplate
            .queryForObject(
                "SELECT birthday FROM DM.PEOPLE WHERE name = ?"
                , String.class
                , NAME_ERICH_EICHINGER
            );

        assertThat(dateTime, equalTo("1980-01-01"));
    }

    @Test
    public void passthrough_nonmatching_queries_throws_assertionerror() {
        thrown.expect(AssertionError.class);

        final String NAME = "Hugo Simon";

        String dateTime = jdbcTemplate
            .queryForObject(
                "SELECT birthday FROM PEOPLE WHERE name = ?"
                , String.class
                , NAME
            );
    }
}
