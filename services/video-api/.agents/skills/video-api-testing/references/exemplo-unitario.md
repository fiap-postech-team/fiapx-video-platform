# Exemplo unitário ilustrativo

```java
@Test
void rejectsJobOwnedByAnotherUser() {
    var repository = mock(JobRepository.class);
    var query = new FindJob(repository);
    var jobId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    var ownerId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    when(repository.findByIdAndUserId(jobId, ownerId))
        .thenReturn(Optional.empty());

    var failure = catchThrowable(() -> query.execute(jobId, ownerId));

    assertThat(failure).isInstanceOf(JobNotFoundException.class);
}
```
