import io.restassured.RestAssured;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import io.restassured.response.Response;

import java.util.LinkedHashMap;
import java.util.List;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

public class PatientIntegrationTest {
    private static String accessToken;

    @BeforeAll
    static void setup() {
        // Set up the base URI for the patient service
        RestAssured.baseURI = "http://localhost:4004";
        // Login to the auth service to get the access token
        getAccessToken();
        // Set up the base path for the patient service
        RestAssured.basePath = "/api/patients";
    }

    @Test
    public void shouldReturnPatientsWithValidToken () {

        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get("")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("patients", notNullValue())
                .extract().response();


        List<LinkedHashMap<String, String>> patients = response.jsonPath().getList("");
        for (LinkedHashMap<String, String> patient : patients) {
            // Print the patient details
            System.out.printf(
                    """
                            {
                               ID: %s
                               Name: %s
                               Age: %s
                            }
                    """, patient.get("id"), patient.get("name"), patient.get("age"));
        }
    }

    private static void getAccessToken() {
        String loginPayLoad = """
                {
                    "email": "testuser@test.com",
                    "password": "password123"
                }
                """;

        accessToken = given()
                .contentType("application/json")
                .body(loginPayLoad)
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .path("accessToken");

    }
}
