package com.sbtgdata.views;

import com.sbtgdata.data.Role;
import com.sbtgdata.data.RoleRepository;
import com.sbtgdata.data.ViewService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

@Route(value = "admin/roles", layout = MainLayout.class)
@PageTitle("Zarządzanie Rolami")
@RolesAllowed("ADMIN")
public class RoleManagementView extends VerticalLayout {

    private final RoleRepository roleRepository;
    private final ViewService viewService;
    private final Grid<Role> grid = new Grid<>(Role.class);

    @Autowired
    public RoleManagementView(RoleRepository roleRepository, ViewService viewService) {
        this.roleRepository = roleRepository;
        this.viewService = viewService;

        setSizeFull();
        configureGrid();

        Button addRoleButton = new Button("Dodaj nową rolę", e -> openRoleEditor(new Role()));

        add(new H2("Zarządzanie Rolami"), addRoleButton, grid);
        updateList();
    }

    private void configureGrid() {
        grid.setColumns("name");
        grid.addColumn(role -> role.getAllowedViews().size() + " widoków").setHeader("Uprawnienia");

        grid.addComponentColumn(role -> {
            Button editButton = new Button("Edytuj", e -> openRoleEditor(role));
            Button deleteButton = new Button("Usuń", e -> {
                roleRepository.delete(role);
                updateList();
                Notification.show("Rola usunięta");
            });
            return new HorizontalLayout(editButton, deleteButton);
        });
    }

    private void updateList() {
        grid.setItems(roleRepository.findAll());
    }

    private void openRoleEditor(Role role) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(role.getId() == null ? "Nowa Rola" : "Edytuj Rolę");

        VerticalLayout dialogLayout = new VerticalLayout();

        TextField nameField = new TextField("Nazwa Roli");
        if (role.getName() != null) {
            nameField.setValue(role.getName());
        }

        Map<String, String> availableViews = viewService.getAllViews();

        // Filter out public/internal views that should be accessible to everyone
        availableViews.entrySet().removeIf(entry -> {
            String className = entry.getKey();
            return className.contains("LoginView") ||
                    className.contains("RegisterView") ||
                    className.contains("WelcomeView") ||
                    className.contains("RootView");
        });

        CheckboxGroup<String> viewsGroup = new CheckboxGroup<>();
        viewsGroup.setLabel("Dostępne Widoki");
        viewsGroup.setItems(availableViews.keySet());
        viewsGroup.setItemLabelGenerator(availableViews::get);

        if (role.getAllowedViews() != null) {
            viewsGroup.setValue(role.getAllowedViews());
        }

        dialogLayout.add(nameField, viewsGroup);
        dialog.add(dialogLayout);

        Button saveButton = new Button("Zapisz", e -> {
            role.setName(nameField.getValue());
            role.setAllowedViews(viewsGroup.getValue());
            roleRepository.save(role);
            updateList();
            dialog.close();
            Notification.show("Rola zapisana");
        });

        Button cancelButton = new Button("Anuluj", e -> dialog.close());

        dialog.getFooter().add(cancelButton, saveButton);
        dialog.open();
    }
}
