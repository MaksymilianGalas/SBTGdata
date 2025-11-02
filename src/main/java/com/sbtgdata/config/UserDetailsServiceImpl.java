package com.sbtgdata.config;

import com.sbtgdata.data.User;
import com.sbtgdata.data.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Optional<User> userOpt = userRepository.findByEmail(email);
        
        if (userOpt.isEmpty()) {
            throw new UsernameNotFoundException("Użytkownik nie znaleziony: " + email);
        }
        
        User user = userOpt.get();
        
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        if (user.getRoles() != null) {
            user.getRoles().forEach(role -> 
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
            );
        }
        
        return org.springframework.security.core.userdetails.User.builder()
            .username(user.getEmail())
            .password(user.getPassword())
            .authorities(authorities)
            .build();
    }
}

