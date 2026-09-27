package br.com.fiapx.videoapi.videos.application;

public record VideoLibraryCriteria(
    String name,
    VideoLibraryNameMatch match,
    VideoLibraryStatusFilter status
) {
    public static final int MAX_NAME_LENGTH = 512;

    public VideoLibraryCriteria {
        name = normalize(name);
        match = match == null ? VideoLibraryNameMatch.PREFIX : match;
        status = status == null ? VideoLibraryStatusFilter.ALL : status;
        if (name != null && name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Invalid video name");
        }
    }

    public static VideoLibraryCriteria all() {
        return new VideoLibraryCriteria(null, VideoLibraryNameMatch.PREFIX, VideoLibraryStatusFilter.ALL);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        var stripped = value.strip();
        return stripped.isEmpty() ? null : stripped;
    }
}
