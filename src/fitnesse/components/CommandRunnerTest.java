// Copyright (C) 2003,2004,2005 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the GNU General Public License version 2 or later.
package fitnesse.components;

import fitnesse.testutil.RegexTestCase;

public class CommandRunnerTest extends RegexTestCase {

  public void setUp() throws Exception {
  }

  public void tearDown() throws Exception {
  }

  public void testBasics() throws Exception {
    CommandRunner runner = new CommandRunner("java -cp ./classes fitnesse.testutil.Echo", "echo this!");
    runner.run();
    assertHasRegexp("echo this!", runner.getOutput());
    assertEquals("", runner.getError());
    assertEquals(false, runner.hasExceptions());
    assertEquals(0, runner.getExitCode());
  }

  public void testClassNotFound() throws Exception {
    CommandRunner runner = new CommandRunner("java BadClass", null);
    runner.run();
    assertHasRegexp("java.lang.NoClassDefFoundError", runner.getError());
    assertEquals("", runner.getOutput());
    assertTrue(0 != runner.getExitCode());
  }
}
