package br.com.fiapx.videoapi.architecture.fixture.domain;

import br.com.fiapx.videoapi.architecture.fixture.application.ApplicationDependingOnAdapter;

public class DomainDependingOnApplication {

    private final ApplicationDependingOnAdapter application = new ApplicationDependingOnAdapter();
}
