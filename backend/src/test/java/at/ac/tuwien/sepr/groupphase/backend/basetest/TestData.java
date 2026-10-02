package at.ac.tuwien.sepr.groupphase.backend.basetest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public interface TestData {

    Long ID = 1L;
    String TEST_NEWS_TITLE = "Title";
    String TEST_NEWS_SUMMARY = "Summary";
    String TEST_NEWS_TEXT = "TestNewsText";
    LocalDateTime TEST_NEWS_PUBLISHED_AT =
        LocalDateTime.of(2019, 11, 13, 12, 15, 0, 0);

    String BASE_URI = "/api/v1";
    String NEWS_BASE_URI = BASE_URI + "/news";

    String ADMIN_USER = "admin@email.com";
    List<String> ADMIN_ROLES = new ArrayList<>() {
        {
            add("ROLE_ADMIN");
            add("ROLE_USER");
        }
    };
    String DEFAULT_USER = "admin@email.com";
    List<String> USER_ROLES = new ArrayList<>() {
        {
            add("ROLE_USER");
        }
    };

    String REGISTER_BASE_URI = BASE_URI + "/users/register";
    String USER_REGISTER_EMAIL = "kevin.ni@example.com";
    String USER_REGISTER_FIRST_NAME = "Kevin";
    String USER_REGISTER_LAST_NAME = "Ni";
    String USER_REGISTER_PASSWORD = "Passwort1!%";


    String AUTH_BASE_URI = BASE_URI + "/users/authentication";
    String USER_AUTH_FIRST_NAME = "admin";
    String USER_AUTH_LAST_NAME = "admin";
    String USER_AUTH_EMAIL = "user@test.at";
    String USER_AUTH_PASSWORD = "Passwort1!";
    String USER_AUTH_WRONG_PASSWORD = "wrong-password";


    String ADMIN_CREATE_URI = BASE_URI + "/users";
    String ADMIN_CREATE_EMAIL = "newuser@example.com";
    String ADMIN_CREATE_FIRST_NAME = "Max";
    String ADMIN_CREATE_LAST_NAME = "Mustermann";
}
