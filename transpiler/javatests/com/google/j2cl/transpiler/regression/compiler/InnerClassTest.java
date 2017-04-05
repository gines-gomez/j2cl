package com.google.j2cl.transpiler.regression.compiler;

import static com.google.common.truth.Truth.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests the new JDK 1.5 varargs functionality. */
@RunWith(JUnit4.class)
@SuppressWarnings("ClassCanBeStatic")
public class InnerClassTest {

  static class OuterRefFromSuperCtorBase {
    OuterRefFromSuperCtorBase(Object o) {
      o.toString();
    }
  }

  class InnerClass {
    {
      callInner();
    }

    void callInner() {
      testAppend.append("a");
      class ReallyInnerClass {
        {
          callReallyInner();
        }

        void callReallyInner() {
          testAppend.append("b");
        }
      }
      new ReallyInnerClass();
    }
  }

  class OuterRefFromSuperCtorCall extends OuterRefFromSuperCtorBase {
    OuterRefFromSuperCtorCall() {
      super(
          new Object() {
            @Override
            public String toString() {
              testAppend.append("OuterRefFromSuperCtorCall");
              return "";
            }
          });
    }
  }

  class OuterRefFromThisCtorCall extends OuterRefFromSuperCtorBase {
    public OuterRefFromThisCtorCall(Object object) {
      super(object);
    }

    public OuterRefFromThisCtorCall() {
      this(
          new Object() {
            @Override
            public String toString() {
              testAppend.append("OuterRefFromThisCtorCall");
              return "";
            }
          });
    }
  }

  static class P1<T1> {
    class P2<T2> extends P1<T1> {
      class P3<T3> extends P2<T2> {
        P3() {
          this(1);
        }

        P3(int i) {
          P2.this.super(i);
        }
      }

      P2() {
        this(1);
      }

      P2(int i) {
        super(i);
      }
    }

    final int value;

    P1() {
      this(1);
    }

    P1(int i) {
      value = i;
    }
  }

  /** Used in test {@link #testExtendsNested()} */
  private static class ESOuter {
    class ESInner {
      public int value;

      public ESInner() {
        value = 1;
      }

      public ESInner(int value) {
        this.value = value;
      }
    }

    public ESInner newESInner() {
      return new ESInner();
    }
  }

  private static class ESInnerSubclass extends ESOuter.ESInner {
    ESInnerSubclass(ESOuter outer) {
      outer.super();
    }

    ESInnerSubclass(int value, ESOuter outer) {
      outer.super(value);
    }
  }

  /** Used in test {@link #testExtendsNestedWithGenerics()} */
  private static class ESWGOuter<T> {
    class ESWGInner {
      public int value;

      public ESWGInner() {
        value = 1;
      }

      public ESWGInner(int value) {
        this.value = value;
      }
    }

    public ESWGInner newESWGInner() {
      return new ESWGInner();
    }
  }

  private static class ESWGInnerSubclass extends ESWGOuter<String>.ESWGInner {
    ESWGInnerSubclass(ESWGOuter<String> outer) {
      outer.super();
    }

    ESWGInnerSubclass(int value, ESWGOuter<String> outer) {
      outer.super(value);
    }
  }

  private StringBuilder testAppend = new StringBuilder();

  @Test
  public void testExtendsNested() {
    ESOuter o = new ESOuter();
    assertEquals(1, o.new ESInner().value);
    assertEquals(2, o.new ESInner(2).value);
    assertEquals(1, new ESInnerSubclass(o).value);
    assertEquals(2, new ESInnerSubclass(2, o).value);
  }

  /** Test for Issue 7789 */
  @Test
  public void testExtendsNestedWithGenerics() {
    ESWGOuter<String> o = new ESWGOuter<String>();
    assertEquals(1, o.new ESWGInner().value);
    assertEquals(2, o.new ESWGInner(2).value);
    assertEquals(1, new ESWGInnerSubclass(o).value);
    assertEquals(2, new ESWGInnerSubclass(2, o).value);
  }

  @Test
  public void testInnerClassCtors() {
    P1<?> p1 = new P1<Object>();
    assertEquals(1, p1.value);
    assertEquals(2, new P1<Object>(2).value);
    P1<?>.P2<?> p2 = p1.new P2<Object>();
    assertEquals(1, p2.value);
    assertEquals(2, p1.new P2<Object>(2).value);
    assertEquals(1, p2.new P3<Object>().value);
    assertEquals(2, p2.new P3<Object>(2).value);
  }

  @Test
  public void testInnerClassInitialization() {
    new InnerClass();
    assertEquals("ab", testAppend.toString());
  }

  @Test
  public void testInnerClassLoop() {
    final StringBuilder b = new StringBuilder();
    abstract class AppendToStringBuilder {
      int num;

      public AppendToStringBuilder(int i) {
        this.num = i;
      }

      public abstract void act();
    }
    List<AppendToStringBuilder> results = new ArrayList<AppendToStringBuilder>();
    for (int i = 0; i < 10; i++) {
      AppendToStringBuilder ap =
          new AppendToStringBuilder(i) {
            @Override
            public void act() {
              b.append(num);
              testAppend.append(num);
            }
          };
      results.add(ap);
    }
    for (AppendToStringBuilder theAp : results) {
      theAp.act();
    }
    assertEquals("0123456789", b.toString());
    assertEquals("0123456789", testAppend.toString());
  }

  @Test
  public void testOuterThisFromSuperCall() {
    new OuterRefFromSuperCtorCall();
    assertEquals("OuterRefFromSuperCtorCall", testAppend.toString());
  }

  @Test
  public void testOuterThisFromThisCall() {
    new OuterRefFromThisCtorCall();
    assertEquals("OuterRefFromThisCtorCall", testAppend.toString());
  }

  private static void assertEquals(Object expected, Object value) {
    assertThat(value).isEqualTo(expected);
  }
}
