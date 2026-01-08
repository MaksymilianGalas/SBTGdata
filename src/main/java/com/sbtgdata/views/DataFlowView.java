package com.sbtgdata.views;

import com.sbtgdata.config.SecurityService;
import com.sbtgdata.data.DataFlow;
import com.sbtgdata.data.DataFlowService;
import com.sbtgdata.data.FlowError;
import com.sbtgdata.data.FlowErrorService;
import com.sbtgdata.data.User;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.HashMap;
import java.util.Map;

@Route(value = "dataflows", layout = MainLayout.class)
@PageTitle("Przepływy danych")
@PermitAll
public class DataFlowView extends VerticalLayout {

    private final DataFlowService dataFlowService;
    private final SecurityService securityService;
    private final FlowErrorService flowErrorService;
    private final Grid<DataFlow> grid = new Grid<>(DataFlow.class);

    @Value("${entry.data.flow.url}")
    private String entryDataFlowUrl;

    @Autowired
    public DataFlowView(DataFlowService dataFlowService, SecurityService securityService,
            FlowErrorService flowErrorService) {
        this.dataFlowService = dataFlowService;
        this.securityService = securityService;
        this.flowErrorService = flowErrorService;

        setSizeFull();
        configureGrid();

        Button addButton = new Button("Dodaj nowy przepływ", e -> openFlowEditor(new DataFlow()));

        add(new H2("Moje przepływy danych"), addButton, grid);
        updateList();
    }

    private void configureGrid() {
        grid.setColumns("name");
        grid.addColumn(flow -> flow.getPackages() != null && !flow.getPackages().isEmpty() ? "Tak"
                : "Nie")
                .setHeader("Dodatkowe biblioteki");

        grid.addColumn(DataFlow::getStatus).setHeader("Status");

        grid.addComponentColumn(flow -> {
            if (flow.getId() == null) {
                return new Paragraph("-");
            }
            User currentUser = securityService.getCurrentUser();
            String apiKey = currentUser != null ? currentUser.getApiKey() : "";
            String url = entryDataFlowUrl + "/" + flow.getId() + "?API_KEY=" + apiKey;

            Button copyButton = new Button(new Icon(VaadinIcon.COPY));
            copyButton.getElement().setAttribute("title", "Kopiuj URL");
            copyButton.addClickListener(e -> {
                getUI().ifPresent(ui -> {
                    ui.getPage().executeJs(
                            "return navigator.clipboard.writeText($0).then(() => true, () => false)",
                            url).then(Boolean.class, success -> {
                                if (Boolean.TRUE.equals(success)) {
                                    Notification.show("URL skopiowany", 2000, Notification.Position.MIDDLE);
                                }
                            });
                });
            });

            Paragraph urlText = new Paragraph(url);
            urlText.getStyle().set("font-size", "0.875rem");
            urlText.getStyle().set("margin", "0");

            HorizontalLayout layout = new HorizontalLayout(urlText, copyButton);
            layout.setAlignItems(Alignment.CENTER);
            layout.setSpacing(false);
            return layout;
        }).setHeader("URL do wysyłania danych").setAutoWidth(true);

        grid.addComponentColumn(flow -> {
            if (flow.getId() == null) {
                return new Paragraph("-");
            }
            long errorCount = flowErrorService.getUniqueErrorsByFlowId(flow.getId()).size();
            if (errorCount > 0) {
                Button errorsButton = new Button("Błędy (" + errorCount + ")",
                        new Icon(VaadinIcon.EXCLAMATION_CIRCLE_O));
                errorsButton.addClickListener(e -> openErrorsDialog(flow));
                return errorsButton;
            }
            return new Paragraph("Brak błędów");
        }).setHeader("Błędy");

        grid.addComponentColumn(flow -> {
            Button editButton = new Button("Edytuj", e -> openFlowEditor(flow));
            Button deleteButton = new Button("Usuń", e -> {
                try {
                    dataFlowService.delete(flow);
                    updateList();
                    Notification.show("Przepływ usunięty");
                } catch (IllegalArgumentException ex) {
                    Notification.show(ex.getMessage(), 5000, Notification.Position.MIDDLE);
                }
            });

            Button toggleButton;
            if ("RUNNING".equals(flow.getStatus())) {
                toggleButton = new Button("Zatrzymaj przepływ", e -> {
                    try {
                        dataFlowService.stopFlow(flow.getId());
                        updateList();
                        Notification.show("Przepływ zatrzymany");
                    } catch (Exception ex) {
                        Notification.show("Błąd: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
                    }
                });
            } else {
                toggleButton = new Button("Uruchom przepływ", e -> {
                    try {
                        dataFlowService.startFlow(flow.getId());
                        updateList();
                        Notification.show("Przepływ uruchomiony");
                    } catch (Exception ex) {
                        Notification.show("Błąd: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
                    }
                });
            }

            return new HorizontalLayout(editButton, deleteButton, toggleButton);
        }).setHeader("Akcje");
    }

    private void updateList() {
        String currentUserEmail = securityService.getAuthenticatedUser().orElse(null);
        if (currentUserEmail != null) {
            grid.setItems(dataFlowService.findByOwnerEmail(currentUserEmail));
        }
    }

    private void openFlowEditor(DataFlow flow) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(flow.getId() == null ? "Nowy przepływ danych" : "Edytuj przepływ");
        dialog.setWidth("800px");

        VerticalLayout dialogLayout = new VerticalLayout();

        TextField nameField = new TextField("Nazwa przepływu");
        nameField.setWidthFull();
        if (flow.getName() != null) {
            nameField.setValue(flow.getName());
        }

        TextArea functionArea = new TextArea("Kod funkcji Python");
        functionArea.setWidthFull();
        functionArea.setHeight("300px");
        functionArea.getStyle().set("font-family", "monospace");
        if (flow.getFunction() != null) {
            functionArea.setValue(flow.getFunction());
        }

        Checkbox hasLibrariesCheckbox = new Checkbox("Mam dodatkowe biblioteki");
        TextArea librariesArea = new TextArea("Dodatkowe biblioteki (jedna na linię)");
        librariesArea.setWidthFull();
        librariesArea.setHeight("100px");
        librariesArea.setVisible(false);

        if (flow.getPackages() != null && !flow.getPackages().isEmpty()) {
            hasLibrariesCheckbox.setValue(true);
            librariesArea.setValue(String.join("\n", flow.getPackages()));
            librariesArea.setVisible(true);
        }

        hasLibrariesCheckbox.addValueChangeListener(e -> {
            librariesArea.setVisible(e.getValue());
        });

        dialogLayout.add(nameField, functionArea, hasLibrariesCheckbox, librariesArea);
        dialog.add(dialogLayout);

        Button saveButton = new Button("Zapisz", e -> {
            if (nameField.getValue().isEmpty()) {
                Notification.show("Nazwa przepływu jest wymagana", 3000, Notification.Position.MIDDLE);
                return;
            }

            flow.setName(nameField.getValue());
            flow.setFunction(functionArea.getValue());
            if (hasLibrariesCheckbox.getValue()) {
                String[] libs = librariesArea.getValue().split("\n");
                flow.setPackages(java.util.Arrays.asList(libs));
            } else {
                flow.setPackages(null);
            }

            if (flow.getId() == null) {
                String currentUserEmail = securityService.getAuthenticatedUser().orElse(null);
                flow.setOwnerEmail(currentUserEmail);
            }

            dataFlowService.save(flow);
            updateList();
            dialog.close();
            Notification.show("Przepływ zapisany");
        });

        Button cancelButton = new Button("Anuluj", e -> dialog.close());

        dialog.getFooter().add(cancelButton, saveButton);
        dialog.open();
    }

    private HorizontalLayout createSchemaField(String varName, String varType, VerticalLayout container) {
        ComboBox<String> typeCombo = new ComboBox<>("Typ");
        typeCombo.setItems("int", "float", "string", "boolean", "list", "dict");
        typeCombo.setValue(varType);
        typeCombo.setWidth("150px");

        TextField varNameField = new TextField("Nazwa zmiennej");
        varNameField.setValue(varName);
        varNameField.setWidth("200px");

        Button removeButton = new Button("Usuń", e -> {
            container.remove(container.getChildren()
                    .filter(c -> c instanceof HorizontalLayout && c.equals(e.getSource().getParent().get()))
                    .findFirst()
                    .orElse(null));
        });

        HorizontalLayout fieldLayout = new HorizontalLayout(typeCombo, varNameField, removeButton);
        fieldLayout.setAlignItems(Alignment.END);
        return fieldLayout;
    }

    private void openErrorsDialog(DataFlow flow) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Błędy dla przepływu: " + flow.getName());
        dialog.setWidth("800px");

        VerticalLayout dialogLayout = new VerticalLayout();

        java.util.List<FlowError> errors = flowErrorService.getUniqueErrorsByFlowId(flow.getId());

        if (errors.isEmpty()) {
            dialogLayout.add(new Paragraph("Brak błędów dla tego przepływu."));
        } else {
            Grid<FlowError> errorsGrid = new Grid<>(FlowError.class);
            errorsGrid.setColumns("message", "date");
            errorsGrid.addComponentColumn(error -> {
                Button acknowledgeButton = new Button("Wiem o tym", e -> {
                    flowErrorService.deleteError(error.getId());
                    errorsGrid.setItems(flowErrorService.getErrorsByFlowId(flow.getId()));
                    updateList();
                    Notification.show("Błąd został usunięty", 2000, Notification.Position.MIDDLE);
                    if (flowErrorService.getErrorsByFlowId(flow.getId()).isEmpty()) {
                        dialog.close();
                    }
                });
                return acknowledgeButton;
            }).setHeader("Akcja");

            errorsGrid.setItems(errors);
            dialogLayout.add(errorsGrid);
        }

        dialog.add(dialogLayout);

        Button closeButton = new Button("Zamknij", e -> dialog.close());
        dialog.getFooter().add(closeButton);
        dialog.open();
    }
}
