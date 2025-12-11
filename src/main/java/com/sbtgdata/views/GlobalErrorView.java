package com.sbtgdata.views;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import jakarta.servlet.http.HttpServletResponse;

public class GlobalErrorView extends VerticalLayout implements HasErrorParameter<Exception> {

    public GlobalErrorView() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
    }

    @Override
    public int setErrorParameter(BeforeEnterEvent event, ErrorParameter<Exception> parameter) {
        removeAll();
        add(new H1("Wystąpił błąd aplikacji"));
        add(new Paragraph("Spróbuj ponownie lub skontaktuj się z administratorem."));
        return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
    }
}

