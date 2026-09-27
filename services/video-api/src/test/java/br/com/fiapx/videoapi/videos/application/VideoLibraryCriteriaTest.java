package br.com.fiapx.videoapi.videos.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class VideoLibraryCriteriaTest {

    @Test
    void trimsTheNameAndDefaultsOptionalEnums() {
        var criteria = new VideoLibraryCriteria("  Blackstock.mp4  ", null, null);

        assertThat(criteria.name()).isEqualTo("Blackstock.mp4");
        assertThat(criteria.match()).isEqualTo(VideoLibraryNameMatch.PREFIX);
        assertThat(criteria.status()).isEqualTo(VideoLibraryStatusFilter.ALL);
    }

    @Test
    void normalizesBlankNamesAsAbsent() {
        assertThat(new VideoLibraryCriteria("  ", VideoLibraryNameMatch.EXACT, null).name()).isNull();
        assertThat(VideoLibraryCriteria.all().name()).isNull();
    }

    @Test
    void rejectsNamesAboveThePersistenceLimit() {
        var name = "a".repeat(VideoLibraryCriteria.MAX_NAME_LENGTH + 1);

        assertThatThrownBy(() -> new VideoLibraryCriteria(name, null, null))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
