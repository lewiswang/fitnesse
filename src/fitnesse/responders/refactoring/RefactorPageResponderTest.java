// Copyright (C) 2003,2004,2005 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the GNU General Public License version 2 or later.
package fitnesse.responders.refactoring;

import fitnesse.FitNesseContext;
import fitnesse.Responder;
import fitnesse.http.MockRequest;
import fitnesse.http.SimpleResponse;
import fitnesse.testutil.RegexTestCase;
import fitnesse.wiki.InMemoryPage;
import fitnesse.wiki.PageCrawler;
import fitnesse.wiki.PathParser;
import fitnesse.wiki.WikiPage;

public class RefactorPageResponderTest extends RegexTestCase {
  WikiPage root;
  private MockRequest request;
  private Responder responder;
  private String childPage = "ChildPage";
  private PageCrawler crawler;

  public void setUp() throws Exception {
    root = InMemoryPage.makeRoot("root");
    crawler = root.getPageCrawler();
    crawler.addPage(root, PathParser.parse(childPage));

    request = new MockRequest();
    request.setResource(childPage);
    responder = new RefactorPageResponder();
  }

  public void testHtml() throws Exception {
    SimpleResponse response = (SimpleResponse) responder.makeResponse(new FitNesseContext(root), request);
    assertEquals(200, response.getStatus());

    String content = response.getContent();
    assertSubString("Delete Page", content);
    assertSubString("Rename Page", content);
    assertSubString("Move Page", content);
  }
}
