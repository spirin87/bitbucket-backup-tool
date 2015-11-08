package io.github.spirin87.bitbucket_backup;

import java.io.File;
import java.io.InputStreamReader;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

/**
 * @author spirin87@gmail.com
 * <p>
 * Nov 8, 2015, 10:38:25 AM
 */
public class Worker {

    private static final Logger log = Logger.getLogger(Worker.class);

    private CloseableHttpClient client;

    private HttpClientContext localContext;

    private HttpHost target;

    private File folder;

    private String user;

    private String password;

    public Worker(String user, String password, File folder) {
        this.user = user;
        this.password = password;
        this.folder = folder;
        target = new HttpHost("bitbucket.org", 443, "https");
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(new AuthScope(target.getHostName(), target.getPort()),
                new UsernamePasswordCredentials(user, password));
        client = HttpClients.custom().setDefaultCredentialsProvider(credsProvider).build();
        AuthCache authCache = new BasicAuthCache();
        BasicScheme basicAuth = new BasicScheme();
        authCache.put(target, basicAuth);
        localContext = HttpClientContext.create();
        localContext.setAuthCache(authCache);
    }

    public void cloneRepos() {
        log.info(">getRepos");
        try {
            HttpGet get = new HttpGet("https://bitbucket.org/api/1.0/user/repositories/dashboard/");
            CloseableHttpResponse response = client.execute(target, get, localContext);
            try {
                int status = response.getStatusLine().getStatusCode();
                if (status != 200) {
                    throw new RuntimeException("auth error");
                }
                JsonArray array = new JsonParser().parse(new JsonReader(new InputStreamReader(response.getEntity().getContent())))
                        .getAsJsonArray();
                log.info("found " + array.size() + " repositories owner");
                for (int i = 0; i < array.size(); i++) {
                    JsonArray repoOwner = array.get(i).getAsJsonArray();
                    if (repoOwner.size() < 2) {
                        throw new RuntimeException("can't parse result json");
                    }
                    JsonObject owner = repoOwner.get(0).getAsJsonObject();
                    String team = owner.get("username").getAsString();
                    log.info("getting " + team + " repositories");
                    File teamFolder = new File(folder, team);
                    teamFolder.mkdir();
                    checkFolder(teamFolder);
                    JsonArray repos = repoOwner.get(1).getAsJsonArray();
                    for (int j = 0; j < repos.size(); j++) {
                        JsonObject repo = repos.get(j).getAsJsonObject();
                        String repoPath = repo.get("absolute_url").getAsString();
                        String repoName = repo.get("name").getAsString();
                        log.info("cloning " + repoPath);
                        File repoFolder = new File(teamFolder, repoName);
                        repoFolder.mkdir();
                        try {
                            checkFolder(repoFolder);
                            Git.cloneRepository().setURI("https://" + user + "@bitbucket.org/" + repoPath + ".git")
                                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider(user, password))
                                    .setDirectory(repoFolder).call();
                        } catch (Exception e) {
                            log.error("error cloning " + repoPath + ": " + e.getMessage());
                        }
                    }
                }
            } finally {
                response.close();
            }
        } catch (Exception e) {
            throw new RuntimeException("error getting repositories:", e);
        }
        log.info("<getRepos");
    }

    private void checkFolder(File folder) {
        if (!folder.exists() || !folder.isDirectory()) {
            throw new RuntimeException("can't create directory: " + folder.getAbsolutePath());
        }
    }
}
