package com.dga.access.config;

import com.dga.access.entity.User;
import com.dga.access.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Autowired
    private UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        OAuth2User oauth2User = token.getPrincipal();
        String provider = token.getAuthorizedClientRegistrationId();
        
        // Note: For real WeChat/Alipay, the attribute key for OpenID might differ.
        // using getName() which usually maps to the primary principal identifier (sub/openid)
        String openId = oauth2User.getName(); 
        
        User user = userRepository.findByOpenId(openId);
        if (user == null) {
            user = new User();
            user.setOpenId(openId);
            user.setProvider(provider);
            // Ensure username is unique and fits length constraints
            String username = provider + "_" + openId;
            if (username.length() > 50) {
                username = username.substring(0, 50);
            }
            user.setUsername(username);
            user.setPassword("N/A"); // No password
            
            // Try to get nickname/name
            String name = oauth2User.getAttribute("name");
            if (name == null) {
                name = oauth2User.getAttribute("nickname");
            }
            user.setNickname(name);
            
            user.setStatus(1); // Active
            userRepository.save(user);
        }

        String authToken = "mock-token-" + user.getUsername();

        // Redirect to frontend with token
        String targetUrl = UriComponentsBuilder.fromUriString("http://localhost:3000/login")
                .queryParam("token", authToken)
                .queryParam("username", user.getUsername())
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
