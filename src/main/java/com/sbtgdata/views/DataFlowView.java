package com.sbtgdata.views;

import com.sbtgdata.config.SecurityService;
import com.sbtgdata.data.DataFlow;
import com.sbtgdata.data.DataFlowService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
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
    private final Grid<DataFlow> grid = new Grid<>(DataFlow.class);

    @Autowired
    public DataFlowView(DataFlowService dataFlowService, SecurityService securityService) {
        this.dataFlowService = dataFlowService;
        this.securityService = securityService;

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
            Button editButton = new Button("Edytuj", e -> openFlowEditor(flow));
            Button deleteButton = new Button("Usuń", e -> {
                dataFlowService.delete(flow);
                updateList();
                Notification.show("Przepływ usunięty");
            });
            return new HorizontalLayout(editButton, deleteButton);
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
}
