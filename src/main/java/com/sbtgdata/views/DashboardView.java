package com.sbtgdata.views;

import com.sbtgdata.config.SecurityService;
import com.sbtgdata.data.ErrorNotificationService;
import com.sbtgdata.data.User;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Route(value = "dashboard", layout = MainLayout.class)
@PageTitle("Panel główny")
@PermitAll
public class DashboardView extends VerticalLayout {
    
    private final ErrorNotificationService errorNotificationService;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    public DashboardView(SecurityService securityService, ErrorNotificationService errorNotificationService) {
        this.errorNotificationService = errorNotificationService;
        
        H1 title = new H1("Panel główny");
        
        User currentUser = securityService.getCurrentUser();
        String welcomeText = "Witaj!";
        
        if (currentUser != null) {
            welcomeText = "Witaj, " + currentUser.getEmail() + "!";
            Paragraph roleInfo = new Paragraph("Twoja rola: " + String.join(", ", currentUser.getRoles()));
            add(title, new Paragraph(welcomeText), roleInfo);
            
            scheduler.scheduleAtFixedRate(() -> {
                UI ui = UI.getCurrent();
                if (ui != null) {
                    ui.access(() -> {
                        List<String> errors = errorNotificationService.getAndClearErrors(currentUser.getId());
                        for (String error : errors) {
                            Notification notification = new Notification(error, 10000, Notification.Position.TOP_CENTER);
                            notification.open();
                        }
                    });
                }
            }, 2, 2, TimeUnit.SECONDS);
        } else {
            add(title, new Paragraph(welcomeText));
        }
        
        setSizeFull();
        setPadding(true);
    }
    
    @Override
    protected void onDetach(DetachEvent detachEvent) {
        scheduler.shutdown();
        super.onDetach(detachEvent);
    }
}

