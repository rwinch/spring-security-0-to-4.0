/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package samples;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import javax.servlet.Filter;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;

import sample.Application;
import sample.data.Message;

import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * @author Rob Winch
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration
public class SecurityMockMvcTests {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private Filter springSecurityFilterChain;

    private MockMvc mvc;

    @Before
    public void setup() {
        mvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    public void inboxRequiresLogin() throws Exception {
        mvc
            .perform(get("/"))
            .andExpect(loginPage());
    }

    @Test
    @WithCustomUser
    public void inboxShowsOnlyRobsMessages() throws Exception {
        mvc
            .perform(get("/"))
            .andExpect(model().attribute("messages", new BaseMatcher<List<Message>>() {
                @Override
                public boolean matches(Object other) {
                    @SuppressWarnings("unchecked")
                    List<Message> messages = (List<Message>) other;
                    return messages.size() == 1 && messages.get(0).getId() == 100;
                }

                @Override
                public void describeTo(Description d) {
                }
            }));
    }

    @Test
    public void invalidUsernamePassword() throws Exception {
        mvc
            .perform(formLogin())
            .andExpect(unauthenticated());
    }

    @Test
    public void validUsernamePassword() throws Exception {
        mvc
            .perform(formLogin().user("rob@example.com"))
            .andExpect(authenticated());
    }

    @Test
    @WithCustomUser
    public void composeRequiresCsrf() throws Exception {
        mvc
            .perform(post("/").param("summary", "Hello Luke").param("message", "This is my message"))
            .andExpect(invalidCsrf());
    }

    @Test
    @WithCustomUser
    public void compose() throws Exception {
      MockHttpServletRequestBuilder compose = post("/")
          .param("summary", "Hello Luke")
          .param("message", "This is my message")
          .with(csrf());
      mvc
        .perform(compose)
        .andExpect(status().is2xxSuccessful());
    }

    @Test
    @WithCustomUser
    public void robCannotAccessLukesMessage() throws Exception {
        mvc
            .perform(get("/110"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithCustomUser(id=1,email="luke@example.com")
    public void lukeCanAccessLukesMessage() throws Exception {
        mvc
            .perform(get("/110"))
            .andExpect(status().isOk());
    }

    @Test
    @WithCustomUser
    public void robCanAccessOwnMessage() throws Exception {
        mvc
            .perform(get("/100"))
            .andExpect(status().isOk());
    }

    private static ResultMatcher loginPage() {
        return new ResultMatcher() {
            @Override
            public void match(MvcResult result) throws Exception {
                status().isMovedTemporarily().match(result);
                redirectedUrl("http://localhost/login").match(result);
            }
        };
    }

    private static ResultMatcher invalidCsrf() {
        return new ResultMatcher() {
            @Override
            public void match(MvcResult result) throws Exception {
                status().isForbidden().match(result);
            }
        };
    }
}
