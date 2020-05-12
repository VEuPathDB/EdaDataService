package org.veupathdb.service.demo.container.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.internal.verification.Times;
import picocli.CommandLine.Option;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TestException extends SecurityException {
  int code;

  public TestException(int code) {
    this.code = code;
  }
}

class CliTest {

  @Nested
  class ParseCLI {

    @AfterEach
    void tearDown() throws Exception {
      var f = Statics.class.getDeclaredField("instance");
      f.setAccessible(true);
      f.set(null, null);
    }

    private class Foo {
      @Option(names = "-a")
      String a;

      @Option(names = "-b")
      String b;
    }

    @Test
    void success() {
      var test = new Foo();
      Cli.parseCLI(new String[] {"-a", "pple", "-b", "anana"}, test);

      assertEquals("pple", test.a);
      assertEquals("anana", test.b);
    }

    @Test
    void unknownParam() {
      var test = new Foo();

      assertThrows(RuntimeException.class,
        () -> Cli.parseCLI(new String[] {"-c", "arp"}, test));
    }

    @Test
    void helpParam() throws Exception {
      var mockStatics = mock(Statics.class);
      var mockRuntime = mock(Runtime.class);

      when(mockStatics.runtime()).thenReturn(mockRuntime);
      doNothing().when(mockRuntime).exit(0);

      var f = Statics.class.getDeclaredField("instance");
      f.setAccessible(true);
      f.set(null, mockStatics);

      var test = new Foo();

      try { Cli.parseCLI(new String[] {"-h"}, test); }
      catch (Throwable ignored) {}

      verify(mockRuntime, new Times(1)).exit(0);

      try { Cli.parseCLI(new String[] {"--help"}, test); }
      catch (Throwable ignored) {}

      verify(mockRuntime, new Times(2)).exit(0);
    }
  }

  @Nested
  class EmptyToNull {
    private class Bar {
      private String a;
      private String b;
      private Integer c;
      private Integer d;
      private Object e;
    }

    @Test
    void success() {
      var test = new Bar();
      test.a = "";
      test.b = "foo";
      test.c = 0;
      test.d = 10;

      Cli.emptyToNull(test);

      assertNull(test.a);
      assertEquals("foo", test.b);
      assertNull(test.c);
      assertEquals(10, test.d);
      assertNull(test.e);
    }
  }
}
