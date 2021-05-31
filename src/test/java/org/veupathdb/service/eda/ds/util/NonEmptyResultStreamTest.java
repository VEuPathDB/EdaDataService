package org.veupathdb.service.eda.ds.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import javax.ws.rs.BadRequestException;
import org.gusdb.fgputil.IoUtil;
import org.gusdb.fgputil.Tuples;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class NonEmptyResultStreamTest {

  private static class TestCase extends Tuples.ThreeTuple<String, String, Boolean> {
    public TestCase(String name, String data, boolean exceptionExpected) {
      super(name, data, exceptionExpected);
    }
    public String getName() { return getFirst(); }
    public String getData() { return getSecond(); }
    public boolean expectException() { return getThird(); }
  }

  private static TestCase[] TEST_CASES = new TestCase[] {
    new TestCase("NO_DATA_STREAM", "", true),
    new TestCase("ONE_LINE_WITHOUT_NL_STREAM", "abc\tdef", true),
    new TestCase("ONE_LINE_WITH_NL_STREAM", "abc\tdef\n", true),
    new TestCase("TWO_LINES_WITHOUT_NL_STREAM", "abc\tdef\n56\t87", false),
    new TestCase("TWO_LINES_WITH_NL_STREAM", "abc\tdef\n56\t87\n", false)
  };

  @Test
  public void test() throws IOException {
    for (TestCase testCase : TEST_CASES) {
      if (testCase.expectException()) {
        // make sure exception is thrown
        Assertions.assertThrows(BadRequestException.class, () -> {
          System.err.println("Trying exception case for " + testCase.getName());
          tryTransfer(testCase);
        });
      }
      else {
        System.err.println("Trying success case for " + testCase.getName());
        Assertions.assertEquals(
          testCase.getData().getBytes(StandardCharsets.UTF_8).length,
          tryTransfer(testCase)
        );
      }
    }
  }

  private int tryTransfer(TestCase testCase) throws IOException {
    try(ByteArrayInputStream byteStream = new ByteArrayInputStream(testCase.getData().getBytes(StandardCharsets.UTF_8));
        InputStream in = new NonEmptyResultStream(testCase.getName(), byteStream);
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      IoUtil.transferStream(out, in);
      return out.size();
    }
  }
}
