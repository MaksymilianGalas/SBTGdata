package com.sbtgdata.views;

import com.sbtgdata.config.SecurityService;
import com.sbtgdata.data.Role;
import com.sbtgdata.data.RoleRepository;
import com.sbtgdata.data.User;
import com.sbtgdata.data.UserRepository;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Route(value = "admin", layout = MainLayout.class)
@PageTitle("Panel Administratora")
@RolesAllowed("ADMIN")
public class AdminPanelView extends VerticalLayout implements BeforeEnterObserver {

    @Autowired
    private SecurityService securityService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private Grid<User> userGrid;

    public AdminPanelView(SecurityService securityService, UserRepository userRepository,
            RoleRepository roleRepository) {
        this.securityService = securityService;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;

        setSizeFull();
        setPadding(true);

        H1 title = new H1("Panel Administratora");

        Paragraph info = new Paragraph("Zarządzanie użytkownikami i ich uprawnieniami");

        userGrid = new Grid<>(User.class, false);
        userGrid.addColumn(User::getEmail).setHeader("Email");
        userGrid.addColumn(user -> String.join(", ", user.getRoles())).setHeader("Role");

        userGrid.addComponentColumn(user -> {
            Button editRolesButton = new Button("Edytuj role", e -> openRoleEditor(user));
            Button deleteButton = new Button("Usuń", e -> {
                if (user.getEmail().equals("admin")) {
                    Notification.show("Nie można usunąć konta administratora", 3000, Notification.Position.MIDDLE);
                    return;
                }
                userRepository.delete(user);
                refreshUsers();
                Notification.show("Użytkownik usunięty");
            });
            return new HorizontalLayout(editRolesButton, deleteButton);
        }).setHeader("Akcje");

        Button refreshButton = new Button("Odśwież", e -> refreshUsers());

        add(title, info, refreshButton, userGrid);

        refreshUsers();
    }

    private void refreshUsers() {
        List<User> users = userRepository.findAll();
        userGrid.setItems(users);
    }

    private void openRoleEditor(User user) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Edytuj role użytkownika: " + user.getEmail());

        VerticalLayout dialogLayout = new VerticalLayout();

        // Get all available roles from database
        List<Role> allRoles = roleRepository.findAll();
        Set<String> roleNames = allRoles.stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        CheckboxGroup<String> rolesGroup = new CheckboxGroup<>();
        rolesGroup.setLabel("Role użytkownika");
        rolesGroup.setItems(roleNames);

        // Set current user roles
        if (user.getRoles() != null) {
            rolesGroup.setValue(user.getRoles());
        }

        dialogLayout.add(rolesGroup);
        dialog.add(dialogLayout);

        Button saveButton = new Button("Zapisz", e -> {
            Set<String> selectedRoles = rolesGroup.getValue();
            if (selectedRoles.isEmpty()) {
                selectedRoles = new HashSet<>();
                selectedRoles.add("USER");
            }
            user.setRoles(selectedRoles);
            userRepository.save(user);
            refreshUsers();
            dialog.close();
            Notification.show("Role użytkownika zaktualizowane");
        });

        Button cancelButton = new Button("Anuluj", e -> dialog.close());

        dialog.getFooter().add(cancelButton, saveButton);
        dialog.open();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!securityService.hasRole("ADMIN")) {
            event.rerouteTo("dashboard");
            Notification.show("Brak uprawnień do tej strony", 3000, Notification.Position.MIDDLE);
        }
    }
}
