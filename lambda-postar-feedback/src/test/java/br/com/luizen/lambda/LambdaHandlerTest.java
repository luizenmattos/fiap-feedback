package br.com.luizen.lambda;

import org.junit.jupiter.api.Test;

import br.com.luizen.lambda.postarFeedback.FeedbackInput;
import io.quarkus.test.junit.QuarkusTest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

@QuarkusTest
class LambdaHandlerTest {
    private static final String apiKeyValida = "7c3f9371e1327c96ff069135205a0b669943c145bcc9833cc3caa9d5847c5c8d";

    @Test
    void testSimpleLambdaSuccess() throws Exception {
        FeedbackInput in = new FeedbackInput();
        in.descricao = "Descrição do feedback";
        in.nota = 5L;
        in.apiKey = apiKeyValida;
        given()
                .contentType("application/json")
                .accept("application/json")
                .body(in)
                .when()
                .post("/feedback")
                .then()
                .statusCode(200)
                .body(containsString("Feedback recebido"));
    }

}
