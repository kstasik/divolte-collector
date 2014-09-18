package io.divolte.browser;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.cache.DirectBufferCache;
import io.undertow.server.handlers.resource.CachingResourceManager;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.server.handlers.resource.ResourceManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.*;
import org.openqa.selenium.phantomjs.PhantomJSDriver;

import java.time.Duration;
import java.util.Deque;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static com.thoughtworks.selenium.SeleneseTestCase.assertEquals;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.collection.IsMapContaining.hasKey;
import static org.junit.Assert.assertThat;

/**
 * Test the dvt.js signals in the headless PhantomJS browser.
 * Expect and assert signals parameters generated by dvt.js
 *
 * It's required to have PhantomJS installed:
 *
 * Mac: brew update && brew install phantomjs
 * Windows: make sure phantomjs.exe is on your PATH.
 */
public class HeadlessLocalJavaScriptEventTest {

    private Undertow server;
    private BlockingQueue<HttpServerExchange> incoming;

    // Default params
    private final String REFERER_FIELD_PRODUCER = "r";
    private final String LOCATION_FIELD_PRODUCER = "l";
    private final String VIEWPORT_PIXEL_WIDTH = "w";
    private final String VIEWPORT_PIXEL_HEIGHT = "h";
    private final String SCREEN_PIXEL_WIDTH = "i";
    private final String SCREEN_PIXEL_HEIGHT = "j";
    private final String CACHE_BUSTING = "n";

    private WebDriver driver;
    private Dimension expectedViewport;

    @Before
    public void setUp() throws Exception {
        incoming = new ArrayBlockingQueue<>(10);

        // Headless
        driver = new PhantomJSDriver();
        expectedViewport = driver.manage().window().getSize();

        // You're able to manage sattings though manage() method on driver:
        // driver.manage().timeouts().pageLoadTimeout(3, TimeUnit.SECONDS);

        startServer((exchange) -> {
            exchange.getResponseSender().send("simple text response.");
            incoming.add(exchange);
        });
    }

    @After
    public void tearDown() {
        driver.quit();
        stopServer();
    }

    @Test
    public void shouldSignalWhenOpeningPage() throws InterruptedException {
        // Open the start page, and check the title
        driver.get("http://localhost:9999/");
        assertEquals("DVT", driver.getTitle());

        // Opening the page triggers an event to devolte
        HttpServerExchange exchange = getLatestExchange();
        assertThat(exchange.getRequestURL(), is("http://localhost:9999/event"));

        Map<String, Deque<String>> params = exchange.getQueryParameters();
        assertThat(params, hasKey(SCREEN_PIXEL_WIDTH));
        assertThat(getParamAsInt(params, SCREEN_PIXEL_WIDTH), greaterThan(0));
        assertThat(params, hasKey(SCREEN_PIXEL_HEIGHT));
        assertThat(getParamAsInt(params, SCREEN_PIXEL_HEIGHT), greaterThan(0));
        assertThat(params, hasKey(VIEWPORT_PIXEL_WIDTH));
        assertThat(getParamAsInt(params, VIEWPORT_PIXEL_WIDTH), is(expectedViewport.getWidth()));
        assertThat(params, hasKey(VIEWPORT_PIXEL_HEIGHT));
        assertThat(getParamAsInt(params, VIEWPORT_PIXEL_HEIGHT), is(expectedViewport.getHeight()));
        assertThat(params, hasKey(LOCATION_FIELD_PRODUCER));
        assertThat(getParam(params, LOCATION_FIELD_PRODUCER), is("http://localhost:9999/"));
    }

    @Test
    public void shouldSignalCustomEvent() throws InterruptedException {
        // Open the start page, and check the title
        driver.get("http://localhost:9999/");
        assertEquals("DVT", driver.getTitle());

        // Default request triggered by opening the page.
        HttpServerExchange exchange = getLatestExchange();
        assertThat(exchange.getRequestURL(), is("http://localhost:9999/event"));

        // When entering my favorite language in the input and press enter...
        WebElement input = driver.findElement(By.id("favoriteLanguage"));
        input.sendKeys("Java" + Keys.ENTER);

        // ...a custom signal should be triggered.
        exchange = getLatestExchange();
        Map<String, Deque<String>> params = exchange.getQueryParameters();

        // Check the default params.
        assertThat(exchange.getRequestURL(), is("http://localhost:9999/event"));
        assertThat(params, hasKey(SCREEN_PIXEL_WIDTH));
        assertThat(params, hasKey(SCREEN_PIXEL_HEIGHT));
        assertThat(params, hasKey(VIEWPORT_PIXEL_WIDTH));
        assertThat(params, hasKey(VIEWPORT_PIXEL_HEIGHT));
        assertThat(params, hasKey(LOCATION_FIELD_PRODUCER));

        // but the "t" custom event is extra.
        assertThat(params, hasKey("t"));
        assertThat(params, hasKey("t.text"));
        assertThat(getParam(params, "t"), is("customInputLanguage"));
        assertThat(getParam(params, "t.text"), is("Java"));

        // Also a anti-cache param should be in the url.
        assertThat(params, hasKey(CACHE_BUSTING));
    }

    private HttpServerExchange getLatestExchange() throws InterruptedException {
        return Optional.ofNullable(incoming.poll(10, TimeUnit.SECONDS))
                .orElseThrow(() -> new InterruptedException("Reading response took longer than 10s."));
    }

    private void startServer(HttpHandler eventRequestHandler) {
        final PathHandler handler = new PathHandler();
        handler.addExactPath("/event", eventRequestHandler);
        handler.addPrefixPath("/", createStaticResourceHandler());

        server = Undertow.builder()
                .addHttpListener(9999, "127.0.0.1")
                .setHandler(handler)
                .build();

        server.start();
    }

    private HttpHandler createStaticResourceHandler() {
        final ResourceManager staticResources =
                new ClassPathResourceManager(getClass().getClassLoader(), "static");
        // Cache tuning is copied from Undertow unit tests.
        final ResourceManager cachedResources =
                new CachingResourceManager(100, 65536,
                new DirectBufferCache(1024, 10, 10480),
                staticResources,
                (int) Duration.ofDays(1).getSeconds());
        final ResourceHandler resourceHandler = new ResourceHandler(cachedResources);
        resourceHandler.setWelcomeFiles("index.html");
        return resourceHandler;
    }

    private void stopServer() {
        server.stop();
    }

    public static String getParam(Map<String, Deque<String>> params, String name) {
        Deque<String> values = params.get(name);
        if (values == null) {
            return null;
        } else {
            return values.getFirst();
        }
    }

    public static Integer getParamAsInt(Map<String, Deque<String>> params, String name) {
        String param = getParam(params, name);
        if (param == null) {
            return null;
        } else {
            return Integer.parseInt(param);
        }
    }
}
