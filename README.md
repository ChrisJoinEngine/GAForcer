## WHAT THIS IS:
An application used to brute force compressed URLs in an intelligent manner. 
It makes up guesses by leveraging a machine learning algorithm  (genetic algorithm). 
The better a guess generating seed does, the more likely it is to continue to be used. 
Traits from well-performing seeds are combined and a seed has a chance to randomly 
mutate an attribute. Recorded successful guesses are stored in an SQLite file that may 
be parsed and grown as needed.

## USES:
This was originally designed to catch compressed URLs and extract them (think along the
lines of somesite.ly/somethingrandom); subsequently, the application has a lot of options
to run with this in mind (such as alerting on key terms provided by a file). That being said,
it works for other things too, with another example use being to access a GUID used in a
restricted URL (such as a document sharing link). While the application alerts as a hit on
redirects or 200, it does transparently support SOX and HTTP proxies, so with some creativity,
it may be altered to accommodate a wider variety of uses--NOTE: full support is not there for
requests from file option yet. With the combination  of flags it may be used as  a raw brute 
forcing tool, and offers a considerable amount of flexibility.

## OPTIMAL RUN:
The default options are NOT optimal for all (or even most) situations. They are designed to be 
somewhat less intrusive than a true optimal configuration may be. In order for the application 
to behave in a fashion that is more than just random, target space must be saturated, or a seed
attempt must have enough tries to obtain decent data as to it’s true success rate. This means that
each generation thread should make enough attempts (the iteration flag -i at the time of writing) 
to be statistically significant, something like 100, 1000, or larger if your keyspace saturation is
lower.

Similarly, to prevent your population from being to homogeneous, you have a decent thread starting
size  (that’s the size flag, -s, at the time of writing this). Depending on your use case, a population
of 25 may be sufficient in most cases. A population size of less than 10 is probably not going to
be very effective.


## FILES GENERATED:
**ScoreTracking.txt**; this keeps track of the application’s hit rate, it’s is useful to see your generational
growth and success rate trend. 

**UpcomingGeneration.txt**; this is used as a save file, so that on restarting the application, it resumes where
it left off. You may want to DELETE or ALTER this file if you want to create unusual seed conditions, start
from scratch, or target a different environment (although the application will adjust for some of these
scenarios at runtime). 

**Discovered_Links.sqlite**; this is exactly what it sounds like (cache + lookup). e.g. select GENERATED_URL from
RESULTS where ACTUAL_URL like '%facebook%’

## RISKS:
You can get your IP blocked. You can cause other unknown damage. Use it wisely or don’t get caught. I’m not to
blame for any damages caused by this tool. That’s about it..



## SAMPLE USES:
Recognized flags are:  
- -h  : display the help message (this) and then exit the program   
- -p  : causes the program to pause after every generation   
- -db : disable brute force mode (will only confirm on redirect, ignores 200 OK response)  
- -dr : disable redirect mode (will only confirm on 200 OK, ignores 301,302, etc. response)  
-k   [file]   : only alert on keywords from a provided file (DB still saves hits)  
-i   [number] : the number of tries to issue per generation child. Default is 10  
-min [number] : min length allowed for URL pieces generated by a thread. Default is 4  
-max [number] : max length allowed for URL pieces generated by a thread. Default is 8  
-s   [number] : size of generations, smaller is less traffic, bigger more accurate. Default is 10  
-m   [number] : chance of bit mutation from 1 to 1000, 1000 is 100%. Best left small. Default is 1  
-r   [file]   : uses a request from a text file. Use ^ to signify an input position. IN PROGRESS  
                Request from file is an in-progress feature, may be oddities. -ssl may be needed.  
-ssl          : forces SSL connection, only useful with the -r option.  

-proxy   [server:port] : proxy requests. (e.g. -proxy 127.0.0.1:8080) works well with timeout set.  
-lproxy  [file]        : provide a file to randomly select a proxy per request. Timeout increase helps.  
-timeout [number]      : duration (milli) to wait for server response. Helps with proxy.  
-delay   [number]      : duration (milli) to wait between each thread's requests (throttle).  
-ua      [user agent]  : sets the user agent to use for requests. Default is random per request.    
-polite                : force threads to operate one at a time (single thread)  
-nocheck               : trust all SSL certs, may be useful with proxies.  
-torcheck              : at the start of each generation check if traffic is routed through tor.  

delays are PER THREAD. Ten threads will still send one requests before delaying.
Use polite mode if you want true throttling

REQUIRED, a base URL [base url, '^' as delimiter] : this sets the URL that should be attacked. 
It has not been tested for all URLs, but if the page throws a 302, 200, 404, it will probably work. 
An example of using this parameter is http://abc.cd/, the '^' character may also be used 
to generate between strings similar to http://abc.cd/^/something.com. Everything after the URL 
or at the generation point will be appended by guesses. The above example will generated guesses 
of the form http://abc.cd/guess. This is confirmed to work with some common targets. Requests
from a file may be used as well, put ^ into injection points. the request from file feature
is still being built out, so it may have some issues.

Also, I am in no way responsible for how you (mis)use this application. -ChrisJoinEngine




