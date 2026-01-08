package com.sbtgdata.views;

import com.sbtgdata.config.SecurityService;
import com.sbtgdata.data.DataFlow;
import com.sbtgdata.data.DataFlowService;
import com.sbtgdata.data.FlowError;
import com.sbtgdata.data.FlowErrorService;
import com.sbtgdata.data.User;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
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

@Route(value = "dataflows", layout = MainLayout.class)
@PageTitle("Moje przepływy danych")
@PermitAll
public class DataFlowView extends VerticalLayout {

    private final DataFlowService dataFlowService;
    private final SecurityService securityService;
    private final FlowErrorService flowErrorService;
    private final Grid<DataFlow> grid = new Grid<>(DataFlow.class);

    @Value("${entry.data.flow.url}")
    private String entryDataFlowUrl;

    @Value("${data.retrieval.url}")
    private String dataRetrievalUrl;

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

            String flowIdLast4 = flow.getId().length() >= 4
                    ? flow.getId().substring(flow.getId().length() - 4)
                    : flow.getId();

            TextField urlField = new TextField();
            urlField.setValue("..." + flowIdLast4);
            urlField.setReadOnly(true);
            urlField.setWidth("120px");
            urlField.getStyle().set("font-size", "0.875rem");

            Button expandButton = new Button(new Icon(VaadinIcon.EXPAND));
            expandButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
            expandButton.getElement().setAttribute("title", "Pokaż pełny URL");
            expandButton.addClickListener(e -> {
                if (urlField.getValue().startsWith("...")) {
                    urlField.setValue(url);
                    urlField.setWidth("100%");
                    expandButton.setIcon(new Icon(VaadinIcon.COMPRESS));
                } else {
                    urlField.setValue("..." + flowIdLast4);
                    urlField.setWidth("120px");
                    expandButton.setIcon(new Icon(VaadinIcon.EXPAND));
                }
            });

            Button copyButton = new Button(new Icon(VaadinIcon.COPY));
            copyButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
            copyButton.getElement().setAttribute("title", "Kopiuj pełny URL");
            copyButton.addClickListener(e -> {
                UI.getCurrent().getPage().executeJs(
                        "navigator.clipboard.writeText($0).then(() => { window.dispatchEvent(new CustomEvent('clipboard-success')); }, () => { window.dispatchEvent(new CustomEvent('clipboard-error')); });",
                        url);
                Notification.show("URL skopiowany", 2000, Notification.Position.MIDDLE);
            });

            HorizontalLayout layout = new HorizontalLayout(urlField, expandButton, copyButton);
            layout.setAlignItems(Alignment.CENTER);
            layout.setSpacing(false);
            layout.setPadding(false);
            return layout;
        }).setHeader("URL do wysyłania danych").setWidth("350px");

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
            if (flow.getId() == null) {
                return new Paragraph("-");
            }
            Button downloadButton = new Button(new Icon(VaadinIcon.DOWNLOAD));
            downloadButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
            downloadButton.getElement().setAttribute("title", "Pobierz dane CSV");
            downloadButton.addClickListener(e -> openDownloadDialog(flow));
            return downloadButton;
        }).setHeader("Pobierz dane").setWidth("120px");

        grid.addComponentColumn(flow -> {
            boolean isRunning = "RUNNING".equals(flow.getStatus());

            Button editButton = new Button("Edytuj", e -> openFlowEditor(flow));
            editButton.setEnabled(!isRunning);
            if (isRunning) {
                editButton.getElement().setAttribute("title", "Zatrzymaj przepływ aby edytować");
            }

            Button deleteButton = new Button("Usuń", e -> {
                try {
                    dataFlowService.delete(flow);
                    updateList();
                    Notification.show("Przepływ usunięty");
                } catch (IllegalArgumentException ex) {
                    Notification.show(ex.getMessage(), 5000, Notification.Position.MIDDLE);
                }
            });
            deleteButton.setEnabled(!isRunning);
            if (isRunning) {
                deleteButton.getElement().setAttribute("title", "Zatrzymaj przepływ aby usunąć");
            }

            Button toggleButton;
            if (isRunning) {
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
        dialog.setHeaderTitle(flow.getId() == null ? "Dodaj nowy przepływ" : "Edytuj przepływ");
        dialog.setWidth("600px");

        VerticalLayout dialogLayout = new VerticalLayout();

        TextField nameField = new TextField("Nazwa przepływu");
        nameField.setValue(flow.getName() != null ? flow.getName() : "");
        nameField.setWidthFull();

        TextArea functionArea = new TextArea("Funkcja Python");
        Checkbox hasLibrariesCheckbox = new Checkbox("Dodatkowe biblioteki");
        TextArea librariesArea = new TextArea("Biblioteki (jedna na linię)");

        functionArea.setValue(flow.getFunction() != null ? flow.getFunction() : "");
        functionArea.setWidthFull();
        functionArea.setHeight("200px");

        hasLibrariesCheckbox.setValue(flow.getPackages() != null && !flow.getPackages().isEmpty());
        librariesArea.setVisible(hasLibrariesCheckbox.getValue());
        librariesArea.setWidthFull();
        librariesArea.setHeight("100px");

        if (flow.getPackages() != null && !flow.getPackages().isEmpty()) {
            librariesArea.setValue(String.join("\n", flow.getPackages()));
        }

        hasLibrariesCheckbox.addValueChangeListener(e -> {
            librariesArea.setVisible(e.getValue());
        });

        dialogLayout.add(nameField, functionArea, hasLibrariesCheckbox, librariesArea);

        Button saveButton = new Button("Zapisz", e -> {
            flow.setName(nameField.getValue());
            flow.setFunction(functionArea.getValue());

            if (hasLibrariesCheckbox.getValue() && !librariesArea.getValue().trim().isEmpty()) {
                flow.setPackages(java.util.Arrays.asList(librariesArea.getValue().split("\n")));
            } else {
                flow.setPackages(new java.util.ArrayList<>());
            }

            String currentUserEmail = securityService.getAuthenticatedUser().orElse(null);
            if (currentUserEmail != null) {
                flow.setOwnerEmail(currentUserEmail);
                try {
                    dataFlowService.save(flow);
                    updateList();
                    dialog.close();
                    Notification.show("Przepływ zapisany");
                } catch (IllegalArgumentException ex) {
                    Notification.show(ex.getMessage(), 5000, Notification.Position.MIDDLE);
                }
            }
        });

        Button cancelButton = new Button("Anuluj", e -> dialog.close());

        dialog.add(dialogLayout);
        dialog.getFooter().add(cancelButton, saveButton);
        dialog.open();
    }

    private void openErrorsDialog(DataFlow flow) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Błędy przepływu: " + flow.getName());
        dialog.setWidth("800px");

        VerticalLayout dialogLayout = new VerticalLayout();
        Grid<FlowError> errorGrid = new Grid<>(FlowError.class);
        errorGrid.setColumns();
        errorGrid.addColumn(error -> {
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter
                    .ofPattern("yyyy-MM-dd HH:mm:ss");
            return error.getDate() != null ? error.getDate().format(formatter) : "";
        }).setHeader("Data");
        errorGrid.addColumn(FlowError::getMessage).setHeader("Komunikat błędu");

        errorGrid.setItems(flowErrorService.getUniqueErrorsByFlowId(flow.getId()));

        dialogLayout.add(errorGrid);

        Button closeButton = new Button("Zamknij", e -> dialog.close());

        dialog.add(dialogLayout);
        dialog.getFooter().add(closeButton);
        dialog.open();
    }

    private void openDownloadDialog(DataFlow flow) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Pobierz dane CSV: " + flow.getName());
        dialog.setWidth("500px");

        VerticalLayout dialogLayout = new VerticalLayout();

        Checkbox downloadAllCheckbox = new Checkbox("Pobierz wszystkie dane");
        downloadAllCheckbox.setValue(false);

        TextField startDateField = new TextField("Data początkowa (YYYY-MM-DD HH:MM:SS)");
        startDateField.setPlaceholder("2026-01-08 14:30:00");
        startDateField.setWidthFull();

        TextField endDateField = new TextField("Data końcowa (YYYY-MM-DD HH:MM:SS)");
        endDateField.setPlaceholder("2026-01-08 15:30:00");
        endDateField.setWidthFull();

        downloadAllCheckbox.addValueChangeListener(e -> {
            boolean downloadAll = e.getValue();
            startDateField.setEnabled(!downloadAll);
            endDateField.setEnabled(!downloadAll);
        });

        dialogLayout.add(downloadAllCheckbox, startDateField, endDateField);

        Button downloadButton = new Button("Pobierz", e -> {
            User currentUser = securityService.getCurrentUser();
            String apiKey = currentUser != null ? currentUser.getApiKey() : "";

            String url = dataRetrievalUrl + "/flows/" + flow.getId() + "/data?API_KEY=" + apiKey;

            if (!downloadAllCheckbox.getValue()) {
                if (startDateField.getValue() == null || startDateField.getValue().trim().isEmpty() ||
                        endDateField.getValue() == null || endDateField.getValue().trim().isEmpty()) {
                    Notification.show("Wybierz daty lub zaznacz 'Pobierz wszystkie dane'",
                            3000, Notification.Position.MIDDLE);
                    return;
                }

                try {
                    java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter
                            .ofPattern("yyyy-MM-dd HH:mm:ss");
                    java.time.LocalDateTime startDateTime = java.time.LocalDateTime
                            .parse(startDateField.getValue().trim(), formatter);
                    java.time.LocalDateTime endDateTime = java.time.LocalDateTime.parse(endDateField.getValue().trim(),
                            formatter);

                    String startDate = startDateTime.format(formatter);
                    String endDate = endDateTime.format(formatter);

                    String encodedStartDate = startDate.replace(" ", "%20");
                    String encodedEndDate = endDate.replace(" ", "%20");

                    url += "&start_date=" + encodedStartDate + "&end_date=" + encodedEndDate;
                } catch (java.time.format.DateTimeParseException ex) {
                    Notification.show("Nieprawidłowy format daty. Użyj: YYYY-MM-DD HH:MM:SS (np. 2026-01-08 14:30:00)",
                            5000, Notification.Position.MIDDLE);
                    return;
                }
            }

            final String finalUrl = url;
            getUI().ifPresent(ui -> {
                ui.getPage().executeJs("window.open($0, '_blank')", finalUrl);
            });

            dialog.close();
        });

        Button cancelButton = new Button("Anuluj", e -> dialog.close());

        dialog.add(dialogLayout);
        dialog.getFooter().add(cancelButton, downloadButton);
        dialog.open();
    }
}
