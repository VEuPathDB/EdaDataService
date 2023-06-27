package org.veupathdb.service.eda.ds.plugin.standalonemap.markers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.veupathdb.service.eda.generated.model.APIVariableDataShape;
import org.veupathdb.service.eda.generated.model.APIVariableType;
import org.veupathdb.service.eda.generated.model.OverlayConfig;

import static org.junit.jupiter.api.Assertions.*;

public class OverlaySpecificationTest {
  private static final ObjectMapper JSON = new ObjectMapper();

  @Test
  public void test() throws JsonProcessingException {
    final String overlayConfig = "{\n" +
        "\t\t\t\"overlayType\": \"continuous\",\n" +
        "\t\t\t\"overlayValues\": [\n" +
        "\t\t\t\t{\n" +
        "\t\t\t\t\t\"binEnd\": \"2020-08-21\",\n" +
        "\t\t\t\t\t\"binLabel\": \"[2020-05-19, 2020-08-21]\",\n" +
        "\t\t\t\t\t\"binStart\": \"2020-05-19\"\n" +
        "\t\t\t\t},\n" +
        "\t\t\t\t{\n" +
        "\t\t\t\t\t\"binEnd\": \"2020-11-23\",\n" +
        "\t\t\t\t\t\"binLabel\": \"(2020-08-21, 2020-11-23]\",\n" +
        "\t\t\t\t\t\"binStart\": \"2020-08-21\"\n" +
        "\t\t\t\t},\n" +
        "\t\t\t\t{\n" +
        "\t\t\t\t\t\"binEnd\": \"2021-02-25\",\n" +
        "\t\t\t\t\t\"binLabel\": \"(2020-11-23, 2021-02-25]\",\n" +
        "\t\t\t\t\t\"binStart\": \"2020-11-23\"\n" +
        "\t\t\t\t},\n" +
        "\t\t\t\t{\n" +
        "\t\t\t\t\t\"binEnd\": \"2021-05-30\",\n" +
        "\t\t\t\t\t\"binLabel\": \"(2021-02-25, 2021-05-30]\",\n" +
        "\t\t\t\t\t\"binStart\": \"2021-02-25\"\n" +
        "\t\t\t\t},\n" +
        "\t\t\t\t{\n" +
        "\t\t\t\t\t\"binEnd\": \"2021-09-02\",\n" +
        "\t\t\t\t\t\"binLabel\": \"(2021-05-30, 2021-09-02]\",\n" +
        "\t\t\t\t\t\"binStart\": \"2021-05-30\"\n" +
        "\t\t\t\t},\n" +
        "\t\t\t\t{\n" +
        "\t\t\t\t\t\"binEnd\": \"2021-12-05\",\n" +
        "\t\t\t\t\t\"binLabel\": \"(2021-09-02, 2021-12-05]\",\n" +
        "\t\t\t\t\t\"binStart\": \"2021-09-02\"\n" +
        "\t\t\t\t},\n" +
        "\t\t\t\t{\n" +
        "\t\t\t\t\t\"binEnd\": \"2022-03-09\",\n" +
        "\t\t\t\t\t\"binLabel\": \"(2021-12-05, 2022-03-09]\",\n" +
        "\t\t\t\t\t\"binStart\": \"2021-12-05\"\n" +
        "\t\t\t\t},\n" +
        "\t\t\t\t{\n" +
        "\t\t\t\t\t\"binEnd\": \"2022-06-11\",\n" +
        "\t\t\t\t\t\"binLabel\": \"(2022-03-09, 2022-06-11]\",\n" +
        "\t\t\t\t\t\"binStart\": \"2022-03-09\"\n" +
        "\t\t\t\t},\n" +
        "\t\t\t\t{\n" +
        "\t\t\t\t\t\"binEnd\": \"2022-09-13\",\n" +
        "\t\t\t\t\t\"binLabel\": \"(2022-06-11, 2022-09-13]\",\n" +
        "\t\t\t\t\t\"binStart\": \"2022-06-11\"\n" +
        "\t\t\t\t},\n" +
        "\t\t\t\t{\n" +
        "\t\t\t\t\t\"binEnd\": \"2022-12-17\",\n" +
        "\t\t\t\t\t\"binLabel\": \"(2022-09-13, 2022-12-17]\",\n" +
        "\t\t\t\t\t\"binStart\": \"2022-09-13\"\n" +
        "\t\t\t\t}\n" +
        "\t\t\t],\n" +
        "\t\t\t\"overlayVariable\": {\n" +
        "\t\t\t\t\"entityId\": \"OBI_0000659\",\n" +
        "\t\t\t\t\"variableId\": \"OBI_REPLACEME1\"\n" +
        "\t\t\t}\n" +
        "\t\t}";
    final OverlayConfig overlay = JSON.readValue(overlayConfig, OverlayConfig.class);
    OverlaySpecification overlaySpecification = new OverlaySpecification(overlay,
        x -> APIVariableType.DATE.getValue(),
        x -> APIVariableDataShape.CONTINUOUS.getValue());
    System.out.println(overlaySpecification.getRBinListAsString());
  }

}