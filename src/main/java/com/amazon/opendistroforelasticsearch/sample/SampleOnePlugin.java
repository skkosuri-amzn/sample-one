package com.amazon.opendistroforelasticsearch.sample;

import com.amazon.opendistroforelasticsearch.sample.actions.SearchErrorsAction;
import com.amazon.opendistroforelasticsearch.sample.monitor.PeriodicMonitor;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.node.DiscoveryNodes;
import org.elasticsearch.common.settings.ClusterSettings;
import org.elasticsearch.common.settings.IndexScopedSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.SettingsFilter;
import org.elasticsearch.plugins.ActionPlugin;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestHandler;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

public class SampleOnePlugin extends Plugin implements ActionPlugin {

    public SampleOnePlugin() {
        //empty
    }

    @Override
    public List<RestHandler> getRestHandlers(Settings settings, RestController restController,
                                             ClusterSettings clusterSettings, IndexScopedSettings indexScopedSettings,
                                             SettingsFilter settingsFilter,
                                             IndexNameExpressionResolver indexNameExpressionResolver,
                                             Supplier<DiscoveryNodes> nodesInCluster) {

        new Thread(new PeriodicMonitor()).start();

        return Arrays.asList(
                new SearchErrorsAction(settings,restController)

        );
    }
}
