package br.com.fiapx.videoapi.architecture.fixture.application;

import br.com.fiapx.videoapi.architecture.fixture.adapter.ExternalAdapter;

public class ApplicationDependingOnAdapter {

    private final ExternalAdapter adapter = new ExternalAdapter();
}
