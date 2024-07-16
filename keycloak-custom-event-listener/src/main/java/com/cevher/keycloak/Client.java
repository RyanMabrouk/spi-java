package com.cevher.keycloak;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import org.json.JSONObject;

public class Client {
  private String REALM;
  private String KEYCLOAK_PORT;
  private String API_LOCALHOST;
  private String API_PORT;

  public Client() {
    this.REALM = System.getenv("KEYCLOAK_REALM_NAME");
    if (REALM == null) {
      System.err.println("KEYCLOAK_REALM_NAME is not set, using default value: " + REALM);
    }
    this.KEYCLOAK_PORT = System.getenv("KEYCLOAK_PORT");
    if (this.KEYCLOAK_PORT == null) {
      System.err.println("KEYCLOAK_PORT is not set, using default value: " + this.KEYCLOAK_PORT);
    }
    this.API_LOCALHOST = System.getenv("API_LOCALHOST");
    if (this.API_LOCALHOST == null) {
      System.err.println("API_LOCALHOST is not set, using default value: " + this.API_LOCALHOST);
    }
    this.API_PORT = System.getenv("API_PORT");
    if (this.API_PORT == null) {
      System.err.println("API_PORT is not set, using default value: " + this.API_PORT);
    }
  }

    public void postService(String data,String userId) throws URISyntaxException {
        try {

            String BACKEND_URL = System.getenv("BACKEND_URL");
            String nodeEnv = System.getenv("NODE_ENV");

            if (nodeEnv == null) {
                System.err.println("NODE_ENV is not set");
                return;
            }

            switch (nodeEnv) {
                case "production":
                    if (BACKEND_URL == null) {
                        System.err.println("BACKEND_URL is not set");
                        return;
                    }
                    break;
                case "development":
                    String API_LOCALHOST = System.getenv("API_LOCALHOST");
                    String API_PORT = System.getenv("API_PORT");
                    if (API_LOCALHOST == null || API_PORT == null) {
                        System.err.println("API_LOCALHOST or API_PORT is not set");
                        return;
                    }
                    BACKEND_URL = "http://" + API_LOCALHOST + ":" + API_PORT;
                    break;
                default:
                    System.err.println("Unknown NODE_ENV value");
                    return;
            }

            URI uri = URI.create(BACKEND_URL+"/api/users/sync");
            String TOKEN = getAdminAccessToken();
            System.out.println("URL: "+uri.toURL());
            HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + TOKEN);

            JSONObject jsonData = new JSONObject(data);

            OutputStream os = conn.getOutputStream();
            os.write(jsonData.toString().getBytes());
            os.flush();

            if (conn.getResponseCode() != HttpURLConnection.HTTP_CREATED) {
                throw new RuntimeException("Failed to create user with id "+userId+": HTTP error code : "
                        + conn.getResponseCode());
            }
            // Only in dev mode
           /*  BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
            String output;
            System.out.println("Output from Server .... \n");
            while ((output = br.readLine()) != null) {
                System.out.println(output);
            }*/
            conn.disconnect();
            System.out.println("user with id: "+userId+" has been created successfully.");
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to create user with id: "+userId);
            throw new RuntimeException("Failed to create user with id: "+userId);
        }

    }

    private String getAdminAccessToken() {
        try {

            String password = System.getenv("KEYCLOAK_ADMIN_PASSWORD");
            String username = System.getenv("KEYCLOAK_ADMIN");
            if (password == null || username == null) {
                System.err.println("KEYCLOAK_ADMIN_PASSWORD or KEYCLOAK_ADMIN is not set");
                return null;
            }

            String clientId = System.getenv("KEYCLOAK_CLIENT_ID");
            if (clientId == null) {
                System.err.println("KEYCLOAK_CLIENT_ID is not set");
                return null;
            }

            String clientSecret = System.getenv("KEYCLOAK_CLIENT_SECRET");
            if (clientSecret == null) {
                System.err.println("KEYCLOAK_CLIENT_SECRET is not set");
                return null;
            }
            String API_LOCALHOST = System.getenv("API_LOCALHOST");
            if (API_LOCALHOST == null) {
                System.err.println("API_LOCALHOST is not set");
                return null;
            }

            String nodeEnv = System.getenv("NODE_ENV");
            if (nodeEnv == null) {
                System.err.println("NODE_ENV is not set");
                return null;
            }
            String KEYCLOAK_AUTH_SERVER_URL = System.getenv("KEYCLOAK_AUTH_SERVER_URL");

            URI uri;
            switch (nodeEnv.toLowerCase()) {
                case "production":
                    if (KEYCLOAK_AUTH_SERVER_URL == null) {
                        System.err.println("KEYCLOAK_AUTH_SERVER_URL is not set");
                        return null;
                    }
                    uri = URI.create(String.format("%s/realms/%s/protocol/openid-connect/token", KEYCLOAK_AUTH_SERVER_URL, REALM));
                    break;
                case "development":
                    uri = URI.create(String.format("http://%s:%s/realms/%s/protocol/openid-connect/token", API_LOCALHOST, KEYCLOAK_PORT, REALM));
                    break;
                default:
                    System.err.println("Unknown NODE_ENV value");
                    return null;
            }
            System.out.println("Token URL: " + uri.toURL());

            HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setDoOutput(true);

            String urlParameters = "client_id=" + clientId + "&username=" + username + "&password=" + password + "&client_secret=" + clientSecret + "&grant_type=password&scope=openid";
            System.out.println("urlParameters: " + urlParameters);
            byte[] postData = urlParameters.getBytes(StandardCharsets.UTF_8);

            try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
                wr.write(postData);
            }

            StringBuilder content;
            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line;
                content = new StringBuilder();
                while ((line = in.readLine()) != null) {
                    content.append(line);
                    content.append(System.lineSeparator());
                }
            }

            conn.disconnect();

            JSONObject jsonResponse = new JSONObject(content.toString());
            String accessToken = jsonResponse.getString("access_token");
            System.out.println("Access token successfully retrieved");
            return accessToken;
        } catch (Exception e) {
            System.err.println("Failed to get admin access token");
            e.printStackTrace();
            throw new RuntimeException("Failed to get admin access token", e);
        }
    }
}


