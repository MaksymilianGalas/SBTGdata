package com.sbtgdata.views;

import com.sbtgdata.config.SecurityService;
import com.sbtgdata.data.DataFlow;
import com.sbtgdata.data.DataFlowService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.Collectors;

@Route(value = "admin/dataflows", layout = MainLayout.class)
@PageTitle("Przepływy danych - Admin")
@RolesAllowed("ADMIN")
public class AdminDataFlowView extends VerticalLayout implements BeforeEnterObserver {

    private final DataFlowService dataFlowService;
    private final SecurityService securityService;
    private final Grid<DataFlow> grid = new Grid<>(DataFlow.class);
    private List<DataFlow> allFlows;

    @Autowired
    public AdminDataFlowView(DataFlowService dataFlowService, SecurityService securityService) {
        this.dataFlowService = dataFlowService;
        this.securityService = securityService;

        setSizeFull();
        setPadding(true);

        H2 title = new H2("Wszystkie przepływy danych");

        TextField filterField = new TextField("Filtruj po emailu użytkownika");
        filterField.setPlaceholder("Wpisz email...");
        filterField.setClearButtonVisible(true);
        filterField.setValueChangeMode(ValueChangeMode.LAZY);
        filterField.addValueChangeListener(e -> updateList(e.getValue()));

        configureGrid();

        Button refreshButton = new Button("Odśwież", e -> updateList(null));

        add(title, filterField, refreshButton, grid);
        updateList(null);
    }

    private void configureGrid() {
        grid.setColumns("name", "ownerEmail");
        grid.addColumn(flow -> flow.getPackages() != null && !flow.getPackages().isEmpty() ? "Tak"
                : "Nie")
                .setHeader("Dodatkowe biblioteki");

        grid.addComponentColumn(flow -> {
            Button viewButton = new Button("Podgląd", e -> openFlowViewer(flow));
            Button deleteButton = new Button("Usuń", e -> {
                dataFlowService.delete(flow);
                updateList(null);
                Notification.show("Przepływ usunięty");
            });
            return new HorizontalLayout(viewButton, deleteButton);
        }).setHeader("Akcje");
    }

    private void updateList(String emailFilter) {
        allFlows = dataFlowService.findAll();

        if (emailFilter != null && !emailFilter.isEmpty()) {
            List<DataFlow> filtered = allFlows.stream()
                    .filter(flow -> flow.getOwnerEmail().toLowerCase().contains(emailFilter.toLowerCase()))
                    .collect(Collectors.toList());
            grid.setItems(filtered);
        } else {
            grid.setItems(allFlows);
        }
    }

    private void openFlowViewer(DataFlow flow) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Podgląd przepływu: " + flow.getName());
        dialog.setWidth("800px");

        VerticalLayout dialogLayout = new VerticalLayout();

        VerticalLayout codeLayout = new VerticalLayout();
        codeLayout.add(new H3("Kod Python:"));
        Pre codeBlock = new Pre(flow.getFunction() != null ? flow.getFunction() : "Brak kodu");
        codeBlock.getStyle().set("background-color", "#f5f5f5");
        codeBlock.getStyle().set("padding", "10px");
        codeBlock.getStyle().set("border-radius", "5px");
        codeLayout.add(codeBlock);

        if (flow.getPackages() != null && !flow.getPackages().isEmpty()) {
            VerticalLayout libsLayout = new VerticalLayout();
            libsLayout.add(new H3("Dodatkowe biblioteki:"));
            Pre libsBlock = new Pre(String.join("\n", flow.getPackages()));
            libsBlock.getStyle().set("background-color", "#f5f5f5");
            libsBlock.getStyle().set("padding", "10px");
            libsBlock.getStyle().set("border-radius", "5px");
            libsLayout.add(libsBlock);
            dialogLayout.add(libsLayout);
        }

        dialogLayout.add(codeLayout);
        dialog.add(dialogLayout);

        Button closeButton = new Button("Zamknij", e -> dialog.close());
        dialog.getFooter().add(closeButton);
        dialog.open();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!securityService.hasRole("ADMIN")) {
            event.rerouteTo("dashboard");
            Notification.show("Brak uprawnień do tej strony", 3000, Notification.Position.MIDDLE);
        }
    }
}
