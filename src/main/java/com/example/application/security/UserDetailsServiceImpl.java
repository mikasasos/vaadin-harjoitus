package com.example.application.security;

import com.example.application.data.Role;
import com.example.application.data.User;
import com.example.application.data.UserRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;

        if(userRepository.count() == 0){
            User admin = new User();
            admin.setUsername("Admin");
            String password = "admin123";
            admin.setHashedPassword(passwordEncoder.encode(password));
            Set<Role> set = new HashSet<Role>();
            set.add(Role.ADMIN);
            admin.setRoles(set);
            this.userRepository.save(admin);
            User user = new User();
            user.setUsername("User");
            String password2 = "user123";
            user.setHashedPassword(passwordEncoder.encode(password2));
            Set<Role> set2 = new HashSet<Role>();
            set2.add(Role.USER);
            user.setRoles(set2);
            this.userRepository.save(user);
        }
    }

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("No user present with username: " + username));
        return new org.springframework.security.core.userdetails.User(user.getUsername(), user.getHashedPassword(),
                getAuthorities(user));
    }

    private static List<GrantedAuthority> getAuthorities(User user) {
        return user.getRoles().stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());

    }

}
