package helperPackage;

import com.aventstack.extentreports.Status;
import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.client.ClientUtil;
import net.lightbody.bmp.core.har.Har;
import net.lightbody.bmp.core.har.HarEntry;
import net.lightbody.bmp.proxy.CaptureType;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;

import testPackage.BaseClass;
import utilityPackage.ConfigReader;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class BrowserFactory {

    //Global driver
    public static WebDriver driver;
    private static DesiredCapabilities capabilities;
    private static BrowserMobProxy proxy;
    private static Proxy seleniumProxy;
    private static Har har;

    //initial Har
    public static void MonitorResponseStart() throws InterruptedException {
        //Initialize Har
        proxy.newHar( BrowserFactory.driver.getCurrentUrl() );
        har = proxy.getHar();
        //Must use the sleep to wait for Har to initialize, or there may be error.
        Thread.sleep( 500 );
    }

    //Check request url and response status
    private static boolean CheckStatus(Har har,String subUrl,int statuCode) {
        //get entriesList
        List<HarEntry> entriesList = har.getLog().getEntries();
        //loop to check
        for (HarEntry harEntry : entriesList) {
            if ((harEntry.getRequest().getUrl().contains( subUrl ))
                    &&
                    (harEntry.getResponse().getStatus() == statuCode)) {
                BaseClass.testLog.log( Status.INFO, "Table reloaded done ! " );
                return true;
            }
        }
        return false;
    }

    //It is used to make sure that the table are loaded completely
    public static void MMonitorResponseEnd(String subUrl,int statuCode) throws IOException, InterruptedException {

        //if check status fail, wait and keep checking.
        while (!CheckStatus( har,subUrl,statuCode)) {
            Thread.sleep( 200 );
        }
        //write har to file, this step can be delete, just used for debugging....
        har.writeTo( new File( "har.json" ) );
    }

    //@Parameters("browserName")
    //A custom method to choose the browser on which the test need to be executed
    public static void startBrowser(String browserName) {
        //choose Firefox browser
        if (browserName.equalsIgnoreCase( "firefox" )) {
            driver = new FirefoxDriver();
        }
        //choose Chrome browser
        else if (browserName.equalsIgnoreCase( "chrome" )) {
            System.setProperty( "webdriver.chrome.driver", ConfigReader.getChromePath() );
            //****************************
            proxy = new BrowserMobProxyServer();
            proxy.start();
            // get the Selenium proxy object
            seleniumProxy = ClientUtil.createSeleniumProxy( proxy );
            // configure it as a desired capability
            capabilities = new DesiredCapabilities();
            capabilities.setCapability( CapabilityType.PROXY, seleniumProxy );
            // start the browser up
            driver = new ChromeDriver( capabilities );
            // enable more detailed HAR capture, if desired (see CaptureType for the complete list)
            proxy.enableHarCaptureTypes( CaptureType.REQUEST_CONTENT, CaptureType.RESPONSE_CONTENT );

        }
        //choose IE browser
        else if (browserName.equalsIgnoreCase( "ie" )) {
            System.setProperty( "webdriver.ie.driver", ConfigReader.getIEPath() );
            driver = new InternetExplorerDriver();
        }

        //choose chrome Headless browser
        if (browserName.equalsIgnoreCase( "headless" )) {
            System.setProperty( "webdriver.chrome.driver", ConfigReader.getChromePath() );
            //config driver for har, important step.......
            //log
            BaseClass.testLog.log( Status.INFO, "Config driver for Har... " );
            driver = new ChromeDriver();
            ChromeOptions options = new ChromeOptions();
            options.addArguments( "headless" );
            options.addArguments( "window-size=1200x600" );

            driver = new ChromeDriver( options );
        }

        //maximize browser
        driver.manage().window().maximize();

        //launch the url
        driver.get( ConfigReader.getURL() );
    }
}
