package kr.co.cleverchat.poc;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Throwaway diagnostic: dump KEPCO board DOM structure (post-AJAX) to find the right selectors. */
@Tag("integration")
class KepcoDomProbeTest {

    @Test
    void probe() {
        String url = System.getenv("CLEVERCHAT_KEPCO_BOARD_URL");
        if (url == null || url.isBlank()) {
            System.out.println("[PROBE] no URL");
            return;
        }
        try (Playwright pw = Playwright.create();
                Browser browser =
                        pw.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true))) {
            Page page = browser.newPage();
            page.navigate(url);
            page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);
            page.waitForTimeout(3000);

            System.out.println("[PROBE] title = " + page.title());
            System.out.println(
                    "[PROBE] bodyTextLen = " + page.locator("body").innerText().length());

            String[] selectors = {
                "tbody tr",
                "table tr",
                "table tbody tr",
                "main .board-list",
                "main [class*=board]",
                "main [class*=list]",
                "main [class*=bbs]",
                "main ul[class*=list] li",
                "main ul[class*=board] li",
                "main li a[onclick*=fn_]",
                "main li a[onclick*=view]",
                "main a[onclick*='fn_view']",
                "main a[onclick*='fn_View']",
                "main a[onclick*='boardView']",
                "main .board-list-tbody .board-title a[href^='javascript:fn_Detail']",
                "main .card.board a.title[href^='javascript:fn_Detail']",
                "main .board-list-tbody .board-title a[href^='javascript:fn_Detail'], "
                        + "main .card.board a.title[href^='javascript:fn_Detail']",
                "ul li",
                "ol li",
                "a[href*=boardView]",
                "a[onclick]",
                "[class*=board] a",
                "[class*=list] a",
                ".bbs a",
                "td a",
                "th a",
                "[onclick*=view]",
                "[onclick*=fn_]",
                "iframe"
            };
            for (String sel : selectors) {
                System.out.println("[PROBE] count " + sel + " = " + page.locator(sel).count());
            }

            // Dump the first plausible list container's HTML (truncated).
            for (String container :
                    new String[] {
                        "main .board-list",
                        "main .board_list",
                        "main .list-board",
                        "main .list_board",
                        "main .board-wrap",
                        "main .board_wrap",
                        "main .bbs-list",
                        "main .bbs_list",
                        "main ul[class*=list]",
                        "main ul[class*=board]",
                        "table",
                        "ul.board",
                        ".board-list",
                        ".bbs",
                        "main"
                    }) {
                if (page.locator(container).count() > 0) {
                    String html = page.locator(container).first().innerHTML();
                    System.out.println(
                            "[PROBE] container "
                                    + container
                                    + " innerHTML[0..900] = "
                                    + html.substring(0, Math.min(900, html.length()))
                                            .replaceAll("\\s+", " "));
                }
            }

            List<String> classSamples =
                    (List<String>)
                            page.evalOnSelectorAll(
                                    "main *[class]",
                                    "els => els.map(e => e.tagName.toLowerCase() + '.'"
                                            + " + (e.className || '').toString().trim().replace(/\\s+/g,'.'))"
                                            + ".filter(s => /board|bbs|list|page|view|content|post/i.test(s))"
                                            + ".slice(0,80)");
            System.out.println("[PROBE] class samples = " + classSamples);

            // Sample anchors that look like detail links.
            List<String> sample =
                    (List<String>)
                            page.evalOnSelectorAll(
                                    "main a",
                                    "els => els"
                                            + ".filter(a => (a.getAttribute('onclick')||'').match(/view|fn_/i)"
                                            + " || (a.getAttribute('href')||'').match(/boardView|view\\.do/i))"
                                            + ".slice(0,20)"
                                            + ".map(a => (a.outerHTML||'').slice(0,260))");
            System.out.println("[PROBE] detail-anchor samples = " + sample.size());
            for (String s : sample) {
                System.out.println("[PROBE]   " + s.replaceAll("\\s+", " "));
            }

            String rowSelector =
                    "main .board-list-tbody .board-title a[href^='javascript:fn_Detail'], "
                            + "main .card.board a.title[href^='javascript:fn_Detail']";
            if (page.locator(rowSelector).count() > 0) {
                page.locator(rowSelector).first().click();
                page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);
                page.waitForTimeout(1000);
                System.out.println("[PROBE] detail url = " + page.url());
                System.out.println("[PROBE] detail title = " + page.title());
                String[] detailSelectors = {
                    "main h1",
                    "main h2",
                    "main h3",
                    "main .title",
                    "main .view-title",
                    "main .view_title",
                    "main .board-view",
                    "main .board_view",
                    "main .view",
                    "main .conts",
                    "main .contents",
                    "main [class*=view]",
                    "main [class*=content]",
                };
                for (String sel : detailSelectors) {
                    int count = page.locator(sel).count();
                    System.out.println("[PROBE] detail count " + sel + " = " + count);
                    if (count > 0) {
                        String text = page.locator(sel).first().innerText();
                        System.out.println(
                                "[PROBE] detail text "
                                        + sel
                                        + "[0..300] = "
                                        + text.substring(0, Math.min(300, text.length()))
                                                .replaceAll("\\s+", " "));
                    }
                }
            }
        }
    }
}
