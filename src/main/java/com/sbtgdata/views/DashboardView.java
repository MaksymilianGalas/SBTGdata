package com.sbtgdata.views;

import com.sbtgdata.config.SecurityService;
import com.sbtgdata.data.User;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

@Route(value = "dashboard", layout = MainLayout.class)
@PageTitle("Panel główny")
@PermitAll
public class DashboardView extends VerticalLayout {
    
    public DashboardView(SecurityService securityService) {
        
        H1 title = new H1("Panel główny");
        
        User currentUser = securityService.getCurrentUser();
        String welcomeText = "Witaj!";
        
        if (currentUser != null) {
            welcomeText = "Witaj, " + currentUser.getEmail() + "!";
            Paragraph roleInfo = new Paragraph("Twoja rola: " + String.join(", ", currentUser.getRoles()));
            add(title, new Paragraph(welcomeText), roleInfo);
        } else {
            add(title, new Paragraph(welcomeText));
        }
        
        setSizeFull();
        setPadding(true);
    }
}

