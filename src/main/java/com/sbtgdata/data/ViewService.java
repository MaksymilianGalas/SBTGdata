package com.sbtgdata.data;

import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
public class ViewService {

    public Map<String, String> getAllViews() {
        Map<String, String> views = new HashMap<>();
        ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
        provider.addIncludeFilter(new AnnotationTypeFilter(Route.class));

        Set<BeanDefinition> components = provider.findCandidateComponents("com.sbtgdata.views");
        for (BeanDefinition component : components) {
            try {
                Class<?> cls = Class.forName(component.getBeanClassName());
                String title = cls.getSimpleName();
                com.vaadin.flow.router.PageTitle pageTitle = cls.getAnnotation(com.vaadin.flow.router.PageTitle.class);
                if (pageTitle != null) {
                    title = pageTitle.value();
                }
                views.put(cls.getName(), title);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return views;
    }
}
