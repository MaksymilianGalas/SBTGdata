package com.sbtgdata.views;

import com.sbtgdata.config.SecurityService;
import com.sbtgdata.data.RoleRepository;
import com.sbtgdata.data.ViewService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.RouterLink;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MainLayout extends AppLayout implements BeforeEnterObserver {

    private final SecurityService securityService;
    private final RoleRepository roleRepository;
    private final ViewService viewService;

    @Autowired
    public MainLayout(SecurityService securityService, RoleRepository roleRepository, ViewService viewService) {
        this.securityService = securityService;
        this.roleRepository = roleRepository;
        this.viewService = viewService;

        try {
            createHeader();
            createDrawer();
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback UI for debugging
            addToNavbar(new H1("Error: " + e.getMessage()));
            VerticalLayout errorLayout = new VerticalLayout();
            errorLayout.add(new H1("Initialization Error"));
            errorLayout.add(new com.vaadin.flow.component.html.Pre(e.toString()));
            for (StackTraceElement ste : e.getStackTrace()) {
                errorLayout.add(new com.vaadin.flow.component.html.Div(ste.toString()));
            }
            addToDrawer(errorLayout);
        }
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
                logoutButton);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.setWidthFull();
        header.expand(logo);
        header.addClassNames("py-0", "px-m");

        addToNavbar(header);
    }

    private void createDrawer() {
        VerticalLayout drawer = new VerticalLayout();

        Set<String> allowedViews = getAllowedViewsForCurrentUser();
        Map<String, String> allViews = viewService.getAllViews();
        boolean isAdmin = securityService.hasRole("ADMIN");

        // Sort views by name or some other criteria if needed
        allViews.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .forEach(entry -> {
                    String className = entry.getKey();
                    String viewName = entry.getValue();

                    // Skip internal/public views
                    if (className.contains("LoginView") ||
                            className.contains("RegisterView") ||
                            className.contains("WelcomeView") ||
                            className.contains("RootView")) {
                        return;
                    }

                    // Special handling for Admin Panel - only show if user is explicitly ADMIN
                    if (className.contains("RoleManagementView") && !isAdmin) {
                        return;
                    }

                    if (isAdmin || allowedViews.contains(className)) {
                        try {
                            Class<?> clazz = Class.forName(className);
                            // Only add if it's a Component (can be used in RouterLink)
                            if (Component.class.isAssignableFrom(clazz)) {
                                @SuppressWarnings("unchecked")
                                Class<? extends Component> viewClass = (Class<? extends Component>) clazz;
                                drawer.add(new RouterLink(viewName, viewClass));
                            }
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                });

        addToDrawer(drawer);
    }

    private Set<String> getAllowedViewsForCurrentUser() {
        Set<String> allowedViews = new HashSet<>();
        var user = securityService.getCurrentUser();
        if (user != null && user.getRoles() != null) {
            for (String roleName : user.getRoles()) {
                roleRepository.findByName(roleName).ifPresent(role -> {
                    if (role.getAllowedViews() != null) {
                        allowedViews.addAll(role.getAllowedViews());
                    }
                });
            }
        }
        return allowedViews;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Class<?> targetClass = event.getNavigationTarget();
        String targetClassName = targetClass.getName();

        // Always allow public views
        if (targetClassName.contains("LoginView") ||
                targetClassName.contains("RegisterView") ||
                targetClassName.contains("WelcomeView")) {
            return;
        }

        // Always allow ADMIN
        if (securityService.hasRole("ADMIN")) {
            return;
        }

        // Check dynamic permissions
        Set<String> allowedViews = getAllowedViewsForCurrentUser();
        if (!allowedViews.contains(targetClassName)) {
            // Reroute to WelcomeView if access denied
            event.rerouteTo(WelcomeView.class);
        }
    }
}
