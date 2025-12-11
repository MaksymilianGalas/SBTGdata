package com.sbtgdata.config;

import com.sbtgdata.data.Role;
import com.sbtgdata.data.RoleRepository;
import com.sbtgdata.data.User;
import com.sbtgdata.data.UserRepository;
import com.sbtgdata.data.ViewService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Set;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initializeData(UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            ViewService viewService) {
        return args -> {
            createRoleIfNotFound(roleRepository, "ADMIN", viewService.getAllViews().keySet());
            createRoleIfNotFound(roleRepository, "USER", new HashSet<>());

            User admin;
            if (userRepository.existsByEmail("admin")) {
                admin = userRepository.findByEmail("admin").get();
            } else {
                admin = new User();
                admin.setEmail("admin");
                admin.setPassword(passwordEncoder.encode("admin"));
            }

            Set<String> roles = admin.getRoles();
            if (roles == null) {
                roles = new HashSet<>();
            }
            if (!roles.contains("ADMIN")) {
                roles.add("ADMIN");
                admin.setRoles(roles);
                userRepository.save(admin);
                System.out.println("Updated admin user with ADMIN role");
            } else if (!userRepository.existsByEmail("admin")) {
                userRepository.save(admin);
                System.out.println("Created admin user: admin / admin");
            }
        };
    }

    private void createRoleIfNotFound(RoleRepository roleRepository, String name, Set<String> allowedViews) {
        if (!roleRepository.existsByName(name)) {
            Role role = new Role(name);
            role.setAllowedViews(allowedViews);
            roleRepository.save(role);
            System.out.println("Created role: " + name);
        } else if (name.equals("ADMIN")) {
            Role adminRole = roleRepository.findByName("ADMIN").get();
            adminRole.setAllowedViews(allowedViews);
            roleRepository.save(adminRole);
        }
    }
}
