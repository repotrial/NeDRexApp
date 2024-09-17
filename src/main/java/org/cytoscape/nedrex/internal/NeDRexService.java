package org.cytoscape.nedrex.internal;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.cytoscape.nedrex.internal.io.HttpGetWithEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.osgi.service.component.annotations.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;

/**
 * NeDRex App
 *
 * @author Andreas Maier
 */

@Component
public class NeDRexService {

    public static final String TUTORIAL_LINK = "https://nedrex.net/tutorial/";
    public static final String API_LINK = "https://api.nedrex.net/licensed/";

    public CloseableHttpClient API_client;
    public static final String NEDREX_LINK = "https://nedrex.net";
    public static final String CITATION_LINK = "https://www.nature.com/articles/s41467-021-27138-2";

    private String apiKey = null;

    public NeDRexService() {
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(100);
        cm.setDefaultMaxPerRoute(20);
        this.API_client = HttpClients.custom()
                .setConnectionManager(cm)
                .build();
        this.apiKey = getAPIKey();  // Moved this line below the API_client initialization
    }


    public void setCredentials(HttpURLConnection connection) {
        connection.setRequestProperty("x-api-key", this.apiKey);
    }

    public HttpResponse send(HttpGet request) throws IOException {
        request.setHeader("x-api-key", this.apiKey);
        System.out.println(request);
        return this.API_client.execute(request);
    }

    public HttpResponse send(HttpGetWithEntity request) throws IOException {
        request.setHeader("x-api-key", this.apiKey);
        System.out.println(request);
        return this.API_client.execute(request);
    }

    public HttpResponse send(HttpPost request) throws IOException {
        request.setHeader("x-api-key", this.apiKey);
        System.out.println(request);
        return this.API_client.execute(request);
    }


    private String getAPIKey() {
        String get_credentials_route = "/admin/api_key/generate";
        HttpPost post = new HttpPost(NeDRexService.API_LINK + get_credentials_route);
        post.setEntity(new StringEntity("{\"accept_eula\": true}", ContentType.APPLICATION_JSON));
        String api_key = new String();

        try {
            HttpResponse response = this.API_client.execute(post);
            BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            api_key = rd.readLine().replace('"', ' ').trim();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return api_key;
    }

}

