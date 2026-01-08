package com.sbtgdata.views;

import com.sbtgdata.config.SecurityService;
import com.sbtgdata.data.FlowError;
import com.sbtgdata.data.FlowErrorService;
import com.sbtgdata.data.User;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Route(value = "errors", layout = MainLayout.class)
@PageTitle("Błędy")
@PermitAll
public class ErrorsView extends VerticalLayout {

    private final FlowErrorService flowErrorService;
    private final SecurityService securityService;
    private final Grid<FlowError> grid = new Grid<>(FlowError.class);

    @Autowired
    public ErrorsView(FlowErrorService flowErrorService, SecurityService securityService) {
        this.flowErrorService = flowErrorService;
        this.securityService = securityService;

        setSizeFull();
        configureGrid();

        add(new H2("Moje błędy"), grid);
        updateList();
    }

    private void configureGrid() {
        grid.setColumns();
        grid.addColumn(error -> {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return error.getDate() != null ? error.getDate().format(formatter) : "";
        }).setHeader("Data").setWidth("180px").setFlexGrow(0);

        grid.addColumn(FlowError::getFlowId).setHeader("ID Przepływu").setWidth("200px");
        grid.addColumn(FlowError::getMessage).setHeader("Komunikat błędu").setAutoWidth(true).setFlexGrow(1);

        grid.addComponentColumn(error -> {
            Button deleteButton = new Button(new Icon(VaadinIcon.CLOSE_SMALL));
            deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
            deleteButton.addClickListener(e -> {
                try {
                    flowErrorService.deleteError(error.getId());
                    updateList();
                    Notification.show("Błąd usunięty", 2000, Notification.Position.MIDDLE);
                } catch (Exception ex) {
                    Notification.show("Błąd podczas usuwania: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
                }
            });
            return deleteButton;
        }).setHeader("Akcje").setWidth("100px").setFlexGrow(0);
    }

    private void updateList() {
        User currentUser = securityService.getCurrentUser();
        if (currentUser != null) {
            List<FlowError> errors = flowErrorService.getErrorsByUserId(currentUser.getId());
            grid.setItems(errors);
        }
    }
}
