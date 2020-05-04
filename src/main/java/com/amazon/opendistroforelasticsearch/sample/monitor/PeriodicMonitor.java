package com.amazon.opendistroforelasticsearch.sample.monitor;

import com.amazon.opendistroforelasticsearch.sample.client.ElasticClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *  Sample code to demonstrate secure calls with fgac from
 *      1. Plugin to Elastic
 *      2. Plugin to Plugin
 *  on a background thread.
 *
 *  Refer to README for the demo data and roles configuration.
 */
public class PeriodicMonitor implements Runnable{

    private final Logger log = LogManager.getLogger(PeriodicMonitor.class);
    private List<Monitor> monitorList = new ArrayList<>();

        //Represents configured search monitor by an user.
    private class Monitor {
        private String createdBy;
        private String indexpattern;
        private Monitor(String createdBy, String indexpattern) {
            this.indexpattern = indexpattern;
            this.createdBy = createdBy;
        }

        @Override
        public String toString() {
            return String.format("createdby: %s ; indexpattern: %s", createdBy, indexpattern);
        }
    }

    public PeriodicMonitor(){
        log.info("Starting periodic monitor.....");
    }

    private void readMonitorConfig(){
        monitorList.clear();
        //hard coded here for this sample.
        //should succeed: chip has operations_role which is mapped to iad_data index.
        monitorList.add(new Monitor("chip", "iad_data"));
        //should fail chip has operations_role only. pdt_data is mapped to gov_clearance_role and chip cant access.
        monitorList.add(new Monitor("chip", "pdt_data"));
        //should succeed: dale has operations_role and gov_clearance_role and can access both.
        monitorList.add(new Monitor("dale", "iad_data,pdt_data"));
    }

    private Request createRequest(Monitor monitor) {
        Request request = new Request("GET",  String.format("/%s/_search?q=ERROR", monitor.indexpattern));
        RequestOptions.Builder builder = RequestOptions.DEFAULT.toBuilder();
        builder.addHeader("opendistro_security_impersonate_as", monitor.createdBy);
        request.setOptions(builder);
        return request;
    }

    private String getResponse(RestClient restClient, Request request) {
        try {
            Response response = restClient.performRequest(request);
            return response.getStatusLine().getReasonPhrase();
        } catch (IOException ex){
            return ex.getMessage();
        }
    }

    private Request notifyRequest(String msg){
        Request notifyRequest = new Request("POST", "/notify");
        notifyRequest.setJsonEntity(String.format("{\"msg\":\"%s\"}", msg));
        RequestOptions.Builder builder = RequestOptions.DEFAULT.toBuilder();
        notifyRequest.setOptions(builder);
        return notifyRequest;
    }

    @Override
    public void run() {
        while(true) {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException ex) {
                log.error("Periodic thread interrupted!");
                return;
            }

            try {
                readMonitorConfig();

                for(Monitor monitor : monitorList) {

                    //Step #1: Create low level REST client with user: JobAdmin and its certs.
                    RestClient restClient = ElasticClient.createWithCertsAndClientCerts();

                    //Step #2: Create a request to Elasticsearch, with impersonating as user from monitor object.
                    Request request = createRequest(monitor);

                    //Step #3: send request and parse response.
                    String resp = getResponse(restClient, request);

                    //Step #4: Create and make request to other plugin impersonating as user from monitor object.
                    Request notifyRequest = notifyRequest(monitor.toString()+ " : "+resp);
                    restClient.performRequest(notifyRequest);
                }
            } catch (Exception ex) {
                log.error(ex);
            }
        }
    }
}