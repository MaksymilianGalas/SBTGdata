package com.sbtgdata.views;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

@Route(value = "test1", layout = MainLayout.class)
@PageTitle("Widok Testowy 1")
@PermitAll
public class TestView1 extends VerticalLayout {
    
    public TestView1() {
        setSizeFull();
        setPadding(true);
        
        H1 title = new H1("Widok Testowy 1");
        Paragraph description = new Paragraph(
            "To jest widok testowy numer 1. " +
            "Dostępny dla wszystkich zalogowanych użytkowników (księgowy i administrator)."
        );
        
        Paragraph content = new Paragraph(
            "Tutaj można dodać dowolną funkcjonalność dla użytkowników z rolą księgowy i administrator."
        );
        
        add(title, description, content);
    }
}

