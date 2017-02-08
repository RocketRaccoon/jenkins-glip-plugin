package jenkins.plugins.glip;

import hudson.Extension;
import hudson.model.*;
import hudson.model.listeners.RunListener;
import hudson.tasks.Publisher;

import java.util.Map;
import java.util.logging.Logger;

/**
     * Created by andrey.smirnov on 19.01.2017.
     */
@Extension
public class GlipListener extends RunListener<AbstractBuild> {

    private static final Logger logger = Logger.getLogger(GlipListener.class.getName());

    public GlipListener() {super(AbstractBuild.class);}

    @Override
    public void onCompleted(AbstractBuild r, TaskListener listener) {
        getNotifier(r.getProject(), listener).completed(r);
        super.onCompleted(r, listener);
    }

    @Override
    public void onStarted(AbstractBuild r, TaskListener listener) {
        // getNotifier(r.getProject()).started(r);
        // super.onStarted(r, listener);
    }

    @Override
    public void onDeleted(AbstractBuild r) {
        // getNotifier(r.getProject()).deleted(r);
        // super.onDeleted(r);
    }

    @Override
    public void onFinalized(AbstractBuild r) {
        // getNotifier(r.getProject()).finalized(r);
        // super.onFinalized(r);
    }

    FineGrainedNotifier getNotifier(AbstractProject project, TaskListener listener) {
        Map<Descriptor<Publisher>, Publisher> map = project.getPublishersList().toMap();
        for (Publisher publisher : map.values()) {
            if (publisher instanceof GlipNotifier) {
                return new ActiveNotifier((GlipNotifier) publisher, (BuildListener)listener);
            }
        }
        return new DisabledNotifier();
    }


}
