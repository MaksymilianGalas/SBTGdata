package com.sbtgdata.views;

import com.sbtgdata.config.SecurityService;
import com.sbtgdata.data.DataFlow;
import com.sbtgdata.data.DataFlowService;
import com.sbtgdata.data.FlowError;
import com.sbtgdata.data.FlowErrorService;
import com.sbtgdata.data.User;
import com.sbtgdata.data.UserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
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

import java.util.HashMap;
import java.util.Map;

@Route(value = "dataflows", layout = MainLayout.class)
@PageTitle("Przepływy danych")
@PermitAll
public class DataFlowView extends VerticalLayout {

    private final DataFlowService dataFlowService;
    private final SecurityService securityService;
    private final UserService userService;
    private final FlowErrorService flowErrorService;
    private final Grid<DataFlow> grid = new Grid<>(DataFlow.class);

    @Autowired
    public DataFlowView(DataFlowService dataFlowService, SecurityService securityService,
            UserService userService, FlowErrorService flowErrorService) {
        this.dataFlowService = dataFlowService;
        this.securityService = securityService;
        this.userService = userService;
        this.flowErrorService = flowErrorService;

        setSizeFull();
        configureGrid();

        Button addButton = new Button("Dodaj nowy przepływ", e -> openFlowEditor(new DataFlow()));

        add(new H2("Moje przepływy danych"), addButton, grid);
        updateList();
    }

    private void configureGrid() {
        grid.setColumns("name");
        grid.addColumn(flow -> flow.getInputSchema().size() + " pól wejściowych").setHeader("Schemat");
        grid.addColumn(flow -> flow.getAdditionalLibraries() != null && !flow.getAdditionalLibraries().isEmpty() ? "Tak"
                : "Nie")
                .setHeader("Dodatkowe biblioteki");

        grid.addComponentColumn(flow -> {
            if (flow.getId() == null) {
                return new Paragraph("-");
            }
            long errorCount = flowErrorService.getErrorsByFlowId(flow.getId()).size();
            if (errorCount > 0) {
                Button errorsButton = new Button("Błędy (" + errorCount + ")",
                        new Icon(VaadinIcon.EXCLAMATION_CIRCLE_O));
                errorsButton.addClickListener(e -> openErrorsDialog(flow));
                return errorsButton;
            }
            return new Paragraph("Brak błędów");
        }).setHeader("Błędy");

        grid.addComponentColumn(flow -> {
            User currentUser = securityService.getCurrentUser();
            if (currentUser == null) {
                return new Paragraph("Brak dostępu");
            }
            String apiKey = userService.getApiKey(currentUser.getId());
            if (apiKey == null || apiKey.isEmpty()) {
                return new Paragraph("Brak klucza API");
            }
            try {
                String flowUrl = dataFlowService.getDataRetrievalUrl(flow.getId(), apiKey, null, null);
                Anchor urlLink = new Anchor(flowUrl, flowUrl);
                urlLink.setTarget("_blank");
                return urlLink;
            } catch (IllegalStateException ex) {
                return new Paragraph("URL nie skonfigurowany");
            }
        }).setHeader("URL pobierania danych");

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
            Button retrieveButton = new Button("Pobierz dane", e -> openDataRetrievalDialog(flow));
            return new HorizontalLayout(editButton, deleteButton, retrieveButton);
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

        VerticalLayout schemaLayout = new VerticalLayout();
        schemaLayout.add(new H2("Schemat danych wejściowych"));

        VerticalLayout schemaFieldsContainer = new VerticalLayout();

        if (flow.getInputSchema() != null && !flow.getInputSchema().isEmpty()) {
            flow.getInputSchema().forEach((varName, varType) -> {
                schemaFieldsContainer.add(createSchemaField(varName, varType, schemaFieldsContainer));
            });
        }

        Button addSchemaFieldButton = new Button("Dodaj pole", e -> {
            schemaFieldsContainer.add(createSchemaField("", "int", schemaFieldsContainer));
        });

        schemaLayout.add(schemaFieldsContainer, addSchemaFieldButton);

        TextArea pythonCodeArea = new TextArea("Kod funkcji Python");
        pythonCodeArea.setWidthFull();
        pythonCodeArea.setHeight("300px");
        pythonCodeArea.getStyle().set("font-family", "monospace");
        if (flow.getPythonCode() != null) {
            pythonCodeArea.setValue(flow.getPythonCode());
        }

        Checkbox hasLibrariesCheckbox = new Checkbox("Mam dodatkowe biblioteki");
        TextArea librariesArea = new TextArea("Dodatkowe biblioteki (jedna na linię)");
        librariesArea.setWidthFull();
        librariesArea.setHeight("100px");
        librariesArea.setVisible(false);

        if (flow.getAdditionalLibraries() != null && !flow.getAdditionalLibraries().isEmpty()) {
            hasLibrariesCheckbox.setValue(true);
            librariesArea.setValue(flow.getAdditionalLibraries());
            librariesArea.setVisible(true);
        }

        hasLibrariesCheckbox.addValueChangeListener(e -> {
            librariesArea.setVisible(e.getValue());
        });

        dialogLayout.add(nameField, schemaLayout, pythonCodeArea, hasLibrariesCheckbox, librariesArea);
        dialog.add(dialogLayout);

        Button saveButton = new Button("Zapisz", e -> {
            if (nameField.getValue().isEmpty()) {
                Notification.show("Nazwa przepływu jest wymagana", 3000, Notification.Position.MIDDLE);
                return;
            }

            Map<String, String> schema = new HashMap<>();
            schemaFieldsContainer.getChildren().forEach(component -> {
                if (component instanceof HorizontalLayout) {
                    HorizontalLayout fieldLayout = (HorizontalLayout) component;
                    var first = fieldLayout.getComponentAt(0);
                    var second = fieldLayout.getComponentAt(1);
                    if (first instanceof ComboBox<?> combo && second instanceof TextField varNameField) {
                        Object value = combo.getValue();
                        if (value instanceof String type && !varNameField.getValue().isEmpty()) {
                            schema.put(varNameField.getValue(), type);
                        }
                    }
                }
            });

            flow.setName(nameField.getValue());
            flow.setInputSchema(schema);
            flow.setPythonCode(pythonCodeArea.getValue());
            flow.setAdditionalLibraries(hasLibrariesCheckbox.getValue() ? librariesArea.getValue() : null);

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

    private void openDataRetrievalDialog(DataFlow flow) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Pobierz dane dla przepływu: " + flow.getName());
        dialog.setWidth("500px");

        VerticalLayout dialogLayout = new VerticalLayout();

        Checkbox allDataCheckbox = new Checkbox("Pobierz wszystkie dane");
        allDataCheckbox.setValue(true);

        DatePicker startDatePicker = new DatePicker("Data początkowa");
        startDatePicker.setWidthFull();
        startDatePicker.setVisible(false);

        DatePicker endDatePicker = new DatePicker("Data końcowa");
        endDatePicker.setWidthFull();
        endDatePicker.setVisible(false);

        allDataCheckbox.addValueChangeListener(e -> {
            boolean showDates = !e.getValue();
            startDatePicker.setVisible(showDates);
            endDatePicker.setVisible(showDates);
        });

        Button redirectButton = new Button("Przejdź do danych", e -> {
            User currentUser = securityService.getCurrentUser();
            if (currentUser == null) {
                Notification.show("Brak dostępu", 3000, Notification.Position.MIDDLE);
                return;
            }

            String apiKey = userService.getApiKey(currentUser.getId());
            if (apiKey == null || apiKey.isEmpty()) {
                Notification.show("Brak klucza API", 3000, Notification.Position.MIDDLE);
                return;
            }

            if (!allDataCheckbox.getValue()) {
                if (startDatePicker.getValue() == null || endDatePicker.getValue() == null) {
                    Notification.show("Proszę wybrać obie daty", 3000, Notification.Position.MIDDLE);
                    return;
                }

                if (startDatePicker.getValue().isAfter(endDatePicker.getValue())) {
                    Notification.show("Data początkowa nie może być późniejsza niż końcowa", 3000,
                            Notification.Position.MIDDLE);
                    return;
                }
            }

            try {
                String redirectUrl;
                if (allDataCheckbox.getValue()) {
                    redirectUrl = dataFlowService.getDataRetrievalUrl(flow.getId(), apiKey, null, null);
                } else {
                    java.time.LocalDateTime startDateTime = startDatePicker.getValue().atStartOfDay();
                    java.time.LocalDateTime endDateTime = endDatePicker.getValue().atTime(23, 59, 59);
                    redirectUrl = dataFlowService.getDataRetrievalUrl(flow.getId(), apiKey, startDateTime, endDateTime);
                }

                getUI().ifPresent(ui -> {
                    ui.getPage().open(redirectUrl, "_blank");
                });

                dialog.close();
            } catch (IllegalStateException ex) {
                Notification.show(ex.getMessage(), 5000, Notification.Position.MIDDLE);
            } catch (Exception ex) {
                Notification.show("Błąd podczas generowania URL: " + ex.getMessage(), 5000,
                        Notification.Position.MIDDLE);
            }
        });

        Button cancelButton = new Button("Anuluj", ev -> dialog.close());

        dialogLayout.add(allDataCheckbox, startDatePicker, endDatePicker);
        dialog.add(dialogLayout);
        dialog.getFooter().add(cancelButton, redirectButton);
        dialog.open();
    }

    private void openErrorsDialog(DataFlow flow) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Błędy dla przepływu: " + flow.getName());
        dialog.setWidth("800px");

        VerticalLayout dialogLayout = new VerticalLayout();

        java.util.List<FlowError> errors = flowErrorService.getErrorsByFlowId(flow.getId());

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
