// Copyright (C) 2003,2004,2005 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the GNU General Public License version 2 or later.
package fitnesse.responders;

import fitnesse.Responder;
import fitnesse.http.MockResponseSender;
import fitnesse.http.Response;
import fitnesse.wiki.PathParser;

public class RawContentResponderTest extends ResponderTestCase {
  protected Responder responderInstance() {
    return new RawContentResponder();
  }

  public void testSimplePage() throws Exception {
    String result = getResultsUsing("simple content");
    assertSubString("simple content", result);
  }

  public void testNoHtmlRendered() throws Exception {
    String result = getResultsUsing("'''simple content'''");
    assertSubString("'''simple content'''", result);
  }

  private String getResultsUsing(String content) throws Exception {
    crawler.addPage(root, PathParser.parse("SimplePage"), content);
    request.setResource("SimplePage");
    Response response = responder.makeResponse(context, request);
    MockResponseSender sender = new MockResponseSender();
    sender.doSending(response);
    String result = sender.sentData();
    return result;
  }
}
