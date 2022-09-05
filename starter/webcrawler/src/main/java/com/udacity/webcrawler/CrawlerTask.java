package com.udacity.webcrawler;

import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.RecursiveAction;
import java.util.regex.Pattern;

public final class CrawlerTask extends RecursiveAction {

    private final Instant deadline;
    private final int maxDepth;
    private final Map<String, Integer> counts;
    private final Set<String> visitedUrls;
    private final String url;
    private final List<Pattern> ignoredUrls;
    private final Clock clock;
    private final PageParserFactory parserFactory;


    private CrawlerTask(Instant deadline,
                        int maxDepth,
                        Map<String, Integer> counts,
                        Set<String> visitedUrls,
                        String url,
                        List<Pattern> ignoredUrls,
                        Clock clock,
                        PageParserFactory parserFactory) {

        this.deadline = deadline;
        this.maxDepth = maxDepth;
        this.counts = counts;
        this.visitedUrls = visitedUrls;
        this.url = url;
        this.ignoredUrls = ignoredUrls;
        this.clock = clock;
        this.parserFactory = parserFactory;
    }

    public Instant getDeadline() {
        return deadline;
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public Map<String, Integer> getCounts() {
        return counts;
    }

    public Set<String> getVisitedUrls() {
        return visitedUrls;
    }

    public String getUrl() {
        return url;
    }

    public List<Pattern> getIgnoredUrls() {
        return ignoredUrls;
    }

    @Override
    protected void compute() {
        if (maxDepth == 0 || clock.instant().isAfter(deadline) || isIgnored(url) || !visitedUrls.add(url)) {
            return;
        }
        // visitedUrls.add(url);

        PageParser.Result result = parserFactory.get(url).parse();
        for (Map.Entry<String, Integer> e : result.getWordCounts().entrySet()) {
            if (counts.containsKey(e.getKey())) {
                counts.put(e.getKey(), e.getValue() + counts.get(e.getKey()));
            } else {
                counts.put(e.getKey(), e.getValue());
            }
        }

        List<CrawlerTask> subUrlTasks = new ArrayList<>();

        result.getLinks().forEach(subUrl -> subUrlTasks.add(new CrawlerTask(
                deadline,
                maxDepth - 1,
                counts,
                visitedUrls,
                subUrl,
                ignoredUrls,
                clock,
                parserFactory
        )));

        invokeAll(subUrlTasks);
    }

    private boolean isIgnored(String url) {
        return ignoredUrls
                .stream()
                .anyMatch(pattern -> pattern.matcher(url).matches());
    }

    public static final class CrawlerTaskBuilder {

        private Instant deadline;
        private int maxDepth;
        private Map<String, Integer> counts;
        private Set<String> visitedUrls;
        private List<Pattern> ignoredUrls;
        private String url;
        private Clock clock;
        private PageParserFactory parserFactory;

        public CrawlerTaskBuilder setDeadline(Instant deadline) {
            this.deadline = deadline;
            return this;
        }

        public CrawlerTaskBuilder setCounts(Map<String, Integer> counts) {
            this.counts = counts;
            return this;
        }

        public CrawlerTaskBuilder setVisitedUrls(Set<String> visitedUrls) {
            this.visitedUrls = visitedUrls;
            return this;
        }

        public CrawlerTaskBuilder setUrl(String url) {
            this.url = url;
            return this;
        }

        public CrawlerTaskBuilder setClock(Clock clock) {
            this.clock = clock;
            return this;
        }

        public CrawlerTaskBuilder setMaxDepth(int maxDepth) {
            this.maxDepth = maxDepth;
            return this;
        }

        public CrawlerTaskBuilder setIgnoredUrls(List<Pattern> ignoredUrls) {
            this.ignoredUrls = ignoredUrls;
            return this;
        }

        public CrawlerTaskBuilder setParserFactory(PageParserFactory parserFactory) {
            this.parserFactory = parserFactory;
            return this;
        }

        public CrawlerTask build() {
            return new CrawlerTask(deadline, maxDepth, counts, visitedUrls, url, ignoredUrls, clock, parserFactory);
        }


    }


}
