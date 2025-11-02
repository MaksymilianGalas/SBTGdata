package com.sbtgdata.config;

import com.vaadin.flow.spring.security.VaadinWebSecurity;
import com.sbtgdata.views.LoginView;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class VaadinSecurityConfig extends VaadinWebSecurity {
    
    private final UserDetailsService userDetailsService;
    
    public VaadinSecurityConfig(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
            .requestMatchers("/images/**").permitAll()
        );
        super.configure(http);
        
        setLoginView(http, LoginView.class);
        
        http.userDetailsService(userDetailsService);
        
        SimpleUrlAuthenticationSuccessHandler successHandler = new SimpleUrlAuthenticationSuccessHandler();
        successHandler.setDefaultTargetUrl("/dashboard");
        successHandler.setAlwaysUseDefaultTargetUrl(true);
        http.formLogin()
            .usernameParameter("username")
            .passwordParameter("password")
            .successHandler(successHandler);
    }
}

