import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import io.restassured.RestAssured
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import org.apache.logging.log4j.kotlin.logger
import org.junit.jupiter.api.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.net.URL
import java.nio.file.Path
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApiTest {
    private val AuthToken: String = System.getProperty("AUTH_TOKEN")
    private val AuthTokenKey: String = "Auth-Key"
    private val YamlMapper = ObjectMapper(YAMLFactory()).registerModule(KotlinModule.Builder().build())

    // Files in the testdata resource directory must follow structure testdata/{dataset-type}/{upload-file}
    private val TestFilesDir = "testdata"
    private val TestSuite = testSuite()

    @BeforeAll
    internal fun setup() {
        RestAssured.baseURI = System.getProperty("BASE_URL")
        RestAssured.port = System.getProperty("SERVICE_PORT").toInt()
        RestAssured.useRelaxedHTTPSValidation()
    }

    @ParameterizedTest
    @MethodSource("bubbleTestCaseProvider")
    fun parameterizedBubbleTest(input: BubbleMarkerTestCase) {
        val overlayValues = given() // Setup request
            .contentType(ContentType.JSON)
            .header(AuthTokenKey, AuthToken)
            .body(input.body)
            // Execute request
            .`when`()
            .post("apps/standalone-map/visualizations/map-markers/bubbles")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .extract()
            .path<List<Double>>("mapElements.overlayValue")
        logger().info("Received overlay values: $overlayValues")
        Assertions.assertEquals(input.expectedMarkerCount, overlayValues.size)
    }

    @ParameterizedTest
    @MethodSource("donutTestCaseProvider")
    fun parameterizedDonutTest(input: DonutMarkerTestCase) {
        logger().info("BODY: " + input.body)
        val overlayValues = given() // Setup request
            .contentType(ContentType.JSON)
            .header(AuthTokenKey, AuthToken)
            .body(input.body)
            // Execute request
            .`when`()
            .post("apps/standalone-map/visualizations/map-markers")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .extract()
            .path<List<Int>>("mapElements.entityCount")
        logger().info("Received count values: $overlayValues")
    }

    /**
     * Provide bubble test cases from YAML file.
     */
    private fun bubbleTestCaseProvider(): Stream<BubbleMarkerTestCase> {
        return TestSuite.bubbles.stream()
            .map {
                BubbleMarkerTestCase(
                    expectedMarkerCount = it.expectedMarkerCount,
                    body = it.body
                )
            }
    }

    /**
     * Provide donut test cases.
     */
    private fun donutTestCaseProvider(): Stream<DonutMarkerTestCase> {
        return TestSuite.donuts.stream()
            .map {
                DonutMarkerTestCase(
                    expectedMarkerCount = it.expectedMarkerCount,
                    body = it.body
                )
            }
    }

    private fun testSuite(): TestSuite {
        val loader = Thread.currentThread().contextClassLoader
        val url: URL = loader.getResource(TestFilesDir)!!
        val testDataDir: String = url.path
        val testConfig = Path.of(testDataDir, "test-cases.yaml")
        return YamlMapper.readValue(testConfig.toFile())
    }

}