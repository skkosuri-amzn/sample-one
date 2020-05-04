package com.amazon.opendistroforelasticsearch.sample.actions;

import com.amazon.opendistroforelasticsearch.sample.client.ElasticClient;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestStatus;

import java.io.IOException;
import java.util.Map;

import static org.elasticsearch.rest.RestRequest.Method.POST;


/***
 *  Sample code to demonstrate secure calls with fgac from
 *      1. Plugin to Elastic
 *      2. Plugin to Plugin
 *  for a user action.
 *  Refer to README for the demo data and roles configuration.
 */
public class SearchErrorsAction extends BaseRestHandler {

    private final Logger log = LogManager.getLogger(SearchErrorsAction.class);

    @Override
    public String getName(){
        return "SampleOnePlugin";
    }

    @Inject
    public SearchErrorsAction(Settings settings, RestController controller) {
        super(settings);
        controller.registerHandler(POST, "/errors", this);
    }

    private String getSearchIndex(String json) throws IOException {
        Map<String, Object> mapValue = XContentHelper.convertToMap(JsonXContent.jsonXContent, json, false);
        String region = (String)mapValue.get("region");
        String index = null;
        if("iad".equals(region)) {
            index = "iad_data"; //operations_role has access to this index.
        } else if("pdt".equals(region)) {
            index = "pdt_data"; //gove_clearance_role has access to this index.
        } else {
            throw new IOException("Search not supported in: "+region);
        }
        return index;
    }

    @Override
    protected final BaseRestHandler.RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client)  {
        try {
            /* debug
            log.info("SampleOnePlugin request headers ....");
            restRequest.getHeaders().forEach((k,v)->{
                log.info(k + " : " + v);
            });
            log.info("SampleOnePlugin request content: "+restRequest.content().utf8ToString());
            */

            //Step #1: Parse request
            String index = getSearchIndex(restRequest.content().utf8ToString());
            log.info("Searching in index: "+index);

            //Step #2: Create low level REST client
            RestClient restClient = ElasticClient.createWithSelfSigned();


            //Step #3: Create request to search for ERRORS from Elasticsearch. (Plugin to Elasticsearch secure call)
            Request request = new Request("GET",  String.format("/%s/_search?q=ERROR", index));
            RequestOptions.Builder builder = RequestOptions.DEFAULT.toBuilder();
            //todo: add logic to work with other Authorization systems like Kerboros, JWT, ...
            if(restRequest.getHeaders().get("Authorization") !=null )
                builder.addHeader("Authorization", restRequest.getHeaders().get("Authorization").get(0));
            request.setOptions(builder);

            //Step #4: send request and parse response.
            Response response = restClient.performRequest(request);
            int statusCode = response.getStatusLine().getStatusCode();
            log.info("SampleOnePlugin statusCode: "+statusCode);
            String responseBody = EntityUtils.toString(response.getEntity());


            //Step #5: call sampletwo plugin to notify. (Plugin to Plugin secure call)
            Request notifyRequest = new Request("POST", "/notify");
            notifyRequest.setJsonEntity(String.format("{\"msg\":\"%s\"}", responseBody));
            if(restRequest.getHeaders().get("Authorization") !=null )
                builder.addHeader("Authorization", restRequest.getHeaders().get("Authorization").get(0));

            return channel -> channel.sendResponse(new BytesRestResponse(RestStatus.OK,
                    "Done - Search and Notify."));

        } catch (final Exception ex){
            log.error("SampleOnePlugin: error", ex);
            return channel -> channel.sendResponse(new BytesRestResponse(RestStatus.BAD_REQUEST,
                    ex.getMessage() == null ? "Unknown" : ex.getMessage()));
        }
    }
}