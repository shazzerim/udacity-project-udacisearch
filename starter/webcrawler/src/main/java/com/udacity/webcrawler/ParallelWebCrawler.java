package com.udacity.webcrawler;

import com.udacity.webcrawler.json.CrawlResult;
import com.udacity.webcrawler.parser.PageParserFactory;

import javax.inject.Inject;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.regex.Pattern;

/**
 * A concrete implementation of {@link WebCrawler} that runs multiple threads on a
 * {@link ForkJoinPool} to fetch and process multiple web pages in parallel.
 */
final class ParallelWebCrawler implements WebCrawler {
    private final Clock clock;
    private final Duration timeout;
    private final int popularWordCount;
    private final ForkJoinPool pool;
    private final int maxDepth;
    private final List<Pattern> ignoredUrls;

    @Inject
    ParallelWebCrawler(
            Clock clock,
            @Timeout Duration timeout,
            @PopularWordCount int popularWordCount,
            @MaxDepth int maxDepth,
            @TargetParallelism int threadCount,
            @IgnoredUrls List<Pattern> ignoredUrls
    ) {
        this.clock = clock;
        this.timeout = timeout;
        this.popularWordCount = popularWordCount;
        this.maxDepth = maxDepth;
        this.ignoredUrls = ignoredUrls;
        this.pool = new ForkJoinPool(Math.min(threadCount, getMaxParallelism()));
    }

    @Inject
    PageParserFactory pageParserFactory;


    @Override
    public CrawlResult crawl(List<String> startingUrls) {

        Instant deadline = clock.instant().plus(timeout);
        Map<String, Integer> counts = Collections.synchronizedMap(new HashMap<>());
        Set<String> visitedUrls = Collections.synchronizedSet(new HashSet<>());



        CrawlerTask.CrawlerTaskBuilder crawlerTaskBuilder = new CrawlerTask
                .CrawlerTaskBuilder()
                .setCounts(counts)
                .setDeadline(deadline)
                .setVisitedUrls(visitedUrls)
                .setClock (clock)
                .setMaxDepth(maxDepth)
                .setIgnoredUrls(ignoredUrls)
                .setParserFactory(pageParserFactory);


       for (String url : startingUrls) {
            pool.invoke(crawlerTaskBuilder.setUrl(url).build());
        }

        if (counts.isEmpty()) {
            return new CrawlResult.Builder()
                    .setWordCounts(counts)
                    .setUrlsVisited(visitedUrls.size())
                    .build();
        }

        return new CrawlResult.Builder()
                .setWordCounts(WordCounts.sort(counts, popularWordCount))
                .setUrlsVisited(visitedUrls.size())
                .build();



    }

    @Override
    public int getMaxParallelism() {
        return Runtime.getRuntime().availableProcessors();
    }
}
