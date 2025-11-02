package com.sbtgdata.views;

import com.sbtgdata.config.SecurityService;
import com.sbtgdata.data.User;
import com.sbtgdata.data.UserRepository;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

@Route(value = "admin", layout = MainLayout.class)
@PageTitle("Panel Administratora")
@RolesAllowed("ROLE_ADMINISTRATOR")
public class AdminPanelView extends VerticalLayout implements BeforeEnterObserver {
    
    @Autowired
    private SecurityService securityService;
    
    @Autowired
    private UserRepository userRepository;
    
    private Grid<User> userGrid;
    
    public AdminPanelView(SecurityService securityService, UserRepository userRepository) {
        this.securityService = securityService;
        this.userRepository = userRepository;
        
        setSizeFull();
        setPadding(true);
        
        H1 title = new H1("Panel Administratora");
        
        Paragraph info = new Paragraph("Zarządzanie użytkownikami");
        
        userGrid = new Grid<>(User.class, false);
        userGrid.addColumn(User::getEmail).setHeader("Email");
        userGrid.addColumn(user -> String.join(", ", user.getRoles())).setHeader("Role");
        
        Button refreshButton = new Button("Odśwież", e -> refreshUsers());
        
        add(title, info, refreshButton, userGrid);
        
        refreshUsers();
    }
    
    private void refreshUsers() {
        List<User> users = userRepository.findAll();
        userGrid.setItems(users);
    }
    
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!securityService.hasRole("administrator")) {
            event.rerouteTo("dashboard");
            Notification.show("Brak uprawnień do tej strony", 3000, Notification.Position.MIDDLE);
        }
    }
}

