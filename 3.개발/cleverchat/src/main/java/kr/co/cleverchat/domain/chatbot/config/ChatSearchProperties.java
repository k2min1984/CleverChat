package kr.co.cleverchat.domain.chatbot.config;

import java.util.ArrayList;
import java.util.List;
import kr.co.cleverchat.domain.settings.RuntimeSetting;
import kr.co.cleverchat.domain.settings.RuntimeSettingsService;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "chat.search")
public class ChatSearchProperties {
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private RuntimeSettingsService runtimeSettings;

    private int poolLimit = 10;
    private int displayMax = 5;
    private double relativeThreshold = 0.5;
    private double minScore = 0.0;
    private int maxCrawlDocuments = 3;
    private boolean moreEnabled = true;
    private Label label = new Label();

    public int getPoolLimit() {
        return runtimeSettings == null
                ? Math.max(getDisplayMax(), poolLimit)
                : runtimeSettings.current().integer(RuntimeSetting.SEARCH_POOL);
    }

    public void setPoolLimit(int poolLimit) {
        this.poolLimit = poolLimit;
    }

    public int getDisplayMax() {
        return runtimeSettings == null
                ? Math.max(1, displayMax)
                : runtimeSettings.current().integer(RuntimeSetting.SEARCH_DISPLAY);
    }

    public void setDisplayMax(int displayMax) {
        this.displayMax = displayMax;
    }

    public double getRelativeThreshold() {
        return runtimeSettings == null
                ? Math.max(0.0, Math.min(1.0, relativeThreshold))
                : runtimeSettings.current().decimal(RuntimeSetting.SEARCH_RELATIVE);
    }

    public void setRelativeThreshold(double relativeThreshold) {
        this.relativeThreshold = relativeThreshold;
    }

    public boolean isMoreEnabled() {
        return runtimeSettings == null
                ? moreEnabled
                : runtimeSettings.current().bool(RuntimeSetting.SEARCH_MORE);
    }

    public void setMoreEnabled(boolean moreEnabled) {
        this.moreEnabled = moreEnabled;
    }

    public double getMinScore() {
        return runtimeSettings == null
                ? Math.max(0.0, minScore)
                : runtimeSettings.current().decimal(RuntimeSetting.SEARCH_MIN_SCORE);
    }

    public void setMinScore(double minScore) {
        this.minScore = minScore;
    }

    public int getMaxCrawlDocuments() {
        return runtimeSettings == null
                ? Math.max(1, maxCrawlDocuments)
                : runtimeSettings.current().integer(RuntimeSetting.SEARCH_CRAWL);
    }

    public void setMaxCrawlDocuments(int maxCrawlDocuments) {
        this.maxCrawlDocuments = maxCrawlDocuments;
    }

    public Label getLabel() {
        return label;
    }

    public void setLabel(Label label) {
        this.label = label == null ? new Label() : label;
    }

    public static class Label {
        private List<String> delimiters = new ArrayList<>(List.of("|", ">", "·", " - ", "/"));
        private List<String> siteSuffixes = new ArrayList<>(List.of("한국전력공사", "KEPCO"));
        private Prefer prefer = Prefer.FIRST;

        public List<String> getDelimiters() {
            return delimiters == null ? List.of() : delimiters;
        }

        public void setDelimiters(List<String> delimiters) {
            this.delimiters = delimiters == null ? List.of() : delimiters;
        }

        public List<String> getSiteSuffixes() {
            return siteSuffixes == null ? List.of() : siteSuffixes;
        }

        public void setSiteSuffixes(List<String> siteSuffixes) {
            this.siteSuffixes = siteSuffixes == null ? List.of() : siteSuffixes;
        }

        public Prefer getPrefer() {
            return prefer == null ? Prefer.FIRST : prefer;
        }

        public void setPrefer(Prefer prefer) {
            this.prefer = prefer == null ? Prefer.FIRST : prefer;
        }
    }

    public enum Prefer {
        FIRST,
        LAST
    }
}
