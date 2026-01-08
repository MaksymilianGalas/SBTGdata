package com.sbtgdata.views;

import com.sbtgdata.config.SecurityService;
import com.sbtgdata.data.User;
import com.sbtgdata.data.UserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

@Route(value = "api-key", layout = MainLayout.class)
@PageTitle("Zarządzanie kluczem API")
@PermitAll
public class ApiKeyView extends VerticalLayout {

    private final UserService userService;
    private final SecurityService securityService;
    private PasswordField apiKeyField;
    private Button toggleVisibilityButton;
    private String currentApiKey;

    public ApiKeyView(UserService userService, SecurityService securityService) {
        this.userService = userService;
        this.securityService = securityService;

        setSizeFull();
        setPadding(true);

        H2 title = new H2("Zarządzanie kluczem API");

        User currentUser = securityService.getCurrentUser();
        if (currentUser == null) {
            add(new Paragraph("Musisz być zalogowany, aby zarządzać kluczem API."));
            return;
        }

        currentApiKey = userService.getApiKey(currentUser.getId());
        if (currentApiKey == null) {
            currentApiKey = "";
        }

        apiKeyField = new PasswordField("Twój klucz API");
        apiKeyField.setValue(currentApiKey);
        apiKeyField.setWidthFull();
        apiKeyField.setReadOnly(true);

        toggleVisibilityButton = new Button(new Icon(VaadinIcon.EYE));
        toggleVisibilityButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        toggleVisibilityButton.addClickListener(e -> {
            apiKeyField.setRevealButtonVisible(!apiKeyField.isRevealButtonVisible());
        });

        Button regenerateButton = new Button("Wygeneruj nowy klucz", e -> {
            Dialog confirmDialog = new Dialog();
            confirmDialog.setHeaderTitle("Potwierdzenie");

            VerticalLayout dialogLayout = new VerticalLayout();
            dialogLayout.add(new Paragraph("Czy na pewno chcesz wygenerować nowy klucz API?"));
            dialogLayout.add(new Paragraph("Stary klucz przestanie działać."));

            Button confirmButton = new Button("Tak, wygeneruj", ev -> {
                try {
                    String newApiKey = userService.regenerateApiKey(currentUser.getId());
                    currentApiKey = newApiKey;
                    apiKeyField.setValue(newApiKey);
                    Notification.show("Nowy klucz API został wygenerowany", 3000, Notification.Position.MIDDLE);
                    confirmDialog.close();
                } catch (IllegalArgumentException ex) {
                    Notification.show(ex.getMessage(), 3000, Notification.Position.MIDDLE);
                }
            });

            Button cancelButton = new Button("Anuluj", ev -> confirmDialog.close());

            confirmDialog.add(dialogLayout);
            confirmDialog.getFooter().add(cancelButton, confirmButton);
            confirmDialog.open();
        });

        Button copyButton = new Button("Kopiuj klucz", e -> {
            if (currentApiKey != null && !currentApiKey.isEmpty()) {
                getUI().ifPresent(ui -> {
                    ui.getPage().executeJs(
                            "return navigator.clipboard.writeText($0).then(() => true, () => false)",
                            currentApiKey).then(Boolean.class, success -> {
                                if (Boolean.TRUE.equals(success)) {
                                    Notification.show("Klucz skopiowany do schowka", 2000,
                                            Notification.Position.MIDDLE);
                                } else {
                                    Notification.show("Nie udało się skopiować klucza", 2000,
                                            Notification.Position.MIDDLE);
                                }
                            });
                });
            }
        });

        HorizontalLayout apiKeyLayout = new HorizontalLayout(apiKeyField, toggleVisibilityButton);
        apiKeyLayout.setWidthFull();
        apiKeyLayout.setAlignItems(Alignment.END);
        apiKeyField.getStyle().set("flex-grow", "1");

        add(title,
                new Paragraph("Twój klucz API jest używany do autoryzacji przy wysyłaniu danych do przepływów."),
                apiKeyLayout,
                new HorizontalLayout(regenerateButton, copyButton));
    }
}
