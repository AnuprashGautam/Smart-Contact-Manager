package com.smart.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class MyConfig {

	// It helps in encoding the password so that if a person get access to the DB, can't use the user profile.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    // Provides the user details.
    @Bean 
    public UserDetailsService getUserDetailsService() {
    	return new UserDetailsServiceImpl();
    }
    
    // It authenticate the user by using the getUserDetailsService, and passwordEncoder beans.
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
    	DaoAuthenticationProvider daoAuthenticationProvider=new DaoAuthenticationProvider();
    	
    	daoAuthenticationProvider.setUserDetailsService(this.getUserDetailsService());
    	daoAuthenticationProvider.setPasswordEncoder(passwordEncoder());
    	
    	return daoAuthenticationProvider;
    }
    
    
    // Authrization based on the http request is managed here.
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
            .csrf(csrf -> csrf.disable())  // Disable CSRF
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/user/**").hasRole("USER")
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/**").permitAll()
            )
//            .formLogin(Customizer.withDefaults())  // Default Spring Security login form
            .formLogin(form -> form
                    .loginPage("/signin")   // Specify your custom login page URL
                    .loginProcessingUrl("/dologin")		// URL responsible for processing the login form.
                    .defaultSuccessUrl("/user/index")	// If the authentication is successful.
//                  .failureUrl("/login-fail")			// If the authentication is failed.
                    .permitAll()           // Allow all users to access the login page
                )
            .build();
    }
}
