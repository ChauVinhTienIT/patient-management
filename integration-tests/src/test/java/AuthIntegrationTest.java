import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

public class AuthIntegrationTest {
    @BeforeAll
    static void setup() {
        RestAssured.baseURI = "http://localhost:4004";
        RestAssured.basePath = "/auth";
    }

    @Test
    public void shouldReturnOkWithValidEmailAndPassword() {
        // 1. Arrange
        String loginPayLoad = """
                {
                    "email": "testuser@test.com",
                    "password": "password123"
                }
                """;
        // 2. Act
        // 3. Assert
        Response response = given()
                .contentType("application/json")
                .body(loginPayLoad)
                .when()
                .post("/login");

        Assertions.assertEquals(HttpStatus.SC_OK, response.getStatusCode());
        Assertions.assertNotNull(response.jsonPath().getString("accessToken"));

        System.out.println("Generated access token: " + response.jsonPath().getString("accessToken"));
    }

    @Test
    public void shouldReturnUnAuthorizedWithInvalidEmailAndPassword() {
        String loginPayLoad = """
                {
                    "email": "invalidtestuser@test.com",
                    "password": "invalidpassword123"
                }
                """;
        Response response = given()
                .contentType("application/json")
                .body(loginPayLoad)
                .when()
                .post("/login");

        Assertions.assertEquals(HttpStatus.SC_UNAUTHORIZED, response.getStatusCode());

        System.out.println("Login failed, no access token generated.");
    }
}
