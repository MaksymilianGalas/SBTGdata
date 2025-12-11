package com.sbtgdata.views;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.NotFoundException;
import jakarta.servlet.http.HttpServletResponse;

public class NotFoundErrorView extends VerticalLayout implements HasErrorParameter<NotFoundException> {

    public NotFoundErrorView() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
    }

    @Override
    public int setErrorParameter(BeforeEnterEvent event, ErrorParameter<NotFoundException> parameter) {
        removeAll();
        add(new H1("Nie znaleziono strony"));
        add(new Paragraph("Adres, którego szukasz, nie istnieje lub nie masz dostępu."));
        return HttpServletResponse.SC_NOT_FOUND;
    }
}

