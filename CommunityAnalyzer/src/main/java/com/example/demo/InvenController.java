package com.example.demo;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
public class InvenController {

    /* ======================
       DTO
       ====================== */
    public record PostDto(String title, String url) {}

    public record CommentDto(
        String writer,
        String content,
        int like,
        int dislike,
        int depth,
        boolean best,
        boolean blind
    ) {}

    /* ======================
       게시글 목록
       ====================== */
    @GetMapping("/inven")
    public String list(
            @RequestParam(defaultValue = "1") int page,
            Model model) throws Exception {

        String listUrl =
            "https://www.inven.co.kr/board/maple/5974?my=chu&p=" + page;

        Document doc = Jsoup.connect(listUrl)
                .userAgent("Mozilla/5.0")
                .timeout(5000)
                .get();

        List<PostDto> posts = new ArrayList<>();

        for (Element row : doc.select("tr")) {
            Element a = row.selectFirst("td.tit a");
            if (a == null) continue;

            String href = a.attr("href");
            String detailUrl = href.startsWith("http")
                    ? href
                    : "https://www.inven.co.kr" + href;

            posts.add(new PostDto(a.text(), detailUrl));
        }

        model.addAttribute("posts", posts);
        model.addAttribute("page", page);
        return "list";
    }

    /* ======================
       게시글 상세 (본문 + 댓글)
       ====================== */
    @GetMapping("/inven/detail")
    public String detail(
            @RequestParam String url,
            Model model) throws Exception {

        /* ---------- 본문 ---------- */
        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0")
                .timeout(5000)
                .get();

        Element rawContent = doc.selectFirst("#powerbbsContent");
        Document contentDoc = Jsoup.parseBodyFragment(
                rawContent != null ? rawContent.html() : ""
        );
        contentDoc.select("script, style").remove();

        // 이미지 프록시
        for (Element img : contentDoc.select("img")) {
            String src = img.attr("src");
            if (!src.isBlank()) {
                img.attr("src",
                    "/image?url=" + URLEncoder.encode(src, StandardCharsets.UTF_8)
                );
            }
        }

        /* ---------- 댓글(JSON) ---------- */
        String articleCode = url.replaceAll(".*/(\\d+).*", "$1");
        List<CommentDto> all = fetchAllComments(articleCode);

        model.addAttribute(
            "bestComments",
            all.stream().filter(CommentDto::best).toList()
        );
        model.addAttribute(
            "comments",
            all.stream().filter(c -> !c.best()).toList()
        );

        model.addAttribute("content", contentDoc.body().html());

        return "detail";
    }

    /* ======================
       댓글 JSON 호출
       ====================== */
	private List<CommentDto> fetchAllComments(String articleCode) throws Exception {
	
	    ObjectMapper mapper = new ObjectMapper();
	    List<CommentDto> result = new ArrayList<>();
	
	    // 중복 제거용 Set
	    Set<Integer> bestSeenCmtIds = new HashSet<>();
	    Set<Integer> normalSeenCmtIds = new HashSet<>();
	
	    int titles = 0;
	    int prevTitles = -1;
	    boolean bestCollected = false;
	
	    while (true) {
	
	        String json = fetchCommentJson(articleCode, titles);
	        JsonNode root = mapper.readTree(json);
	
	        int nextTitles = -1;
	
	        // 베스트 댓글 (1회만)
	        if (!bestCollected) {
	            for (JsonNode c : root.path("bestcomment").path("list")) {
	
	                JsonNode attr = c.path("__attr__");
	                int cmtidx = attr.path("cmtidx").asInt();
	
	                if (bestSeenCmtIds.contains(cmtidx)) continue;
	                bestSeenCmtIds.add(cmtidx);
	
	                CommentDto dto = new CommentDto(
	                    c.path("o_name").asText(),
	                    decodeComment(c.path("o_comment").asText()),
	                    c.path("o_recommend").asInt(),
	                    c.path("o_notrecommend").asInt(),
	                    0,
	                    true,
	                    !"Y".equals(attr.path("state").asText())
	                );
	
	                result.add(dto);
	            }
	            bestCollected = true;
	        }
	
	        // 일반 댓글
	        for (JsonNode block : root.path("commentlist")) {
	            for (JsonNode c : block.path("list")) {
	                JsonNode attr = c.path("__attr__");
	                int cmtidx  = attr.path("cmtidx").asInt();
	                int cmtpidx = attr.path("cmtpidx").asInt();
	
	                // 중복 제거
	                if (normalSeenCmtIds.contains(cmtidx)) continue;
	                normalSeenCmtIds.add(cmtidx);
	
	                CommentDto dto = new CommentDto(
	                    c.path("o_name").asText(),
	                    decodeComment(c.path("o_comment").asText()),
	                    c.path("o_recommend").asInt(),
	                    c.path("o_notrecommend").asInt(),
	                    cmtidx == cmtpidx ? 0 : 1,
	                    false,
	                    !"Y".equals(attr.path("state").asText())
	                );
	
	                result.add(dto);
	            }
	
	            // 다음 titles 힌트
	            if (block.path("list").size() == 0) {
	                int t = block.path("__attr__").path("titlenum").asInt();
	                if (t > 0 && t != titles) {
	                    nextTitles = t;
	                }
	            }
	        }
	
	        if (nextTitles <= 0 || nextTitles == prevTitles) break;
	
	        prevTitles = titles;
	        titles = nextTitles;
	    }
	
	    return result;
	}
    
    private String fetchCommentJson(String articleCode, int titles)
            throws Exception {

        String apiUrl = "https://www.inven.co.kr/common/board/comment.json.php";

        StringBuilder params = new StringBuilder();
        params.append("act=list");
        params.append("&out=json");
        params.append("&comeidx=5974");
        params.append("&articlecode=").append(articleCode);
        params.append("&sortorder=date");

        // 핵심: titles 파라미터
        if (titles > 0) {
            params.append("&titles=").append(titles);
        }

        HttpURLConnection conn =
            (HttpURLConnection) new URL(apiUrl).openConnection();

        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty(
            "Content-Type",
            "application/x-www-form-urlencoded; charset=UTF-8"
        );
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        conn.setRequestProperty("X-Requested-With", "XMLHttpRequest");
        conn.setRequestProperty(
            "Referer",
            "https://www.inven.co.kr/board/maple/5974/" + articleCode
        );

        conn.getOutputStream()
            .write(params.toString().getBytes(StandardCharsets.UTF_8));

        try (InputStream in = conn.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

      
    private String decodeComment(String raw) {
        if (raw == null) return "";
        return Jsoup.parse(Parser.unescapeEntities(raw, false)).text();
    }
    
    /* ======================
       이미지 프록시
       ====================== */
    @GetMapping("/image")
    public ResponseEntity<byte[]> proxyImage(@RequestParam String url)
            throws Exception {

        HttpURLConnection conn =
            (HttpURLConnection) new URL(url).openConnection();

        conn.setRequestMethod("GET");
        conn.setRequestProperty("Referer", "https://www.inven.co.kr/");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");

        try (InputStream in = conn.getInputStream()) {
            byte[] bytes = in.readAllBytes();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_JPEG);

            return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
        }
    }

    /* ======================
       JSON 단독 테스트
       ====================== */
    @GetMapping("/debug/comments")
    @ResponseBody
    public String debugComments() throws Exception {

        String articleCode = "6021470"; // 테스트용

        int titlenum = 0;
        int prev = -1;
        int loop = 0;

        while (true) {

            String json = fetchCommentJson(articleCode, titlenum);

            // 파일로 저장
            Path path = Path.of(
                "C:/Users/delta39/Desktop/comments_" + titlenum + ".json"
            );
            Files.writeString(path, json, StandardCharsets.UTF_8);

            // 다음 titlenum 추출
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);

            int next = -1;

            for (JsonNode block : root.path("commentlist")) {
                if (block.path("list").size() == 0) {
                    int t = block.path("__attr__").path("titlenum").asInt();
                    if (t > 0) {
                        next = t;
                        break; // 가장 첫 힌트만 사용
                    }
                }
            }

            System.out.println(
                "loop=" + loop +
                ", titlenum=" + titlenum +
                ", next=" + next
            );

            if (next <= 0 || next == prev) break;

            prev = titlenum;
            titlenum = next;
            loop++;

            // 안전장치
            if (loop > 20) break;
        }

        return "DONE (check json files)";
    }
}
