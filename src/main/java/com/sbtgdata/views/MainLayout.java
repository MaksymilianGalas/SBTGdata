package com.sbtgdata.views;

import com.sbtgdata.config.SecurityService;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.RouterLink;
import org.springframework.beans.factory.annotation.Autowired;

public class MainLayout extends AppLayout {
    
    @Autowired
    private SecurityService securityService;
    
    public MainLayout(SecurityService securityService) {
        this.securityService = securityService;
        
        createHeader();
        createDrawer();
    }
    
    private void createHeader() {
        H1 logo = new H1("Sbtgdata");
        logo.addClassNames("text-l", "m-m");
        
        Button logoutButton = new Button("Wyloguj", e -> {
            securityService.logout();
        });
        
        HorizontalLayout header = new HorizontalLayout(
            new DrawerToggle(),
            logo,
            logoutButton
        );
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.setWidthFull();
        header.expand(logo);
        header.addClassNames("py-0", "px-m");
        
        addToNavbar(header);
    }
    
    private void createDrawer() {
        VerticalLayout drawer = new VerticalLayout();
        
        RouterLink dashboardLink = new RouterLink("Dashboard", DashboardView.class);
        drawer.add(dashboardLink);
        
        // Panel administratora - tylko dla administratorów
        if (securityService.hasRole("administrator")) {
            RouterLink adminLink = new RouterLink("Panel Administratora", AdminPanelView.class);
            drawer.add(adminLink);
        }
        
        // Widok testowy 1 - dostępny dla wszystkich zalogowanych
        RouterLink test1Link = new RouterLink("Widok Testowy 1", TestView1.class);
        drawer.add(test1Link);
        
        // Widok testowy 2 - tylko dla administratorów
        if (securityService.hasRole("administrator")) {
            RouterLink test2Link = new RouterLink("Widok Testowy 2", TestView2.class);
            drawer.add(test2Link);
        }
        
        addToDrawer(drawer);
    }
}

