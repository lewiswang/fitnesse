// Copyright (C) 2003,2004,2005 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the GNU General Public License version 2 or later.
package fitnesse.responders.run;

import fitnesse.html.HtmlPage;
import fitnesse.html.HtmlPageFactory;
import fitnesse.testutil.RegexTestCase;

public class SuiteHtmlFormatterTest extends RegexTestCase {
  private HtmlPage page;
  private SuiteHtmlFormatter formatter;

  public void setUp() throws Exception {
    page = new HtmlPageFactory().newPage();
    formatter = new SuiteHtmlFormatter(page);
  }

  public void tearDown() throws Exception {
  }

  public void testTestSummary() throws Exception {
    formatter.setPageAssertions(new TestSummary(12, 0, 0, 0));
    String summary = formatter.testSummary(new TestSummary(49, 0, 0, 0));

    assertSubString("<strong>Test Pages:</strong> 12 right, 0 wrong, 0 ignored, 0 exceptions", summary);
    assertSubString("<strong>Assertions:</strong> 49 right, 0 wrong, 0 ignored, 0 exceptions", summary);
  }

  public void testCountsHtml() throws Exception {
    String row1 = formatter.acceptResults("RelativePageName", new TestSummary(1, 0, 0, 0));
    String row2 = formatter.acceptResults("AnotherPageName", new TestSummary(0, 1, 0, 0));

    assertSubString("<div class=\"alternating_row_2\">", row1);
    assertSubString("<span class=\"test_summary_results pass\">1 right, 0 wrong, 0 ignored, 0 exceptions</span>", row1);
    assertSubString("<a href=\"#RelativePageName0\" class=\"test_summary_link\">RelativePageName</a>", row1);

    assertSubString("<div class=\"alternating_row_1\">", row2);
    assertSubString("<span class=\"test_summary_results fail\">0 right, 1 wrong, 0 ignored, 0 exceptions</span>", row2);
    assertSubString("<a href=\"#AnotherPageName0\" class=\"test_summary_link\">AnotherPageName</a>", row2);
  }

  public void testResultsHtml() throws Exception {
    formatter.announceTestSystem("Fit");
    formatter.startOutputForNewTest("RelativeName", "FullName");
    formatter.acceptOutput("starting");
    formatter.acceptOutput(" output");
    formatter.acceptResults("RelativeName", new TestSummary(1, 0, 0, 0));
    formatter.announceTestSystem("Slim");
    formatter.startOutputForNewTest("NewRelativeName", "NewFullName");
    formatter.acceptOutput("second");
    formatter.acceptOutput(" test");
    formatter.acceptResults("NewRelativeName", new TestSummary(0, 1, 0, 0));

    String results = formatter.testOutput();
    assertSubString("<h2 class=\"centered\">Test Output</h2>", results);
    assertSubString("<h2 class=\"centered\">Test System: Slim</h2>", results);

    assertSubString("<div class=\"test_output_name\">", results);
    assertSubString("<a href=\"FullName\" id=\"RelativeName1\">RelativeName</a>", results);
    assertSubString("<div class=\"alternating_block_1\">starting output</div>", results);

    assertSubString("<a href=\"NewFullName\" id=\"NewRelativeName2\">NewRelativeName</a>", results);
    assertSubString("<div class=\"alternating_block_2\">second test</div>", results);
  }
}
