package jenkins.plugins.glip.webhook;

public interface RouterCommand<T> {
    public T execute(String... args);
}
