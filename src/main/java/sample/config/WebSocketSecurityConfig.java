/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package sample.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.security.config.annotation.web.messaging.MessageSecurityMetadataSourceRegistry;
import org.springframework.security.config.annotation.web.socket.AbstractSecurityWebSocketMessageBrokerConfigurer;
import org.springframework.security.messaging.util.matcher.MessageMatcher;
import org.springframework.security.messaging.util.matcher.SimpDestinationMessageMatcher;

/**
 * @author Rob Winch
 */
@Configuration
public class WebSocketSecurityConfig extends AbstractSecurityWebSocketMessageBrokerConfigurer {

    @Override
    protected void configure(MessageSecurityMetadataSourceRegistry messages) {
        messages
            .matchers(message("/topic/**","/queue/**")).denyAll()
            .anyMessage().hasRole("USER");

    }

    // avoid processing outbound channel
    public void configureClientOutboundChannel(ChannelRegistration registration) {}
    
    private static MessageMatcher<Object>[] message(String... patterns) {
        MessageMatcher<Object>[] result = new MessageMatcher[patterns.length];
        for(int i=0;i<patterns.length;i++) {
            result[i] = new TypedDestinationMatcher(SimpMessageType.MESSAGE, patterns[i]);
        }
        return result;
    }
}

class TypedDestinationMatcher implements MessageMatcher<Object> {
    private SimpMessageType type;
    private MessageMatcher<Object> delegate;
    
    public TypedDestinationMatcher(SimpMessageType type, String pattern) {
        this.type = type;
        this.delegate = new SimpDestinationMessageMatcher(pattern);
    }
    
    @Override
    public boolean matches(Message<? extends Object> message) {
        SimpMessageType actualType = SimpMessageHeaderAccessor.getMessageType(message.getHeaders());
        return actualType == type && this.delegate.matches(message);
    }
    
}