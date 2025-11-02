package com.sbtgdata.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("login")
@PageTitle("Logowanie")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver {
    
    private LoginForm login = new LoginForm();
    
    public LoginView() {
        addClassName("login-view");
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        
        login.setAction("login");
        login.setForgotPasswordButtonVisible(false);
        
        LoginI18n i18n = LoginI18n.createDefault();
        i18n.setHeader(new LoginI18n.Header());
        i18n.getHeader().setTitle("Logowanie");
        i18n.getForm().setUsername("Email");
        i18n.getForm().setPassword("Hasło");
        i18n.getForm().setSubmit("Zaloguj się");
        login.setI18n(i18n);
        
        Button registerButton = new Button("Rejestracja", e -> {
            getUI().ifPresent(ui -> ui.navigate("register"));
        });
        
        add(login, registerButton);
    }
    
    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        if (beforeEnterEvent.getLocation()
            .getQueryParameters()
            .getParameters()
            .containsKey("error")) {
            login.setError(true);
        }
    }
}

