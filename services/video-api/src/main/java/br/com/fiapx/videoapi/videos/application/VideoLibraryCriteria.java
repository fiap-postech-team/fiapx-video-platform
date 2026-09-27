package br.com.fiapx.videoapi.videos.application;

public record VideoLibraryCriteria(
    String name,
    VideoLibraryNameMatch match,
    VideoLibraryStatusFilter status,
    VideoLibrarySort sort,
    VideoLibrarySortDirection direction
) {
    public static final int MAX_NAME_LENGTH = 512;

    public VideoLibraryCriteria {
        name = normalize(name);
        match = match == null ? VideoLibraryNameMatch.PREFIX : match;
        status = status == null ? VideoLibraryStatusFilter.ALL : status;
        sort = sort == null ? VideoLibrarySort.UPDATED_AT : sort;
        direction = direction == null ? VideoLibrarySortDirection.DESC : direction;
        if (name != null && name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Invalid video name");
        }
    }

    public VideoLibraryCriteria(String name, VideoLibraryNameMatch match, VideoLibraryStatusFilter status) {
        this(name, match, status, VideoLibrarySort.UPDATED_AT, VideoLibrarySortDirection.DESC);
    }

    public static VideoLibraryCriteria all() {
        return new VideoLibraryCriteria(null, VideoLibraryNameMatch.PREFIX, VideoLibraryStatusFilter.ALL,
            VideoLibrarySort.UPDATED_AT, VideoLibrarySortDirection.DESC);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        var stripped = value.strip();
        return stripped.isEmpty() ? null : stripped;
    }
}
