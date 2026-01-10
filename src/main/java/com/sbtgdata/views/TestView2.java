package com.sbtgdata.views;

import com.sbtgdata.config.SecurityService;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Autowired;

// @Route(value = "test2", layout = MainLayout.class)
@PageTitle("Widok Testowy 2")
@RolesAllowed("ADMIN")
public class TestView2 extends VerticalLayout implements BeforeEnterObserver {

    @Autowired
    private SecurityService securityService;

    public TestView2(SecurityService securityService) {
        this.securityService = securityService;

        setSizeFull();
        setPadding(true);

        H1 title = new H1("Widok Testowy 2");
        Paragraph description = new Paragraph(
                "To jest widok testowy numer 2. " +
                        "Dostępny TYLKO dla administratorów.");

        Paragraph content = new Paragraph(
                "Tutaj można dodać funkcjonalność dostępną wyłącznie dla administratorów.");

        add(title, description, content);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!securityService.hasRole("ADMIN")) {
            event.rerouteTo("dataflows");
            Notification.show("Brak uprawnień do tej strony", 3000, Notification.Position.MIDDLE);
        }
    }
}
