package com.cevher.keycloak;
import java.util.stream.Collectors;

import org.keycloak.models.UserModel;

public class UserUtils {
    public static void sendUserData(UserModel user) {
      String roles = user.getRealmRoleMappingsStream()
                    .map(role -> role.getName())
                    .collect(Collectors.joining(","));
      String data =
      "{" +
              "\"id\": \"" + user.getId() + "\"," +
              "\"email\": \"" + user.getEmail() + "\"," +
              "\"user_name\":\"" + user.getUsername() + "\"," +
              "\"first_name\":\"" + user.getFirstName() + "\"," +
              "\"last_name\":\"" + user.getLastName() + "\"," +
              "\"roles\":\"" + roles + "\"" +
              "}";
        try {
            Client client = new Client();
            client.postService(data,user.getId());
        } catch (Exception err) {
            throw new RuntimeException("Failed to send user data to the API: " + err.getMessage());
        }
    }
}
