package com.sbtgdata.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Image;
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
        Image logo = new Image("/images/sbtgdata_logo1.jpg", "Logo Sbtgdata");
        logo.setMaxWidth("220px");
        logo.setWidth("60%");
        logo.setHeight("auto");
        logo.getStyle().set("max-width", "220px");

        login.setAction("login");
        login.setForgotPasswordButtonVisible(false);

        LoginI18n i18n = LoginI18n.createDefault();
        i18n.setHeader(new LoginI18n.Header());
        i18n.getHeader().setTitle("Logowanie");
        i18n.getForm().setTitle("Zaloguj się");
        i18n.getForm().setUsername("Email");
        i18n.getForm().setPassword("Hasło");
        i18n.getForm().setSubmit("Zaloguj się");
        LoginI18n.ErrorMessage errorMessage = new LoginI18n.ErrorMessage();
        errorMessage.setTitle("Logowanie nieudane");
        errorMessage.setMessage("Sprawdź email i hasło, a następnie spróbuj ponownie.");
        i18n.setErrorMessage(errorMessage);
        login.setI18n(i18n);

        Button registerButton = new Button("Rejestracja", e -> {
            getUI().ifPresent(ui -> ui.navigate("register"));
        });

        add(logo, login, registerButton);
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
