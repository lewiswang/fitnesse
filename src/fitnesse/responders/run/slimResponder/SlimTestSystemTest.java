package fitnesse.responders.run.slimResponder;

import fitnesse.FitNesseContext;
import fitnesse.http.MockRequest;
import fitnesse.http.Response;
import fitnesse.http.SimpleResponse;
import fitnesse.wiki.*;
import fitnesse.wikitext.Utils;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

public class SlimTestSystemTest {
  private WikiPage root;
  private PageCrawler crawler;
  private FitNesseContext context;
  private MockRequest request;
  protected SlimResponder responder;
  private WikiPage testPage;
  public String testResults;
  protected SimpleResponse response;

  private void assertTestResultsContain(String fragment) {
    String unescapedResults = unescape(testResults);
    assertTrue(unescapedResults, unescapedResults.indexOf(fragment) != -1);
  }

  private void getResultsForPageContents(String pageContents) throws Exception {
    request.setResource("TestPage");
    PageData data = testPage.getData();
    data.setContent(data.getContent() + "\n" + pageContents);
    testPage.commit(data);
    response = (SimpleResponse) responder.makeResponse(context, request);
    testResults = response.getContent();
  }

  @Before
  public void setUp() throws Exception {
    root = InMemoryPage.makeRoot("root");
    crawler = root.getPageCrawler();
    context = new FitNesseContext(root);
    request = new MockRequest();
    responder = getSlimResponder();
    responder.setFastTest(true);
    testPage = crawler.addPage(root, PathParser.parse("TestPage"), "!path classes");
  }

  protected SlimResponder getSlimResponder() {
    return new HtmlSlimResponder();
  }


  @Test
  public void slimResponderStartsAndQuitsSlim() throws Exception {
    responder.setFastTest(false);
    request.setResource("TestPage");
    responder.makeResponse(context, request);
    assertTrue(!responder.slimOpen());
  }

  @Test
  public void verboseOutputIfSlimFlagSet() throws Exception {
    getResultsForPageContents("!define SLIM_FLAGS {-v}\n");
    assertTrue(responder.getCommandLine().indexOf("java -cp classes fitnesse.slim.SlimService -v") != -1);
  }

  @Test
  public void tableWithoutPrefixWillBeConstructed() throws Exception {
    getResultsForPageContents("|XX|\n");
    assertTestResultsContain("<td>XX <span class=\"error\">Could not invoke constructor for XX[0]</span></td>");
  }

  @Test
  public void emptyQueryTable() throws Exception {
    getResultsForPageContents("|Query:x|\n");
    assertTestResultsContain("Query tables must have at least two rows.");
  }

  @Test
  public void queryFixtureHasNoQueryFunction() throws Exception {
    getResultsForPageContents(
      "!|Query:fitnesse.slim.test.TestSlim|\n" +
        "|x|y|\n"
    );
    assertTestResultsContain("Method query[0] not found in fitnesse.slim.test.TestSlim");
  }

  @Test
  public void emptyScriptTable() throws Exception {
    getResultsForPageContents("|Script|\n");
    assertTestResultsContain("Script tables must at least one statement.");
  }

  @Test
  public void emptyImportTable() throws Exception {
    getResultsForPageContents("|Import|\n");
    assertTestResultsContain("Import tables must have at least two rows.");
  }

  @Test
  public void emptyTableTable() throws Exception {
    getResultsForPageContents("|Table|\n");
    assertTestResultsContain("Table tables must have at least two rows.");
  }

  @Test
  public void tableFixtureHasNoDoTableFunction() throws Exception {
    getResultsForPageContents(
      "|Table:fitnesse.slim.test.TestSlim|\n" +
        "|a|b|\n"
    );
    assertTestResultsContain("Table fixture has no valid doTable method");
  }


  @Test
  public void simpleDecisionTable() throws Exception {
    getResultsForPageContents(
      "!|DT:fitnesse.slim.test.TestSlim|\n" +
        "|returnInt?|\n" +
        "|7|\n"
    );
    assertTestResultsContain("<span class=\"pass\">7</span>");
  }

  @Test
  public void decisionTableWithNoExecuteDoesNotCountExceptionsForExecute() throws Exception {
    getResultsForPageContents(
      "!|DT:fitnesse.slim.test.DummyDecisionTable|\n" +
        "|x?|\n" +
        "|1|\n"
    );
    assertEquals(0, responder.getTestSummary().exceptions);
  }

  @Test
  public void decisionTableWithExecuteThatThrowsDoesShowsException() throws Exception {
    getResultsForPageContents(
      "!|DT:fitnesse.slim.test.DecisionTableExecuteThrows|\n" +
        "|x?|\n" +
        "|1|\n"
    );
    assertEquals(1, responder.getTestSummary().exceptions);
    assertTestResultsContain("EXECUTE_THROWS");
  }

  @Test
  public void tableWithException() throws Exception {
    getResultsForPageContents(
      "!|DT:NoSuchClass|\n" +
        "|returnInt?|\n" +
        "|7|\n"
    );
    assertTestResultsContain("<span class=\"error\">Could not invoke constructor for NoSuchClass");
  }

  @Test
  public void tableWithBadConstructorHasException() throws Exception {
    getResultsForPageContents(
      "!|DT:fitnesse.slim.test.TestSlim|badArgument|\n" +
        "|returnConstructorArgument?|\n" +
        "|3|\n"
    );
    TableScanner ts = new HtmlTableScanner(responder.getTestResults().getHtml());
    ts.getTable(0);
    assertTestResultsContain("Could not invoke constructor");
  }

  @Test
  public void tableWithBadVariableHasException() throws Exception {
    getResultsForPageContents(
      "!|DT:fitnesse.slim.test.TestSlim|\n" +
        "|noSuchVar|\n" +
        "|3|\n"
    );
    assertTestResultsContain("<span class=\"error\">Method setNoSuchVar[1] not found in fitnesse.slim.test.TestSlim");
  }

  @Test
  public void tableWithSymbolSubstitution() throws Exception {
    getResultsForPageContents(
      "!|DT:fitnesse.slim.test.TestSlim|\n" +
        "|string|getStringArg?|\n" +
        "|Bob|$V=|\n" +
        "|$V|$V|\n" +
        "|Bill|$V|\n" +
        "|John|$Q|\n"
    );
    TableScanner ts = getScannedResults();
    Table dt = ts.getTable(0);
    assertEquals("$V<-[Bob]", unescape(dt.getCellContents(1, 2)));
    assertEquals("$V->[Bob]", unescape(dt.getCellContents(0, 3)));
    assertEquals("<span class=\"pass\">$V->[Bob]</span>", unescape(dt.getCellContents(1, 3)));
    assertEquals("[Bill] <span class=\"fail\">expected [$V->[Bob]]</span>", unescape(dt.getCellContents(1, 4)));
    assertEquals("[John] <span class=\"fail\">expected [$Q]</span>", unescape(dt.getCellContents(1, 5)));
  }

  protected TableScanner getScannedResults() throws Exception {
    return new HtmlTableScanner(testResults);
  }

  private String unescape(String x) {
    return Utils.unescapeWiki(Utils.unescapeHTML(x));
  }

  @Test
  public void substituteSymbolIntoExpression() throws Exception {
    getResultsForPageContents(
      "!|DT:fitnesse.slim.test.TestSlim|\n" +
        "|string|getStringArg?|\n" +
        "|3|$A=|\n" +
        "|2|<$A|\n" +
        "|5|$B=|\n" +
        "|4|$A<_<$B|\n"
    );
    TableScanner ts = getScannedResults();
    Table dt = ts.getTable(0);
    assertEquals("<span class=\"pass\">2<$A->[3]</span>", unescape(dt.getCellContents(1, 3)));
    assertEquals("<span class=\"pass\">$A->[3]<4<$B->[5]</span>", unescape(dt.getCellContents(1, 5)));
  }

  @Test
  public void tableWithExpression() throws Exception {
    getResultsForPageContents(
      "!|DT:fitnesse.slim.test.TestSlim|\n" +
        "|string|getStringArg?|\n" +
        "|${=3+4=}|7|\n"
    );
    TableScanner ts = getScannedResults();
    Table dt = ts.getTable(0);
    assertEquals("<span class=\"pass\">7</span>", dt.getCellContents(1, 2));
  }

  @Test
  public void noSuchConverter() throws Exception {
    getResultsForPageContents(
      "|!-DT:fitnesse.slim.test.TestSlim-!|\n" +
        "|noSuchConverter|noSuchConverter?|\n" +
        "|x|x|\n"
    );
    TableScanner ts = getScannedResults();
    Table dt = ts.getTable(0);
    assertEquals("x <span class=\"error\">No converter for fitnesse.slim.test.TestSlim$NoSuchConverter.</span>", dt.getCellContents(0, 2));
  }

  @Test
  public void translateExceptionMessage() throws Exception {
    assertTranslatedException("Could not find constructor for SomeClass", "NO_CONSTRUCTOR SomeClass");
    assertTranslatedException("Could not invoke constructor for SomeClass", "COULD_NOT_INVOKE_CONSTRUCTOR SomeClass");
    assertTranslatedException("No converter for SomeClass", "NO_CONVERTER_FOR_ARGUMENT_NUMBER SomeClass");
    assertTranslatedException("Method someMethod not found in SomeClass", "NO_METHOD_IN_CLASS someMethod SomeClass");
    assertTranslatedException("The instance someInstance does not exist", "NO_INSTANCE someInstance");
    assertTranslatedException("Could not find class SomeClass", "NO_CLASS SomeClass");
    assertTranslatedException("The instruction [a, b, c] is malformed", "MALFORMED_INSTRUCTION [a, b, c]");
  }

  private void assertTranslatedException(String expected, String message) {
    assertEquals(expected, SlimTestSystem.translateExceptionMessage(message));
  }

  @Test
  public void returnedListsBecomeStrings() throws Exception {
    getResultsForPageContents("!|script|\n" +
      "|start|fitnesse.slim.test.TestSlim|\n" +
      "|one list|1,2|\n" +
      "|check|get list arg|[1, 2]|\n");
    assertTestResultsContain("<td><span class=\"pass\">[1, 2]</span></td>");
  }

  @Test
  public void nullStringReturned() throws Exception {
    getResultsForPageContents("!|fitnesse.slim.test.TestSlim|\n" +
      "|nullString?|\n" +
      "|null|\n");
    assertTestResultsContain("<td><span class=\"pass\">null</span></td>");
  }

  @Test
  public void reportableExceptionsAreReported() throws Exception {
    getResultsForPageContents(
      "!|fitnesse.slim.test.ExecuteThrowsReportableException|\n" +
        "|x|\n" +
        "|1|\n");
    assertTestResultsContain("A Reportable Exception");
  }

  @Test
  public void emptyScenarioTable() throws Exception {
    getResultsForPageContents("|Scenario|\n");
    assertTestResultsContain("Scenario tables must have a name.");
  }

  @Test
  public void scenarioTableIsRegistered() throws Exception {
    getResultsForPageContents("|Scenario|myScenario|\n");
    assertTrue("scenario should be registered", responder.testSystem.getScenarios().containsKey("myScenario"));
  }


}
