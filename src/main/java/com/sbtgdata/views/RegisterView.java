package com.sbtgdata.views;

import com.sbtgdata.data.UserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.springframework.beans.factory.annotation.Autowired;

@Route("register")
@PageTitle("Rejestracja")
@AnonymousAllowed
public class RegisterView extends VerticalLayout {
    
    @Autowired
    private UserService userService;
    
    public RegisterView(UserService userService) {
        this.userService = userService;
        
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        
        H1 title = new H1("Rejestracja");
        
        EmailField emailField = new EmailField("Email");
        emailField.setWidth("300px");
        
        PasswordField passwordField = new PasswordField("Hasło");
        passwordField.setWidth("300px");
        
        PasswordField confirmPasswordField = new PasswordField("Potwierdź hasło");
        confirmPasswordField.setWidth("300px");
        
        ComboBox<String> roleComboBox = new ComboBox<>("Rola");
        roleComboBox.setItems("księgowy", "administrator");
        roleComboBox.setWidth("300px");
        roleComboBox.setPlaceholder("Wybierz rolę");
        
        Button registerButton = new Button("Zarejestruj się", e -> {
            String email = emailField.getValue();
            String password = passwordField.getValue();
            String confirmPassword = confirmPasswordField.getValue();
            String role = roleComboBox.getValue();
            
            if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || role == null) {
                Notification.show("Proszę wypełnić wszystkie pola", 3000, Notification.Position.MIDDLE);
                return;
            }
            
            if (!password.equals(confirmPassword)) {
                Notification.show("Hasła nie są identyczne", 3000, Notification.Position.MIDDLE);
                return;
            }
            
            if (password.length() < 6) {
                Notification.show("Hasło musi mieć co najmniej 6 znaków", 3000, Notification.Position.MIDDLE);
                return;
            }
            
            try {
                userService.registerUser(email, password, role);
                Notification.show("Rejestracja zakończona pomyślnie", 3000, Notification.Position.MIDDLE);
                getUI().ifPresent(ui -> ui.navigate("login"));
            } catch (IllegalArgumentException ex) {
                Notification.show(ex.getMessage(), 3000, Notification.Position.MIDDLE);
            }
        });
        
        Button loginButton = new Button("Powrót do logowania", e -> {
            getUI().ifPresent(ui -> ui.navigate("login"));
        });
        
        add(title, emailField, passwordField, confirmPasswordField, roleComboBox, registerButton, loginButton);
    }
}

