package sample.config;


import java.security.Principal;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.support.ChannelInterceptorAdapter;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.AbstractWebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.server.HandshakeInterceptor;

import sample.data.ActiveWebSocketUser;
import sample.data.ActiveWebSocketUserRepository;

@Configuration
@EnableScheduling
@EnableWebSocketMessageBroker
public class WebSocketConfig extends AbstractWebSocketMessageBrokerConfigurer {

    @Autowired
    Environment env;

    @Autowired
    SessionRepository sessionRepository;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/messages")
                .withSockJS()
                .setInterceptors(new HttpSessionIdHandshakeInterceptor());

    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.setInterceptors(sessionContextChannelInterceptorAdapter());
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/queue/", "/topic/");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Bean
    public ChannelInterceptorAdapter sessionContextChannelInterceptorAdapter() {
        return new ChannelInterceptorAdapter() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                Map<String, Object> sessionHeaders = SimpMessageHeaderAccessor.getSessionAttributes(message.getHeaders());
                String sessionId = (String) sessionHeaders.get(SESSION_ATTR);
                if (sessionId != null) {
                    Session session = sessionRepository.getSession(sessionId);
                    if (session != null) {

                        sessionRepository.save(session);
                    }
                }
                return super.preSend(message, channel);
            }
        };
    }

    @Bean
    public WebSocketConnectHandler webSocketConnectHandler(SimpMessageSendingOperations messagingTemplate, ActiveWebSocketUserRepository repository) {
    	return new WebSocketConnectHandler(messagingTemplate, repository);
    }
    
    @Bean
    public WebSocketDisconnectHandler webSocketDisconnectHandler(SimpMessageSendingOperations messagingTemplate, ActiveWebSocketUserRepository repository) {
    	return new WebSocketDisconnectHandler(messagingTemplate, repository);
    }

    private static final String SESSION_ATTR = "httpSession.id";

    static class HttpSessionIdHandshakeInterceptor implements HandshakeInterceptor {

        @Override
        public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
            if (request instanceof ServletServerHttpRequest) {
                ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
                HttpSession session = servletRequest.getServletRequest().getSession(false);
                if (session != null) {
                    attributes.put(SESSION_ATTR, session.getId());
                }
            }
            return true;
        }

        public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Exception ex) {
        }
    }
    
    class WebSocketConnectHandler implements ApplicationListener<SessionConnectEvent> {
    	private ActiveWebSocketUserRepository repository;
    	private SimpMessageSendingOperations messagingTemplate;

		public WebSocketConnectHandler(SimpMessageSendingOperations messagingTemplate, ActiveWebSocketUserRepository repository) {
			super();
			this.messagingTemplate = messagingTemplate;
			this.repository = repository;
		}

		@Override
		public void onApplicationEvent(SessionConnectEvent event) {
			MessageHeaders headers = event.getMessage().getHeaders();
			Principal user = SimpMessageHeaderAccessor.getUser(headers);
			if(user == null) {
				return;
			}
			String id = SimpMessageHeaderAccessor.getSessionId(headers);
			repository.save(new ActiveWebSocketUser(id, user.getName(), Calendar.getInstance()));
			messagingTemplate.convertAndSend("/topic/friends/signin", Arrays.asList(user.getName()));
		}
    }
    
    class WebSocketDisconnectHandler implements ApplicationListener<SessionDisconnectEvent> {
    	private ActiveWebSocketUserRepository repository;
    	private SimpMessageSendingOperations messagingTemplate;

		public WebSocketDisconnectHandler(SimpMessageSendingOperations messagingTemplate, ActiveWebSocketUserRepository repository) {
			super();
			this.messagingTemplate = messagingTemplate;
			this.repository = repository;
		}
		
		@Override
		public void onApplicationEvent(SessionDisconnectEvent event) {
			String id = event.getSessionId();
			if(id == null) {
				return;
			}
			ActiveWebSocketUser user = repository.findOne(id);
			if(user == null) {
				return;
			}
			
			repository.delete(id);

			messagingTemplate.convertAndSend("/topic/friends/signout", Arrays.asList(user.getUsername()));
		}
    	
    }
}