/**
 * This is used to get web pages corresponding to generated URLs.
 * @author Christopher Ellis (ChrisJoinEngine)
 */
package parser;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.jsoup.Connection.Response;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;

import urlLibrary.URLBank;

public class WebParser 
{
	private static final String LOCATION_HEADER="location";
	private static final String PROXY_DELIMITER = ":";
	private static final String CHECK_TOR_URL="https://check.torproject.org/";
	private static final String FAIL_STRING="You are not using Tor";
	private static final String SSL_PREFIX="https://";
	private static final String HTTP_PREFIX="http://";
	private static final String POST_REQUEST_KEYWORD="POST";
	
	private static final int OK_RESPONSE_CODE=200;
	private static final int FORBIDDEN_RESPONSE_CODE=403;
	private static final String HOST_KEYWORD="Host";
	private static boolean FORCE_SSL=false;
	
    // Build the client.
	private static SocketAddress sockAddr = null;
	private static Proxy proxy = null;
	private static List<String> proxyList= null;
	private static boolean requestTypeAsPost=true;
	private static boolean proxyNotSet=true;
	private static boolean stopOnBlock=false;
	private static boolean proxyListSet=false;
	private static boolean BRUTE_MODE=true; //set to a default option
	private static boolean REDIRECT_MODE=true;
	private static Random random=new Random();
	private static String redirectHolder=null;
	private static int responseHolder=0;
	private static String bodyHolder="";
	private static CloseableHttpClient httpClient = null;
	static HttpClientBuilder builder = HttpClientBuilder.create();
	static PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();

	//http://www.useragentstring.com/pages/useragentstring.php used for reference
	private static final String[] USER_AGENT_LIST=
			{"Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36",
			"Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2226.0 Safari/537.36",
			"Mozilla/5.0 (Windows NT 6.1; WOW64; rv:40.0) Gecko/20100101 Firefox/40.1",
			"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10; rv:33.0) Gecko/20100101 Firefox/33.0",
			"Mozilla/5.0 (X11; Linux x86_64; rv:17.0) Gecko/20121202 Firefox/17.0 Iceweasel/17.0.1",
			"Mozilla/5.0 (Windows NT 6.1; WOW64; Trident/7.0; AS; rv:11.0) like Gecko",
			"Mozilla/5.0 (Windows; U; MSIE 7.0; Windows NT 6.0; en-US)",
			"Opera/9.80 (X11; Linux i686; Ubuntu/14.10) Presto/2.12.388 Version/12.16",
			"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_3) AppleWebKit/537.75.14 (KHTML, like Gecko) Version/7.0.3 Safari/7046A194A",
			"Mozilla/5.0 (iPad; CPU OS 6_0 like Mac OS X) AppleWebKit/536.26 (KHTML, like Gecko) Version/6.0 Mobile/10A5355d Safari/8536.25",
			"Mozilla/5.0 (BlackBerry; U; BlackBerry 9900; en) AppleWebKit/534.11+ (KHTML, like Gecko) Version/7.1.0.346 Mobile Safari/534.11+",
			"Mozilla/5.0 (Linux; U; Android 4.0.3; ko-kr; LG-L160L Build/IML74K) AppleWebkit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30"};
	
	private static String USER_AGENT="Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36";
	private static boolean STATIC_USER_AGENT=false;
	private static int TIMEOUT = 3000;
	private static boolean USE_PROXY=false;
	
	private static String proxyServer="127.0.0.1";
	private static int proxyPort=8080;
	private static Random rand=new Random();

	
	/**
	 * This is used to get a page URL from a condensed URL. If a page is fetched successfully, it's expanded URL is returned.
	 * Pages are NOT navigated to directly (lots of malware, that would be a mistake).
	 * @param url the URL to attempt navigation and expansion on
	 * @return the expanded URL or null if the URL was found to be invalid
	 */
	public static String getPage(String url) 
	{
		String result=null;
		
		if (!STATIC_USER_AGENT)
			setUserAgent(USER_AGENT_LIST[rand.nextInt(USER_AGENT_LIST.length)]);
		
		if (USE_PROXY && proxyNotSet && proxyListSet==false)
		{
			sockAddr = new InetSocketAddress(proxyServer,proxyPort);
			proxy=new Proxy(Proxy.Type.SOCKS, sockAddr);
			proxyNotSet=false;
		}
		else if (proxyListSet) //We will have to do the overhead of detecting if socks or http, so don't set the proxyNotSet to false
		{
			int choice=random.nextInt(proxyList.size());
			parseProxy(proxyList.get(choice));
			sockAddr = new InetSocketAddress(proxyServer,proxyPort);
			proxy = new Proxy(Proxy.Type.SOCKS, sockAddr);
		}
	
		try
		{		
			result=connect(url);
			return result;	
		}
		catch (org.jsoup.HttpStatusException pageNotFound)
		{
			return null;
		}
		catch (java.net.SocketException malformedRequest)
		{
			sockAddr = new InetSocketAddress(proxyServer,proxyPort);
			proxy=new Proxy(Proxy.Type.HTTP, sockAddr);
		
			try
			{
				result=connect(url);	
				return result;
			}
			catch (SocketTimeoutException ranOut)
			{
				System.out.println("connection timed out. Try increasing timeout.");
				return null;
			}
			catch (Exception e)
			{
				e.printStackTrace();
				System.out.println("Unknown exception occurred. Possible connection issue, try increasing timeout");
				return null;
			}
		}
		catch (SocketTimeoutException ranOut)
		{
			System.out.println("connection timed out. Try increasing timeout.");
			return null;
		}
		catch (SSLException sslIssue)
		{
			System.out.println("Could not negotiate an SSL connection, probably an untusted cert.");
			System.out.println("You can disable SSL checks with a flag or trust the cert");
			System.out.println("Terminating.");
			System.exit(0);
			return null;
		}
		catch (Exception unknownException)
		{
			unknownException.printStackTrace();
			System.out.println("Unknown exception occurred. Possible connection issue, try increasing timeout");
			return null;
		}
	}
	
	/**
	 * Splits a human readable proxy setting into a separate server and proxy, and then turns on the
	 * use proxy setting.
	 * @param proxy the human readable version to split
	 */
	public static void parseProxy(String proxy)
	{
		try
		{
			String[] settings=proxy.split(PROXY_DELIMITER);
			proxyServer=settings[0].replaceAll(PROXY_DELIMITER, "");
			proxyPort=Integer.parseInt(settings[1].replaceAll(PROXY_DELIMITER, ""));
			USE_PROXY=true; 
		}
		catch (Exception e)
		{
			System.out.println("Invalid proxy setting detected. Terminating.");
			System.exit(0);
		}
	}
	
	/**
	 * Parses a list of proxies to use and then loads them into a list for other methods
	 * @param filename the file containing a list of proxies, each line item is one proxy
	 * i.e.
	 * 127.0.0.1:1080
	 * 127.0.0.1:1090
	 * 127.0.0.1:8080
	 * proxies can be any combination of HTTP/SOX proxies.
	 */
	public static void parseProxyList(String filename)
	{
		try
		{
			proxyList = Files.readAllLines(Paths.get(filename));
			proxyListSet=true;
		} 
		catch (IOException e)//IOException e)
		{
			e.printStackTrace();
			System.out.println("Invalid proxy list file.");
			proxyListSet=false;
		}
	}
	
	/**
	 * Returns the timeout setting
	 * @return the timeout setting in milliseconds
	 */
	public static int getTimeout()
	{
		return TIMEOUT;
	}
	
	/**
	 * This sets the duration before page timeout
	 * @param args the duration to wait before page timeout
	 */
	public static void setTimeout(String args)
	{
		try
		{
			TIMEOUT=Integer.parseInt(args);
		}
		catch (Exception e)
		{
			System.out.println("Invalid timeout parameter value. Terminating.");
			System.exit(0);
		}
	}

	/**
	 * Sets the user agent to a custom value
	 * @param newValue the new value to use
	 */
	public static void setUserAgent(String newValue) 
	{
		USER_AGENT=newValue;
	}
	
	/**
	 * Sets if the a static user agent should be used, otherwise UA is rotated per request
	 * @param newValue true or false if a user agent rotation should be used
	 */
	public static void setStaticUserAgent(boolean newValue)
	{
		STATIC_USER_AGENT=newValue;
	}
	
	/**
	 * This sets the brute force value to the provided input
	 * @param newValue the new value to set the brute force mode to
	 */
	public static void setBruteMode(boolean newValue)
	{
		BRUTE_MODE=newValue;
	}
	
	/**
	 * This returns the value of the brute force mode
	 * @return the value of the brute force mode
	 */
	public static boolean getBruteMode()
	{
		return BRUTE_MODE;
	}
	
	/**
	 * This checks if TOR is enabled. If traffic is not routed through TOR, the program terminates.
	 * This is an optional flag.
	 */
	public static void checkTor()
	{
		try
		{
			Response response=Jsoup.connect(CHECK_TOR_URL).followRedirects(false).proxy(proxy).userAgent(USER_AGENT).timeout(TIMEOUT).execute();
			if (response.statusCode()!=200)
			{
				System.out.println("TOR check page not reached. You are likely not using tor. Terminating.");
				System.exit(0);
			}
			if (response.body().contains(FAIL_STRING))
			{
				System.out.println("You are not making a connection through TOR. Terminating.");
				System.exit(0);
			}
			System.out.println("Tor check validated.");
		}
		catch (Exception e)
		{
			System.out.println("Tor check failed. This could be a timeout, or unreachable page (blocked). Terminating.");
			System.exit(0);
		}
	}
	
	/**
	 * This handles the actual connection. It is a helper method of this class.
	 * @param url the url to connect to 
	 * @return returns the result of the connection, this will be a location depending on mode
	 * @throws IOException
	 */
	private static String connect(String url) throws IOException
	{
		String result=null;
		Response response=null;
		String stringResponse="";
		String body="";
		int responseCode=0;
		
		if (URLBank.getUseRequestFile())
		{
			stringResponse=executeRequestFromFile(url);
			responseCode=responseHolder;
		}
		else
		{
			try
			{
				response=Jsoup.connect(url).followRedirects(false).proxy(proxy).userAgent(USER_AGENT).timeout(TIMEOUT).execute();
				responseCode=response.statusCode();
				body=response.body().toString();
			}
			catch (HttpStatusException pageNotFound)
			{
				result=null;
				return result;
			}
		}
		
		if (stopOnBlock && (response.statusCode()==FORBIDDEN_RESPONSE_CODE || stringResponse.contains(FORBIDDEN_RESPONSE_CODE+"")))
		{
			System.out.println("Block detected. Terminating");
			System.exit(0);
		}
		
		if (REDIRECT_MODE)
		{
			if (response!=null)
			{
				System.out.println("We are here at the top");
				result=response.header(LOCATION_HEADER);
			}
			else
			{
				result=redirectHolder;
			}
		}
				
		if (BRUTE_MODE && (responseCode==OK_RESPONSE_CODE))
		{	
			if (URLBank.getUseRequestFile())
			{
				
				Pattern p = Pattern.compile("<head>.*?<title>(.*?)</title>.*?</head>", Pattern.DOTALL); 
				Matcher m = p.matcher(bodyHolder);  //https://stackoverflow.com/questions/9912644/java-regular-expression-to-extract-page-title
				while (m.find())
				{
					result = m.group(1);
				}
			}
			else	
			{
				result = response.parse().title();
			}
			
			if ((result==null) || result.equals("") || result.equals(" "))
			{
				if (bodyHolder.contains("captcha") || body.contains("captcha")) 
				{
					System.out.println("POSSIBLE CAPTCHA DETECTED.");
					result=null;
				}
				else 
					result=url;
			}
		}
		return result;
	}
	
	/**
	 * Disable all SSL cert validation, code is taken from stack exchange directly, code from
	 * http://stackoverflow.com/questions/2793150/using-java-net-urlconnection-to-fire-and-handle-http-requests/2793153#2793153
	 */
	public static void disableSSLCheck()
	{
		//For Apache
		try 
		{
			
			 SSLContext sslContext = SSLContextBuilder.create().loadTrustMaterial(new TrustSelfSignedStrategy()).build();
		     HostnameVerifier allowAllHosts = new NoopHostnameVerifier();
		     SSLConnectionSocketFactory connectionFactory = new SSLConnectionSocketFactory(sslContext, allowAllHosts);
		     builder.setSSLSocketFactory(connectionFactory);			
		}
		catch (Exception e)
		{
			System.out.println("Could not disable cert checking. Terminating.");
			e.printStackTrace();
			System.exit(0);
		}
		
		//For other
		TrustManager[] trustAllCertificates = new TrustManager[] 
		{
				new X509TrustManager() 
				{
					@Override
					public X509Certificate[] getAcceptedIssuers() {return null;}
		            @Override
		            public void checkClientTrusted(X509Certificate[] certs, String authType) {}
		            @Override
		            public void checkServerTrusted(X509Certificate[] certs, String authType) {}
		        }
		    };

		    HostnameVerifier trustAllHostnames = new HostnameVerifier() 
		    {
		        @Override
		        public boolean verify(String hostname, SSLSession session) {return true; }
		    };

		    try
		    {
		        System.setProperty("jsse.enableSNIExtension", "false");
		        SSLContext sc = SSLContext.getInstance("SSL");
		        sc.init(null, trustAllCertificates, new SecureRandom());
		        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		        HttpsURLConnection.setDefaultHostnameVerifier(trustAllHostnames);
		    }
		    catch (GeneralSecurityException e) 
		    {
		        throw new ExceptionInInitializerError(e);
		    }
	}

	/**
	 * This sets the new value for the redirect mode. If enabled, the applcation will not flag redirects.
	 * @param newValue the new value to use for the redirect mode
	 */
	public static void setRedirectMode(boolean newValue) 
	{
		REDIRECT_MODE=newValue;
	}
	
	/**
	 * This returns the current redirect mode setting.
	 * @return the current redirect mode setting.
	 */
	public static boolean getRedirectMode()
	{
		return REDIRECT_MODE;
	}
	
	/**
	 * Sets if the application should terminate on 403 responses, sometimes
	 * these are thrown when the application begins to get dynamically blocked
	 * @param newValue true or false if the application should stop when blocking occurs
	 */
	public static void setStopOnBlock(boolean newValue)
	{
		stopOnBlock=newValue;
	}
	
	/**
	 * Used in conjunction with post requests, set this flag true to 
	 * force requests to use SSL instead of HTTP.
	 * @param newValue the new value for this flag
	 */
	public static void setForceSSL(boolean newValue)
	{
		FORCE_SSL=newValue;
	}
	
	/**
	 * Parses a post request and returns the URL that the data should be sent to. This
	 * is tested against a post construction made in BurpSuite, should work in other context
	 * but have not tested.
	 * @param requestData the post request that should be parsed
	 * @return the post request host/url
	 */
	private static String getURLFromRawRequest(String requestData)
	{
		 String[] dataLines = requestData.split("\\r?\\n");
		 try
		 {
			 String firstLine=dataLines[0];
			 if (firstLine.contains(POST_REQUEST_KEYWORD))
				 requestTypeAsPost=true;
			 else
				 requestTypeAsPost=false;
					 
			 String resourceID=firstLine.substring(firstLine.indexOf("/"), firstLine.lastIndexOf(" "));
			 for (String element : dataLines)
			 {
				 if (element.contains(HOST_KEYWORD))
				 {
					 if (FORCE_SSL)
					 {
						 return SSL_PREFIX+element.substring(element.indexOf(" ")+1) + resourceID; //assuming colon and space before data 
					 }
					 else
						 return HTTP_PREFIX+element.substring(element.indexOf(" ")+1) + resourceID;
				 }
			 }
		 }
		 catch (Exception e)
		 {
			 System.out.println("Could not parse the post request, issue in the URI");
		 }
		
		return null;
	}
	
	/**
	 * In the event of a post request read from file, there is sometimes additional data
	 * below the headers, this extracts that data as a string so it can be appended to 
	 * a new request.
	 * @param requestData the request to be parsed for additional post data beyond headers
	 * @return the additonal data at the bottom of the orginal post request
	 */
	private static String getAdditionalPostData(String requestData)
	{
		String[] dataLines = requestData.split("\\r?\\n");
		int finishIndex=1;
		for (finishIndex=1; finishIndex<dataLines.length; finishIndex++)
		{
			//I am assuming the space, possible to fool, will come back and refine
			int point=dataLines[finishIndex].indexOf(": "); 
			if (point<0)
				break;
		}
		
		String toReturn="";
		for (int i=finishIndex; i<dataLines.length; i++)
		{
			toReturn+=dataLines[i];
		}
		return toReturn;
	}
	/**
	 * Pulls out the requested URL from an input request file
	 * @param requestData the input request file to extract a URL from to target
	 * @return the requested URL (host header combined with resource ID in 
	 * standard requests it seems).
	 * @throws IOException 
	 */
	private static String executeRequestFromFile(String requestData) throws IOException
	{
		CloseableHttpResponse response = null;
		try
		{
			String targetURL=getURLFromRawRequest(requestData);
			RequestConfig config= null;
			
			
			config = RequestConfig.custom()
					.setConnectTimeout(TIMEOUT)
					.setConnectionRequestTimeout(TIMEOUT)
					.setSocketTimeout(TIMEOUT)
					.build();
			
			try //Construct the HTTpClients
			{
				String[] proxySplit=proxy.address().toString().substring(1).split(PROXY_DELIMITER);
				HttpHost newProxy = new HttpHost(proxySplit[0], Integer.parseInt(proxySplit[1]));
				
				//DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(newProxy);
				builder.setProxy(newProxy);
			}
			catch (NullPointerException npe){}

			builder.disableRedirectHandling();
			builder.setDefaultRequestConfig(config);
			builder.setMaxConnTotal(200);
			builder.setMaxConnPerRoute(50);
			httpClient = builder.build();
			
			if (requestTypeAsPost)
			{
				HttpPost httpPost=new HttpPost(targetURL);
				String body=getAdditionalPostData(requestData);
				setHeaders(httpPost, requestData);
				httpPost.setEntity(new StringEntity(body));
				
				response = httpClient.execute(httpPost);
			}
			else
			{
				HttpGet httpGet=new HttpGet(targetURL);
				setHeaders(httpGet, requestData);
				response = httpClient.execute(httpGet);
			}
			setHoldersFromResponse(response);
			String toReturn=response.toString();
			EntityUtils.consumeQuietly(response.getEntity());
			response.close();
			
			return toReturn;
		}
		catch (Exception e)
		{
			System.out.println("Some unknown error occured. Terminating \nThis sometimes occurs if you are not using SSL and the\n"
					+ "target does not support that. Try the force ssl option.");
			System.out.println("You may also want to decrease threads or enable polite mode.");
			e.printStackTrace();
			System.exit(0);
		}
		//finally
		//{
	//		response.close();
	//		EntityUtils.consumeQuietly(response.getEntity());
	//	}
		return null;
	}
	
	/**
	 * This sets the headers of a request to match those of an input file
	 * @param request the request to set the headers for
	 * @param newHeaderString the file to set the headers for
	 * @return the headers to set
	 */
	private static HttpRequest setHeaders(HttpRequest request, String newHeaderString)
	{ 
		String[] dataLines = newHeaderString.split("\\r?\\n");
		for (int i=1; i<dataLines.length; i++)
		{
			String workingHeader=dataLines[i].replace(" ", "");
			try
			{
				int headerSplitIndex=workingHeader.indexOf(":");
				String headerName=workingHeader.substring(0, headerSplitIndex);
				String headerValue=workingHeader.substring(headerSplitIndex+1);
				request.setHeader(headerName, headerValue);
			}
			catch (IndexOutOfBoundsException oob)
			{
				break;
			}
		}
		return request;		
	}
	
	/**
	 * A method used to set value holders when using a request from file. This
	 * is not a great way to do this, will need to refine.
	 * @param response the response to parse and set values from.
	 */
	private static void setHoldersFromResponse(CloseableHttpResponse response)
	{
		ResponseHandler<String> handler = new BasicResponseHandler();
		
		try {redirectHolder=response.getFirstHeader(LOCATION_HEADER).getValue();} catch (NullPointerException e){redirectHolder=null;}
		try {bodyHolder=handler.handleResponse(response);}catch (Exception e){bodyHolder="";}
		responseHolder=response.getStatusLine().getStatusCode();	
	}
}
