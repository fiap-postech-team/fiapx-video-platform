package br.com.fiapx.videoapi.architecture.fixture.configuration;

import org.springframework.beans.factory.annotation.Autowired;

public class FieldInjectedComponent {

    @Autowired
    private MutableDependency dependency;
}
