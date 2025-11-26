package com.sbtgdata.views;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

@Route("welcome")
@PageTitle("Witaj")
@PermitAll
public class WelcomeView extends VerticalLayout {

    public WelcomeView() {
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        setSizeFull();

        add(new H1("Witaj w systemie!"));
        add(new Paragraph("Twoje konto zostało utworzone, ale nie masz jeszcze przypisanych uprawnień."));
        add(new Paragraph("Skontaktuj się z administratorem, aby uzyskać dostęp do funkcjonalności."));
    }
}
