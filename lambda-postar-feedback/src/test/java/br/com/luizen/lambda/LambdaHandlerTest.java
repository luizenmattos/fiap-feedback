package br.com.luizen.lambda;

import org.junit.jupiter.api.Test;

import br.com.luizen.lambda.postarFeedback.FeedbackInput;
import io.quarkus.test.junit.QuarkusTest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

@QuarkusTest
class LambdaHandlerTest {
    @Test
    void testSimpleLambdaSuccess() throws Exception {
        FeedbackInput in = new FeedbackInput();
        in.descricao = "Descrição do feedback";
        in.nota = 5L;
        given()
                .contentType("application/json")
                .accept("application/json")
                .body(in)
                .when()
                .post()
                .then()
                .statusCode(200)
                .body(containsString("Feedback recebido"));
    }

}
